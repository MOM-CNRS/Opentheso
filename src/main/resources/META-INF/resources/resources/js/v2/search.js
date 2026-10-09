/**
 * OpenTheso V2 — recherche (barre + panneau Recherche).
 */
"use strict";

var SEARCH_PAGE = 24;
var searchRequest = 0;

function searchMsg(name, fallback) {
  const box = $("#searchBox");
  const value = box && box.getAttribute(name);
  return value || fallback;
}

function searchLangParam() {
  if (state.searchAllLang) return "all";
  return (typeof thesaurusLang === "function" && thesaurusLang()) || "fr";
}

function searchApi(path, params) {
  const ctx = document.body.getAttribute("data-ctx") || "";
  const query = new URLSearchParams();
  Object.keys(params).forEach((key) => {
    const value = params[key];
    if (value != null && value !== "") query.set(key, value);
  });
  return ctx + "/v2/api/thesaurus-search" + path + "?" + query.toString();
}

function rankHits() {
  return (state.searchHits || []).map((hit) => ({
    id: hit.id,
    pref: hit.label || "",
    via: hit.via || "",
    el: null
  }));
}

function shownAcRows() {
  return $$("#ac .ac-row.is-shown");
}

function setAcIdx(i) {
  const rows = shownAcRows();
  state.acIdx = i;
  rows.forEach((row, n) => row.classList.toggle("is-active", n === i));
}

function hitIcon(kind) {
  if (kind === "group") return "▤";
  if (kind === "facet") return "◇";
  return "◆";
}

function hitNodeType(kind) {
  if (kind === "group") return "group";
  if (kind === "facet") return "facet";
  return "concept";
}

function viaHtml(hit) {
  if (hit.via === "note") {
    return '<span class="ac-via ac-via-note is-on">' + escapeHtml(searchMsg("data-msg-via-note", "via une note")) + "</span>";
  }
  if (hit.kind === "alt" && hit.via) {
    const label = escapeHtml(searchMsg("data-msg-via-syn", "synonyme"));
    return '<span class="ac-via ac-via-syn is-on" data-q="' + escapeHtml(norm(hit.via)) + '">'
      + label + " : <em>" + escapeHtml(hit.via) + "</em></span>";
  }
  return "";
}

function resultViaHtml(hit) {
  if (hit.via === "note") {
    return '<span class="rl-via rl-via-note is-on">' + escapeHtml(searchMsg("data-msg-via-note", "via une note")) + "</span>";
  }
  if (hit.kind === "alt" && hit.via) {
    const label = escapeHtml(searchMsg("data-msg-via-syn", "synonyme"));
    return '<span class="rl-via rl-via-syn is-on" data-q="' + escapeHtml(norm(hit.via)) + '">'
      + label + " : <em>" + escapeHtml(hit.via) + "</em></span>";
  }
  return "";
}

function suggestionHtml(hit) {
  const type = hitNodeType(hit.kind);
  const dep = hit.deprecated ? " is-dep" : "";
  const match = hit.via === "note" ? " match-note" : (hit.kind === "alt" ? " match-syn" : "");
  return '<button type="button" class="ac-row is-shown' + dep + match + '" data-act="open"'
    + ' data-id="' + escapeHtml(hit.id) + '" data-type="' + type + '">'
    + '<span class="ac-ico">' + hitIcon(hit.kind) + "</span>"
    + '<span class="ac-body"><span class="ac-pref">' + escapeHtml(hit.label || hit.id) + "</span>"
    + viaHtml(hit)
    + (hit.path ? '<span class="path">' + escapeHtml(hit.path) + "</span>" : "")
    + "</span></button>";
}

function resultHtml(hit) {
  const type = hitNodeType(hit.kind);
  const dep = hit.deprecated ? " is-dep" : "";
  const match = hit.via === "note" ? " match-note" : (hit.kind === "alt" ? " match-syn" : "");
  const sel = state.conceptId && state.conceptId === hit.id ? " is-sel" : "";
  return '<li class="rl-li" data-id="' + escapeHtml(hit.id) + '">'
    + '<button type="button" class="rl-item is-shown' + dep + match + sel + '" data-act="open"'
    + ' data-id="' + escapeHtml(hit.id) + '" data-type="' + type + '">'
    + '<span class="rl-pref">' + escapeHtml(hit.label || hit.id) + "</span>"
    + resultViaHtml(hit)
    + (hit.path && state.showPath ? '<span class="path">' + escapeHtml(hit.path) + "</span>" : "")
    + "</button></li>";
}

function looksLikeIdentifier(text) {
  if (!text || /\s/.test(text)) return false;
  if (/^ark:/i.test(text)) return true;
  if (/^(hdl:|handle:)/i.test(text)) return true;
  if (/^https?:\/\/hdl\.handle\.net\//i.test(text)) return true;
  if (/^\d+\.\d+\/\S+/.test(text)) return true;
  if (/^[A-Za-z]{1,12}[-_.][A-Za-z0-9][A-Za-z0-9._-]*$/.test(text) && /\d/.test(text)) return true;
  if (/^\d{2,}$/.test(text)) return true;
  if (/^[A-Za-z0-9][A-Za-z0-9._:/-]{7,}$/.test(text) && /\d/.test(text)) return true;
  return false;
}

function unwrapQuotes(text) {
  const match = String(text || "").match(/^(?:"|«|“)\s*(.+?)\s*(?:"|»|”)$/);
  if (!match) return null;
  const inner = match[1].trim();
  return inner || null;
}

function activeSearch(raw) {
  const text = (raw || "").trim();
  if (state.searchNote) {
    return { mode: "NOTE", q: text, hint: searchMsg("data-hint-note", "Note") };
  }
  const quoted = unwrapQuotes(text);
  if (quoted) {
    return { mode: "EXACT", q: quoted, hint: searchMsg("data-hint-exact", "Mot exact") };
  }
  if (looksLikeIdentifier(text)) {
    return { mode: "IDENTIFIER", q: text, hint: searchMsg("data-hint-id", "Identifiant") };
  }
  if (state.searchStartWithQuery && norm(state.searchStartWithQuery) === norm(text)) {
    return { mode: "START_WITH", q: text, hint: searchMsg("data-hint-start", "Commence par") };
  }
  return { mode: "FULL_TEXT", q: text, hint: searchMsg("data-hint-full", "Flexible") };
}

function paintSearchHint(raw) {
  const parsed = activeSearch(raw);
  state.searchMode = parsed.mode;
  state.searchHint = parsed.hint;
  const hint = $("#searchModeHint");
  if (hint) hint.textContent = parsed.hint;
  const input = $("#searchInput");
  if (input) {
    input.placeholder = state.searchNote
      ? searchMsg("data-placeholder-note", "Rechercher dans les notes")
      : searchMsg("data-placeholder", "Rechercher…");
  }
  return parsed;
}

function showEmptySearchHelp() {
  const programs = $("#acPrograms");
  const box = $("#searchBox");
  const ac = $("#ac");
  const list = $("#acList");
  if (list) list.innerHTML = "";
  const start = $("#acStartWith");
  if (start) start.hidden = true;
  const more = $("#acSeeAll");
  if (more) more.hidden = true;
  state.acIdx = -1;
  if (!programs || !box || !ac) {
    if (box) box.classList.remove("is-open");
    if (ac) ac.classList.remove("is-programs", "has-hits", "is-empty");
    return;
  }
  programs.hidden = false;
  ac.classList.add("is-programs");
  ac.classList.remove("has-hits", "is-empty");
  box.classList.add("is-open");
}

function paintSuggestions(data, query) {
  const ac = $("#ac");
  const list = $("#acList");
  const box = $("#searchBox");
  if (!ac || !list || !box) return;
  const programs = $("#acPrograms");
  if (programs) programs.hidden = true;
  ac.classList.remove("is-programs");
  const hits = (data && data.hits) || [];
  const total = (data && data.total) || hits.length;
  list.innerHTML = hits.map(suggestionHtml).join("");
  const q = $("#acQuery");
  if (q) q.textContent = query;
  ac.classList.toggle("has-hits", hits.length > 0);
  ac.classList.toggle("is-empty", hits.length === 0);
  box.classList.add("is-open");
  state.acIdx = -1;
  const parsed = activeSearch(query);
  const offer = $("#acStartWith");
  const folded = norm(query.trim());
  const canOffer = parsed.mode === "FULL_TEXT" && folded.length >= 2
    && hits.some((hit) => norm(hit.label || "").startsWith(folded));
  if (offer) {
    offer.hidden = !canOffer;
    const offerQ = $("#acStartWithQ");
    if (offerQ) offerQ.textContent = query.trim();
  }
  const more = $("#acSeeAll");
  const moreN = $("#acSeeAllN");
  if (more) {
    more.hidden = !(total > hits.length);
    if (moreN) moreN.textContent = String(total);
  }
  if (typeof paintQueryHighlight === "function") paintQueryHighlight();
}

function filterAc(query) {
  const q = (query || "").trim();
  clearTimeout(filterAc._t);
  const box = $("#searchBox");
  const ac = $("#ac");
  if (!q) {
    searchRequest += 1;
    paintSearchHint("");
    showEmptySearchHelp();
    if (box && ac && !$("#acPrograms")) box.classList.remove("is-open");
    return;
  }
  paintSearchHint(q);
  filterAc._t = setTimeout(() => fetchSuggestions(q), 280);
}

function fetchSuggestions(query) {
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  if (!theso) return;
  const parsed = paintSearchHint(query);
  const seq = ++searchRequest;
  fetch(searchApi("/suggest", {
    thesaurusId: theso,
    lang: searchLangParam(),
    q: parsed.q,
    mode: parsed.mode
  }), { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((response) => (response.ok ? response.json() : { hits: [], total: 0 }))
    .then((data) => {
      if (seq !== searchRequest) return;
      const live = ($("#searchInput") && $("#searchInput").value || "").trim();
      if (live !== query) return;
      paintSuggestions(data, query);
    })
    .catch(() => {
      if (seq !== searchRequest) return;
      paintSuggestions({ hits: [], total: 0 }, query);
    });
}

function paintCommittedResults() {
  const rl = $("#resultsList");
  if (!rl) return;
  const hits = state.searchHits || [];
  const query = (state.committed || "").trim();
  const title = (state.searchTitle || "").trim();
  const list = $("#rlList");
  if (!query && !title) {
    rl.classList.remove("is-empty", "is-filled");
    rl.classList.add("is-idle");
    if (list) list.innerHTML = "";
    return;
  }
  rl.classList.remove("is-idle");
  rl.classList.toggle("is-empty", hits.length === 0);
  rl.classList.toggle("is-filled", hits.length > 0);
  if (list) list.innerHTML = hits.map(resultHtml).join("");
  const count = $("#rlN");
  if (count) count.textContent = String(state.searchTotal || hits.length);
  const label = $("#rlLbl");
  if (label) label.textContent = searchMsg("data-msg-results", "résultats");
  const head = $("#rlHeadQ");
  if (head) head.textContent = title || ((state.searchHint && query) ? state.searchHint + " · " + query : query);
  const none = $("#rlQuery");
  if (none) none.textContent = title || query;
  const more = $("#rlMore");
  if (more) {
    const rest = Math.max(0, (state.searchTotal || 0) - hits.length);
    more.hidden = rest === 0;
    const moreN = $("#rlMoreN");
    if (moreN) moreN.textContent = "+" + Math.min(rest, SEARCH_PAGE);
  }
  if (typeof paintQueryHighlight === "function") paintQueryHighlight();
  if (typeof paintBadges === "function") paintBadges();
}

function syncRechercheConcept() {
  const hits = state.searchHits || [];
  if (state.conceptId && hits.some((hit) => hit.id === state.conceptId)) {
    highlightConcept(state.conceptId);
  }
}

function openSearchRow(row) {
  if (!row) return;
  openSearchHit({
    id: row.getAttribute("data-id"),
    kind: row.getAttribute("data-type") === "group" ? "group"
      : row.getAttribute("data-type") === "facet" ? "facet" : "concept"
  }, !!row.closest("#panelResults, #resultsList"));
}

function openSearchHit(hit, stay) {
  if (!hit || !hit.id) return;
  if (hit.kind === "group") {
    if (typeof openCollectionFromTree === "function") openCollectionFromTree(hit.id);
    closeSearchUi();
    return;
  }
  const nodeType = hit.kind === "facet" ? "facet" : "concept";
  if (typeof openLiveDetail === "function" && openLiveDetail(hit.id, nodeType)) {
    state.home = false;
    state.draft = false;
    state.conceptDraft = false;
    state.facetDraft = false;
    state.conceptId = hit.id;
    if (stay) state.view = "recherche";
    else state.view = "arbo";
    highlightConcept(hit.id);
    closeSearchUi();
    if (typeof paint === "function") paint();
    return;
  }
  if (typeof openConcept === "function") openConcept(hit.id, stay ? "stay" : "jump");
}

function applySearchPayload(data, append) {
  const hits = (data && data.hits) || [];
  state.searchHits = append ? (state.searchHits || []).concat(hits) : hits;
  state.searchTotal = (data && typeof data.total === "number") ? data.total : state.searchHits.length;
  if (!append && !(data && data.mode === "PROGRAMMED")) {
    state.searchTitle = "";
    state.searchKind = "";
  }
  state.view = "recherche";
  state.home = false;
  closeSearchUi();
  paintCommittedResults();
  if (typeof paint === "function") paint();
  if (!append && state.searchHits.length === 1) {
    openSearchHit(state.searchHits[0], true);
  } else if (!append && !state.searchHits.length && typeof toast === "function") {
    toast(searchMsg("data-msg-none", "Aucun résultat"));
  }
}

function runSearch() {
  const input = $("#searchInput");
  const raw = ((input && input.value) || "").trim();
  if (!raw) {
    showEmptySearchHelp();
    return;
  }
  const parsed = paintSearchHint(raw);
  if (!parsed.q) return;
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  if (!theso) return;
  state.committed = parsed.q;
  state.searchTitle = "";
  state.searchKind = "";
  state.resultLimit = SEARCH_PAGE;
  fetch(searchApi("", {
    thesaurusId: theso,
    lang: searchLangParam(),
    q: parsed.q,
    mode: parsed.mode,
    offset: "0",
    limit: String(SEARCH_PAGE)
  }), { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((response) => (response.ok ? response.json() : { hits: [], total: 0, query: parsed.q }))
    .then((data) => applySearchPayload(data, false))
    .catch(() => applySearchPayload({ hits: [], total: 0, query: parsed.q }, false));
}

function loadMoreSearchResults() {
  const loaded = (state.searchHits || []).length;
  if (loaded >= (state.searchTotal || 0)) return;
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  if (!theso) return;
  const programmed = state.searchKind;
  const path = programmed ? "/programmed" : "";
  const params = programmed ? {
    thesaurusId: theso,
    lang: (typeof thesaurusLang === "function" && thesaurusLang()) || "fr",
    kind: programmed,
    offset: String(loaded),
    limit: String(SEARCH_PAGE)
  } : {
    thesaurusId: theso,
    lang: searchLangParam(),
    q: state.committed || "",
    mode: state.searchMode || "FULL_TEXT",
    offset: String(loaded),
    limit: String(SEARCH_PAGE)
  };
  fetch(searchApi(path, params), { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((response) => (response.ok ? response.json() : null))
    .then((data) => { if (data) applySearchPayload(data, true); })
    .catch(() => {});
}

function runProgrammedSearch(kind, title) {
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  if (!theso || !kind) return;
  state.searchKind = kind;
  state.searchTitle = title || kind;
  state.committed = "";
  const input = $("#searchInput");
  fetch(searchApi("/programmed", {
    thesaurusId: theso,
    lang: (typeof thesaurusLang === "function" && thesaurusLang()) || "fr",
    kind: kind,
    offset: "0",
    limit: String(SEARCH_PAGE)
  }), { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((response) => (response.ok ? response.json() : { hits: [], total: 0, mode: "PROGRAMMED" }))
    .then((data) => {
      if (input) state.committed = "";
      applySearchPayload(data, false);
    })
    .catch(() => applySearchPayload({ hits: [], total: 0, mode: "PROGRAMMED" }, false));
}

function bindSearchModes() {
  const note = $("#searchNoteToggle");
  if (note) {
    note.addEventListener("click", () => {
      state.searchNote = !state.searchNote;
      if (state.searchNote) state.searchStartWithQuery = "";
      note.setAttribute("aria-pressed", state.searchNote ? "true" : "false");
      const input = $("#searchInput");
      paintSearchHint(input ? input.value : "");
      if (input && input.value.trim()) filterAc(input.value);
    });
  }
  const allLang = $("#searchAllLang");
  if (allLang) {
    allLang.addEventListener("click", () => {
      state.searchAllLang = !state.searchAllLang;
      allLang.setAttribute("aria-pressed", state.searchAllLang ? "true" : "false");
      const input = $("#searchInput");
      if (input && input.value.trim()) filterAc(input.value);
    });
  }
  $$("[data-search-programmed]").forEach((item) => {
    item.addEventListener("click", () => {
      const label = item.querySelector(".smm-l");
      runProgrammedSearch(item.getAttribute("data-search-programmed"), label ? label.textContent.trim() : "");
    });
  });
  const start = $("#acStartWith");
  if (start) {
    start.addEventListener("click", () => {
      const input = $("#searchInput");
      const q = ((input && input.value) || "").trim();
      if (!q) return;
      state.searchNote = false;
      state.searchStartWithQuery = q;
      if (note) note.setAttribute("aria-pressed", "false");
      filterAc(q);
    });
  }
  const seeAll = $("#acSeeAll");
  if (seeAll) seeAll.addEventListener("click", runSearch);
}

bindSearchModes();

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


var STATUS_FOREST = { insere: 1, candidat: 1, rejete: 1, deprecie: 1 };

function candFilterOk(status, by, on) {
  if (status !== "candidat") return true;
  if (state.candBy && by !== state.candBy) return false;
  if (state.candFrom && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on < state.candFrom)) return false;
  if (state.candTo && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on > state.candTo)) return false;
  return true;
}

function wantsStatusForest() {
  const set = state.statusSet;
  return !set.has("valide") && Object.keys(STATUS_FOREST).some((s) => set.has(s));
}

function statusForestHost() {
  const tree = $("#previewTree") || treePanel();
  if (!tree) return null;
  let box = $("#statusForest");
  if (!box) {
    box = document.createElement("div");
    box.id = "statusForest";
    box.setAttribute("data-status-forest", "1");
    tree.appendChild(box);
  }
  return box;
}

function hideStatusForest() {
  const box = $("#statusForest");
  if (box) {
    box.hidden = true;
    box.innerHTML = "";
  }
}

function statusForestNodeHtml(node) {
  const status = node.status || "valide";
  const type = node.nodeType || "concept";
  const depth = Number(node.depth || 0);
  const inactive = !!node.inactive;
  const open = !!node.hasChildren;
  const pad = 6 + depth * 18;
  const rowCls = "tn-row"
    + (status === "candidat" ? " is-candidate" : "")
    + (status === "rejete" ? " is-rejected" : "")
    + (status === "deprecie" ? " is-deprecated" : "")
    + (inactive ? " is-status-inactive" : "");
  const tags = (status === "candidat" || status === "rejete"
    ? '<span class="tn-dot" title="candidat"></span>' : "")
    + (status === "rejete" ? '<span class="tn-tag">rejeté</span>' : "");
  return '<div class="tn' + (open ? " is-open" : "") + (inactive ? " is-status-inactive" : "") + '"'
    + ' data-id="' + escapeHtml(node.id) + '"'
    + ' data-type="' + escapeHtml(type) + '"'
    + ' data-status="' + escapeHtml(status) + '"'
    + ' data-depth="' + depth + '"'
    + ' data-has-children="' + (node.hasChildren ? "true" : "false") + '"'
    + ' data-nota="' + escapeHtml(node.notation || "") + '"'
    + ' data-cand-by="' + escapeHtml(node.candidateBy || "") + '"'
    + ' data-cand-on="' + escapeHtml(node.candidateOn || "") + '"'
    + ' data-key="' + escapeHtml(node.label || node.id) + '">'
    + '<div class="' + rowCls + '">'
    + (inactive ? '<span class="tn-check is-off"></span>'
      : '<span class="tn-check" data-act="sel-node" data-id="' + escapeHtml(node.id) + '"></span>')
    + '<div class="tn-rowmain" style="padding-left:' + pad + 'px">'
    + '<button type="button" class="tn-caret' + (node.hasChildren ? "" : " is-empty") + '"'
    + ' data-act="sf-toggle" aria-label="Déplier">'
    + '<span class="caret-open">▾</span><span class="caret-shut">▸</span></button>'
    + '<button type="button" class="tn-label" data-act="open" data-id="' + escapeHtml(node.id)
    + '" data-type="' + escapeHtml(type) + '">'
    + '<span class="tn-textwrap"><span class="tn-text">' + escapeHtml(node.label || node.id) + "</span></span>"
    + tags
    + "</button></div></div></div>";
}

function renderStatusForest(nodes) {
  const box = statusForestHost();
  if (!box) return;
  const set = state.statusSet;
  const keep = new Set();
  (nodes || []).forEach((node, i) => {
    if (node.inactive) return;
    if (!set.has(node.status || "valide")) return;
    if (!candFilterOk(node.status, node.candidateBy || "", node.candidateOn || "")) return;
    keep.add(i);
    let depth = Number(node.depth || 0);
    for (let j = i - 1; j >= 0 && depth > 0; j--) {
      const parentDepth = Number(nodes[j].depth || 0);
      if (parentDepth < depth) {
        keep.add(j);
        depth = parentDepth;
      }
    }
  });
  const filtered = (nodes || []).filter((_, i) => keep.has(i));
  if (!filtered.length) {
    box.innerHTML = '<div class="tree-empty">Aucun concept pour les statuts sélectionnés.</div>';
    box.hidden = false;
    return;
  }
  box.innerHTML = filtered.map(statusForestNodeHtml).join("");
  box.hidden = false;
}

function loadStatusForest() {
  const box = statusForestHost();
  if (!box) return;
  const statuses = Object.keys(STATUS_FOREST)
    .filter((s) => state.statusSet.has(s))
    .filter((s) => s !== "candidat" || document.body.getAttribute("data-logged-in") === "1")
    .join(",");
  if (!statuses) {
    box.innerHTML = '<div class="tree-empty">Aucun concept pour les statuts sélectionnés.</div>';
    box.hidden = false;
    return;
  }
  const key = thesaurusId() + "|" + thesaurusLang() + "|" + statuses
    + "|" + (state.candBy || "") + "|" + (state.candFrom || "") + "|" + (state.candTo || "");
  if (loadStatusForest._key === key && box.childElementCount && !box.hidden) return;
  box.hidden = false;
  box.innerHTML = '<div class="tree-empty">Chargement…</div>';
  const ctx = document.body.getAttribute("data-ctx") || "";
  const url = ctx + "/v2/api/tree-status-forest?thesaurusId=" + encodeURIComponent(thesaurusId())
    + "&lang=" + encodeURIComponent(thesaurusLang() || "fr")
    + "&statuses=" + encodeURIComponent(statuses);
  fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((r) => (r.ok ? r.json() : []))
    .then((nodes) => {
      loadStatusForest._key = key;
      renderStatusForest(Array.isArray(nodes) ? nodes : []);
    })
    .catch(() => {
      box.innerHTML = '<div class="tree-empty">Impossible de charger l’arbre filtré.</div>';
    });
}

function applyStatusFilter() {
  const set = state.statusSet;
  function candOk(tn) {
    return candFilterOk(
      tn.getAttribute("data-status") || "",
      tn.getAttribute("data-cand-by") || "",
      tn.getAttribute("data-cand-on") || ""
    );
  }
  const forestOn = wantsStatusForest();
  const nodes = treeNodes().filter((tn) => !tn.closest("[data-status-forest]"));
  if (forestOn) {
    nodes.forEach((tn) => tn.classList.add("is-status-off"));
    const empty = ($("#previewTree") || treePanel() || document).querySelector(".tree-status-empty");
    if (empty) empty.hidden = true;
    loadStatusForest();
  } else {
    hideStatusForest();
    loadStatusForest._key = "";
    const ownOk = nodes.map((tn) => {
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
    const host = $("#previewTree") || treePanel();
    if (host) {
      let empty = host.querySelector(".tree-status-empty");
      const anyOn = vis.some(Boolean);
      if (!anyOn && nodes.length) {
        if (!empty) {
          empty = document.createElement("div");
          empty.className = "tree-empty tree-status-empty";
          empty.textContent = "Aucun concept pour les statuts sélectionnés.";
          host.appendChild(empty);
        }
        empty.hidden = false;
      } else if (empty) {
        empty.hidden = true;
      }
    }
  }
  if (tableRowsCache.length) {
    state.tblPage = 1;
    renderTablePage();
    return;
  }
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
  updateTableCount();
}
