/**
 * Sphère de voisinage : un concept au centre, ses génériques (pôle haut), spécifiques (pôle bas)
 * et associés (côtés). Le thésaurus n'est jamais chargé en entier.
 * API : window.OTGlobe.attach(host).ensure() / stop() / invalidate() / selectId() / reload()
 */
(function (global) {
  "use strict";

  const ROLES = ["TG", "TS", "TA", "FA"];
  const COLOR = { TG: "#1f7a5c", TS: "#2f6cb0", TA: "#a06e24", FA: "#b3392f", center: "#17382c" };
  /* Icône de facette de l'arbre (includes/tree-nodes.xhtml), dessinée dans un carré de 24. */
  const FACET_ICON = ["m12 2 9 5-9 5-9-5 9-5Z", "m3 12 9 5 9-5", "m3 17 9 5 9-5"];
  const FACET_PATHS = FACET_ICON.map((d) => new Path2D(d));
  const FOCAL = 2.35;
  const GOLDEN = Math.PI * (3 - Math.sqrt(5));
  const LABEL_MAX = 30;

  let host = null;
  let data = null;
  let wanted = { id: "", kind: "", key: "", offsets: { TG: 0, TS: 0, TA: 0, FA: 0 } };
  let request = null;
  let refocus = "";
  const loop = {
    rotX: 0.1, rotY: 0, zoom: 1, panX: 0, panY: 0,
    shown: false, hover: -1, drag: null, pinch: null, moved: false, pointers: new Map(),
    nodes: [], w: 0, h: 0, dpr: 1
  };

  function el(id) {
    return document.getElementById(id);
  }

  function msg(name) {
    const root = el("hyperRoot");
    return (root && root.getAttribute("data-" + name)) || "";
  }

  function fill(pattern, values) {
    return pattern.replace(/\{(\d)\}/g, (all, i) => (values[i] == null ? all : String(values[i])));
  }

  function isHyper() {
    return !!(host && host.isHyper());
  }

  function groups() {
    if (!data || !data.found) return { TG: null, TS: null, TA: null, FA: null };
    return { TG: data.broader, TS: data.narrower, TA: data.related, FA: data.facets };
  }

  /* ── Données ── */

  function keyOf(id, kind) {
    return host.thesaurusId() + "|" + host.thesaurusLang() + "|" + (kind || "concept") + "|" + (id || "");
  }

  function setLoading(on) {
    const spin = el("hyperLoading");
    if (spin) spin.hidden = !on;
    if (on) setEmpty("");
  }

  function setEmpty(text) {
    const box = el("hyperEmpty");
    if (!box) return;
    box.textContent = text;
    box.hidden = !text;
  }

  function clear(text) {
    if (request) request.abort();
    request = null;
    data = null;
    loop.nodes = [];
    loop.hover = -1;
    setLoading(false);
    setEmpty(text);
    paintPanels();
    draw();
  }

  /* Recentre sur un concept. Sans changement de concept ni de langue, ne relance rien. */
  function selectId(id, kind) {
    if (!host || !el("hyperGlobe")) return;
    if (!id || !host.thesaurusId()) {
      wanted = { id: "", kind: "", key: "", offsets: { TG: 0, TS: 0, TA: 0, FA: 0 } };
      clear(msg("pick"));
      return;
    }
    const key = keyOf(id, kind);
    if (wanted.key === key && (data || request)) return;
    wanted = { id: id, kind: kind || "concept", key: key, offsets: { TG: 0, TS: 0, TA: 0, FA: 0 } };
    fetchNeighborhood(true);
  }

  function fetchNeighborhood(recenter) {
    const key = wanted.key;
    if (request) request.abort();
    const ctl = new AbortController();
    request = ctl;
    const spinner = setTimeout(() => {
      if (request === ctl) setLoading(true);
    }, 150);
    const query = new URLSearchParams({
      thesaurusId: host.thesaurusId(),
      lang: host.thesaurusLang(),
      conceptId: wanted.id,
      kind: wanted.kind,
      facetOffset: wanted.offsets.FA,
      broaderOffset: wanted.offsets.TG,
      narrowerOffset: wanted.offsets.TS,
      relatedOffset: wanted.offsets.TA
    });
    fetch(host.ctx() + "/api/graph-neighborhood?" + query.toString(), {
      headers: { Accept: "application/json" },
      credentials: "same-origin",
      signal: ctl.signal
    }).then((res) => {
      if (!res.ok) throw new Error("http");
      return res.json();
    }).then((body) => {
      if (request !== ctl || wanted.key !== key) return;
      request = null;
      setLoading(false);
      data = body;
      if (!body.found) {
        data = null;
        wanted.key = "";
        clear(msg("hidden"));
        return;
      }
      /* Voisins supprimés entre-temps : la page demandée n'existe plus. */
      const stale = ROLES.filter((role) => wanted.offsets[role] > 0 && !groups()[role].items.length);
      if (stale.length) {
        stale.forEach((role) => { wanted.offsets[role] = 0; });
        fetchNeighborhood(recenter);
        return;
      }
      setEmpty("");
      if (recenter) resetView();
      layout();
      paintPanels();
      resize();
      draw();
    }).catch((err) => {
      if (err && err.name === "AbortError") return;
      if (request !== ctl) return;
      wanted.key = "";
      clear(msg("error"));
      if (host.toast) host.toast(msg("error"));
    }).finally(() => clearTimeout(spinner));
  }

  function turnPage(role, step) {
    const group = groups()[role];
    if (!group) return;
    const next = group.offset + step * data.pageSize;
    if (next < 0 || next >= group.total) return;
    wanted.offsets[role] = next;
    refocus = role + (step > 0 ? ":next" : ":prev");
    fetchNeighborhood(false);
  }

  /* ── Disposition sur la sphère unité (y vers le bas) ── */

  function point(y, lon) {
    const r = Math.sqrt(Math.max(0, 1 - y * y));
    return { x: Math.sin(lon) * r, y: y, z: Math.cos(lon) * r };
  }

  /* Peu de voisins : éventail face au lecteur. Beaucoup : spirale sur la calotte. */
  function cap(n, i, sign) {
    if (n <= 7) {
      const t = n === 1 ? 0 : (i / (n - 1)) * 2 - 1;
      return point(sign * (0.86 - Math.abs(t) * 0.3), t * 1.25);
    }
    const t = (i + 0.5) / n;
    return point(sign * (0.36 + 0.6 * t), i * GOLDEN);
  }

  function side(n, i) {
    const dir = i % 2 === 0 ? 1 : -1;
    const rows = Math.ceil(n / 2);
    const row = Math.floor(i / 2);
    const t = rows === 1 ? 0 : (row / (rows - 1)) * 2 - 1;
    const depth = rows <= 4 ? 0 : ((row % 3) - 1) * 0.5;
    return point(t * 0.26, dir * (1.35 + depth));
  }

  /* Facettes : arc entre le centre et les spécifiques. */
  function facetPoint(n, i) {
    if (n === 1) return point(0.34, 0.4);
    if (n <= 5) return point(0.34, ((i / (n - 1)) * 2 - 1) * 0.75);
    return point(0.22 + 0.12 * (i % 2), (i / n) * Math.PI * 2);
  }

  function layout() {
    const nodes = [{
      id: data.id, label: data.label, role: "center", kind: data.kind, status: data.status, x: 0, y: 0, z: 0
    }];
    const g = groups();
    ROLES.forEach((role) => {
      const items = g[role].items;
      items.forEach((item, i) => {
        const p = role === "TG" ? cap(items.length, i, -1)
          : role === "TS" ? cap(items.length, i, 1)
          : role === "FA" ? facetPoint(items.length, i)
          : side(items.length, i);
        nodes.push({
          id: item.id, label: item.label, role: role, status: item.status, type: item.type,
          x: p.x, y: p.y, z: p.z
        });
      });
    });
    loop.nodes = nodes;
    loop.hover = -1;
  }

  /* ── Dessin ── */

  function resize() {
    const cv = el("hyperGlobe");
    const st = el("hyperStage");
    if (!cv || !st) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const w = Math.max(1, st.clientWidth);
    const h = Math.max(1, st.clientHeight);
    if (cv.width !== Math.round(w * dpr) || cv.height !== Math.round(h * dpr)) {
      cv.width = Math.round(w * dpr);
      cv.height = Math.round(h * dpr);
      cv.style.width = w + "px";
      cv.style.height = h + "px";
    }
    loop.w = w;
    loop.h = h;
    loop.dpr = dpr;
  }

  function radius() {
    return Math.min(loop.w, loop.h) * 0.4 * loop.zoom;
  }

  function project() {
    const cosY = Math.cos(loop.rotY);
    const sinY = Math.sin(loop.rotY);
    const cosX = Math.cos(loop.rotX);
    const sinX = Math.sin(loop.rotX);
    const cx = loop.w / 2 + loop.panX;
    const cy = loop.h / 2 + loop.panY;
    const r = radius();
    loop.nodes.forEach((n) => {
      const x1 = n.x * cosY + n.z * sinY;
      const z1 = n.z * cosY - n.x * sinY;
      const y2 = n.y * cosX + z1 * sinX;
      const z2 = z1 * cosX - n.y * sinX;
      const scale = r * (FOCAL / (FOCAL + 0.35 - z2 * 0.6));
      n.sx = cx + x1 * scale;
      n.sy = cy + y2 * scale;
      n.sz = z2;
    });
  }

  function shape(ctx, n, size) {
    const x = n.sx;
    const y = n.sy;
    ctx.beginPath();
    if (n.role === "TG") {
      ctx.moveTo(x, y - size * 1.1);
      ctx.lineTo(x + size, y + size * 0.8);
      ctx.lineTo(x - size, y + size * 0.8);
    } else if (n.role === "TS") {
      ctx.moveTo(x, y + size * 1.1);
      ctx.lineTo(x + size, y - size * 0.8);
      ctx.lineTo(x - size, y - size * 0.8);
    } else if (n.role === "TA") {
      ctx.moveTo(x, y - size * 1.1);
      ctx.lineTo(x + size * 1.1, y);
      ctx.lineTo(x, y + size * 1.1);
      ctx.lineTo(x - size * 1.1, y);
    } else {
      ctx.arc(x, y, size, 0, Math.PI * 2);
    }
    ctx.closePath();
  }

  function drawSphere(ctx) {
    const cx = loop.w / 2 + loop.panX;
    const cy = loop.h / 2 + loop.panY;
    const r = radius();
    const disc = ctx.createRadialGradient(cx - r * 0.25, cy - r * 0.3, r * 0.1, cx, cy, r);
    disc.addColorStop(0, "rgba(31,122,92,0.10)");
    disc.addColorStop(0.72, "rgba(31,122,92,0.04)");
    disc.addColorStop(1, "rgba(31,122,92,0.02)");
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fillStyle = disc;
    ctx.fill();
    ctx.strokeStyle = "rgba(31,122,92,0.22)";
    ctx.lineWidth = 1.25;
    ctx.stroke();
  }

  function fade(n) {
    return n.role === "center" ? 1 : 0.4 + 0.6 * Math.min(1, Math.max(0, (n.sz + 0.8) / 1.2));
  }

  function isFacet(n) {
    return n.role === "FA" || n.kind === "facet";
  }

  function nodeColor(n) {
    return isFacet(n) ? COLOR.FA : COLOR[n.role];
  }

  /* Pastille claire sous l'icône : le lien ne la traverse pas. */
  function drawFacet(ctx, n, size, active) {
    const side = size * 2.5;
    ctx.save();
    ctx.beginPath();
    ctx.arc(n.sx, n.sy, side * 0.62, 0, Math.PI * 2);
    ctx.fillStyle = "#ffffff";
    ctx.fill();
    ctx.translate(n.sx - side / 2, n.sy - side / 2);
    ctx.scale(side / 24, side / 24);
    ctx.strokeStyle = COLOR.FA;
    ctx.lineWidth = active ? 2.6 : 2;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    FACET_PATHS.forEach((path) => ctx.stroke(path));
    ctx.restore();
  }

  function drawNode(ctx, n, active) {
    const size = n.role === "center" ? 13 : (n.sz > 0 ? 8 : 6.5) + (active ? 2 : 0);
    ctx.globalAlpha = active ? 1 : fade(n);
    if (isFacet(n)) {
      drawFacet(ctx, n, size, active);
      ctx.globalAlpha = 1;
      return;
    }
    shape(ctx, n, size);
    const ghost = n.status === "candidat" || n.status === "deprecie";
    ctx.fillStyle = ghost ? "#ffffff" : COLOR[n.role];
    ctx.fill();
    ctx.strokeStyle = COLOR[n.role];
    ctx.lineWidth = active ? 3 : 2;
    ctx.setLineDash(n.status === "candidat" ? [3, 2.5] : []);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.globalAlpha = 1;
  }

  function shorten(label, full) {
    return full || label.length <= LABEL_MAX ? label : label.slice(0, LABEL_MAX - 1).trim() + "…";
  }

  /* Étiquettes : centre et survol toujours, puis de l'avant vers l'arrière sans recouvrement. */
  function drawLabels(ctx) {
    const boxes = loop.nodes.map((n) => ({
      x: n.sx - 9, y: n.sy - 9, w: 18, h: 18, owner: n
    }));
    const order = loop.nodes.slice().sort((a, b) => rank(b) - rank(a));
    order.forEach((n) => {
      const strong = n.role === "center" || loop.nodes[loop.hover] === n;
      if (!strong && n.sz < -0.35 && loop.nodes.length > 12) return;
      const text = shorten(n.label, strong);
      const size = n.role === "center" ? 15 : 12.5;
      ctx.font = (strong ? "700 " : "600 ") + size + "px 'IBM Plex Sans', sans-serif";
      const w = ctx.measureText(text).width + 6;
      const h = size + 5;
      const gap = n.role === "center" ? 18 : 13;
      const above = { x: n.sx - w / 2, y: n.sy - gap - h };
      const below = { x: n.sx - w / 2, y: n.sy + gap };
      const right = { x: n.sx + gap, y: n.sy - h / 2 };
      const left = { x: n.sx - gap - w, y: n.sy - h / 2 };
      const spots = n.role === "TG" ? [above, right, left, below]
        : n.role === "TA" ? (n.sx >= loop.w / 2 + loop.panX ? [right, below, above, left] : [left, below, above, right])
        : [below, right, left, above];
      const inside = n.sx >= 0 && n.sx <= loop.w && n.sy >= 0 && n.sy <= loop.h;
      const tries = inside ? spots.concat(spots.map((s) => clamp(s, w, h))) : spots;
      let spot = tries.find((s) => free(boxes, s, w, h, n));
      if (!spot && strong) spot = clamp(spots[0], w, h);
      if (!spot) return;
      boxes.push({ x: spot.x, y: spot.y, w: w, h: h, owner: null });
      ctx.globalAlpha = strong ? 1 : Math.max(0.72, fade(n));
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.lineWidth = 4;
      ctx.lineJoin = "round";
      ctx.strokeStyle = "rgba(255,255,255,0.94)";
      ctx.fillStyle = nodeColor(n);
      const tx = spot.x + w / 2;
      const ty = spot.y + h / 2;
      ctx.strokeText(text, tx, ty);
      ctx.fillText(text, tx, ty);
      ctx.globalAlpha = 1;
    });
  }

  function rank(n) {
    if (loop.nodes[loop.hover] === n) return 10;
    return n.role === "center" ? 9 : n.sz;
  }

  function clamp(spot, w, h) {
    return {
      x: Math.max(2, Math.min(loop.w - w - 2, spot.x)),
      y: Math.max(2, Math.min(loop.h - h - 2, spot.y))
    };
  }

  function free(boxes, s, w, h, owner) {
    if (s.x < 0 || s.y < 0 || s.x + w > loop.w || s.y + h > loop.h) return false;
    return !boxes.some((b) => b.owner !== owner
      && s.x < b.x + b.w && s.x + w > b.x && s.y < b.y + b.h && s.y + h > b.y);
  }

  function draw() {
    const cv = el("hyperGlobe");
    const ctx = cv && isHyper() ? cv.getContext("2d") : null;
    if (!ctx || !loop.w) return;
    ctx.setTransform(loop.dpr, 0, 0, loop.dpr, 0, 0);
    ctx.clearRect(0, 0, loop.w, loop.h);
    if (!loop.nodes.length) return;
    project();
    drawSphere(ctx);
    const center = loop.nodes[0];
    loop.nodes.forEach((n, i) => {
      if (!i) return;
      const active = i === loop.hover;
      ctx.globalAlpha = active ? 1 : fade(n) * 0.6;
      ctx.strokeStyle = COLOR[n.role];
      ctx.lineWidth = active ? 2.4 : 1.4;
      ctx.setLineDash(n.role === "TA" ? [5, 4] : []);
      ctx.beginPath();
      ctx.moveTo(center.sx, center.sy);
      ctx.lineTo(n.sx, n.sy);
      ctx.stroke();
    });
    ctx.setLineDash([]);
    ctx.globalAlpha = 1;
    loop.nodes.slice().sort((a, b) => a.sz - b.sz)
      .forEach((n) => drawNode(ctx, n, loop.nodes[loop.hover] === n));
    drawLabels(ctx);
  }

  /* ── Panneau latéral : légende, liste au clavier, pages ── */

  function node(tag, cls, text) {
    const out = document.createElement(tag);
    if (cls) out.className = cls;
    if (text != null) out.textContent = text;
    return out;
  }

  function badge(status) {
    if (status !== "candidat" && status !== "deprecie") return null;
    return node("span", "hyper-badge is-" + status, msg(status === "candidat" ? "candidate" : "deprecated"));
  }

  function paintPanels() {
    const picked = el("hyperPicked");
    const open = el("hyperOpen");
    const status = el("hyperStatus");
    const on = !!(data && data.found);
    if (picked) picked.textContent = on ? data.label : "";
    if (open) open.disabled = !on;
    if (status) {
      const chip = on ? badge(data.status) : null;
      status.hidden = !chip;
      status.className = chip ? chip.className : "hyper-badge";
      status.textContent = chip ? chip.textContent : "";
    }
    paintCap(on);
    paintList(on);
  }

  function paintCap(on) {
    const box = el("hyperCap");
    if (!box) return;
    const g = groups();
    const over = on ? ROLES.filter((role) => g[role].total > data.pageSize) : [];
    box.hidden = !over.length;
    box.textContent = over.length ? fill(msg("cap"), [data.pageSize]) : "";
  }

  function paintList(on) {
    const list = el("hyperNeighbors");
    if (!list) return;
    const inside = list.contains(document.activeElement);
    list.textContent = "";
    list.hidden = !on;
    if (!on) return;
    const g = groups();
    ROLES.forEach((role) => {
      const group = g[role];
      const code = role.toLowerCase();
      const facet = data.kind === "facet";
      if (facet && (role === "TA" || role === "FA")) return;
      if (role === "FA" && !group.total) return;
      const section = node("section", "hyper-sec hyper-sec-" + role);
      const head = node("h3", "hyper-sec-h");
      const mark = role === "FA" ? facetMark() : node("span", "hyper-mark");
      mark.setAttribute("aria-hidden", "true");
      head.append(mark);
      if (!facet && role !== "FA") head.append(node("span", "hyper-code", msg("code-" + code)));
      head.append(node("span", "hyper-sec-l", sectionLabel(role)),
        node("span", "hyper-sec-n", String(group.total)));
      section.append(head);
      if (group.items.length) {
        const ul = node("ul", "hyper-items");
        group.items.forEach((item) => ul.append(listItem(item, role)));
        section.append(ul);
      }
      if (group.total > data.pageSize) section.append(pager(group, role));
      list.append(section);
    });
    if (!g.TG.total && !g.TS.total && !g.TA.total && !g.FA.total) {
      list.prepend(node("p", "hyper-none", msg("none")));
    }
    restoreFocus(list, inside);
  }

  function facetMark() {
    const ns = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(ns, "svg");
    svg.setAttribute("viewBox", "0 0 24 24");
    FACET_ICON.forEach((d) => {
      const path = document.createElementNS(ns, "path");
      path.setAttribute("d", d);
      svg.append(path);
    });
    const box = node("span", "tn-facet-ico");
    box.append(svg);
    return box;
  }

  function sectionLabel(role) {
    if (data.kind === "facet") return msg(role === "TG" ? "label-parent" : "label-members");
    return msg("label-" + role.toLowerCase());
  }

  function listItem(item, role) {
    const li = node("li");
    const btn = node("button", "hyper-item");
    btn.type = "button";
    btn.dataset.id = item.id;
    btn.title = item.label;
    const label = node("bdi", "hyper-item-l", item.label);
    btn.append(label);
    const typed = role !== "FA" && data.kind !== "facet";
    const kind = typed && (item.type || "").length > 2 ? item.type.slice(2) : "";
    if (item.type === "FACETOF") btn.append(node("span", "hyper-kind", msg("facetof")));
    if (kind) btn.append(node("span", "hyper-kind", msg("code-" + role.toLowerCase()) + kind));
    const chip = badge(item.status);
    if (chip) btn.append(chip);
    btn.addEventListener("click", () => pickId(item.id, item.label, role));
    const light = (on) => () => {
      loop.hover = on ? loop.nodes.findIndex((n) => n.id === item.id && n.role === role) : -1;
      draw();
    };
    btn.addEventListener("mouseenter", light(true));
    btn.addEventListener("focus", light(true));
    btn.addEventListener("mouseleave", light(false));
    btn.addEventListener("blur", light(false));
    li.append(btn);
    return li;
  }

  function pager(group, role) {
    const bar = node("div", "hyper-pager");
    const first = group.offset + 1;
    const last = group.offset + group.items.length;
    const make = (step, name, sign, off) => {
      const btn = node("button", "hyper-page", sign);
      btn.type = "button";
      btn.disabled = off;
      btn.dataset.page = role + ":" + name;
      btn.title = msg(name);
      btn.setAttribute("aria-label", msg(name) + " — " + sectionLabel(role));
      btn.addEventListener("click", () => turnPage(role, step));
      return btn;
    };
    bar.append(make(-1, "prev", "‹", group.offset === 0),
      node("span", "hyper-range", fill(msg("range"), [first, last, group.total])),
      make(1, "next", "›", last >= group.total));
    return bar;
  }

  function restoreFocus(list, inside) {
    const want = refocus;
    refocus = "";
    if (!want && !inside) return;
    const btn = want && want !== "center" ? list.querySelector('[data-page="' + want + '"]') : null;
    const other = want && want !== "center"
      ? list.querySelector('[data-page^="' + want.split(":")[0] + '"]:not(:disabled)') : null;
    const target = btn && !btn.disabled ? btn : other || el("hyperPicked");
    if (target) target.focus({ preventScroll: true });
  }

  /* ── Choix et ouverture ── */

  function pickId(id, label, role) {
    if (!id || !data || id === data.id) return;
    if (host.onPick) host.onPick(id, label, role === "FA" ? "facet" : "concept");
  }

  function pick(i) {
    const n = loop.nodes[i];
    if (n && i > 0) pickId(n.id, n.label, n.role);
  }

  function openCenter() {
    if (data && data.found && host.onOpen) host.onOpen(data.id, data.kind);
  }

  function hit(x, y) {
    let best = -1;
    let bestD = 16 * 16;
    let bestZ = -2;
    loop.nodes.forEach((n, i) => {
      const d = (n.sx - x) * (n.sx - x) + (n.sy - y) * (n.sy - y);
      if (d < bestD || (d < 100 && n.sz > bestZ && d <= bestD + 40)) {
        best = i;
        bestD = d;
        bestZ = n.sz;
      }
    });
    return best;
  }

  /* ── Cycle de vie ── */

  function stop() {
    loop.shown = false;
    loop.hover = -1;
  }

  function resetView() {
    loop.rotX = 0.1;
    loop.rotY = 0;
    loop.zoom = 1;
    loop.panX = 0;
    loop.panY = 0;
  }

  function ensure() {
    if (!el("hyperGlobe") || !host) return;
    window.OTGlobeBind({
      loop: loop, isHyper: isHyper, draw: draw, stop: stop, hit: hit, pick: pick,
      openCenter: openCenter, resize: resize,
      resetView: () => { resetView(); draw(); }
    });
    resize();
    const id = host.conceptId();
    const same = id && wanted.key === keyOf(id, host.conceptKind()) && data;
    const back = !loop.shown;
    loop.shown = true;
    if (same && !request) {
      /* Retour au graphe : les relations ont pu être modifiées dans la fiche. */
      if (el("hyperPicked").textContent !== data.label || el("hyperNeighbors").hidden) paintPanels();
      draw();
      if (back) fetchNeighborhood(false);
      return;
    }
    selectId(id, host.conceptKind());
    draw();
  }

  function invalidate() {
    wanted.key = "";
    if (request) request.abort();
    request = null;
    data = null;
    loop.nodes = [];
  }

  function reload() {
    invalidate();
    ensure();
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
    reload: reload
  };
})(window);
