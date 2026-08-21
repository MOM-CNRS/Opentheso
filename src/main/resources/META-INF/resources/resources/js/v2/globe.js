/**
 * Globe V2 — module autonome (WebGL points, overlay 2D, rAF idle, voisinage).
 * API : window.OTGlobe.attach(host).ensure() / stop() / invalidate() / selectId() / reload()
 */
(function (global) {
  "use strict";

  const COLORS = {
    valide: [31, 122, 92],
    insere: [31, 122, 92],
    candidat: [224, 138, 76],
    deprecie: [154, 160, 166],
    rejete: [194, 69, 58]
  };
  const GOLDEN = Math.PI * (3 - Math.sqrt(5));
  const EDGE_COLOR = {
    TG: "rgba(31,122,92,0.72)",
    TS: "rgba(47,108,176,0.72)",
    TA: "rgba(180,120,40,0.72)"
  };
  const VS = [
    "attribute vec3 aPos;",
    "attribute vec3 aColor;",
    "uniform float uRotY,uRotX,uZoom,uPanX,uPanY,uDpr;",
    "uniform vec2 uRes;",
    "varying vec3 vColor;",
    "void main(){",
    " float cy=cos(uRotY),sy=sin(uRotY),cx=cos(uRotX),sx=sin(uRotX);",
    " float x1=aPos.x*cy-aPos.z*sy;",
    " float z1=aPos.x*sy+aPos.z*cy;",
    " float y2=aPos.y*cx-z1*sx;",
    " float z2=aPos.y*sx+z1*cx;",
    " float radius=min(uRes.x,uRes.y)*0.42*uZoom;",
    " float scale=radius*(2.35/(2.35+1.0-z2));",
    " vec2 scr=vec2(uRes.x*0.5+uPanX+x1*scale,uRes.y*0.5+uPanY+y2*scale);",
    " gl_Position=vec4((scr.x/uRes.x)*2.0-1.0,1.0-(scr.y/uRes.y)*2.0,(1.0-z2)*0.5,1.0);",
    " float t=(z2+1.0)*0.5;",
    " gl_PointSize=mix(2.2,6.2,t)*uDpr;",
    " if(z2<-0.18) gl_PointSize=0.0;",
    " vColor=aColor*mix(0.28,1.0,t);",
    "}"
  ].join("\n");
  const FS = [
    "precision mediump float;",
    "varying vec3 vColor;",
    "void main(){",
    " vec2 c=gl_PointCoord-vec2(0.5);",
    " if(dot(c,c)>0.25) discard;",
    " gl_FragColor=vec4(vColor,1.0);",
    "}"
  ].join("\n");

  let host = null;
  let cache = { key: "", loading: false, nodes: [], truncated: false };
  let loop = resetLoop();
  let gl = null;
  let glProg = null;
  let glBufPos = null;
  let glBufCol = null;
  let glLoc = null;
  let neighborhood = { id: "", edges: [], neighbor: {} };
  let matchQuery = "";

  function $(s, r) {
    return (r || document).querySelector(s);
  }

  function resetLoop() {
    return {
      raf: 0,
      running: false,
      rotX: 0.22,
      rotY: 0.35,
      velX: 0,
      velY: 0,
      zoom: 1,
      panX: 0,
      panY: 0,
      hover: -1,
      selected: -1,
      drag: null,
      pinch: null,
      pointers: new Map(),
      fly: null,
      moved: false,
      px: null,
      py: null,
      pz: null,
      sx: null,
      sy: null,
      sz: null,
      colors: null,
      ids: [],
      labels: [],
      n: 0,
      w: 0,
      h: 0,
      dpr: 1,
      bound: false,
      hash: null,
      cell: 48
    };
  }

  function reducedMotion() {
    return !!(window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  }

  function canvas() {
    return document.getElementById("hyperGlobe");
  }

  function overlay() {
    return document.getElementById("hyperGlobeFx");
  }

  function stage() {
    return document.getElementById("hyperStage");
  }

  function cacheKey() {
    return host.thesaurusId() + "|" + host.thesaurusLang();
  }

  function isHyper() {
    return host && host.isHyper();
  }

  function setLoading(on) {
    const view = $("#viewHyper");
    const spin = $("#hyperLoading");
    if (view) view.classList.toggle("is-loading", !!on);
    if (spin) {
      if (on) spin.removeAttribute("hidden");
      else spin.setAttribute("hidden", "hidden");
      spin.hidden = !on;
    }
    const empty = $("#hyperEmpty");
    if (on && empty) empty.hidden = true;
  }

  function paintCount() {
    const el = $("#hyperCount");
    if (!el) return;
    const n = cache.nodes.length;
    el.textContent = n
      ? (n.toLocaleString("fr-FR") + " concept" + (n > 1 ? "s" : "") + (cache.truncated ? " (aperçu)" : ""))
      : "";
  }

  function paintPicked() {
    const el = $("#hyperPicked");
    const btn = $("#hyperOpen");
    const i = loop.selected;
    if (el) el.textContent = i >= 0 ? (loop.labels[i] || "") : "";
    if (btn) btn.disabled = i < 0;
  }

  function paintNeighbors() {
    const box = $("#hyperNeighbors");
    if (!box) return;
    const n = neighborhood;
    if (!n.id || loop.selected < 0) {
      box.hidden = true;
      box.innerHTML = "";
      return;
    }
    const chips = [];
    function add(list, role) {
      list.forEach((item) => {
        chips.push("<span class=\"hyper-chip hyper-chip-" + role + "\">" + role + " "
          + escape(item.label || item.id) + "</span>");
      });
    }
    add(n.broader || [], "TG");
    add(n.narrower || [], "TS");
    add(n.related || [], "TA");
    box.hidden = chips.length === 0;
    box.innerHTML = chips.slice(0, 12).join("");
  }

  function escape(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  function layout(nodes) {
    const n = nodes.length;
    loop.n = n;
    loop.ids = new Array(n);
    loop.labels = new Array(n);
    loop.px = new Float32Array(n);
    loop.py = new Float32Array(n);
    loop.pz = new Float32Array(n);
    loop.sx = new Float32Array(n);
    loop.sy = new Float32Array(n);
    loop.sz = new Float32Array(n);
    loop.colors = new Float32Array(n * 3);
    const denom = Math.max(1, n - 1);
    for (let i = 0; i < n; i++) {
      const node = nodes[i];
      loop.ids[i] = node.id;
      loop.labels[i] = node.label || node.id;
      const y = 1 - (i / denom) * 2;
      const r = Math.sqrt(Math.max(0, 1 - y * y));
      const theta = GOLDEN * i;
      loop.px[i] = Math.cos(theta) * r;
      loop.py[i] = y;
      loop.pz[i] = Math.sin(theta) * r;
      const rgb = COLORS[node.status] || COLORS.valide;
      loop.colors[i * 3] = rgb[0] / 255;
      loop.colors[i * 3 + 1] = rgb[1] / 255;
      loop.colors[i * 3 + 2] = rgb[2] / 255;
    }
    loop.hover = -1;
    loop.selected = -1;
    uploadGl();
    if (host.conceptId()) selectId(host.conceptId(), false);
    paintPicked();
  }

  function initGl() {
    const el = canvas();
    if (!el || gl) return;
    try {
      gl = el.getContext("webgl", { antialias: false, alpha: true, premultipliedAlpha: false })
        || el.getContext("experimental-webgl", { antialias: false, alpha: true, premultipliedAlpha: false });
    } catch (ex) {
      gl = null;
    }
    if (!gl) return;
    const vs = compile(gl.VERTEX_SHADER, VS);
    const fs = compile(gl.FRAGMENT_SHADER, FS);
    if (!vs || !fs) {
      gl = null;
      return;
    }
    glProg = gl.createProgram();
    gl.attachShader(glProg, vs);
    gl.attachShader(glProg, fs);
    gl.linkProgram(glProg);
    if (!gl.getProgramParameter(glProg, gl.LINK_STATUS)) {
      gl = null;
      return;
    }
    glLoc = {
      aPos: gl.getAttribLocation(glProg, "aPos"),
      aColor: gl.getAttribLocation(glProg, "aColor"),
      uRotY: gl.getUniformLocation(glProg, "uRotY"),
      uRotX: gl.getUniformLocation(glProg, "uRotX"),
      uZoom: gl.getUniformLocation(glProg, "uZoom"),
      uPanX: gl.getUniformLocation(glProg, "uPanX"),
      uPanY: gl.getUniformLocation(glProg, "uPanY"),
      uDpr: gl.getUniformLocation(glProg, "uDpr"),
      uRes: gl.getUniformLocation(glProg, "uRes")
    };
    glBufPos = gl.createBuffer();
    glBufCol = gl.createBuffer();
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
  }

  function compile(type, src) {
    const sh = gl.createShader(type);
    gl.shaderSource(sh, src);
    gl.compileShader(sh);
    if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) return null;
    return sh;
  }

  function uploadGl() {
    if (!gl || !glProg || !loop.n) return;
    const pos = new Float32Array(loop.n * 3);
    for (let i = 0; i < loop.n; i++) {
      pos[i * 3] = loop.px[i];
      pos[i * 3 + 1] = loop.py[i];
      pos[i * 3 + 2] = loop.pz[i];
    }
    gl.bindBuffer(gl.ARRAY_BUFFER, glBufPos);
    gl.bufferData(gl.ARRAY_BUFFER, pos, gl.STATIC_DRAW);
    uploadGlColors();
  }

  function uploadGlColors() {
    if (!gl || !glBufCol || !loop.n) return;
    const col = new Float32Array(loop.n * 3);
    const focused = !!neighborhood.id;
    const q = matchQuery;
    for (let i = 0; i < loop.n; i++) {
      let f = 1;
      if (focused && i !== loop.selected && !neighborhood.neighbor[loop.ids[i]]) f = 0.22;
      if (q && (loop.labels[i] || "").toLowerCase().indexOf(q) < 0) f *= 0.35;
      col[i * 3] = loop.colors[i * 3] * f;
      col[i * 3 + 1] = loop.colors[i * 3 + 1] * f;
      col[i * 3 + 2] = loop.colors[i * 3 + 2] * f;
    }
    gl.bindBuffer(gl.ARRAY_BUFFER, glBufCol);
    gl.bufferData(gl.ARRAY_BUFFER, col, gl.DYNAMIC_DRAW);
  }

  function resize() {
    const el = canvas();
    const fx = overlay();
    const st = stage();
    if (!el || !st) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const w = Math.max(1, st.clientWidth);
    const h = Math.max(1, st.clientHeight);
    const pw = Math.round(w * dpr);
    const ph = Math.round(h * dpr);
    [el, fx].forEach((c) => {
      if (!c) return;
      if (c.width !== pw || c.height !== ph) {
        c.width = pw;
        c.height = ph;
        c.style.width = w + "px";
        c.style.height = h + "px";
      }
    });
    loop.w = w;
    loop.h = h;
    loop.dpr = dpr;
    if (gl) gl.viewport(0, 0, pw, ph);
  }

  function project() {
    const n = loop.n;
    const cosY = Math.cos(loop.rotY);
    const sinY = Math.sin(loop.rotY);
    const cosX = Math.cos(loop.rotX);
    const sinX = Math.sin(loop.rotX);
    const cx = loop.w * 0.5 + loop.panX;
    const cy = loop.h * 0.5 + loop.panY;
    const radius = Math.min(loop.w, loop.h) * 0.42 * loop.zoom;
    const focal = 2.35;
    const hash = Object.create(null);
    const cell = loop.cell;
    for (let i = 0; i < n; i++) {
      const x1 = loop.px[i] * cosY - loop.pz[i] * sinY;
      const z1 = loop.px[i] * sinY + loop.pz[i] * cosY;
      const y2 = loop.py[i] * cosX - z1 * sinX;
      const z2 = loop.py[i] * sinX + z1 * cosX;
      const scale = radius * (focal / (focal + 1 - z2));
      const sx = cx + x1 * scale;
      const sy = cy + y2 * scale;
      loop.sx[i] = sx;
      loop.sy[i] = sy;
      loop.sz[i] = z2;
      if (z2 < -0.02) continue;
      const kx = Math.floor(sx / cell);
      const ky = Math.floor(sy / cell);
      const key = kx + ":" + ky;
      const bucket = hash[key] || (hash[key] = []);
      bucket.push(i);
    }
    loop.hash = hash;
  }

  function hit(mx, my) {
    if (!loop.n || !loop.hash) return -1;
    const hitR = 12 + loop.zoom * 6;
    const maxD = hitR * hitR;
    const cell = loop.cell;
    const kx = Math.floor(mx / cell);
    const ky = Math.floor(my / cell);
    let best = -1;
    let bestD = maxD;
    for (let ox = -1; ox <= 1; ox++) {
      for (let oy = -1; oy <= 1; oy++) {
        const bucket = loop.hash[(kx + ox) + ":" + (ky + oy)];
        if (!bucket) continue;
        for (let b = 0; b < bucket.length; b++) {
          const i = bucket[b];
          if (loop.sz[i] < -0.02) continue;
          const dx = loop.sx[i] - mx;
          const dy = loop.sy[i] - my;
          const d = dx * dx + dy * dy;
          if (d < bestD) {
            bestD = d;
            best = i;
          }
        }
      }
    }
    return best;
  }

  function drawGl() {
    if (!gl || !glProg || !loop.n) return false;
    gl.viewport(0, 0, canvas().width, canvas().height);
    gl.clearColor(0, 0, 0, 0);
    gl.clear(gl.COLOR_BUFFER_BIT);
    gl.useProgram(glProg);
    gl.uniform1f(glLoc.uRotY, loop.rotY);
    gl.uniform1f(glLoc.uRotX, loop.rotX);
    gl.uniform1f(glLoc.uZoom, loop.zoom);
    gl.uniform1f(glLoc.uPanX, loop.panX);
    gl.uniform1f(glLoc.uPanY, loop.panY);
    gl.uniform1f(glLoc.uDpr, loop.dpr);
    gl.uniform2f(glLoc.uRes, loop.w, loop.h);
    gl.bindBuffer(gl.ARRAY_BUFFER, glBufPos);
    gl.enableVertexAttribArray(glLoc.aPos);
    gl.vertexAttribPointer(glLoc.aPos, 3, gl.FLOAT, false, 0, 0);
    gl.bindBuffer(gl.ARRAY_BUFFER, glBufCol);
    gl.enableVertexAttribArray(glLoc.aColor);
    gl.vertexAttribPointer(glLoc.aColor, 3, gl.FLOAT, false, 0, 0);
    gl.drawArrays(gl.POINTS, 0, loop.n);
    return true;
  }

  function pointsContext() {
    const el = canvas();
    if (!gl && el) {
      try {
        const ctx = el.getContext("2d");
        if (ctx) return ctx;
      } catch (ex) { /* canvas already WebGL */ }
    }
    const fx = overlay();
    return fx ? fx.getContext("2d") : null;
  }

  function drawCanvasPoints(ctx) {
    const n = loop.n;
    const buckets = [ [], [], [], [], [], [], [], [] ];
    for (let i = 0; i < n; i++) {
      const z = loop.sz[i];
      if (z < -0.18) continue;
      buckets[Math.min(7, Math.max(0, (z + 1) * 4 | 0))].push(i);
    }
    const baseR = n > 8000 ? 1.15 : n > 3000 ? 1.45 : 1.85;
    const q = matchQuery;
    for (let b = 0; b < 8; b++) {
      const list = buckets[b];
      for (let k = 0; k < list.length; k++) {
        const i = list[k];
        const z = loop.sz[i];
        const t = (z + 1) * 0.5;
        const r = baseR * (0.7 + t * 0.7);
        const o = i * 3;
        const dim = neighborhood.id && !neighborhood.neighbor[loop.ids[i]] && i !== loop.selected;
        const a = (dim ? 0.12 : 0.2 + t * 0.8);
        ctx.fillStyle = "rgba(" + Math.round(loop.colors[o] * 255) + ","
          + Math.round(loop.colors[o + 1] * 255) + ","
          + Math.round(loop.colors[o + 2] * 255) + "," + a + ")";
        const x = loop.sx[i];
        const y = loop.sy[i];
        if (i === loop.selected || i === loop.hover || (q && loop.labels[i].toLowerCase().indexOf(q) >= 0)) {
          ctx.beginPath();
          ctx.arc(x, y, r + 2.2, 0, Math.PI * 2);
          ctx.fill();
        } else {
          const s = Math.max(1.2, r * 1.6);
          ctx.fillRect(x - s * 0.5, y - s * 0.5, s, s);
        }
      }
    }
  }

  function drawOverlay(ctx, skipClear) {
    const w = loop.w;
    const h = loop.h;
    ctx.setTransform(loop.dpr, 0, 0, loop.dpr, 0, 0);
    if (!skipClear) ctx.clearRect(0, 0, w, h);
    const cx = w * 0.5 + loop.panX;
    const cy = h * 0.5 + loop.panY;
    const radius = Math.min(w, h) * 0.42 * loop.zoom;
    const disc = ctx.createRadialGradient(cx - radius * 0.25, cy - radius * 0.3, radius * 0.1, cx, cy, radius);
    disc.addColorStop(0, "rgba(31,122,92,0.10)");
    disc.addColorStop(0.72, "rgba(31,122,92,0.04)");
    disc.addColorStop(1, "rgba(31,122,92,0)");
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.fillStyle = disc;
    ctx.fill();
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.strokeStyle = "rgba(31,122,92,0.22)";
    ctx.lineWidth = 1.25;
    ctx.stroke();
    const edges = neighborhood.edges || [];
    for (let e = 0; e < edges.length; e++) {
      const a = edges[e].a;
      const b = edges[e].b;
      if (loop.sz[a] < -0.05 || loop.sz[b] < -0.05) continue;
      ctx.beginPath();
      ctx.moveTo(loop.sx[a], loop.sy[a]);
      ctx.lineTo(loop.sx[b], loop.sy[b]);
      ctx.strokeStyle = EDGE_COLOR[edges[e].role] || EDGE_COLOR.TG;
      ctx.lineWidth = 1.35;
      ctx.stroke();
    }
    const mark = loop.hover >= 0 ? loop.hover : loop.selected;
    if (mark >= 0 && loop.sz[mark] > -0.05) {
      ctx.beginPath();
      ctx.arc(loop.sx[mark], loop.sy[mark], 6, 0, Math.PI * 2);
      ctx.strokeStyle = "rgba(31,122,92,0.95)";
      ctx.lineWidth = 1.6;
      ctx.stroke();
    }
    let labelI = mark;
    if (labelI < 0) {
      let bestZ = 0.55;
      for (let i = 0; i < loop.n; i++) {
        if (loop.sz[i] > bestZ) {
          bestZ = loop.sz[i];
          labelI = i;
        }
      }
    }
    if (labelI >= 0 && loop.sz[labelI] > 0.12) {
      const label = loop.labels[labelI];
      ctx.font = "600 13px IBM Plex Sans, sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "bottom";
      ctx.lineWidth = 4;
      ctx.strokeStyle = "rgba(255,255,255,0.92)";
      ctx.fillStyle = "#17382c";
      ctx.strokeText(label, loop.sx[labelI], loop.sy[labelI] - 8);
      ctx.fillText(label, loop.sx[labelI], loop.sy[labelI] - 8);
    }
  }

  function draw() {
    const el = canvas();
    if (!el || !isHyper()) return;
    if (!loop.n) {
      const fx = overlay();
      if (fx) {
        const ctx = fx.getContext("2d");
        if (ctx) {
          ctx.setTransform(loop.dpr, 0, 0, loop.dpr, 0, 0);
          ctx.clearRect(0, 0, loop.w, loop.h);
        }
      }
      return;
    }
    project();
    const usedGl = drawGl();
    const pts = usedGl ? null : pointsContext();
    const fx = overlay();
    const ptsOnFx = !!(pts && fx && pts.canvas === fx);
    if (pts && !ptsOnFx) {
      pts.setTransform(loop.dpr, 0, 0, loop.dpr, 0, 0);
      pts.clearRect(0, 0, loop.w, loop.h);
      drawCanvasPoints(pts);
    }
    if (fx) {
      const ctx = fx.getContext("2d");
      if (ctx) {
        if (ptsOnFx) {
          ctx.setTransform(loop.dpr, 0, 0, loop.dpr, 0, 0);
          ctx.clearRect(0, 0, loop.w, loop.h);
          drawCanvasPoints(ctx);
          drawOverlay(ctx, true);
        } else {
          drawOverlay(ctx, false);
        }
      }
    }
  }

  function needsTick() {
    return !!(loop.drag || loop.pinch || loop.fly
      || Math.abs(loop.velX) > 0.00006 || Math.abs(loop.velY) > 0.00006);
  }

  function tick() {
    if (!loop.running) return;
    if (!isHyper()) {
      stop();
      return;
    }
    if (loop.fly) {
      loop.fly.t = Math.min(1, loop.fly.t + (reducedMotion() ? 1 : 0.08));
      const ease = 1 - Math.pow(1 - loop.fly.t, 3);
      loop.rotX = loop.fly.fromX + loop.fly.dx * ease;
      loop.rotY = loop.fly.fromY + loop.fly.dy * ease;
      if (loop.fly.t >= 1) loop.fly = null;
    } else if (!loop.drag && !loop.pinch) {
      loop.rotY += loop.velY;
      loop.rotX += loop.velX;
      loop.velY *= 0.92;
      loop.velX *= 0.92;
      if (Math.abs(loop.velY) < 0.00006) loop.velY = 0;
      if (Math.abs(loop.velX) < 0.00006) loop.velX = 0;
      loop.rotX = Math.max(-1.45, Math.min(1.45, loop.rotX));
    }
    draw();
    if (needsTick()) loop.raf = requestAnimationFrame(tick);
    else loop.running = false;
  }

  function kick() {
    if (!isHyper()) return;
    if (loop.running) return;
    loop.running = true;
    loop.raf = requestAnimationFrame(tick);
  }

  function stop() {
    loop.running = false;
    if (loop.raf) cancelAnimationFrame(loop.raf);
    loop.raf = 0;
  }

  function clampPan() {
    const max = Math.min(loop.w || 1, loop.h || 1) * 0.55 * Math.max(1, loop.zoom);
    loop.panX = Math.max(-max, Math.min(max, loop.panX));
    loop.panY = Math.max(-max, Math.min(max, loop.panY));
  }

  function aimAt(i) {
    if (i < 0 || !loop.px) return;
    const toY = Math.atan2(loop.px[i], loop.pz[i]);
    const z1 = loop.px[i] * Math.sin(toY) + loop.pz[i] * Math.cos(toY);
    const toX = Math.max(-1.45, Math.min(1.45, Math.atan2(loop.py[i], z1)));
    let dy = toY - loop.rotY;
    while (dy > Math.PI) dy -= Math.PI * 2;
    while (dy < -Math.PI) dy += Math.PI * 2;
    if (reducedMotion()) {
      loop.rotX = toX;
      loop.rotY = toY;
      loop.fly = null;
      return;
    }
    loop.fly = { fromX: loop.rotX, fromY: loop.rotY, dx: toX - loop.rotX, dy: dy, t: 0 };
    loop.velX = 0;
    loop.velY = 0;
    kick();
  }

  function selectId(id, doDraw) {
    loop.selected = -1;
    if (id) {
      for (let i = 0; i < loop.n; i++) {
        if (loop.ids[i] === id) {
          loop.selected = i;
          break;
        }
      }
    }
    paintPicked();
    if (loop.selected >= 0) loadNeighborhood(id);
    else {
      neighborhood = { id: "", edges: [], neighbor: {}, broader: [], narrower: [], related: [] };
      paintNeighbors();
      uploadGlColors();
    }
    if (doDraw) draw();
  }

  function loadNeighborhood(id) {
    if (!host || !id) return;
    const theso = host.thesaurusId();
    const lang = host.thesaurusLang();
    const params = new URLSearchParams({ thesaurusId: theso, lang: lang, conceptId: id });
    fetch(host.ctx() + "/v2/api/graph-neighborhood?" + params.toString(), {
      headers: { Accept: "application/json" }
    }).then((res) => res.ok ? res.json() : Promise.reject()).then((data) => {
      if (loop.selected < 0 || loop.ids[loop.selected] !== id) return;
      const neighbor = {};
      const edges = [];
      const indexOf = Object.create(null);
      for (let i = 0; i < loop.n; i++) indexOf[loop.ids[i]] = i;
      const from = loop.selected;
      function ingest(list, role) {
        (list || []).forEach((item) => {
          neighbor[item.id] = role;
          const to = indexOf[item.id];
          if (to == null) return;
          edges.push({ a: from, b: to, role: role });
        });
      }
      ingest(data.broader, "TG");
      ingest(data.narrower, "TS");
      ingest(data.related, "TA");
      neighborhood = {
        id: id,
        edges: edges,
        neighbor: neighbor,
        broader: data.broader || [],
        narrower: data.narrower || [],
        related: data.related || []
      };
      paintNeighbors();
      uploadGlColors();
      draw();
    }).catch(() => {});
  }

  function setTip(i, clientX, clientY) {
    const tip = $("#hyperTip");
    const el = canvas();
    if (!tip || !el) return;
    if (i < 0) {
      tip.hidden = true;
      el.classList.remove("is-hover");
      return;
    }
    tip.hidden = false;
    tip.textContent = loop.labels[i] || "";
    const box = el.getBoundingClientRect();
    tip.style.left = (clientX - box.left) + "px";
    tip.style.top = (clientY - box.top) + "px";
    el.classList.add("is-hover");
  }

  function localXY(clientX, clientY) {
    const st = stage() || canvas();
    const box = st.getBoundingClientRect();
    return { x: clientX - box.left, y: clientY - box.top };
  }

  function pick(i) {
    if (i < 0) return;
    loop.selected = i;
    aimAt(i);
    paintPicked();
    const id = loop.ids[i];
    if (!id) return;
    loadNeighborhood(id);
    if (host.onPick) host.onPick(id);
    draw();
  }

  function openNode(i) {
    if (i < 0) i = loop.selected;
    if (i < 0) return;
    const id = loop.ids[i];
    if (!id) return;
    loop.selected = i;
    paintPicked();
    if (host.onOpen) host.onOpen(id);
  }

  function resetView() {
    loop.rotX = 0.22;
    loop.rotY = 0.35;
    loop.velX = 0;
    loop.velY = 0;
    loop.zoom = 1;
    loop.panX = 0;
    loop.panY = 0;
    loop.fly = null;
    draw();
  }

  function applyDrag(e) {
    if (!loop.drag) return;
    const dx = e.clientX - loop.drag.x;
    const dy = e.clientY - loop.drag.y;
    if (Math.hypot(dx, dy) > 2) loop.moved = true;
    if (loop.drag.pan) {
      loop.panX = loop.drag.panX + dx;
      loop.panY = loop.drag.panY + dy;
      clampPan();
      loop.velX = 0;
      loop.velY = 0;
    } else {
      loop.rotY = loop.drag.rotY + dx * 0.01;
      loop.rotX = Math.max(-1.45, Math.min(1.45, loop.drag.rotX + dy * 0.01));
      loop.velY = reducedMotion() ? 0 : dx * 0.00045;
      loop.velX = reducedMotion() ? 0 : dy * 0.00045;
    }
    setTip(-1);
    draw();
  }

  function bindOnce() {
    if (loop.bound) return;
    const el = canvas();
    const st = stage();
    if (!el || !st) return;
    loop.bound = true;
    initGl();
    const onDown = (e) => {
      if (!isHyper()) return;
      if (e.target.closest && e.target.closest(".hyper-bar, #hyperCenter, #hyperOpen, #hyperSearch")) return;
      if (e.pointerType === "mouse" && e.button !== 0 && e.button !== 1 && e.button !== 2) return;
      e.preventDefault();
      loop.fly = null;
      loop.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
      st.classList.add("is-grabbing");
      if (loop.pointers.size >= 2) {
        const pts = Array.from(loop.pointers.values());
        loop.pinch = {
          dist: Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y) || 1,
          zoom: loop.zoom,
          panX: loop.panX,
          panY: loop.panY,
          mx: (pts[0].x + pts[1].x) / 2,
          my: (pts[0].y + pts[1].y) / 2
        };
        loop.drag = null;
        return;
      }
      loop.drag = {
        x: e.clientX, y: e.clientY,
        rotX: loop.rotX, rotY: loop.rotY,
        panX: loop.panX, panY: loop.panY,
        pan: e.shiftKey || e.button === 1 || e.button === 2,
        pointerId: e.pointerId
      };
      loop.moved = false;
      loop.velX = 0;
      loop.velY = 0;
      kick();
    };
    const onMove = (e) => {
      if (!isHyper()) return;
      if (loop.pointers.has(e.pointerId)) loop.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
      if (loop.pinch && loop.pointers.size >= 2) {
        const pts = Array.from(loop.pointers.values());
        const dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y) || 1;
        loop.zoom = Math.max(0.4, Math.min(6, loop.pinch.zoom * (dist / loop.pinch.dist)));
        loop.panX = loop.pinch.panX + ((pts[0].x + pts[1].x) / 2 - loop.pinch.mx);
        loop.panY = loop.pinch.panY + ((pts[0].y + pts[1].y) / 2 - loop.pinch.my);
        clampPan();
        setTip(-1);
        draw();
        return;
      }
      if (loop.drag && (loop.drag.pointerId == null || e.pointerId === loop.drag.pointerId)) {
        applyDrag(e);
        return;
      }
      if (loop.drag || loop.pinch) return;
      const box = st.getBoundingClientRect();
      if (e.clientX < box.left || e.clientX > box.right || e.clientY < box.top || e.clientY > box.bottom) {
        if (loop.hover >= 0) {
          loop.hover = -1;
          setTip(-1);
          draw();
        }
        return;
      }
      if (!loop.hash) project();
      const local = localXY(e.clientX, e.clientY);
      const next = hit(local.x, local.y);
      if (next !== loop.hover) {
        loop.hover = next;
        setTip(next, e.clientX, e.clientY);
        draw();
      } else if (next >= 0) {
        setTip(next, e.clientX, e.clientY);
      }
    };
    const onUp = (e) => {
      if (!loop.drag && !loop.pinch && loop.pointers.size === 0) return;
      loop.pointers.delete(e.pointerId);
      if (loop.pointers.size < 2) loop.pinch = null;
      if (loop.drag && loop.drag.pointerId === e.pointerId && !loop.moved) {
        if (!loop.hash) project();
        const local = localXY(e.clientX, e.clientY);
        const found = hit(local.x, local.y);
        if (found >= 0) pick(found);
        else {
          loop.selected = -1;
          neighborhood = { id: "", edges: [], neighbor: {} };
          paintPicked();
          paintNeighbors();
          uploadGlColors();
          draw();
        }
      }
      if (loop.drag && loop.drag.pointerId === e.pointerId) loop.drag = null;
      if (loop.pointers.size === 0) {
        loop.drag = null;
        loop.pinch = null;
        st.classList.remove("is-grabbing");
        kick();
      }
    };
    st.addEventListener("pointerdown", onDown);
    window.addEventListener("pointermove", onMove, { passive: true });
    window.addEventListener("pointerup", onUp);
    window.addEventListener("pointercancel", (e) => {
      loop.pointers.delete(e.pointerId);
      if (e.pointerType === "touch") {
        loop.pinch = null;
        if (loop.drag && loop.drag.pointerId === e.pointerId) loop.drag = null;
        if (loop.pointers.size === 0) st.classList.remove("is-grabbing");
      }
    });
    st.addEventListener("dblclick", (e) => {
      if (!loop.hash) project();
      const local = localXY(e.clientX, e.clientY);
      const found = hit(local.x, local.y);
      openNode(found >= 0 ? found : loop.selected);
    });
    st.addEventListener("contextmenu", (e) => e.preventDefault());
    st.addEventListener("wheel", (e) => {
      e.preventDefault();
      loop.fly = null;
      if (e.ctrlKey || e.metaKey) {
        const prev = loop.zoom;
        const next = Math.max(0.4, Math.min(6, prev * Math.exp(-e.deltaY * 0.01)));
        const local = localXY(e.clientX, e.clientY);
        const k = next / prev;
        loop.panX += (local.x - loop.w * 0.5 - loop.panX) * (1 - k);
        loop.panY += (local.y - loop.h * 0.5 - loop.panY) * (1 - k);
        loop.zoom = next;
        clampPan();
        draw();
        return;
      }
      if (e.shiftKey) {
        loop.panX -= e.deltaX || e.deltaY;
        loop.panY -= e.deltaY;
        clampPan();
        draw();
        return;
      }
      if (e.deltaMode === 0) {
        loop.rotY += e.deltaX * 0.005;
        loop.rotX = Math.max(-1.45, Math.min(1.45, loop.rotX + e.deltaY * 0.005));
        if (!reducedMotion()) {
          loop.velY = e.deltaX * 0.00014;
          loop.velX = e.deltaY * 0.00014;
          kick();
        }
        draw();
        return;
      }
      const prev = loop.zoom;
      const next = Math.max(0.4, Math.min(6, prev * (e.deltaY > 0 ? 0.9 : 1.12)));
      const local = localXY(e.clientX, e.clientY);
      const k = next / prev;
      loop.panX += (local.x - loop.w * 0.5 - loop.panX) * (1 - k);
      loop.panY += (local.y - loop.h * 0.5 - loop.panY) * (1 - k);
      loop.zoom = next;
      clampPan();
      draw();
    }, { passive: false });
    el.addEventListener("keydown", (e) => {
      const step = e.shiftKey ? 0.16 : 0.08;
      if (e.key === "ArrowLeft") { loop.rotY -= step; e.preventDefault(); }
      else if (e.key === "ArrowRight") { loop.rotY += step; e.preventDefault(); }
      else if (e.key === "ArrowUp") { loop.rotX = Math.max(-1.45, loop.rotX - step); e.preventDefault(); }
      else if (e.key === "ArrowDown") { loop.rotX = Math.min(1.45, loop.rotX + step); e.preventDefault(); }
      else if (e.key === "+" || e.key === "=") { loop.zoom = Math.min(6, loop.zoom * 1.12); e.preventDefault(); }
      else if (e.key === "-") { loop.zoom = Math.max(0.4, loop.zoom * 0.9); e.preventDefault(); }
      else if (e.key === "0") { resetView(); e.preventDefault(); return; }
      else if (e.key === "Enter") { openNode(loop.selected); e.preventDefault(); return; }
      else if (e.key === "Escape") {
        if (loop.selected >= 0) {
          loop.selected = -1;
          neighborhood = { id: "", edges: [], neighbor: {} };
          paintPicked();
          paintNeighbors();
          uploadGlColors();
        } else resetView();
        e.preventDefault();
      } else return;
      loop.fly = null;
      loop.velX = 0;
      loop.velY = 0;
      draw();
    });
    const center = $("#hyperCenter");
    if (center) center.addEventListener("click", resetView);
    const openBtn = $("#hyperOpen");
    if (openBtn) openBtn.addEventListener("click", () => openNode(loop.selected));
    const search = $("#hyperSearch");
    if (search) {
      search.addEventListener("input", () => {
        matchQuery = (search.value || "").trim().toLowerCase();
        uploadGlColors();
        if (!matchQuery) {
          draw();
          return;
        }
        for (let i = 0; i < loop.n; i++) {
          if ((loop.labels[i] || "").toLowerCase().indexOf(matchQuery) >= 0) {
            loop.hover = i;
            break;
          }
        }
        draw();
      });
      search.addEventListener("keydown", (e) => {
        if (e.key !== "Enter") return;
        e.preventDefault();
        const q = (search.value || "").trim().toLowerCase();
        if (!q) return;
        for (let i = 0; i < loop.n; i++) {
          if ((loop.labels[i] || "").toLowerCase().indexOf(q) >= 0) {
            pick(i);
            return;
          }
        }
      });
    }
    document.addEventListener("visibilitychange", () => {
      if (document.hidden) stop();
      else if (isHyper()) draw();
    });
    if (window.ResizeObserver) {
      new ResizeObserver(() => {
        if (isHyper()) {
          resize();
          draw();
        }
      }).observe(st);
    }
  }

  function load() {
    const el = canvas();
    if (!el || !host) return;
    const theso = host.thesaurusId();
    const lang = host.thesaurusLang();
    const key = theso + "|" + lang;
    const empty = $("#hyperEmpty");
    if (!theso) {
      cache = { key: key, loading: false, nodes: [], truncated: false };
      setLoading(false);
      if (empty) empty.hidden = false;
      paintCount();
      return;
    }
    cache.loading = true;
    cache.key = "";
    setLoading(true);
    if (empty) empty.hidden = true;
    const params = new URLSearchParams({ thesaurusId: theso, lang: lang });
    fetch(host.ctx() + "/v2/api/graph-globe?" + params.toString(), {
      headers: { Accept: "application/json" }
    }).then((res) => {
      if (!res.ok) throw new Error("http");
      return res.json();
    }).then((data) => {
      if (cacheKey() !== key) return;
      const nodes = data && Array.isArray(data.nodes) ? data.nodes : [];
      cache = { key: key, loading: false, nodes: nodes, truncated: !!(data && data.truncated) };
      layout(nodes);
      setLoading(false);
      if (empty) empty.hidden = nodes.length === 0;
      paintCount();
      resize();
      draw();
    }).catch(() => {
      if (cacheKey() !== key) return;
      cache = { key: "", loading: false, nodes: [], truncated: false };
      setLoading(false);
      if (empty) {
        empty.hidden = false;
        empty.textContent = "Impossible de charger le globe.";
      }
      if (host.toast) host.toast("Impossible de charger le globe.");
    });
  }

  function ensure() {
    if (!canvas()) return;
    bindOnce();
    resize();
    draw();
    const key = cacheKey();
    if (cache.key === key || cache.loading) {
      if (cache.key === key) draw();
      return;
    }
    load();
  }

  function invalidate() {
    cache = { key: "", loading: false, nodes: [], truncated: false };
    loop.n = 0;
    paintCount();
  }

  global.OTGlobe = {
    attach: function (nextHost) {
      host = nextHost;
      return global.OTGlobe;
    },
    ensure: ensure,
    stop: stop,
    invalidate: invalidate,
    selectId: selectId,
    reload: load
  };
})(window);
