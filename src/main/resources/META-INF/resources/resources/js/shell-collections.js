/** Fiche — sélecteur de collections. */
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
  cvCollPicker._load = fetch(ctx + "/api/collection-tree/list?" + params.toString(), {
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
  if ($("#cvRelEditor")) {
    seedConceptRelFromHidden();
    return;
  }
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

