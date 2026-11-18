/** Fiche — traductions. */
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

var cvTrState = { order: [], values: {}, alts: {}, confirm: "" };

function cvTrRoot() {
  if (cvTrState.root && cvTrState.root.isConnected) return cvTrState.root;
  return document.getElementById("cvTrEditor");
}

function cvTrEl(name) {
  const root = cvTrRoot();
  if (!root) return null;
  if (name === "editor") return root;
  const byData = root.querySelector("[data-cv='" + name + "']");
  if (byData) return byData;
  const ids = {
    list: "cvTrList",
    empty: "cvTrEmpty",
    seed: "cvTrSeed",
    btn: "cvTrBtn",
    menu: "cvTrMenu",
    pick: "cvTrPick",
    payload: "cvTrPayload"
  };
  const id = ids[name];
  return id ? root.querySelector("#" + id) : null;
}

function focusTrEditor(node) {
  const editor = node && node.closest && node.closest("#cvTrEditor, #collTrEditor");
  if (!editor) return null;
  if (cvTrState.bound !== editor) seedConceptTrFromHidden(editor);
  else cvTrState.root = editor;
  return editor;
}

function trTextDir(code) {
  return /^(ar|he|fa|ur)(-|$)/i.test(code || "") ? "rtl" : "ltr";
}

function jsField(id) {
  return document.getElementById(id)
    || document.getElementById("previewDetailForm:" + id)
    || document.querySelector("[id$='" + id + "']");
}

function trLangAlready(order, code) {
  const key = String(code || "").trim().toLowerCase();
  if (!key) return false;
  return (order || []).some((item) => String(item || "").trim().toLowerCase() === key);
}

function markUsedTrLangOpts(menuSel, order, usedTitle) {
  const scoped = menuSel === "#cvNoteMenu" ? cvNoteEl("menu")
    : menuSel === "#cvTrMenu" ? cvTrEl("menu") : null;
  const opts = scoped
    ? [...scoped.querySelectorAll(".tree-lang-opt")]
    : $$(menuSel + " .tree-lang-opt");
  opts.forEach((opt) => {
    const used = trLangAlready(order, opt.getAttribute("data-lang"));
    opt.hidden = false;
    opt.disabled = used;
    opt.classList.toggle("is-used", used);
    opt.setAttribute("aria-disabled", used ? "true" : "false");
    if (used && usedTitle) opt.setAttribute("title", usedTitle);
    else opt.removeAttribute("title");
  });
  return opts;
}

function cvTrOpt(code) {
  const menu = cvTrEl("menu");
  return menu && menu.querySelector('.tree-lang-opt[data-lang="' + code + '"]');
}

function cvTrMeta(code) {
  const opt = cvTrOpt(code);
  return {
    code: code,
    flag: (opt && (opt.getAttribute("data-flag") || "").trim()) || "",
    name: (opt && (opt.getAttribute("data-name") || "").trim()) || code
  };
}

function closeConceptTrLang() {
  const btn = cvTrEl("btn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function setConceptTrLangOpen(open) {
  const btn = cvTrEl("btn");
  if (!btn) return;
  btn.classList.toggle("is-open", !!open);
  btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function syncConceptTrHidden() {
  const el = cvTrEl("payload") || jsField("cvTrPayload");
  if (!el) return;
  el.value = cvTrState.order.map((code) => {
    return code + "\t"
      + String(cvTrState.values[code] || "").replace(/\r?\n/g, " ")
      + "\t"
      + String(cvTrState.alts[code] || "").replace(/\r?\n/g, " ");
  }).join("\n");
}

function seedConceptTrFromHidden(editor) {
  editor = editor && editor.nodeType === 1 ? editor : document.getElementById("cvTrEditor");
  if (!editor) return;
  if (cvTrState.bound === editor) return;
  cvTrState = { order: [], values: {}, alts: {}, confirm: "", bound: editor, root: editor };
  const seed = cvTrEl("seed");
  (seed ? [...seed.querySelectorAll("[data-lang]")] : []).forEach((el) => {
    const code = (el.getAttribute("data-lang") || "").trim();
    if (!code || cvTrState.order.indexOf(code) >= 0) return;
    cvTrState.order.push(code);
    cvTrState.values[code] = el.getAttribute("data-value") || "";
    cvTrState.alts[code] = el.getAttribute("data-alts") || "";
  });
  if (!cvTrState.order.length) {
    const hidden = cvTrEl("payload") || jsField("cvTrPayload");
    String((hidden && hidden.value) || "").split(/\r?\n/).forEach((line) => {
      const parts = line.split("\t");
      const code = (parts[0] || "").trim();
      if (!code || cvTrState.order.indexOf(code) >= 0) return;
      cvTrState.order.push(code);
      cvTrState.values[code] = parts[1] || "";
      cvTrState.alts[code] = parts.slice(2).join("\t");
    });
  }
  syncConceptTrHidden();
  paintConceptTr();
}

function addConceptTr(code) {
  if (!code || trLangAlready(cvTrState.order, code)) return;
  cvTrState.order.push(code);
  if (cvTrState.values[code] == null) cvTrState.values[code] = "";
  if (cvTrState.alts[code] == null) cvTrState.alts[code] = "";
  cvTrState.confirm = "";
  closeConceptTrLang();
  syncConceptTrHidden();
  paintConceptTr();
  requestAnimationFrame(() => {
    const root = cvTrRoot();
    const input = root && root.querySelector('[data-cv-tr="value"][data-lang="' + code + '"]');
    if (input) input.focus();
  });
}

function askRemoveConceptTr(code) {
  cvTrState.confirm = code || "";
  paintConceptTr();
}

function keepConceptTr() {
  cvTrState.confirm = "";
  paintConceptTr();
}

function removeConceptTr(code) {
  cvTrState.order = cvTrState.order.filter((item) => item !== code);
  cvTrState.values[code] = "";
  cvTrState.alts[code] = "";
  cvTrState.confirm = "";
  syncConceptTrHidden();
  paintConceptTr();
}

function setConceptTrValue(code, value) {
  cvTrState.values[code] = value;
  syncConceptTrHidden();
}

function setConceptTrAlt(code, value) {
  cvTrState.alts[code] = value;
  syncConceptTrHidden();
}

function paintConceptTr() {
  const list = cvTrEl("list");
  const empty = cvTrEl("empty");
  const editor = cvTrEl("editor");
  if (!list || !editor) return;
  const ph = editor.getAttribute("data-ph") || "Nom en {0}…";
  const altPh = editor.getAttribute("data-alt") || "";
  const remove = editor.getAttribute("data-remove") || "";
  const drop = editor.getAttribute("data-drop") || "";
  const no = editor.getAttribute("data-no") || "Non";
  const yes = editor.getAttribute("data-yes") || "Oui";
  if (empty) empty.hidden = cvTrState.order.length > 0;
  list.innerHTML = cvTrState.order.map((code) => {
    const meta = cvTrMeta(code);
    const placeholder = escapeHtml(ph.replace("{0}", String(meta.name || code).toLowerCase()));
    const confirming = cvTrState.confirm === code;
    const dir = trTextDir(code);
    const actions = confirming
      ? '<span class="te-confirm">' + escapeHtml(drop)
        + '<button type="button" class="bo-btn ghost sm" data-act="cv-tr-keep">' + escapeHtml(no) + "</button>"
        + '<button type="button" class="bo-btn primary sm" data-act="cv-tr-drop" data-lang="' + code + '">' + escapeHtml(yes) + "</button>"
        + "</span>"
      : '<button type="button" class="re-x te-x" data-act="cv-tr-ask" data-lang="' + code
        + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>';
    const flag = meta.flag && meta.flag !== "🏳️"
      ? '<span class="tr-flag">' + meta.flag + "</span>"
      : '<span class="tr-code">' + escapeHtml(code) + "</span>";
    return '<div class="te-lang">'
      + '<div class="te-head">'
      + flag
      + '<span class="te-name">' + escapeHtml(meta.name) + "</span>"
      + '<span class="te-code">' + escapeHtml(code) + "</span>"
      + actions
      + "</div>"
      + '<input type="text" class="st-input"' + (editor.id === "collTrEditor" ? "" : ' id="cvTrVal-' + code + '"')
      + ' lang="' + code + '" dir="' + dir
      + '" data-cv-tr="value" data-lang="' + code + '" value="' + escapeHtml(cvTrState.values[code] || "")
      + '" placeholder="' + placeholder + '" autocomplete="off"/>'
      + (altPh
        ? '<input type="text" class="st-input te-alt" lang="' + code + '" dir="' + dir + '" data-cv-tr="alt" data-lang="' + code
          + '" value="' + escapeHtml(cvTrState.alts[code] || "") + '" placeholder="' + escapeHtml(altPh) + '" autocomplete="off"/>'
        : "")
      + "</div>";
  }).join("");
  markUsedTrLangOpts("#cvTrMenu", cvTrState.order, editor.getAttribute("data-used") || "");
  const pick = cvTrEl("pick");
  const menu = cvTrEl("menu");
  if (pick) pick.hidden = !menu || menu.querySelectorAll(".tree-lang-opt").length === 0;
}

var fcTrState = { order: [], values: {}, confirm: "" };

function fcTrOpt(code) {
  return $("#fcTrMenu") && $("#fcTrMenu").querySelector('.tree-lang-opt[data-lang="' + code + '"]');
}

function fcTrMeta(code) {
  const opt = fcTrOpt(code);
  return {
    code: code,
    flag: (opt && (opt.getAttribute("data-flag") || "").trim()) || "",
    name: (opt && (opt.getAttribute("data-name") || "").trim()) || code
  };
}

function closeFacetCardTrLang() {
  const btn = $("#fcTrBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function setFacetCardTrLangOpen(open) {
  const btn = $("#fcTrBtn");
  if (!btn) return;
  btn.classList.toggle("is-open", !!open);
  btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function syncFacetCardTrHidden() {
  const el = jsField("fcTrPayload");
  if (!el) return;
  el.value = fcTrState.order.map((code) => {
    return code + "\t" + String(fcTrState.values[code] || "").replace(/\r?\n/g, " ");
  }).join("\n");
}

function seedFacetCardTrFromHidden() {
  if (!$("#fcTrEditor")) return;
  fcTrState = { order: [], values: {}, confirm: "" };
  $$("#fcTrSeed [data-lang]").forEach((el) => {
    const code = (el.getAttribute("data-lang") || "").trim();
    if (!code || fcTrState.order.indexOf(code) >= 0) return;
    fcTrState.order.push(code);
    fcTrState.values[code] = el.getAttribute("data-value") || "";
  });
  if (!fcTrState.order.length) {
    String((jsField("fcTrPayload") && jsField("fcTrPayload").value) || "").split(/\r?\n/).forEach((line) => {
      const parts = line.split("\t");
      const code = (parts[0] || "").trim();
      if (!code || fcTrState.order.indexOf(code) >= 0) return;
      fcTrState.order.push(code);
      fcTrState.values[code] = parts.slice(1).join("\t");
    });
  }
  syncFacetCardTrHidden();
  paintFacetCardTr();
}

function addFacetCardTr(code) {
  if (!code || trLangAlready(fcTrState.order, code)) return;
  fcTrState.order.push(code);
  if (fcTrState.values[code] == null) fcTrState.values[code] = "";
  fcTrState.confirm = "";
  closeFacetCardTrLang();
  syncFacetCardTrHidden();
  paintFacetCardTr();
  requestAnimationFrame(() => {
    const input = $("#fcTrVal-" + code);
    if (input) input.focus();
  });
}

function askRemoveFacetCardTr(code) {
  fcTrState.confirm = code || "";
  paintFacetCardTr();
}

function keepFacetCardTr() {
  fcTrState.confirm = "";
  paintFacetCardTr();
}

function removeFacetCardTr(code) {
  fcTrState.order = fcTrState.order.filter((item) => item !== code);
  fcTrState.values[code] = "";
  fcTrState.confirm = "";
  syncFacetCardTrHidden();
  paintFacetCardTr();
}

function setFacetCardTrValue(code, value) {
  fcTrState.values[code] = value;
  syncFacetCardTrHidden();
}

function paintFacetCardTr() {
  const list = $("#fcTrList");
  const empty = $("#fcTrEmpty");
  const editor = $("#fcTrEditor");
  if (!list || !editor) return;
  const ph = editor.getAttribute("data-ph") || "Nom en {0}…";
  const remove = editor.getAttribute("data-remove") || "";
  const drop = editor.getAttribute("data-drop") || "";
  const no = editor.getAttribute("data-no") || "Non";
  const yes = editor.getAttribute("data-yes") || "Oui";
  if (empty) empty.hidden = fcTrState.order.length > 0;
  list.innerHTML = fcTrState.order.map((code) => {
    const meta = fcTrMeta(code);
    const placeholder = escapeHtml(ph.replace("{0}", String(meta.name || code).toLowerCase()));
    const confirming = fcTrState.confirm === code;
    const dir = trTextDir(code);
    const actions = confirming
      ? '<span class="te-confirm">' + escapeHtml(drop)
        + '<button type="button" class="bo-btn ghost sm" data-act="fc-tr-keep">' + escapeHtml(no) + "</button>"
        + '<button type="button" class="bo-btn primary sm" data-act="fc-tr-drop" data-lang="' + code + '">' + escapeHtml(yes) + "</button>"
        + "</span>"
      : '<button type="button" class="re-x te-x" data-act="fc-tr-ask" data-lang="' + code
        + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>';
    const flag = meta.flag && meta.flag !== "🏳️"
      ? '<span class="tr-flag">' + meta.flag + "</span>"
      : '<span class="tr-code">' + escapeHtml(code) + "</span>";
    return '<div class="te-lang">'
      + '<div class="te-head">'
      + flag
      + '<span class="te-name">' + escapeHtml(meta.name) + "</span>"
      + '<span class="te-code">' + escapeHtml(code) + "</span>"
      + actions
      + "</div>"
      + '<input type="text" class="st-input" id="fcTrVal-' + code + '" lang="' + code + '" dir="' + dir
      + '" data-lang="' + code + '" value="' + escapeHtml(fcTrState.values[code] || "")
      + '" placeholder="' + placeholder + '" autocomplete="off"/>'
      + "</div>";
  }).join("");
  markUsedTrLangOpts("#fcTrMenu", fcTrState.order, editor.getAttribute("data-used") || "");
  const pick = $("#fcTrPick");
  if (pick) pick.hidden = $$("#fcTrMenu .tree-lang-opt").length === 0;
}

var fcMemState = { items: [], hits: [], seq: 0, timer: null };
var fcNoteState = { notes: [], openLang: -1 };

function syncFacetCardMemHidden() {
  const el = jsField("fcMembersPayload");
  if (!el) return;
  el.value = fcMemState.items.map((item) => (item.id || "") + "\t" + (item.label || "")).join("\n");
}

function seedFacetCardMemFromHidden() {
  if (!$("#fcMemEditor")) return;
  fcMemState = { items: [], hits: [], seq: 0, timer: null };
  $$("#fcMemSeed [data-id]").forEach((el) => {
    const id = (el.getAttribute("data-id") || "").trim();
    if (!id || fcMemState.items.some((item) => item.id === id)) return;
    fcMemState.items.push({ id: id, label: el.getAttribute("data-label") || id });
  });
  if (!fcMemState.items.length) {
    String((jsField("fcMembersPayload") && jsField("fcMembersPayload").value) || "").split(/\r?\n/).forEach((line) => {
      const tab = line.indexOf("\t");
      const id = (tab < 0 ? line : line.slice(0, tab)).trim();
      const label = tab < 0 ? id : line.slice(tab + 1).trim();
      if (!id || fcMemState.items.some((item) => item.id === id)) return;
      fcMemState.items.push({ id: id, label: label || id });
    });
  }
  const q = $("#fcMemQ");
  if (q) q.value = "";
  hideFacetCardMemDrop();
  syncFacetCardMemHidden();
  paintFacetCardMem();
}

function hideFacetCardMemDrop() {
  const drop = $("#fcMemDrop");
  const empty = $("#fcMemEmpty");
  if (drop) {
    drop.hidden = true;
    drop.innerHTML = "";
  }
  if (empty) empty.hidden = true;
}

function setFacetCardMemMsg(text) {
  const el = $("#fcMemMsg");
  if (!el) return;
  el.hidden = !text;
  el.textContent = text || "";
}

function facetCardMemAlready(id, label) {
  return fcMemState.items.some((item) => item.id === id || (label && item.label === label));
}

function addFacetCardMem(id, label) {
  if (!id) return;
  const editor = $("#fcMemEditor");
  if (facetCardMemAlready(id, label)) {
    setFacetCardMemMsg((editor && editor.getAttribute("data-exists")) || "");
    return;
  }
  fcMemState.items.push({ id: id, label: label || id });
  const q = $("#fcMemQ");
  if (q) q.value = "";
  fcMemState.hits = [];
  hideFacetCardMemDrop();
  setFacetCardMemMsg("");
  syncFacetCardMemHidden();
  paintFacetCardMem();
}

function removeFacetCardMem(index) {
  fcMemState.items.splice(Number(index), 1);
  setFacetCardMemMsg("");
  syncFacetCardMemHidden();
  paintFacetCardMem();
}

function paintFacetCardMem() {
  const rows = $("#fcMemRows");
  const editor = $("#fcMemEditor");
  if (!rows || !editor) return;
  const remove = editor.getAttribute("data-remove") || "";
  rows.innerHTML = fcMemState.items.map((item, index) => (
    '<div class="re-row">'
    + '<span class="re-lbl">' + escapeHtml(item.label || item.id) + "</span>"
    + '<button type="button" class="re-x" data-act="fc-mem-remove" data-index="' + index
    + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
    + "</div>"
  )).join("");
}

function searchFacetCardMem(q) {
  const seq = ++fcMemState.seq;
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
    if (seq !== fcMemState.seq) return;
    fcMemState.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id) : "";
      const label = item && item.label ? String(item.label) : "";
      return id && !facetCardMemAlready(id, label);
    }).slice(0, 6);
    paintFacetCardMemHits(q);
  }).catch(() => {
    if (seq !== fcMemState.seq) return;
    fcMemState.hits = [];
    paintFacetCardMemHits(q);
  });
}

function paintFacetCardMemHits(q) {
  const drop = $("#fcMemDrop");
  const empty = $("#fcMemEmpty");
  if (!drop) return;
  if (fcMemState.hits.length) {
    drop.hidden = false;
    if (empty) empty.hidden = true;
    drop.innerHTML = fcMemState.hits.map((hit) => (
      '<button type="button" class="re-hit" data-act="fc-mem-hit" data-id="'
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

function scheduleFacetCardMemSearch() {
  const q = ($("#fcMemQ") && $("#fcMemQ").value.trim()) || "";
  setFacetCardMemMsg("");
  if (fcMemState.timer) clearTimeout(fcMemState.timer);
  if (q.length < 2) {
    fcMemState.seq += 1;
    fcMemState.hits = [];
    hideFacetCardMemDrop();
    return;
  }
  fcMemState.timer = setTimeout(() => searchFacetCardMem(q), 220);
}

function fcNoteTypes() {
  return $$("#fcNoteTypeOpts [data-code]").map((el) => ({
    code: el.getAttribute("data-code") || "",
    label: el.getAttribute("data-label") || el.getAttribute("data-code") || ""
  })).filter((item) => item.code);
}

function fcNoteLangs() {
  return $$("#fcNoteLangOpts [data-code]").map((el) => ({
    code: el.getAttribute("data-code") || "",
    name: el.getAttribute("data-name") || el.getAttribute("data-code") || "",
    flag: el.getAttribute("data-flag") || ""
  })).filter((item) => item.code);
}

function fcNoteUsedKeys(exceptIndex) {
  const used = {};
  fcNoteState.notes.forEach((note, index) => {
    if (index === exceptIndex) return;
    used[note.type + "\t" + note.lang] = true;
  });
  return used;
}

function firstFreeFacetCardNote() {
  const types = fcNoteTypes();
  const langs = fcNoteLangs();
  const used = fcNoteUsedKeys(-1);
  const work = ($("#fcNoteEditor") && $("#fcNoteEditor").getAttribute("data-work-lang")) || (langs[0] && langs[0].code) || "fr";
  const tryLangs = [work].concat(langs.map((lang) => lang.code).filter((code) => code !== work));
  for (let i = 0; i < types.length; i++) {
    for (let j = 0; j < tryLangs.length; j++) {
      if (!used[types[i].code + "\t" + tryLangs[j]]) {
        return { type: types[i].code, lang: tryLangs[j] };
      }
    }
  }
  return null;
}

function syncFacetCardNotesHidden() {
  const el = jsField("fcNotesPayload");
  if (!el) return;
  el.value = fcNoteState.notes.map((note) => {
    return [note.type || "", note.lang || "", encodeURIComponent(note.value || ""), encodeURIComponent(note.source || "")].join("\t");
  }).join("\n");
}

function decodeNotePart(raw) {
  const value = String(raw == null ? "" : raw);
  try {
    return decodeURIComponent(value);
  } catch (err) {
    return value;
  }
}

function seedFacetCardNotesFromHidden() {
  if (!$("#fcNoteEditor")) return;
  fcNoteState = { notes: [], openLang: -1 };
  $$("#fcNoteSeed [data-lang]").forEach((el) => {
    const type = (el.getAttribute("data-type") || "").trim();
    const lang = (el.getAttribute("data-lang") || "").trim();
    if (!type || !lang) return;
    fcNoteState.notes.push({
      type: type,
      lang: lang,
      value: el.getAttribute("data-value") || "",
      source: el.getAttribute("data-source") || ""
    });
  });
  if (!fcNoteState.notes.length) {
    String((jsField("fcNotesPayload") && jsField("fcNotesPayload").value) || "").split(/\r?\n/).forEach((line) => {
      const parts = line.split("\t");
      const type = (parts[0] || "").trim();
      const lang = (parts[1] || "").trim();
      if (!type || !lang) return;
      fcNoteState.notes.push({
        type: type,
        lang: lang,
        value: decodeNotePart(parts[2] || ""),
        source: decodeNotePart(parts[3] || "")
      });
    });
  }
  syncFacetCardNotesHidden();
  paintFacetCardNotes();
}

function addFacetCardNote() {
  const next = firstFreeFacetCardNote();
  if (!next) return;
  fcNoteState.notes.push({ type: next.type, lang: next.lang, value: "", source: "" });
  fcNoteState.openLang = -1;
  syncFacetCardNotesHidden();
  paintFacetCardNotes();
  requestAnimationFrame(() => {
    const input = $("#fcNoteVal-" + (fcNoteState.notes.length - 1));
    if (input) input.focus();
  });
}

function removeFacetCardNote(index) {
  fcNoteState.notes.splice(index, 1);
  fcNoteState.openLang = -1;
  syncFacetCardNotesHidden();
  paintFacetCardNotes();
}

function setFacetCardNoteField(index, field, value) {
  const note = fcNoteState.notes[index];
  if (!note || !field) return;
  note[field] = value;
  syncFacetCardNotesHidden();
}

function setFacetCardNoteLang(index, code) {
  const note = fcNoteState.notes[index];
  if (!note || !code) return;
  note.lang = code;
  fcNoteState.openLang = -1;
  syncFacetCardNotesHidden();
  paintFacetCardNotes();
}

function closeFacetCardNoteLang() {
  if (fcNoteState.openLang < 0) return false;
  fcNoteState.openLang = -1;
  paintFacetCardNotes();
  return true;
}

function setFacetCardNoteLangOpen(index) {
  fcNoteState.openLang = fcNoteState.openLang === index ? -1 : index;
  paintFacetCardNotes();
}

function paintFacetCardNotes() {
  const list = $("#fcNoteList");
  const add = $("#fcNoteAdd");
  const editor = $("#fcNoteEditor");
  if (!list || !editor) return;
  const empty = editor.getAttribute("data-empty") || "";
  const remove = editor.getAttribute("data-remove") || "";
  const textPh = editor.getAttribute("data-text-ph") || "";
  const sourcePh = editor.getAttribute("data-source-ph") || "";
  const types = fcNoteTypes();
  const langs = fcNoteLangs();
  if (!fcNoteState.notes.length) {
    list.innerHTML = empty ? '<div class="re-none">' + escapeHtml(empty) + "</div>" : "";
  } else {
    list.innerHTML = fcNoteState.notes.map((note, index) => {
      const used = fcNoteUsedKeys(index);
      const typeOpts = types.filter((type) => type.code === note.type || !used[type.code + "\t" + note.lang])
        .map((type) => '<option value="' + escapeHtml(type.code) + '"' + (type.code === note.type ? " selected" : "") + ">"
          + escapeHtml(type.label) + "</option>").join("");
      const lang = langs.find((item) => item.code === note.lang) || { code: note.lang, name: note.lang, flag: "" };
      const open = fcNoteState.openLang === index;
      const langOpts = langs.filter((item) => item.code === note.lang || !used[note.type + "\t" + item.code])
        .map((item) => '<button type="button" class="tree-lang-opt' + (item.code === note.lang ? " is-on" : "")
          + '" data-act="fc-note-lang" data-index="' + index + '" data-lang="' + escapeHtml(item.code) + '" role="option">'
          + '<span class="tree-lang-opt-flag" aria-hidden="true">' + (item.flag || "") + "</span>"
          + '<span class="tree-lang-opt-name">' + escapeHtml(item.name) + "</span>"
          + '<span class="tree-lang-opt-code">' + escapeHtml(item.code) + "</span>"
          + '<span class="tree-lang-opt-check" aria-hidden="true">✓</span></button>').join("");
      return '<div class="note-edit-card">'
        + '<div class="note-edit-head">'
        + '<select class="st-input note-type-select" data-fc-note="type" data-index="' + index + '" aria-label="Type">'
        + typeOpts + "</select>"
        + '<div class="tp-lang-pick draft-note-lang fc-note-lang' + (open ? " is-open" : "") + '">'
        + '<button type="button" class="tree-lang-btn tp-lang-btn draft-note-lang-btn' + (open ? " is-open" : "")
        + '" data-act="fc-note-lang-toggle" data-index="' + index + '" aria-haspopup="listbox" aria-expanded="' + (open ? "true" : "false") + '">'
        + '<span class="tree-lang-flag" aria-hidden="true">' + (lang.flag || "🏳️") + "</span>"
        + '<span class="tree-lang-name">' + escapeHtml(lang.name) + "</span>"
        + '<span class="tree-lang-code">' + escapeHtml(lang.code) + "</span>"
        + '<span class="tree-lang-caret" aria-hidden="true">▾</span></button>'
        + (open ? '<div class="tree-lang-menu tp-lang-menu" role="listbox">' + langOpts + "</div>" : "")
        + "</div>"
        + '<button type="button" class="note-edit-remove" data-act="fc-note-remove" data-index="' + index
        + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
        + "</div>"
        + '<textarea class="st-input note-edit-value disc-input" id="fcNoteVal-' + index + '" data-fc-note="value" data-index="' + index
        + '" rows="3" placeholder="' + escapeHtml(textPh) + '">' + escapeHtml(note.value || "") + "</textarea>"
        + '<input type="text" class="st-input draft-note-source" data-fc-note="source" data-index="' + index
        + '" value="' + escapeHtml(note.source || "") + '" placeholder="' + escapeHtml(sourcePh) + '" autocomplete="off"/>'
        + "</div>";
    }).join("");
  }
  if (add) add.hidden = !firstFreeFacetCardNote();
}

