/** Brouillon facette. */
var fctTrState = { order: [], values: {}, alts: {}, confirm: "" };
var fctNoteState = { notes: [], tabs: [], lang: "", compare: false, typeOpen: false };
var fctMemState = { items: [], hits: [], seq: 0, timer: null };

function fctTrOpt(code) {
  return $("#fctTrMenu") && $("#fctTrMenu").querySelector('.tree-lang-opt[data-lang="' + code + '"]');
}

function fctTrMeta(code) {
  const opt = fctTrOpt(code);
  return {
    code: code,
    flag: (opt && (opt.getAttribute("data-flag") || "").trim()) || "",
    name: (opt && (opt.getAttribute("data-name") || "").trim()) || code
  };
}

function closeFacetDraftTrLang() {
  const btn = $("#fctTrBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function setFacetDraftTrLangOpen(open) {
  const btn = $("#fctTrBtn");
  if (!btn) return;
  btn.classList.toggle("is-open", !!open);
  btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function syncFacetDraftTrHidden() {
  const el = $("#fctTrPayload");
  if (!el) return;
  el.value = fctTrState.order.map((code) => {
    return code + "\t" + String(fctTrState.values[code] || "").replace(/\r?\n/g, " ");
  }).join("\n");
}

function resetFacetDraftTr() {
  fctTrState = { order: [], values: {}, alts: {}, confirm: "" };
  const el = $("#fctTrPayload");
  if (el) el.value = "";
  closeFacetDraftTrLang();
  paintFacetDraftTr();
}

function addFacetDraftTr(code) {
  if (!code || trLangAlready(fctTrState.order, code)) return;
  fctTrState.order.push(code);
  if (fctTrState.values[code] == null) fctTrState.values[code] = "";
  fctTrState.confirm = "";
  closeFacetDraftTrLang();
  syncFacetDraftTrHidden();
  paintFacetDraftTr();
  requestAnimationFrame(() => {
    const input = $("#fctTrVal-" + code);
    if (input) input.focus();
  });
}

function askRemoveFacetDraftTr(code) {
  fctTrState.confirm = code || "";
  paintFacetDraftTr();
}

function keepFacetDraftTr() {
  fctTrState.confirm = "";
  paintFacetDraftTr();
}

function removeFacetDraftTr(code) {
  fctTrState.order = fctTrState.order.filter((item) => item !== code);
  fctTrState.values[code] = "";
  fctTrState.alts[code] = "";
  fctTrState.confirm = "";
  syncFacetDraftTrHidden();
  paintFacetDraftTr();
}

function setFacetDraftTrValue(code, value) {
  fctTrState.values[code] = value;
  syncFacetDraftTrHidden();
}

function setFacetDraftTrAlt(code, value) {
  fctTrState.alts[code] = value;
}

function paintFacetDraftTr() {
  const list = $("#fctTrList");
  const empty = $("#fctTrEmpty");
  const editor = $("#fctTrEditor");
  if (!list || !editor) return;
  const ph = editor.getAttribute("data-ph") || "Nom en {0}…";
  const remove = editor.getAttribute("data-remove") || "";
  const drop = editor.getAttribute("data-drop") || "";
  const no = editor.getAttribute("data-no") || "Non";
  const yes = editor.getAttribute("data-yes") || "Oui";
  if (empty) empty.hidden = fctTrState.order.length > 0;
  list.innerHTML = fctTrState.order.map((code) => {
    const meta = fctTrMeta(code);
    const placeholder = escapeHtml(ph.replace("{0}", String(meta.name || code).toLowerCase()));
    const confirming = fctTrState.confirm === code;
    const actions = confirming
      ? '<span class="te-confirm">' + escapeHtml(drop)
        + '<button type="button" class="bo-btn ghost sm" data-act="fct-tr-keep">' + escapeHtml(no) + "</button>"
        + '<button type="button" class="bo-btn primary sm" data-act="fct-tr-drop" data-lang="' + code + '">' + escapeHtml(yes) + "</button>"
        + "</span>"
      : '<button type="button" class="re-x te-x" data-act="fct-tr-ask" data-lang="' + code
        + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>';
    return '<div class="te-lang">'
      + '<div class="te-head">'
      + (meta.flag ? '<span class="tr-flag">' + meta.flag + "</span>" : "")
      + '<span class="te-name">' + escapeHtml(meta.name) + "</span>"
      + '<span class="te-code">' + escapeHtml(code) + "</span>"
      + actions
      + "</div>"
      + '<input type="text" class="st-input" id="fctTrVal-' + code + '" lang="' + code + '" data-lang="' + code
      + '" value="' + escapeHtml(fctTrState.values[code] || "") + '" placeholder="' + placeholder + '" autocomplete="off"/>'
      + "</div>";
  }).join("");
  markUsedTrLangOpts("#fctTrMenu", fctTrState.order, editor.getAttribute("data-used") || "");
  const pick = $("#fctTrPick");
  if (pick) pick.hidden = $$("#fctTrMenu .tree-lang-opt").length === 0;
}

function fctNoteTypes() {
  return $$("#fctNoteTypeOpts [data-code]").map((el) => ({
    code: el.getAttribute("data-code") || "",
    label: el.getAttribute("data-label") || el.getAttribute("data-code") || ""
  })).filter((item) => item.code);
}

function fctNoteLangs() {
  return $$("#fctNoteLangOpts [data-code]").map((el) => ({
    code: el.getAttribute("data-code") || "",
    name: el.getAttribute("data-name") || el.getAttribute("data-code") || "",
    flag: el.getAttribute("data-flag") || ""
  })).filter((item) => item.code);
}

function fctNoteWorkLang() {
  const editor = $("#fctNoteEditor");
  const langs = fctNoteLangs();
  return (editor && editor.getAttribute("data-work-lang")) || (langs[0] && langs[0].code) || "fr";
}

function fctNoteMeta(code) {
  const langs = fctNoteLangs();
  return langs.find((item) => String(item.code).toLowerCase() === String(code || "").toLowerCase())
    || { code: code || "", name: code || "", flag: "" };
}

function fctNoteNorm(code) {
  return String(code || "").trim().toLowerCase();
}

function fctNoteFilled(note) {
  return !!(note && String(note.value || "").trim());
}

function fctNoteTabs() {
  const seen = {};
  const tabs = [];
  function add(code) {
    const key = fctNoteNorm(code);
    if (!key || seen[key]) return;
    seen[key] = true;
    tabs.push(code);
  }
  add(fctNoteWorkLang());
  fctNoteState.notes.forEach((note) => add(note.lang));
  (fctNoteState.tabs || []).forEach((code) => add(code));
  return tabs;
}

function fctNoteOfLang(lang) {
  const key = fctNoteNorm(lang);
  return fctNoteState.notes
    .map((note, index) => ({ note: note, index: index }))
    .filter((item) => fctNoteNorm(item.note.lang) === key);
}

function fctNoteUsedTypes(lang, exceptIndex) {
  const used = {};
  fctNoteOfLang(lang).forEach((item) => {
    if (item.index === exceptIndex) return;
    if (item.note.type) used[item.note.type] = true;
  });
  return used;
}

function syncFacetDraftNotesHidden() {
  const el = $("#fctNotesPayload");
  if (!el) return;
  el.value = fctNoteState.notes.filter(fctNoteFilled).map((note) => {
    return [note.type || "", note.lang || "", encodeURIComponent(note.value || ""), encodeURIComponent(note.source || "")].join("\t");
  }).join("\n");
}

function resetFacetDraftNotes() {
  fctNoteState = { notes: [], tabs: [], lang: fctNoteWorkLang(), compare: false, typeOpen: false };
  const el = $("#fctNotesPayload");
  if (el) el.value = "";
  paintFacetDraftNotes();
}

function closeFacetDraftNoteLang() {
  const btn = $("#fctNoteBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function setFacetDraftNoteLangOpen(open) {
  const btn = $("#fctNoteBtn");
  if (!btn) return;
  btn.classList.toggle("is-open", !!open);
  btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function closeFacetDraftNoteTypeMenu() {
  if (!fctNoteState.typeOpen) return false;
  fctNoteState.typeOpen = false;
  const menu = $("#fctNoteTypeMenu");
  if (menu) menu.remove();
  return true;
}

function setFacetDraftNoteLang(code) {
  if (!code) return;
  fctNoteState.lang = code;
  fctNoteState.typeOpen = false;
  closeFacetDraftNoteLang();
  paintFacetDraftNotes();
}

function addFacetDraftNoteLang(code) {
  if (!code) return;
  if (!trLangAlready((fctNoteState.tabs || []).concat(fctNoteTabs()), code)) {
    fctNoteState.tabs.push(code);
  }
  fctNoteState.lang = code;
  fctNoteState.typeOpen = false;
  closeFacetDraftNoteLang();
  syncFacetDraftNotesHidden();
  paintFacetDraftNotes();
}

function toggleFacetDraftNoteCompare() {
  fctNoteState.compare = !fctNoteState.compare;
  fctNoteState.typeOpen = false;
  paintFacetDraftNotes();
}

function addFacetDraftNote() {
  const lang = fctNoteState.lang || fctNoteWorkLang();
  const free = fctNoteTypes().filter((type) => !fctNoteUsedTypes(lang, -1)[type.code]);
  if (!free.length) return;
  if (free.length === 1) {
    addFacetDraftNoteType(free[0].code);
    return;
  }
  fctNoteState.typeOpen = !fctNoteState.typeOpen;
  paintFacetDraftNotes();
}

function addFacetDraftNoteType(type) {
  const lang = fctNoteState.lang || fctNoteWorkLang();
  if (!type || fctNoteUsedTypes(lang, -1)[type]) {
    fctNoteState.typeOpen = false;
    paintFacetDraftNotes();
    return;
  }
  fctNoteState.notes.push({ type: type, lang: lang, value: "", source: "" });
  fctNoteState.typeOpen = false;
  syncFacetDraftNotesHidden();
  paintFacetDraftNotes();
  requestAnimationFrame(() => {
    const input = $("#fctNoteVal-" + (fctNoteState.notes.length - 1));
    if (input) input.focus();
  });
}

function removeFacetDraftNote(index) {
  fctNoteState.notes.splice(index, 1);
  fctNoteState.typeOpen = false;
  syncFacetDraftNotesHidden();
  paintFacetDraftNotes();
}

function setFacetDraftNoteField(index, field, value) {
  const note = fctNoteState.notes[index];
  if (!note || !field) return;
  if (field === "type" && fctNoteUsedTypes(note.lang, index)[value]) return;
  note[field] = value;
  syncFacetDraftNotesHidden();
  if (field === "type") paintFacetDraftNotes();
}

function fctNoteCompareLang() {
  const current = fctNoteNorm(fctNoteState.lang);
  const work = fctNoteNorm(fctNoteWorkLang());
  if (current !== work) return fctNoteWorkLang();
  return fctNoteTabs().find((code) => fctNoteNorm(code) !== current) || "";
}

function paintFacetDraftNoteColHead(lang) {
  const meta = fctNoteMeta(lang);
  return '<div class="note-ed-colh">'
    + (meta.flag ? '<span class="tr-flag">' + meta.flag + "</span>" : "")
    + "<span>" + escapeHtml(meta.name || lang) + "</span>"
    + '<span class="te-code">' + escapeHtml(lang) + "</span>"
    + "</div>";
}

function paintFacetDraftNoteGhost(type) {
  const label = ((fctNoteTypes().find((item) => item.code === type) || {}).label) || type || "";
  return '<div class="note-ghost"><span class="note-ghost-t">' + escapeHtml(label) + "</span></div>";
}

function paintFacetDraftNoteAdd(lang) {
  const editor = $("#fctNoteEditor");
  const add = (editor && editor.getAttribute("data-add")) || "";
  const free = fctNoteTypes().filter((type) => !fctNoteUsedTypes(lang, -1)[type.code]);
  if (!free.length) return "";
  const menu = fctNoteState.typeOpen
    ? '<div class="note-menu" id="fctNoteTypeMenu">'
      + free.map((type) => '<button type="button" data-act="fct-note-add-type" data-type="'
        + escapeHtml(type.code) + '">' + escapeHtml(type.label) + "</button>").join("")
      + "</div>"
    : "";
  return '<div class="note-menu-w">'
    + '<button type="button" class="note-ed-add" data-act="fct-note-add">' + escapeHtml(add) + "</button>"
    + menu
    + "</div>";
}

function paintFacetDraftNoteCard(item, editable) {
  const editor = $("#fctNoteEditor");
  const remove = (editor && editor.getAttribute("data-remove")) || "";
  const textPh = (editor && editor.getAttribute("data-text-ph")) || "";
  const sourcePh = (editor && editor.getAttribute("data-source-ph")) || "";
  const note = item.note;
  const dir = typeof trTextDir === "function" ? trTextDir(note.lang) : "ltr";
  const used = fctNoteUsedTypes(note.lang, item.index);
  const typeOpts = fctNoteTypes()
    .filter((type) => type.code === note.type || !used[type.code])
    .map((type) => '<option value="' + escapeHtml(type.code) + '"' + (type.code === note.type ? " selected" : "") + ">"
      + escapeHtml(type.label) + "</option>").join("");
  const typeLabel = (fctNoteTypes().find((type) => type.code === note.type) || { label: note.type }).label;
  if (!editable) {
    const empty = !fctNoteFilled(note);
    return '<div class="note-ref' + (empty ? " is-empty" : "") + '">'
      + '<div class="note-c-h"><span class="note-c-type-lbl">' + escapeHtml(typeLabel) + "</span></div>"
      + (empty
        ? ""
        : '<p class="cv-def" lang="' + escapeHtml(note.lang) + '" dir="' + dir + '">' + escapeHtml(note.value) + "</p>"
          + (note.source ? '<span class="note-src">' + escapeHtml(note.source) + "</span>" : ""))
      + "</div>";
  }
  return '<div class="note-c">'
    + '<div class="note-c-h">'
    + '<select class="note-c-type" data-fct-note="type" data-index="' + item.index + '" aria-label="Type">'
    + typeOpts + "</select>"
    + '<input type="text" class="note-c-src" data-fct-note="source" data-index="' + item.index
    + '" value="' + escapeHtml(note.source || "") + '" placeholder="' + escapeHtml(sourcePh) + '" autocomplete="off"/>'
    + '<button type="button" class="note-ed-x" data-act="fct-note-remove" data-index="' + item.index
    + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
    + "</div>"
    + '<textarea class="disc-input" id="fctNoteVal-' + item.index + '" data-fct-note="value" data-index="' + item.index
    + '" rows="3" lang="' + escapeHtml(note.lang) + '" dir="' + dir + '" placeholder="' + escapeHtml(textPh) + '">'
    + escapeHtml(note.value || "") + "</textarea>"
    + "</div>";
}

function paintFacetDraftNoteColumn(lang, editable) {
  const items = fctNoteOfLang(lang);
  let html = items.map((item) => paintFacetDraftNoteCard(item, editable)).join("");
  if (editable) html += paintFacetDraftNoteAdd(lang);
  return html;
}

function paintFacetDraftNoteCompare(leftLang, rightLang) {
  const typeOrder = fctNoteTypes().map((type) => type.code);
  const leftItems = fctNoteOfLang(leftLang);
  const rightItems = fctNoteOfLang(rightLang);
  const seen = {};
  const types = [];
  function addType(code) {
    if (!code || seen[code]) return;
    seen[code] = true;
    types.push(code);
  }
  typeOrder.forEach(addType);
  leftItems.forEach((item) => addType(item.note.type));
  rightItems.forEach((item) => addType(item.note.type));
  const shown = types.filter((code) =>
    leftItems.some((item) => item.note.type === code)
    || rightItems.some((item) => item.note.type === code)
  );
  let html = '<div class="note-cmp-h">'
    + paintFacetDraftNoteColHead(leftLang)
    + paintFacetDraftNoteColHead(rightLang)
    + "</div>";
  if (!shown.length) {
    const editor = $("#fctNoteEditor");
    const empty = (editor && editor.getAttribute("data-empty")) || "";
    html += '<div class="note-cmp-r">'
      + '<div class="note-ghost"><span class="note-ghost-t">' + escapeHtml(empty) + "</span></div>"
      + '<div class="note-ghost"><span class="note-ghost-t">' + escapeHtml(empty) + "</span></div>"
      + "</div>";
  } else {
    html += shown.map((type) => {
      const left = leftItems.find((item) => item.note.type === type);
      const right = rightItems.find((item) => item.note.type === type);
      return '<div class="note-cmp-r">'
        + (left ? paintFacetDraftNoteCard(left, true) : paintFacetDraftNoteGhost(type))
        + (right ? paintFacetDraftNoteCard(right, false) : paintFacetDraftNoteGhost(type))
        + "</div>";
    }).join("");
  }
  html += paintFacetDraftNoteAdd(leftLang);
  return html;
}

function paintFacetDraftNotes() {
  const tabsEl = $("#fctNoteTabs");
  const body = $("#fctNoteBody");
  const editor = $("#fctNoteEditor");
  if (!tabsEl || !body || !editor) return;
  const tabs = fctNoteTabs();
  if (!fctNoteState.lang && tabs[0]) fctNoteState.lang = tabs[0];
  tabsEl.innerHTML = tabs.map((code) => {
    const meta = fctNoteMeta(code);
    const count = fctNoteOfLang(code).filter((item) => fctNoteFilled(item.note)).length;
    const on = fctNoteNorm(code) === fctNoteNorm(fctNoteState.lang);
    return '<button type="button" class="note-ed-tab' + (on ? " on" : "") + '" data-act="fct-note-tab" data-lang="'
      + escapeHtml(code) + '" role="tab" aria-selected="' + (on ? "true" : "false") + '">'
      + (meta.flag ? '<span class="tr-flag">' + meta.flag + "</span>" : "")
      + "<span>" + escapeHtml(meta.name || code) + "</span>"
      + (count ? '<span class="note-ed-n">' + count + "</span>" : '<span class="note-ed-void" aria-hidden="true"></span>')
      + "</button>";
  }).join("");
  const cmpBtn = $("#fctNoteCmp");
  if (cmpBtn) {
    cmpBtn.classList.toggle("on", !!fctNoteState.compare);
    cmpBtn.hidden = tabs.length < 2;
  }
  const cmpLang = fctNoteState.compare ? fctNoteCompareLang() : "";
  if (fctNoteState.compare && cmpLang) {
    body.className = "note-cmp";
    body.innerHTML = paintFacetDraftNoteCompare(fctNoteState.lang, cmpLang);
  } else {
    body.className = "note-ed-mono";
    body.innerHTML = paintFacetDraftNoteColumn(fctNoteState.lang, true);
  }
  if (typeof markUsedTrLangOpts === "function") {
    markUsedTrLangOpts("#fctNoteMenu", tabs, editor.getAttribute("data-used") || "");
  }
  const pick = $("#fctNotePick");
  if (pick) pick.hidden = $$("#fctNoteMenu .tree-lang-opt").length === 0;
}

function syncFacetDraftMemHidden() {
  const el = $("#fctMembersPayload");
  if (!el) return;
  el.value = fctMemState.items.map((item) => (item.id || "") + "\t" + (item.label || "")).join("\n");
}

function resetFacetDraftMem() {
  fctMemState = { items: [], hits: [], seq: 0, timer: null };
  const el = $("#fctMembersPayload");
  if (el) el.value = "";
  const q = $("#fctMemQ");
  if (q) q.value = "";
  hideFacetDraftMemDrop();
  paintFacetDraftMem();
}

function hideFacetDraftMemDrop() {
  const drop = $("#fctMemDrop");
  const empty = $("#fctMemEmpty");
  if (drop) {
    drop.hidden = true;
    drop.innerHTML = "";
  }
  if (empty) empty.hidden = true;
}

function setFacetDraftMemMsg(text) {
  const el = $("#fctMemMsg");
  if (!el) return;
  el.textContent = text || "";
  el.hidden = !text;
}

function facetDraftMemAlready(id, label) {
  return fctMemState.items.some((item) => item.id === id || (label && item.label === label));
}

function addFacetDraftMem(id, label) {
  const editor = $("#fctMemEditor");
  if (!id) return;
  if (facetDraftMemAlready(id, label)) {
    setFacetDraftMemMsg((editor && editor.getAttribute("data-exists")) || "");
    return;
  }
  fctMemState.items.push({ id: id, label: label || id });
  const q = $("#fctMemQ");
  if (q) q.value = "";
  fctMemState.hits = [];
  hideFacetDraftMemDrop();
  setFacetDraftMemMsg("");
  syncFacetDraftMemHidden();
  paintFacetDraftMem();
}

function removeFacetDraftMem(index) {
  fctMemState.items.splice(Number(index), 1);
  syncFacetDraftMemHidden();
  paintFacetDraftMem();
}

function paintFacetDraftMem() {
  const rows = $("#fctMemRows");
  const editor = $("#fctMemEditor");
  if (!rows || !editor) return;
  const remove = editor.getAttribute("data-remove") || "";
  rows.innerHTML = fctMemState.items.map((item, index) => (
    '<div class="re-row">'
    + '<span class="re-lbl">' + escapeHtml(item.label || item.id) + "</span>"
    + '<button type="button" class="re-x" data-act="fct-mem-remove" data-index="' + index
    + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
    + "</div>"
  )).join("");
}

function searchFacetDraftMem(q) {
  const seq = ++fctMemState.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    q: q
  });
  fetch(ctx + "/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== fctMemState.seq) return;
    fctMemState.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id) : "";
      const label = item && item.label ? String(item.label) : "";
      return id && !facetDraftMemAlready(id, label);
    }).slice(0, 6);
    paintFacetDraftMemHits(q);
  }).catch(() => {
    if (seq !== fctMemState.seq) return;
    fctMemState.hits = [];
    paintFacetDraftMemHits(q);
  });
}

function paintFacetDraftMemHits(q) {
  const drop = $("#fctMemDrop");
  const empty = $("#fctMemEmpty");
  if (!drop) return;
  if (fctMemState.hits.length) {
    drop.hidden = false;
    if (empty) empty.hidden = true;
    drop.innerHTML = fctMemState.hits.map((hit) => (
      '<button type="button" class="re-hit" data-act="fct-mem-hit" data-id="'
      + escapeHtml(hit.id || "") + '" data-label="' + escapeHtml(hit.label || hit.id || "") + '">'
      + '<span class="re-hit-l">' + escapeHtml(hit.label || hit.id || "") + "</span>"
      + "</button>"
    )).join("");
    return;
  }
  drop.hidden = true;
  drop.innerHTML = "";
  if (empty) empty.hidden = String(q || "").trim().length < 2;
}

function scheduleFacetDraftMemSearch() {
  const q = ($("#fctMemQ") && $("#fctMemQ").value.trim()) || "";
  setFacetDraftMemMsg("");
  if (fctMemState.timer) clearTimeout(fctMemState.timer);
  if (q.length < 2) {
    fctMemState.seq += 1;
    fctMemState.hits = [];
    hideFacetDraftMemDrop();
    return;
  }
  fctMemState.timer = setTimeout(() => searchFacetDraftMem(q), 220);
}

function showFacetDraftPanel() {
  state.home = false;
  state.draft = false;
  state.conceptDraft = false;
  state.facetDraft = true;
  state.collectionDraft = false;
  state.conceptId = null;
  if (state.view !== "arbo" && state.view !== "tableau" && state.view !== "recherche") {
    state.view = "arbo";
  }
  prepareFacetDraftForm();
  highlightConcept(null);
  closeSearchUi();
  showPanel(".view-panel", "viewFacetDraft");
  requestAnimationFrame(() => { const t = $("#fctTitle"); if (t) t.focus(); });
}

function createFacetDraft(src) {
  const parentName = ((src && src.getAttribute("data-pref")) || "").trim();
  const parentId = ((src && src.getAttribute("data-id")) || "").trim();
  if (!parentId) return;
  if (!$("#viewFacetDraft")) {
    const q = new URLSearchParams({ create: "facet" });
    if (parentName) q.set("pref", parentName);
    q.set("idc", parentId);
    go(conceptPageHref(q.toString()));
    return;
  }
  const idEl = $("#fctParentId");
  const labEl = $("#fctParentLabel");
  if (idEl) idEl.value = parentId;
  if (labEl) labEl.value = parentName;
  showFacetDraftPanel();
  const go = $("#fctPrepare");
  if (go) go.click();
}

function onFacetDraftPrepare(data) {
  if (data.status !== "success") return;
  showFacetDraftPanel();
}
window.onFacetDraftPrepare = onFacetDraftPrepare;

function hideFacetDraftCreateConfirm() {
  const dlg = $("#fctCreateConfirm");
  if (dlg) dlg.hidden = true;
}

function hideFacetDraftLeave() {
  const dlg = $("#fctLeaveConfirm");
  if (dlg) dlg.hidden = true;
}

function requestFacetDraftCreate() {
  const title = $("#fctTitle");
  if (!title || !String(title.value || "").trim()) {
    setFacetDraftTitleError(true);
    if (title) title.focus();
    return;
  }
  setFacetDraftTitleError(false);
  const root = facetDraftRoot();
  const dlg = $("#fctCreateConfirm");
  const text = $("#fctCreateText");
  const go = $("#fctCreateGo");
  if (dlg && root) {
    const name = String(title.value || "").trim();
    const tpl = root.getAttribute("data-confirm-create") || "";
    if (text) text.textContent = tpl.replace("{0}", name);
    if (go) go.textContent = root.getAttribute("data-confirm-go") || go.textContent;
    dlg.hidden = false;
    return;
  }
  confirmFacetDraftCreate();
}

function confirmFacetDraftCreate() {
  hideFacetDraftCreateConfirm();
  if (typeof syncFacetDraftNotesHidden === "function") syncFacetDraftNotesHidden();
  if (typeof syncFacetDraftTrHidden === "function") syncFacetDraftTrHidden();
  if (typeof syncFacetDraftMemHidden === "function") syncFacetDraftMemHidden();
  const createBtn = $("#fctCreate");
  if (createBtn) createBtn.click();
}

function requestFacetDraftLeave() {
  const dlg = $("#fctLeaveConfirm");
  if (facetDraftDirty() && dlg) {
    dlg.hidden = false;
    return;
  }
  resolveFacetDraft("annulé");
}

function resolveFacetDraft(kind) {
  if (kind === "annulé") {
    hideFacetDraftLeave();
    const cancelBtn = $("#fctCancel");
    if (cancelBtn) {
      cancelBtn.click();
      return;
    }
  }
  resolveFacetDraftUi(kind);
}

function onFacetDraftAjax(data) {
  if (data.status !== "success") return;
  const result = $("#fctResult");
  if (!facetDraftFlagOn(result, "data-created")) {
    const title = $("#fctTitle");
    if (title && !String(title.value || "").trim()) setFacetDraftTitleError(true);
    return;
  }
  const root = facetDraftRoot();
  const msg = root && root.getAttribute("data-created-msg");
  if (msg && typeof toast === "function") toast(msg, { soft: true });
  const createdId = result && result.getAttribute("data-id");
  if (createdId && typeof selectOpenedInTree === "function") {
    selectOpenedInTree(createdId);
  }
  resolveFacetDraftUi("créé");
  if (createdId && typeof openLiveDetail === "function") {
    openLiveDetail(createdId, "facet");
  }
}
window.onFacetDraftAjax = onFacetDraftAjax;

function resolveFacetDraftUi(kind) {
  hideFacetDraftLeave();
  hideFacetDraftCreateConfirm();
  state.facetDraft = false;
  paint();
}
if (typeof window !== "undefined") {
  window.resolveFacetDraftUi = resolveFacetDraftUi;
  window.syncFacetDraftPref = syncFacetDraftPref;
  window.setFacetDraftMore = setFacetDraftMore;
  window.setFacetDraftTrValue = setFacetDraftTrValue;
  window.setFacetDraftTrAlt = setFacetDraftTrAlt;
  window.setFacetDraftNoteField = setFacetDraftNoteField;
  window.askRemoveFacetDraftTr = askRemoveFacetDraftTr;
  window.keepFacetDraftTr = keepFacetDraftTr;
  window.removeFacetDraftTr = removeFacetDraftTr;
  window.setFacetDraftTrLangOpen = setFacetDraftTrLangOpen;
  window.addFacetDraftTr = addFacetDraftTr;
  window.closeFacetDraftTrLang = closeFacetDraftTrLang;
  window.addFacetDraftNote = addFacetDraftNote;
  window.addFacetDraftNoteType = addFacetDraftNoteType;
  window.addFacetDraftNoteLang = addFacetDraftNoteLang;
  window.removeFacetDraftNote = removeFacetDraftNote;
  window.setFacetDraftNoteLangOpen = setFacetDraftNoteLangOpen;
  window.setFacetDraftNoteLang = setFacetDraftNoteLang;
  window.toggleFacetDraftNoteCompare = toggleFacetDraftNoteCompare;
  window.closeFacetDraftNoteLang = closeFacetDraftNoteLang;
  window.closeFacetDraftNoteTypeMenu = closeFacetDraftNoteTypeMenu;
  window.addFacetDraftMem = addFacetDraftMem;
  window.removeFacetDraftMem = removeFacetDraftMem;
  window.hideFacetDraftMemDrop = hideFacetDraftMemDrop;
  window.scheduleFacetDraftMemSearch = scheduleFacetDraftMemSearch;
  window.createFacetDraft = createFacetDraft;
  window.requestFacetDraftCreate = requestFacetDraftCreate;
  window.confirmFacetDraftCreate = confirmFacetDraftCreate;
  window.requestFacetDraftLeave = requestFacetDraftLeave;
  window.resolveFacetDraft = resolveFacetDraft;
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
  const hasPanel = root.querySelector(
      `.bo-panel[data-obj="${CSS.escape(obj)}"][data-op="${CSS.escape(op)}"], .bo-panel[data-obj="${CSS.escape(obj)}"]`
  );
  if (!hasPanel) {
    const file = location.pathname.indexOf("actions-lot") >= 0 ? "actions-lot.xhtml" : "atelier.xhtml";
    const q = "obj=" + encodeURIComponent(obj) + (op ? "&op=" + encodeURIComponent(op) : "");
    if (typeof go === "function") go("toolbox/" + file + "?" + q);
    else location.href = "toolbox/" + file + "?" + q;
    return;
  }
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

