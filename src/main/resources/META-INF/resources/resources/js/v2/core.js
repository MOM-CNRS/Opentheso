/**
 * OpenTheso V2 — noyau partagé (DOM, état, thésaurus).
 */
"use strict";

function $(s, r) {
  return (r || document).querySelector(s);
}

function $$(s, r) {
  return Array.from((r || document).querySelectorAll(s));
}

function norm(s) {
  return (s || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

var SCREEN = document.body.getAttribute("data-page") || "consultation";
var IS_CONSULT = SCREEN === "consultation";
var PAGE = 12;
var SIDE = {
  arbo: "panelTree", tableau: "panelTable", hyper: "panelTree",
  collection: "panelCollection", recherche: "panelResults"
};
var VIEW_LABEL = {
  arbo: "Arborescence", tableau: "Tableau", hyper: "Graphe",
  collection: "Collection", recherche: "Recherche"
};
var GROUPS = {
  actif: ["valide", "insere"],
  candidat: ["candidat"],
  inactif: ["rejete", "deprecie"]
};
var TREE_STATUS_ALL = ["valide", "insere", "candidat", "rejete", "deprecie"];
var TREE_STATUS_DEFAULT = ["valide", "insere", "candidat"];
var TABLE_COL_ALL = ["status", "type", "notation", "path"];
var TABLE_COL_DEFAULT = TABLE_COL_ALL.slice();

function readTreeStatusPref() {
  try {
    const raw = JSON.parse(document.body.getAttribute("data-tree-status") || "");
    const list = raw && Array.isArray(raw.selected) ? raw.selected
      : (Array.isArray(raw) ? raw : null);
    if (!list) return TREE_STATUS_DEFAULT.slice();
    return list.filter((s) => TREE_STATUS_ALL.indexOf(s) >= 0);
  } catch (e) {
    return TREE_STATUS_DEFAULT.slice();
  }
}

function readTableColPref() {
  try {
    const raw = JSON.parse(document.body.getAttribute("data-table-cols") || "");
    const list = raw && Array.isArray(raw.selected) ? raw.selected
      : (Array.isArray(raw) ? raw : null);
    if (!list) return TABLE_COL_DEFAULT.slice();
    return list.filter((s) => TABLE_COL_ALL.indexOf(s) >= 0);
  } catch (e) {
    return TABLE_COL_DEFAULT.slice();
  }
}

function treeSortMode() {
  const el = $("#previewTreeSortState") || $("#voSortRow");
  return (el && el.getAttribute("data-tree-sort") === "nota") ? "nota" : "alpha";
}

var state = {
  view: "arbo",
  home: true,
  conceptId: null,
  draft: false,
  committed: "",
  selected: new Set(),
  subtreeSize: new Map(),
  subtreeSizePending: new Map(),
  selectedAllThesaurus: false,
  statusSet: new Set(readTreeStatusPref()),
  candBy: "",
  candFrom: "",
  candTo: "",
  sort: treeSortMode(),
  acIdx: -1,
  resultLimit: PAGE,
  tblSort: "concept",
  tblDir: 1,
  tblPage: 1,
  tblPageSize: 50,
  tblCols: new Set(readTableColPref()),
  colId: null,
  colSort: treeSortMode(),
  showPath: true,
  highlight: true,
  density: "regular",
  moveTarget: null,
  graphFront: true
};

function toast(msg, opts) {
  const el = $("#appToast");
  if (!el) return;
  const text = el.querySelector(".app-toast-txt") || el;
  const soft = !!(opts && opts.soft);
  const error = !!(opts && opts.error);
  el.classList.toggle("is-soft", soft);
  el.classList.toggle("is-error", error);
  const ico = el.querySelector(".app-toast-ico path");
  if (ico) {
    ico.setAttribute("d", error ? "M18 6 6 18M6 6l12 12" : "M20 6 9 17l-5-5");
  }
  text.textContent = msg;
  el.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => {
    el.hidden = true;
    el.classList.remove("is-soft", "is-error");
  }, soft ? 2200 : error ? 3600 : 2600);
}
window.toast = toast;

function prefersReducedMotion() {
  return !!(window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
}

function replayAnim(el, cls) {
  if (!el || !cls || prefersReducedMotion()) return;
  el.classList.remove(cls);
  void el.offsetWidth;
  el.classList.add(cls);
}

function showPanel(sel, id) {
  $$(sel).forEach((el) => {
    const on = el.id === id;
    const was = el.classList.contains("is-on");
    el.classList.toggle("is-on", on);
    if (!on || was) return;
    if (el.id !== "viewHyper" && !el.classList.contains("hyper")) {
      replayAnim(el, "is-nav-in");
    }
    if (sel.indexOf("view-panel") >= 0) {
      const view = document.getElementById("previewView");
      if (!view) return;
      if (prefersReducedMotion() || typeof view.scrollTo !== "function") view.scrollTop = 0;
      else view.scrollTo({ top: 0, behavior: "smooth" });
    }
  });
}

function treePanel() {
  return document.getElementById("previewTree")
      || document.getElementById("previewTreeForm:previewTree");
}

function treeMeta() {
  return document.getElementById("previewTreeMeta") || treePanel();
}

function thesaurusTitle() {
  const el = treeMeta();
  return (el && el.getAttribute("data-thesaurus-title")) || "";
}

function thesaurusId() {
  const el = treeMeta();
  return (el && el.getAttribute("data-thesaurus-id")) || "";
}

function thesaurusConceptCount() {
  const el = treeMeta();
  const n = Number(el && el.getAttribute("data-concept-count"));
  return Number.isFinite(n) && n > 0 ? n : 0;
}

function thesaurusLang() {
  const sel = document.getElementById("termLang");
  if (sel && sel.value) return sel.value;
  const el = treeMeta();
  return (el && el.getAttribute("data-lang")) || "fr";
}

function csrfToken() {
  const el = document.getElementById("previewExportCsrf")
    || document.querySelector("input[name='csrfToken']");
  return (el && el.value) || "";
}

function clickPreviewJsf(buttonId, fields) {
  if (fields) {
    Object.keys(fields).forEach((id) => {
      const el = document.getElementById(id);
      if (el) el.value = fields[id];
    });
  }
  const btn = document.getElementById(buttonId);
  if (!btn) return;
  window.setTimeout(() => btn.click(), 0);
}

function hideConfirm(id) {
  const dlg = $(id);
  if (!dlg || dlg.hidden) return false;
  dlg.hidden = true;
  return true;
}

function showConfirm(id) {
  const dlg = $(id);
  if (!dlg) return;
  dlg.hidden = false;
  const cancel = dlg.querySelector(".confirm-cancel");
  if (cancel) cancel.focus();
}

function escapeHtml(value) {
  return String(value == null ? "" : value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

window.OT = {
  $, $$, norm, SCREEN, IS_CONSULT, PAGE, SIDE, VIEW_LABEL, GROUPS, TREE_STATUS_DEFAULT, state,
  toast, showPanel, treePanel, treeMeta, thesaurusId, thesaurusLang,
  thesaurusTitle, thesaurusConceptCount, csrfToken, clickPreviewJsf,
  hideConfirm, showConfirm, escapeHtml
};
