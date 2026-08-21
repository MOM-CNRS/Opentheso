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
  statusSet: new Set(["valide", "insere", "candidat"]),
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
  tblCols: new Set(["status", "type", "notation", "path"]),
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
  el.classList.toggle("is-soft", soft);
  text.textContent = msg;
  el.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => {
    el.hidden = true;
    el.classList.remove("is-soft");
  }, soft ? 2200 : 2600);
}

function showPanel(sel, id) {
  $$(sel).forEach(el => el.classList.toggle("is-on", el.id === id));
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
  $, $$, norm, SCREEN, IS_CONSULT, PAGE, SIDE, VIEW_LABEL, GROUPS, state,
  toast, showPanel, treePanel, treeMeta, thesaurusId, thesaurusLang,
  thesaurusTitle, thesaurusConceptCount, csrfToken, clickPreviewJsf,
  hideConfirm, showConfirm, escapeHtml
};
