/** Globe — pointeur, clavier et recherche sur la scène. */
window.OTGlobeBind = function (shared, loop, canvas, stage, isHyper, initGl, kick, clampPan, draw, hit, localXY, setTip, pick, openNode, resetView, applyDrag, reducedMotion, stop, paintNeighbors, paintPicked, project, resize, uploadGlColors, $) {
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
          shared.neighborhood = { id: "", edges: [], neighbor: {} };
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
          shared.neighborhood = { id: "", edges: [], neighbor: {} };
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
        shared.matchQuery = (search.value || "").trim().toLowerCase();
        uploadGlColors();
        if (!shared.matchQuery) {
          draw();
          return;
        }
        for (let i = 0; i < loop.n; i++) {
          if ((loop.labels[i] || "").toLowerCase().indexOf(shared.matchQuery) >= 0) {
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
;
