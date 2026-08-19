/**
 * Effets uniquement — le DOM est dans les pages HTML.
 * Comportement calqué sur le prototype React (target.xhtml / app.jsx).
 */
(function () {
  "use strict";

  const $ = (s, r) => (r || document).querySelector(s);
  const $$ = (s, r) => Array.from((r || document).querySelectorAll(s));
  const norm = (s) => (s || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
  const SCREEN = document.body.getAttribute("data-page") || "consultation";
  const IS_CONSULT = SCREEN === "consultation";

  let settingsDraftDirty = false;
  let settingsBaseline = "";
  let settingsAllowLeave = false;
  let settingsLeaveAction = null;

  function settingsForm() {
    return $("#previewPrefForm");
  }

  function serializeSettingsForm() {
    const form = settingsForm();
    if (!form) return "";
    const parts = [];
    form.querySelectorAll("input, select, textarea").forEach((el) => {
      if (!el.name) return;
      if (el.closest("#previewCorpusBox, #previewAlignBox, #stSaveConfirm, #stLeaveConfirm")) return;
      if (el.classList.contains("st-jsf-go")) return;
      if (el.type === "button" || el.type === "submit") return;
      if (el.type === "hidden" && /ViewState|csrfToken|previewCorpus|previewAlign/i.test(el.id || el.name || "")) return;
      if ((el.type === "checkbox" || el.type === "radio") && !el.checked) return;
      if (el.type === "password" && !el.value) return;
      parts.push(el.name + "=" + (el.value || "on"));
    });
    parts.sort();
    return parts.join("\n");
  }

  function isSettingsDirty() {
    if (SCREEN !== "preference" || !settingsForm()) return false;
    return settingsDraftDirty || serializeSettingsForm() !== settingsBaseline;
  }

  function markSettingsDraft() {
    settingsDraftDirty = true;
  }

  function rememberSettingsBaseline() {
    settingsDraftDirty = false;
    settingsBaseline = serializeSettingsForm();
  }

  function allowSettingsLeave() {
    settingsAllowLeave = true;
  }

  function isHashOnlyNavigation(url) {
    if (!url) return false;
    const raw = String(url);
    if (raw.charAt(0) === "#") return true;
    try {
      const next = new URL(raw, location.href);
      return next.origin === location.origin
        && next.pathname === location.pathname
        && next.search === location.search
        && !!next.hash;
    } catch (ex) {
      return false;
    }
  }

  function askLeaveThen(proceed) {
    if (settingsAllowLeave || !isSettingsDirty()) return false;
    settingsLeaveAction = proceed;
    showConfirm("#stLeaveConfirm");
    return true;
  }

  function dismissSettingsLeave() {
    hideConfirm("#stLeaveConfirm");
    settingsLeaveAction = null;
  }

  function confirmSettingsLeave() {
    hideConfirm("#stLeaveConfirm");
    const action = settingsLeaveAction;
    settingsLeaveAction = null;
    if (typeof action === "function") action();
  }

  function go(url) {
    if (askLeaveThen(() => {
      allowSettingsLeave();
      location.href = url;
    })) return;
    location.href = url;
  }

  const PAGE = 12;
  const SIDE = {
    arbo: "panelTree", tableau: "panelTable", hyper: "panelTree",
    collection: "panelTree", recherche: "panelResults"
  };
  const VIEW_LABEL = {
    arbo: "Arborescence", tableau: "Tableau", hyper: "Graphe",
    collection: "Collection", recherche: "Recherche"
  };
  const GROUPS = {
    actif: ["valide", "insere"],
    candidat: ["candidat"],
    inactif: ["rejete", "deprecie"]
  };

  const state = {
    view: "arbo",
    home: true,
    conceptId: null,
    draft: false,
    committed: "",
    selected: new Set(),
    subtreeSize: new Map(),
    subtreeSizePending: new Map(),
    statusSet: new Set(["valide", "insere", "candidat"]),
    candBy: "",
    candFrom: "",
    candTo: "",
    sort: "alpha",
    acIdx: -1,
    resultLimit: PAGE,
    tblSort: "concept",
    tblDir: 1,
    tblCols: new Set(["status", "type", "notation", "path"]),
    showPath: true,
    highlight: true,
    density: "regular",
    moveTarget: null
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
    if (type === "candidat" || status === "candidat") return "candidat";
    return type || "concept";
  }

  function openLiveDetail(id, nodeType) {
    const idEl = document.getElementById("previewOpenForm:openId");
    const typeEl = document.getElementById("previewOpenForm:openType");
    const btn = document.getElementById("previewOpenForm:openBtn");
    if (!id || !idEl || !btn) return false;
    idEl.value = id;
    if (typeEl) typeEl.value = nodeType || "";
    btn.click();
    return true;
  }

  function showLiveDetail() {
    const el = liveDetailState();
    if (!el || el.getAttribute("data-requested") !== "1") return false;
    state.home = false;
    state.draft = false;
    state.conceptId = el.getAttribute("data-id") || state.conceptId;
    highlightConcept(state.conceptId);
    showPanel(".view-panel", "viewLive");
    const view = $("#previewView");
    if (view) view.scrollTop = 0;
    return true;
  }

  function paintMain() {
    const v = state.view;
    let id;
    if (liveDetailRequested() && !state.home && !state.draft) {
      id = "viewLive";
    } else if (v === "hyper" && $("#viewHyper")) {
      id = "viewHyper";
    } else if (!IS_CONSULT) {
      if (v === "arbo" && $("#viewHome")) id = "viewHome";
      else return;
    } else if (v === "collection") {
      id = "viewCollection";
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
    const nq = $("#nrQuery");
    if (nq) nq.textContent = state.committed;
  }

  function paintSidebar() {
    if (state.view === "collection") {
      $$(".sidebar-panel").forEach(el => el.classList.remove("is-on"));
    } else {
      showPanel(".sidebar-panel", SIDE[state.view] || "panelTree");
    }
    const sb = $("#panelTree");
    if (sb) sb.classList.toggle("sb-tree", state.view === "arbo" || state.view === "hyper");
    document.body.classList.toggle("hide-path", !state.showPath);
    document.body.classList.toggle("density-compact", state.density === "compact");
  }

  function paintViewPick() {
    const pick = $("#viewPick");
    if (pick) pick.setAttribute("data-view", state.view);
    $$(".view-pick-item").forEach(b => b.classList.toggle("is-on", b.getAttribute("data-view") === state.view));
    const vo = $("#voPopView");
    if (vo) vo.textContent = VIEW_LABEL[state.view] || state.view;
    const sortRow = $("#voSortRow");
    if (sortRow) sortRow.hidden = state.view !== "arbo" && state.view !== "hyper";
    const searchOn = state.view === "recherche";
    ["voSearchOpts", "voPathRow", "voHlRow"].forEach(id => {
      const el = $("#" + id); if (el) el.hidden = !searchOn;
    });
    const tbl = $("#voTableOpts");
    if (tbl) tbl.hidden = state.view !== "tableau";
    const stk = $(".vo-pop-stk");
    if (stk) stk.hidden = !(state.view === "arbo" || state.view === "tableau" || state.view === "hyper");
    $("#thIdBtn") && $("#thIdBtn").classList.toggle("is-active", SCREEN === "accueil");
    const gear = $("#voWrap");
    if (gear) gear.hidden = state.view === "collection";
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
    paintSidebar();
    paintMain();
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

  function highlightConcept(id) {
    $$(".tn-row.is-active").forEach(r => r.classList.remove("is-active"));
    $$("#panelTable tr.is-active").forEach(r => r.classList.remove("is-active"));
    $$(".rl-item.is-sel").forEach(r => r.classList.remove("is-sel"));
    $$(".hyper-dot.is-active").forEach(d => d.classList.remove("is-active"));
    $$("#viewConcept .cv").forEach(el => el.classList.toggle("is-on", el.getAttribute("data-id") === id));
    if (!id) return;
    const btn = $(`.tn-label[data-id="${CSS.escape(id)}"]`);
    if (btn) {
      const row = btn.closest(".tn-row");
      if (row) {
        row.classList.add("is-active");
        row.scrollIntoView({ block: "nearest" });
      }
      let tn = btn.closest(".tn");
      while (tn) {
        tn.classList.add("is-open");
        tn = previousTreeParent(tn);
      }
    }
    const tr = $(`#panelTable tr[data-id="${CSS.escape(id)}"]`);
    if (tr) tr.classList.add("is-active");
    const rl = $(`.rl-item[data-id="${CSS.escape(id)}"]`);
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
      const nameEl = $("#draftParentName");
      if (nameEl) nameEl.textContent = parentName;
      const pathEl = $("#draftPathLabel");
      if (pathEl) pathEl.textContent = pathKey.replace(/\//g, " › ");
      const bt = $("#draftBt");
      if (bt) bt.value = parentName;
      const coll = $("#draftColl");
      if (coll) coll.value = pathKey ? pathKey.split("/")[0] : "";
      ["draftTitle", "draftAlts", "draftNt", "draftRt", "draftTrFr", "draftTrEn", "draftTrDe", "draftTrEs", "draftTrIt", "draftDef", "draftScope"].forEach(id => {
        const el = $("#" + id);
        if (el) el.value = "";
      });
    }
    highlightConcept(null);
    $("#searchBox") && $("#searchBox").classList.remove("is-open");
    showPanel(".view-panel", "viewDraft");
    requestAnimationFrame(() => { const t = $("#draftTitle"); if (t) t.focus(); });
  }

  function resolveDraft(kind) {
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
      if (!on) el.classList.remove("has-file", "is-checked", "is-corrected", "is-done");
    });
  }

  function boReset(p) {
    if (p) p.classList.remove("has-file", "is-checked", "is-corrected", "is-done");
  }

  function scoreEl(el, q) {
    if (!q) return { score: -1 };
    const pref = el.getAttribute("data-pref") || "";
    if (pref === q) return { score: 0 };
    if (pref.startsWith(q)) return { score: 1 };
    if (pref.includes(q)) return { score: 2 };
    const alts = (el.getAttribute("data-alts") || "").split("|").filter(Boolean);
    const alt = alts.find(a => a.includes(q));
    if (alt) return { score: 3, via: "syn", viaQ: alt };
    const def = el.getAttribute("data-def") || "";
    if (def.includes(q)) return { score: 5, via: "note" };
    return { score: -1 };
  }

  function rankHits(query) {
    const q = norm((query || "").trim());
    if (!q) return [];
    return $$("#resultsList .rl-item").map(el => {
      const sc = scoreEl(el, q);
      return sc.score < 0 ? null : {
        el, id: el.getAttribute("data-id"),
        score: sc.score, via: sc.via, viaQ: sc.viaQ,
        pref: el.getAttribute("data-pref-label") || ""
      };
    }).filter(Boolean).sort((a, b) => a.score - b.score || a.pref.localeCompare(b.pref, "fr"));
  }

  function shownAcRows() {
    return $$("#ac .ac-row.is-shown");
  }

  function rangeForQuery(textNode, rawQuery) {
    const text = textNode && textNode.nodeValue;
    const nq = norm((rawQuery || "").trim());
    if (!text || !nq) return null;
    let folded = "";
    const origAt = [];
    for (let i = 0; i < text.length; i++) {
      const piece = norm(text[i]);
      for (let k = 0; k < piece.length; k++) origAt.push(i);
      folded += piece;
    }
    const at = folded.indexOf(nq);
    if (at < 0 || origAt[at] == null || origAt[at + nq.length - 1] == null) return null;
    const range = document.createRange();
    range.setStart(textNode, origAt[at]);
    range.setEnd(textNode, origAt[at + nq.length - 1] + 1);
    return range;
  }

  function unwrapQueryMarks() {
    $$("#ac mark.hl, #resultsList mark.hl").forEach(m => {
      const p = m.parentNode;
      if (!p) return;
      while (m.firstChild) p.insertBefore(m.firstChild, m);
      p.removeChild(m);
      p.normalize();
    });
  }

  function wrapQueryIn(el, rawQuery) {
    const nodes = [];
    const walk = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null);
    let node;
    while ((node = walk.nextNode())) nodes.push(node);
    nodes.forEach(n => {
      const range = rangeForQuery(n, rawQuery);
      if (!range || range.collapsed) return;
      const mark = document.createElement("mark");
      mark.className = "hl";
      try { range.surroundContents(mark); } catch (_) {}
    });
  }

  function paintQueryHighlight() {
    unwrapQueryMarks();
    if (!state.highlight) return;
    const live = (($("#searchInput") && $("#searchInput").value) || "").trim();
    if (live && $("#searchBox") && $("#searchBox").classList.contains("is-open")) {
      $$("#ac .ac-row.is-shown .ac-pref, #ac .ac-row.is-shown .ac-via-syn.is-on em").forEach(el => {
        wrapQueryIn(el, live);
      });
    }
    const committed = (state.committed || "").trim();
    if (committed) {
      $$("#resultsList .rl-item.is-shown .rl-pref, #resultsList .rl-item.is-shown .rl-via-syn.is-on em").forEach(el => {
        wrapQueryIn(el, committed);
      });
    }
  }

  function setAcIdx(i) {
    const rows = shownAcRows();
    state.acIdx = i;
    rows.forEach((r, n) => r.classList.toggle("is-active", n === i));
  }

  function filterAc(query) {
    const q = norm((query || "").trim());
    const ac = $("#ac");
    const box = $("#searchBox");
    const rows = $$("#ac .ac-row").map(el => {
      el.classList.remove("is-shown", "is-hit", "match-pref", "match-syn", "match-note", "is-active");
      $$(".ac-via-syn", el).forEach(v => v.classList.remove("is-on"));
      const sc = scoreEl(el, q);
      if (sc.score < 0) return null;
      el.classList.add("is-hit");
      if (sc.score <= 2) el.classList.add("match-pref");
      else if (sc.via === "syn") {
        el.classList.add("match-syn");
        const syn = $$(".ac-via-syn", el).find(v => (v.getAttribute("data-q") || "") === sc.viaQ)
          || $$(".ac-via-syn", el).find(v => (v.getAttribute("data-q") || "").includes(q));
        if (syn) syn.classList.add("is-on");
      } else el.classList.add("match-note");
      return { el, score: sc.score, pref: el.getAttribute("data-pref-label") || "" };
    }).filter(Boolean).sort((a, b) => a.score - b.score || a.pref.localeCompare(b.pref, "fr"));

    rows.forEach((h, i) => {
      h.el.style.order = String(i);
      if (i < 7) h.el.classList.add("is-shown");
    });
    if (ac) {
      ac.classList.toggle("is-empty", !!q && rows.length === 0);
      ac.classList.toggle("has-hits", rows.length > 0);
    }
    if (box) box.classList.toggle("is-open", !!q);
    const acQ = $("#acQuery"); if (acQ) acQ.textContent = (query || "").trim();
    const acN = $("#acCount"); if (acN) acN.textContent = String(rows.length);
    state.acIdx = -1;
    paintQueryHighlight();
    return rows.length;
  }

  function paintCommittedResults() {
    const hits = rankHits(state.committed);
    const q = norm(state.committed.trim());
    $$("#resultsList .rl-item").forEach(el => {
      el.classList.remove("is-hit", "is-shown", "match-syn", "match-note");
      $$(".rl-via", el).forEach(v => v.classList.remove("is-on"));
    });
    hits.forEach((h, i) => {
      h.el.classList.add("is-hit");
      const li = h.el.closest("li");
      if (li) li.style.order = String(i);
      if (i < state.resultLimit) h.el.classList.add("is-shown");
      if (h.via === "syn") {
        h.el.classList.add("match-syn");
        const syn = $$(".rl-via-syn", h.el).find(v => (v.getAttribute("data-q") || "") === h.viaQ);
        if (syn) syn.classList.add("is-on");
      } else if (h.via === "note") {
        h.el.classList.add("match-note");
        const note = $(".rl-via-note", h.el);
        if (note) note.classList.add("is-on");
      }
    });
    const n = hits.length;
    const rl = $("#resultsList");
    if (rl) {
      rl.classList.toggle("is-idle", false);
      rl.classList.toggle("is-empty", n === 0);
      rl.classList.toggle("is-filled", n > 0);
    }
    const rlN = $("#rlN"); if (rlN) rlN.textContent = String(n);
    const rlQ = $("#rlQuery"); if (rlQ) rlQ.textContent = state.committed;
    const more = $("#rlMore");
    if (more) {
      const rest = Math.max(0, n - state.resultLimit);
      more.hidden = rest === 0;
      const mn = $("#rlMoreN");
      if (mn) mn.textContent = "+" + Math.min(rest, PAGE);
    }
    paintBadges();
    paintQueryHighlight();
  }

  function syncRechercheConcept() {
    const hits = rankHits(state.committed);
    const ids = hits.map(h => h.id);
    if (state.conceptId && ids.includes(state.conceptId)) highlightConcept(state.conceptId);
    else {
      state.conceptId = ids[0] || null;
      highlightConcept(state.conceptId);
    }
  }

  function runSearch() {
    const q = ($("#searchInput") && $("#searchInput").value) || "";
    if (!IS_CONSULT) {
      go("thesaurus/consultation.xhtml?q=" + encodeURIComponent(q.trim()));
      return;
    }
    state.committed = q.trim();
    state.resultLimit = PAGE;
    state.home = false;
    state.draft = false;
    state.view = "recherche";
    filterAc(q);
    paintCommittedResults();
    syncRechercheConcept();
    closeSearchUi();
    paint();
  }

  function applyStatusFilter() {
    const set = state.statusSet;
    function candOk(tn) {
      if ((tn.getAttribute("data-status") || "") !== "candidat") return true;
      const by = tn.getAttribute("data-cand-by") || "";
      const on = tn.getAttribute("data-cand-on") || "";
      if (state.candBy && by !== state.candBy) return false;
      if (state.candFrom && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on < state.candFrom)) return false;
      if (state.candTo && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on > state.candTo)) return false;
      return true;
    }
    const nodes = treeNodes();
    const ownOk = nodes.map(tn => {
      const st = tn.getAttribute("data-status") || "valide";
      return set.has(st) && candOk(tn);
    });
    const vis = ownOk.slice();
    for (let i = 0; i < nodes.length; i++) {
      if (!ownOk[i]) continue;
      let depth = treeDepth(nodes[i]);
      for (let j = i - 1; j >= 0 && depth > 0; j--) {
        const parentDepth = treeDepth(nodes[j]);
        if (parentDepth < depth) {
          vis[j] = true;
          depth = parentDepth;
        }
      }
    }
    nodes.forEach((tn, i) => tn.classList.toggle("is-status-off", !vis[i]));
    $$("#panelTable tr[data-status]").forEach(tr => {
      const st = tr.getAttribute("data-status") || "valide";
      let on = set.has(st);
      if (on && st === "candidat") {
        const by = tr.getAttribute("data-cand-by") || "";
        const day = tr.getAttribute("data-cand-on") || "";
        if (state.candBy && by !== state.candBy) on = false;
        if (state.candFrom && (!/^\d{4}-\d{2}-\d{2}$/.test(day) || day < state.candFrom)) on = false;
        if (state.candTo && (!/^\d{4}-\d{2}-\d{2}$/.test(day) || day > state.candTo)) on = false;
      }
      tr.classList.toggle("is-status-off", !on);
    });
  }

  function treeNodes() {
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
    groups.sort((a, b) => (a[0].dataset.sortkey || "").localeCompare(b[0].dataset.sortkey || "", "fr"));
    return groups.flat();
  }

  function applySort() {
    const nota = state.sort === "nota";
    const box = $("#panelTree .tree-body");
    const nodes = treeNodes();
    nodes.forEach(tn => {
      tn.dataset.sortkey = nota
        ? (tn.getAttribute("data-nota") || "~")
        : (tn.getAttribute("data-key") || "").split("/").pop() || "";
    });
    if (box && nodes.length) {
      sortTreeRange(nodes).forEach(el => box.appendChild(el));
    }
    $$(".vo-seg-b[data-sort]").forEach(b => b.classList.toggle("is-on", b.getAttribute("data-sort") === state.sort));
  }

  function applyTableSort() {
    const col = state.tblSort;
    const dir = state.tblDir;
    const body = $("#panelTable tbody");
    if (!body) return;
    const rows = $$("#panelTable tbody tr[data-id]");
    rows.sort((a, b) => {
      const va = a.getAttribute("data-s-" + col) || "";
      const vb = b.getAttribute("data-s-" + col) || "";
      return va.localeCompare(vb, "fr", { numeric: true }) * dir;
    });
    rows.forEach(r => body.appendChild(r));
    $$("#panelTable .flat-th").forEach(th => {
      const on = th.getAttribute("data-col") === col;
      th.classList.toggle("is-sorted", on);
      const s = th.querySelector(".flat-th-s");
      if (s) s.textContent = on ? (dir > 0 ? "▲" : "▼") : "↕";
    });
  }

  function applyTableCols() {
    ["status", "type", "notation", "path"].forEach(col => {
      const on = state.tblCols.has(col);
      $$(".col-" + col).forEach(el => el.classList.toggle("is-col-off", !on));
      const lab = $(`.tbl-colrow[data-col="${col}"]`);
      if (lab) {
        lab.classList.toggle("on", on);
        const box = lab.querySelector(".tbl-colbox");
        if (box) box.textContent = on ? "✓" : "";
      }
    });
  }

  function syncStatusUi() {
    Object.keys(GROUPS).forEach(key => {
      const list = GROUPS[key];
      const nOn = list.filter(s => state.statusSet.has(s)).length;
      const gs = nOn === 0 ? "off" : (nOn === list.length ? "on" : "mixed");
      const group = $(`.stk-group[data-group="${key}"]`);
      if (!group) return;
      const head = group.querySelector(".stk-ghead");
      const input = group.querySelector(".stk-ghead input");
      const box = group.querySelector(".stk-ghead .stk-box");
      if (head) head.classList.toggle("on", gs !== "off");
      group.classList.toggle("on", gs !== "off");
      if (input) {
        input.checked = gs === "on";
        input.indeterminate = gs === "mixed";
      }
      if (box) {
        box.classList.toggle("mixed", gs === "mixed");
        box.textContent = gs === "on" ? "✓" : gs === "mixed" ? "–" : "";
      }
      list.forEach(s => {
        const lab = group.querySelector(`.stk-item[data-status="${s}"]`);
        if (!lab) return;
        const on = state.statusSet.has(s);
        const item = lab.querySelector("input");
        if (item) item.checked = on;
        lab.classList.toggle("on", on);
        const ib = lab.querySelector(".stk-box");
        if (ib) ib.textContent = on ? "✓" : "";
      });
    });
    syncCandFilterUi();
    applyStatusFilter();
  }

  function syncCandFilterUi() {
    const active = !!(state.candBy || state.candFrom || state.candTo);
    const clear = $("#cfClear");
    if (clear) clear.hidden = !active;
  }

  function collectIds(tn) {
    const ids = [];
    const push = id => { if (id && !ids.includes(id)) ids.push(id); };
    push(tn.getAttribute("data-id"));
    const depth = treeDepth(tn);
    let next = tn.nextElementSibling;
    while (next && next.classList.contains("tn")) {
      if (treeDepth(next) <= depth) break;
      push(next.getAttribute("data-id"));
      next = next.nextElementSibling;
    }
    return ids;
  }

  function paintSelectedId(id, on) {
    $$(`[data-id="${CSS.escape(id)}"]`).forEach(el => {
      const check = el.matches(".tn-check") ? el : (el.querySelector && el.querySelector(".tn-check"));
      const row = el.closest && (el.closest(".tn-row") || el.closest("tr"));
      if (check) check.classList.toggle("on", on);
      if (row) row.classList.toggle("is-sel", on);
    });
  }

  function setSelectedIds(ids, on) {
    ids.forEach(id => {
      if (on) state.selected.add(id);
      else state.selected.delete(id);
      paintSelectedId(id, on);
    });
    updateBulk();
  }

  function restoreSelection() {
    treeNodes().forEach(tn => {
      const id = tn.getAttribute("data-id");
      if (!id) return;
      let selected = state.selected.has(id);
      if (!selected) {
        let parent = previousTreeParent(tn);
        while (parent && !selected) {
          const parentId = parent.getAttribute("data-id");
          if (parentId && state.selected.has(parentId)) selected = true;
          parent = previousTreeParent(parent);
        }
      }
      if (selected) {
        state.selected.add(id);
        paintSelectedId(id, true);
      }
    });
    updateBulk();
  }

  function clearSelection(keepUi) {
    if (!keepUi) setSelectedIds([...state.selected], false);
    else {
      $$(".tn-check.on").forEach(c => c.classList.remove("on"));
      $$(".is-sel").forEach(r => r.classList.remove("is-sel"));
    }
    state.selected.clear();
    bulkMode("acts");
    updateBulk();
  }

  function selectionRootIds() {
    const roots = [];
    const seen = new Set();
    treeNodes().forEach(tn => {
      const id = tn.getAttribute("data-id");
      if (!id || !state.selected.has(id) || seen.has(id)) return;
      let parent = previousTreeParent(tn);
      while (parent) {
        const parentId = parent.getAttribute("data-id");
        if (parentId && state.selected.has(parentId)) return;
        parent = previousTreeParent(parent);
      }
      seen.add(id);
      roots.push(id);
    });
    state.selected.forEach(id => {
      if (!seen.has(id) && !$$(`.tn[data-id="${CSS.escape(id)}"]`).length) {
        seen.add(id);
        roots.push(id);
      }
    });
    return roots;
  }

  function selectedCount() {
    const roots = selectionRootIds();
    if (!roots.length) return state.selected.size;
    let n = 0;
    let complete = true;
    roots.forEach(id => {
      if (state.subtreeSize.has(id)) n += state.subtreeSize.get(id);
      else complete = false;
    });
    return complete ? n : Math.max(n, state.selected.size);
  }

  function fetchSubtreeSize(id, nodeType) {
    const ctx = document.body.getAttribute("data-ctx") || "";
    const params = new URLSearchParams({ id });
    if (nodeType) params.set("nodeType", nodeType);
    return fetch(ctx + "/v2/api/subtree-size?" + params.toString(), {
      headers: { Accept: "application/json" }
    }).then(res => res.ok ? res.json() : { size: 1 })
      .then(data => {
        const size = Number(data && data.size);
        return Number.isFinite(size) && size > 0 ? size : 1;
      })
      .catch(() => 1);
  }

  function ensureSubtreeSize(id, nodeType, hasChildren) {
    if (!id) return Promise.resolve(1);
    if (state.subtreeSize.has(id)) return Promise.resolve(state.subtreeSize.get(id));
    if (hasChildren === false) {
      state.subtreeSize.set(id, 1);
      return Promise.resolve(1);
    }
    if (state.subtreeSizePending.has(id)) return state.subtreeSizePending.get(id);
    const pending = fetchSubtreeSize(id, nodeType).then(size => {
      state.subtreeSize.set(id, size);
      state.subtreeSizePending.delete(id);
      updateBulk();
      return size;
    });
    state.subtreeSizePending.set(id, pending);
    return pending;
  }

  function refreshSubtreeCounts() {
    selectionRootIds().forEach(id => {
      const tn = $(`.tn[data-id="${CSS.escape(id)}"]`);
      ensureSubtreeSize(
        id,
        tn && tn.getAttribute("data-type"),
        tn && tn.getAttribute("data-has-children") === "true"
      );
    });
    updateBulk();
  }

  function updateBulk() {
    const bar = $("#bulkSel");
    const n = selectedCount();
    const ok = n > 0 && (state.view === "arbo" || state.view === "tableau" || state.view === "hyper");
    if (bar) bar.classList.toggle("is-on", ok);
    const b = $("#bulkN");
    if (b) b.textContent = String(n);
    const plural = $("#bulkPlural");
    if (plural) plural.textContent = n > 1 ? "s" : "";
    const plural2 = $("#bulkPlural2");
    if (plural2) plural2.textContent = n > 1 ? "s" : "";
  }

  function bulkMode(mode) {
    const acts = $("#bulkActs"), coll = $("#bulkColl"), move = $("#bulkMove");
    if (acts) acts.hidden = mode !== "acts";
    if (coll) coll.hidden = mode !== "coll";
    if (move) move.hidden = mode !== "move";
    $("#bulkStatusMenu") && $("#bulkStatusMenu").classList.remove("is-on");
    if (mode !== "move") {
      state.moveTarget = null;
      const tgt = $("#bulkMoveTarget"); if (tgt) tgt.hidden = true;
      const pick = $("#bulkMovePick"); if (pick) pick.hidden = true;
      const q = $("#bulkMoveQ"); if (q) q.value = "";
      const run = $("#bulkMoveRun"); if (run) run.classList.add("is-off");
    }
  }

  function bulkAct(label) {
    const n = state.selected.size;
    const s = n > 1 ? "s" : "";
    toast(label + " · " + n + " concept" + s);
    clearSelection();
  }

  function filterMovePick(qraw) {
    const q = norm(qraw.trim());
    const pick = $("#bulkMovePick");
    const empty = $("#bulkMoveEmpty");
    if (state.moveTarget) {
      if (pick) pick.hidden = true;
      return;
    }
    if (pick) pick.hidden = !q;
    if (!q) { if (empty) empty.hidden = true; return; }
    let n = 0;
    $$("#bulkMovePick .bulksel-po").forEach(b => {
      const id = b.getAttribute("data-id");
      const hit = !state.selected.has(id) && (b.getAttribute("data-q") || "").includes(q);
      if (hit && n < 6) {
        b.classList.add("is-shown");
        n += 1;
      } else {
        b.classList.remove("is-shown");
      }
    });
    if (empty) empty.hidden = n > 0;
  }

  function closeThesaurus() {
    $$(".thesaurus-btn.is-on").forEach(b => {
      b.classList.remove("is-on");
      b.setAttribute("aria-expanded", "false");
    });
  }

  (function hyperDrag() {
    const svg = $("#hyperSvg");
    const scene = $("#hyperScene");
    const stage = $("#hyperStage");
    if (!svg || !scene || !stage) return;
    let drag = null, tx = 0, ty = 0, justDragged = false;
    function apply() {
      scene.setAttribute("transform", "translate(" + tx + " " + ty + ")");
    }
    svg.addEventListener("pointerdown", (e) => {
      drag = { x: e.clientX, y: e.clientY, tx, ty, moved: false };
    });
    svg.addEventListener("pointermove", (e) => {
      if (!drag) return;
      const dx = e.clientX - drag.x, dy = e.clientY - drag.y;
      if (Math.hypot(dx, dy) > 4) drag.moved = true;
      const b = stage.getBoundingClientRect();
      const s = Math.min(b.width, b.height) || 1;
      tx = drag.tx + dx * (1000 / s);
      ty = drag.ty + dy * (1000 / s);
      apply();
    });
    svg.addEventListener("pointerup", () => {
      if (drag && drag.moved) justDragged = true;
      drag = null;
      setTimeout(() => { justDragged = false; }, 0);
    });
    svg.addEventListener("click", (e) => {
      if (justDragged) e.stopPropagation();
    }, true);
    const center = $("#hyperCenter");
    if (center) center.addEventListener("click", () => { tx = 0; ty = 0; apply(); });
  })();

  document.addEventListener("pointerdown", (e) => {
    const go = e.target.closest(".login-go");
    if (!go) return;
    go.classList.remove("is-click");
    void go.offsetWidth;
    go.classList.add("is-click");
  });
  document.addEventListener("click", (e) => {
    const go = e.target.closest(".login-go");
    if (go) go.classList.add("is-busy");
  }, true);
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Enter" || e.repeat || e.isComposing) return;
    const form = e.target.closest("#previewLoginForm");
    if (!form) return;
    const go = form.querySelector(".login-go");
    if (!go || go.classList.contains("is-busy")) return;
    e.preventDefault();
    go.click();
  });
  document.addEventListener("keydown", (e) => {
    if (e.repeat || e.isComposing) return;
    if (e.key !== "Enter" && e.key !== " ") return;
    const card = e.target.closest(".boc-clickable[data-act='show']");
    if (!card || e.target !== card) return;
    e.preventDefault();
    card.click();
  });

  window.onPreviewLoginAjax = function (data) {
    if (data.status !== "success") return;
    const form = document.getElementById("previewLoginForm");
    const btn = document.querySelector('[data-thesaurus="account"]');
    if (!form || !btn) return;
    btn.classList.add("is-on");
    btn.setAttribute("aria-expanded", "true");
  };

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
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    if (hideConfirm("#aboutSaveConfirm") || hideConfirm("#logoutConfirm") || hideConfirm("#stSaveConfirm")
        || hideConfirm("#previewCorpusCreateConfirm") || hideConfirm("#stLeaveConfirm")) {
      settingsLeaveAction = null;
      e.preventDefault();
    }
  });

  document.addEventListener("mousedown", (e) => {
    if (e.target.closest(".abt-fmt-btn, [data-act='about-src']")) e.preventDefault();
    const save = e.target.closest(".abt-save:not(.is-off):not(.is-busy)");
    if (save) {
      save.classList.remove("is-click");
      void save.offsetWidth;
      save.classList.add("is-click");
    }
    const corpusBtn = e.target.closest(".st-corpus-btn");
    if (corpusBtn) {
      corpusBtn.classList.remove("is-click");
      void corpusBtn.offsetWidth;
      corpusBtn.classList.add("is-click");
    }
  });
  document.addEventListener("click", (e) => {
    const save = e.target.closest(".abt-save");
    if (save && (save.classList.contains("is-off") || save.classList.contains("is-busy"))) {
      e.preventDefault();
      e.stopPropagation();
    }
    interceptAboutSwap(e);
  }, true);

  document.addEventListener("input", (e) => {
    if (e.target && (e.target.id === "aboutVisual" || e.target.classList.contains("abt-editor"))) {
      syncAboutEditor();
      markAboutVisualEmpty();
      refreshAboutSaveState();
    }
  });
  document.addEventListener("selectionchange", refreshAboutFmtState);

  document.addEventListener("click", (e) => {
    const t = e.target.closest("[data-act]");
    if (!t) {
      if ($("#navThesaurus") && !$("#navThesaurus").contains(e.target)) closeThesaurus();
      if ($("#voWrap") && !$("#voWrap").contains(e.target)) $("#voGear") && $("#voGear").classList.remove("is-on");
      if ($("#viewPick") && !$("#viewPick").contains(e.target)) $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
      if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
      return;
    }
    if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
    const act = t.getAttribute("data-act");
    if (act === "logout-ask") {
      closeThesaurus();
      if (askLeaveThen(() => showConfirm("#logoutConfirm"))) return;
      showConfirm("#logoutConfirm");
    } else if (act === "logout-dismiss") {
      hideConfirm("#logoutConfirm");
    } else if (act === "logout-modal") {
      return;
    } else if (act === "about-save-ask") {
      const btn = $("#aboutSaveBtn");
      if (!btn || btn.classList.contains("is-off") || btn.classList.contains("is-busy")) return;
      syncAboutEditor();
      showConfirm("#aboutSaveConfirm");
    } else if (act === "about-save-dismiss") {
      hideConfirm("#aboutSaveConfirm");
    } else if (act === "about-save-modal") {
      return;
    } else if (act === "st-save-ask") {
      showConfirm("#stSaveConfirm");
    } else if (act === "st-save-go") {
      e.preventDefault();
      clickPreviewJsf("previewPrefSaveGo");
    } else if (act === "st-save-dismiss") {
      hideConfirm("#stSaveConfirm");
    } else if (act === "st-save-modal") {
      return;
    } else if (act === "st-leave-dismiss") {
      dismissSettingsLeave();
    } else if (act === "st-leave-confirm") {
      confirmSettingsLeave();
    } else if (act === "st-leave-modal") {
      return;
    } else if (act === "corpus-create-ask") {
      const name = ($("#previewCorpusName") && $("#previewCorpusName").value || "").trim();
      const label = $("#previewCorpusCreateName");
      if (label) label.textContent = name ? "« " + name + " »" : "ce corpus";
      showConfirm("#previewCorpusCreateConfirm");
    } else if (act === "corpus-create-dismiss") {
      hideConfirm("#previewCorpusCreateConfirm");
    } else if (act === "corpus-create-modal") {
      return;
    } else if (act === "preview-corpus-new") {
      clickPreviewJsf("previewCorpusNewGo");
    } else if (act === "preview-corpus-toggle") {
      clickPreviewJsf("previewCorpusToggleGo", {
        previewCorpusTarget: t.getAttribute("data-name") || ""
      });
    } else if (act === "preview-corpus-edit") {
      clickPreviewJsf("previewCorpusEditGo", {
        previewCorpusTarget: t.getAttribute("data-name") || ""
      });
    } else if (act === "preview-corpus-del") {
      clickPreviewJsf("previewCorpusDelGo", {
        previewCorpusTarget: t.getAttribute("data-name") || ""
      });
    } else if (act === "preview-corpus-prev") {
      if (!t.classList.contains("is-off")) clickPreviewJsf("previewCorpusPrevGo");
    } else if (act === "preview-corpus-next") {
      if (!t.classList.contains("is-off")) clickPreviewJsf("previewCorpusNextGo");
    } else if (act === "preview-corpus-page") {
      clickPreviewJsf("previewCorpusPageGo", {
        previewCorpusPage: t.getAttribute("data-page") || "1"
      });
    } else if (act === "preview-align-toggle") {
      clickPreviewJsf("previewAlignToggleGo", {
        previewAlignSourceId: t.getAttribute("data-id") || ""
      });
    } else if (act === "preview-align-new") {
      clickPreviewJsf("previewAlignNewGo");
    } else if (act === "preview-align-edit") {
      clickPreviewJsf("previewAlignEditGo", {
        previewAlignSourceId: t.getAttribute("data-id") || ""
      });
    } else if (act === "preview-align-del") {
      clickPreviewJsf("previewAlignDelGo", {
        previewAlignSourceId: t.getAttribute("data-id") || ""
      });
    } else if (act === "preview-align-prev") {
      if (!t.classList.contains("is-off")) clickPreviewJsf("previewAlignPrevGo");
    } else if (act === "preview-align-next") {
      if (!t.classList.contains("is-off")) clickPreviewJsf("previewAlignNextGo");
    } else if (act === "preview-align-page") {
      clickPreviewJsf("previewAlignPageGo", {
        previewAlignPage: t.getAttribute("data-page") || "1"
      });
    } else if (act === "go-top") {
      const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      const behavior = reduce ? "auto" : "smooth";
      ["#previewView", "#viewHome", "#viewSettings", "main.content .view"].forEach((sel) => {
        const el = $(sel);
        if (el) el.scrollTo({ top: 0, behavior });
      });
    } else if (act === "thesaurus") {
      const on = t.classList.contains("is-on");
      closeThesaurus();
      if (!on) {
        t.classList.add("is-on");
        t.setAttribute("aria-expanded", "true");
      }
    } else if (act === "home") openHome();
    else if (act === "set-view") setView(t.getAttribute("data-view"));
    else if (act === "show") {
      e.preventDefault();
      showHomePanel(t.getAttribute("data-panel"));
    }
    else if (act === "settings-open") {
      const pages = {
        prefs: "setting/preference.xhtml#stPrefs",
        servers: "setting/preference.xhtml#stServers",
        corpus: "setting/preference.xhtml#stCorpus"
      };
      go(pages[t.getAttribute("data-section")] || "setting/preference.xhtml");
    }
    else if (act === "bo-open") {
      const obj = t.getAttribute("data-obj");
      go("toolbox/atelier.xhtml" + (obj ? "?obj=" + encodeURIComponent(obj) : ""));
    } else if (act === "cand-tab") {
      const board = t.closest(".cand-board") || $("#candBoard");
      if (board) board.setAttribute("data-tab", t.getAttribute("data-tab") || "attente");
    } else if (act === "bo-op") {
      setBatch(t.getAttribute("data-obj"), t.getAttribute("data-op"));
    } else if (act === "bo-acc") {
      const step = t.closest(".bo-acc-step");
      if (step) step.classList.toggle("open");
    } else if (act === "bo-pick") {
      const p = t.closest(".bo-panel");
      if (p) { p.classList.add("has-file"); p.classList.remove("is-checked", "is-done"); }
    } else if (act === "bo-clear") {
      boReset(t.closest(".bo-panel"));
    } else if (act === "bo-check") {
      const p = t.closest(".bo-panel");
      if (p && p.classList.contains("has-file") && !p.classList.contains("is-checked")) p.classList.add("is-checked");
    } else if (act === "bo-reimport") {
      const p = t.closest(".bo-panel");
      if (!p) return;
      p.classList.add("has-file", "is-corrected");
      p.classList.remove("is-checked", "is-done");
      toast("Fichier corrigé réimporté");
    } else if (act === "bo-run") {
      const p = t.closest(".bo-panel");
      if (p) p.classList.add("is-done");
    }
    else if (act === "toggle") {
      const tn = t.closest(".tn");
      if (tn && !t.classList.contains("is-empty")) tn.classList.toggle("is-open");
    } else if (act === "open") {
      const id = t.getAttribute("data-id");
      const stay = !!(t.closest("#panelTable") || t.closest("#panelResults") || t.closest("#resultsList"));
      const nodeType = resolveNodeType(t);
      if (openLiveDetail(id, nodeType)) {
        state.home = false;
        state.draft = false;
        state.conceptId = id;
        if (!stay) state.view = "arbo";
        highlightConcept(id);
        closeSearchUi();
        return;
      }
      openConcept(id, stay ? "stay" : "jump");
    } else if (act === "hyper-pick") {
      const id = t.getAttribute("data-id");
      if (!id) return;
      const nodeType = resolveNodeType(t);
      if (openLiveDetail(id, nodeType)) {
        state.home = false;
        state.draft = false;
        state.conceptId = id;
        highlightConcept(id);
        return;
      }
      state.home = false;
      state.draft = false;
      state.conceptId = id;
      highlightConcept(id);
      paint();
    } else if (act === "about") {
      const fold = t.closest(".abt-fold") || $("#aboutFold");
      if (!fold) return;
      const open = fold.classList.toggle("is-open");
      t.classList.toggle("open", open);
      t.setAttribute("aria-expanded", String(open));
      if (!open) {
        const title = document.querySelector("#viewHome .cv-head") || document.querySelector("#viewHome .cv-pref");
        const scroller = $("#previewView") || document.querySelector("main.content .view") || document.querySelector(".view");
        const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        const behavior = reduce ? "auto" : "smooth";
        if (title) title.scrollIntoView({ block: "start", behavior });
        else if (scroller) scroller.scrollTo({ top: 0, behavior });
      }
      if (window.syncViewRail) {
        window.syncViewRail();
        const html = fold.querySelector(".abt-html");
        if (html) html.addEventListener("transitionend", () => window.syncViewRail(), { once: true });
      }
    } else if (act === "about-fmt") {
      e.preventDefault();
      applyAboutFormat(t.getAttribute("data-cmd"), t.getAttribute("data-val"));
    } else if (act === "about-src") {
      e.preventDefault();
      toggleAboutSource();
    } else if (act === "toast") {
      toast(t.getAttribute("data-msg"));
      $("#bulkStatusMenu") && $("#bulkStatusMenu").classList.remove("is-on");
    } else if (act === "bulk-act") {
      bulkAct(t.getAttribute("data-msg") || "Action");
    } else if (act === "copy") {
      try { navigator.clipboard.writeText(t.getAttribute("data-copy")); } catch (_) {}
      t.classList.add("is-copied");
      t.title = "Copié";
      setTimeout(() => { t.classList.remove("is-copied"); t.title = "Copier"; }, 1400);
    } else if (act === "st-save") {
      hideConfirm("#stSaveConfirm");
      const el = $("#stSaveToast");
      if (!el) return;
      el.hidden = false;
      clearTimeout(el._t);
      el._t = setTimeout(() => { el.hidden = true; }, 2200);
    } else if (act === "toggle-sw") {
      t.classList.toggle("on");
      t.setAttribute("aria-pressed", String(t.classList.contains("on")));
      const wrap = t.closest(".st-integ");
      if (wrap) wrap.classList.toggle("on", t.classList.contains("on"));
    }
    else if (act === "toggle-path") {
      state.showPath = !state.showPath;
      t.classList.toggle("on", state.showPath);
      t.setAttribute("aria-checked", String(state.showPath));
      paintSidebar();
    } else if (act === "toggle-hl") {
      state.highlight = !state.highlight;
      t.classList.toggle("on", state.highlight);
      t.setAttribute("aria-checked", String(state.highlight));
      document.body.classList.toggle("hl-off", !state.highlight);
      paintQueryHighlight();
    } else if (act === "density") {
      state.density = t.getAttribute("data-density");
      $$("[data-act='density']").forEach(b => b.classList.toggle("is-on", b === t));
      paintSidebar();
    }     else if (act === "see-all") runSearch();
    else if (act === "create") {
      e.stopPropagation();
      createCandidate(t);
    } else if (act === "cand-resolve") {
      resolveDraft(t.getAttribute("data-kind"));
    }
    else if (act === "sel-node") {
      e.stopPropagation();
      const tn = t.closest(".tn");
      if (!tn) return;
      const ids = collectIds(tn);
      const on = !t.classList.contains("on");
      if (!on) {
        let parent = previousTreeParent(tn);
        while (parent) {
          const parentId = parent.getAttribute("data-id");
          if (parentId) ids.push(parentId);
          parent = previousTreeParent(parent);
        }
      }
      setSelectedIds(ids, on);
      refreshSubtreeCounts();
    } else if (act === "sel-id") {
      e.stopPropagation();
      const id = t.getAttribute("data-id");
      if (id) setSelectedIds([id], !state.selected.has(id));
    } else if (act === "sel-all") {
      e.stopPropagation();
      const rows = $$("#panelTable tr[data-id]:not(.is-status-off)");
      const ids = rows.map(r => r.getAttribute("data-id"));
      const allOn = ids.length > 0 && ids.every(id => state.selected.has(id));
      setSelectedIds(ids, !allOn);
    } else if (act === "clear-sel") {
      clearSelection();
    } else if (act === "st-group") {
      e.preventDefault();
      const key = t.getAttribute("data-group");
      const list = GROUPS[key] || [];
      const allOn = list.every(s => state.statusSet.has(s));
      list.forEach(s => allOn ? state.statusSet.delete(s) : state.statusSet.add(s));
      syncStatusUi();
    } else if (act === "st-item") {
      e.preventDefault();
      const s = t.getAttribute("data-status");
      if (state.statusSet.has(s)) state.statusSet.delete(s);
      else state.statusSet.add(s);
      syncStatusUi();
    } else if (act === "cf-toggle") {
      const combo = t.closest(".cf-combo") || $("#cfCombo");
      if (combo) combo.classList.toggle("open");
    } else if (act === "cf-by") {
      state.candBy = t.getAttribute("data-by") || "";
      $$(".cf-opt").forEach(o => o.classList.toggle("on", o === t));
      const lab = $("#cfByLabel");
      if (lab) lab.textContent = t.getAttribute("data-label") || "Tout le monde";
      const combo = t.closest(".cf-combo");
      if (combo) combo.classList.remove("open");
      syncCandFilterUi();
      applyStatusFilter();
    } else if (act === "cf-clear") {
      state.candBy = "";
      state.candFrom = "";
      state.candTo = "";
      const from = $("#cfFrom"), to = $("#cfTo");
      if (from) from.value = "";
      if (to) to.value = "";
      $$(".cf-opt").forEach(o => o.classList.toggle("on", !o.getAttribute("data-by")));
      const lab = $("#cfByLabel");
      if (lab) lab.textContent = "Tout le monde";
      syncCandFilterUi();
      applyStatusFilter();
    } else if (act === "sort") {
      state.sort = t.getAttribute("data-sort");
      applySort();
    } else if (act === "tbl-sort") {
      const col = t.getAttribute("data-col");
      if (state.tblSort === col) state.tblDir *= -1;
      else { state.tblSort = col; state.tblDir = 1; }
      applyTableSort();
    } else if (act === "tbl-col") {
      e.preventDefault();
      const col = t.getAttribute("data-col");
      if (state.tblCols.has(col)) state.tblCols.delete(col);
      else state.tblCols.add(col);
      applyTableCols();
    } else if (act === "ui-lang") {
      $$(".lang-opt").forEach(o => o.classList.toggle("is-on", o === t));
      const flag = t.querySelector(".lang-opt-flag");
      const btn = $('[data-thesaurus="lang"] .thesaurus-flag');
      if (flag && btn) btn.textContent = flag.textContent;
      closeThesaurus();
    } else if (act === "bulk-coll") bulkMode("coll");
    else if (act === "bulk-move") bulkMode("move");
    else if (act === "bulk-back") bulkMode("acts");
    else if (act === "bulk-status-menu") {
      $("#bulkStatusMenu") && $("#bulkStatusMenu").classList.toggle("is-on");
    } else if (act === "bulk-coll-run") {
      const name = ($("#bulkCollName") && $("#bulkCollName").value.trim()) || "";
      if (name) bulkAct("Collection « " + name + " » créée");
    } else if (act === "bulk-move-pick") {
      state.moveTarget = { id: t.getAttribute("data-id"), pref: t.getAttribute("data-pref") };
      const box = $("#bulkMoveTarget");
      const lab = $("#bulkMoveTargetL");
      if (lab) lab.textContent = state.moveTarget.pref;
      if (box) box.hidden = false;
      $("#bulkMovePick") && ($("#bulkMovePick").hidden = true);
      const run = $("#bulkMoveRun");
      if (run) run.classList.remove("is-off");
    } else if (act === "bulk-move-clear") {
      state.moveTarget = null;
      $("#bulkMoveTarget") && ($("#bulkMoveTarget").hidden = true);
      const q = $("#bulkMoveQ"); if (q) { q.value = ""; q.focus(); }
      $("#bulkMoveRun") && $("#bulkMoveRun").classList.add("is-off");
    } else if (act === "bulk-move-run") {
      if (!state.moveTarget || $("#bulkMoveRun").classList.contains("is-off")) return;
      const n = state.selected.size;
      const s = n > 1 ? "s" : "";
      bulkAct(n + " concept" + s + " déplacé" + s + " sous « " + state.moveTarget.pref + " »");
    }
  });

  $("#viewPickBtn") && $("#viewPickBtn").addEventListener("click", () => {
    $("#viewPickBtn").classList.toggle("is-open");
  });
  $("#voGear") && $("#voGear").addEventListener("click", (e) => {
    e.stopPropagation();
    $("#voGear").classList.toggle("is-on");
  });
  $("#rlMore") && $("#rlMore").addEventListener("click", () => {
    state.resultLimit += PAGE;
    paintCommittedResults();
  });

  const input = $("#searchInput"), clear = $("#searchClear"), field = $("#searchField");
  if (input && field) {
    input.addEventListener("input", () => {
      if (clear) clear.hidden = !input.value;
      field.classList.add("is-focused");
      filterAc(input.value);
    });
    input.addEventListener("focus", () => {
      field.classList.add("is-focused");
      if (input.value) filterAc(input.value);
    });
    input.addEventListener("keydown", (e) => {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        if (input.value.trim()) filterAc(input.value);
        const downRows = shownAcRows();
        setAcIdx(Math.min(state.acIdx + 1, Math.max(downRows.length - 1, -1)));
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setAcIdx(Math.max(state.acIdx - 1, -1));
      } else if (e.key === "Enter") {
        const rows = shownAcRows();
        if (state.acIdx >= 0 && rows[state.acIdx]) {
          openConcept(rows[state.acIdx].getAttribute("data-id"), "jump");
        } else runSearch();
      } else if (e.key === "Escape") {
        closeSearchUi();
        input.blur();
      }
    });
  }
  if (clear && input) {
    clear.addEventListener("click", () => {
      input.value = "";
      clear.hidden = true;
      filterAc("");
      input.focus();
    });
  }
  $("#searchGo") && $("#searchGo").addEventListener("click", runSearch);
  const searchBox = $("#searchBox");
  if (searchBox && searchBox.tagName === "FORM") {
    searchBox.addEventListener("submit", (e) => {
      e.preventDefault();
      runSearch();
    });
  }
  ["cfFrom", "cfTo"].forEach(id => {
    const el = $("#" + id);
    if (!el) return;
    el.addEventListener("input", () => {
      state[id === "cfFrom" ? "candFrom" : "candTo"] = el.value || "";
      syncCandFilterUi();
      applyStatusFilter();
    });
  });

  const moveQ = $("#bulkMoveQ");
  if (moveQ) {
    moveQ.addEventListener("input", () => {
      if (state.moveTarget) return;
      filterMovePick(moveQ.value);
    });
    moveQ.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && state.moveTarget) {
        const run = $("#bulkMoveRun");
        if (run) run.click();
      }
    });
  }
  const collName = $("#bulkCollName");
  if (collName) {
    collName.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && collName.value.trim()) {
        const run = $('[data-act="bulk-coll-run"]');
        if (run) run.click();
      }
    });
  }

  document.addEventListener("mousedown", (e) => {
    if (e.target.closest(".ac-row, .ac-footer")) e.preventDefault();
    if ($("#searchBox") && !$("#searchBox").contains(e.target)) closeSearchUi();
  });

  const resizer = $("#resizer");
  if (resizer) {
    const saved = parseInt(localStorage.getItem("ot-sidebar-w"), 10);
    if (saved && saved >= 240) document.documentElement.style.setProperty("--tree-w", saved + "px");
    resizer.addEventListener("mousedown", (e) => {
      e.preventDefault();
      const startX = e.clientX;
      const startW = parseInt(getComputedStyle(document.documentElement).getPropertyValue("--tree-w"), 10) || 328;
      document.body.style.cursor = "col-resize";
      document.body.style.userSelect = "none";
      const onMove = (ev) => {
        const w = Math.min(window.innerWidth * 0.75, Math.max(240, startW + (ev.clientX - startX)));
        document.documentElement.style.setProperty("--tree-w", w + "px");
      };
      const onUp = () => {
        document.removeEventListener("mousemove", onMove);
        document.removeEventListener("mouseup", onUp);
        document.body.style.cursor = "";
        document.body.style.userSelect = "";
        const w = parseInt(getComputedStyle(document.documentElement).getPropertyValue("--tree-w"), 10);
        if (w) localStorage.setItem("ot-sidebar-w", String(w));
      };
      document.addEventListener("mousemove", onMove);
      document.addEventListener("mouseup", onUp);
    });
    resizer.addEventListener("dblclick", () => {
      document.documentElement.style.setProperty("--tree-w", "328px");
      localStorage.setItem("ot-sidebar-w", "328");
    });
  }

  applyStatusFilter();
  applySort();
  applyTableSort();
  applyTableCols();
  bulkMode("acts");

  if (SCREEN === "graphe") state.view = "hyper";

  const params = new URLSearchParams(location.search);
  function aboutComposer() {
    return $(".abt-composer");
  }
  function aboutVisual() {
    return $("#aboutVisual");
  }
  function aboutTextarea() {
    const composer = aboutComposer();
    return composer ? composer.querySelector("textarea.abt-editor") : null;
  }
  function syncAboutEditor() {
    const composer = aboutComposer();
    const visual = aboutVisual();
    const ta = aboutTextarea();
    if (!composer || !ta || composer.classList.contains("is-source")) return;
    if (visual) ta.value = visual.innerHTML;
  }
  function markAboutVisualEmpty() {
    const visual = aboutVisual();
    if (!visual) return;
    visual.classList.toggle("is-empty", visual.textContent.trim() === "");
  }
  function applyAboutFormat(cmd, val) {
    const visual = aboutVisual();
    if (!visual || !cmd) return;
    const composer = aboutComposer();
    if (composer && composer.classList.contains("is-source")) return;
    visual.focus();
    if (cmd === "createLink") {
      const url = window.prompt("Adresse du lien :", "https://");
      if (!url) return;
      document.execCommand("createLink", false, url);
    } else if (cmd === "formatBlock") {
      const tag = val || "p";
      if (!document.execCommand("formatBlock", false, tag)) {
        document.execCommand("formatBlock", false, "<" + tag + ">");
      }
    } else {
      document.execCommand(cmd, false, val || null);
    }
    syncAboutEditor();
    markAboutVisualEmpty();
    refreshAboutSaveState();
    refreshAboutFmtState();
  }
  function refreshAboutFmtState() {
    const composer = aboutComposer();
    if (!composer || composer.classList.contains("is-source")) return;
    composer.querySelectorAll(".abt-fmt-btn[data-cmd]").forEach((btn) => {
      const cmd = btn.getAttribute("data-cmd");
      let on = false;
      try {
        if (cmd === "formatBlock") {
          const want = (btn.getAttribute("data-val") || "").replace(/[<>]/g, "").toUpperCase();
          const cur = (document.queryCommandValue("formatBlock") || "").replace(/[<>]/g, "").toUpperCase();
          on = !!want && cur === want;
        } else if (cmd === "createLink" || cmd === "unlink" || cmd === "undo" || cmd === "redo"
            || cmd === "indent" || cmd === "outdent" || cmd === "removeFormat" || cmd === "insertHorizontalRule") {
          on = false;
        } else {
          on = document.queryCommandState(cmd);
        }
      } catch (err) {
        on = false;
      }
      btn.classList.toggle("is-on", on);
    });
  }
  function toggleAboutSource() {
    const composer = aboutComposer();
    const visual = aboutVisual();
    const ta = aboutTextarea();
    if (!composer || !ta) return;
    const on = !composer.classList.contains("is-source");
    if (on) {
      if (visual) ta.value = visual.innerHTML;
      composer.classList.add("is-source");
      ta.focus();
    } else {
      if (visual) visual.innerHTML = ta.value;
      composer.classList.remove("is-source");
      markAboutVisualEmpty();
      if (visual) visual.focus();
    }
    const btn = composer.querySelector("[data-act='about-src']");
    if (btn) btn.setAttribute("aria-pressed", String(on));
    refreshAboutSaveState();
  }
  function currentAboutHtml() {
    const composer = aboutComposer();
    const ta = aboutTextarea();
    const visual = aboutVisual();
    if (!composer) return "";
    if (composer.classList.contains("is-source") && ta) return ta.value;
    if (visual) return visual.innerHTML;
    return ta ? ta.value : "";
  }
  let aboutBaseline = null;
  let aboutHadComposer = false;
  function rememberAboutBaseline() {
    aboutBaseline = currentAboutHtml();
    refreshAboutSaveState();
  }
  function maybeRememberAboutBaseline() {
    const has = !!aboutComposer();
    const swapped = has !== aboutHadComposer;
    if (has && !aboutHadComposer) rememberAboutBaseline();
    if (!has) aboutBaseline = null;
    aboutHadComposer = has;
    refreshAboutSaveState();
    if (swapped) playAboutEnter();
  }
  function playAboutEnter() {
    const about = document.querySelector(".th-about");
    if (!about) return;
    const panel = about.querySelector(".abt-composer")
      || about.querySelector(".abt-fold")
      || about.querySelector(".abt-empty");
    if (panel) panel.classList.add("is-enter");
  }
  function interceptAboutSwap(e) {
    const edit = e.target.closest(".abt-edit-btn");
    const cancel = e.target.closest(".abt-actions .abt-cancel");
    const trigger = edit || cancel;
    if (!trigger) return;
    const about = trigger.closest(".th-about") || document.querySelector(".th-about");
    if (!about) return;
    if (about.classList.contains("is-swap")) return;
    e.preventDefault();
    e.stopImmediatePropagation();
    if (about.classList.contains("is-leaving")) return;
    const leaving = cancel
      ? about.querySelector(".abt-composer")
      : (about.querySelector(".abt-fold") || about.querySelector(".abt-empty"));
    about.classList.add("is-leaving");
    if (leaving) leaving.classList.add("is-leave");
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let done = false;
    const go = () => {
      if (done) return;
      done = true;
      about.classList.remove("is-leaving");
      about.classList.add("is-swap");
      trigger.click();
    };
    if (reduce || !leaving) {
      go();
      return;
    }
    leaving.addEventListener("animationend", go, { once: true });
    setTimeout(go, 280);
  }
  function refreshAboutSaveState() {
    const btn = $("#aboutSaveBtn");
    if (!btn) return;
    const dirty = currentAboutHtml() !== aboutBaseline;
    btn.classList.toggle("is-off", !dirty);
    btn.setAttribute("aria-disabled", String(!dirty));
    if (!dirty) btn.setAttribute("tabindex", "-1");
    else btn.removeAttribute("tabindex");
  }
  window.onPreviewAboutSave = function (data) {
    const btns = document.querySelectorAll(".abt-save");
    if (data.status === "begin") {
      syncAboutEditor();
      hideConfirm("#aboutSaveConfirm");
      btns.forEach((btn) => btn.classList.add("is-busy"));
    }
    if (data.status === "success" || data.status === "complete") {
      btns.forEach((btn) => btn.classList.remove("is-busy", "is-click"));
    }
    if (data.status === "success") {
      requestAnimationFrame(showAboutSaveToast);
    }
  };
  window.onPreviewCorpusDialog = function (data) {
    const src = data.source;
    const cancel = src && /previewCorpusCancel/.test(src.id || "");
    const creating = src && src.id === "previewCorpusCreateOk";
    if (data.status === "begin") {
      if (src) src.classList.add("is-click");
      if (creating) hideConfirm("#previewCorpusCreateConfirm");
      if (cancel) {
        hideConfirm("#previewCorpusCreateConfirm");
        const overlay = document.getElementById("previewCorpusOverlay");
        if (overlay) overlay.classList.add("is-off");
        const del = document.getElementById("previewCorpusDeleteConfirm");
        if (del) del.classList.add("is-off");
      }
      return;
    }
    if (src) src.classList.remove("is-click");
    if (data.status !== "success") return;
    hideConfirm("#previewCorpusCreateConfirm");
    const overlay = document.getElementById("previewCorpusOverlay");
    if (overlay) {
      const formPane = overlay.querySelector("#previewCorpusFields");
      const formOpen = formPane && !formPane.classList.contains("is-off");
      overlay.classList.toggle("is-off", !formOpen);
      const first = overlay.querySelector("#previewCorpusFields:not(.is-off) .st-input");
      if (first && formOpen) requestAnimationFrame(() => first.focus());
    }
    const saved = src && /previewCorpus(CreateOk|Apply|Delete)$/.test(src.id || "");
    if (!saved) return;
    const msg = $("#previewCorpusSaveMsg");
    if (!msg) return;
    const text = (msg.textContent || "").trim();
    if (!text || msg.getAttribute("data-ok") !== "true") return;
    markSettingsDraft();
    const el = $("#previewCorpusToast");
    const txt = $("#previewCorpusToastTxt");
    if (!el) return;
    if (txt) txt.textContent = text;
    el.hidden = false;
    clearTimeout(el._t);
    el._t = setTimeout(() => { el.hidden = true; }, 2200);
  };
  window.onPreviewAlignDialog = function (data) {
    const src = data.source;
    const cancel = src && /previewAlignCancel/.test(src.id || "");
    if (data.status === "begin") {
      if (src) src.classList.add("is-click");
      if (src && src.id === "previewAlignCreate") src.classList.add("is-busy");
      if (cancel) {
        const overlay = document.getElementById("previewAlignOverlay");
        if (overlay) overlay.classList.add("is-off");
        const del = document.getElementById("previewAlignDeleteConfirm");
        if (del) del.classList.add("is-off");
      }
      return;
    }
    if (src) src.classList.remove("is-click", "is-busy");
    if (data.status !== "success") return;
    const overlay = document.getElementById("previewAlignOverlay");
    if (overlay) {
      const formPane = overlay.querySelector("#previewAlignFields");
      const formOpen = formPane && !formPane.classList.contains("is-off");
      overlay.classList.toggle("is-off", !formOpen);
      const first = overlay.querySelector("#previewAlignFields:not(.is-off) .st-input");
      if (first && formOpen) requestAnimationFrame(() => first.focus());
    }
    const saved = src && /previewAlign(Create|Apply|Delete)$/.test(src.id || "");
    if (!saved) return;
    const msg = $("#previewAlignSaveMsg");
    if (!msg) return;
    const text = (msg.textContent || "").trim();
    if (!text || msg.getAttribute("data-ok") !== "true") return;
    markSettingsDraft();
    const el = $("#previewAlignToast");
    const txt = $("#previewAlignToastTxt");
    if (!el) return;
    if (txt) txt.textContent = text;
    el.hidden = false;
    clearTimeout(el._t);
    el._t = setTimeout(() => { el.hidden = true; }, 2200);
  };
  window.onPreviewPrefSave = function (data) {
    if (data.status === "begin") {
      hideConfirm("#stSaveConfirm");
    }
    if (data.status === "error") {
      toast("L'enregistrement a échoué. Réessayez.");
      return;
    }
    if (data.status !== "success") return;
    const msg = $("#previewPrefSaveMsg");
    if (!msg) return;
    const text = (msg.textContent || "").trim();
    if (!text) {
      toast("L'enregistrement n'a pas pu être effectué.");
      return;
    }
    if (msg.getAttribute("data-ok") === "true") {
      requestAnimationFrame(rememberSettingsBaseline);
      const el = $("#stSaveToast");
      if (!el) return;
      el.hidden = false;
      clearTimeout(el._t);
      el._t = setTimeout(() => { el.hidden = true; }, 2200);
      return;
    }
    toast(text);
  };
  function showAboutSaveToast() {
    const msg = $("#previewAboutSaveMsg");
    if (!msg) return;
    const text = (msg.textContent || "").trim();
    if (!text || msg.getAttribute("data-ok") !== "true") return;
    toast(text, { soft: true });
  }
  function syncAboutFold() {
    const fold = $("#aboutFold");
    if (!fold) return;
    const html = fold.querySelector(".abt-html");
    const btn = fold.querySelector(".abt-toggle");
    if (!html) return;
    const wasOpen = fold.classList.contains("is-open");
    fold.classList.remove("is-open", "is-expandable", "is-short");
    if (btn) {
      btn.classList.remove("open");
      btn.setAttribute("aria-expanded", "false");
    }
    fold.classList.add("is-measuring");
    void html.offsetHeight;
    const overflowing = html.scrollHeight > html.clientHeight + 4;
    fold.classList.remove("is-measuring");
    fold.classList.toggle("is-expandable", overflowing);
    fold.classList.toggle("is-short", !overflowing);
    if (wasOpen && overflowing) {
      fold.classList.add("is-open");
      if (btn) {
        btn.classList.add("open");
        btn.setAttribute("aria-expanded", "true");
      }
    }
  }

  if (window.faces && faces.ajax) {
    faces.ajax.addOnEvent((data) => {
      if (data.status === "begin") syncAboutEditor();
      if (data.status === "success") {
        const srcId = (data.source && data.source.id) || "";
        if (srcId === "previewCorpusToggleGo" || srcId === "previewAlignToggleGo") {
          markSettingsDraft();
        }
        requestAnimationFrame(() => {
        syncAboutFold();
        markAboutVisualEmpty();
        maybeRememberAboutBaseline();
        refreshAboutFmtState();
        applyStatusFilter();
        applySort();
        restoreSelection();
        showLiveDetail();
        if (window.syncViewRail) window.syncViewRail();
      });
      }
    });
  } else if (window.jsf && jsf.ajax) {
    jsf.ajax.addOnEvent((data) => {
      if (data.status === "begin") syncAboutEditor();
      if (data.status === "success") {
        const srcId = (data.source && data.source.id) || "";
        if (srcId === "previewCorpusToggleGo" || srcId === "previewAlignToggleGo") {
          markSettingsDraft();
        }
        requestAnimationFrame(() => {
        syncAboutFold();
        markAboutVisualEmpty();
        maybeRememberAboutBaseline();
        refreshAboutFmtState();
        applyStatusFilter();
        applySort();
        restoreSelection();
        showLiveDetail();
        if (window.syncViewRail) window.syncViewRail();
      });
      }
    });
  }

  function scrollToPrefHash() {
    const id = (location.hash || "").replace(/^#/, "");
    if (!id || SCREEN !== "preference") return false;
    const el = document.getElementById(id);
    const view = $("#previewView") || document.querySelector("main.content .view");
    if (!el || !view) return false;
    const top = el.getBoundingClientRect().top - view.getBoundingClientRect().top + view.scrollTop - 12;
    view.scrollTo({ top: Math.max(0, top) });
    if (window.syncViewRail) window.syncViewRail();
    return true;
  }

  if (SCREEN === "atelier") {
    setBatch(params.get("obj") || "alignements");
  }
  if (SCREEN === "candidats" && (params.get("new") === "1" || params.get("pref") || params.get("path"))) {
    createCandidate({
      getAttribute: (name) => {
        if (name === "data-pref") return params.get("pref") || "";
        if (name === "data-path") return params.get("path") || "";
        return "";
      }
    });
  }
  if (IS_CONSULT) {
    const q = params.get("q");
    const id = params.get("id");
    const view = params.get("view");
    if (q) {
      if (input) input.value = q;
      if (clear) clear.hidden = !q;
      runSearch();
    } else if (id) {
      if (!openLiveDetail(id, params.get("type") || "")) {
        openConcept(id);
      }
    } else if (view) {
      setView(view);
    } else {
      paint();
    }
  } else {
    paint();
  }
  function bindPrefSwitches() {
    const sync = (input) => {
      if (!input || input.type !== "checkbox") return;
      const sw = input.closest(".st-sw");
      if (!sw || sw.closest(".st-table")) return;
      sw.classList.toggle("on", input.checked);
      const integ = input.closest(".st-integ");
      if (integ) integ.classList.toggle("on", input.checked);
    };
    document.addEventListener("change", (e) => {
      const input = e.target;
      if (!input || !input.closest || !input.closest(".st-sw")) return;
      sync(input);
    });
    $$(".st-sw input[type='checkbox']").forEach(sync);
  }
  function bindViewRail() {
    const view = $("#previewView") || document.querySelector("main.content .view");
    const home = $("#viewHome");
    const settings = $("#viewSettings");
    const rail = $("#previewRail");
    const thumb = $("#previewRailThumb");
    const goTop = $("#previewGoTop");
    const shell = view && view.closest(".content");
    if (!view) return;
    const scrollers = [view, home, settings].filter((el, i, arr) => el && arr.indexOf(el) === i);
    let idle;
    const mark = () => {
      if (shell) shell.classList.add("is-scrolling");
      clearTimeout(idle);
      idle = setTimeout(() => { if (shell) shell.classList.remove("is-scrolling"); }, 900);
    };
    const currentScroll = () => scrollers.reduce((max, el) => Math.max(max, el.scrollTop || 0), 0);
    const sync = () => {
      const st = currentScroll();
      if (goTop) {
        const off = st < 72;
        goTop.classList.toggle("is-off", off);
        goTop.setAttribute("aria-hidden", String(off));
        if (off) goTop.setAttribute("tabindex", "-1");
        else goTop.removeAttribute("tabindex");
      }
      if (!rail || !thumb) return;
      const ch = view.clientHeight;
      const sh = view.scrollHeight;
      if (sh <= ch + 4) {
        rail.classList.add("is-off");
        return;
      }
      rail.classList.remove("is-off");
      const track = rail.clientHeight;
      const h = Math.max(40, Math.round((ch / sh) * track));
      const max = Math.max(0, track - h);
      const y = max === 0 || sh === ch ? 0 : (st / (sh - ch)) * max;
      thumb.style.height = h + "px";
      thumb.style.transform = "translate3d(0," + y + "px,0)";
    };
    const scrollFromY = (clientY) => {
      if (!rail || !thumb) return;
      const rect = rail.getBoundingClientRect();
      const track = rail.clientHeight;
      const h = thumb.offsetHeight;
      const y = Math.min(Math.max(0, clientY - rect.top - h / 2), Math.max(0, track - h));
      const max = track - h;
      const span = view.scrollHeight - view.clientHeight;
      view.scrollTop = max <= 0 ? 0 : (y / max) * span;
    };
    scrollers.forEach((el) => el.addEventListener("scroll", () => { mark(); sync(); }, { passive: true }));
    window.addEventListener("resize", sync);
    if (rail && thumb) {
      rail.addEventListener("pointerdown", (e) => {
        e.preventDefault();
        rail.classList.add("is-drag");
        if (shell) shell.classList.add("is-scrolling");
        scrollFromY(e.clientY);
        rail.setPointerCapture(e.pointerId);
      });
      rail.addEventListener("pointermove", (e) => {
        if (!rail.classList.contains("is-drag")) return;
        scrollFromY(e.clientY);
      });
      rail.addEventListener("pointerup", () => rail.classList.remove("is-drag"));
      rail.addEventListener("pointercancel", () => rail.classList.remove("is-drag"));
    }
    window.syncViewRail = sync;
    requestAnimationFrame(sync);
  }
  function fetchStat(path) {
    const ctx = document.body.getAttribute("data-ctx") || "";
    return fetch(ctx + "/v2/api/stats/" + path, {
      headers: { Accept: "application/json" }
    }).then((res) => (res.ok ? res.json() : Promise.reject()));
  }

  function formatInt(value) {
    const n = Number(value);
    return Number.isFinite(n) ? n.toLocaleString("fr-FR") : "—";
  }

  function capitalizeLabel(value) {
    const text = String(value == null ? "" : value);
    if (!text) return text;
    return text.charAt(0).toUpperCase() + text.slice(1);
  }

  function monthLabel(key) {
    const parts = String(key || "").split("-");
    if (parts.length < 2) return "—";
    const date = new Date(Number(parts[0]), Number(parts[1]) - 1, 1);
    if (Number.isNaN(date.getTime())) return "—";
    return new Intl.DateTimeFormat("fr-FR", { month: "short", year: "2-digit" }).format(date)
      .replace(/\./g, "")
      .replace(/\u00a0/g, " ")
      .replace(/\u202f/g, " ")
      .trim();
  }

  function fillKpi(el, value, formatted) {
    if (!el) return 0;
    return fillStatNode(el, {
      value: value,
      formatted: formatted != null ? formatted : formatInt(value)
    });
  }

  function fillOverviewKpis(kpis) {
    const map = kpis || {};
    $$("[data-stat]").forEach((el) => {
      const key = el.getAttribute("data-stat");
      if (!key || key === "max-depth" || key === "without-definition") return;
      if (Object.prototype.hasOwnProperty.call(map, key)) fillKpi(el, map[key]);
    });
  }

  function dashStatNodes(nodes) {
    nodes.forEach((el) => {
      el.textContent = "—";
      el.removeAttribute("aria-busy");
    });
  }

  function fillStatNode(el, data) {
    const formatted = data && data.formatted;
    const value = Number(data && data.value);
    el.textContent = formatted
      || (Number.isFinite(value) ? value.toLocaleString("fr-FR") : "—");
    el.removeAttribute("aria-busy");
    el.classList.add("is-ready");
    return Number.isFinite(value) ? value : 0;
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function coveragePercent(translatedCount, total) {
    if (!total) return 0;
    return Math.max(0, Math.min(100, Math.round((Number(translatedCount) || 0) * 100 / total)));
  }

  function animateBarFills(container) {
    const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const fills = $$(".mg-bar-fill", container);
    const paint = () => {
      fills.forEach((fill, i) => {
        const pct = fill.getAttribute("data-width") || "0";
        const display = fill.getAttribute("data-display") || pct + "%";
        const labelN = fill.closest(".mg-bar-row") && fill.closest(".mg-bar-row").querySelector(".mg-bar-n");
        if (reduceMotion) {
          fill.style.width = pct + "%";
          if (labelN) labelN.textContent = display;
          return;
        }
        fill.style.transitionDelay = (i * 70) + "ms";
        fill.style.width = pct + "%";
        if (labelN) {
          window.setTimeout(() => { labelN.textContent = display; }, 70 * i + 180);
        }
      });
    };
    requestAnimationFrame(() => requestAnimationFrame(paint));
  }

  function renderStatBars(container, items) {
    if (!items.length) {
      container.innerHTML = '<div class="mg-bar-row"><span class="mg-bar-l">—</span></div>';
      container.removeAttribute("aria-busy");
      if (window.syncViewRail) window.syncViewRail();
      return;
    }
    container.innerHTML = items.map((item) => {
      const label = escapeHtml(item.label || "—");
      const start = escapeHtml(item.startDisplay || "0");
      return '<div class="mg-bar-row">'
        + '<span class="mg-bar-l">' + label + "</span>"
        + '<span class="mg-bar-track"><span class="mg-bar-fill" data-width="' + item.widthPct
        + '" data-display="' + escapeHtml(item.display) + '"></span></span>'
        + '<span class="mg-bar-n">' + start + "</span>"
        + "</div>";
    }).join("");
    container.removeAttribute("aria-busy");
    if (window.syncViewRail) window.syncViewRail();
    animateBarFills(container);
  }

  function renderLanguageCoverage(container, total, languages) {
    renderStatBars(container, (languages || []).map((item) => {
      const pct = coveragePercent(item.translatedCount, total);
      return {
        label: capitalizeLabel(item.label || item.code || "—"),
        widthPct: pct,
        display: pct + "%",
        startDisplay: "0%"
      };
    }));
  }

  function renderCollectionCoverage(container, collections) {
    const rows = collections || [];
    const max = rows.reduce((acc, row) => Math.max(acc, Number(row.memberCount) || 0), 0);
    renderStatBars(container, rows.map((item) => {
      const count = Number(item.memberCount) || 0;
      return {
        label: item.label || item.id || "—",
        widthPct: max ? Math.round(count * 100 / max) : 0,
        display: count.toLocaleString("fr-FR"),
        startDisplay: "0"
      };
    }));
  }

  function metricValue(body, key) {
    if (body && body[key] && typeof body[key] === "object" && body[key].value != null) {
      return Number(body[key].value) || 0;
    }
    return Number(body && body[key]) || 0;
  }

  function renderCandidateOutcome(container, body) {
    const accepted = metricValue(body, "accepted");
    const pendingCount = metricValue(body, "pending");
    const rejected = metricValue(body, "rejected");
    const total = accepted + pendingCount + rejected;
    const cap = $("[data-stat-outcome-cap]");
    if (cap) {
      cap.textContent = total === 1
        ? "1 terme proposé"
        : total.toLocaleString("fr-FR") + " termes proposés";
    }
    if (!total) {
      container.innerHTML = '<p class="cs-foot">Aucune proposition pour le moment.</p>';
      container.removeAttribute("aria-busy");
      if (window.syncViewRail) window.syncViewRail();
      return;
    }
    const pct = (n) => Math.round(n * 1000 / total) / 10;
    const segs = [
      { cls: "t-ins", count: accepted, label: "Insérés" },
      { cls: "t-cand", count: pendingCount, label: "En attente" },
      { cls: "t-rej", count: rejected, label: "Rejetés" }
    ];
    const pipe = segs.filter((s) => s.count > 0).map((s) => (
      '<span class="cs-seg ' + s.cls + '" data-width="' + pct(s.count) + '"><b>'
      + s.count.toLocaleString("fr-FR") + "</b></span>"
    )).join("");
    const legend = segs.map((s) => (
      '<span class="cs-lg"><i class="cs-dot ' + s.cls + '"></i>'
      + s.label + " — " + s.count.toLocaleString("fr-FR") + "</span>"
    )).join("");
    const decided = accepted + rejected;
    const decidedRate = decided ? Math.round(accepted * 100 / decided) : 0;
    const medianRaw = body && body.medianDecisionDays;
    const median = medianRaw && typeof medianRaw === "object"
      ? (medianRaw.formatted !== "—" ? Number(medianRaw.value) : null)
      : (medianRaw == null ? null : Number(medianRaw));
    let foot;
    if (!decided) {
      foot = "Aucune proposition n'a encore été tranchée.";
    } else {
      foot = "Sur <b>" + decided.toLocaleString("fr-FR") + "</b> proposition"
        + (decided > 1 ? "s" : "") + " déjà tranchée" + (decided > 1 ? "s" : "")
        + ", <b>" + decidedRate + "\u202f%</b> ont été acceptées.";
      if (median != null && Number.isFinite(median)) {
        foot += " Délai médian entre proposition et décision : <b>"
          + median.toLocaleString("fr-FR") + "\u202fjour" + (median > 1 ? "s" : "")
          + "</b>.";
      }
    }
    container.innerHTML = '<div class="cs-pipe">' + pipe + "</div>"
      + '<div class="cs-legend">' + legend + "</div>"
      + '<p class="cs-foot">' + foot + "</p>";
    container.removeAttribute("aria-busy");
    const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const paint = () => {
      $$(".cs-seg", container).forEach((seg, i) => {
        if (!reduceMotion) seg.style.transitionDelay = (i * 70) + "ms";
        seg.style.width = (seg.getAttribute("data-width") || "0") + "%";
      });
    };
    requestAnimationFrame(() => requestAnimationFrame(paint));
    if (window.syncViewRail) window.syncViewRail();
  }

  function monthCount(row, key) {
    return Math.max(0, Number(row && row[key]) || 0);
  }

  function monthTooltip(row) {
    const total = monthCount(row, "total");
    const parts = [];
    const accepted = monthCount(row, "accepted");
    const pendingCount = monthCount(row, "pending");
    const rejected = monthCount(row, "rejected");
    if (accepted) parts.push(accepted.toLocaleString("fr-FR") + " inséré" + (accepted > 1 ? "s" : ""));
    if (pendingCount) parts.push(pendingCount.toLocaleString("fr-FR") + " en attente");
    if (rejected) parts.push(rejected.toLocaleString("fr-FR") + " rejeté" + (rejected > 1 ? "s" : ""));
    const head = total === 1 ? "1 proposé" : total.toLocaleString("fr-FR") + " proposés";
    return parts.length ? head + " · " + parts.join(", ") : head;
  }

  function renderCandidateMonths(container, months) {
    const rows = months || [];
    const max = rows.reduce((acc, row) => Math.max(acc, monthCount(row, "total")), 0);
    const cols = rows.map((row) => {
      const total = monthCount(row, "total");
      const height = max ? Math.max(8, Math.round(total * 100 / max)) : 0;
      const segs = [
        { cls: "t-ins", count: monthCount(row, "accepted") },
        { cls: "t-cand", count: monthCount(row, "pending") },
        { cls: "t-rej", count: monthCount(row, "rejected") }
      ].filter((s) => s.count > 0).map((s) => (
        '<span class="cs-col-seg ' + s.cls + '" style="flex:' + s.count + '"></span>'
      )).join("");
      return '<div class="cs-col">'
        + '<div class="cs-col-plot"><div class="cs-col-stack'
        + (total ? "" : " is-empty")
        + '" data-height="' + (total ? height : 0) + '" title="' + escapeHtml(monthTooltip(row)) + '">'
        + segs + "</div></div>"
        + '<span class="cs-col-n" data-count="' + total + '">0</span>'
        + '<span class="cs-col-lbl">' + escapeHtml(row.label || monthLabel(row.key) || "—") + "</span>"
        + "</div>";
    }).join("");
    const legend = '<div class="cs-legend">'
      + '<span class="cs-lg"><i class="cs-dot t-ins"></i>Insérés</span>'
      + '<span class="cs-lg"><i class="cs-dot t-cand"></i>En attente</span>'
      + '<span class="cs-lg"><i class="cs-dot t-rej"></i>Rejetés</span>'
      + "</div>";
    container.innerHTML = '<div class="cs-months">' + (cols || "") + "</div>" + legend;
    container.removeAttribute("aria-busy");
    const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const paint = () => {
      $$(".cs-col-stack", container).forEach((stack, i) => {
        if (stack.classList.contains("is-empty")) return;
        const height = stack.getAttribute("data-height") || "0";
        if (!reduceMotion) stack.style.transitionDelay = (i * 70) + "ms";
        stack.style.height = height + "%";
      });
      $$(".cs-col-n", container).forEach((label, i) => {
        const count = Number(label.getAttribute("data-count")) || 0;
        const text = count.toLocaleString("fr-FR");
        if (reduceMotion) {
          label.textContent = text;
          return;
        }
        window.setTimeout(() => { label.textContent = text; }, 70 * i + 180);
      });
    };
    requestAnimationFrame(() => requestAnimationFrame(paint));
    if (window.syncViewRail) window.syncViewRail();
  }

  function fillCandidateLife(body) {
    const life = body || {};
    const rate = Number(life.acceptanceRatePercent);
    const median = life.medianDecisionDays;
    $$("[data-stat-life]").forEach((el) => {
      const key = el.getAttribute("data-stat-life");
      if (key === "pending") fillKpi(el, life.pending);
      else if (key === "accepted12m") fillKpi(el, life.acceptedLast12Months);
      else if (key === "rejected12m") fillKpi(el, life.rejectedLast12Months);
      else if (key === "acceptanceRate") {
        fillKpi(el, rate, Number.isFinite(rate) ? formatInt(rate) + "\u202f%" : "—");
      } else if (key === "medianDecisionDays") {
        fillKpi(
          el,
          median,
          median == null ? "—" : formatInt(median) + "\u202fj"
        );
      } else if (key === "activeContributors") fillKpi(el, life.activeContributors);
    });
  }

  function applyOverview(body) {
    const overview = body || {};
    fillOverviewKpis(overview.kpis);
    const languageCoverage = $("[data-stat-coverage='language']");
    if (languageCoverage) {
      renderLanguageCoverage(
        languageCoverage,
        Number(overview.kpis && overview.kpis.concepts) || 0,
        overview.languages
      );
    }
    const collectionCoverage = $("[data-stat-coverage='collections']");
    if (collectionCoverage) {
      renderCollectionCoverage(collectionCoverage, overview.collections);
    }
    const cap = $("[data-stat-collections-cap]");
    if (cap) {
      cap.textContent = overview.collectionsTruncated
        ? "12 micro-thésaurus les plus peuplés"
        : "";
    }
    fillCandidateLife(overview.candidates);
    const outcome = $("[data-stat-outcome]");
    if (outcome) renderCandidateOutcome(outcome, overview.candidates || {});
    const months = $("[data-stat-months]");
    if (months) renderCandidateMonths(months, overview.months);
  }

  function applyCompleteness(body) {
    fillKpi($("[data-stat='max-depth']"), body && body.maxDepth);
    fillKpi($("[data-stat='without-definition']"), body && body.withoutDefinition);
  }

  function loadStatKpis() {
    const kpiNodes = $$("[data-stat]");
    const dashboard = $("[data-stat-life]")
      || $("[data-stat-coverage='language']")
      || $("[data-stat-coverage='collections']")
      || $("[data-stat-outcome]")
      || $("[data-stat-months]");
    if (!kpiNodes.length && !dashboard) return;

    if (dashboard) {
      fetchStat("overview")
        .then(applyOverview)
        .catch(() => {
          dashStatNodes(kpiNodes.filter((el) => {
            const key = el.getAttribute("data-stat");
            return key !== "max-depth" && key !== "without-definition";
          }));
          dashStatNodes($$("[data-stat-life]"));
          ["language", "collections"].forEach((kind) => {
            const el = $("[data-stat-coverage='" + kind + "']");
            if (el) {
              el.innerHTML = '<div class="mg-bar-row"><span class="mg-bar-l">—</span></div>';
              el.removeAttribute("aria-busy");
            }
          });
          const outcome = $("[data-stat-outcome]");
          if (outcome) {
            outcome.innerHTML = '<p class="cs-foot">—</p>';
            outcome.removeAttribute("aria-busy");
          }
          const months = $("[data-stat-months]");
          if (months) {
            months.innerHTML = '<p class="cs-foot">—</p>';
            months.removeAttribute("aria-busy");
          }
        });
      fetchStat("completeness")
        .then(applyCompleteness)
        .catch(() => {
          dashStatNodes($$("[data-stat='max-depth'], [data-stat='without-definition']"));
        });
      return;
    }

    fetchStat("kpis")
      .then(fillOverviewKpis)
      .catch(() => dashStatNodes(kpiNodes));
  }

  bindPrefSwitches();
  function initSettingsLeaveGuard() {
    if (SCREEN !== "preference" || !settingsForm()) return;
    rememberSettingsBaseline();
    window.addEventListener("beforeunload", (e) => {
      if (settingsAllowLeave || !isSettingsDirty()) return;
      e.preventDefault();
      e.returnValue = "";
    });
    document.addEventListener("click", (e) => {
      if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      if (e.target.closest("[data-act]")) return;
      if (e.target.closest("#previewLogoutForm a, #previewLogoutForm .bo-btn")) {
        allowSettingsLeave();
        return;
      }
      const a = e.target.closest("a[href]");
      if (!a || a.target === "_blank" || a.hasAttribute("download")) return;
      if (a.closest("#stLeaveConfirm, #stSaveConfirm, #logoutConfirm")) return;
      const href = a.getAttribute("href");
      if (!href || href.charAt(0) === "#" || /^javascript:/i.test(href)) return;
      if (isHashOnlyNavigation(a.href)) return;
      if (!askLeaveThen(() => {
        allowSettingsLeave();
        location.href = a.href;
      })) return;
      e.preventDefault();
      e.stopPropagation();
    }, true);
  }

  bindViewRail();
  loadStatKpis();
  initSettingsLeaveGuard();
  requestAnimationFrame(() => {
    syncAboutFold();
    markAboutVisualEmpty();
    maybeRememberAboutBaseline();
    refreshAboutFmtState();
    if (window.syncViewRail) window.syncViewRail();
    scrollToPrefHash();
    if (SCREEN === "preference") rememberSettingsBaseline();
  });
  if (SCREEN === "preference" && location.hash) {
    window.addEventListener("load", scrollToPrefHash);
    setTimeout(scrollToPrefHash, 80);
    setTimeout(scrollToPrefHash, 250);
  }
})();
