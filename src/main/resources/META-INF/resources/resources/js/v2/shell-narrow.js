/**
 * OpenTheso V2 — tiroir sidebar (≤900px).
 */
"use strict";

const V2_BP_DRAWER = 900;
const SHELL_NARROW = "(max-width: " + V2_BP_DRAWER + "px)";

function isNarrowShell() {
  return !!(window.matchMedia && window.matchMedia(SHELL_NARROW).matches);
}

function setSidebarDrawer(open) {
  const want = !!open && isNarrowShell();
  document.body.classList.toggle("is-drawer", want);
  const side = document.getElementById("sidebar");
  if (side) {
    if (isNarrowShell()) {
      side.toggleAttribute("inert", !want);
      side.setAttribute("aria-hidden", want ? "false" : "true");
    } else {
      side.removeAttribute("inert");
      side.removeAttribute("aria-hidden");
    }
  }
  const btn = document.getElementById("sbOpenBtn");
  if (btn) {
    btn.classList.toggle("is-on", want);
    btn.setAttribute("aria-expanded", want ? "true" : "false");
    const label = btn.getAttribute(want ? "data-close" : "data-open");
    if (label) {
      btn.setAttribute("aria-label", label);
      btn.setAttribute("title", label);
    }
  }
  const scrim = document.getElementById("sbScrim");
  if (scrim) scrim.hidden = !want;
}

function closeSidebarDrawer() {
  setSidebarDrawer(false);
}
window.closeSidebarDrawer = closeSidebarDrawer;

function bindNarrowShell() {
  const mq = window.matchMedia && window.matchMedia(SHELL_NARROW);
  const sync = () => {
    document.body.classList.toggle("is-narrow", !!(mq && mq.matches));
    if (!mq || !mq.matches) closeSidebarDrawer();
    else setSidebarDrawer(document.body.classList.contains("is-drawer"));
  };
  sync();
  if (!mq) return;
  if (mq.addEventListener) mq.addEventListener("change", sync);
  else if (mq.addListener) mq.addListener(sync);
}
