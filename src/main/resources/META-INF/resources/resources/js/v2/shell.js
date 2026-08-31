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
  live.classList.remove("is-ready", "is-opening");
  showPanel(".view-panel", "viewLive");
  const back = $("#liveBackList");
  if (back && fromCandList()) back.hidden = false;
  paintListBack();
  const view = $("#previewView");
  if (view) view.scrollTop = 0;
  live.scrollTop = 0;
}

var FACET_AC_PAGE = 7;
var labelFacetPicker = { items: null, key: "", selected: [], active: -1, showAll: false };

function labelFacetPickerKey() {
  return thesaurusId() + "|" + thesaurusLang();
}

function labelFacetHidden() {
  return document.querySelector("[id$='labelFacetJson']");
}

function loadLabelFacetItems() {
  const key = labelFacetPickerKey();
  if (labelFacetPicker.items && labelFacetPicker.key === key) {
    return Promise.resolve(labelFacetPicker.items);
  }
  if (labelFacetPicker._load && labelFacetPicker.key === key) {
    return labelFacetPicker._load;
  }
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang()
  });
  labelFacetPicker.key = key;
  labelFacetPicker._load = fetch(ctx + "/v2/api/facets?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    labelFacetPicker.items = Array.isArray(items) ? items : [];
    labelFacetPicker._load = null;
    return labelFacetPicker.items;
  }).catch(() => {
    labelFacetPicker._load = null;
    labelFacetPicker.items = [];
    return labelFacetPicker.items;
  });
  return labelFacetPicker._load;
}

function scoreLabelFacet(item, q) {
  if (!q) return { score: 0 };
  const pref = norm(item.label || "");
  if (pref === q) return { score: 0 };
  if (pref.startsWith(q)) return { score: 1 };
  if (pref.includes(q)) return { score: 2 };
  const id = norm(item.id || "");
  if (id.includes(q)) return { score: 4, via: "id" };
  return { score: -1 };
}

function rankLabelFacetHits(query) {
  const q = norm((query || "").trim());
  return (labelFacetPicker.items || []).map((item) => {
    const sc = scoreLabelFacet(item, q);
    if (sc.score < 0) return null;
    return { item: item, score: sc.score, via: sc.via };
  }).filter(Boolean).sort((a, b) => a.score - b.score || (a.item.label || "").localeCompare(b.item.label || "", "fr"));
}

function shownLabelFacetRows() {
  return $$("#labelFacetMenu .ac-row.is-shown");
}

function isLabelFacetSelected(id) {
  return labelFacetPicker.selected.some((item) => item.id === id);
}

function syncLabelFacetHidden() {
  const hidden = labelFacetHidden();
  if (hidden) hidden.value = JSON.stringify(labelFacetPicker.selected.map((item) => ({
    id: item.id,
    label: item.label || item.id
  })));
  const chips = $("#labelFacetChips");
  if (!chips) return;
  chips.hidden = labelFacetPicker.selected.length === 0;
  chips.innerHTML = labelFacetPicker.selected.map((item) => {
    return '<button type="button" class="coll-chip" data-act="facet-chip-remove" data-id="'
      + escapeHtml(item.id) + '" title="Retirer">'
      + escapeHtml(item.label)
      + '<span aria-hidden="true">×</span></button>';
  }).join("");
}

function syncLabelFacetClear() {
  const input = $("#labelFacetQuery");
  const clear = $("#labelFacetClear");
  if (clear) clear.hidden = !(input && input.value);
}

function toggleLabelFacetPick(id, label) {
  if (!id) return;
  if (isLabelFacetSelected(id)) {
    labelFacetPicker.selected = labelFacetPicker.selected.filter((item) => item.id !== id);
  } else {
    labelFacetPicker.selected.push({ id: id, label: label || id });
  }
  syncLabelFacetHidden();
  renderLabelFacetMenu($("#labelFacetQuery") ? $("#labelFacetQuery").value : "");
}

function seedLabelFacetSelection() {
  const hidden = labelFacetHidden();
  labelFacetPicker.selected = [];
  if (!hidden || !hidden.value) return;
  try {
    const rows = JSON.parse(hidden.value);
    if (!Array.isArray(rows)) return;
    const seen = {};
    rows.forEach((row) => {
      const id = row && row.id != null ? String(row.id) : "";
      if (!id || seen[id]) return;
      seen[id] = true;
      labelFacetPicker.selected.push({ id: id, label: row.label || id });
    });
  } catch (e) {
    labelFacetPicker.selected = [];
  }
}

function bindLabelFacetPicker() {
  if (!$("#labelFacetPick")) {
    closeLabelFacetPicker();
    return;
  }
  seedLabelFacetSelection();
  syncLabelFacetClear();
  syncLabelFacetHidden();
  loadLabelFacetItems();
}

function highlightLabelFacetQuery(query) {
  $$("#labelFacetMenu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#labelFacetMenu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function labelFacetItemHtml(hit) {
  const item = hit.item;
  const on = isLabelFacetSelected(item.id);
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="facet-pick-item" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label) + '">'
    + '<span class="ac-ico">▦</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function setLabelFacetAcIdx(index) {
  const rows = shownLabelFacetRows();
  labelFacetPicker.active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function renderLabelFacetMenu(query) {
  const menu = $("#labelFacetMenu");
  const list = $("#labelFacetList");
  if (!menu || !list) return;
  const raw = (query || "").trim();
  const hits = rankLabelFacetHits(raw);
  const limit = labelFacetPicker.showAll ? hits.length : FACET_AC_PAGE;
  list.innerHTML = hits.map((hit) => labelFacetItemHtml(hit)).join("");
  const rows = $$("#labelFacetList .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", !!raw && hits.length === 0);
  const qEl = $("#labelFacetEmptyQuery");
  if (qEl) qEl.textContent = raw;
  const count = $("#labelFacetCount");
  if (count) count.textContent = String(hits.length);
  const more = $("#labelFacetMore");
  if (more) more.hidden = labelFacetPicker.showAll || hits.length <= FACET_AC_PAGE;
  labelFacetPicker.active = -1;
  highlightLabelFacetQuery(raw);
  syncLabelFacetCombo();
}

function syncLabelFacetCombo() {
  const menu = $("#labelFacetMenu");
  const input = $("#labelFacetQuery");
  const combo = $("#labelFacetCombo");
  const pick = $("#labelFacetPick");
  const open = !!(menu && !menu.hidden);
  if (pick) pick.classList.toggle("is-open", open);
  if (combo) combo.classList.toggle("is-open", open);
  if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
}

function openLabelFacetMenu(showAll) {
  const menu = $("#labelFacetMenu");
  if (!menu) return;
  if (showAll) labelFacetPicker.showAll = true;
  menu.hidden = false;
  syncLabelFacetCombo();
  loadLabelFacetItems().then(() => renderLabelFacetMenu($("#labelFacetQuery") ? $("#labelFacetQuery").value : ""));
}

function toggleLabelFacetMenu() {
  const menu = $("#labelFacetMenu");
  if (menu && !menu.hidden) {
    closeLabelFacetPicker();
    return;
  }
  const input = $("#labelFacetQuery");
  if (input) input.focus();
  openLabelFacetMenu(true);
}

function closeLabelFacetPicker() {
  const menu = $("#labelFacetMenu");
  const combo = $("#labelFacetCombo");
  if (combo) combo.classList.remove("is-focused");
  if (!menu || menu.hidden) return false;
  menu.hidden = true;
  labelFacetPicker.showAll = false;
  labelFacetPicker.active = -1;
  syncLabelFacetCombo();
  return true;
}

function pickActiveLabelFacet() {
  const rows = shownLabelFacetRows();
  const row = labelFacetPicker.active >= 0 ? rows[labelFacetPicker.active] : rows[0];
  if (!row) return false;
  toggleLabelFacetPick(row.getAttribute("data-id"), row.getAttribute("data-label"));
  const input = $("#labelFacetQuery");
  if (input) {
    input.value = "";
    syncLabelFacetClear();
    input.focus();
  }
  labelFacetPicker.showAll = false;
  renderLabelFacetMenu("");
  return true;
}

var CV_COLL_AC_PAGE = 7;
var cvCollPicker = { items: null, key: "", selected: [], active: -1, showAll: false };

function cvCollPickerKey() {
  return thesaurusId() + "|" + thesaurusLang() + "|" + (state && state.colSort === "nota" ? "nota" : "alpha");
}

function cvCollHidden() {
  return document.querySelector("[id$='cvCollJson']");
}

function loadCvCollItems() {
  const key = cvCollPickerKey();
  if (cvCollPicker.items && cvCollPicker.key === key) {
    return Promise.resolve(cvCollPicker.items);
  }
  if (cvCollPicker._load && cvCollPicker.key === key) {
    return cvCollPicker._load;
  }
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    sortByNotation: state && state.colSort === "nota" ? "true" : "false"
  });
  cvCollPicker.key = key;
  cvCollPicker._load = fetch(ctx + "/v2/api/collection-tree/list?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    cvCollPicker.items = Array.isArray(items) ? items : [];
    cvCollPicker._load = null;
    return cvCollPicker.items;
  }).catch(() => {
    cvCollPicker._load = null;
    cvCollPicker.items = [];
    return cvCollPicker.items;
  });
  return cvCollPicker._load;
}

function scoreCvColl(item, q) {
  if (!q) return { score: 0 };
  const pref = norm(item.label || "");
  if (pref === q) return { score: 0 };
  if (pref.startsWith(q)) return { score: 1 };
  if (pref.includes(q)) return { score: 2 };
  const nota = norm(item.notation || "");
  if (nota && nota.includes(q)) return { score: 3, via: "nota" };
  const id = norm(item.id || "");
  if (id.includes(q)) return { score: 4, via: "id" };
  return { score: -1 };
}

function rankCvCollHits(query) {
  const q = norm((query || "").trim());
  return (cvCollPicker.items || []).map((item) => {
    const sc = scoreCvColl(item, q);
    if (sc.score < 0) return null;
    return { item: item, score: sc.score, via: sc.via };
  }).filter(Boolean).sort((a, b) => a.score - b.score || (a.item.label || "").localeCompare(b.item.label || "", "fr"));
}

function shownCvCollRows() {
  return $$("#cvCollMenu .ac-row.is-shown");
}

function isCvCollSelected(id) {
  return cvCollPicker.selected.some((item) => item.id === id);
}

function syncCvCollHidden() {
  const hidden = cvCollHidden();
  if (hidden) hidden.value = JSON.stringify(cvCollPicker.selected.map((item) => ({
    id: item.id,
    label: item.label || item.id
  })));
  const chips = $("#cvCollChips");
  if (!chips) return;
  chips.hidden = cvCollPicker.selected.length === 0;
  chips.innerHTML = cvCollPicker.selected.map((item) => {
    return '<button type="button" class="coll-chip" data-act="cv-coll-chip-remove" data-id="'
      + escapeHtml(item.id) + '" title="Retirer">'
      + escapeHtml(item.label)
      + '<span aria-hidden="true">×</span></button>';
  }).join("");
}

function syncCvCollClear() {
  const input = $("#cvCollQuery");
  const clear = $("#cvCollClear");
  if (clear) clear.hidden = !(input && input.value);
}

function toggleCvCollPick(id, label) {
  if (!id) return;
  if (isCvCollSelected(id)) {
    cvCollPicker.selected = cvCollPicker.selected.filter((item) => item.id !== id);
  } else {
    cvCollPicker.selected.push({ id: id, label: label || id });
  }
  syncCvCollHidden();
  renderCvCollMenu($("#cvCollQuery") ? $("#cvCollQuery").value : "");
}

function seedCvCollSelection() {
  const hidden = cvCollHidden();
  cvCollPicker.selected = [];
  if (!hidden || !hidden.value) return;
  try {
    const rows = JSON.parse(hidden.value);
    if (!Array.isArray(rows)) return;
    const seen = {};
    rows.forEach((row) => {
      const id = row && row.id != null ? String(row.id) : "";
      if (!id || seen[id]) return;
      seen[id] = true;
      cvCollPicker.selected.push({ id: id, label: row.label || id });
    });
  } catch (e) {
    cvCollPicker.selected = [];
  }
}

function bindCvCollPicker() {
  if (!$("#cvCollPick")) {
    closeCvCollPicker();
    return;
  }
  seedCvCollSelection();
  syncCvCollClear();
  syncCvCollHidden();
  loadCvCollItems();
}

function highlightCvCollQuery(query) {
  $$("#cvCollMenu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#cvCollMenu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function cvCollItemHtml(hit) {
  const item = hit.item;
  const on = isCvCollSelected(item.id);
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + (hit.via === "nota" ? " match-nota" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="cv-coll-pick-item" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label) + '">'
    + '<span class="ac-ico">▦</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function setCvCollAcIdx(index) {
  const rows = shownCvCollRows();
  cvCollPicker.active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function renderCvCollMenu(query) {
  const menu = $("#cvCollMenu");
  const list = $("#cvCollList");
  if (!menu || !list) return;
  const raw = (query || "").trim();
  const hits = rankCvCollHits(raw);
  const limit = cvCollPicker.showAll ? hits.length : CV_COLL_AC_PAGE;
  list.innerHTML = hits.map((hit) => cvCollItemHtml(hit)).join("");
  const rows = $$("#cvCollList .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", !!raw && hits.length === 0);
  const qEl = $("#cvCollEmptyQuery");
  if (qEl) qEl.textContent = raw;
  const count = $("#cvCollCount");
  if (count) count.textContent = String(hits.length);
  const more = $("#cvCollMore");
  if (more) more.hidden = cvCollPicker.showAll || hits.length <= CV_COLL_AC_PAGE;
  cvCollPicker.active = -1;
  highlightCvCollQuery(raw);
  syncCvCollCombo();
}

function syncCvCollCombo() {
  const menu = $("#cvCollMenu");
  const input = $("#cvCollQuery");
  const combo = $("#cvCollCombo");
  const pick = $("#cvCollPick");
  const open = !!(menu && !menu.hidden);
  if (pick) pick.classList.toggle("is-open", open);
  if (combo) combo.classList.toggle("is-open", open);
  if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
}

function openCvCollMenu(showAll) {
  const menu = $("#cvCollMenu");
  if (!menu) return;
  if (showAll) cvCollPicker.showAll = true;
  menu.hidden = false;
  syncCvCollCombo();
  loadCvCollItems().then(() => renderCvCollMenu($("#cvCollQuery") ? $("#cvCollQuery").value : ""));
}

function toggleCvCollMenu() {
  const menu = $("#cvCollMenu");
  if (menu && !menu.hidden) {
    closeCvCollPicker();
    return;
  }
  const input = $("#cvCollQuery");
  if (input) input.focus();
  openCvCollMenu(true);
}

function closeCvCollPicker() {
  const menu = $("#cvCollMenu");
  const combo = $("#cvCollCombo");
  if (combo) combo.classList.remove("is-focused");
  if (!menu || menu.hidden) return false;
  menu.hidden = true;
  cvCollPicker.showAll = false;
  cvCollPicker.active = -1;
  syncCvCollCombo();
  return true;
}

function pickActiveCvColl() {
  const rows = shownCvCollRows();
  const row = cvCollPicker.active >= 0 ? rows[cvCollPicker.active] : rows[0];
  if (!row) return false;
  toggleCvCollPick(row.getAttribute("data-id"), row.getAttribute("data-label"));
  const input = $("#cvCollQuery");
  if (input) {
    input.value = "";
    syncCvCollClear();
    input.focus();
  }
  cvCollPicker.showAll = false;
  renderCvCollMenu("");
  return true;
}

var REL_KINDS = ["bt", "nt", "rt"];
var REL_CAP = { bt: "Bt", nt: "Nt", rt: "Rt" };
var REL_AC_PAGE = 5;
var REL_DEBOUNCE = 200;
var relPickers = {
  bt: emptyRelState(),
  nt: emptyRelState(),
  rt: emptyRelState()
};

function emptyRelState() {
  return { selected: [], hits: [], query: "", active: -1, showAll: false, timer: null, seq: 0 };
}

function relEls(kind) {
  const cap = REL_CAP[kind];
  return {
    pick: $("#cvRel" + cap + "Pick"),
    chips: $("#cvRel" + cap + "Chips"),
    combo: $("#cvRel" + cap + "Combo"),
    query: $("#cvRel" + cap + "Query"),
    clear: $("#cvRel" + cap + "Clear"),
    menu: $("#cvRel" + cap + "Menu"),
    list: $("#cvRel" + cap + "List"),
    more: $("#cvRel" + cap + "More"),
    count: $("#cvRel" + cap + "Count"),
    empty: $("#cvRel" + cap + "Empty")
  };
}

function relHidden(kind) {
  return document.querySelector("[id$='cvRel" + REL_CAP[kind] + "Json']");
}

function relKindFromQueryId(id) {
  if (id === "cvRelBtQuery") return "bt";
  if (id === "cvRelNtQuery") return "nt";
  if (id === "cvRelRtQuery") return "rt";
  return "";
}

function currentConceptId() {
  const cv = document.querySelector("#viewLive .cv.is-on");
  return (cv && cv.getAttribute("data-id")) || "";
}

function otherRelSelectedIds(kind) {
  const ids = new Set();
  const selfId = (currentConceptId() || "").toLowerCase();
  if (selfId) ids.add(selfId);
  REL_KINDS.forEach((other) => {
    if (other === kind) return;
    (relPickers[other].selected || []).forEach((item) => {
      if (item.id) ids.add(String(item.id).toLowerCase());
    });
  });
  return ids;
}

function isRelSelected(kind, id) {
  const key = String(id || "").toLowerCase();
  return relPickers[kind].selected.some((item) => String(item.id).toLowerCase() === key);
}

function syncRelHidden(kind) {
  const hidden = relHidden(kind);
  if (hidden) hidden.value = JSON.stringify(relPickers[kind].selected.map((item) => ({
    id: item.id,
    label: item.label || item.id
  })));
  const chips = relEls(kind).chips;
  if (!chips) return;
  chips.hidden = relPickers[kind].selected.length === 0;
  chips.innerHTML = relPickers[kind].selected.map((item) => {
    return '<button type="button" class="coll-chip" data-act="rel-chip-remove" data-kind="'
      + kind + '" data-id="' + escapeHtml(item.id) + '" title="Retirer">'
      + escapeHtml(item.label)
      + '<span aria-hidden="true">×</span></button>';
  }).join("");
}

function syncRelClear(kind) {
  const els = relEls(kind);
  if (els.clear) els.clear.hidden = !(els.query && els.query.value);
}

function seedRelSelection(kind) {
  const hidden = relHidden(kind);
  relPickers[kind].selected = [];
  if (!hidden || !hidden.value) {
    syncRelHidden(kind);
    return;
  }
  try {
    const rows = JSON.parse(hidden.value);
    if (!Array.isArray(rows)) return;
    const seen = new Set();
    rows.forEach((row) => {
      const id = row && row.id ? String(row.id) : "";
      const key = id.toLowerCase();
      if (!id || seen.has(key)) return;
      seen.add(key);
      relPickers[kind].selected.push({ id: id, label: row.label || id });
    });
  } catch (ex) {
    relPickers[kind].selected = [];
  }
  syncRelHidden(kind);
}

function bindRelPickers() {
  if (!$("#cvRelBtPick") && !$("#cvRelNtPick") && !$("#cvRelRtPick")) {
    closeAllRelPickers();
    return;
  }
  REL_KINDS.forEach((kind) => {
    if (relPickers[kind].timer) {
      clearTimeout(relPickers[kind].timer);
      relPickers[kind].timer = null;
    }
    relPickers[kind].hits = [];
    relPickers[kind].query = "";
    relPickers[kind].active = -1;
    relPickers[kind].showAll = false;
    seedRelSelection(kind);
    syncRelClear(kind);
  });
}

function shownRelRows(kind) {
  const cap = REL_CAP[kind];
  return $$("#cvRel" + cap + "Menu .ac-row.is-shown");
}

function relItemHtml(kind, item) {
  const on = isRelSelected(kind, item.id);
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="rel-pick-item" data-kind="' + kind
    + '" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label) + '">'
    + '<span class="ac-ico">◇</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function highlightRelQuery(kind, query) {
  const cap = REL_CAP[kind];
  $$("#cvRel" + cap + "Menu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#cvRel" + cap + "Menu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function setRelAcIdx(kind, index) {
  const rows = shownRelRows(kind);
  relPickers[kind].active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function syncRelCombo(kind) {
  const els = relEls(kind);
  const open = !!(els.menu && !els.menu.hidden);
  if (els.pick) els.pick.classList.toggle("is-open", open);
  if (els.combo) els.combo.classList.toggle("is-open", open);
  if (els.query) els.query.setAttribute("aria-expanded", open ? "true" : "false");
}

function renderRelMenu(kind) {
  const els = relEls(kind);
  if (!els.menu || !els.list) return;
  const st = relPickers[kind];
  const raw = st.query || "";
  const hits = st.hits || [];
  const limit = st.showAll ? hits.length : REL_AC_PAGE;
  els.list.innerHTML = hits.map((item) => relItemHtml(kind, item)).join("");
  const rows = $$("#cvRel" + REL_CAP[kind] + "List .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  els.menu.classList.toggle("has-hits", hits.length > 0);
  els.menu.classList.toggle("is-empty", hits.length === 0);
  if (els.empty) {
    els.empty.textContent = raw
      ? ("Aucun concept pour « " + raw + " »")
      : "Tapez pour rechercher un concept…";
  }
  if (els.count) els.count.textContent = String(hits.length);
  if (els.more) els.more.hidden = st.showAll || hits.length <= REL_AC_PAGE;
  st.active = -1;
  highlightRelQuery(kind, raw);
  syncRelCombo(kind);
}

function closeRelPicker(kind) {
  const els = relEls(kind);
  if (els.combo) els.combo.classList.remove("is-focused");
  if (!els.menu || els.menu.hidden) return false;
  els.menu.hidden = true;
  relPickers[kind].showAll = false;
  relPickers[kind].active = -1;
  syncRelCombo(kind);
  return true;
}

function closeAllRelPickers(except) {
  REL_KINDS.forEach((kind) => {
    if (kind === except) return;
    closeRelPicker(kind);
  });
}

function openRelMenu(kind) {
  const els = relEls(kind);
  if (!els.menu) return;
  closeAllRelPickers(kind);
  closeCrelPicker();
  els.menu.hidden = false;
  syncRelCombo(kind);
  renderRelMenu(kind);
}

function toggleRelMenu(kind) {
  if (!kind || !relPickers[kind]) return;
  const els = relEls(kind);
  if (els.menu && !els.menu.hidden) {
    closeRelPicker(kind);
    return;
  }
  if (els.query) els.query.focus();
  const q = els.query ? els.query.value.trim() : "";
  if (q) scheduleRelSearch(kind);
  else searchRel(kind, "%");
}

function toggleRelPick(kind, id, label) {
  if (!kind || !id) return;
  const removing = isRelSelected(kind, id);
  if (removing) {
    const key = String(id).toLowerCase();
    relPickers[kind].selected = relPickers[kind].selected.filter(
      (item) => String(item.id).toLowerCase() !== key
    );
  } else {
    if (otherRelSelectedIds(kind).has(String(id).toLowerCase())) return;
    relPickers[kind].selected.push({ id: id, label: label || id });
  }
  syncRelHidden(kind);
  const els = relEls(kind);
  if (!removing && els.query) {
    els.query.value = "";
    syncRelClear(kind);
    relPickers[kind].hits = [];
    relPickers[kind].query = "";
    closeRelPicker(kind);
    els.query.focus();
    return;
  }
  if (els.menu && !els.menu.hidden) renderRelMenu(kind);
}

function searchRel(kind, q) {
  const st = relPickers[kind];
  const seq = ++st.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    q: q,
    excludeId: currentConceptId()
  });
  fetch(ctx + "/v2/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== st.seq) return;
    const exclude = otherRelSelectedIds(kind);
    const els = relEls(kind);
    const typed = els.query ? els.query.value.trim() : "";
    st.query = typed || (q === "%" ? "" : q);
    st.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id).toLowerCase() : "";
      return id && !exclude.has(id);
    });
    openRelMenu(kind);
  }).catch(() => {
    if (seq !== st.seq) return;
    const els = relEls(kind);
    const typed = els.query ? els.query.value.trim() : "";
    st.query = typed || (q === "%" ? "" : q);
    st.hits = [];
    openRelMenu(kind);
  });
}

function scheduleRelSearch(kind) {
  if (!kind || !relPickers[kind]) return;
  const st = relPickers[kind];
  const els = relEls(kind);
  const q = els.query ? els.query.value.trim() : "";
  st.showAll = false;
  syncRelClear(kind);
  if (st.timer) clearTimeout(st.timer);
  if (!q) {
    st.hits = [];
    st.query = "";
    st.seq += 1;
    closeRelPicker(kind);
    return;
  }
  st.timer = setTimeout(() => searchRel(kind, q), REL_DEBOUNCE);
}

function pickActiveRel(kind) {
  const rows = shownRelRows(kind);
  const row = relPickers[kind].active >= 0 ? rows[relPickers[kind].active] : rows[0];
  if (!row) return false;
  toggleRelPick(kind, row.getAttribute("data-id"), row.getAttribute("data-label"));
  return true;
}

var CREL_AC_PAGE = 7;
var crelPicker = { selected: [], hits: [], query: "", active: -1, showAll: false, timer: null, seq: 0 };

function crelHidden() {
  return document.querySelector("[id$='cvCrelJson']");
}

function isCrelSelected(id) {
  const key = String(id || "").toLowerCase();
  return crelPicker.selected.some((item) => String(item.id).toLowerCase() === key);
}

function syncCrelHidden() {
  const hidden = crelHidden();
  if (hidden) hidden.value = JSON.stringify(crelPicker.selected.map((item) => ({
    id: item.id,
    label: item.label || item.id,
    role: item.role || "",
    roleLabel: item.roleLabel || item.role || "",
    reciprocal: !!item.reciprocal
  })));
  const chips = $("#cvCrelChips");
  if (!chips) return;
  chips.hidden = crelPicker.selected.length === 0;
  chips.innerHTML = crelPicker.selected.map((item) => {
    const role = item.roleLabel || item.role || "";
    const text = role ? (role + " · " + (item.label || item.id)) : (item.label || item.id);
    return '<button type="button" class="coll-chip" data-act="crel-chip-remove" data-id="'
      + escapeHtml(item.id) + '" title="Retirer">'
      + escapeHtml(text)
      + '<span aria-hidden="true">×</span></button>';
  }).join("");
}

function syncCrelClear() {
  const input = $("#cvCrelQuery");
  const clear = $("#cvCrelClear");
  if (clear) clear.hidden = !(input && input.value);
}

function seedCrelSelection() {
  const hidden = crelHidden();
  crelPicker.selected = [];
  if (!hidden || !hidden.value) {
    syncCrelHidden();
    return;
  }
  try {
    const rows = JSON.parse(hidden.value);
    if (!Array.isArray(rows)) return;
    const seen = new Set();
    rows.forEach((row) => {
      const id = row && row.id ? String(row.id) : "";
      const key = id.toLowerCase();
      if (!id || seen.has(key)) return;
      seen.add(key);
      crelPicker.selected.push({
        id: id,
        label: row.label || id,
        role: row.role || "",
        roleLabel: row.roleLabel || row.role || "",
        reciprocal: !!row.reciprocal
      });
    });
  } catch (ex) {
    crelPicker.selected = [];
  }
  syncCrelHidden();
}

function bindCrelPicker() {
  if (!$("#cvCrelPick")) {
    closeCrelPicker();
    return;
  }
  if (crelPicker.timer) {
    clearTimeout(crelPicker.timer);
    crelPicker.timer = null;
  }
  crelPicker.hits = [];
  crelPicker.query = "";
  crelPicker.active = -1;
  crelPicker.showAll = false;
  seedCrelSelection();
  syncCrelClear();
}

function shownCrelRows() {
  return $$("#cvCrelMenu .ac-row.is-shown");
}

function crelItemHtml(item) {
  const on = isCrelSelected(item.id);
  const type = item.type ? '<span class="ac-type">' + escapeHtml(item.type) + "</span>" : "";
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="crel-pick-item" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label)
    + '" data-type="' + escapeHtml(item.type || "") + '">'
    + '<span class="ac-ico">◇</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + (type ? " " + type : "") + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function highlightCrelQuery(query) {
  $$("#cvCrelMenu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#cvCrelMenu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function setCrelAcIdx(index) {
  const rows = shownCrelRows();
  crelPicker.active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function syncCrelCombo() {
  const menu = $("#cvCrelMenu");
  const input = $("#cvCrelQuery");
  const combo = $("#cvCrelCombo");
  const pick = $("#cvCrelPick");
  const open = !!(menu && !menu.hidden);
  if (pick) pick.classList.toggle("is-open", open);
  if (combo) combo.classList.toggle("is-open", open);
  if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
}

function renderCrelMenu() {
  const menu = $("#cvCrelMenu");
  const list = $("#cvCrelList");
  if (!menu || !list) return;
  const raw = crelPicker.query || "";
  const hits = crelPicker.hits || [];
  const limit = crelPicker.showAll ? hits.length : CREL_AC_PAGE;
  list.innerHTML = hits.map((item) => crelItemHtml(item)).join("");
  const rows = $$("#cvCrelList .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", hits.length === 0);
  const empty = $("#cvCrelEmpty");
  if (empty) {
    empty.textContent = raw
      ? ("Aucun concept pour « " + raw + " »")
      : "Tapez pour rechercher un concept…";
  }
  const count = $("#cvCrelCount");
  if (count) count.textContent = String(hits.length);
  const more = $("#cvCrelMore");
  if (more) more.hidden = crelPicker.showAll || hits.length <= CREL_AC_PAGE;
  crelPicker.active = -1;
  highlightCrelQuery(raw);
  syncCrelCombo();
}

function closeCrelPicker() {
  const menu = $("#cvCrelMenu");
  const combo = $("#cvCrelCombo");
  if (combo) combo.classList.remove("is-focused");
  if (!menu || menu.hidden) return false;
  menu.hidden = true;
  crelPicker.showAll = false;
  crelPicker.active = -1;
  syncCrelCombo();
  return true;
}

function openCrelMenu() {
  const menu = $("#cvCrelMenu");
  if (!menu) return;
  closeAllRelPickers();
  menu.hidden = false;
  syncCrelCombo();
  renderCrelMenu();
}

function toggleCrelMenu() {
  const menu = $("#cvCrelMenu");
  if (menu && !menu.hidden) {
    closeCrelPicker();
    return;
  }
  const input = $("#cvCrelQuery");
  if (input) input.focus();
  const q = input ? input.value.trim() : "";
  if (q) scheduleCrelSearch();
  else {
    crelPicker.hits = [];
    crelPicker.query = "";
    openCrelMenu();
  }
}

function toggleCrelPick(id, label, type) {
  if (!id) return;
  const removing = isCrelSelected(id);
  if (removing) {
    const key = String(id).toLowerCase();
    crelPicker.selected = crelPicker.selected.filter((item) => String(item.id).toLowerCase() !== key);
  } else {
    const selfId = (currentConceptId() || "").toLowerCase();
    if (selfId && String(id).toLowerCase() === selfId) return;
    crelPicker.selected.push({
      id: id,
      label: label || id,
      role: type || "",
      roleLabel: type || "",
      reciprocal: false
    });
  }
  syncCrelHidden();
  const input = $("#cvCrelQuery");
  if (!removing && input) {
    input.value = "";
    syncCrelClear();
    crelPicker.hits = [];
    crelPicker.query = "";
    closeCrelPicker();
    input.focus();
    return;
  }
  const menu = $("#cvCrelMenu");
  if (menu && !menu.hidden) renderCrelMenu();
}

function searchCrel(q) {
  const seq = ++crelPicker.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    q: q,
    excludeId: currentConceptId(),
    customOnly: "true"
  });
  fetch(ctx + "/v2/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== crelPicker.seq) return;
    const selfId = (currentConceptId() || "").toLowerCase();
    crelPicker.query = q;
    crelPicker.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id).toLowerCase() : "";
      return id && id !== selfId;
    });
    openCrelMenu();
  }).catch(() => {
    if (seq !== crelPicker.seq) return;
    crelPicker.query = q;
    crelPicker.hits = [];
    openCrelMenu();
  });
}

function scheduleCrelSearch() {
  const input = $("#cvCrelQuery");
  const q = input ? input.value.trim() : "";
  crelPicker.showAll = false;
  syncCrelClear();
  if (crelPicker.timer) clearTimeout(crelPicker.timer);
  if (!q) {
    crelPicker.hits = [];
    crelPicker.query = "";
    crelPicker.seq += 1;
    closeCrelPicker();
    return;
  }
  crelPicker.timer = setTimeout(() => searchCrel(q), REL_DEBOUNCE);
}

function pickActiveCrel() {
  const rows = shownCrelRows();
  const row = crelPicker.active >= 0 ? rows[crelPicker.active] : rows[0];
  if (!row) return false;
  toggleCrelPick(row.getAttribute("data-id"), row.getAttribute("data-label"), row.getAttribute("data-type"));
  return true;
}

const imgGallery = { index: 0, items: [], lightbox: false, conceptId: "" };

function imgGalleryCaption(item) {
  if (!item) return "";
  return [item.name, item.copyright, item.creator]
      .map((part) => (part || "").trim())
      .filter(Boolean)
      .join(" · ");
}

function closeImgLightbox(opts) {
  const open = imgGallery.lightbox;
  imgGallery.lightbox = false;
  document.body.classList.remove("is-img-lightbox");
  const box = $("#cvImgLightbox");
  if (box) {
    box.hidden = true;
    box.setAttribute("aria-hidden", "true");
  }
  if (open && !(opts && opts.restoreFocus === false)) {
    const opener = $("#cvImgGalleryOpen");
    if (opener) opener.focus();
  }
  return open;
}

function showImgAt(index, alsoLightbox) {
  const items = imgGallery.items;
  if (!items.length) return;
  const n = items.length;
  imgGallery.index = ((index % n) + n) % n;
  const item = items[imgGallery.index];
  const main = $("#cvImgGalleryMain");
  if (main) {
    main.src = item.src || "";
    main.alt = item.name || "Image du concept";
  }
  const cap = $("#cvImgGalleryCap");
  if (cap) cap.textContent = imgGalleryCaption(item);
  const count = $("#cvImgGalleryCount");
  if (count) {
    count.hidden = n < 2;
    count.textContent = (imgGallery.index + 1) + " / " + n;
  }
  $$("#cvImgGalleryThumbs [data-act='img-gallery-thumb']").forEach((btn, i) => {
    btn.classList.toggle("is-on", i === imgGallery.index);
  });
  const root = $("#cvImgGallery");
  if (root) root.classList.toggle("is-single", n < 2);
  if (alsoLightbox || imgGallery.lightbox) {
    const light = $("#cvImgLightboxImg");
    if (light) {
      light.src = item.src || "";
      light.alt = item.name || "Image du concept";
    }
    const lightCap = $("#cvImgLightboxCap");
    if (lightCap) lightCap.textContent = imgGalleryCaption(item);
    const orig = $("#cvImgLightboxOrig");
    if (orig) orig.href = item.src || "#";
    const box = $("#cvImgLightbox");
    if (box) box.classList.toggle("is-single", n < 2);
  }
}

function openImgLightbox() {
  if (!imgGallery.items.length) return;
  closeGpsLightbox({ restoreFocus: false });
  imgGallery.lightbox = true;
  document.body.classList.add("is-img-lightbox");
  const box = $("#cvImgLightbox");
  if (box) {
    box.hidden = false;
    box.setAttribute("aria-hidden", "false");
  }
  showImgAt(imgGallery.index, true);
  const closeBtn = box && box.querySelector("[data-act='img-lightbox-close']");
  if (closeBtn) closeBtn.focus();
}

function bindImgGallery() {
  const cv = document.querySelector("#viewLive .cv.is-on");
  const conceptId = cv ? (cv.getAttribute("data-id") || "") : "";
  if (conceptId !== imgGallery.conceptId) {
    closeImgLightbox({ restoreFocus: false });
    imgGallery.index = 0;
    imgGallery.conceptId = conceptId;
  }
  const root = $("#cvImgGallery");
  if (!root) {
    closeImgLightbox({ restoreFocus: false });
    imgGallery.items = [];
    imgGallery.index = 0;
    return;
  }
  const items = $$("#cvImgGalleryThumbs [data-act='img-gallery-thumb']").map((btn) => ({
    src: btn.getAttribute("data-src") || "",
    name: btn.getAttribute("data-name") || "",
    copyright: btn.getAttribute("data-copy") || "",
    creator: btn.getAttribute("data-creator") || ""
  }));
  const same = items.length === imgGallery.items.length
      && items.every((item, i) => item.src === imgGallery.items[i].src);
  imgGallery.items = items;
  if (!same) imgGallery.index = 0;
  root.classList.toggle("is-single", items.length < 2);
  if (imgGallery.lightbox && items.length) {
    const box = $("#cvImgLightbox");
    if (box) {
      box.hidden = false;
      box.setAttribute("aria-hidden", "false");
    }
    document.body.classList.add("is-img-lightbox");
    showImgAt(imgGallery.index, true);
  } else {
    closeImgLightbox({ restoreFocus: false });
    showImgAt(imgGallery.index, false);
  }
}

const gpsMap = {
  points: [],
  mode: "point",
  index: 0,
  lightbox: false,
  conceptId: "",
  gen: 0,
  edit: false,
  ignoreClick: false,
  mini: null,
  big: null,
  miniEl: null,
  bigEl: null,
  markersMini: [],
  markersBig: []
};

function gpsVisibleCount(mode, n) {
  return mode === "polygon" && n > 1 ? n - 1 : n;
}

function gpsModeOf(points) {
  if (points.length < 2) return "point";
  const a = points[0];
  const b = points[points.length - 1];
  if (points.length >= 3
      && Math.abs(a.lat - b.lat) < 1e-9
      && Math.abs(a.lng - b.lng) < 1e-9) {
    return "polygon";
  }
  return "polyline";
}

function gpsBadgeText() {
  if (gpsMap.edit && !gpsMap.points.length) return "Cliquez pour poser un point";
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  if (gpsMap.mode === "polygon") return "Polygone · " + n + " sommet" + (n > 1 ? "s" : "");
  if (gpsMap.mode === "polyline") return "Polyligne · " + n + " point" + (n > 1 ? "s" : "");
  return gpsMap.edit ? (n + " point" + (n > 1 ? "s" : "")) : "Point";
}

function gpsOsmUrl(pt) {
  if (!pt) return "#";
  const lat = encodeURIComponent(pt.lat);
  const lng = encodeURIComponent(pt.lng);
  return "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lng + "#map=16/" + lat + "/" + lng;
}

function gpsAccent() {
  return getComputedStyle(document.documentElement).getPropertyValue("--accent").trim() || "#1f7a5c";
}

function gpsDrawLatLngs() {
  if (gpsMap.edit) {
    return gpsMap.points.map((p) => [p.lat, p.lng]);
  }
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  return gpsMap.points.slice(0, n).map((p) => [p.lat, p.lng]);
}

function formatGpsNum(n) {
  const text = String(Math.round(Number(n) * 1e6) / 1e6);
  return text.indexOf(".") >= 0 ? text : text + ".0";
}

function writeGpsEditCoord(index, lat, lng) {
  const latIn = document.querySelector("[data-gps-axis='lat'][data-index='" + index + "']");
  const lngIn = document.querySelector("[data-gps-axis='lng'][data-index='" + index + "']");
  const latText = formatGpsNum(lat);
  const lngText = formatGpsNum(lng);
  if (latIn) latIn.value = latText;
  if (lngIn) lngIn.value = lngText;
  const btn = document.querySelector("#cvGpsPts [data-act='gps-map-goto'][data-index='" + index + "']");
  if (btn) {
    btn.setAttribute("data-lat", latText);
    btn.setAttribute("data-lng", lngText);
  }
}

function bindGpsEditHandlers(map) {
  const canvas = map.getContainer();
  if (canvas) canvas.style.cursor = "crosshair";
  map.on("dragstart", () => { gpsMap.ignoreClick = true; });
  map.on("dragend", () => {
    window.setTimeout(() => { gpsMap.ignoreClick = false; }, 220);
  });
  map.on("click", (e) => {
    if (gpsMap.ignoreClick) return;
    const latEl = document.querySelector("[id$='gpsClickLat']");
    const lngEl = document.querySelector("[id$='gpsClickLng']");
    const go = document.querySelector("[id$='gpsClickAdd']");
    if (!latEl || !lngEl || !go) return;
    latEl.value = formatGpsNum(e.latlng.lat);
    lngEl.value = formatGpsNum(e.latlng.lng);
    go.click();
  });
}

function ensureLeaflet() {
  if (window.L) return Promise.resolve();
  if (ensureLeaflet._p) return ensureLeaflet._p;
  const cssHref = document.body.getAttribute("data-leaflet-css") || "";
  const jsHref = document.body.getAttribute("data-leaflet-js") || "";
  ensureLeaflet._p = new Promise((resolve, reject) => {
    if (!jsHref) {
      reject(new Error("leaflet"));
      return;
    }
    if (cssHref && !document.querySelector("link[data-leaflet]")) {
      const css = document.createElement("link");
      css.rel = "stylesheet";
      css.href = cssHref;
      css.dataset.leaflet = "1";
      document.head.appendChild(css);
    }
    const script = document.createElement("script");
    script.src = jsHref;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("leaflet"));
    document.head.appendChild(script);
  });
  return ensureLeaflet._p;
}

function destroyGpsMap(slot) {
  if (gpsMap[slot]) {
    gpsMap[slot].remove();
    gpsMap[slot] = null;
  }
  if (slot === "mini") {
    gpsMap.miniEl = null;
    gpsMap.markersMini = [];
  } else {
    gpsMap.bigEl = null;
    gpsMap.markersBig = [];
  }
}

function destroyGpsMaps() {
  destroyGpsMap("mini");
  destroyGpsMap("big");
}

function closeGpsLightbox(opts) {
  const open = gpsMap.lightbox;
  gpsMap.lightbox = false;
  document.body.classList.remove("is-gps-lightbox");
  const box = $("#cvGpsLightbox");
  if (box) {
    box.hidden = true;
    box.setAttribute("aria-hidden", "true");
  }
  destroyGpsMap("big");
  if (open && !(opts && opts.restoreFocus === false)) {
    const fab = $("[data-act='gps-map-expand']");
    if (fab) fab.focus();
  }
  return open;
}

function updateGpsMeta() {
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  const pt = gpsMap.points[Math.min(gpsMap.index, Math.max(n - 1, 0))];
  const badge = $("#cvGpsBadge");
  if (badge) badge.textContent = gpsBadgeText();
  const cap = $("#cvGpsLightboxCap");
  if (cap) {
    cap.textContent = pt
        ? gpsBadgeText() + " · " + pt.lat + "  " + pt.lng
        : gpsBadgeText();
  }
  const href = gpsOsmUrl(pt);
  const osm = $("#cvGpsOsm");
  if (osm) osm.href = href;
  const osmBig = $("#cvGpsLightboxOsm");
  if (osmBig) osmBig.href = href;
  $$("#cvGpsPts .gps-pt").forEach((btn, i) => {
    const row = btn.closest("li");
    if (row) row.hidden = !gpsMap.edit && i >= n;
    btn.classList.toggle("is-on", i === gpsMap.index);
  });
}

function paintGpsPins(markers) {
  (markers || []).forEach((marker, i) => {
    const el = marker.getElement();
    if (el) el.classList.toggle("is-on", i === gpsMap.index);
  });
}

function selectGpsPoint(index) {
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  if (!n) return;
  gpsMap.index = ((index % n) + n) % n;
  updateGpsMeta();
  paintGpsPins(gpsMap.markersMini);
  paintGpsPins(gpsMap.markersBig);
  const pt = gpsMap.points[gpsMap.index];
  if (!pt) return;
  const ll = [pt.lat, pt.lng];
  if (gpsMap.mini) gpsMap.mini.panTo(ll);
  if (gpsMap.big) gpsMap.big.panTo(ll);
}

function mountGpsLayer(map, markerSlot) {
  const latlngs = gpsDrawLatLngs();
  if (!latlngs.length || !window.L) return;
  const accent = gpsAccent();
  if (gpsMap.mode === "polygon" && latlngs.length >= 3) {
    L.polygon(latlngs, { color: accent, weight: 2.5, fillColor: accent, fillOpacity: 0.18 }).addTo(map);
  } else if (gpsMap.mode === "polyline" && latlngs.length >= 2) {
    L.polyline(latlngs, { color: accent, weight: 2.5 }).addTo(map);
  }
  const markers = latlngs.map((ll, i) => {
    const marker = L.marker(ll, {
      draggable: !!gpsMap.edit,
      icon: L.divIcon({
        className: "gps-pin" + (i === gpsMap.index ? " is-on" : ""),
        html: '<span class="gps-pin-n">' + (i + 1) + "</span>",
        iconSize: [22, 22],
        iconAnchor: [11, 11]
      })
    }).addTo(map);
    marker.on("click", () => selectGpsPoint(i));
    if (gpsMap.edit) {
      marker.on("dragstart", () => { gpsMap.ignoreClick = true; });
      marker.on("dragend", (ev) => {
        const next = ev.target.getLatLng();
        writeGpsEditCoord(i, next.lat, next.lng);
        gpsMap.points[i] = { lat: next.lat, lng: next.lng };
        gpsMap.mode = gpsModeOf(gpsMap.points);
        mountGpsMini();
        window.setTimeout(() => { gpsMap.ignoreClick = false; }, 220);
      });
    }
    return marker;
  });
  gpsMap[markerSlot] = markers;
  if (latlngs.length === 1) map.setView(latlngs[0], 14);
  else map.fitBounds(L.latLngBounds(latlngs).pad(0.14));
}

function mountGpsMini() {
  const canvas = $("#cvGpsCanvas");
  if (!canvas || !window.L) return;
  if (!gpsMap.points.length && !gpsMap.edit) return;
  destroyGpsMap("mini");
  gpsMap.miniEl = canvas;
  gpsMap.mini = L.map(canvas, {
    scrollWheelZoom: false,
    doubleClickZoom: !gpsMap.edit,
    attributionControl: true
  });
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "© OpenStreetMap"
  }).addTo(gpsMap.mini);
  if (gpsMap.points.length) mountGpsLayer(gpsMap.mini, "markersMini");
  else gpsMap.mini.setView([46.7, 2.5], 5);
  if (gpsMap.edit) bindGpsEditHandlers(gpsMap.mini);
  const refreshMini = () => gpsMap.mini && gpsMap.mini.invalidateSize();
  requestAnimationFrame(refreshMini);
  setTimeout(refreshMini, 260);
}

function mountGpsBig() {
  const canvas = $("#cvGpsLightboxCanvas");
  if (!canvas || !window.L || !gpsMap.points.length) return;
  destroyGpsMap("big");
  gpsMap.bigEl = canvas;
  gpsMap.big = L.map(canvas, { scrollWheelZoom: true, attributionControl: true });
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "© OpenStreetMap"
  }).addTo(gpsMap.big);
  mountGpsLayer(gpsMap.big, "markersBig");
  const refreshBig = () => gpsMap.big && gpsMap.big.invalidateSize();
  requestAnimationFrame(refreshBig);
  setTimeout(refreshBig, 80);
}

function openGpsLightbox() {
  if (!gpsMap.points.length) return;
  if (typeof closeImgLightbox === "function") closeImgLightbox({ restoreFocus: false });
  gpsMap.lightbox = true;
  document.body.classList.add("is-gps-lightbox");
  const box = $("#cvGpsLightbox");
  if (box) {
    box.hidden = false;
    box.setAttribute("aria-hidden", "false");
  }
  updateGpsMeta();
  ensureLeaflet().then(() => {
    if (!gpsMap.lightbox) return;
    mountGpsBig();
    const closeBtn = box && box.querySelector("[data-act='gps-lightbox-close']");
    if (closeBtn) closeBtn.focus();
  }).catch(() => {});
}

function bindGpsMap() {
  const cv = document.querySelector("#viewLive .cv.is-on");
  const conceptId = cv ? (cv.getAttribute("data-id") || "") : "";
  if (conceptId !== gpsMap.conceptId) {
    closeGpsLightbox({ restoreFocus: false });
    gpsMap.index = 0;
    gpsMap.conceptId = conceptId;
  }
  const root = $("#cvGpsMap");
  const gen = ++gpsMap.gen;
  if (!root) {
    closeGpsLightbox({ restoreFocus: false });
    destroyGpsMaps();
    gpsMap.points = [];
    gpsMap.index = 0;
    gpsMap.edit = false;
    return;
  }
  gpsMap.edit = root.getAttribute("data-edit") === "1";
  const points = $$("#cvGpsPts [data-act='gps-map-goto']").map((btn) => ({
    lat: parseFloat(btn.getAttribute("data-lat") || ""),
    lng: parseFloat(btn.getAttribute("data-lng") || "")
  })).filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng));
  gpsMap.points = points;
  gpsMap.mode = gpsModeOf(points);
  const n = gpsVisibleCount(gpsMap.mode, points.length);
  if (gpsMap.index >= n) gpsMap.index = 0;
  updateGpsMeta();
  if (gpsMap.miniEl && gpsMap.miniEl !== $("#cvGpsCanvas")) destroyGpsMap("mini");
  if (gpsMap.bigEl && gpsMap.bigEl !== $("#cvGpsLightboxCanvas")) destroyGpsMap("big");
  if (!points.length && !gpsMap.edit) {
    destroyGpsMaps();
    return;
  }
  ensureLeaflet().then(() => {
    if (gen !== gpsMap.gen) return;
    mountGpsMini();
    if (gpsMap.lightbox) {
      const box = $("#cvGpsLightbox");
      if (box) {
        box.hidden = false;
        box.setAttribute("aria-hidden", "false");
      }
      document.body.classList.add("is-gps-lightbox");
      mountGpsBig();
    }
  }).catch(() => {
    if (gen !== gpsMap.gen) return;
    const canvas = $("#cvGpsCanvas");
    if (canvas) {
      canvas.classList.add("is-off");
      canvas.textContent = "Carte indisponible";
    }
  });
}

function applyDndFlash() {
  const host = document.getElementById("previewDetailState");
  if (!host) return;
  const msg = host.getAttribute("data-flash-dnd");
  const token = host.getAttribute("data-flash-dnd-token");
  if (!msg || !token || token === applyConceptLabelUi._dndToken) return;
  applyConceptLabelUi._dndToken = token;
  if (typeof toast === "function") toast(msg, { soft: true });
}

function applyConceptLabelUi(source) {
  applyDndFlash();
  const cv = document.querySelector("#viewLive .cv.is-on");
  if (!cv) {
    closeImgLightbox({ restoreFocus: false });
    closeGpsLightbox({ restoreFocus: false });
    return;
  }
  const flashes = [
    { msg: cv.getAttribute("data-flash"), token: cv.getAttribute("data-flash-token"), kind: "label" },
    { msg: cv.getAttribute("data-flash-coll"), token: cv.getAttribute("data-flash-coll-token"), kind: "coll" },
    { msg: cv.getAttribute("data-flash-rel"), token: cv.getAttribute("data-flash-rel-token"), kind: "rel" },
    { msg: cv.getAttribute("data-flash-crel"), token: cv.getAttribute("data-flash-crel-token"), kind: "crel" },
    { msg: cv.getAttribute("data-flash-tr"), token: cv.getAttribute("data-flash-tr-token"), kind: "tr" },
    { msg: cv.getAttribute("data-flash-note"), token: cv.getAttribute("data-flash-note-token"), kind: "note" },
    { msg: cv.getAttribute("data-flash-res"), token: cv.getAttribute("data-flash-res-token"), kind: "res" },
    { msg: cv.getAttribute("data-flash-align"), token: cv.getAttribute("data-flash-align-token"), kind: "align" }
  ];
  flashes.forEach((item) => {
    if (!item.msg || !item.token) return;
    const seenKey = "_" + item.kind + "Token";
    if (item.token === applyConceptLabelUi[seenKey]) return;
    applyConceptLabelUi[seenKey] = item.token;
    if (typeof toast === "function") toast(item.msg, { soft: true });
    if (item.kind === "coll" && typeof invalidateCollectionTree === "function") {
      invalidateCollectionTree();
      if (typeof ensureCollectionTree === "function") ensureCollectionTree();
    }
    if (item.kind === "rel" && cv.getAttribute("data-rel-tree") === "1"
        && typeof revealTreeConcept === "function") {
      revealTreeConcept(cv.getAttribute("data-id"));
    }
  });
  const id = cv.getAttribute("data-id");
  const pref = cv.getAttribute("data-pref");
  if (id && pref) {
    const escaped = window.CSS && CSS.escape ? CSS.escape(id) : id.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
    document.querySelectorAll('.tn-label[data-id="' + escaped + '"] .tn-text').forEach((el) => {
      el.textContent = pref;
    });
  }
  bindImgGallery();
  bindGpsMap();
  const editing = document.querySelector("#viewLive .cblock.is-editing");
  if (!editing) {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    playLabelBlockEnter(false);
    return;
  }
  const card = editing.getAttribute("data-card-id");
  if (card === "contexte") {
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    bindLabelFacetPicker();
  } else if (card === "collections") {
    closeLabelFacetPicker();
    closeAllRelPickers();
    closeCrelPicker();
    bindCvCollPicker();
  } else if (card === "relations") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeCrelPicker();
    bindRelPickers();
  } else if (card === "relPerso") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    bindCrelPicker();
  } else if (card === "traductions") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
  } else if (card === "notes") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
  } else if (card === "ressources") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    closeImgLightbox({ restoreFocus: false });
    closeGpsLightbox({ restoreFocus: false });
  } else if (card === "alignement") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
  }
  playLabelBlockEnter(true);
  const sourceId = source && source.id ? String(source.id) : "";
  if (/gpsClickAdd|alignAutoStart|alignCompareStart|alignAutoAdd|alignDeleteSave|alignReplaceSave/.test(sourceId)) return;
  if (/trAddRow|noteAddRow|resAddRow|imgAddRow|gpsAddRow/.test(sourceId)) {
    const cards = editing.querySelectorAll(".tr-edit-card, .note-edit-card, .media-edit-card, .gps-edit-row");
    const last = cards[cards.length - 1];
    const added = last && last.querySelector(".tr-edit-value, .note-edit-value, .media-edit-uri, .gps-edit-coord");
    if (added) added.focus({ preventScroll: true });
    return;
  }
  if (/trRemove|noteRemove|resRemove|imgRemove|gpsRemove/.test(sourceId)) return;
  if (editing.contains(document.activeElement)) return;
  const input = editing.querySelector(".crow.is-editing .st-input")
      || editing.querySelector(".cblock-edit-row .st-input")
      || editing.querySelector(".search-input");
  if (input) input.focus({ preventScroll: true });
}

function playLabelBlockEnter(nowEditing) {
  const editingEl = document.querySelector("#viewLive .cblock.is-editing");
  const id = editingEl
      ? editingEl.getAttribute("data-card-id")
      : playLabelBlockEnter._id;
  const block = editingEl
      || (id ? document.querySelector("#viewLive .cblock[data-card-id='" + id + "']") : null);
  if (id) playLabelBlockEnter._id = id;
  if (!block) return;
  const was = playLabelBlockEnter._editing;
  playLabelBlockEnter._editing = nowEditing;
  if (typeof was !== "boolean" || was === nowEditing) return;
  const body = block.querySelector(".cblock-body");
  if (body) body.classList.add("is-enter");
  block.classList.remove("is-swap", "is-leaving");
}

function interceptLabelEditSwap(e) {
  const start = e.target.closest("[id$='labelEditStart'], [id$='collEditStart'], [id$='relEditStart'], [id$='crelEditStart'], [id$='trEditStart'], [id$='noteEditStart']");
  const cancel = e.target.closest("[id$='labelEditCancel'], [id$='collEditCancel'], [id$='relEditCancel'], [id$='crelEditCancel'], [id$='trEditCancel'], [id$='noteEditCancel']");
  const trigger = start || cancel;
  if (!trigger) return;
  const block = trigger.closest(".cblock")
    || document.querySelector("#viewLive .cblock[data-card-id='contexte']");
  if (!block || block.classList.contains("is-swap")) return;
  e.preventDefault();
  e.stopImmediatePropagation();
  if (block.classList.contains("is-leaving")) return;
  const leaving = block.querySelector(".cblock-body");
  block.classList.add("is-leaving");
  if (leaving) leaving.classList.add("is-leave");
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  let done = false;
  const go = () => {
    if (done) return;
    done = true;
    block.classList.remove("is-leaving");
    block.classList.add("is-swap");
    trigger.click();
  };
  if (reduce || !leaving) {
    go();
    return;
  }
  leaving.addEventListener("animationend", go, { once: true });
  setTimeout(go, 260);
}

const ficheScrollLock = { held: false, y: 0 };

function lockFicheScroll(source) {
  const form = document.getElementById("previewDetailForm");
  if (!form) return;
  let src = source;
  if (typeof src === "string") src = document.getElementById(src);
  if (!src || src.nodeType !== 1 || !form.contains(src)) return;
  const live = $("#viewLive");
  if (!live || !live.classList.contains("is-on")) return;
  const view = $("#previewView");
  if (!view) return;
  ficheScrollLock.y = view.scrollTop;
  ficheScrollLock.held = true;
}

function restoreFicheScroll() {
  if (!ficheScrollLock.held) return;
  const y = ficheScrollLock.y || 0;
  const apply = () => {
    const view = $("#previewView");
    if (view) view.scrollTop = y;
  };
  apply();
  requestAnimationFrame(apply);
  window.setTimeout(() => {
    apply();
    ficheScrollLock.held = false;
  }, 80);
}

window.onLabelSave = function (data) {
  const btns = document.querySelectorAll(
      "#labelSaveConfirm .abt-save, #collSaveConfirm .abt-save, #relSaveConfirm .abt-save, #crelSaveConfirm .abt-save, #trSaveConfirm .abt-save, #noteSaveConfirm .abt-save, #resLinkSaveConfirm .abt-save, #resImgSaveConfirm .abt-save, #resGpsSaveConfirm .abt-save, .cblock-edit-warn .abt-save"
  );
  if (data.status === "begin") {
    if (typeof hideConfirm === "function") {
      hideConfirm("#labelSaveConfirm");
      hideConfirm("#collSaveConfirm");
      hideConfirm("#relSaveConfirm");
      hideConfirm("#crelSaveConfirm");
      hideConfirm("#trSaveConfirm");
      hideConfirm("#noteSaveConfirm");
      hideConfirm("#resLinkSaveConfirm");
      hideConfirm("#resImgSaveConfirm");
      hideConfirm("#resGpsSaveConfirm");
    }
    btns.forEach((btn) => btn.classList.add("is-busy"));
  }
  if (data.status === "success" || data.status === "complete") {
    btns.forEach((btn) => btn.classList.remove("is-busy", "is-click"));
  }
}

window.onAlignDelete = function (data) {
  const btns = document.querySelectorAll("#alignDeleteConfirm .abt-save");
  if (data.status === "begin") {
    if (typeof hideConfirm === "function") hideConfirm("#alignDeleteConfirm");
    btns.forEach((btn) => btn.classList.add("is-busy"));
  }
  if (data.status === "success" || data.status === "complete") {
    btns.forEach((btn) => btn.classList.remove("is-busy", "is-click"));
  }
}

window.onAlignReplace = function (data) {
  const btns = document.querySelectorAll("#alignReplaceConfirm .abt-save");
  if (data.status === "begin") {
    if (typeof hideConfirm === "function") hideConfirm("#alignReplaceConfirm");
    btns.forEach((btn) => btn.classList.add("is-busy"));
  }
  if (data.status === "success" || data.status === "complete") {
    btns.forEach((btn) => btn.classList.remove("is-busy", "is-click"));
  }
}

window.onAlignAutoSearch = function (data) {
  const panel = document.querySelector("#viewLive .align-auto");
  const actions = document.querySelector("#viewLive .align-auto-actions");
  if (data.status === "begin") {
    if (panel) panel.classList.add("is-searching");
    if (actions) actions.classList.add("is-searching");
  }
  if (data.status === "success" || data.status === "complete") {
    if (panel) panel.classList.remove("is-searching");
    if (actions) actions.classList.remove("is-searching");
  }
}

function finishLiveOpen() {
  const live = $("#viewLive");
  if (!live) return;
  const opening = live.classList.contains("is-loading");
  live.classList.remove("is-loading");
  live.classList.add("is-ready");
  if (opening) {
    live.classList.add("is-opening");
    window.setTimeout(() => live.classList.remove("is-opening"), 280);
  }
  requestAnimationFrame(() => {
    if (window.applyConceptCardOrder) window.applyConceptCardOrder();
    if (window.syncViewRail) window.syncViewRail();
    applyConceptLabelUi();
    restoreFicheScroll();
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
  if (view && !ficheScrollLock.held) view.scrollTop = 0;
  else restoreFicheScroll();
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
  const settings = $("#viewSettings");
  if (settings && !IS_CONSULT) {
    showPanel(".view-panel", "viewSettings");
    return;
  }
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
  if (tbl) {
    tbl.hidden = state.view !== "tableau";
    if (state.view === "tableau") applyTableCols();
  }
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
  if (view === "arbo" && !state.conceptId) {
    state.home = true;
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
  const titleEl = ov.querySelector(".cblock-overlay-title");
  const dest = ov.querySelector(".block-modal-body");
  if (titleEl) {
    const named = block.querySelector(".cblock-title");
    titleEl.textContent = (named && named.textContent || "").trim();
  }
  if (dest) dest.replaceChildren(body.cloneNode(true));
  ov.hidden = false;
  ov.setAttribute("aria-hidden", "false");
}

const CARD_IDS = ["contexte", "collections", "relations", "relPerso", "traductions", "notes", "ressources", "alignement", "identifiants", "temporel"];

function isCardLayoutEditable() {
  return document.body.getAttribute("data-logged-in") === "1";
}

function defaultCardLayout() {
  return { order: CARD_IDS.slice(), collapsed: {} };
}

function parseCardLayout() {
  const layout = defaultCardLayout();
  try {
    const raw = JSON.parse(document.body.getAttribute("data-card-layout") || "");
    if (Array.isArray(raw.order)) {
      const next = raw.order.filter((id) => CARD_IDS.indexOf(id) >= 0);
      layout.order = next.concat(CARD_IDS.filter((id) => next.indexOf(id) < 0));
    }
    layout.collapsed = {};
    if (Array.isArray(raw.collapsed)) {
      raw.collapsed.forEach((id) => {
        if (CARD_IDS.indexOf(id) >= 0) layout.collapsed[id] = true;
      });
    }
  } catch (ex) {}
  return layout;
}

let cardLayout = null;

function currentCardLayout() {
  if (!cardLayout) cardLayout = parseCardLayout();
  return cardLayout;
}

function readCardOrder() {
  return currentCardLayout().order.slice();
}

function readCardFold() {
  return Object.assign({}, currentCardLayout().collapsed);
}

function persistCardLayout() {
  if (!isCardLayoutEditable()) return;
  const layout = currentCardLayout();
  const body = document.body;
  const payload = {
    order: layout.order,
    collapsed: Object.keys(layout.collapsed)
  };
  try {
    body.setAttribute("data-card-layout", JSON.stringify(payload));
  } catch (ex) {}
  const ctx = body.getAttribute("data-ctx") || "";
  clearTimeout(persistCardLayout._t);
  persistCardLayout._t = setTimeout(() => {
    fetch(ctx + "/v2/api/account/concept-blocks", {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload)
    }).catch(() => {});
  }, 180);
}

function accountBlockLabels() {
  const map = {};
  $$("#accBlockLabelSrc [data-id]").forEach((el) => {
    map[el.getAttribute("data-id")] = (el.textContent || "").trim();
  });
  return map;
}

function accountBlockMsg(name, fallback) {
  const root = $("#accBlockLayout");
  return (root && root.getAttribute("data-msg-" + name)) || fallback;
}

function accBlockReduceMotion() {
  return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function accBlockGripHtml(editable) {
  const label = escapeHtml(accountBlockMsg("drag", "Glisser pour réordonner"));
  return '<span class="cblock-drag acc-block-drag" role="button" tabindex="' + (editable ? "0" : "-1") + '"'
    + ' title="' + label + '" aria-label="' + label + '">'
    + '<svg class="cblock-grip" viewBox="0 0 8 14" aria-hidden="true" focusable="false">'
    + '<circle cx="2" cy="2" r="1.35"/><circle cx="6" cy="2" r="1.35"/>'
    + '<circle cx="2" cy="7" r="1.35"/><circle cx="6" cy="7" r="1.35"/>'
    + '<circle cx="2" cy="12" r="1.35"/><circle cx="6" cy="12" r="1.35"/>'
    + "</svg></span>";
}

function paintAccountBlockRow(row, id, index) {
  if (!row) return;
  const open = !currentCardLayout().collapsed[id];
  const openLbl = accountBlockMsg("open", "Ouvert");
  const closedLbl = accountBlockMsg("closed", "Fermé");
  const n = row.querySelector(".acc-block-n");
  const state = row.querySelector(".acc-block-state");
  const sw = row.querySelector("[data-act='acc-block-toggle']");
  if (n) n.textContent = String(index + 1);
  row.classList.toggle("is-closed", !open);
  if (state) {
    state.textContent = open ? openLbl : closedLbl;
    state.classList.toggle("is-open", open);
  }
  if (sw) {
    sw.classList.toggle("on", open);
    sw.setAttribute("aria-checked", open ? "true" : "false");
    sw.setAttribute("title", open ? openLbl : closedLbl);
    sw.setAttribute("aria-label", open ? openLbl : closedLbl);
  }
}

function renderAccountBlockList() {
  const box = $("#accBlockRows");
  const root = $("#accBlockLayout");
  if (!box || !root) return;
  const layout = currentCardLayout();
  const labels = accountBlockLabels();
  const editable = isCardLayoutEditable();
  const resetBtn = root.querySelector("[data-act='acc-block-reset']");
  if (resetBtn) resetBtn.disabled = !editable;
  box.innerHTML = layout.order.map((id, i) => {
    const open = !layout.collapsed[id];
    return '<div class="acc-block-row' + (open ? "" : " is-closed") + '" data-id="' + escapeHtml(id) + '">'
      + accBlockGripHtml(editable)
      + '<span class="acc-block-n">' + (i + 1) + "</span>"
      + '<span class="acc-block-t">' + escapeHtml(labels[id] || id) + "</span>"
      + '<span class="acc-block-state' + (open ? " is-open" : "") + '">'
      + escapeHtml(open ? accountBlockMsg("open", "Ouvert") : accountBlockMsg("closed", "Fermé"))
      + "</span>"
      + '<button type="button" class="vo-sw acc-block-sw' + (open ? " on" : "") + '" data-act="acc-block-toggle"'
      + ' role="switch" aria-checked="' + (open ? "true" : "false") + '"'
      + ' title="' + escapeHtml(open ? accountBlockMsg("open", "Ouvert") : accountBlockMsg("closed", "Fermé")) + '"'
      + (editable ? "" : " disabled") + "></button>"
      + "</div>";
  }).join("");
}

function applyAccountBlockDomOrder() {
  const box = $("#accBlockRows");
  if (!box) return;
  currentCardLayout().order.forEach((id, i) => {
    const row = box.querySelector('.acc-block-row[data-id="' + CSS.escape(id) + '"]');
    if (!row) return;
    box.appendChild(row);
    paintAccountBlockRow(row, id, i);
  });
}

function flipAccountBlockRows(run) {
  const box = $("#accBlockRows");
  if (!box) {
    run();
    return;
  }
  const rows = $$(".acc-block-row", box);
  if (accBlockReduceMotion() || !rows.length) {
    run();
    return;
  }
  const first = new Map(rows.map((el) => [el, el.getBoundingClientRect()]));
  run();
  $$(".acc-block-row", box).forEach((el) => {
    const prev = first.get(el);
    if (!prev) return;
    const next = el.getBoundingClientRect();
    const dy = prev.top - next.top;
    if (Math.abs(dy) < 1) return;
    el.style.transition = "none";
    el.style.transform = "translateY(" + dy + "px)";
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        el.style.transition = "transform .22s cubic-bezier(.2,.8,.2,1)";
        el.style.transform = "";
        const clear = () => {
          el.style.transition = "";
          el.style.transform = "";
          el.removeEventListener("transitionend", clear);
        };
        el.addEventListener("transitionend", clear);
      });
    });
  });
}

function toggleAccountBlock(id) {
  if (!isCardLayoutEditable() || !id) return;
  const layout = currentCardLayout();
  if (layout.collapsed[id]) delete layout.collapsed[id];
  else layout.collapsed[id] = true;
  persistCardLayout();
  const row = $("#accBlockRows") && $("#accBlockRows").querySelector('.acc-block-row[data-id="' + CSS.escape(id) + '"]');
  const index = currentCardLayout().order.indexOf(id);
  paintAccountBlockRow(row, id, index);
}

function resetAccountBlocks() {
  if (!isCardLayoutEditable()) return;
  cardLayout = defaultCardLayout();
  persistCardLayout();
  renderAccountBlockList();
  const msg = accountBlockMsg("reset-done", "");
  if (msg) toast(msg);
}

function clearAccountDropMarks() {
  $$(".acc-block-row.drop-before, .acc-block-row.drop-after, .acc-block-row.is-dragging").forEach((el) => {
    el.classList.remove("drop-before", "drop-after", "is-dragging");
  });
}

function bindAccountBlockLayout() {
  const root = $("#accBlockLayout");
  if (!root) return;
  if (!bindAccountBlockLayout._on) {
    bindAccountBlockLayout._on = true;
    let handleId = null;
    let dragId = null;
    root.addEventListener("click", (e) => {
      const btn = e.target.closest("[data-act]");
      if (!btn || btn.disabled || !root.contains(btn)) return;
      const act = btn.getAttribute("data-act");
      const row = btn.closest(".acc-block-row");
      const id = row && row.getAttribute("data-id");
      if (act === "acc-block-reset") {
        e.preventDefault();
        resetAccountBlocks();
        return;
      }
      if (act === "acc-block-toggle") {
        e.preventDefault();
        toggleAccountBlock(id);
      }
    });
    root.addEventListener("pointerdown", (e) => {
      if (!isCardLayoutEditable()) return;
      const handle = e.target.closest(".acc-block-drag");
      if (!handle || handle.getAttribute("tabindex") === "-1") return;
      const row = handle.closest(".acc-block-row[data-id]");
      if (!row) return;
      handleId = row.getAttribute("data-id");
      row.setAttribute("draggable", "true");
    });
    root.addEventListener("pointerup", () => {
      if (dragId) return;
      handleId = null;
      $$('.acc-block-row[draggable="true"]', root).forEach((el) => el.removeAttribute("draggable"));
    });
    root.addEventListener("dragstart", (e) => {
      const row = e.target.closest && e.target.closest(".acc-block-row[data-id]");
      if (!row || row.getAttribute("data-id") !== handleId) {
        if (row) e.preventDefault();
        return;
      }
      dragId = handleId;
      row.classList.add("is-dragging");
      e.dataTransfer.effectAllowed = "move";
      try { e.dataTransfer.setData("text/plain", dragId); } catch (ex) {}
    });
    root.addEventListener("dragover", (e) => {
      if (!dragId) return;
      const row = e.target.closest && e.target.closest(".acc-block-row[data-id]");
      if (!row) return;
      e.preventDefault();
      e.dataTransfer.dropEffect = "move";
      const id = row.getAttribute("data-id");
      const pos = cardDropPos(e, row);
      $$(".acc-block-row.drop-before, .acc-block-row.drop-after", root).forEach((el) => {
        if (el !== row) el.classList.remove("drop-before", "drop-after");
      });
      if (id === dragId) {
        row.classList.remove("drop-before", "drop-after");
        return;
      }
      row.classList.toggle("drop-before", pos === "before");
      row.classList.toggle("drop-after", pos === "after");
    });
    root.addEventListener("drop", (e) => {
      const row = e.target.closest && e.target.closest(".acc-block-row[data-id]");
      if (!row || !dragId) return;
      e.preventDefault();
      const toId = row.getAttribute("data-id");
      const pos = cardDropPos(e, row);
      flipAccountBlockRows(() => {
        dropCardAt(dragId, toId, pos);
        applyAccountBlockDomOrder();
      });
    });
    root.addEventListener("dragend", () => {
      dragId = null;
      handleId = null;
      $$('.acc-block-row[draggable="true"]', root).forEach((el) => el.removeAttribute("draggable"));
      clearAccountDropMarks();
    });
  }
  renderAccountBlockList();
}

function applyConceptCardOrder(root) {
  const order = readCardOrder();
  const scope = root || document;
  scope.querySelectorAll(".cv-blocks .cblock[data-card-id]").forEach((el) => {
    const id = el.getAttribute("data-card-id");
    const i = order.indexOf(id);
    el.style.order = String(i < 0 ? 99 : i);
  });
  applyConceptCardFold(scope);
}

function foldLabels() {
  const cv = document.querySelector("#viewLive .cv");
  return {
    collapse: (cv && cv.getAttribute("data-msg-fold")) || "Replier",
    expand: (cv && cv.getAttribute("data-msg-unfold")) || "Déplier"
  };
}

function syncFoldButton(block, collapsed) {
  const btn = block.querySelector(":scope > .cblock-head .cblock-fold");
  if (!btn) return;
  const labels = foldLabels();
  const label = collapsed ? labels.expand : labels.collapse;
  btn.setAttribute("aria-expanded", collapsed ? "false" : "true");
  btn.setAttribute("title", label);
  btn.setAttribute("aria-label", label);
}

function applyConceptCardFold(root) {
  const map = readCardFold();
  const scope = root || document;
  scope.querySelectorAll(".cv-blocks .cblock[data-card-id]").forEach((el) => {
    const id = el.getAttribute("data-card-id");
    const editing = el.classList.contains("is-editing");
    const collapsed = !editing && !!map[id];
    el.classList.toggle("is-collapsed", collapsed);
    syncFoldButton(el, collapsed);
  });
}

function toggleConceptBlockFold(el) {
  if (!isCardLayoutEditable()) return;
  const block = el && el.closest && el.closest(".cblock[data-card-id]");
  if (!block || block.classList.contains("is-editing")) return;
  const id = block.getAttribute("data-card-id");
  if (!id) return;
  const layout = currentCardLayout();
  const collapsed = !block.classList.contains("is-collapsed");
  if (collapsed) layout.collapsed[id] = true;
  else delete layout.collapsed[id];
  persistCardLayout();
  block.classList.toggle("is-collapsed", collapsed);
  syncFoldButton(block, collapsed);
  if (!collapsed && id === "ressources") {
    requestAnimationFrame(() => {
      if (typeof bindGpsMap === "function") bindGpsMap();
    });
  }
  if (window.syncViewRail) window.syncViewRail();
}

function clearCardDropMarks() {
  $$(".cblock.drop-before, .cblock.drop-after, .cblock.is-dragging").forEach((el) => {
    el.classList.remove("drop-before", "drop-after", "is-dragging");
  });
}

function dropCardAt(fromId, toId, pos) {
  if (!isCardLayoutEditable() || !fromId || !toId || fromId === toId) return;
  const layout = currentCardLayout();
  const order = layout.order;
  const fi = order.indexOf(fromId);
  if (fi < 0) return;
  order.splice(fi, 1);
  let ti = order.indexOf(toId);
  if (ti < 0) return;
  if (pos === "after") ti += 1;
  order.splice(ti, 0, fromId);
  persistCardLayout();
  applyConceptCardOrder();
}

function cardDropPos(e, el) {
  const r = el.getBoundingClientRect();
  return e.clientY < r.top + r.height / 2 ? "before" : "after";
}

function bindConceptCardOrder() {
  try {
    localStorage.removeItem("ot.conceptCardOrder.v2");
    localStorage.removeItem("ot.conceptCardFold.v2");
  } catch (ex) {}
  applyConceptCardOrder();
  bindAccountBlockLayout();
  if (bindConceptCardOrder._on) return;
  bindConceptCardOrder._on = true;
  document.addEventListener("click", (e) => {
    if (e.target.closest(".cblock-fold, .cblock-drag, input, button, a, textarea, select")) return;
    const title = e.target.closest("#viewLive .cblock-title");
    if (!title) return;
    toggleConceptBlockFold(title);
  });
  let handleId = null;
  let dragId = null;
  document.addEventListener("pointerdown", (e) => {
    if (!isCardLayoutEditable()) return;
    const handle = e.target.closest(".cblock-drag");
    if (!handle) return;
    const block = handle.closest(".cblock[data-card-id]");
    if (!block) return;
    handleId = block.getAttribute("data-card-id");
    block.setAttribute("draggable", "true");
  });
  document.addEventListener("pointerup", () => {
    if (dragId) return;
    handleId = null;
    $$('.cblock[draggable="true"]').forEach((el) => el.removeAttribute("draggable"));
  });
  document.addEventListener("dragstart", (e) => {
    const block = e.target.closest && e.target.closest(".cblock[data-card-id]");
    if (!block || block.getAttribute("data-card-id") !== handleId) {
      if (block) e.preventDefault();
      return;
    }
    dragId = handleId;
    block.classList.add("is-dragging");
    e.dataTransfer.effectAllowed = "move";
    try { e.dataTransfer.setData("text/plain", dragId); } catch (ex) {}
  });
  document.addEventListener("dragover", (e) => {
    if (!dragId) return;
    const block = e.target.closest && e.target.closest(".cblock[data-card-id]");
    if (!block) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    const id = block.getAttribute("data-card-id");
    const pos = cardDropPos(e, block);
    $$(".cblock.drop-before, .cblock.drop-after").forEach((el) => {
      if (el !== block) el.classList.remove("drop-before", "drop-after");
    });
    if (id === dragId) {
      block.classList.remove("drop-before", "drop-after");
      return;
    }
    block.classList.toggle("drop-before", pos === "before");
    block.classList.toggle("drop-after", pos === "after");
  });
  document.addEventListener("drop", (e) => {
    const block = e.target.closest && e.target.closest(".cblock[data-card-id]");
    if (!block || !dragId) return;
    e.preventDefault();
    const toId = block.getAttribute("data-card-id");
    dropCardAt(dragId, toId, cardDropPos(e, block));
  });
  document.addEventListener("dragend", () => {
    dragId = null;
    handleId = null;
    $$('.cblock[draggable="true"]').forEach((el) => el.removeAttribute("draggable"));
    clearCardDropMarks();
  });
}

window.applyConceptCardOrder = applyConceptCardOrder;
window.applyConceptCardFold = applyConceptCardFold;
window.toggleConceptBlockFold = toggleConceptBlockFold;
window.bindConceptCardOrder = bindConceptCardOrder;
window.applyConceptLabelUi = applyConceptLabelUi;
window.closeImgLightbox = closeImgLightbox;
window.closeGpsLightbox = closeGpsLightbox;
document.addEventListener("click", interceptLabelEditSwap, true);
document.addEventListener("click", (e) => {
  const act = e.target.closest("[data-act]");
  const action = act && act.getAttribute("data-act");
  if (action === "img-gallery-open") {
    e.preventDefault();
    openImgLightbox();
    return;
  }
  if (action === "img-gallery-prev" || action === "img-lightbox-prev") {
    e.preventDefault();
    showImgAt(imgGallery.index - 1, action === "img-lightbox-prev" || imgGallery.lightbox);
    return;
  }
  if (action === "img-gallery-next" || action === "img-lightbox-next") {
    e.preventDefault();
    showImgAt(imgGallery.index + 1, action === "img-lightbox-next" || imgGallery.lightbox);
    return;
  }
  if (action === "img-gallery-thumb") {
    e.preventDefault();
    const idx = parseInt(act.getAttribute("data-index") || "0", 10);
    showImgAt(Number.isFinite(idx) ? idx : 0, imgGallery.lightbox);
    return;
  }
  if (action === "img-lightbox-modal") {
    return;
  }
  if (action === "img-lightbox-dismiss" || action === "img-lightbox-close") {
    e.preventDefault();
    closeImgLightbox();
    return;
  }
  if (action === "gps-map-expand") {
    e.preventDefault();
    openGpsLightbox();
    return;
  }
  if (action === "gps-map-goto") {
    e.preventDefault();
    const idx = parseInt(act.getAttribute("data-index") || "0", 10);
    selectGpsPoint(Number.isFinite(idx) ? idx : 0);
    return;
  }
  if (action === "gps-lightbox-modal") {
    return;
  }
  if (action === "gps-lightbox-dismiss" || action === "gps-lightbox-close") {
    e.preventDefault();
    closeGpsLightbox();
    return;
  }
  if (action === "facet-pick-toggle") {
    e.preventDefault();
    toggleLabelFacetMenu();
    return;
  }
  if (action === "facet-pick-clear") {
    e.preventDefault();
    const input = $("#labelFacetQuery");
    if (input) {
      input.value = "";
      input.focus();
    }
    labelFacetPicker.showAll = false;
    syncLabelFacetClear();
    closeLabelFacetPicker();
    return;
  }
  if (action === "facet-pick-more") {
    e.preventDefault();
    labelFacetPicker.showAll = true;
    renderLabelFacetMenu($("#labelFacetQuery") ? $("#labelFacetQuery").value : "");
    return;
  }
  if (action === "facet-pick-item") {
    e.preventDefault();
    toggleLabelFacetPick(act.getAttribute("data-id"), act.getAttribute("data-label"));
    const input = $("#labelFacetQuery");
    if (input) {
      input.value = "";
      syncLabelFacetClear();
      input.focus();
    }
    labelFacetPicker.showAll = false;
    renderLabelFacetMenu("");
    return;
  }
  if (action === "facet-chip-remove") {
    e.preventDefault();
    toggleLabelFacetPick(act.getAttribute("data-id"), "");
    return;
  }
  if (action === "cv-coll-pick-toggle") {
    e.preventDefault();
    toggleCvCollMenu();
    return;
  }
  if (action === "cv-coll-pick-clear") {
    e.preventDefault();
    const input = $("#cvCollQuery");
    if (input) {
      input.value = "";
      input.focus();
    }
    cvCollPicker.showAll = false;
    syncCvCollClear();
    closeCvCollPicker();
    return;
  }
  if (action === "cv-coll-pick-more") {
    e.preventDefault();
    cvCollPicker.showAll = true;
    renderCvCollMenu($("#cvCollQuery") ? $("#cvCollQuery").value : "");
    return;
  }
  if (action === "cv-coll-pick-item") {
    e.preventDefault();
    toggleCvCollPick(act.getAttribute("data-id"), act.getAttribute("data-label"));
    const input = $("#cvCollQuery");
    if (input) {
      input.value = "";
      syncCvCollClear();
      input.focus();
    }
    cvCollPicker.showAll = false;
    renderCvCollMenu("");
    return;
  }
  if (action === "cv-coll-chip-remove") {
    e.preventDefault();
    toggleCvCollPick(act.getAttribute("data-id"), "");
    return;
  }
  if (action === "rel-pick-toggle") {
    e.preventDefault();
    toggleRelMenu(act.getAttribute("data-kind"));
    return;
  }
  if (action === "rel-pick-clear") {
    e.preventDefault();
    const kind = act.getAttribute("data-kind");
    const els = relEls(kind);
    if (els.query) {
      els.query.value = "";
      els.query.focus();
    }
    if (relPickers[kind]) {
      relPickers[kind].showAll = false;
      relPickers[kind].hits = [];
      relPickers[kind].query = "";
      relPickers[kind].seq += 1;
    }
    syncRelClear(kind);
    closeRelPicker(kind);
    return;
  }
  if (action === "rel-pick-more") {
    e.preventDefault();
    const kind = act.getAttribute("data-kind");
    if (relPickers[kind]) relPickers[kind].showAll = true;
    renderRelMenu(kind);
    return;
  }
  if (action === "rel-pick-item") {
    e.preventDefault();
    toggleRelPick(act.getAttribute("data-kind"), act.getAttribute("data-id"), act.getAttribute("data-label"));
    return;
  }
  if (action === "rel-chip-remove") {
    e.preventDefault();
    toggleRelPick(act.getAttribute("data-kind"), act.getAttribute("data-id"), "");
    return;
  }
  if (action === "crel-pick-toggle") {
    e.preventDefault();
    toggleCrelMenu();
    return;
  }
  if (action === "crel-pick-clear") {
    e.preventDefault();
    const input = $("#cvCrelQuery");
    if (input) {
      input.value = "";
      input.focus();
    }
    crelPicker.showAll = false;
    crelPicker.hits = [];
    crelPicker.query = "";
    crelPicker.seq += 1;
    syncCrelClear();
    closeCrelPicker();
    return;
  }
  if (action === "crel-pick-more") {
    e.preventDefault();
    crelPicker.showAll = true;
    renderCrelMenu();
    return;
  }
  if (action === "crel-pick-item") {
    e.preventDefault();
    toggleCrelPick(act.getAttribute("data-id"), act.getAttribute("data-label"), act.getAttribute("data-type"));
    return;
  }
  if (action === "crel-chip-remove") {
    e.preventDefault();
    toggleCrelPick(act.getAttribute("data-id"), "", "");
    return;
  }
  const inFacet = $("#labelFacetPick") && $("#labelFacetPick").contains(e.target);
  const inColl = $("#cvCollPick") && $("#cvCollPick").contains(e.target);
  const inRel = e.target.closest("[data-rel-kind]");
  const inCrel = $("#cvCrelPick") && $("#cvCrelPick").contains(e.target);
  if (!inFacet) closeLabelFacetPicker();
  if (!inColl) closeCvCollPicker();
  if (!inRel) closeAllRelPickers();
  if (!inCrel) closeCrelPicker();
});
document.addEventListener("input", (e) => {
  if (!e.target) return;
  if (e.target.id === "labelFacetQuery") {
    labelFacetPicker.showAll = false;
    syncLabelFacetClear();
    if (e.target.value.trim()) openLabelFacetMenu(false);
    else closeLabelFacetPicker();
    return;
  }
  if (e.target.id === "cvCollQuery") {
    cvCollPicker.showAll = false;
    syncCvCollClear();
    if (e.target.value.trim()) openCvCollMenu(false);
    else closeCvCollPicker();
    return;
  }
  if (e.target.id === "cvCrelQuery") {
    scheduleCrelSearch();
    return;
  }
  const relKind = relKindFromQueryId(e.target.id);
  if (relKind) scheduleRelSearch(relKind);
});
document.addEventListener("focusin", (e) => {
  if (e.target && e.target.id === "labelFacetQuery") {
    const combo = $("#labelFacetCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) openLabelFacetMenu(false);
  }
  if (e.target && e.target.id === "cvCollQuery") {
    const combo = $("#cvCollCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) openCvCollMenu(false);
  }
  if (e.target && e.target.id === "cvCrelQuery") {
    const combo = $("#cvCrelCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) scheduleCrelSearch();
  }
  const relKind = e.target && relKindFromQueryId(e.target.id);
  if (relKind) {
    const combo = relEls(relKind).combo;
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) scheduleRelSearch(relKind);
  }
});
document.addEventListener("focusout", (e) => {
  if (e.target && e.target.id === "labelFacetQuery") {
    const combo = $("#labelFacetCombo");
    requestAnimationFrame(() => {
      const pick = $("#labelFacetPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
  if (e.target && e.target.id === "cvCollQuery") {
    const combo = $("#cvCollCombo");
    requestAnimationFrame(() => {
      const pick = $("#cvCollPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
  if (e.target && e.target.id === "cvCrelQuery") {
    const combo = $("#cvCrelCombo");
    requestAnimationFrame(() => {
      const pick = $("#cvCrelPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
  const relKind = e.target && relKindFromQueryId(e.target.id);
  if (relKind) {
    const els = relEls(relKind);
    requestAnimationFrame(() => {
      if (els.combo && els.pick && !els.pick.contains(document.activeElement)) {
        els.combo.classList.remove("is-focused");
      }
    });
  }
});
document.addEventListener("keydown", (e) => {
  if (!e.target) return;
  if (imgGallery.lightbox) {
    if (e.key === "Escape") {
      e.preventDefault();
      e.stopPropagation();
      closeImgLightbox();
      return;
    }
    if (e.key === "ArrowLeft") {
      e.preventDefault();
      showImgAt(imgGallery.index - 1, true);
      return;
    }
    if (e.key === "ArrowRight") {
      e.preventDefault();
      showImgAt(imgGallery.index + 1, true);
      return;
    }
  }
  if (gpsMap.lightbox) {
    if (e.key === "Escape") {
      e.preventDefault();
      e.stopPropagation();
      closeGpsLightbox();
      return;
    }
    if (e.key === "ArrowLeft") {
      e.preventDefault();
      selectGpsPoint(gpsMap.index - 1);
      return;
    }
    if (e.key === "ArrowRight") {
      e.preventDefault();
      selectGpsPoint(gpsMap.index + 1);
      return;
    }
  }
  if (e.target.id === "labelFacetQuery") {
    const menu = $("#labelFacetMenu");
    const open = menu && !menu.hidden;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!open) openLabelFacetMenu(!!e.target.value.trim() ? false : true);
      else setLabelFacetAcIdx(Math.min(labelFacetPicker.active + 1, Math.max(shownLabelFacetRows().length - 1, -1)));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setLabelFacetAcIdx(Math.max(labelFacetPicker.active - 1, -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      if (open && shownLabelFacetRows().length) pickActiveLabelFacet();
    } else if (e.key === "Escape") {
      if (open) {
        e.preventDefault();
        closeLabelFacetPicker();
      }
    }
    return;
  }
  if (e.target.id === "cvCrelQuery") {
    const menu = $("#cvCrelMenu");
    const open = menu && !menu.hidden;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!open) {
        if (e.target.value.trim()) scheduleCrelSearch();
        else {
          crelPicker.hits = [];
          crelPicker.query = "";
          openCrelMenu();
        }
      } else {
        setCrelAcIdx(Math.min(crelPicker.active + 1, Math.max(shownCrelRows().length - 1, -1)));
      }
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setCrelAcIdx(Math.max(crelPicker.active - 1, -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      if (open && shownCrelRows().length) pickActiveCrel();
    } else if (e.key === "Escape") {
      if (open) {
        e.preventDefault();
        closeCrelPicker();
      }
    }
    return;
  }
  const relKind = relKindFromQueryId(e.target.id);
  if (relKind) {
    const els = relEls(relKind);
    const open = els.menu && !els.menu.hidden;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!open) {
        if (e.target.value.trim()) scheduleRelSearch(relKind);
        else {
          relPickers[relKind].hits = [];
          relPickers[relKind].query = "";
          openRelMenu(relKind);
        }
      } else {
        setRelAcIdx(relKind, Math.min(relPickers[relKind].active + 1, Math.max(shownRelRows(relKind).length - 1, -1)));
      }
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setRelAcIdx(relKind, Math.max(relPickers[relKind].active - 1, -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      if (open && shownRelRows(relKind).length) pickActiveRel(relKind);
    } else if (e.key === "Escape") {
      if (open) {
        e.preventDefault();
        closeRelPicker(relKind);
      }
    }
    return;
  }
  if (e.target.id !== "cvCollQuery") return;
  const menu = $("#cvCollMenu");
  const open = menu && !menu.hidden;
  if (e.key === "ArrowDown") {
    e.preventDefault();
    if (!open) openCvCollMenu(!!e.target.value.trim() ? false : true);
    else setCvCollAcIdx(Math.min(cvCollPicker.active + 1, Math.max(shownCvCollRows().length - 1, -1)));
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    setCvCollAcIdx(Math.max(cvCollPicker.active - 1, -1));
  } else if (e.key === "Enter") {
    e.preventDefault();
    e.stopPropagation();
    if (open && shownCvCollRows().length) pickActiveCvColl();
  } else if (e.key === "Escape") {
    if (open) {
      e.preventDefault();
      closeCvCollPicker();
    }
  }
});
if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", bindConceptCardOrder);
else bindConceptCardOrder();
if (window.faces && faces.ajax) {
  faces.ajax.addOnEvent(function (data) {
    if (data.status === "begin") lockFicheScroll(data.source);
    if (data.status === "success") {
      applyConceptCardOrder();
      applyConceptLabelUi(data.source);
      restoreFicheScroll();
    }
  });
} else if (window.jsf && jsf.ajax) {
  jsf.ajax.addOnEvent(function (data) {
    if (data.status === "begin") lockFicheScroll(data.source);
    if (data.status === "success") {
      applyConceptCardOrder();
      applyConceptLabelUi(data.source);
      restoreFicheScroll();
    }
  });
}
