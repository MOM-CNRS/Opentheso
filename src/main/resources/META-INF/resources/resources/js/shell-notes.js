/** Fiche — notes, texte riche et alignements. */
var cvNoteState = { notes: [], tabs: [], lang: "", compare: false, typeOpen: false };

function cvNoteRoot() {
  if (cvNoteState.root && cvNoteState.root.isConnected) return cvNoteState.root;
  return document.getElementById("cvNoteEditor");
}

function cvNoteEl(name) {
  const root = cvNoteRoot();
  if (!root) return null;
  if (name === "editor") return root;
  const byData = root.querySelector("[data-cv='" + name + "']");
  if (byData) return byData;
  const ids = {
    tabs: "cvNoteTabs",
    btn: "cvNoteBtn",
    menu: "cvNoteMenu",
    cmp: "cvNoteCmp",
    body: "cvNoteBody",
    seed: "cvNoteSeed",
    types: "cvNoteTypeOpts",
    langs: "cvNoteLangOpts",
    payload: "cvNotePayload",
    pick: "cvNotePick"
  };
  const id = ids[name];
  return id ? root.querySelector("#" + id) : null;
}

function focusNoteEditor(node) {
  const editor = node && node.closest && node.closest("#cvNoteEditor, #collNoteEditor");
  if (!editor) return null;
  if (cvNoteState.bound !== editor) seedConceptNotesFromHidden(editor);
  else cvNoteState.root = editor;
  return editor;
}

function cvNoteTypes() {
  const box = cvNoteEl("types");
  return (box ? [...box.querySelectorAll("[data-code]")] : []).map((el) => ({
    code: el.getAttribute("data-code") || "",
    label: el.getAttribute("data-label") || el.getAttribute("data-code") || ""
  })).filter((item) => item.code);
}

function cvNoteLangs() {
  const box = cvNoteEl("langs");
  return (box ? [...box.querySelectorAll("[data-code]")] : []).map((el) => ({
    code: el.getAttribute("data-code") || "",
    name: el.getAttribute("data-name") || el.getAttribute("data-code") || "",
    flag: el.getAttribute("data-flag") || ""
  })).filter((item) => item.code);
}

function cvNoteWorkLang() {
  const editor = cvNoteEl("editor");
  const langs = cvNoteLangs();
  return (editor && editor.getAttribute("data-work-lang")) || (langs[0] && langs[0].code) || "fr";
}

function cvNoteMeta(code) {
  const langs = cvNoteLangs();
  return langs.find((item) => String(item.code).toLowerCase() === String(code || "").toLowerCase())
    || { code: code || "", name: code || "", flag: "" };
}

function cvNoteLangMark(meta, lang) {
  if (meta.flag && meta.flag !== "🏳️") {
    return '<span class="tr-flag">' + meta.flag + "</span>";
  }
  return '<span class="tr-code">' + escapeHtml(lang) + "</span>";
}

function cvNoteNorm(code) {
  return String(code || "").trim().toLowerCase();
}

function cvNoteFilled(note) {
  return !!(note && String(note.value || "").trim());
}

function cvNoteTabs() {
  const work = cvNoteNorm(cvNoteWorkLang());
  const seen = {};
  const tabs = [];
  function add(code) {
    const key = cvNoteNorm(code);
    if (!key || seen[key]) return;
    seen[key] = true;
    tabs.push(code);
  }
  add(work);
  cvNoteState.notes.forEach((note) => add(note.lang));
  (cvNoteState.tabs || []).forEach((code) => add(code));
  return tabs;
}

function cvNoteOfLang(lang) {
  const key = cvNoteNorm(lang);
  return cvNoteState.notes
    .map((note, index) => ({ note: note, index: index }))
    .filter((item) => cvNoteNorm(item.note.lang) === key);
}

function cvNoteUsedTypes(lang, exceptIndex) {
  const used = {};
  cvNoteOfLang(lang).forEach((item) => {
    if (item.index === exceptIndex) return;
    if (item.note.type) used[item.note.type] = true;
  });
  return used;
}

function firstFreeConceptNoteType(lang) {
  const used = cvNoteUsedTypes(lang, -1);
  const types = cvNoteTypes();
  return types.find((type) => !used[type.code]) || null;
}

function closeConceptNoteLang() {
  const btn = cvNoteEl("btn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function setConceptNoteLangOpen(open) {
  const btn = cvNoteEl("btn");
  if (!btn) return;
  btn.classList.toggle("is-open", !!open);
  btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function closeConceptNoteTypeMenu() {
  if (!cvNoteState.typeOpen) return false;
  cvNoteState.typeOpen = false;
  const root = cvNoteRoot();
  const menu = root && root.querySelector("#cvNoteTypeMenu");
  if (menu) menu.remove();
  return true;
}

function syncConceptNoteHidden() {
  const el = cvNoteEl("payload") || jsField("cvNotePayload");
  if (!el) return;
  const lines = cvNoteState.notes.filter(cvNoteFilled).map((note) => {
    return [note.type || "", note.lang || "", encodeURIComponent(note.value || ""), encodeURIComponent(note.source || "")].join("\t");
  });
  el.value = lines.length ? lines.join("\n") : "\t\t\t";
}

function seedConceptNotesFromHidden(editor) {
  editor = editor && editor.nodeType === 1 ? editor : document.getElementById("cvNoteEditor");
  if (!editor) return;
  if (cvNoteState.bound === editor) return;
  const work = (editor.getAttribute("data-work-lang") || "").trim() || "fr";
  cvNoteState = { notes: [], tabs: [], lang: work, compare: false, typeOpen: false, bound: editor, root: editor };
  const seed = cvNoteEl("seed");
  (seed ? [...seed.querySelectorAll("[data-lang]")] : []).forEach((el) => {
    const type = (el.getAttribute("data-type") || "").trim();
    const lang = (el.getAttribute("data-lang") || "").trim();
    if (!type || !lang) return;
    cvNoteState.notes.push({
      type: type,
      lang: lang,
      value: el.getAttribute("data-value") || "",
      source: el.getAttribute("data-source") || ""
    });
  });
  const tabs = cvNoteTabs();
  if (!tabs.some((code) => cvNoteNorm(code) === cvNoteNorm(cvNoteState.lang)) && tabs[0]) {
    cvNoteState.lang = tabs[0];
  }
  syncConceptNoteHidden();
  paintConceptNotes();
}

function setConceptNoteLang(code) {
  if (!code) return;
  cvNoteState.lang = code;
  cvNoteState.typeOpen = false;
  closeConceptNoteLang();
  paintConceptNotes();
}

function addConceptNoteLang(code) {
  if (!code) return;
  if (!trLangAlready(cvNoteState.tabs.concat(cvNoteTabs()), code)) {
    cvNoteState.tabs.push(code);
  }
  cvNoteState.lang = code;
  cvNoteState.typeOpen = false;
  closeConceptNoteLang();
  syncConceptNoteHidden();
  paintConceptNotes();
}

function toggleConceptNoteCompare() {
  cvNoteState.compare = !cvNoteState.compare;
  cvNoteState.typeOpen = false;
  paintConceptNotes();
}

function addConceptNote() {
  const lang = cvNoteState.lang || cvNoteWorkLang();
  const free = cvNoteTypes().filter((type) => !cvNoteUsedTypes(lang, -1)[type.code]);
  if (!free.length) return;
  if (free.length === 1) {
    addConceptNoteType(free[0].code);
    return;
  }
  cvNoteState.typeOpen = !cvNoteState.typeOpen;
  paintConceptNotes();
}

function addConceptNoteType(type) {
  const lang = cvNoteState.lang || cvNoteWorkLang();
  if (!type || cvNoteUsedTypes(lang, -1)[type]) {
    cvNoteState.typeOpen = false;
    paintConceptNotes();
    return;
  }
  cvNoteState.notes.push({ type: type, lang: lang, value: "", source: "" });
  cvNoteState.typeOpen = false;
  syncConceptNoteHidden();
  paintConceptNotes();
  requestAnimationFrame(() => {
    const root = cvNoteRoot();
    const input = root && root.querySelector('[data-cv-note="value"][data-index="' + (cvNoteState.notes.length - 1) + '"]');
    if (input) input.focus();
  });
}

function removeConceptNote(index) {
  cvNoteState.notes.splice(index, 1);
  cvNoteState.typeOpen = false;
  syncConceptNoteHidden();
  paintConceptNotes();
}

function setConceptNoteField(index, field, value) {
  const note = cvNoteState.notes[index];
  if (!note || !field) return;
  if (field === "type") {
    const used = cvNoteUsedTypes(note.lang, index);
    if (used[value]) return;
  }
  note[field] = value;
  syncConceptNoteHidden();
  if (field === "type") paintConceptNotes();
}

function cvNoteCompareLang() {
  const current = cvNoteNorm(cvNoteState.lang);
  const work = cvNoteNorm(cvNoteWorkLang());
  if (current !== work) return cvNoteWorkLang();
  const tabs = cvNoteTabs();
  return tabs.find((code) => cvNoteNorm(code) !== current) || "";
}

function paintNoteColHead(lang) {
  const meta = cvNoteMeta(lang);
  return '<div class="note-ed-colh">'
    + cvNoteLangMark(meta, lang)
    + '<span>' + escapeHtml(meta.name || lang) + "</span>"
    + '<span class="te-code">' + escapeHtml(lang) + "</span>"
    + "</div>";
}

function paintNoteGhost(typeCode) {
  const type = cvNoteTypes().find((item) => item.code === typeCode);
  const label = (type && type.label) || typeCode || "";
  return '<div class="note-ghost"><span class="note-ghost-t">' + escapeHtml(label) + "</span></div>";
}

function paintConceptNoteAdd(lang) {
  const editor = cvNoteEl("editor");
  const add = (editor && editor.getAttribute("data-add")) || "";
  const free = cvNoteTypes().filter((type) => !cvNoteUsedTypes(lang, -1)[type.code]);
  if (!free.length) return "";
  const menu = cvNoteState.typeOpen
    ? '<div class="note-menu" id="cvNoteTypeMenu">'
      + free.map((type) => '<button type="button" data-act="cv-note-add-type" data-type="'
        + escapeHtml(type.code) + '">' + escapeHtml(type.label) + "</button>").join("")
      + "</div>"
    : "";
  return '<div class="note-menu-w">'
    + '<button type="button" class="note-ed-add" data-act="cv-note-add">' + escapeHtml(add) + "</button>"
    + menu
    + "</div>";
}

var RICH_TAGS = { A: 1, B: 1, STRONG: 1, I: 1, EM: 1, U: 1, S: 1, STRIKE: 1, SUP: 1, SUB: 1, BR: 1, P: 1, SPAN: 1, DIV: 1, UL: 1, OL: 1, LI: 1, SMALL: 1 };
var RICH_STYLE = { color: 1, "background-color": 1, "font-weight": 1, "font-style": 1, "font-size": 1, "text-decoration": 1, "text-align": 1 };

function formatRichHtml(value) {
  let source = String(value == null ? "" : value);
  if (!source.trim()) return "";
  if (source.indexOf("<") < 0 && source.indexOf("&lt;") >= 0) {
    source = source.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, "\"").replace(/&#39;/g, "'").replace(/&amp;/g, "&");
  }
  if (source.indexOf("<") < 0) source = source.replace(/\r\n/g, "\n").replace(/\n/g, "<br>");
  const doc = new DOMParser().parseFromString(source, "text/html");
  ["script", "style", "iframe", "object", "embed", "link", "meta"].forEach((tag) => {
    doc.querySelectorAll(tag).forEach((node) => node.remove());
  });
  sanitizeRichNode(doc.body);
  return doc.body.innerHTML;
}

function sanitizeRichNode(node) {
  let child = node.firstChild;
  while (child) {
    const next = child.nextSibling;
    if (child.nodeType !== 1) {
      if (child.nodeType !== 3) child.remove();
      child = next;
      continue;
    }
    if (!RICH_TAGS[child.tagName]) {
      const first = child.firstChild;
      while (child.firstChild) node.insertBefore(child.firstChild, child);
      child.remove();
      child = first || next;
      continue;
    }
    Array.from(child.attributes).forEach((attr) => {
      const name = attr.name.toLowerCase();
      if (child.tagName === "A" && (name === "href" || name === "title")) return;
      if (name === "style" && (child.tagName === "SPAN" || child.tagName === "P" || child.tagName === "DIV")) return;
      child.removeAttribute(attr.name);
    });
    if (child.tagName === "A") {
      const href = child.getAttribute("href") || "";
      if (!/^(https?:|mailto:)/i.test(href) || /\s/.test(href)) child.removeAttribute("href");
      else {
        child.setAttribute("rel", "noopener noreferrer");
        child.setAttribute("target", "_blank");
      }
    }
    if (child.hasAttribute("style")) {
      const safe = String(child.getAttribute("style") || "").split(";").map((part) => {
        const bits = part.split(":");
        if (bits.length < 2) return "";
        const prop = bits.shift().trim().toLowerCase();
        const cssValue = bits.join(":").trim();
        if (!RICH_STYLE[prop] || /url|expression|javascript|@|!|\\|<|>/i.test(cssValue)) return "";
        return prop + ": " + cssValue;
      }).filter(Boolean).join("; ");
      if (safe) child.setAttribute("style", safe);
      else child.removeAttribute("style");
    }
    sanitizeRichNode(child);
    child = next;
  }
}

function paintConceptNoteCard(item, editable) {
  const editor = cvNoteEl("editor");
  const remove = (editor && editor.getAttribute("data-remove")) || "";
  const textPh = (editor && editor.getAttribute("data-text-ph")) || "";
  const sourcePh = (editor && editor.getAttribute("data-source-ph")) || "";
  const note = item.note;
  const dir = trTextDir(note.lang);
  const used = cvNoteUsedTypes(note.lang, item.index);
  const typeOpts = cvNoteTypes()
    .filter((type) => type.code === note.type || !used[type.code])
    .map((type) => '<option value="' + escapeHtml(type.code) + '"' + (type.code === note.type ? " selected" : "") + ">"
      + escapeHtml(type.label) + "</option>").join("");
  const typeLabel = (cvNoteTypes().find((type) => type.code === note.type) || { label: note.type }).label;
  if (!editable) {
    const empty = !cvNoteFilled(note);
    return '<div class="note-ref' + (empty ? " is-empty" : "") + '">'
      + '<div class="note-c-h"><span class="note-c-type-lbl">' + escapeHtml(typeLabel) + "</span></div>"
      + (empty
        ? ""
        : '<div class="cv-def cv-html" lang="' + escapeHtml(note.lang) + '" dir="' + dir + '">' + formatRichHtml(note.value) + "</div>"
          + (note.source ? '<span class="note-src">' + escapeHtml(note.source) + "</span>" : ""))
      + "</div>";
  }
  return '<div class="note-c">'
    + '<div class="note-c-h">'
    + '<select class="note-c-type" data-cv-note="type" data-index="' + item.index + '" aria-label="Type">'
    + typeOpts + "</select>"
    + '<input type="text" class="note-c-src" data-cv-note="source" data-index="' + item.index
    + '" value="' + escapeHtml(note.source || "") + '" placeholder="' + escapeHtml(sourcePh) + '" autocomplete="off"/>'
    + '<button type="button" class="note-ed-x" data-act="cv-note-remove" data-index="' + item.index
    + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
    + "</div>"
    + '<textarea class="disc-input" id="cvNoteVal-' + item.index + '" data-cv-note="value" data-index="' + item.index
    + '" rows="3" lang="' + escapeHtml(note.lang) + '" dir="' + dir + '" placeholder="' + escapeHtml(textPh) + '">'
    + escapeHtml(note.value || "") + "</textarea>"
    + "</div>";
}

function paintConceptNoteColumn(lang, editable) {
  const items = cvNoteOfLang(lang);
  let html = items.map((item) => paintConceptNoteCard(item, editable)).join("");
  if (editable) html += paintConceptNoteAdd(lang);
  return html;
}

function paintConceptNoteCompare(leftLang, rightLang) {
  const typeOrder = cvNoteTypes().map((type) => type.code);
  const leftItems = cvNoteOfLang(leftLang);
  const rightItems = cvNoteOfLang(rightLang);
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
    + paintNoteColHead(leftLang)
    + paintNoteColHead(rightLang)
    + "</div>";
  if (!shown.length) {
    const editor = cvNoteEl("editor");
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
        + (left ? paintConceptNoteCard(left, true) : paintNoteGhost(type))
        + (right ? paintConceptNoteCard(right, false) : paintNoteGhost(type))
        + "</div>";
    }).join("");
  }
  html += paintConceptNoteAdd(leftLang);
  return html;
}

function paintConceptNotes() {
  const tabsEl = cvNoteEl("tabs");
  const body = cvNoteEl("body");
  const editor = cvNoteEl("editor");
  if (!tabsEl || !body || !editor) return;
  const tabs = cvNoteTabs();
  if (!cvNoteState.lang && tabs[0]) cvNoteState.lang = tabs[0];
  tabsEl.innerHTML = tabs.map((code) => {
    const meta = cvNoteMeta(code);
    const count = cvNoteOfLang(code).filter((item) => cvNoteFilled(item.note)).length;
    const on = cvNoteNorm(code) === cvNoteNorm(cvNoteState.lang);
    return '<button type="button" class="note-ed-tab' + (on ? " on" : "") + '" data-act="cv-note-tab" data-lang="'
      + escapeHtml(code) + '" role="tab" aria-selected="' + (on ? "true" : "false") + '">'
      + cvNoteLangMark(meta, code)
      + '<span>' + escapeHtml(meta.name || code) + "</span>"
      + (count ? '<span class="note-ed-n">' + count + "</span>" : '<span class="note-ed-void" aria-hidden="true"></span>')
      + "</button>";
  }).join("");
  const cmpBtn = cvNoteEl("cmp");
  if (cmpBtn) {
    cmpBtn.classList.toggle("on", !!cvNoteState.compare);
    cmpBtn.hidden = tabs.length < 2;
  }
  const cmpLang = cvNoteState.compare ? cvNoteCompareLang() : "";
  if (cvNoteState.compare && cmpLang) {
    body.className = "note-cmp";
    body.innerHTML = paintConceptNoteCompare(cvNoteState.lang, cmpLang);
  } else {
    body.className = "note-ed-mono";
    body.innerHTML = paintConceptNoteColumn(cvNoteState.lang, true);
  }
  markUsedTrLangOpts("#cvNoteMenu", tabs, editor.getAttribute("data-used") || "");
  const pick = cvNoteEl("pick");
  const menu = cvNoteEl("menu");
  if (pick) pick.hidden = !menu || menu.querySelectorAll(".tree-lang-opt").length === 0;
}

var cvResState = { links: [], images: [], gps: [], bound: null, painted: false };

function cvResEditor() {
  return document.querySelector("#viewLive #cvResEditor")
      || document.getElementById("cvResEditor")
      || jsField("cvResEditor");
}

function cvResNode(id) {
  const editor = cvResEditor();
  if (editor) {
    const local = editor.querySelector("#" + id);
    if (local) return local;
  }
  return document.getElementById(id) || jsField(id);
}

function cvResAttr(name, fallback) {
  const root = cvResEditor();
  return (root && root.getAttribute(name)) || fallback || "";
}

function encodeCvResPart(value) {
  return encodeURIComponent(String(value == null ? "" : value));
}

function syncConceptResHidden() {
  const linksEl = jsField("cvResLinksPayload");
  const imgsEl = jsField("cvResImagesPayload");
  const gpsEl = jsField("cvResGpsPayload");
  if (linksEl) {
    const lines = cvResState.links.map((item) =>
      [encodeCvResPart(item.oldUri), encodeCvResPart(item.uri), encodeCvResPart(item.label)].join("\t"));
    linksEl.value = lines.length ? lines.join("\n") : "__none__";
  }
  if (imgsEl) {
    const lines = cvResState.images.map((item) =>
      [encodeCvResPart(item.id || 0), encodeCvResPart(item.uri), encodeCvResPart(item.name),
        encodeCvResPart(item.creator), encodeCvResPart(item.copyright)].join("\t"));
    imgsEl.value = lines.length ? lines.join("\n") : "__none__";
  }
  if (gpsEl) {
    const lines = cvResState.gps.map((item) =>
      [encodeCvResPart(item.lat), encodeCvResPart(item.lng)].join("\t"));
    gpsEl.value = lines.length ? lines.join("\n") : "__none__";
  }
}

function seedConceptResFromHidden() {
  const editor = cvResEditor();
  if (!editor) {
    try { destroyConceptResGpsMap(); } catch (err) { /* ignore */ }
    try { closeConceptResImgLightbox(); } catch (err) { /* ignore */ }
    cvResState = { links: [], images: [], gps: [], bound: null, painted: false };
    return;
  }
  if (cvResState.bound === editor && cvResState.painted) return;
  cvResState = { links: [], images: [], gps: [], bound: editor, painted: false };
  $$("#cvResLinkSeed span", editor).forEach((el) => {
    cvResState.links.push({
      oldUri: el.getAttribute("data-old") || "",
      uri: el.getAttribute("data-uri") || "",
      label: el.getAttribute("data-label") || ""
    });
  });
  $$("#cvResImgSeed span", editor).forEach((el) => {
    cvResState.images.push({
      id: el.getAttribute("data-id") || "0",
      uri: el.getAttribute("data-uri") || "",
      name: el.getAttribute("data-name") || "",
      creator: el.getAttribute("data-creator") || "",
      copyright: el.getAttribute("data-copy") || ""
    });
  });
  $$("#cvResGpsSeed span", editor).forEach((el) => {
    cvResState.gps.push({
      lat: el.getAttribute("data-lat") || "",
      lng: el.getAttribute("data-lng") || ""
    });
  });
  paintConceptRes();
  cvResState.painted = !!(cvResNode("cvResLinks") || cvResNode("cvResImgs") || cvResNode("cvResGpsPts"));
  syncConceptResHidden();
}

function addConceptResLink() {
  cvResState.links.push({ oldUri: "", uri: "", label: "" });
  paintConceptRes();
  cvResState.painted = true;
  syncConceptResHidden();
  requestAnimationFrame(() => {
    const input = $("#cvResUri-" + (cvResState.links.length - 1));
    if (input) input.focus();
  });
}

function addConceptResImage() {
  cvResState.images.push({ id: 0, uri: "", name: "", creator: "", copyright: "" });
  paintConceptRes();
  cvResState.painted = true;
  syncConceptResHidden();
  requestAnimationFrame(() => {
    const input = $("#cvResImgUri-" + (cvResState.images.length - 1));
    if (input) input.focus();
  });
}

function addConceptResGps() {
  cvResState.gps.push({ lat: "", lng: "" });
  paintConceptRes();
  cvResState.painted = true;
  syncConceptResHidden();
  requestAnimationFrame(() => {
    const input = $("#cvResGpsLat-" + (cvResState.gps.length - 1));
    if (input) input.focus();
  });
}

function removeConceptResLink(index) {
  cvResState.links.splice(index, 1);
  paintConceptRes();
  syncConceptResHidden();
}

function removeConceptResImage(index) {
  cvResState.images.splice(index, 1);
  paintConceptRes();
  syncConceptResHidden();
}

function removeConceptResGps(index) {
  cvResState.gps.splice(index, 1);
  paintConceptRes();
  syncConceptResHidden();
}

function setConceptResLink(index, field, value) {
  const item = cvResState.links[index];
  if (!item) return;
  if (field === "label") item.label = value;
  else item.uri = value;
  syncConceptResHidden();
}

function setConceptResImage(index, field, value) {
  const item = cvResState.images[index];
  if (!item) return;
  item[field] = value;
  if (field === "uri") {
    const preview = $("#cvResImgPreview-" + index);
    const img = preview && preview.querySelector("img");
    const ok = looksLikeUrl(value);
    if (preview) preview.hidden = !ok;
    if (img && ok) img.src = String(value || "").trim();
  }
  const cap = $("#cvResImgCap-" + index);
  if (cap) cap.textContent = draftImgCaption(cvResState.images[index]);
  syncConceptResHidden();
}

function setConceptResGps(index, field, value) {
  const item = cvResState.gps[index];
  if (item) item[field] = value;
  const btn = $("#cvResGpsGoto-" + index);
  if (btn && (field === "lat" || field === "lng")) {
    btn.setAttribute("data-" + field, String(value || "").trim());
  }
  syncConceptResHidden();
  scheduleConceptResGpsRefresh();
}

function paintConceptRes() {
  const links = cvResNode("cvResLinks");
  const images = cvResNode("cvResImgs");
  const gps = cvResNode("cvResGpsPts");
  if (!links && !images && !gps) return;
  const linkPh = escapeHtml(cvResAttr("data-link-ph", "https://…"));
  const linkLabel = escapeHtml(cvResAttr("data-link-label", "libellé optionnel"));
  const linkRemove = escapeHtml(cvResAttr("data-link-remove", ""));
  const imgPh = escapeHtml(cvResAttr("data-img-ph", "https://…"));
  const imgName = escapeHtml(cvResAttr("data-img-name", "nom"));
  const imgCreator = escapeHtml(cvResAttr("data-img-creator", "auteur"));
  const imgCopy = escapeHtml(cvResAttr("data-img-copy", "copyright"));
  const imgRemove = escapeHtml(cvResAttr("data-img-remove", ""));
  const gpsLat = escapeHtml(cvResAttr("data-gps-lat", "latitude"));
  const gpsLng = escapeHtml(cvResAttr("data-gps-lng", "longitude"));
  const gpsRemove = escapeHtml(cvResAttr("data-gps-remove", ""));
  const imgOpen = escapeHtml(cvResAttr("data-img-open", "Agrandir l'image"));
  if (links) {
    links.innerHTML = cvResState.links.map((item, index) => {
      return '<div class="media-edit-card media-edit-link">'
        + '<div class="media-edit-val">'
        + '<input type="text" class="st-input media-edit-uri" id="cvResUri-' + index + '" data-cv-res="link" data-field="uri" data-index="' + index
        + '" value="' + escapeHtml(item.uri || "") + '" placeholder="' + linkPh + '" autocomplete="off"/>'
        + '<button type="button" class="note-edit-remove" data-act="cv-res-remove" data-index="' + index
        + '" title="' + linkRemove + '" aria-label="' + linkRemove + '">×</button>'
        + "</div>"
        + '<input type="text" class="st-input" data-cv-res="link" data-field="label" data-index="' + index
        + '" value="' + escapeHtml(item.label || "") + '" placeholder="' + linkLabel + '" autocomplete="off"/>'
        + "</div>";
    }).join("");
  }
  if (images) {
    images.innerHTML = cvResState.images.map((item, index) => {
      const src = String(item.uri || "").trim();
      const show = looksLikeUrl(src);
      const cap = escapeHtml(draftImgCaption(item));
      return '<div class="media-edit-card media-edit-img is-preview">'
        + '<button type="button" class="draft-img-preview" id="cvResImgPreview-' + index + '" data-act="cv-res-img-open" data-index="' + index
        + '" title="' + imgOpen + '" aria-label="' + imgOpen + '"' + (show ? "" : " hidden=\"hidden\"") + ">"
        + '<img src="' + escapeHtml(show ? src : "") + '" alt="" onerror="this.parentNode.hidden=true"/>'
        + "</button>"
        + '<div class="draft-img-cap" id="cvResImgCap-' + index + '">' + cap + "</div>"
        + '<div class="media-edit-img-fields">'
        + '<div class="media-edit-val">'
        + '<input type="text" class="st-input media-edit-uri" id="cvResImgUri-' + index + '" data-cv-res="image" data-field="uri" data-index="' + index
        + '" value="' + escapeHtml(item.uri || "") + '" placeholder="' + imgPh + '" autocomplete="off"/>'
        + '<button type="button" class="note-edit-remove" data-act="cv-res-img-remove" data-index="' + index
        + '" title="' + imgRemove + '" aria-label="' + imgRemove + '">×</button>'
        + "</div>"
        + '<div class="media-edit-img-meta">'
        + '<input type="text" class="st-input" data-cv-res="image" data-field="name" data-index="' + index
        + '" value="' + escapeHtml(item.name || "") + '" placeholder="' + imgName + '" autocomplete="off"/>'
        + '<input type="text" class="st-input" data-cv-res="image" data-field="creator" data-index="' + index
        + '" value="' + escapeHtml(item.creator || "") + '" placeholder="' + imgCreator + '" autocomplete="off"/>'
        + '<input type="text" class="st-input" data-cv-res="image" data-field="copyright" data-index="' + index
        + '" value="' + escapeHtml(item.copyright || "") + '" placeholder="' + imgCopy + '" autocomplete="off"/>'
        + "</div></div></div>";
    }).join("");
  }
  if (gps) {
    gps.innerHTML = cvResState.gps.map((item, index) => {
      return '<li class="gps-edit-row">'
        + '<button type="button" class="gps-pt" id="cvResGpsGoto-' + index + '" data-act="cv-res-gps-goto" data-index="' + index
        + '" data-lat="' + escapeHtml(item.lat || "") + '" data-lng="' + escapeHtml(item.lng || "") + '">'
        + '<span class="gps-pt-n" aria-hidden="true">' + (index + 1) + "</span></button>"
        + '<input type="text" class="st-input is-mono gps-edit-coord" id="cvResGpsLat-' + index + '" data-cv-res="gps" data-field="lat" data-index="' + index
        + '" value="' + escapeHtml(item.lat || "") + '" placeholder="' + gpsLat + '" autocomplete="off"/>'
        + '<input type="text" class="st-input is-mono gps-edit-coord" id="cvResGpsLng-' + index + '" data-cv-res="gps" data-field="lng" data-index="' + index
        + '" value="' + escapeHtml(item.lng || "") + '" placeholder="' + gpsLng + '" autocomplete="off"/>'
        + '<button type="button" class="note-edit-remove" data-act="cv-res-gps-remove" data-index="' + index
        + '" title="' + gpsRemove + '" aria-label="' + gpsRemove + '">×</button>'
        + "</li>";
    }).join("");
  }
  scheduleConceptResGpsRefresh();
}

function cvResVisibleImages() {
  return cvResState.images
    .map((item, index) => ({ item: item, index: index }))
    .filter((row) => looksLikeUrl(row.item.uri));
}

var cvResImgView = { index: 0 };

function closeConceptResImgLightbox() {
  const box = $("#cvResImgLightbox");
  if (!box || box.hidden) return false;
  box.hidden = true;
  box.setAttribute("aria-hidden", "true");
  document.body.classList.remove("is-img-lightbox");
  return true;
}

function showConceptResImgAt(index) {
  const items = cvResVisibleImages();
  if (!items.length) return;
  const n = items.length;
  cvResImgView.index = ((index % n) + n) % n;
  const row = items[cvResImgView.index];
  const src = String(row.item.uri || "").trim();
  const img = $("#cvResImgLightboxImg");
  if (img) {
    img.src = src;
    img.alt = row.item.name || "";
  }
  const cap = $("#cvResImgLightboxCap");
  if (cap) cap.textContent = draftImgCaption(row.item);
  const orig = $("#cvResImgLightboxOrig");
  if (orig) orig.href = src || "#";
  const box = $("#cvResImgLightbox");
  if (box) box.classList.toggle("is-single", n < 2);
}

function openConceptResImgLightbox(cardIndex) {
  const items = cvResVisibleImages();
  if (!items.length) return;
  const pos = items.findIndex((row) => row.index === Number(cardIndex));
  cvResImgView.index = pos >= 0 ? pos : 0;
  const box = $("#cvResImgLightbox");
  if (!box) return;
  document.body.classList.add("is-img-lightbox");
  box.hidden = false;
  box.setAttribute("aria-hidden", "false");
  showConceptResImgAt(cvResImgView.index);
}

var cvResGpsView = { map: null, layer: null, el: null, ignoreClick: false };

function cvResGpsPoints() {
  return cvResState.gps.map((item, index) => ({
    index: index,
    lat: parseFloat(String(item.lat || "").replace(",", ".")),
    lng: parseFloat(String(item.lng || "").replace(",", "."))
  })).filter((pt) => Number.isFinite(pt.lat) && Number.isFinite(pt.lng));
}

function destroyConceptResGpsMap() {
  try {
    if (cvResGpsView.layer && cvResGpsView.map) {
      cvResGpsView.map.removeLayer(cvResGpsView.layer);
    }
  } catch (err) { /* ignore */ }
  try {
    if (cvResGpsView.map) cvResGpsView.map.remove();
  } catch (err) { /* ajax already replaced the map container */ }
  cvResGpsView.map = null;
  cvResGpsView.layer = null;
  cvResGpsView.el = null;
}

function ensureConceptResGpsMap() {
  const canvas = cvResNode("cvResGpsCanvas") || $("#cvResGpsCanvas");
  if (!canvas || !window.L) return null;
  if (cvResGpsView.map && cvResGpsView.el === canvas) return cvResGpsView.map;
  destroyConceptResGpsMap();
  cvResGpsView.el = canvas;
  cvResGpsView.map = L.map(canvas, {
    scrollWheelZoom: false,
    doubleClickZoom: false,
    attributionControl: true
  });
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "© OpenStreetMap"
  }).addTo(cvResGpsView.map);
  cvResGpsView.map.setView([46.7, 2.5], 5);
  cvResGpsView.map.on("dragstart", () => { cvResGpsView.ignoreClick = true; });
  cvResGpsView.map.on("dragend", () => {
    window.setTimeout(() => { cvResGpsView.ignoreClick = false; }, 220);
  });
  cvResGpsView.map.on("click", (e) => {
    if (cvResGpsView.ignoreClick) return;
    const lat = formatGpsNum(e.latlng.lat);
    const lng = formatGpsNum(e.latlng.lng);
    const last = cvResState.gps[cvResState.gps.length - 1];
    if (last && !String(last.lat || "").trim() && !String(last.lng || "").trim()) {
      last.lat = lat;
      last.lng = lng;
    } else {
      cvResState.gps.push({ lat: lat, lng: lng });
    }
    paintConceptRes();
    syncConceptResHidden();
  });
  return cvResGpsView.map;
}

function refreshConceptResGpsMap() {
  const badge = cvResNode("cvResGpsBadge") || $("#cvResGpsBadge");
  const points = cvResGpsPoints();
  if (badge) {
    badge.textContent = points.length
      ? (points.length + " point" + (points.length > 1 ? "s" : ""))
      : cvResAttr("data-gps-empty", "Cliquez pour poser un point");
  }
  const canvas = cvResNode("cvResGpsCanvas") || $("#cvResGpsCanvas");
  if (!canvas) return;
  ensureLeaflet().then(() => {
    const map = ensureConceptResGpsMap();
    if (!map) return;
    if (cvResGpsView.layer) {
      map.removeLayer(cvResGpsView.layer);
      cvResGpsView.layer = null;
    }
    const layer = L.layerGroup().addTo(map);
    cvResGpsView.layer = layer;
    const accent = gpsAccent();
    if (points.length >= 2) {
      L.polyline(points.map((pt) => [pt.lat, pt.lng]), { color: accent, weight: 2.5 }).addTo(layer);
    }
    points.forEach((pt) => {
      const marker = L.marker([pt.lat, pt.lng], {
        draggable: true,
        icon: L.divIcon({
          className: "gps-pin",
          html: '<span class="gps-pin-n">' + (pt.index + 1) + "</span>",
          iconSize: [22, 22],
          iconAnchor: [11, 11]
        })
      }).addTo(layer);
      marker.on("click", () => gotoConceptResGps(pt.index));
      marker.on("dragstart", () => { cvResGpsView.ignoreClick = true; });
      marker.on("dragend", (ev) => {
        const next = ev.target.getLatLng();
        const row = cvResState.gps[pt.index];
        if (row) {
          row.lat = formatGpsNum(next.lat);
          row.lng = formatGpsNum(next.lng);
        }
        const latIn = $("#cvResGpsLat-" + pt.index);
        const lngIn = $("#cvResGpsLng-" + pt.index);
        if (latIn) latIn.value = formatGpsNum(next.lat);
        if (lngIn) lngIn.value = formatGpsNum(next.lng);
        window.setTimeout(() => { cvResGpsView.ignoreClick = false; }, 220);
        syncConceptResHidden();
        scheduleConceptResGpsRefresh();
      });
    });
    if (!points.length) map.setView([46.7, 2.5], 5);
    else if (points.length === 1) map.setView([points[0].lat, points[0].lng], 14);
    else map.fitBounds(L.latLngBounds(points.map((pt) => [pt.lat, pt.lng])).pad(0.14));
    requestAnimationFrame(() => map.invalidateSize());
    setTimeout(() => map.invalidateSize(), 260);
  }).catch(() => {
    if (canvas) {
      canvas.classList.add("is-off");
      canvas.textContent = "Carte indisponible";
    }
  });
}

function scheduleConceptResGpsRefresh() {
  clearTimeout(scheduleConceptResGpsRefresh._t);
  scheduleConceptResGpsRefresh._t = setTimeout(refreshConceptResGpsMap, 40);
}

function gotoConceptResGps(index) {
  const item = cvResState.gps[index];
  if (!item || !cvResGpsView.map) return;
  const lat = parseFloat(String(item.lat || "").replace(",", "."));
  const lng = parseFloat(String(item.lng || "").replace(",", "."));
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
  cvResGpsView.map.panTo([lat, lng]);
}

window.onLabelSave = function (data) {
  const btns = document.querySelectorAll(
      "[id$='labelEditSave'], #labelSaveConfirm .abt-save, #collSaveConfirm .abt-save, #relSaveConfirm .abt-save, #crelSaveConfirm .abt-save, #trEditSave, #fcTrEditSave, #fcLabelEditSave, #fcMemEditSave, #fcNoteEditSave, #noteEditSave, #resEditSave, #relEditSave, #resLinkSaveConfirm .abt-save, #resImgSaveConfirm .abt-save, #resGpsSaveConfirm .abt-save, .cblock-edit-warn .abt-save"
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

document.addEventListener("toggle", (event) => {
  const menu = event.target && event.target.classList && event.target.classList.contains("align-type-menu")
    ? event.target
    : null;
  if (!menu || !menu.open) return;
  document.querySelectorAll(".align-type-menu[open]").forEach((el) => {
    if (el !== menu) el.removeAttribute("open");
  });
}, true);

document.addEventListener("click", (event) => {
  if (event.target.closest && event.target.closest(".align-type-menu")) return;
  document.querySelectorAll(".align-type-menu[open]").forEach((el) => el.removeAttribute("open"));
});

window.onAlignAutoSearch = function (data) {
  const panel = document.querySelector("#viewLive .align-auto");
  const actions = document.querySelector("#viewLive .align-auto-actions");
  if (data.status === "begin") {
    if (panel) panel.classList.add("is-searching");
    if (actions) actions.classList.add("is-searching");
    const trigger = data.source;
    const card = trigger && trigger.closest ? trigger.closest(".align-src-card") : null;
    if (panel && card) {
      panel.querySelectorAll(".align-src-card").forEach((el) => el.classList.remove("sel", "is-loading"));
      card.classList.add("sel", "is-loading");
      const name = card.querySelector(".asc-name");
      const dest = panel.querySelector(".align-res-load-src");
      if (name && dest) dest.textContent = name.textContent.trim();
    }
  }
  if (data.status === "success" || data.status === "complete") {
    if (panel) panel.classList.remove("is-searching");
    if (actions) actions.classList.remove("is-searching");
  }
}

