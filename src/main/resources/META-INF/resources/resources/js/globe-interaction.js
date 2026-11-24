/** Sphère de voisinage — pointeur, molette et clavier. */
window.OTGlobeBind = function (api) {
  const loop = api.loop;
  const canvas = document.getElementById("hyperGlobe");
  const stage = document.getElementById("hyperStage");
  /* Le changement de langue réécrit la page : on rebranche la nouvelle scène. */
  if (!canvas || !stage || loop.stage === stage) return;
  const first = !loop.stage;
  loop.stage = stage;
  loop.pointers.clear();
  loop.drag = null;
  loop.pinch = null;

  const ROT_MAX = 1.3;
  const tilt = (value) => Math.max(-ROT_MAX, Math.min(ROT_MAX, value));
  const bound = (value) => Math.max(0.5, Math.min(4, value));

  function local(e) {
    const box = stage.getBoundingClientRect();
    return { x: e.clientX - box.left, y: e.clientY - box.top };
  }

  function clampPan() {
    const max = Math.min(loop.w || 1, loop.h || 1) * 0.5 * Math.max(1, loop.zoom);
    loop.panX = Math.max(-max, Math.min(max, loop.panX));
    loop.panY = Math.max(-max, Math.min(max, loop.panY));
  }

  function zoomAt(e, factor) {
    const p = local(e);
    const next = bound(loop.zoom * factor);
    const k = next / loop.zoom;
    loop.panX += (p.x - loop.w / 2 - loop.panX) * (1 - k);
    loop.panY += (p.y - loop.h / 2 - loop.panY) * (1 - k);
    loop.zoom = next;
    clampPan();
    api.draw();
  }

  function setHover(i) {
    if (loop.hover === i) return;
    loop.hover = i;
    canvas.classList.toggle("is-hover", i > 0);
    api.draw();
  }

  stage.addEventListener("pointerdown", (e) => {
    if (e.pointerType === "mouse" && e.button > 2) return;
    e.preventDefault();
    canvas.focus({ preventScroll: true });
    if (stage.setPointerCapture) stage.setPointerCapture(e.pointerId);
    loop.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    stage.classList.add("is-grabbing");
    if (loop.pointers.size === 2) {
      const p = Array.from(loop.pointers.values());
      loop.pinch = { dist: Math.hypot(p[0].x - p[1].x, p[0].y - p[1].y) || 1, zoom: loop.zoom };
      loop.drag = null;
      return;
    }
    loop.moved = false;
    loop.drag = {
      id: e.pointerId, x: e.clientX, y: e.clientY,
      rotX: loop.rotX, rotY: loop.rotY, panX: loop.panX, panY: loop.panY,
      pan: e.shiftKey || e.button === 1 || e.button === 2
    };
  });

  stage.addEventListener("pointermove", (e) => {
    if (loop.pointers.has(e.pointerId)) loop.pointers.set(e.pointerId, { x: e.clientX, y: e.clientY });
    if (loop.pinch) {
      const p = Array.from(loop.pointers.values());
      if (p.length < 2) return;
      loop.zoom = bound(loop.pinch.zoom * Math.hypot(p[0].x - p[1].x, p[0].y - p[1].y) / loop.pinch.dist);
      api.draw();
      return;
    }
    if (loop.drag) {
      if (e.pointerId !== loop.drag.id) return;
      const dx = e.clientX - loop.drag.x;
      const dy = e.clientY - loop.drag.y;
      if (Math.hypot(dx, dy) > 4) loop.moved = true;
      if (!loop.moved) return;
      if (loop.drag.pan) {
        loop.panX = loop.drag.panX + dx;
        loop.panY = loop.drag.panY + dy;
        clampPan();
      } else {
        loop.rotY = loop.drag.rotY + dx * 0.008;
        loop.rotX = tilt(loop.drag.rotX + dy * 0.008);
      }
      loop.hover = -1;
      api.draw();
      return;
    }
    const p = local(e);
    setHover(api.hit(p.x, p.y));
  });

  function release(e, pick) {
    const drag = loop.drag;
    loop.pointers.delete(e.pointerId);
    if (loop.pointers.size < 2) loop.pinch = null;
    if (drag && drag.id === e.pointerId) {
      loop.drag = null;
      if (pick && !loop.moved && !drag.pan) {
        const p = local(e);
        api.pick(api.hit(p.x, p.y));
      }
    }
    if (!loop.pointers.size) stage.classList.remove("is-grabbing");
  }

  stage.addEventListener("pointerup", (e) => release(e, true));
  stage.addEventListener("pointercancel", (e) => release(e, false));
  stage.addEventListener("pointerleave", () => { if (!loop.drag) setHover(-1); });
  stage.addEventListener("contextmenu", (e) => e.preventDefault());
  stage.addEventListener("dblclick", (e) => {
    const p = local(e);
    if (api.hit(p.x, p.y) === 0) api.openCenter();
  });
  stage.addEventListener("wheel", (e) => {
    e.preventDefault();
    zoomAt(e, Math.exp(-Math.max(-60, Math.min(60, e.deltaY)) * (e.ctrlKey ? 0.01 : 0.003)));
  }, { passive: false });

  canvas.addEventListener("keydown", (e) => {
    const step = e.shiftKey ? 0.24 : 0.12;
    if (e.ctrlKey || e.metaKey || e.altKey) return;
    if (e.key === "ArrowLeft") loop.rotY -= step;
    else if (e.key === "ArrowRight") loop.rotY += step;
    else if (e.key === "ArrowUp") loop.rotX = tilt(loop.rotX - step);
    else if (e.key === "ArrowDown") loop.rotX = tilt(loop.rotX + step);
    else if (e.key === "+" || e.key === "=") loop.zoom = bound(loop.zoom * 1.12);
    else if (e.key === "-") loop.zoom = bound(loop.zoom * 0.9);
    else if (e.key === "0" || e.key === "Escape") api.resetView();
    else if (e.key === "Enter") api.openCenter();
    else return;
    e.preventDefault();
    api.draw();
  });

  document.getElementById("hyperCenter").addEventListener("click", api.resetView);
  document.getElementById("hyperOpen").addEventListener("click", api.openCenter);

  if (loop.observer) loop.observer.disconnect();
  if (window.ResizeObserver) {
    loop.observer = new ResizeObserver(() => {
      if (!api.isHyper()) return;
      api.resize();
      api.draw();
    });
    loop.observer.observe(stage);
  }
  if (first) {
    document.addEventListener("visibilitychange", () => {
      if (document.hidden) api.stop();
      else if (api.isHyper()) api.draw();
    });
  }
};
