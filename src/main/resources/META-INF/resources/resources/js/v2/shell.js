/**
 * OpenTheso V2 — vues, peinture, fiche, accueil.
 */
"use strict";

function closeSearchUi() {
  const box = $("#searchBox");
  if (box) box.classList.remove("is-open");
  const fld = $("#searchField");
  if (fld) fld.classList.remove("is-focused");
  state.acIdx = -1;
  paintQueryHighlight();
}

function liveDetailState() {
  return $("#previewDetailState");
}

function liveDetailRequested() {
  const el = liveDetailState();
  return !!(el && el.getAttribute("data-requested") === "1");
}

function resolveNodeType(el) {
  if (!el) return "concept";
  const host = el.closest("[data-type],[data-status]") || el;
  const type = (el.getAttribute("data-type") || host.getAttribute("data-type") || "").toLowerCase();
  const status = (el.getAttribute("data-status") || host.getAttribute("data-status") || "").toLowerCase();
  if (type === "facet") return "facet";
  if (type === "group" || type === "subgroup") return type;
  if (type === "more") return "more";
  if (type === "candidat" || status === "candidat") return "candidat";
  return type || "concept";
}

function beginLiveOpen() {
  const live = $("#viewLive");
  if (!live) return;
  live.classList.add("is-loading");
  live.classList.remove("is-ready");
  showPanel(".view-panel", "viewLive");
  const back = $("#liveBackList");
  if (back && fromCandList()) back.hidden = false;
  paintListBack();
  const view = $("#previewView");
  if (view) view.scrollTop = 0;
}

function finishLiveOpen() {
  const live = $("#viewLive");
  if (!live) return;
  live.classList.remove("is-loading");
  live.classList.add("is-ready");
}

function markCandRowOpen(id) {
  $$(".cand-row").forEach((row) => {
    const on = !!(id && row.getAttribute("data-id") === id);
    row.classList.toggle("is-open", on);
    if (on) row.setAttribute("aria-current", "page");
    else row.removeAttribute("aria-current");
  });
}

function openLiveDetail(id, nodeType) {
  const idEl = document.getElementById("previewOpenForm:openId");
  const typeEl = document.getElementById("previewOpenForm:openType");
  const btn = document.getElementById("previewOpenForm:openBtn");
  if (!id || !idEl || !btn) return false;
  idEl.value = id;
  if (typeEl) typeEl.value = nodeType || "";
  if (!(state.view === "hyper" && state.graphFront)) beginLiveOpen();
  markCandRowOpen(id);
  btn.click();
  return true;
}

function fromCandList() {
  return SCREEN === "candidats"
      || new URLSearchParams(location.search).get("from") === "candidats";
}

function paintListBack() {
  const btn = $("#liveBackList");
  if (!btn) return;
  const show = fromCandList() && liveDetailRequested() && !state.home && !state.draft;
  btn.hidden = !show;
}

function backToCandList() {
  if (SCREEN === "candidats") {
    const keepId = state.conceptId;
    state.home = false;
    state.draft = false;
    state.conceptId = null;
    highlightConcept(null);
    const live = $("#viewLive");
    if (live) live.classList.remove("is-loading", "is-ready");
    const clearBtn = document.getElementById("previewTreeRevealForm:clearRevealBtn");
    if (clearBtn) clearBtn.click();
    showPanel(".view-panel", "viewCandList");
    paintListBack();
    markCandRowOpen(keepId);
    const row = keepId && document.querySelector('.cand-row[data-id="' + CSS.escape(keepId) + '"]');
    if (row) requestAnimationFrame(() => row.focus());
    return;
  }
  go("candidat/candidats.xhtml");
}

function selectOpenedInTree(id) {
  if (!id) return;
  const tn = findTreeNodeById(id);
  if (tn) {
    const st = tn.getAttribute("data-status");
    if (st && !state.statusSet.has(st)) {
      state.statusSet.add(st);
      if (typeof syncStatusUi === "function") syncStatusUi();
    }
    if (typeof applyStatusFilter === "function") applyStatusFilter();
    highlightConcept(id);
    return;
  }
  revealTreeConcept(id);
}

function showLiveDetail() {
  const el = liveDetailState();
  if (!el || el.getAttribute("data-requested") !== "1") return false;
  state.home = false;
  state.draft = false;
  state.conceptId = el.getAttribute("data-id") || state.conceptId;
  highlightConcept(state.conceptId);
  selectOpenedInTree(state.conceptId);
  paintGraphBack();
  paintListBack();
  if (state.view === "hyper" && state.graphFront) {
    return false;
  }
  showPanel(".view-panel", "viewLive");
  finishLiveOpen();
  const view = $("#previewView");
  if (view) view.scrollTop = 0;
  return true;
}

function paintGraphBack() {
  const btn = $("#liveBackGraph");
  if (!btn) return;
  const show = state.view === "hyper" && !state.graphFront;
  btn.hidden = !show;
}

function backToGraph() {
  state.graphFront = true;
  state.home = false;
  state.draft = false;
  paint();
}

function paintMain() {
  const v = state.view;
  let id;
  if (v === "collection") {
    if (state.colId) id = "viewCollection";
    else if (liveDetailRequested() && !state.home && !state.draft && state.conceptId) id = "viewLive";
    else id = "viewCollection";
  } else if (v === "hyper" && state.graphFront && $("#viewHyper")) {
    id = "viewHyper";
  } else if (liveDetailRequested() && !state.home && !state.draft) {
    id = "viewLive";
  } else if (v === "hyper" && $("#viewHyper")) {
    id = "viewHyper";
  } else if (!IS_CONSULT) {
    if (v === "arbo" && $("#viewHome")) id = "viewHome";
    else return;
  } else if (state.draft && (v === "arbo" || v === "tableau" || v === "recherche")) {
    id = "viewDraft";
  } else if (v === "recherche") {
    if (!rankHits(state.committed).length) id = "viewNoResults";
    else id = state.conceptId ? "viewConcept" : "viewEmpty";
  } else if (v === "tableau") {
    id = state.conceptId ? "viewConcept" : "viewEmpty";
  } else if (state.home && !state.conceptId && $("#viewHome")) {
    id = "viewHome";
  } else {
    id = state.conceptId ? "viewConcept" : "viewEmpty";
  }
  showPanel(".view-panel", id);
  paintGraphBack();
  paintListBack();
  const nq = $("#nrQuery");
  if (nq) nq.textContent = state.committed;
}

function paintSidebar() {
  showPanel(".sidebar-panel", SIDE[state.view] || "panelTree");
  const sb = $("#panelTree");
  if (sb) sb.classList.toggle("sb-tree", state.view === "arbo" || state.view === "hyper");
  document.body.classList.toggle("hide-path", !state.showPath);
  document.body.classList.toggle("density-compact", state.density === "compact");
  if (state.view === "tableau") ensureTableRows();
  if (state.view === "collection") {
    const key = collectionCacheKey();
    if (collectionTreeState.key !== key && !collectionTreeState.loading) {
      setCollectionLoading(true);
    }
    ensureCollectionTree();
  }
  if (state.view === "hyper" && state.graphFront) ensureGlobe();
  else stopGlobe();
}

function paintViewPick() {
  const pick = $("#viewPick");
  if (pick) pick.setAttribute("data-view", state.view);
  $$(".view-pick-item").forEach(b => b.classList.toggle("is-on", b.getAttribute("data-view") === state.view));
  const vo = $("#voPopView");
  if (vo) vo.textContent = VIEW_LABEL[state.view] || state.view;
  const sortRow = $("#voSortRow");
  if (sortRow) sortRow.hidden = state.view !== "arbo" && state.view !== "hyper";
  const colSortRow = $("#voColSortRow");
  if (colSortRow) {
    colSortRow.hidden = state.view !== "collection";
    $$("#voColSortRow .vo-seg-b").forEach(b => {
      b.classList.toggle("is-on", b.getAttribute("data-sort") === state.colSort);
    });
  }
  const searchOn = state.view === "recherche";
  ["voSearchOpts", "voPathRow", "voHlRow"].forEach(id => {
    const el = $("#" + id); if (el) el.hidden = !searchOn;
  });
  const tbl = $("#voTableOpts");
  if (tbl) tbl.hidden = state.view !== "tableau";
  const stk = $(".vo-pop-stk");
  if (stk) stk.hidden = !(state.view === "arbo" || state.view === "tableau" || state.view === "hyper");
  $("#thIdBtn") && $("#thIdBtn").classList.toggle("is-active", SCREEN === "accueil");
  paintBadges();
}

function paintBadges() {
  const n = rankHits(state.committed).length;
  ["viewPickBadge", "viewMenuBadge"].forEach(id => {
    const el = $("#" + id);
    if (!el) return;
    el.textContent = String(n);
    el.hidden = n === 0;
  });
}

function paint() {
  paintViewPick();
  paintMain();
  paintSidebar();
  updateBulk();
}

function setView(view) {
  if (!view) return;
  if (view === "hyper" && !$("#viewHyper")) {
    go("graph/graphe.xhtml");
    return;
  }
  if (!IS_CONSULT && view !== "arbo" && view !== "hyper") {
    go("thesaurus/consultation.xhtml?view=" + encodeURIComponent(view));
    return;
  }
  if (!IS_CONSULT && view === "arbo" && !$("#viewHome")) {
    go("index.xhtml");
    return;
  }
  state.view = view;
  if (view === "hyper") state.graphFront = true;
  if (view !== "arbo" && view !== "hyper" && view !== "tableau") {
    clearSelection(true);
  }
  if (view === "recherche") {
    state.home = false;
    syncRechercheConcept();
    paintCommittedResults();
  }
  $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
  paint();
}

function findTreeNodeById(id) {
  if (!id) return null;
  const boxes = [treePanel(), $("#panelTree"), $("#panelCollectionTree")].filter(Boolean);
  const seen = new Set();
  for (let i = 0; i < boxes.length; i++) {
    const box = boxes[i];
    if (seen.has(box)) continue;
    seen.add(box);
    const nodes = box.querySelectorAll(".tn[data-id]");
    for (let j = 0; j < nodes.length; j++) {
      if (nodes[j].getAttribute("data-id") === id) return nodes[j];
    }
  }
  return null;
}

function highlightConcept(id) {
  $$(".tn-row.is-active").forEach(r => r.classList.remove("is-active"));
  $$("#panelTable tr.is-active").forEach(r => r.classList.remove("is-active"));
  $$(".rl-item.is-sel").forEach(r => r.classList.remove("is-sel"));
  $$(".hyper-dot.is-active").forEach(d => d.classList.remove("is-active"));
  $$("#viewConcept .cv").forEach(el => el.classList.toggle("is-on", el.getAttribute("data-id") === id));
  if (!id) return;
  const tn = findTreeNodeById(id);
  if (tn) {
    const row = tn.querySelector(".tn-row");
    if (row) {
      row.classList.add("is-active");
      row.scrollIntoView({ block: state.view === "hyper" ? "center" : "nearest" });
    }
    let walk = tn;
    while (walk) {
      walk.classList.add("is-open");
      walk = previousTreeParent(walk);
    }
    if (state.view === "collection") paintCollectionVisibility();
  }
  if (state.view === "tableau") revealTableConcept(id);
  if (state.view === "hyper") globeSelectId(id, true);
  const tr = $(`#panelTable tr[data-id="${CSS.escape(id)}"]`);
  if (tr) tr.classList.add("is-active");
  const rl = Array.from(document.querySelectorAll(".rl-item[data-id]")).find(el => el.getAttribute("data-id") === id);
  if (rl) {
    rl.classList.add("is-sel");
    rl.scrollIntoView({ block: "nearest" });
  }
  const hg = $(`.hyper-node[data-id="${CSS.escape(id)}"] .hyper-dot`);
  if (hg) hg.classList.add("is-active");
}

/* jump = arbre / autocomplete / relation (openConcept). stay = tableau / résultats (openInView). */
function openConcept(id, mode) {
  if (!id) return;
  if (!IS_CONSULT) {
    go("thesaurus/consultation.xhtml?id=" + encodeURIComponent(id));
    return;
  }
  state.home = false;
  state.draft = false;
  state.conceptId = id;
  if (mode !== "stay") state.view = "arbo";
  highlightConcept(id);
  closeSearchUi();
  paint();
}

function openHome() {
  if (IS_CONSULT) {
    state.home = true;
    state.conceptId = null;
    state.draft = false;
    state.view = "arbo";
    highlightConcept(null);
    paint();
    return;
  }
  if (SCREEN !== "accueil") go("index.xhtml");
}

function createCandidate(btn) {
  const parentName = ((btn && btn.getAttribute("data-pref")) || "").trim();
  const pathKey = ((btn && btn.getAttribute("data-path")) || "").trim();
  if (SCREEN !== "candidats") {
    const q = new URLSearchParams({ new: "1" });
    if (parentName) q.set("pref", parentName);
    if (pathKey) q.set("path", pathKey);
    go("candidat/candidats.xhtml?" + q.toString());
    return;
  }
  state.home = false;
  state.draft = true;
  state.conceptId = null;
  if (state.view !== "arbo" && state.view !== "tableau" && state.view !== "recherche") {
    state.view = "arbo";
  }
  const root = $("#viewDraft");
  if (root) {
    root.classList.toggle("is-under", !!parentName);
    root.classList.toggle("has-path", !!pathKey);
    const pathEl = $("#draftPathLabel");
    if (pathEl) pathEl.textContent = pathKey.replace(/\//g, " › ");
    const pathBox = $("#draftPathBox");
    if (pathBox) pathBox.hidden = !pathKey;
    const bt = $("#draftBt");
    if (bt) bt.value = parentName;
    if (typeof resetCollectionPicker === "function") {
      resetCollectionPicker(pathKey ? pathKey.split("/")[0] : "");
    }
    ["draftTitle", "draftAlts", "draftHidden", "draftNt", "draftRt", "draftCustomRel",
     "draftTrFr", "draftTrEn", "draftTrDe", "draftTrEs", "draftTrIt",
     "draftDef", "draftScope", "draftExt", "draftImg", "draftGps"].forEach(id => {
      const el = $("#" + id);
      if (el) el.value = "";
    });
    syncDraftPrefMirror();
  }
  highlightConcept(null);
  $("#searchBox") && $("#searchBox").classList.remove("is-open");
  showPanel(".view-panel", "viewDraft");
  requestAnimationFrame(() => { const t = $("#draftTitle"); if (t) t.focus(); });
}

function syncDraftPrefMirror() {
  const title = $("#draftTitle");
  const mirror = $("#draftPrefMirror");
  const value = title && title.value.trim();
  if (mirror) mirror.textContent = value || "—";
  const create = $("#draftCreate");
  if (create) create.disabled = !value;
}

function resolveDraft(kind) {
  if (kind === "créé") {
    const title = $("#draftTitle");
    if (!title || !title.value.trim()) {
      if (title) title.focus();
      toast("Indiquez un intitulé pour créer le candidat", { soft: true });
      return;
    }
  }
  state.draft = false;
  toast(kind === "créé" ? "Candidat créé · en attente de validation" : "Création annulée");
  if (SCREEN === "candidats") {
    showPanel(".view-panel", "viewCandList");
    return;
  }
  paint();
}

function showHomePanel(panel) {
  if (panel === "viewStats") go("toolbox/statistiques.xhtml");
  else if (panel === "viewSettings") go("setting/parametres.xhtml");
  else if (panel === "viewBatch") go("toolbox/atelier.xhtml");
  else if (panel === "viewMaintenance") go("toolbox/maintenance.xhtml");
  else go("index.xhtml");
}

function setBatch(obj, op) {
  const root = $("#boRoot");
  if (!root || !obj) return;
  const firstSeg = $(`.bo-op-seg[data-obj="${CSS.escape(obj)}"]`);
  op = op || (firstSeg && firstSeg.getAttribute("data-op")) || "import";
  root.setAttribute("data-obj", obj);
  root.setAttribute("data-op", op);
  $$(".bo-head[data-obj]").forEach(el => el.classList.toggle("is-on", el.getAttribute("data-obj") === obj));
  $$(".bo-opsel[data-obj]").forEach(el => el.classList.toggle("is-on", el.getAttribute("data-obj") === obj));
  $$(".bo-op-seg").forEach(el => {
    el.classList.toggle("is-on", el.getAttribute("data-obj") === obj && el.getAttribute("data-op") === op);
  });
  $$(".bo-panel[data-obj]").forEach(el => {
    const on = el.getAttribute("data-obj") === obj && el.getAttribute("data-op") === op;
    el.classList.toggle("is-on", on);
    // Ne pas effacer l'état serveur des panneaux JSF live
    if (!on && el.getAttribute("data-live") !== "1") {
      el.classList.remove("has-file", "is-checked", "is-corrected", "is-done", "is-busy", "has-errors");
    }
  });
}

function boReset(p) {
  if (p) p.classList.remove("has-file", "is-checked", "is-corrected", "is-done", "is-busy", "has-errors");
}

/** Applique les classes d'état renvoyées par le bean après une action AJAX. */
function boSyncPanel(obj, op, cssClasses) {
  const panel = document.querySelector(`.bo-panel[data-obj="${CSS.escape(obj)}"][data-op="${CSS.escape(op)}"]`);
  if (!panel) return;
  const keepOn = panel.classList.contains("is-on");
  panel.className = ("bo-panel " + (cssClasses || "")).trim();
  if (keepOn) panel.classList.add("is-on");
  panel.setAttribute("data-live", "1");
  panel.setAttribute("data-obj", obj);
  panel.setAttribute("data-op", op);
  if (panel.classList.contains("has-errors")) {
    panel.classList.add("bo-shake");
    setTimeout(() => panel.classList.remove("bo-shake"), 420);
  }
}
window.boSyncPanel = boSyncPanel;

function boAjaxBusy(data) {
  if (!data) return;
  const src = data.source;
  const panel = src && src.closest ? src.closest(".bo-panel") : null;
  if (data.status === "begin" && panel) panel.classList.add("is-busy");
  if (data.status === "success" || data.status === "complete") {
    if (panel) panel.classList.remove("is-busy");
    // Relire un éventuel marqueur d'état dans le fragment mis à jour
    document.querySelectorAll(".bo-state[data-cls]").forEach((el) => {
      const o = el.getAttribute("data-obj");
      const p = el.getAttribute("data-op");
      const cls = el.getAttribute("data-cls") || "";
      if (o && p) boSyncPanel(o, p, cls);
    });
  }
}
window.boAjaxBusy = boAjaxBusy;

function bindBoDropZones() {
  document.querySelectorAll(".bo-panel[data-live='1'] .bo-drop-empty").forEach((zone) => {
    if (zone.dataset.dropBound === "1") return;
    zone.dataset.dropBound = "1";
    const inputId = zone.getAttribute("for");
    const input = inputId ? document.getElementById(inputId) : null;
    if (!input) return;
    ["dragenter", "dragover"].forEach((ev) => {
      zone.addEventListener(ev, (e) => {
        e.preventDefault();
        zone.classList.add("is-drag");
      });
    });
    ["dragleave", "drop"].forEach((ev) => {
      zone.addEventListener(ev, (e) => {
        e.preventDefault();
        zone.classList.remove("is-drag");
      });
    });
    zone.addEventListener("drop", (e) => {
      const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
      if (!file) return;
      const dt = new DataTransfer();
      dt.items.add(file);
      input.files = dt.files;
      input.dispatchEvent(new Event("change", { bubbles: true }));
    });
  });
}
window.bindBoDropZones = bindBoDropZones;
document.addEventListener("DOMContentLoaded", bindBoDropZones);
if (window.faces && faces.ajax) {
  faces.ajax.addOnEvent(function (data) {
    if (data.status === "success") bindBoDropZones();
  });
} else if (window.jsf && jsf.ajax) {
  jsf.ajax.addOnEvent(function (data) {
    if (data.status === "success") bindBoDropZones();
  });
}

function closeConceptBlockOverlay() {
  const ov = $("#cblockOverlay");
  if (!ov) return;
  ov.hidden = true;
  ov.setAttribute("aria-hidden", "true");
  const body = ov.querySelector(".block-modal-body");
  if (body) body.innerHTML = "";
}

function expandConceptBlock(gear) {
  const block = gear && gear.closest(".cblock");
  if (!block) return;
  const titleNode = block.querySelector(".cblock-head");
  const body = block.querySelector(".cblock-body");
  if (!body) return;
  let ov = $("#cblockOverlay");
  if (!ov) {
    ov = document.createElement("div");
    ov.id = "cblockOverlay";
    ov.className = "block-overlay";
    ov.innerHTML = '<div class="block-modal" role="dialog" aria-modal="true">'
      + '<div class="block-modal-head"><span class="cblock-overlay-title"></span>'
      + '<button type="button" data-act="cblock-collapse" title="Fermer" aria-label="Fermer">×</button></div>'
      + '<div class="block-modal-body"></div></div>';
    ov.addEventListener("click", function (e) {
      if (e.target === ov) closeConceptBlockOverlay();
    });
    document.body.appendChild(ov);
  }
  const title = ov.querySelector(".cblock-overlay-title");
  const dest = ov.querySelector(".block-modal-body");
  if (title) {
    title.textContent = (titleNode && titleNode.childNodes[0] && titleNode.childNodes[0].textContent || "").trim();
  }
  if (dest) dest.replaceChildren(body.cloneNode(true));
  ov.hidden = false;
  ov.setAttribute("aria-hidden", "false");
}
