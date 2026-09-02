/**
 * OpenTheso V2 — arbre : tri, caret, révélation.
 */
"use strict";

let treeToggleScroll = null;

function isTreeCaretSource(src) {
  if (!src || !src.closest) return false;
  return !!src.closest(".tn-caret");
}

function lockTreeToggleScroll(src) {
  const root = document.getElementById("panelTree");
  const box = treePanel();
  const tn = src && src.closest ? src.closest(".tn") : null;
  treeToggleScroll = {
    top: root ? root.scrollTop : 0,
    minH: box ? box.offsetHeight : 0
  };
  if (box && treeToggleScroll.minH) {
    box.style.minHeight = treeToggleScroll.minH + "px";
  }
  if (tn) {
    const open = !tn.classList.contains("is-open");
    tn.classList.toggle("is-open", open);
    if (open && typeof replayAnim === "function") replayAnim(tn, "is-branch-in");
    tn.classList.add("is-fetching");
  }
}

function restoreTreeToggleScroll() {
  const lock = treeToggleScroll;
  if (!lock) return;
  const root = document.getElementById("panelTree");
  const box = treePanel();
  if (box) box.style.minHeight = "";
  const apply = () => {
    if (root) root.scrollTop = lock.top;
  };
  apply();
  requestAnimationFrame(() => {
    apply();
    requestAnimationFrame(() => {
      apply();
      treeToggleScroll = null;
    });
  });
}

function treeNodes() {
  const box = treePanel();
  if (box) return Array.from(box.querySelectorAll(":scope > .tn, .tn"));
  return $$("#panelTree .tree-body > .tn");
}

function treeDepth(tn) {
  return Number(tn && tn.getAttribute("data-depth") || 0);
}

function previousTreeParent(tn) {
  const depth = treeDepth(tn);
  let prev = tn && tn.previousElementSibling;
  while (prev) {
    if (prev.classList.contains("tn") && treeDepth(prev) < depth) return prev;
    prev = prev.previousElementSibling;
  }
  return null;
}

function sortTreeRange(list) {
  if (!list.length) return [];
  const depth = treeDepth(list[0]);
  const groups = [];
  for (let i = 0; i < list.length; ) {
    const head = list[i];
    let j = i + 1;
    while (j < list.length && treeDepth(list[j]) > depth) j++;
    groups.push([head, ...sortTreeRange(list.slice(i + 1, j))]);
    i = j;
  }
  const numeric = state.sort !== "nota";
  groups.sort((a, b) => (a[0].dataset.sortkey || "").localeCompare(
      b[0].dataset.sortkey || "",
      "fr",
      numeric ? { numeric: true } : { numeric: false, sensitivity: "accent" }
  ));
  return groups.flat();
}

function applySort(mode) {
  if (mode === "nota" || mode === "alpha") {
    state.sort = mode;
  } else if ($("#previewTreeSortState")) {
    state.sort = treeSortMode();
  }
  const box = treePanel();
  // Arbre JSF : l'ordre est celui du serveur, comme le PrimeFaces legacy.
  if (box && (box.id === "previewTree" || box.id === "previewTreeForm:previewTree")) {
    $$(".vo-seg-b[data-sort]").forEach(b => b.classList.toggle("is-on", b.getAttribute("data-sort") === state.sort));
    return;
  }
  const nota = state.sort === "nota";
  const nodes = treeNodes();
  nodes.forEach(tn => {
    const label = (tn.getAttribute("data-key") || "").split("/").pop() || "";
    if (nota) {
      const type = tn.getAttribute("data-type") || "";
      const notation = tn.getAttribute("data-nota") || "";
      tn.dataset.sortkey = type === "facet" ? "\uFFFF\t" + label : notation + "\t" + label;
    } else {
      tn.dataset.sortkey = label;
    }
  });
  const protoBox = box || $("#panelTree .tree-body");
  if (protoBox && nodes.length) {
    sortTreeRange(nodes).forEach(el => protoBox.appendChild(el));
  }
  $$(".vo-seg-b[data-sort]").forEach(b => b.classList.toggle("is-on", b.getAttribute("data-sort") === state.sort));
}
function revealTreeConcept(id) {
  const idEl = document.getElementById("previewTreeRevealForm:revealId");
  const btn = document.getElementById("previewTreeRevealForm:revealBtn");
  if (!id || !idEl || !btn) return;
  idEl.value = id;
  btn.click();
}

/* ── Drag-and-drop arbre (pointeur, pas HTML5 — plus fiable) ── */

var treeDnd = {
  src: null,
  over: null,
  active: false,
  suppressClick: false,
  ghost: null,
  scrollTimer: 0,
  pointerId: null,
  startX: 0,
  startY: 0,
  lastY: 0
};

function treeDndEnabled() {
  const body = document.body;
  if (body && body.getAttribute("data-tree-dnd") === "1") return true;
  const meta = document.getElementById("previewTreeMeta");
  if (meta && (meta.getAttribute("data-dnd") === "1" || meta.getAttribute("data-dnd") === "true")) return true;
  return !!(body && body.getAttribute("data-logged-in") === "1");
}

function treeDndEl(e) {
  const t = e && e.target;
  if (!t) return null;
  return t.nodeType === 1 ? t : t.parentElement;
}

function treeDndField(id) {
  return document.getElementById("previewTreeDropForm:" + id)
      || document.querySelector('#previewTreeDropForm [id$=":' + id + '"]')
      || document.getElementById(id);
}

function treeDndRootLabel() {
  const meta = document.getElementById("previewTreeMeta");
  return (meta && meta.getAttribute("data-msg-dnd-root")) || "Déposer à la racine";
}

function treeDndIsFacet(type) {
  return (type || "") === "facet";
}

function treeDndIsGroup(type) {
  const t = (type || "").toLowerCase();
  return t === "group" || t === "subgroup";
}

function treeDndSubtreeContains(src, candidate) {
  if (!src || !candidate || src === candidate) return false;
  const depth = treeDepth(src);
  let el = src.nextElementSibling;
  while (el && el.classList.contains("tn") && treeDepth(el) > depth) {
    if (el === candidate) return true;
    el = el.nextElementSibling;
  }
  return false;
}

function treeDndParent(tn) {
  return previousTreeParent(tn);
}

function treeDndCanDropOn(src, target, toRoot) {
  if (!src) return false;
  if (toRoot) {
    if (treeDndIsFacet(src.getAttribute("data-type"))) return false;
    return treeDepth(src) > 0 || !!treeDndParent(src);
  }
  if (!target || target === src) return false;
  if (treeDndSubtreeContains(src, target)) return false;
  const srcType = src.getAttribute("data-type") || "";
  const dstType = target.getAttribute("data-type") || "";
  if (treeDndIsGroup(dstType)) return false;
  if (treeDndIsFacet(srcType)) {
    return !treeDndIsFacet(dstType) && !treeDndIsGroup(dstType);
  }
  if (treeDndIsFacet(dstType)) {
    return src.getAttribute("data-has-children") !== "true";
  }
  return true;
}

function treeDndEnsureRootZone() {
  let zone = document.getElementById("treeRootDrop");
  if (zone && document.getElementById("previewTree") && previewTreeContains(zone)) return zone;
  if (zone && zone.parentNode) zone.parentNode.removeChild(zone);
  const box = treePanel();
  if (!box) return null;
  zone = document.createElement("div");
  zone.id = "treeRootDrop";
  zone.className = "tree-root-drop";
  zone.hidden = true;
  zone.innerHTML = "<span>" + treeDndRootLabel() + "</span>";
  box.insertBefore(zone, box.firstChild);
  return zone;
}

function previewTreeContains(el) {
  const box = treePanel();
  return !!(box && el && box.contains(el));
}

function treeDndShowRoot(on) {
  const zone = treeDndEnsureRootZone();
  if (!zone) return;
  zone.hidden = !on;
  if (!on) zone.classList.remove("is-over");
}

function treeDndClearOver() {
  if (treeDnd.over) {
    treeDnd.over.classList.remove("is-dnd-over", "is-dnd-bad");
    treeDnd.over = null;
  }
  const zone = document.getElementById("treeRootDrop");
  if (zone) zone.classList.remove("is-over");
}

function treeDndSetOver(tn) {
  if (treeDnd.over === tn) return;
  treeDndClearOver();
  if (!tn) return;
  treeDnd.over = tn;
  const ok = treeDndCanDropOn(treeDnd.src, tn, false);
  tn.classList.add(ok ? "is-dnd-over" : "is-dnd-bad");
}

function treeDndMakeGhost(tn) {
  treeDndRemoveGhost();
  const text = ((tn.querySelector(".tn-text") || {}).textContent || "").trim();
  const ghost = document.createElement("div");
  ghost.className = "tn-dnd-ghost";
  ghost.textContent = text;
  ghost.setAttribute("aria-hidden", "true");
  document.body.appendChild(ghost);
  treeDnd.ghost = ghost;
  return ghost;
}

function treeDndMoveGhost(x, y) {
  if (!treeDnd.ghost) return;
  treeDnd.ghost.style.transform = "translate(" + (x + 12) + "px," + (y + 10) + "px)";
}

function treeDndRemoveGhost() {
  if (treeDnd.ghost && treeDnd.ghost.parentNode) {
    treeDnd.ghost.parentNode.removeChild(treeDnd.ghost);
  }
  treeDnd.ghost = null;
}

function treeDndStopScroll() {
  if (treeDnd.scrollTimer) {
    cancelAnimationFrame(treeDnd.scrollTimer);
    treeDnd.scrollTimer = 0;
  }
}

function treeDndAutoScroll(clientY) {
  const root = document.getElementById("panelTree");
  if (!root) return;
  const rect = root.getBoundingClientRect();
  const edge = 36;
  let dir = 0;
  if (clientY < rect.top + edge) dir = -1;
  else if (clientY > rect.bottom - edge) dir = 1;
  treeDndStopScroll();
  if (!dir) return;
  const step = () => {
    root.scrollTop += dir * 8;
    treeDnd.scrollTimer = requestAnimationFrame(step);
  };
  treeDnd.scrollTimer = requestAnimationFrame(step);
}

function treeDndHit(x, y) {
  const stack = document.elementsFromPoint ? document.elementsFromPoint(x, y) : [document.elementFromPoint(x, y)];
  for (let i = 0; i < stack.length; i++) {
    const el = stack[i];
    if (!el || !el.closest) continue;
    if (el.id === "treeRootDrop" || el.closest("#treeRootDrop")) {
      return { toRoot: true, tn: null };
    }
    const tn = el.closest(".tn");
    if (tn && tn.closest("#panelTree")) return { toRoot: false, tn: tn };
  }
  return { toRoot: false, tn: null };
}

function treeDndCleanup() {
  if (treeDnd.src) {
    treeDnd.src.classList.remove("is-dnd-src");
    try {
      if (treeDnd.pointerId != null) treeDnd.src.releasePointerCapture(treeDnd.pointerId);
    } catch (ex) {}
  }
  treeDndClearOver();
  treeDndShowRoot(false);
  treeDndRemoveGhost();
  treeDndStopScroll();
  document.documentElement.classList.remove("is-tree-dnd");
  treeDnd.src = null;
  treeDnd.active = false;
  treeDnd.pointerId = null;
}

function treeDndSubmit(toRoot, target) {
  const src = treeDnd.src;
  if (!src || !treeDndCanDropOn(src, target, toRoot)) return;
  const dragId = treeDndField("htmlDragId");
  const dropId = treeDndField("htmlDropId");
  const dragType = treeDndField("htmlDragType");
  const dropType = treeDndField("htmlDropType");
  const parentId = treeDndField("htmlParentId");
  const dropRoot = treeDndField("htmlDropRoot");
  const btn = treeDndField("dropBtn");
  if (!dragId || !btn) return;
  const parent = treeDndParent(src);
  dragId.value = src.getAttribute("data-id") || "";
  dropId.value = toRoot ? "" : (target ? target.getAttribute("data-id") || "" : "");
  dragType.value = src.getAttribute("data-type") || "";
  dropType.value = toRoot ? "root" : (target ? target.getAttribute("data-type") || "" : "");
  parentId.value = (parent && treeDndIsFacet(parent.getAttribute("data-type")))
      ? (parent.getAttribute("data-id") || "")
      : "";
  dropRoot.value = toRoot ? "1" : "";
  btn.click();
}

function treeDndBegin(e) {
  const src = treeDnd.src;
  if (!src || treeDnd.active) return;
  treeDnd.active = true;
  treeDnd.suppressClick = true;
  src.classList.add("is-dnd-src");
  document.documentElement.classList.add("is-tree-dnd");
  try { src.setPointerCapture(e.pointerId); } catch (ex) {}
  treeDndMakeGhost(src);
  treeDndMoveGhost(e.clientX, e.clientY);
  treeDndShowRoot(treeDndCanDropOn(src, null, true));
}

function bindTreeDnd() {
  if (bindTreeDnd._on) return;
  bindTreeDnd._on = true;

  document.addEventListener("pointerdown", (e) => {
    if (!treeDndEnabled() || e.button) return;
    const el = treeDndEl(e);
    if (!el || !el.closest) return;
    if (el.closest(".tn-caret, .tn-check, .tn-add")) return;
    const row = el.closest(".tn-row");
    if (!row || !row.closest("#panelTree")) return;
    const tn = row.closest(".tn");
    if (!tn || !tn.getAttribute("data-id")) return;
    e.preventDefault();
    treeDnd.src = tn;
    treeDnd.active = false;
    treeDnd.suppressClick = true;
    treeDnd.pointerId = e.pointerId;
    treeDnd.startX = e.clientX;
    treeDnd.startY = e.clientY;
    treeDnd.lastY = e.clientY;
    try { tn.setPointerCapture(e.pointerId); } catch (ex) {}
  }, { passive: false });

  document.addEventListener("pointermove", (e) => {
    if (!treeDnd.src || treeDnd.pointerId !== e.pointerId) return;
    const dx = e.clientX - treeDnd.startX;
    const dy = e.clientY - treeDnd.startY;
    if (!treeDnd.active) {
      if ((dx * dx + dy * dy) < 36) return;
      treeDndBegin(e);
    }
    e.preventDefault();
    treeDnd.lastY = e.clientY;
    treeDndMoveGhost(e.clientX, e.clientY);
    treeDndAutoScroll(e.clientY);
    const hit = treeDndHit(e.clientX, e.clientY);
    if (hit.toRoot) {
      treeDndClearOver();
      const zone = document.getElementById("treeRootDrop");
      if (zone) zone.classList.toggle("is-over", treeDndCanDropOn(treeDnd.src, null, true));
      return;
    }
    if (hit.tn) treeDndSetOver(hit.tn);
    else treeDndClearOver();
  }, { passive: false });

  function treeDndFinish(e, dropped) {
    if (!treeDnd.src) return;
    if (e && e.pointerId != null && treeDnd.pointerId != null && treeDnd.pointerId !== e.pointerId) return;
    const active = treeDnd.active;
    const src = treeDnd.src;
    if (dropped && active && e) {
      const hit = treeDndHit(e.clientX, e.clientY);
      treeDndSubmit(!!hit.toRoot, hit.tn);
    }
    treeDndCleanup();
    if (!active && dropped && src) {
      const label = src.querySelector(".tn-label");
      treeDnd.suppressClick = false;
      if (label) label.click();
    }
  }

  document.addEventListener("pointerup", (e) => treeDndFinish(e, true));
  document.addEventListener("pointercancel", (e) => treeDndFinish(e, false));

  document.addEventListener("click", (e) => {
    if (!treeDnd.suppressClick) return;
    const el = treeDndEl(e);
    if (!el || !el.closest || !el.closest("#panelTree")) {
      treeDnd.suppressClick = false;
      return;
    }
    e.preventDefault();
    e.stopPropagation();
    treeDnd.suppressClick = false;
  }, true);

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && treeDnd.src) {
      treeDndFinish(e, false);
      return;
    }
    if (e.key !== "Enter" && e.key !== " ") return;
    const el = treeDndEl(e);
    if (!el || !el.closest) return;
    if (el.closest("input, textarea, select, button, a")) return;
    const label = el.closest("#panelTree .tn-label");
    if (!label) return;
    e.preventDefault();
    label.click();
  });
}

bindTreeDnd();

