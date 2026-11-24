/** Fiche — corpus, branche et navigation. */
let conceptExtrasCtrl = null;
let conceptExtrasToken = 0;
let corpusLinksCtrl = null;
let corpusLinksToken = 0;

function conceptExtrasCurrent(token, conceptId) {
  if (token !== conceptExtrasToken) return false;
  const state = document.getElementById("previewDetailState");
  return !!(state && state.getAttribute("data-id") === conceptId);
}

function escapeConceptExtra(value) {
  return String(value == null ? "" : value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function safeConceptExtraHref(uri) {
  try {
    const url = new URL(uri, location.origin);
    if (url.protocol === "http:" || url.protocol === "https:") return url.href;
  } catch (err) {
    /* ignore */
  }
  return "";
}

function branchCountText(count) {
  const n = Number(count);
  const value = Number.isFinite(n) ? n : 0;
  return String(value);
}

function calcButtonIconHtml() {
  return '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">'
    + '<path d="M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"/>'
    + '<path d="M8 7h8v3H8z"/>'
    + '<path d="M8 14h.01"/><path d="M12 14h.01"/><path d="M16 14h.01"/>'
    + '<path d="M8 17h.01"/><path d="M12 17h.01"/><path d="M16 17h.01"/>'
    + "</svg>";
}

function branchCalcButtonHtml() {
  return '<button type="button" class="bo-btn ghost sm cv-corpus-calc" data-act="branch-calc">'
    + calcButtonIconHtml() + "Calculer</button>";
}

function branchLoadingHtml() {
  return '<span class="cv-branch-result is-loading" role="status" aria-live="polite" aria-busy="true">'
    + "Calcul en cours…</span>";
}

function paintBranchTotal(count) {
  const text = escapeConceptExtra(branchCountText(count));
  document.querySelectorAll("#previewDetailState [data-cv-extra='branch']").forEach((box) => {
    box.innerHTML = '<span class="cv-branch-result">' + text + "</span>";
  });
}

function paintBranchTotalFailed() {
  document.querySelectorAll("#previewDetailState [data-cv-extra='branch']").forEach((box) => {
    box.innerHTML = branchCalcButtonHtml()
      + '<span class="cv-corpus-error">Le calcul n\'a pas abouti. Réessayez.</span>';
  });
}

function corpusLoadingHtml() {
  return '<span class="cv-corpus-wait" role="status" aria-live="polite" aria-busy="true">Calcul en cours…</span>';
}

function revealCorpusBlock(box) {
  const section = box && box.closest ? box.closest('[data-card-id="usage"]') : null;
  if (!section) return;
  section.classList.remove("is-sparse", "is-collapsed");
  const fold = section.querySelector(":scope > .cblock-head .cblock-fold");
  if (fold) fold.setAttribute("aria-expanded", "true");
}
function corpusHost(href) {
  try {
    return new URL(href).host;
  } catch (err) {
    return "";
  }
}

function corpusLinksHtml(links) {
  const rows = Array.isArray(links) ? links : [];
  if (!rows.length) return '<span class="muted">Aucun</span>';
  return rows.map((link) => {
    const name = escapeConceptExtra(link && link.name) || "Source";
    const href = safeConceptExtraHref(link && link.uri);
    const host = escapeConceptExtra(corpusHost(href));
    const count = Number(link && link.count);
    let metric = "";
    if (link && link.onlyUri) {
      metric = '<span class="cv-corpus-open">Ouvrir</span>';
    } else if (Number.isFinite(count) && count > 0) {
      metric = '<span class="cv-corpus-count">' + escapeConceptExtra(String(count)) + "</span>";
    }
    const body = '<span class="cv-corpus-name">' + name + "</span>"
      + (host ? '<span class="cv-corpus-host">' + host + "</span>" : "")
      + metric;
    if (!href) return '<span class="cv-corpus-item">' + body + "</span>";
    return '<a class="cv-corpus-item" href="' + escapeConceptExtra(href) + '" target="_blank" rel="noopener">'
      + body + "</a>";
  }).join("");
}

function paintCorpusLinks(links) {
  const html = corpusLinksHtml(links);
  document.querySelectorAll("#previewDetailState [data-cv-extra='corpus']").forEach((box) => {
    revealCorpusBlock(box);
    box.innerHTML = html;
  });
}

function paintCorpusLinksFailed() {
  document.querySelectorAll("#previewDetailState [data-cv-extra='corpus']").forEach((box) => {
    revealCorpusBlock(box);
    box.innerHTML = '<span class="cv-corpus-error">Le calcul n\'a pas abouti. Réessayez.</span>';
  });
}

function corpusLinksQuery(conceptId) {
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  if (!theso || !conceptId) return "";
  const params = new URLSearchParams({ thesaurusId: theso, conceptId: conceptId });
  const lang = typeof thesaurusLang === "function" ? thesaurusLang() : "";
  if (lang) params.set("lang", lang);
  return params.toString();
}

function abortCorpusLinks() {
  if (corpusLinksCtrl) corpusLinksCtrl.abort();
  corpusLinksCtrl = null;
}

function corpusLinksCurrent(token, conceptId) {
  if (token !== corpusLinksToken) return false;
  const state = document.getElementById("previewDetailState");
  return !!(state && state.getAttribute("data-id") === conceptId);
}

function loadCorpusLinks() {
  const state = document.getElementById("previewDetailState");
  if (!state) return;
  const conceptId = state.getAttribute("data-id") || "";
  const kind = (state.getAttribute("data-kind") || "").toLowerCase();
  if (!conceptId || (kind !== "concept" && kind !== "candidat")) return;
  const query = corpusLinksQuery(conceptId);
  if (!query) return;
  const box = state.querySelector("[data-cv-extra='corpus']");
  if (!box || box.querySelector("[aria-busy='true']")) return;
  revealCorpusBlock(box);
  box.innerHTML = corpusLoadingHtml();
  abortCorpusLinks();
  const ctrl = new AbortController();
  corpusLinksCtrl = ctrl;
  const token = ++corpusLinksToken;
  const ctx = document.body.getAttribute("data-ctx") || "";
  fetch(ctx + "/api/concepts/corpus-links?" + query, {
    headers: { Accept: "application/json" },
    signal: ctrl.signal
  })
    .then((res) => {
      if (!res.ok) throw new Error("http");
      return res.json();
    })
    .then((data) => {
      if (!corpusLinksCurrent(token, conceptId)) return;
      paintCorpusLinks(data && data.links);
    })
    .catch((err) => {
      if (err && err.name === "AbortError") return;
      if (!corpusLinksCurrent(token, conceptId)) return;
      paintCorpusLinksFailed();
    })
    .finally(() => {
      if (token !== corpusLinksToken) return;
      corpusLinksCtrl = null;
    });
}

function abortBranchCount() {
  if (conceptExtrasCtrl) conceptExtrasCtrl.abort();
  conceptExtrasCtrl = null;
}

function loadBranchTotal() {
  const state = document.getElementById("previewDetailState");
  if (!state) return;
  const conceptId = state.getAttribute("data-id") || "";
  const kind = (state.getAttribute("data-kind") || "").toLowerCase();
  if (!conceptId || (kind !== "concept" && kind !== "candidat")) return;
  const query = corpusLinksQuery(conceptId);
  if (!query) return;
  const box = state.querySelector("[data-cv-extra='branch']");
  if (!box || box.querySelector("[aria-busy='true']")) return;
  box.innerHTML = branchLoadingHtml();
  abortBranchCount();
  const ctrl = new AbortController();
  conceptExtrasCtrl = ctrl;
  const token = ++conceptExtrasToken;
  const ctx = document.body.getAttribute("data-ctx") || "";
  fetch(ctx + "/api/concepts/branch-count?" + query, {
    headers: { Accept: "application/json" },
    signal: ctrl.signal
  })
    .then((res) => {
      if (!res.ok) throw new Error("http");
      return res.json();
    })
    .then((data) => {
      if (!conceptExtrasCurrent(token, conceptId)) return;
      paintBranchTotal(data && data.count);
    })
    .catch((err) => {
      if (err && err.name === "AbortError") return;
      if (!conceptExtrasCurrent(token, conceptId)) return;
      paintBranchTotalFailed();
    })
    .finally(() => {
      if (token !== conceptExtrasToken) return;
      conceptExtrasCtrl = null;
    });
}

window.loadCorpusLinks = loadCorpusLinks;
window.loadBranchTotal = loadBranchTotal;

function finishLiveOpen() {
  const live = $("#viewLive");
  if (!live) return;
  live.classList.remove("is-loading", "is-opening", "is-optimistic");
  live.classList.add("is-ready");
  live.removeAttribute("aria-busy");
  const bar = $("#liveProgress");
  if (bar) bar.setAttribute("aria-hidden", "true");
  abortBranchCount();
  abortCorpusLinks();
  requestAnimationFrame(() => {
    if (window.applyConceptCardOrder) window.applyConceptCardOrder();
    if (window.syncViewRail) window.syncViewRail();
    applyConceptLabelUi();
    restoreFicheScroll();
    if (typeof window.runPendingCreate === "function") window.runPendingCreate();
  });
}

function markCandRowOpen(id) {
  $$(".cand-row").forEach((row) => {
    const on = !!(id && row.getAttribute("data-id") === id);
    row.classList.toggle("is-open", on);
    if (on) row.setAttribute("aria-current", "page");
    else row.removeAttribute("aria-current");
  });
}

function syncConceptUrl(id, nodeType) {
  if (SCREEN !== "accueil" && SCREEN !== "consultation" && SCREEN !== "graphe") return;
  try {
    const url = new URL(location.href);
    if (SCREEN === "accueil") {
      const params = new URLSearchParams();
      const idt = currentConceptParam("idt");
      if (idt) params.set("idt", idt);
      if (id) params.set("idc", id);
      if (nodeType && nodeType !== "concept") params.set("type", nodeType);
      if (!params.get("idt") && !params.get("idc")) return;
      const next = conceptPathFromParams(params) + url.hash;
      if (next !== location.pathname + location.search + location.hash) {
        history.replaceState({ conceptId: id || "", type: nodeType || "" }, "", next);
      }
      return;
    }
    if (id) {
      url.searchParams.set("id", id);
      url.searchParams.delete("idc");
      if (nodeType && nodeType !== "concept") url.searchParams.set("type", nodeType);
      else url.searchParams.delete("type");
    } else {
      url.searchParams.delete("id");
      url.searchParams.delete("idc");
      url.searchParams.delete("type");
    }
    const next = url.pathname + url.search + url.hash;
    if (next !== location.pathname + location.search + location.hash) {
      history.replaceState({ conceptId: id || "", type: nodeType || "" }, "", next);
    }
  } catch (err) {
    /* ignore */
  }
}

function encPathParam(value) {
  return encodeURIComponent(value == null ? "" : String(value));
}

function currentConceptParam(name) {
  try {
    const fromQuery = new URL(location.href).searchParams.get(name);
    if (fromQuery) return fromQuery;
  } catch (err) {
    /* ignore */
  }
  let raw = location.pathname || "";
  const idtAt = raw.indexOf("/idt=");
  const idcAt = raw.indexOf("/idc=");
  const cut = idtAt >= 0 && idcAt >= 0 ? Math.min(idtAt, idcAt) : Math.max(idtAt, idcAt);
  raw = cut >= 0 ? raw.slice(cut + 1) : raw.replace(/^\//, "");
  const parts = raw.split("&");
  for (let i = 0; i < parts.length; i++) {
    const eq = parts[i].indexOf("=");
    if (eq <= 0) continue;
    let key = parts[i].slice(0, eq);
    let value = parts[i].slice(eq + 1);
    try {
      key = decodeURIComponent(key);
      value = decodeURIComponent(value.replace(/\+/g, " "));
    } catch (err) {
      /* keep raw */
    }
    if (key === name) return value;
  }
  return "";
}

function appRootPath() {
  let path = location.pathname || "/";
  const idtAt = path.indexOf("/idt=");
  const idcAt = path.indexOf("/idc=");
  const cut = idtAt >= 0 && idcAt >= 0 ? Math.min(idtAt, idcAt) : Math.max(idtAt, idcAt);
  if (cut >= 0) path = cut === 0 ? "/" : path.slice(0, cut);
  if (path.endsWith("/index.xhtml")) path = path.slice(0, -"/index.xhtml".length) || "/";
  else if (path.endsWith("/index")) path = path.slice(0, -"/index".length) || "/";
  if (path.length > 1 && path.endsWith("/")) path = path.slice(0, -1);
  return path === "/" ? "" : path;
}

/** Accueil thésaurus : /idt=th217&idc=1895753 */
function conceptPathFromParams(params) {
  const parts = [];
  const idt = params.get("idt");
  const idc = params.get("idc");
  if (idt) parts.push("idt=" + encPathParam(idt));
  if (idc) parts.push("idc=" + encPathParam(idc));
  ["type", "create", "pref", "_", "_r"].forEach((key) => {
    const value = params.get(key);
    if (value) parts.push(key + "=" + encPathParam(value));
  });
  if (!parts.length) return appRootPath() + "/";
  return appRootPath() + "/" + parts.join("&");
}

function conceptPageHref(query) {
  const params = new URLSearchParams(String(query || "").replace(/^\?/, ""));
  if (!params.get("idt")) {
    const idt = currentConceptParam("idt");
    if (idt) params.set("idt", idt);
  }
  return conceptPathFromParams(params);
}

function sanitizeFacesFormAction(form) {
  if (!form) return;
  const raw = form.getAttribute("action");
  if (!raw) return;
  try {
    const url = new URL(raw, location.href);
    const path = url.pathname || "";
    if (path.indexOf("/idt=") >= 0 || path.indexOf("/idc=") >= 0) {
      url.pathname = appRootPath() + "/index.xhtml";
      url.search = "";
      form.setAttribute("action", url.pathname);
      return;
    }
    if (!url.searchParams.has("id") && !url.searchParams.has("idc") && !url.searchParams.has("type")) {
      return;
    }
    url.searchParams.delete("id");
    url.searchParams.delete("idc");
    url.searchParams.delete("type");
    form.setAttribute("action", url.pathname + url.search + url.hash);
  } catch (err) {
    /* ignore */
  }
}

function openLiveDetail(id, nodeType, label) {
  const idEl = document.getElementById("previewOpenForm:openId");
  const typeEl = document.getElementById("previewOpenForm:openType");
  const btn = document.getElementById("previewOpenForm:openBtn");
  if (!id || !idEl || !btn) return false;
  idEl.value = id;
  if (typeEl) typeEl.value = nodeType || "";
  sanitizeFacesFormAction(btn.form);
  state.pendingOpenId = id;
  state.pendingOpenAt = Date.now();
  if (!graphOnScreen()) {
    paintOptimisticFiche(id, label);
    beginLiveOpen();
  }
  markCandRowOpen(id);
  btn.click();
  return true;
}

function fromCandList() {
  return SCREEN === "candidats"
      || new URLSearchParams(location.search).get("from") === "candidats";
}

function fromPropList() {
  return SCREEN === "propositions"
      || SCREEN === "proposition-review"
      || new URLSearchParams(location.search).get("from") === "propositions";
}

function paintListBack() {
  const liveDetail = liveDetailRequested() && !state.home && !state.draft;
  const prop = $("#liveBackPropList");
  if (prop) prop.hidden = !(fromPropList() && liveDetail);
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
  if (state.conceptDraft || state.facetDraft || state.draft) return false;
  if (state.view === "collection" && state.colId) return false;
  const el = liveDetailState();
  if (!el || el.getAttribute("data-requested") !== "1") return false;
  const shownId = el.getAttribute("data-id") || "";
  const superseded = graphOnScreen() && state.pendingOpenId && shownId !== state.pendingOpenId
      && Date.now() - state.pendingOpenAt < 10000;
  /* Réponse d'un clic antérieur : la sphère est déjà sur le concept suivant. */
  if (superseded) return false;
  state.pendingOpenId = "";
  state.home = false;
  state.draft = false;
  state.conceptId = shownId || state.conceptId;
  syncConceptUrl(state.conceptId, el.getAttribute("data-kind") || "");
  highlightConcept(state.conceptId);
  selectOpenedInTree(state.conceptId);
  paintGraphBack();
  paintListBack();
  if (graphOnScreen()) {
    return false;
  }
  showPanel(".view-panel", "viewLive");
  finishLiveOpen();
  if (typeof closeSidebarDrawer === "function") closeSidebarDrawer();
  const view = $("#previewView");
  if (view && !ficheScrollLock.held) view.scrollTop = 0;
  else restoreFicheScroll();
  return true;
}

function paintGraphBack() {
  const btn = $("#liveBackGraph");
  if (!btn) return;
  const show = graphMode() && !state.graphFront;
  btn.hidden = !show;
}

function backToGraph() {
  state.graphFront = true;
  state.home = false;
  state.draft = false;
  paint();
}

function paintMain() {
  const settings = $("#viewSettings");
  if (settings && !IS_CONSULT) {
    showPanel(".view-panel", "viewSettings");
    return;
  }
  if (state.collectionDraft && $("#viewCollectionDraft")) {
    showPanel(".view-panel", "viewCollectionDraft");
    paintGraphBack();
    paintListBack();
    const add = $("#viewAdd");
    if (add) add.setAttribute("aria-expanded", "true");
    return;
  }
  if (state.conceptDraft && $("#viewConceptDraft")) {
    showPanel(".view-panel", "viewConceptDraft");
    paintGraphBack();
    paintListBack();
    return;
  }
  if (state.facetDraft && $("#viewFacetDraft")) {
    showPanel(".view-panel", "viewFacetDraft");
    paintGraphBack();
    paintListBack();
    return;
  }
  if (state.draft && $("#viewDraft")) {
    showPanel(".view-panel", "viewDraft");
    paintGraphBack();
    paintListBack();
    return;
  }
  if (typeof bulkSelectionActive === "function" && bulkSelectionActive() && $("#viewBulk")) {
    showPanel(".view-panel", "viewBulk");
    paintGraphBack();
    paintListBack();
    return;
  }
  const v = state.view;
  let id;
  if (v === "collection") {
    if (state.colId) id = "viewCollection";
    else if (liveDetailRequested() && !state.home && !state.draft && state.conceptId) id = "viewLive";
    else id = "viewCollection";
  } else if (graphOnScreen()) {
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
    if (state.conceptId && liveDetailRequested()) id = "viewLive";
    else if (!rankHits(state.committed).length) id = "viewNoResults";
    else id = state.conceptId ? "viewConcept" : "viewEmpty";
  } else if (v === "tableau") {
    id = state.conceptId ? "viewConcept" : "viewEmpty";
  } else if (!state.conceptId && $("#viewHome") && (state.home || v === "arbo")) {
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
  if (graphOnScreen()) ensureGlobe();
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
    $$("#voColSortRow .vo-sort-b").forEach(b => {
      b.classList.toggle("is-on", b.getAttribute("data-sort") === state.colSort);
    });
  }
  const searchOn = state.view === "recherche";
  ["voSearchOpts", "voPathRow", "voHlRow"].forEach(id => {
    const el = $("#" + id); if (el) el.hidden = !searchOn;
  });
  const tbl = $("#voTableOpts");
  if (tbl) {
    tbl.hidden = state.view !== "tableau";
    if (state.view === "tableau") applyTableCols();
  }
  const stk = $("#voStatusOpts");
  if (stk) stk.hidden = !(state.view === "arbo" || state.view === "tableau" || state.view === "hyper");
  $("#thIdBtn") && $("#thIdBtn").classList.toggle("is-active", SCREEN === "accueil");
  paintBadges();
}

function paintBadges() {
  const n = state.searchTotal > 0 ? state.searchTotal : rankHits(state.committed).length;
  ["viewPickBadge", "viewMenuBadge"].forEach(id => {
    const el = $("#" + id);
    if (!el) return;
    el.textContent = String(n);
    el.hidden = n === 0;
  });
}

function paint() {
  if (SCREEN === "synchronisation" || SCREEN === "portail") {
    return;
  }
  paintViewPick();
  paintMain();
  paintSidebar();
  updateBulk();
}

function setView(view) {
  if (!view) return;
  if (view === "hyper" && !$("#viewHyper")) {
    go(graphPageUrl());
    return;
  }
  if (!IS_CONSULT && view !== "arbo" && view !== "hyper" && view !== "recherche") {
    go("thesaurus/consultation.xhtml?view=" + encodeURIComponent(view));
    return;
  }
  if (!IS_CONSULT && view === "arbo" && !$("#viewHome")) {
    const cid = SCREEN === "graphe" ? state.conceptId : "";
    go("index.xhtml" + (cid ? "?idc=" + encodeURIComponent(cid) : ""));
    return;
  }
  state.graphSearch = false;
  state.collectionDraft = false;
  state.view = view;
  if (view === "hyper") state.graphFront = true;
  if (view !== "arbo" && view !== "hyper" && view !== "tableau") {
    clearSelection(true);
  }
  if (view === "arbo" && !state.conceptId) {
    state.home = true;
  }
  if (view === "recherche") {
    syncRechercheConcept();
    paintCommittedResults();
  }
  $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
  if (view === "hyper" && typeof closeSidebarDrawer === "function") closeSidebarDrawer();
  paint();
}

function rememberTreeFocus(tn, id) {
  const nodeId = id || (tn && tn.getAttribute && tn.getAttribute("data-id")) || "";
  state.treeFocusId = nodeId;
  state.treeFocusKey = tn && tn.getAttribute ? (tn.getAttribute("data-key") || "") : "";
}

function findTreeNodeById(id) {
  if (!id) return null;
  const key = state.treeFocusId === id ? (state.treeFocusKey || "") : "";
  const boxes = [treePanel(), $("#panelTree"), $("#panelCollectionTree")].filter(Boolean);
  const seen = new Set();
  let fallback = null;
  for (let i = 0; i < boxes.length; i++) {
    const box = boxes[i];
    if (seen.has(box)) continue;
    seen.add(box);
    const nodes = box.querySelectorAll(".tn[data-id]");
    for (let j = 0; j < nodes.length; j++) {
      const tn = nodes[j];
      if (tn.getAttribute("data-id") !== id) continue;
      if (key && tn.getAttribute("data-key") === key) return tn;
      if (!fallback) fallback = tn;
    }
  }
  return fallback;
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
      const reduce = typeof prefersReducedMotion === "function" && prefersReducedMotion();
      row.scrollIntoView({
        block: state.view === "hyper" ? "center" : "nearest",
        behavior: reduce ? "auto" : "smooth"
      });
    }
    if (tn.closest("#panelCollectionTree") || tn.closest("[data-status-forest]")) {
      let walk = previousTreeParent(tn);
      while (walk) {
        walk.classList.add("is-open");
        walk = previousTreeParent(walk);
      }
      if (tn.closest("#panelCollectionTree") && typeof paintCollectionVisibility === "function") {
        paintCollectionVisibility();
      } else if (typeof paintStatusForestVisibility === "function") {
        paintStatusForestVisibility(tn.parentElement);
      }
    } else if (typeof syncConceptTreeCarets === "function") {
      syncConceptTreeCarets();
    }
  }
  if (state.view === "tableau") revealTableConcept(id);
  if (graphOnScreen()) globeSelectId(graphConceptId());
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
  state.conceptDraft = false;
  state.facetDraft = false;
  state.collectionDraft = false;
  state.conceptId = id;
  if (mode !== "stay") state.view = "arbo";
  syncConceptUrl(id, "");
  highlightConcept(id);
  closeSearchUi();
  paint();
}

function openHome() {
  if (IS_CONSULT || SCREEN === "accueil") {
    state.home = true;
    state.conceptId = null;
    state.draft = false;
    state.conceptDraft = false;
    state.facetDraft = false;
    state.view = "arbo";
    highlightConcept(null);
    syncConceptUrl("", "");
    paint();
    return;
  }
  go("index.xhtml");
}

