/** Fiche — ouverture, facettes de libellé et pastilles. */
/**
 * OpenTheso — vues, peinture, fiche, accueil.
 */
"use strict";

function closeSearchUi() {
  const menu = $("#searchModeMenu");
  const modeBtn = $("#searchModeBtn");
  if (menu) menu.hidden = true;
  if (modeBtn) {
    modeBtn.setAttribute("aria-expanded", "false");
    modeBtn.classList.remove("is-open");
  }
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

function openLabelFrom(el) {
  if (!el) return "";
  const text = el.querySelector(".tn-text");
  if (text && text.textContent.trim()) return text.textContent.trim();
  return (el.textContent || "").replace(/\s+/g, " ").trim();
}

function optimisticOpenLabel(id, label) {
  const given = (label || "").replace(/\s+/g, " ").trim();
  if (given) return given;
  if (!id || typeof CSS === "undefined" || !CSS.escape) return "";
  const text = document.querySelector('#panelTree .tn[data-id="' + CSS.escape(id) + '"] .tn-text');
  return text ? text.textContent.replace(/\s+/g, " ").trim() : "";
}

function paintOptimisticFiche(id, label) {
  const live = $("#viewLive");
  const title = optimisticOpenLabel(id, label);
  if (!live || !title) return;
  const heading = live.querySelector(".cv-pref .cv-pref-label")
    || live.querySelector(".cv-pref > span:not(.cv-lang):not(.cv-status):not(.cv-dot):not(.tn-lang)");
  if (!heading) return;
  heading.textContent = title;
  const nodeText = id && window.CSS && CSS.escape
    ? document.querySelector('#panelTree .tn[data-id="' + CSS.escape(id) + '"] .tn-text')
    : null;
  const fromSource = !!(nodeText && nodeText.classList.contains("is-source"));
  heading.classList.toggle("cv-source", fromSource);
  const lang = fromSource && nodeText.parentElement
    ? ((nodeText.parentElement.querySelector(".tn-lang") || {}).textContent || "").trim()
    : "";
  syncSourceLangBadge(heading, fromSource, lang);
  live.classList.add("is-optimistic");
}

function beginLiveOpen() {
  const live = $("#viewLive");
  if (!live) return;
  live.classList.add("is-loading");
  live.classList.remove("is-ready", "is-opening");
  live.setAttribute("aria-busy", "true");
  const bar = $("#liveProgress");
  if (bar) bar.setAttribute("aria-hidden", "false");
  showPanel(".view-panel", "viewLive");
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
  labelFacetPicker._load = fetch(ctx + "/api/facets?" + params.toString(), {
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

function termTagHidden(root) {
  return root && root.querySelector("input[type='hidden']");
}

function termTagValues(root) {
  const hidden = termTagHidden(root);
  if (!hidden || !hidden.value) return [];
  try {
    const parsed = JSON.parse(hidden.value);
    if (!Array.isArray(parsed)) return [];
    const seen = {};
    const values = [];
    parsed.forEach((item) => {
      const value = String(item == null ? "" : item).trim();
      if (!value || seen[value]) return;
      seen[value] = true;
      values.push(value);
    });
    return values;
  } catch (e) {
    return [];
  }
}

function writeTermTags(root, values) {
  if (!root) return;
  const hidden = termTagHidden(root);
  if (hidden) hidden.value = JSON.stringify(values);
  const chips = root.querySelector(".term-tags-chips");
  if (!chips) return;
  const hiddenKind = root.getAttribute("data-kind") === "hidden";
  chips.innerHTML = values.map((value, index) => {
    return '<span class="term-chip' + (hiddenKind ? " is-hidden" : "") + '">'
      + escapeHtml(value)
      + '<button type="button" data-act="term-tag-remove" data-index="' + index
      + '" title="Retirer" aria-label="Retirer">×</button></span>';
  }).join("");
}

function termFold(value) {
  return String(value || "").trim().toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function termTagMessage(root, text) {
  if (!root) return;
  let note = root.querySelector(".term-tags-dup");
  if (!text) {
    if (note) note.remove();
    return;
  }
  if (!note) {
    note = document.createElement("p");
    note.className = "term-tags-dup";
    note.setAttribute("role", "status");
    root.appendChild(note);
  }
  note.textContent = text;
}

function localTermDuplicate(root, value) {
  const key = termFold(value);
  if (!key) return false;
  const block = root.closest("section.cblock") || root;
  const lists = block.querySelectorAll(".term-tags");
  for (let i = 0; i < lists.length; i++) {
    const values = termTagValues(lists[i]);
    for (let j = 0; j < values.length; j++) {
      if (termFold(values[j]) === key) return true;
    }
  }
  const pref = block.querySelector("[id$='labelPrefInput']");
  return !!(pref && termFold(pref.value) === key);
}

function termTagLang(root) {
  const lang = root && root.getAttribute("data-lang");
  if (lang) return lang;
  return typeof thesaurusLang === "function" ? thesaurusLang() : "fr";
}

function labelAlreadyUsed(root, value) {
  const id = typeof thesaurusId === "function" ? thesaurusId() : "";
  if (!id || !value) return Promise.resolve(false);
  const ctx = document.body.getAttribute("data-ctx") || "";
  const query = new URLSearchParams({
    thesaurusId: id,
    lang: termTagLang(root),
    value: value,
    kind: (root && root.getAttribute("data-kind")) || ""
  });
  return fetch(ctx + "/api/concepts/label-duplicate?" + query.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => res.ok ? res.json() : { duplicate: false })
    .then((body) => !!(body && body.duplicate))
    .catch(() => false);
}

let termDupSerial = 0;

function addTermTag(root, raw) {
  const value = String(raw || "").trim();
  if (!root || !value) return false;
  if (localTermDuplicate(root, value)) {
    termTagMessage(root, "Cette forme est déjà utilisée sur ce concept.");
    return false;
  }
  const values = termTagValues(root);
  values.push(value);
  writeTermTags(root, values);
  termTagMessage(root, "");
  const token = String(++termDupSerial);
  root.setAttribute("data-dup-token", token);
  labelAlreadyUsed(root, value).then((used) => {
    if (!used || !root.isConnected || root.getAttribute("data-dup-token") !== token) return;
    const values = termTagValues(root);
    const at = values.indexOf(value);
    if (at < 0) return;
    values.splice(at, 1);
    writeTermTags(root, values);
    termTagMessage(root, "Un label identique existe déjà.");
  });
  return true;
}

function removeTermTag(root, index) {
  const values = termTagValues(root);
  const at = parseInt(index, 10);
  if (!root || !Number.isFinite(at) || at < 0 || at >= values.length) return;
  values.splice(at, 1);
  writeTermTags(root, values);
}

function bindTermTags() {
  document.querySelectorAll(".term-tags").forEach((root) => writeTermTags(root, termTagValues(root)));
}

function flushTermTagInputs() {
  document.querySelectorAll(".term-tags").forEach((root) => {
    const input = root.querySelector(".term-tags-add input");
    if (!input || !input.value.trim()) return;
    if (addTermTag(root, input.value)) input.value = "";
  });
}

function saveEditingLabelBlock(from) {
  const root = (from && from.closest && from.closest("#viewLive")) || document;
  const block = from && from.closest
      ? from.closest("section.cblock.is-editing[data-card-id='contexte'], section.cblock.is-editing[data-card-id='f-label'], section.cblock[data-card-id='branche']")
      : null;
  if (!block) return;
  flushTermTagInputs();
  const save = block.querySelector("[id$='labelEditSave'], [id$='fcLabelEditSave']")
      || root.querySelector("section.cblock.is-editing [id$='labelEditSave'], section.cblock.is-editing [id$='fcLabelEditSave']");
  if (save && !save.classList.contains("is-busy")) save.click();
}

document.addEventListener("keydown", (e) => {
  if (e.key !== "Enter" || e.repeat || e.isComposing || e.defaultPrevented) return;
  if (!e.target || !e.target.closest) return;
  const block = e.target.closest("section.cblock.is-editing[data-card-id='contexte'], section.cblock.is-editing[data-card-id='f-label']")
      || (document.querySelector("section.cblock.is-editing[data-card-id='contexte']")
          && e.target.closest("section.cblock[data-card-id='branche']"));
  if (!block) return;
  if (e.target.closest("a, button, textarea, .term-tags-add")) return;
  if (e.target.id === "labelFacetQuery") return;
  e.preventDefault();
  saveEditingLabelBlock(block);
});

document.addEventListener("keydown", (e) => {
  const input = e.target && e.target.closest && e.target.closest(".term-tags-add input");
  if (!input) return;
  if (e.key === "Enter") {
    e.preventDefault();
    e.stopPropagation();
    const root = input.closest(".term-tags");
    if (input.value.trim()) {
      if (addTermTag(root, input.value)) input.value = "";
      return;
    }
    termTagMessage(root, "");
    saveEditingLabelBlock(input);
    return;
  }
  termTagMessage(input.closest(".term-tags"), "");
  if (e.key === "Backspace" && !input.value) {
    const root = input.closest(".term-tags");
    const values = termTagValues(root);
    if (!values.length) return;
    e.preventDefault();
    removeTermTag(root, String(values.length - 1));
  }
}, true);
document.addEventListener("submit", (e) => {
  if (e.target && e.target.id === "previewDetailForm") flushTermTagInputs();
}, true);

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

