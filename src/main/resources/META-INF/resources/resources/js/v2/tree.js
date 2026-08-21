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
    tn.classList.toggle("is-open");
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
