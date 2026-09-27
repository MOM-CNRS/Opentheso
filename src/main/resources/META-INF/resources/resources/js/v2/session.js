/**
 * OpenTheso V2 — expiration idle : avertissement puis déconnexion.
 * Réagit à l'activité utilisateur, pas aux polls AJAX de fond.
 */
"use strict";

(function initV2SessionIdle() {
  const body = document.body;
  if (!body || body.getAttribute("data-logged-in") !== "1") return;

  const timeout = parseInt(body.getAttribute("data-session-timeout") || "0", 10);
  const expireUrl = body.getAttribute("data-session-expire") || "";
  const keepAliveUrl = body.getAttribute("data-session-keepalive") || "";
  if (!expireUrl || !Number.isFinite(timeout) || timeout < 8000) return;

  const EXPIRE_LEAD_MS = 30000;
  const WARN_LEAD_MS = 120000;
  const expireAfter = Math.max(timeout - EXPIRE_LEAD_MS, 8000);
  const warnAfter = expireAfter > WARN_LEAD_MS + 8000
    ? expireAfter - WARN_LEAD_MS
    : Math.max(Math.floor(expireAfter / 2), 0);

  let warnTimer = 0;
  let expireTimer = 0;
  let lastActivity = Date.now();
  let lastPing = 0;
  let actLock = false;
  const PING_GAP_MS = 10000;

  function goExpire() {
    window.location.href = expireUrl;
  }

  function hideWarn() {
    if (typeof hideConfirm === "function") hideConfirm("#sessionExpireConfirm");
  }

  function showWarn() {
    if (typeof showConfirm === "function") showConfirm("#sessionExpireConfirm");
  }

  function clearTimers() {
    window.clearTimeout(warnTimer);
    window.clearTimeout(expireTimer);
  }

  function scheduleFrom(elapsed) {
    clearTimers();
    const remainWarn = Math.max(warnAfter - elapsed, 0);
    const remainExpire = Math.max(expireAfter - elapsed, 0);
    if (elapsed < warnAfter) {
      warnTimer = window.setTimeout(showWarn, remainWarn);
    } else {
      showWarn();
    }
    expireTimer = window.setTimeout(goExpire, remainExpire);
  }

  function schedule() {
    lastActivity = Date.now();
    scheduleFrom(0);
  }

  function pingAndReset() {
    const now = Date.now();
    if (keepAliveUrl && now - lastPing >= PING_GAP_MS) {
      lastPing = now;
      fetch(keepAliveUrl, { method: "GET", credentials: "same-origin", cache: "no-store" })
        .then((response) => {
          if (response.status === 401 || response.status === 403) goExpire();
        })
        .catch(() => {});
    }
    hideWarn();
    schedule();
  }

  function syncFromClock() {
    const elapsed = Date.now() - lastActivity;
    if (elapsed >= expireAfter) {
      goExpire();
      return;
    }
    scheduleFrom(elapsed);
  }

  window.v2SessionStay = function v2SessionStay() {
    pingAndReset();
    return false;
  };

  window.v2SessionExpire = function v2SessionExpire() {
    goExpire();
    return false;
  };

  function noteActivity(e) {
    if (e && e.target && e.target.closest && e.target.closest("[data-act='session-expire']")) return;
    if (actLock) return;
    actLock = true;
    pingAndReset();
    window.setTimeout(() => { actLock = false; }, 1000);
  }

  document.addEventListener("pointerdown", noteActivity, { passive: true, capture: true });
  document.addEventListener("wheel", noteActivity, { passive: true });
  window.addEventListener("scroll", noteActivity, { passive: true, capture: true });
  document.addEventListener("keydown", (e) => {
    if (e.metaKey || e.ctrlKey || e.altKey) return;
    if (e.key === "Escape") return;
    noteActivity(e);
  }, { passive: true });

  document.addEventListener("click", (e) => {
    const actEl = e.target.closest && e.target.closest("[data-act]");
    if (!actEl) return;
    const act = actEl.getAttribute("data-act");
    if (act === "session-stay" || act === "session-expire-dismiss") {
      e.preventDefault();
      pingAndReset();
    } else if (act === "session-expire") {
      e.preventDefault();
      goExpire();
    }
  });

  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") syncFromClock();
  });

  schedule();
})();
