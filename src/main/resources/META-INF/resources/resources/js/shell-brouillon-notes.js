/** Brouillon concept — notes et enregistrement. */
var draftNoteState = { notes: [], openLang: -1 };

function draftNoteTypes() {
  return $$("#draftNoteTypeOpts [data-code]").map((el) => ({
    code: el.getAttribute("data-code") || "",
    label: el.getAttribute("data-label") || el.getAttribute("data-code") || ""
  })).filter((item) => item.code);
}

function draftNoteLangs() {
  return $$("#draftNoteLangOpts [data-code]").map((el) => ({
    code: el.getAttribute("data-code") || "",
    name: el.getAttribute("data-name") || el.getAttribute("data-code") || "",
    flag: el.getAttribute("data-flag") || ""
  })).filter((item) => item.code);
}

function draftNoteUsedKeys(exceptIndex) {
  const used = {};
  draftNoteState.notes.forEach((note, index) => {
    if (index === exceptIndex) return;
    used[note.type + "\t" + note.lang] = true;
  });
  return used;
}

function firstFreeDraftNote() {
  const types = draftNoteTypes();
  const langs = draftNoteLangs();
  const used = draftNoteUsedKeys(-1);
  const work = ($("#draftNoteEditor") && $("#draftNoteEditor").getAttribute("data-work-lang")) || (langs[0] && langs[0].code) || "fr";
  const preferred = ["scopeNote", "example", "historyNote", "editorialNote", "changeNote"];
  const ordered = preferred.filter((code) => types.some((type) => type.code === code))
    .concat(types.map((type) => type.code).filter((code) => preferred.indexOf(code) < 0));
  const tryLangs = [work].concat(langs.map((lang) => lang.code).filter((code) => code !== work));
  for (let i = 0; i < ordered.length; i++) {
    for (let j = 0; j < tryLangs.length; j++) {
      if (!used[ordered[i] + "\t" + tryLangs[j]]) {
        return { type: ordered[i], lang: tryLangs[j] };
      }
    }
  }
  return null;
}

function syncDraftNotesHidden() {
  const el = $("#draftNotesPayload");
  if (!el) return;
  el.value = draftNoteState.notes.map((note) => {
    return [note.type || "", note.lang || "", encodeURIComponent(note.value || ""), encodeURIComponent(note.source || "")].join("\t");
  }).join("\n");
}

function resetDraftNotes() {
  draftNoteState = { notes: [], openLang: -1 };
  const el = $("#draftNotesPayload");
  if (el) el.value = "";
  paintDraftNotes();
}

function addDraftNote() {
  const next = firstFreeDraftNote();
  if (!next) return;
  draftNoteState.notes.push({ type: next.type, lang: next.lang, value: "", source: "" });
  draftNoteState.openLang = -1;
  syncDraftNotesHidden();
  paintDraftNotes();
  requestAnimationFrame(() => {
    const input = $("#draftNoteVal-" + (draftNoteState.notes.length - 1));
    if (input) input.focus();
  });
}

function removeDraftNote(index) {
  draftNoteState.notes.splice(index, 1);
  draftNoteState.openLang = -1;
  syncDraftNotesHidden();
  paintDraftNotes();
}

function setDraftNoteField(index, field, value) {
  const note = draftNoteState.notes[index];
  if (!note) return;
  note[field] = value;
  syncDraftNotesHidden();
  if (field === "type") paintDraftNotes();
}

function setDraftNoteLang(index, lang) {
  const note = draftNoteState.notes[index];
  if (!note || !lang) return;
  note.lang = lang;
  draftNoteState.openLang = -1;
  syncDraftNotesHidden();
  paintDraftNotes();
}

function closeDraftNoteLang() {
  if (draftNoteState.openLang < 0) return false;
  draftNoteState.openLang = -1;
  paintDraftNotes();
  return true;
}

function setDraftNoteLangOpen(index) {
  draftNoteState.openLang = draftNoteState.openLang === index ? -1 : index;
  paintDraftNotes();
}

function paintDraftNotes() {
  const list = $("#draftNoteList");
  const add = $("#draftNoteAdd");
  const editor = $("#draftNoteEditor");
  if (!list || !editor) return;
  const empty = editor.getAttribute("data-empty") || "";
  const remove = editor.getAttribute("data-remove") || "";
  const textPh = editor.getAttribute("data-text-ph") || "";
  const sourcePh = editor.getAttribute("data-source-ph") || "";
  const types = draftNoteTypes();
  const langs = draftNoteLangs();
  if (!draftNoteState.notes.length) {
    list.innerHTML = empty ? '<div class="re-none">' + escapeHtml(empty) + "</div>" : "";
  } else {
    list.innerHTML = draftNoteState.notes.map((note, index) => {
      const used = draftNoteUsedKeys(index);
      const typeOpts = types.filter((type) => type.code === note.type || !used[type.code + "\t" + note.lang])
        .map((type) => '<option value="' + escapeHtml(type.code) + '"' + (type.code === note.type ? " selected" : "") + ">"
          + escapeHtml(type.label) + "</option>").join("");
      const lang = langs.find((item) => item.code === note.lang) || { code: note.lang, name: note.lang, flag: "" };
      const open = draftNoteState.openLang === index;
      const langOpts = langs.filter((item) => item.code === note.lang || !used[note.type + "\t" + item.code])
        .map((item) => '<button type="button" class="tree-lang-opt' + (item.code === note.lang ? " is-on" : "")
          + '" data-act="draft-note-lang" data-index="' + index + '" data-lang="' + escapeHtml(item.code) + '" role="option">'
          + '<span class="tree-lang-opt-flag" aria-hidden="true">' + (item.flag || "") + "</span>"
          + '<span class="tree-lang-opt-name">' + escapeHtml(item.name) + "</span>"
          + '<span class="tree-lang-opt-code">' + escapeHtml(item.code) + "</span>"
          + '<span class="tree-lang-opt-check" aria-hidden="true">✓</span></button>').join("");
      return '<div class="note-edit-card">'
        + '<div class="note-edit-head">'
        + '<select class="st-input note-type-select" data-note="type" data-index="' + index + '" aria-label="Type">'
        + typeOpts + "</select>"
        + '<div class="tp-lang-pick draft-note-lang' + (open ? " is-open" : "") + '">'
        + '<button type="button" class="tree-lang-btn tp-lang-btn draft-note-lang-btn' + (open ? " is-open" : "")
        + '" data-act="draft-note-lang-toggle" data-index="' + index + '" aria-haspopup="listbox" aria-expanded="' + (open ? "true" : "false") + '">'
        + '<span class="tree-lang-flag" aria-hidden="true">' + (lang.flag || "🏳️") + "</span>"
        + '<span class="tree-lang-name">' + escapeHtml(lang.name) + "</span>"
        + '<span class="tree-lang-code">' + escapeHtml(lang.code) + "</span>"
        + '<span class="tree-lang-caret" aria-hidden="true">▾</span></button>'
        + (open ? '<div class="tree-lang-menu tp-lang-menu" role="listbox">' + langOpts + "</div>" : "")
        + "</div>"
        + '<button type="button" class="note-edit-remove" data-act="draft-note-remove" data-index="' + index
        + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
        + "</div>"
        + '<textarea class="st-input note-edit-value disc-input" id="draftNoteVal-' + index + '" data-note="value" data-index="' + index
        + '" rows="3" placeholder="' + escapeHtml(textPh) + '">' + escapeHtml(note.value || "") + "</textarea>"
        + '<input type="text" class="st-input draft-note-source" data-note="source" data-index="' + index
        + '" value="' + escapeHtml(note.source || "") + '" placeholder="' + escapeHtml(sourcePh) + '" autocomplete="off"/>'
        + "</div>";
    }).join("");
  }
  if (add) add.hidden = !firstFreeDraftNote();
}

function currentDraftParentName() {
  if (draftRelState.bt && draftRelState.bt[0]) {
    return String(draftRelState.bt[0].label || draftRelState.bt[0].id || "").trim();
  }
  const hidden = $("#draftBt");
  return hidden ? String(hidden.value || "").trim() : "";
}

function currentDraftPathKey() {
  const root = $("#viewDraft");
  if (!root || !root.classList.contains("has-path")) return "";
  const pathEl = $("#draftPathLabel");
  return pathEl ? String(pathEl.textContent || "").replace(/ › /g, "/").trim() : "";
}

function prepareDraftForm(parentName, pathKey) {
  const root = $("#viewDraft");
  if (!root) return;
  root.classList.toggle("is-under", !!parentName);
  root.classList.toggle("has-path", !!pathKey);
  const pathEl = $("#draftPathLabel");
  if (pathEl) pathEl.textContent = pathKey.replace(/\//g, " › ");
  const pathBox = $("#draftPathBox");
  if (pathBox) pathBox.hidden = !pathKey;
  if (typeof resetCollectionPicker === "function") {
    resetCollectionPicker(pathKey ? pathKey.split("/")[0] : "");
  }
  ["draftTitle", "draftAlts", "draftHidden", "draftDef"].forEach((id) => {
    const el = $("#" + id);
    if (el) el.value = "";
  });
  resetDraftRel(parentName);
  resetDraftTr();
  resetDraftNotes();
  resetDraftRes();
  setDraftMore(false);
  setDraftTitleError(false);
  syncDraftPrefMirror();
}

function openNextDraftForm() {
  seedDraftRelFromHidden();
  state.home = false;
  state.draft = true;
  state.conceptDraft = false;
  state.facetDraft = false;
  state.collectionDraft = false;
  state.conceptId = null;
  if (state.view !== "arbo" && state.view !== "tableau" && state.view !== "recherche") {
    state.view = "arbo";
  }
  prepareDraftForm(currentDraftParentName(), currentDraftPathKey());
  highlightConcept(null);
  $("#searchBox") && $("#searchBox").classList.remove("is-open");
  showPanel(".view-panel", "viewDraft");
  requestAnimationFrame(() => { const t = $("#draftTitle"); if (t) t.focus(); });
}

function createCandidate(btn) {
  const parentName = ((btn && btn.getAttribute("data-pref")) || "").trim();
  const pathKey = ((btn && btn.getAttribute("data-path")) || "").trim();
  if (!$("#viewDraft")) {
    const q = new URLSearchParams({ new: "1" });
    if (parentName) q.set("pref", parentName);
    if (pathKey) q.set("path", pathKey);
    go("candidat/candidats.xhtml?" + q.toString());
    return;
  }
  state.home = false;
  state.draft = true;
  state.conceptDraft = false;
  state.facetDraft = false;
  state.collectionDraft = false;
  state.conceptId = null;
  if (state.view !== "arbo" && state.view !== "tableau" && state.view !== "recherche") {
    state.view = "arbo";
  }
  prepareDraftForm(parentName, pathKey);
  highlightConcept(null);
  $("#searchBox") && $("#searchBox").classList.remove("is-open");
  showPanel(".view-panel", "viewDraft");
  requestAnimationFrame(() => { const t = $("#draftTitle"); if (t) t.focus(); });
}

function draftRoot() {
  return $("#viewDraft");
}

function setDraftMore(open) {
  const root = draftRoot();
  const more = $("#draftMore");
  const toggle = $("#draftMoreToggle");
  const label = $("#draftMoreLabel");
  if (more) {
    more.hidden = !open;
    more.classList.toggle("is-off", !open);
  }
  if (toggle) {
    toggle.classList.toggle("open", !!open);
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
  }
  if (label && root) {
    label.textContent = root.getAttribute(open ? "data-more-hide" : "data-more") || label.textContent;
  }
  if (open) scheduleDraftGpsRefresh();
}

function setDraftTitleError(on) {
  const title = $("#draftTitle");
  const err = $("#draftTitleErr");
  if (title) title.classList.toggle("is-err", !!on);
  if (err) err.hidden = !on;
}

function setDraftDefError(on, silent) {
  const def = $("#draftDef");
  const err = $("#draftDefErr");
  if (def) def.classList.toggle("is-err", !!on);
  if (err) err.hidden = !on;
  if (on && !silent && typeof toast === "function") {
    const msg = err ? String(err.textContent || "").trim() : "";
    if (msg) toast(msg, { error: true });
  }
}

function syncDraftLocation() {
  const root = draftRoot();
  const loc = $("#draftLoc");
  const warn = $("#draftRootWarn");
  const bt = ($("#draftBt") && $("#draftBt").value.trim()) || "";
  if (loc && root) {
    const tpl = root.getAttribute("data-loc-under") || "Créé sous « {0} »";
    loc.textContent = bt
      ? tpl.replace("{0}", bt)
      : (root.getAttribute("data-loc-root") || loc.textContent);
  }
  if (warn) warn.hidden = true;
}

function syncDraftPrefMirror() {
  const title = $("#draftTitle");
  if (title && title.value.trim()) setDraftTitleError(false);
}

function isDraftDirty() {
  const hasRel = draftRelState.nt.length + draftRelState.rt.length > 0;
  const hasTr = draftTrState.order.some((code) => String(draftTrState.values[code] || "").trim()
    || String(draftTrState.alts[code] || "").trim());
  const hasRes = draftResState.links.some((item) => String(item.uri || "").trim() || String(item.label || "").trim())
    || draftResState.images.some((item) => String(item.uri || "").trim() || String(item.name || "").trim())
    || draftResState.gps.some((item) => String(item.lat || "").trim() || String(item.lng || "").trim());
  const hasNotes = draftNoteState.notes.some((item) => String(item.value || "").trim() || String(item.source || "").trim());
  return hasRel || hasTr || hasRes || hasNotes || ["draftTitle", "draftDef", "draftAlts", "draftHidden",
          "draftColl"].some((id) => {
    const el = $("#" + id);
    return !!(el && String(el.value || "").trim());
  });
}

function hideDraftLeave() {
  const dlg = $("#draftLeaveConfirm");
  if (dlg) dlg.hidden = true;
}

var draftCreateKind = "create";

function hideDraftCreateConfirm() {
  const dlg = $("#draftCreateConfirm");
  if (dlg) dlg.hidden = true;
}

function draftDefinitionValue() {
  const def = $("#draftDef");
  return def ? String(def.value || "").trim() : "";
}

function requestDraftCreate(kind) {
  const title = $("#draftTitle");
  const name = title ? String(title.value || "").trim() : "";
  if (!name) {
    setDraftTitleError(true);
    if (title) title.focus();
    return;
  }
  setDraftTitleError(false);
  if (!draftDefinitionValue()) {
    setDraftDefError(true);
    const def = $("#draftDef");
    if (def) def.focus();
    return;
  }
  setDraftDefError(false, true);
  draftCreateKind = kind === "chain" ? "chain" : "create";
  const root = draftRoot();
  const dlg = $("#draftCreateConfirm");
  const text = $("#draftCreateText");
  const go = $("#draftCreateGo");
  const tpl = root && root.getAttribute(draftCreateKind === "chain" ? "data-confirm-chain" : "data-confirm-create");
  if (text && tpl) text.textContent = tpl.replace("{0}", name);
  if (go && root) {
    go.textContent = root.getAttribute(draftCreateKind === "chain" ? "data-confirm-go-chain" : "data-confirm-go") || go.textContent;
  }
  hideDraftLeave();
  if (dlg) dlg.hidden = false;
}

function confirmDraftCreate() {
  hideDraftCreateConfirm();
  resolveDraft(draftCreateKind === "chain" ? "chaîne" : "créé");
}

function requestDraftLeave() {
  hideDraftCreateConfirm();
  if (isDraftDirty()) {
    const dlg = $("#draftLeaveConfirm");
    if (dlg) dlg.hidden = false;
    return;
  }
  resolveDraft("annulé");
}

function draftFlagOn(el, name) {
  if (!el) return false;
  const value = String(el.getAttribute(name) || "").trim().toLowerCase();
  return value === "1" || value === "true";
}

function bindDraftCreateFields() {
  if (typeof syncDraftRelHidden === "function") syncDraftRelHidden();
  if (typeof syncDraftTrHidden === "function") syncDraftTrHidden();
  if (typeof syncDraftNotesHidden === "function") syncDraftNotesHidden();
  const collIds = $("#draftCollIds");
  const collBound = $("#draftCollIdsBound");
  if (collIds && collBound) collBound.value = collIds.value || "";
}

function resolveDraft(kind) {
  if (kind === "créé" || kind === "chaîne") {
    const title = $("#draftTitle");
    if (!title || !String(title.value || "").trim()) {
      setDraftTitleError(true);
      if (title) title.focus();
      return;
    }
    setDraftTitleError(false);
    if (!draftDefinitionValue()) {
      setDraftDefError(true);
      const def = $("#draftDef");
      if (def) def.focus();
      return;
    }
    setDraftDefError(false, true);
    bindDraftCreateFields();
    const mode = $("#draftCreateMode");
    if (mode) mode.value = kind === "chaîne" ? "chain" : "create";
    const createBtn = kind === "chaîne"
      ? ($("#draftCreateNext") || $("#draftCreate"))
      : $("#draftCreate");
    if (createBtn) {
      createBtn.click();
      return;
    }
  }
  if (kind === "annulé") {
    hideDraftLeave();
    const cancelBtn = $("#draftCancel");
    if (cancelBtn) {
      cancelBtn.click();
      return;
    }
  }
  resolveDraftUi(kind);
}

function onDraftCreateAjax(data) {
  if (data.status !== "success") return;
  const result = $("#draftResult");
  const error = result && String(result.getAttribute("data-error") || "").trim();
  if (!draftFlagOn(result, "data-created")) {
    if (error && typeof toast === "function") toast(error, { error: true });
    const title = $("#draftTitle");
    if (title && !String(title.value || "").trim()) setDraftTitleError(true);
    if (!draftDefinitionValue()) setDraftDefError(true, !!error);
    if (typeof syncDraftLocation === "function") syncDraftLocation();
    return;
  }
  const root = draftRoot();
  const msg = root && root.getAttribute("data-created-msg");
  if (msg && typeof toast === "function") toast(msg, { soft: true });
  if (draftFlagOn(result, "data-chain")) {
    openNextDraftForm();
    return;
  }
  resolveDraftUi("créé");
}
window.onDraftCreateAjax = onDraftCreateAjax;

function guardDraftCreate() {
  const title = $("#draftTitle");
  if (!title || !String(title.value || "").trim()) {
    setDraftTitleError(true);
    if (title) title.focus();
    return false;
  }
  setDraftTitleError(false);
  if (!draftDefinitionValue()) {
    setDraftDefError(true);
    const def = $("#draftDef");
    if (def) def.focus();
    return false;
  }
  setDraftDefError(false, true);
  bindDraftCreateFields();
  return true;
}

function resolveDraftUi(kind) {
  hideDraftLeave();
  hideDraftCreateConfirm();
  state.draft = false;
  if (SCREEN === "candidats") {
    showPanel(".view-panel", "viewCandList");
    return;
  }
  paint();
}
if (typeof window !== "undefined") {
  window.resolveDraftUi = resolveDraftUi;
  window.syncDraftPrefMirror = syncDraftPrefMirror;
  window.setDraftDefError = setDraftDefError;
  window.syncDraftLocation = syncDraftLocation;
  window.guardDraftCreate = guardDraftCreate;
  window.requestDraftLeave = requestDraftLeave;
  window.hideDraftLeave = hideDraftLeave;
  window.requestDraftCreate = requestDraftCreate;
  window.hideDraftCreateConfirm = hideDraftCreateConfirm;
  window.confirmDraftCreate = confirmDraftCreate;
  window.setDraftMore = setDraftMore;
  window.resetDraftRel = resetDraftRel;
  window.seedDraftRelFromHidden = seedDraftRelFromHidden;
  window.setDraftRelKind = setDraftRelKind;
  window.setDraftRelKindMenu = setDraftRelKindMenu;
  window.addDraftRel = addDraftRel;
  window.removeDraftRel = removeDraftRel;
  window.scheduleDraftRelSearch = scheduleDraftRelSearch;
  window.hideDraftRelDrop = hideDraftRelDrop;
  window.resetDraftTr = resetDraftTr;
  window.seedDraftTrFromHidden = seedDraftTrFromHidden;
  window.seedConceptTrFromHidden = seedConceptTrFromHidden;
  window.seedFacetCardTrFromHidden = seedFacetCardTrFromHidden;
  window.addFacetCardTr = addFacetCardTr;
  window.removeFacetCardTr = removeFacetCardTr;
  window.askRemoveFacetCardTr = askRemoveFacetCardTr;
  window.keepFacetCardTr = keepFacetCardTr;
  window.setFacetCardTrValue = setFacetCardTrValue;
  window.paintFacetCardTr = paintFacetCardTr;
  window.closeFacetCardTrLang = closeFacetCardTrLang;
  window.setFacetCardTrLangOpen = setFacetCardTrLangOpen;
  window.syncFacetCardTrHidden = syncFacetCardTrHidden;
  window.seedFacetCardMemFromHidden = seedFacetCardMemFromHidden;
  window.addFacetCardMem = addFacetCardMem;
  window.removeFacetCardMem = removeFacetCardMem;
  window.hideFacetCardMemDrop = hideFacetCardMemDrop;
  window.scheduleFacetCardMemSearch = scheduleFacetCardMemSearch;
  window.syncFacetCardMemHidden = syncFacetCardMemHidden;
  window.seedFacetCardNotesFromHidden = seedFacetCardNotesFromHidden;
  window.addFacetCardNote = addFacetCardNote;
  window.removeFacetCardNote = removeFacetCardNote;
  window.setFacetCardNoteField = setFacetCardNoteField;
  window.setFacetCardNoteLang = setFacetCardNoteLang;
  window.closeFacetCardNoteLang = closeFacetCardNoteLang;
  window.setFacetCardNoteLangOpen = setFacetCardNoteLangOpen;
  window.syncFacetCardNotesHidden = syncFacetCardNotesHidden;
  window.paintFacetCardNotes = paintFacetCardNotes;
  window.addConceptTr = addConceptTr;
  window.removeConceptTr = removeConceptTr;
  window.askRemoveConceptTr = askRemoveConceptTr;
  window.keepConceptTr = keepConceptTr;
  window.setConceptTrValue = setConceptTrValue;
  window.setConceptTrAlt = setConceptTrAlt;
  window.paintConceptTr = paintConceptTr;
  window.closeConceptTrLang = closeConceptTrLang;
  window.setConceptTrLangOpen = setConceptTrLangOpen;
  window.syncConceptTrHidden = syncConceptTrHidden;
  window.seedConceptNotesFromHidden = seedConceptNotesFromHidden;
  window.addConceptNoteLang = addConceptNoteLang;
  window.setConceptNoteLang = setConceptNoteLang;
  window.toggleConceptNoteCompare = toggleConceptNoteCompare;
  window.addConceptNote = addConceptNote;
  window.addConceptNoteType = addConceptNoteType;
  window.removeConceptNote = removeConceptNote;
  window.setConceptNoteField = setConceptNoteField;
  window.paintConceptNotes = paintConceptNotes;
  window.closeConceptNoteLang = closeConceptNoteLang;
  window.setConceptNoteLangOpen = setConceptNoteLangOpen;
  window.closeConceptNoteTypeMenu = closeConceptNoteTypeMenu;
  window.syncConceptNoteHidden = syncConceptNoteHidden;
  window.seedConceptResFromHidden = seedConceptResFromHidden;
  window.syncConceptResHidden = syncConceptResHidden;
  window.seedConceptRelFromHidden = seedConceptRelFromHidden;
  window.syncCvRelHidden = syncCvRelHidden;
  window.setCvRelKind = setCvRelKind;
  window.setCvRelKindMenu = setCvRelKindMenu;
  window.setCvRelRowMenu = setCvRelRowMenu;
  window.addCvRel = addCvRel;
  window.retypeCvRel = retypeCvRel;
  window.removeCvRel = removeCvRel;
  window.scheduleCvRelSearch = scheduleCvRelSearch;
  window.hideCvRelDrop = hideCvRelDrop;
  window.addConceptResLink = addConceptResLink;
  window.addConceptResImage = addConceptResImage;
  window.addConceptResGps = addConceptResGps;
  window.removeConceptResLink = removeConceptResLink;
  window.removeConceptResImage = removeConceptResImage;
  window.removeConceptResGps = removeConceptResGps;
  window.setConceptResLink = setConceptResLink;
  window.setConceptResImage = setConceptResImage;
  window.setConceptResGps = setConceptResGps;
  window.openConceptResImgLightbox = openConceptResImgLightbox;
  window.closeConceptResImgLightbox = closeConceptResImgLightbox;
  window.showConceptResImgAt = showConceptResImgAt;
  window.gotoConceptResGps = gotoConceptResGps;
  window.destroyConceptResGpsMap = destroyConceptResGpsMap;
  window.addDraftTr = addDraftTr;
  window.removeDraftTr = removeDraftTr;
  window.setDraftTrValue = setDraftTrValue;
  window.setDraftTrAlt = setDraftTrAlt;
  window.paintDraftTr = paintDraftTr;
  window.closeDraftTrLang = closeDraftTrLang;
  window.setDraftTrLangOpen = setDraftTrLangOpen;
  window.resetDraftRes = resetDraftRes;
  window.paintDraftRes = paintDraftRes;
  window.addDraftResLink = addDraftResLink;
  window.addDraftResImage = addDraftResImage;
  window.addDraftResGps = addDraftResGps;
  window.removeDraftResLink = removeDraftResLink;
  window.removeDraftResImage = removeDraftResImage;
  window.removeDraftResGps = removeDraftResGps;
  window.setDraftResLink = setDraftResLink;
  window.setDraftResImage = setDraftResImage;
  window.setDraftResGps = setDraftResGps;
  window.openDraftImgLightbox = openDraftImgLightbox;
  window.closeDraftImgLightbox = closeDraftImgLightbox;
  window.showDraftImgAt = showDraftImgAt;
  window.gotoDraftGps = gotoDraftGps;
  window.resetDraftNotes = resetDraftNotes;
  window.paintDraftNotes = paintDraftNotes;
  window.openNextDraftForm = openNextDraftForm;
  window.addDraftNote = addDraftNote;
  window.removeDraftNote = removeDraftNote;
  window.setDraftNoteField = setDraftNoteField;
  window.setDraftNoteLang = setDraftNoteLang;
  window.setDraftNoteLangOpen = setDraftNoteLangOpen;
  window.closeDraftNoteLang = closeDraftNoteLang;
}

function conceptDraftRoot() {
  return $("#viewConceptDraft");
}

function conceptDraftFlagOn(el, attr) {
  return !!(el && (el.getAttribute(attr) === "1" || el.getAttribute(attr) === "true"));
}

function setConceptDraftTitleError(on) {
  const title = $("#cptTitle");
  const err = $("#cptTitleErr");
  if (title) title.classList.toggle("is-err", !!on);
  if (err) err.hidden = true;
  if (on && err && typeof toast === "function") {
    const msg = String(err.textContent || "").trim();
    if (msg) toast(msg, { error: true });
  }
}

function syncConceptDraftPref() {
  setConceptDraftTitleError(false);
}

function syncConceptDraftLocation() {
  const root = conceptDraftRoot();
  const loc = $("#cptLoc");
  const warn = $("#cptRootWarn");
  const relRow = $("#cptRelRow");
  const parent = String(($("#cptParentLabel") && $("#cptParentLabel").value) || "").trim();
  if (loc && root) {
    const tpl = root.getAttribute("data-loc-under") || "Créé sous « {0} »";
    loc.textContent = parent
      ? tpl.replace("{0}", parent)
      : (root.getAttribute("data-loc-root") || loc.textContent);
  }
  if (warn) warn.hidden = !!parent;
  if (relRow) relRow.hidden = !parent;
  if (root) {
    root.classList.toggle("is-under", !!parent);
  }
}

function setConceptDraftMore(open) {
  const root = conceptDraftRoot();
  const more = $("#cptMore");
  const toggle = $("#cptMoreToggle");
  const label = $("#cptMoreLabel");
  if (more) {
    more.hidden = !open;
    more.classList.toggle("is-off", !open);
  }
  if (toggle) {
    toggle.classList.toggle("open", !!open);
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
  }
  if (label && root) {
    label.textContent = root.getAttribute(open ? "data-more-hide" : "data-more") || label.textContent;
  }
}

function prepareConceptDraftForm(parentName) {
  const root = conceptDraftRoot();
  if (!root) return;
  ["cptTitle", "cptDef", "cptNotation", "cptCustomId"].forEach((id) => {
    const el = $("#" + id);
    if (el) el.value = "";
  });
  setConceptDraftMore(false);
  setConceptDraftTitleError(false);
  syncConceptDraftLocation();
}

function showConceptDraftPanel(parentName) {
  state.home = false;
  state.draft = false;
  state.conceptDraft = true;
  state.facetDraft = false;
  state.collectionDraft = false;
  state.conceptId = null;
  if (state.view !== "arbo" && state.view !== "tableau" && state.view !== "recherche") {
    state.view = "arbo";
  }
  prepareConceptDraftForm(parentName);
  highlightConcept(null);
  closeSearchUi();
  showPanel(".view-panel", "viewConceptDraft");
  requestAnimationFrame(() => { const t = $("#cptTitle"); if (t) t.focus(); });
}

function createConceptDraft(src) {
  const parentName = ((src && src.getAttribute("data-pref")) || "").trim();
  const parentId = ((src && src.getAttribute("data-id")) || "").trim();
  if (!$("#viewConceptDraft")) {
    const q = new URLSearchParams({ create: "concept" });
    if (parentName) q.set("pref", parentName);
    if (parentId) q.set("idc", parentId);
    go(conceptPageHref(q.toString()));
    return;
  }
  const idEl = $("#cptParentId");
  const labEl = $("#cptParentLabel");
  if (idEl) idEl.value = parentId;
  if (labEl) labEl.value = parentName;
  showConceptDraftPanel(parentName);
  const go = $("#cptPrepare");
  if (go) go.click();
}

function onConceptDraftPrepare(data) {
  if (data.status !== "success") return;
  const parentName = String(($("#cptParentLabel") && $("#cptParentLabel").value) || "").trim();
  showConceptDraftPanel(parentName);
}
window.onConceptDraftPrepare = onConceptDraftPrepare;

function hideConceptDraftCreateConfirm() {
  const dlg = $("#cptCreateConfirm");
  if (dlg) dlg.hidden = true;
}

function hideConceptDraftLeave() {
  const dlg = $("#cptLeaveConfirm");
  if (dlg) dlg.hidden = true;
}

function requestConceptDraftCreate(kind) {
  const title = $("#cptTitle");
  if (!title || !String(title.value || "").trim()) {
    setConceptDraftTitleError(true);
    if (title) title.focus();
    return;
  }
  setConceptDraftTitleError(false);
  confirmConceptDraftCreate(kind);
}

function confirmConceptDraftCreate(kind) {
  hideConceptDraftCreateConfirm();
  const mode = $("#cptCreateMode");
  const resolved = kind === "chain" ? "chain" : "create";
  if (mode) mode.value = resolved;
  const createBtn = resolved === "chain"
    ? ($("#cptCreateNext") || $("#cptCreate"))
    : $("#cptCreate");
  if (createBtn) createBtn.click();
}

function conceptDraftDirty() {
  const text = ["cptTitle", "cptDef", "cptNotation", "cptCustomId"].some((id) => {
    const el = $("#" + id);
    return el && String(el.value || "").trim();
  });
  const group = $("#cptGroup");
  return text || !!(group && String(group.value || "").trim());
}

function requestConceptDraftLeave() {
  const dlg = $("#cptLeaveConfirm");
  if (conceptDraftDirty() && dlg) {
    dlg.hidden = false;
    return;
  }
  resolveConceptDraft("annulé");
}

function resolveConceptDraft(kind) {
  if (kind === "annulé") {
    hideConceptDraftLeave();
    const cancelBtn = $("#cptCancel");
    if (cancelBtn) {
      cancelBtn.click();
      return;
    }
  }
  resolveConceptDraftUi(kind);
}

function onConceptDraftAjax(data) {
  if (data.status !== "success") return;
  const result = $("#cptResult");
  const error = result && String(result.getAttribute("data-error") || "").trim();
  if (error && typeof toast === "function") toast(error, { error: true });
  if (conceptDraftFlagOn(result, "data-dup")) {
    return;
  }
  if (!conceptDraftFlagOn(result, "data-created")) {
    const title = $("#cptTitle");
    if (!error && title && !String(title.value || "").trim()) setConceptDraftTitleError(true);
    return;
  }
  const root = conceptDraftRoot();
  const msg = root && root.getAttribute("data-created-msg");
  if (msg && typeof toast === "function") toast(msg, { soft: true });
  const createdId = result && result.getAttribute("data-id");
  if (createdId && typeof selectOpenedInTree === "function") {
    selectOpenedInTree(createdId);
  }
  if (conceptDraftFlagOn(result, "data-chain")) {
    const parentName = String(($("#cptParentLabel") && $("#cptParentLabel").value) || "").trim();
    showConceptDraftPanel(parentName);
    return;
  }
  resolveConceptDraftUi("créé");
  if (createdId && typeof openLiveDetail === "function") {
    openLiveDetail(createdId, "concept");
  } else if (createdId && typeof openConcept === "function") {
    openConcept(createdId);
  }
}
window.onConceptDraftAjax = onConceptDraftAjax;

function resolveConceptDraftUi(kind) {
  hideConceptDraftLeave();
  hideConceptDraftCreateConfirm();
  state.conceptDraft = false;
  paint();
}
if (typeof window !== "undefined") {
  window.resolveConceptDraftUi = resolveConceptDraftUi;
  window.syncConceptDraftPref = syncConceptDraftPref;
  window.syncConceptDraftLocation = syncConceptDraftLocation;
  window.createConceptDraft = createConceptDraft;
  window.requestConceptDraftCreate = requestConceptDraftCreate;
  window.confirmConceptDraftCreate = confirmConceptDraftCreate;
  window.requestConceptDraftLeave = requestConceptDraftLeave;
  window.resolveConceptDraft = resolveConceptDraft;
  window.setConceptDraftMore = setConceptDraftMore;
}

function facetDraftRoot() {
  return $("#viewFacetDraft");
}

function facetDraftFlagOn(el, attr) {
  return !!(el && (el.getAttribute(attr) === "1" || el.getAttribute(attr) === "true"));
}

function setFacetDraftTitleError(on) {
  const title = $("#fctTitle");
  const err = $("#fctTitleErr");
  if (title) title.classList.toggle("is-err", !!on);
  if (err) err.hidden = !on;
}

function syncFacetDraftPref() {
  setFacetDraftTitleError(false);
}

function setFacetDraftMore(open) {
  const root = facetDraftRoot();
  const more = $("#fctMore");
  const toggle = $("#fctMoreToggle");
  const label = $("#fctMoreLabel");
  if (more) {
    more.hidden = !open;
    more.classList.toggle("is-off", !open);
  }
  if (toggle) {
    toggle.classList.toggle("open", !!open);
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
  }
  if (label && root) {
    label.textContent = root.getAttribute(open ? "data-more-hide" : "data-more") || label.textContent;
  }
}

function facetDraftDirty() {
  const text = ["fctTitle", "fctDef", "fctAlts"].some((id) => {
    const el = $("#" + id);
    return el && String(el.value || "").trim();
  });
  return text || fctTrState.order.length > 0 || fctNoteState.notes.length > 0 || fctMemState.items.length > 0;
}

function prepareFacetDraftForm() {
  const root = facetDraftRoot();
  if (!root) return;
  ["fctTitle", "fctDef", "fctAlts"].forEach((id) => {
    const el = $("#" + id);
    if (el) el.value = "";
  });
  resetFacetDraftTr();
  resetFacetDraftNotes();
  resetFacetDraftMem();
  setFacetDraftMore(false);
  setFacetDraftTitleError(false);
}

