/**
 * OpenTheso — collections : arbre et fiche.
 */
"use strict";

var collectionTreeState = { key: "", loading: false };
var collectionFiche = null;
var collectionNotesAllLangs = false;
var collectionNotesGroupId = "";
var collectionMembersVisible = 10;
var collectionMembersFreshFrom = 10;
var collectionMembersGroupId = "";
var COLL_MEMBER_PAGE = 10;
var COLL_TREE_PAGE = 40;
var collectionTreePages = new WeakMap();

var COLL_NOTE_TYPES = [
  ["definition", "Définition"],
  ["scopeNote", "Note d'application"],
  ["example", "Exemple"],
  ["historyNote", "Note historique"],
  ["editorialNote", "Note éditoriale"],
  ["changeNote", "Note de changement"],
  ["note", "Note"]
];

function collectionSortByNotation() {
  return state.colSort === "nota";
}

function collectionCacheKey() {
  return thesaurusId() + "|" + thesaurusLang() + "|" + (collectionSortByNotation() ? "nota" : "alpha");
}

function collectionApi(path, extra) {
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    sortByNotation: collectionSortByNotation() ? "true" : "false"
  });
  if (extra) {
    Object.keys(extra).forEach(key => {
      if (extra[key] != null && extra[key] !== "") params.set(key, extra[key]);
    });
  }
  return fetch(ctx + "/api/collection-tree/" + path + "?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then(res => {
    if (!res.ok) throw new Error("http");
    return res.json();
  });
}

function setCollectionLoading(on) {
  const panel = $("#panelCollection");
  const spin = $("#panelCollectionLoading");
  if (panel) {
    panel.classList.toggle("is-loading", !!on);
    if (on) panel.setAttribute("aria-busy", "true");
    else panel.removeAttribute("aria-busy");
  }
  if (spin) {
    if (on) spin.removeAttribute("hidden");
    else spin.setAttribute("hidden", "hidden");
    spin.hidden = !on;
  }
}

function setCollectionTreeMessage(text) {
  const box = $("#panelCollectionTree");
  if (!box) return;
  box.innerHTML = '<div class="tree-empty">' + escapeHtml(text) + "</div>";
  setCollectionLoading(false);
}

function invalidateCollectionTree() {
  collectionTreeState = { key: "", loading: false };
  const box = $("#panelCollectionTree");
  if (box) box.innerHTML = "";
}

function ensureCollectionTree() {
  if (!$("#panelCollectionTree")) return;
  const key = collectionCacheKey();
  if (collectionTreeState.key === key || collectionTreeState.loading) return;
  setCollectionLoading(true);
  loadCollectionRoots();
}

function loadCollectionRoots() {
  const box = $("#panelCollectionTree");
  if (!box) return;
  const theso = thesaurusId();
  const key = collectionCacheKey();
  if (!theso) {
    collectionTreeState = { key: key, loading: false };
    setCollectionTreeMessage("Aucun thésaurus sélectionné.");
    return;
  }
  collectionTreeState = { key: key, loading: true };
  setCollectionLoading(true);
  collectionApi("roots").then(nodes => {
    if (collectionCacheKey() !== key) return;
    collectionTreeState.loading = false;
    collectionTreeState.key = key;
    renderCollectionRoots(Array.isArray(nodes) ? nodes : []);
  }).catch(() => {
    if (collectionCacheKey() !== key) return;
    collectionTreeState = { key: "", loading: false };
    setCollectionTreeMessage("Impossible de charger les collections.");
  });
}

function renderCollectionRoots(nodes) {
  const box = $("#panelCollectionTree");
  if (!box) return;
  setCollectionLoading(false);
  if (!nodes.length) {
    box.innerHTML = '<div class="tree-empty">Aucune collection dans ce thésaurus.</div>';
    return;
  }
  box.innerHTML = nodes.map(node => collectionNodeHtml(node, 0)).join("");
  paintCollectionVisibility();
  if (state.colId) highlightConcept(state.colId);
  else if (state.conceptId) highlightConcept(state.conceptId);
}

function collectionNodeHtml(node, depth) {
  const id = node && node.id != null ? String(node.id) : "";
  const type = (node && node.nodeType) || "file";
  const isGroup = type === "group" || type === "subGroup";
  const isMore = type === "more";
  const hasChildren = !!(node && node.hasChildren);
  const label = (node && node.label) || id;
  const notation = (node && node.notation) || "";
  const status = (node && node.status) || (isGroup ? "" : "valide");
  const pad = 6 + depth * 18;
  const caretClass = "tn-caret" + (hasChildren ? "" : " is-empty");
  const caretAct = hasChildren ? ' data-act="col-toggle"' : "";
  const rowClass = "tn-row"
    + (isGroup ? " is-group" : "")
    + (isMore ? " is-more" : "")
    + (status === "deprecie" ? " is-deprecated" : "");
  const nota = collectionSortByNotation() && notation
    ? '<span class="tn-nota">' + escapeHtml(notation) + "</span>"
    : "";
  const icon = isGroup ? '<span class="tn-coll" title="Collection">⧉</span>' : "";
  const openAct = isMore ? "" : ' data-act="open"';
  return '<div class="tn"'
    + ' data-id="' + escapeHtml(id) + '"'
    + ' data-type="' + escapeHtml(type) + '"'
    + ' data-has-children="' + (hasChildren ? "true" : "false") + '"'
    + ' data-loaded="0"'
    + ' data-depth="' + depth + '"'
    + ' data-status="' + escapeHtml(status) + '"'
    + ' data-nota="' + escapeHtml(notation) + '">'
    + '<div class="' + rowClass + '">'
    + '<div class="tn-rowmain" style="padding-left:' + pad + 'px">'
    + '<button type="button" class="' + caretClass + '"' + caretAct
    + ' title="' + (hasChildren ? "Déplier" : "") + '">'
    + '<span class="caret-open">▾</span><span class="caret-shut">▸</span>'
    + "</button>"
    + '<button type="button" class="tn-label"' + openAct
    + ' data-id="' + escapeHtml(id) + '" data-type="' + escapeHtml(type) + '">'
    + icon + nota
    + '<span class="tn-textwrap"><span class="tn-text">' + escapeHtml(label) + "</span></span>"
    + "</button></div></div></div>";
}

function collectionTreeMoreHtml(depth, count) {
  const pad = 6 + depth * 18;
  const label = "Afficher " + count + " de plus";
  return '<div class="tn" data-id="" data-type="page" data-depth="' + depth + '" data-has-children="false">'
    + '<div class="tn-row is-more"><div class="tn-rowmain" style="padding-left:' + pad + 'px">'
    + '<span class="tn-caret is-empty" aria-hidden="true"></span>'
    + '<button type="button" class="tn-label" data-act="col-page">' + label + "</button>"
    + "</div></div></div>";
}

function appendCollectionChildPage(parent) {
  const page = collectionTreePages.get(parent);
  if (!page) return;
  const depth = Number(parent.getAttribute("data-depth") || 0) + 1;
  const start = page.shown;
  const end = Math.min(page.nodes.length, start + COLL_TREE_PAGE);
  const html = page.nodes.slice(start, end).map((node) => collectionNodeHtml(node, depth)).join("");
  page.shown = end;
  const left = page.nodes.length - end;
  const more = left > 0 ? collectionTreeMoreHtml(depth, Math.min(COLL_TREE_PAGE, left)) : "";
  const desc = collectionDescendants(parent);
  const anchor = desc.length ? desc[desc.length - 1] : parent;
  anchor.insertAdjacentHTML("afterend", html + more);
  paintCollectionVisibility();
}

function showMoreCollectionChildren(moreNode) {
  const parent = typeof previousTreeParent === "function" ? previousTreeParent(moreNode) : null;
  if (!parent) return;
  moreNode.remove();
  appendCollectionChildPage(parent);
}

function paintCollectionVisibility() {
  const nodes = $$("#panelCollectionTree > .tn");
  const showAt = [true];
  nodes.forEach(tn => {
    const depth = Number(tn.getAttribute("data-depth") || 0);
    const visible = !!showAt[depth];
    tn.hidden = !visible;
    showAt[depth + 1] = visible && tn.classList.contains("is-open");
  });
}

function collectionDescendants(parent) {
  const depth = Number(parent.getAttribute("data-depth") || 0);
  const out = [];
  let el = parent.nextElementSibling;
  while (el && el.classList.contains("tn")) {
    const d = Number(el.getAttribute("data-depth") || 0);
    if (d <= depth) break;
    out.push(el);
    el = el.nextElementSibling;
  }
  return out;
}

function toggleCollectionNode(tn) {
  if (!tn || tn.getAttribute("data-has-children") !== "true") return;
  if (tn.classList.contains("is-fetching")) return;
  if (tn.classList.contains("is-open")) {
    tn.classList.remove("is-open");
    paintCollectionVisibility();
    return;
  }
  if (tn.getAttribute("data-loaded") === "1") {
    tn.classList.add("is-open");
    if (typeof replayAnim === "function") replayAnim(tn, "is-branch-in");
    paintCollectionVisibility();
    return;
  }
  tn.classList.add("is-open", "is-fetching");
  if (typeof replayAnim === "function") replayAnim(tn, "is-branch-in");
  const parentId = tn.getAttribute("data-id");
  collectionApi("children", { parentId: parentId }).then(nodes => {
    tn.classList.remove("is-fetching");
    collectionDescendants(tn).forEach(el => el.remove());
    const list = Array.isArray(nodes) ? nodes : [];
    tn.setAttribute("data-loaded", "1");
    if (!list.length) {
      tn.classList.remove("is-open");
      tn.setAttribute("data-has-children", "false");
      const caret = tn.querySelector(".tn-caret");
      if (caret) caret.classList.add("is-empty");
      collectionTreePages.delete(tn);
      return;
    }
    collectionTreePages.set(tn, { nodes: list, shown: 0 });
    appendCollectionChildPage(tn);
    if (state.colId && typeof highlightConcept === "function") highlightConcept(state.colId);
  }).catch(() => {
    tn.classList.remove("is-fetching", "is-open");
    toast("Impossible de charger les éléments de la collection.");
  });
}

function setCollectionSort(sort) {
  const next = sort === "nota" ? "nota" : "alpha";
  if (state.colSort === next) return;
  state.colSort = next;
  $$("#voColSortRow .vo-sort-b").forEach(b => {
    b.classList.toggle("is-on", b.getAttribute("data-sort") === next);
  });
  invalidateCollectionTree();
  if (state.view === "collection") loadCollectionRoots();
}

function revealCreatedCollection(newId, parentId) {
  const parent = parentId && window.CSS
    ? document.querySelector('#panelCollectionTree .tn[data-id="' + CSS.escape(parentId) + '"]')
    : null;
  if (!parent) {
    invalidateCollectionTree();
    if (newId) openCollectionFromTree(newId);
    else if (state.view === "collection") loadCollectionRoots();
    return;
  }
  parent.setAttribute("data-has-children", "true");
  parent.setAttribute("data-loaded", "0");
  parent.classList.remove("is-open", "is-fetching");
  collectionDescendants(parent).forEach((el) => el.remove());
  const caret = parent.querySelector(".tn-caret");
  if (caret) {
    caret.classList.remove("is-empty");
    if (!caret.getAttribute("data-act")) caret.setAttribute("data-act", "col-toggle");
  }
  if (newId) openCollectionFromTree(newId);
  toggleCollectionNode(parent);
}

function openCollectionFromTree(id) {
  if (!id) return;
  state.view = "collection";
  state.home = false;
  state.draft = false;
  state.collectionDraft = false;
  state.colId = id;
  state.conceptId = null;
  highlightConcept(id);
  loadCollectionDetail(id);
  paint();
}

function dashText(value) {
  return value == null || String(value).trim() === "" ? "—" : String(value);
}

function collectionCrow(label, valueHtml) {
  return '<div class="crow"><div class="crow-lbl">' + label + '</div><div class="crow-val">'
    + valueHtml + "</div></div>";
}

function collectionLangMeta(code) {
  const norm = String(code || "").trim().toLowerCase();
  const opts = document.querySelectorAll("#previewTermLangUi .tree-lang-opt");
  let opt = null;
  for (let i = 0; i < opts.length; i++) {
    if ((opts[i].getAttribute("data-lang") || "").trim().toLowerCase() === norm) {
      opt = opts[i];
      break;
    }
  }
  let flagText = opt && opt.querySelector(".tree-lang-opt-flag")
    ? opt.querySelector(".tree-lang-opt-flag").textContent.trim()
    : "";
  let name = "";
  const nameEl = opt && opt.querySelector(".tree-lang-opt-name");
  if (nameEl && nameEl.textContent.trim()) name = nameEl.textContent.trim();
  if (!flagText || !name) {
    const spans = document.querySelectorAll("#collFicheLangs [data-code]");
    for (let i = 0; i < spans.length; i++) {
      if ((spans[i].getAttribute("data-code") || "").trim().toLowerCase() !== norm) continue;
      if (!flagText) flagText = (spans[i].getAttribute("data-flag") || "").trim();
      if (!name) name = (spans[i].getAttribute("data-name") || "").trim();
      break;
    }
  }
  if (!flagText && typeof exportLangFlag === "function") flagText = exportLangFlag(norm);
  if (!name) name = norm.toUpperCase();
  return { flag: flagText && flagText !== "🏳️" ? flagText : "", name: name };
}

function collectionTextDirection(code) {
  const base = String(code || "").trim().toLowerCase().split("-")[0];
  return base === "ar" || base === "he" || base === "fa" || base === "ur" ? "rtl" : "ltr";
}

function collectionTrMsg(name, fallback) {
  const el = document.getElementById("collTrMeta");
  const value = el ? el.getAttribute("data-" + name) : "";
  return value || fallback;
}

function collectionTranslationsHtml(translations) {
  const rows = (translations || []).filter((tr) => tr && (tr.lang || tr.value));
  if (!rows.length) {
    return '<div class="muted">' + escapeHtml(collectionTrMsg("none", "Aucune traduction renseignée.")) + "</div>";
  }
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  let html = '<div class="tr-list">';
  rows.forEach((tr) => {
    const code = String(tr.lang || "").trim();
    const value = tr.value == null ? "" : String(tr.value);
    const meta = collectionLangMeta(code);
    const todo = !value.trim();
    html += '<div class="tr-block"><div class="tr-item">';
    html += '<span class="tr-dot ' + (todo ? "tr-dot-todo" : "tr-dot-ok") + '" aria-hidden="true"></span>';
    if (meta.flag) {
      html += '<span class="tr-flag">' + meta.flag + "</span>";
    } else if (code) {
      html += '<span class="tr-code">' + esc(code) + "</span>";
    }
    html += '<button type="button" class="tr-label" data-act="term-lang" data-lang="' + esc(code) + '"'
      + ' lang="' + esc(code) + '" dir="' + collectionTextDirection(code) + '"'
      + ' title="' + esc(meta.name) + '">' + esc(value) + "</button>";
    html += '<span class="tr-lang">' + esc(meta.name) + "</span>";
    if (todo) html += '<span class="tr-todo">' + esc(collectionTrMsg("todo", "à compléter")) + "</span>";
    html += "</div></div>";
  });
  html += "</div>";
  return html;
}

function collectionMembersHtml(members) {
  const all = members || [];
  if (!all.length) {
    return '<div class="muted">Aucun concept rattaché à cette collection.</div>';
  }
  const shown = all.slice(0, collectionMembersVisible);
  let html = '<div class="rel-items">';
  shown.forEach((member, index) => {
    const id = member.conceptId || "";
    const label = member.label || id;
    const fresh = index >= collectionMembersFreshFrom;
    const delay = fresh ? ' style="animation-delay:' + ((index - collectionMembersFreshFrom) * 42) + 'ms"' : "";
    html += '<button type="button" class="rel-term' + (fresh ? " is-new" : "") + '"' + delay
      + ' data-act="open" data-id="' + escapeHtml(id) + '" data-type="concept">'
      + escapeHtml(label) + "</button>";
  });
  if (all.length > shown.length) {
    html += '<button type="button" class="rel-more" data-act="coll-mem-more"'
      + ' aria-label="Afficher 10 de plus" title="Afficher 10 de plus">'
      + '<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M12 5v14M5 12h14"/></svg>'
      + "</button>";
  }
  html += "</div>";
  return html;
}

function collectionCanEdit() {
  const chooser = document.getElementById("createChooser");
  return !!(chooser && chooser.getAttribute("data-can-collection") === "1");
}

function collectionFicheMsg(name, fallback) {
  const el = document.getElementById("collFicheMsgs");
  const value = el ? el.getAttribute("data-" + name) : "";
  return value || fallback;
}

function collectionFicheGear(block) {
  if (!collectionCanEdit()) return "";
  const label = collectionFicheMsg("edit", "Modifier");
  return '<button type="button" class="cblock-gear" data-act="coll-fiche-edit" data-block="'
    + block + '" title="' + escapeHtml(label) + '" aria-label="' + escapeHtml(label) + '">✎</button>';
}

function collectionBlockHead(title, block, extra) {
  const tools = (extra || "") + (block ? collectionFicheGear(block) : "");
  return '<div class="cblock-head"><span class="cblock-title">' + escapeHtml(title) + "</span>"
    + (tools ? '<div class="cblock-tools">' + tools + "</div>" : "") + "</div>";
}

function collectionNoteMsg(name, fallback) {
  const el = document.getElementById("collNoteMeta");
  const value = el ? el.getAttribute("data-" + name) : "";
  return value || fallback;
}

function collectionFicheFoot(block) {
  return '<p class="coll-fiche-err" hidden="hidden"></p><div class="cblock-edit-foot">'
    + '<button type="button" class="abt-cancel" data-act="coll-fiche-cancel"><span>'
    + escapeHtml(collectionFicheMsg("cancel", "Annuler")) + "</span></button>"
    + '<button type="button" class="abt-save" data-act="coll-fiche-save" data-block="' + escapeHtml(block) + '">'
    + '<span class="abt-save-label">' + escapeHtml(collectionFicheMsg("save", "Enregistrer")) + "</span>"
    + '<span class="abt-save-wait">' + escapeHtml(collectionFicheMsg("saving", "Enregistrement…")) + "</span>"
    + "</button></div>";
}

function collectionFicheLanguages() {
  return [...document.querySelectorAll("#collFicheLangs [data-code]")].map((el) => ({
    code: (el.getAttribute("data-code") || "").trim(),
    name: (el.getAttribute("data-name") || el.getAttribute("data-code") || "").trim()
  })).filter((lang) => lang.code);
}

function collectionNoteTypeLabel(code) {
  const catalog = document.querySelector('#collNoteTypeOpts [data-code="' + code + '"]');
  if (catalog && catalog.getAttribute("data-label")) return catalog.getAttribute("data-label");
  const found = COLL_NOTE_TYPES.find((row) => row[0] === code);
  return found ? found[1] : "Note";
}

function collectionNoteLangSwitch(notes, workLang) {
  const work = String(workLang || "").trim().toLowerCase();
  const others = (notes || []).some((note) => note && String(note.lang || "").trim().toLowerCase() !== work);
  if (!others) return "";
  const title = collectionNoteMsg("show-other", "Afficher les notes dans toutes les langues");
  const on = collectionNotesAllLangs;
  return '<button type="button" class="vo-sw cblock-note-sw' + (on ? " on" : "") + '" data-act="coll-note-langs"'
    + ' role="switch" aria-checked="' + (on ? "true" : "false") + '" title="' + escapeHtml(title) + '"></button>';
}

function collectionNotesHtml(notes, workLang) {
  const all = (notes || []).filter((note) => note && (note.value || note.typeCode));
  const work = String(workLang || "").trim().toLowerCase();
  const shown = collectionNotesAllLangs
    ? all
    : all.filter((note) => String(note.lang || "").trim().toLowerCase() === work);
  if (!shown.length) {
    return '<div class="muted">' + escapeHtml(collectionNoteMsg("none", "Aucune note.")) + "</div>";
  }
  const groups = [];
  const index = {};
  shown.forEach((note) => {
    const type = note.typeCode || "note";
    if (!index[type]) {
      index[type] = { label: note.typeLabel || collectionNoteTypeLabel(type), items: [] };
      groups.push(index[type]);
    }
    index[type].items.push(note);
  });
  return groups.map((group) => {
    let html = '<div class="note-grp"><div class="note-grp-t">' + escapeHtml(group.label) + "</div>";
    group.items.forEach((note) => {
      const code = note.lang || "";
      const meta = collectionLangMeta(code);
      html += '<div class="note-line">';
      html += meta.flag
        ? '<span class="tr-flag">' + meta.flag + "</span>"
        : '<span class="tr-code">' + escapeHtml(code) + "</span>";
      html += '<div class="note-line-b"><p class="cv-def" lang="' + escapeHtml(code) + '" dir="'
        + collectionTextDirection(code) + '">' + escapeHtml(note.value || "")
        + ' <span class="note-lang">(' + escapeHtml(code) + ")</span></p>";
      if (note.source) html += '<span class="note-src">' + escapeHtml(note.source) + "</span>";
      html += "</div></div>";
    });
    return html + "</div>";
  }).join("");
}

function renderCollectionDetail(data) {
  const empty = $("#collectionEmpty");
  const loading = $("#collectionLoading");
  const box = $("#collectionDetail");
  const nextId = data && data.groupId ? data.groupId : "";
  if (nextId !== collectionNotesGroupId) collectionNotesAllLangs = false;
  collectionNotesGroupId = nextId;
  if (nextId !== collectionMembersGroupId) {
    collectionMembersVisible = COLL_MEMBER_PAGE;
    collectionMembersFreshFrom = COLL_MEMBER_PAGE;
    collectionMembersGroupId = nextId;
  }
  collectionFiche = nextId ? data : null;
  if (loading) loading.hidden = true;
  if (!data || !data.groupId) {
    if (empty) empty.hidden = false;
    if (box) {
      box.hidden = true;
      box.innerHTML = "";
    }
    return;
  }
  if (empty) empty.hidden = true;
  if (!box) return;
  const translations = Array.isArray(data.translations) ? data.translations : [];
  const notes = Array.isArray(data.notes) ? data.notes : [];
  const members = Array.isArray(data.members) ? data.members : [];
  let html = '<div class="cv is-on is-collection" data-id="' + escapeHtml(data.groupId) + '">';
  html += '<div class="cv-head"><h1 class="cv-pref"><span>'
    + escapeHtml(dashText(data.label)) + '</span>'
    + '<span class="cv-lang">' + escapeHtml(dashText(data.lang)) + "</span>"
    + '<span class="cv-status is-collection">collection</span></h1></div>';
  html += '<div class="cv-blocks">';
  html += '<section class="cblock" data-coll-block="label">' + collectionBlockHead("Libellé", "label") + '<div class="cblock-body">';
  html += collectionCrow("Libellé",
    '<span class="val-strong">' + escapeHtml(dashText(data.label)) + "</span>"
    + ' <span class="val-lang">(' + escapeHtml(dashText(data.lang)) + ")</span>");
  html += "</div></section>";
  html += '<section class="cblock" data-coll-block="members">' + collectionBlockHead("Membres", "members") + '<div class="cblock-body">';
  html += collectionMembersHtml(members);
  html += "</div></section>";
  html += '<section class="cblock" data-coll-block="translations">'
    + collectionBlockHead("Traductions", "translations") + '<div class="cblock-body">';
  html += collectionTranslationsHtml(translations);
  html += "</div></section>";
  html += '<section class="cblock" data-coll-block="notes">'
    + collectionBlockHead("Notes", "notes", collectionNoteLangSwitch(notes, data.lang))
    + '<div class="cblock-body">' + collectionNotesHtml(notes, data.lang) + "</div></section>";
  html += '<section class="cblock" data-coll-block="notation">' + collectionBlockHead("Identifiants", "notation") + '<div class="cblock-body">';
  html += collectionCrow("Identifiant", '<span class="is-mono">' + escapeHtml(dashText(data.groupId)) + "</span>");
  html += collectionCrow("Notation", escapeHtml(dashText(data.notation)));
  html += collectionCrow("ARK", '<span class="is-mono">' + escapeHtml(dashText(data.arkId)) + "</span>");
  html += collectionCrow("Handle", '<span class="is-mono">' + escapeHtml(dashText(data.handleId)) + "</span>");
  html += "</div></section>";
  html += '<section class="cblock">' + collectionBlockHead("Information temporelle") + '<div class="cblock-body">';
  html += collectionCrow("Créé le", '<span class="is-mono">' + escapeHtml(dashText(data.created)) + "</span>");
  html += collectionCrow("Dernière modification", '<span class="is-mono">' + escapeHtml(dashText(data.modified)) + "</span>");
  html += "</div></section></div></div>";
  box.innerHTML = html;
  box.hidden = false;
  collectionMembersFreshFrom = collectionMembersVisible;
}

function collectionFicheEditorHtml(block) {
  const data = collectionFiche || {};
  if (block === "label") {
    return '<div class="crow"><div class="crow-lbl">Libellé</div><div class="crow-val">'
      + '<input class="st-input" id="collFicheLabelInput" value="' + escapeHtml(data.label || "") + '" autocomplete="off">'
      + ' <span class="val-lang">(' + escapeHtml(data.lang || "") + ")</span></div></div>"
      + collectionFicheFoot(block);
  }
  if (block === "notation") {
    let html = collectionCrow("Identifiant", '<span class="is-mono">' + escapeHtml(dashText(data.groupId)) + "</span>");
    html += '<div class="crow"><div class="crow-lbl">Notation</div><div class="crow-val">'
      + '<input class="st-input" id="collFicheNotationInput" value="' + escapeHtml(data.notation || "") + '" autocomplete="off">'
      + "</div></div>";
    html += collectionCrow("ARK", '<span class="is-mono">' + escapeHtml(dashText(data.arkId)) + "</span>");
    html += collectionCrow("Handle", '<span class="is-mono">' + escapeHtml(dashText(data.handleId)) + "</span>");
    return html + collectionFicheFoot(block);
  }
  if (block === "translations") return collectionTranslationEditorHtml(data);
  if (block === "notes") return collectionNoteEditorHtml(data);
  if (block === "members") return collectionMemberEditorHtml(data);
  return "";
}

var collectionMemState = { items: [], hits: [], seq: 0, timer: null, active: -1 };

function collectionMemberEditorHtml() {
  return '<div class="re" id="collMemEditor"'
    + ' data-none="Aucun concept pour cette recherche."'
    + ' data-remove="Retirer ce membre"'
    + ' data-exists="Ce concept est déjà membre de la collection.">'
    + '<div class="re-rows" id="collMemRows"></div>'
    + '<div class="coll-pick" id="collMemPick">'
    + '<div class="search-field coll-pick-field" id="collMemCombo">'
    + '<span class="search-ico" aria-hidden="true">⌕</span>'
    + '<input type="text" class="search-input" id="collMemQ" autocomplete="off" spellcheck="false"'
    + ' placeholder="Rechercher un concept…" aria-autocomplete="list" aria-expanded="false"'
    + ' aria-controls="collMemMenu" aria-label="Rechercher un concept…"/>'
    + '<button type="button" class="search-clear" id="collMemClear" hidden="hidden"'
    + ' data-act="coll-mem-clear" title="Effacer">×</button>'
    + "</div>"
    + '<div class="ac" id="collMemMenu" hidden="hidden" role="listbox">'
    + '<div class="ac-empty">Aucun concept pour « <span id="collMemEmptyQuery"></span> »</div>'
    + '<div class="coll-ac-list" id="collMemList"></div>'
    + "</div></div>"
    + '<div class="re-msg re-error" id="collMemMsg" hidden="hidden"></div>'
    + '<input type="hidden" id="collMemPayload" value="">'
    + "</div>"
    + collectionFicheFoot("members");
}

function syncCollectionMemHidden() {
  const el = document.getElementById("collMemPayload");
  if (!el) return;
  el.value = collectionMemState.items.map((item) => (item.id || "") + "\t" + (item.label || "")).join("\n");
}

function seedCollectionMem() {
  const members = (collectionFiche && collectionFiche.members) || [];
  collectionMemState = { items: [], hits: [], seq: 0, timer: null, active: -1 };
  members.forEach((member) => {
    const id = String((member && (member.conceptId || member.id)) || "").trim();
    if (!id || collectionMemState.items.some((item) => item.id === id)) return;
    const label = member.label == null ? "" : String(member.label);
    collectionMemState.items.push({ id: id, label: label || id });
  });
  syncCollectionMemHidden();
  paintCollectionMem();
  const input = document.getElementById("collMemQ");
  if (input) input.focus();
}

function syncCollectionMemClear() {
  const input = document.getElementById("collMemQ");
  const clear = document.getElementById("collMemClear");
  if (clear) clear.hidden = !(input && input.value);
}

function hideCollectionMemDrop() {
  const menu = document.getElementById("collMemMenu");
  const list = document.getElementById("collMemList");
  const input = document.getElementById("collMemQ");
  const combo = document.getElementById("collMemCombo");
  const pick = document.getElementById("collMemPick");
  if (menu) menu.hidden = true;
  if (list) list.innerHTML = "";
  if (input) input.setAttribute("aria-expanded", "false");
  if (combo) combo.classList.remove("is-open");
  if (pick) pick.classList.remove("is-open");
  collectionMemState.active = -1;
}

function setCollectionMemMsg(text) {
  const el = document.getElementById("collMemMsg");
  if (!el) return;
  el.hidden = !text;
  el.textContent = text || "";
}

function collectionMemAlready(id, label) {
  return collectionMemState.items.some((item) => item.id === id || (label && item.label === label));
}

function paintCollectionMem() {
  const rows = document.getElementById("collMemRows");
  const editor = document.getElementById("collMemEditor");
  if (!rows || !editor) return;
  const remove = editor.getAttribute("data-remove") || "Retirer ce membre";
  rows.innerHTML = collectionMemState.items.map((item, index) => (
    '<div class="re-row">'
    + '<span class="re-lbl">' + escapeHtml(item.label || item.id) + "</span>"
    + '<button type="button" class="re-x" data-act="coll-mem-remove" data-index="' + index
    + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
    + "</div>"
  )).join("");
}

function addCollectionMem(id, label) {
  if (!id) return;
  const editor = document.getElementById("collMemEditor");
  if (collectionMemAlready(id, label)) {
    setCollectionMemMsg((editor && editor.getAttribute("data-exists")) || "");
    return;
  }
  collectionMemState.items.push({ id: id, label: label || id });
  const input = document.getElementById("collMemQ");
  if (input) input.value = "";
  collectionMemState.hits = [];
  hideCollectionMemDrop();
  syncCollectionMemClear();
  setCollectionMemMsg("");
  syncCollectionMemHidden();
  paintCollectionMem();
  if (input) input.focus();
}

function removeCollectionMem(index) {
  collectionMemState.items.splice(Number(index), 1);
  setCollectionMemMsg("");
  syncCollectionMemHidden();
  paintCollectionMem();
}

function shownCollectionMemRows() {
  return [...document.querySelectorAll("#collMemList .ac-row.is-shown")];
}

function paintCollectionMemHits(q) {
  const menu = document.getElementById("collMemMenu");
  const list = document.getElementById("collMemList");
  const emptyQ = document.getElementById("collMemEmptyQuery");
  const input = document.getElementById("collMemQ");
  const combo = document.getElementById("collMemCombo");
  const pick = document.getElementById("collMemPick");
  if (!menu || !list) return;
  const hits = collectionMemState.hits || [];
  list.innerHTML = hits.map((hit) => (
    '<button type="button" class="ac-row is-shown" role="option" aria-selected="false" data-act="coll-mem-hit"'
    + ' data-id="' + escapeHtml(hit.id || "") + '" data-label="' + escapeHtml(hit.label || hit.id || "") + '">'
    + '<span class="ac-ico">◇</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(hit.label || hit.id || "") + "</span></span></button>"
  )).join("");
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", hits.length === 0);
  if (emptyQ) emptyQ.textContent = q || "";
  menu.hidden = false;
  if (input) input.setAttribute("aria-expanded", "true");
  if (combo) combo.classList.add("is-open");
  if (pick) pick.classList.add("is-open");
  collectionMemState.active = -1;
  if (q && typeof wrapQueryIn === "function") {
    list.querySelectorAll(".ac-pref").forEach((el) => wrapQueryIn(el, q));
  }
}

function searchCollectionMem(q) {
  const seq = ++collectionMemState.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: typeof thesaurusId === "function" ? (thesaurusId() || "") : "",
    lang: typeof thesaurusLang === "function" ? thesaurusLang() : "fr",
    q: q
  });
  fetch(ctx + "/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== collectionMemState.seq) return;
    collectionMemState.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id) : "";
      const label = item && item.label ? String(item.label) : "";
      return id && !collectionMemAlready(id, label);
    }).slice(0, 6);
    paintCollectionMemHits(q);
  }).catch(() => {
    if (seq !== collectionMemState.seq) return;
    collectionMemState.hits = [];
    paintCollectionMemHits(q);
  });
}

function scheduleCollectionMemSearch() {
  const input = document.getElementById("collMemQ");
  const q = (input && input.value.trim()) || "";
  setCollectionMemMsg("");
  syncCollectionMemClear();
  if (collectionMemState.timer) clearTimeout(collectionMemState.timer);
  if (q.length < 2) {
    collectionMemState.seq += 1;
    collectionMemState.hits = [];
    hideCollectionMemDrop();
    return;
  }
  collectionMemState.timer = setTimeout(() => searchCollectionMem(q), 220);
}

function collectionTranslationEditorHtml(data) {
  const translations = Array.isArray(data.translations) ? data.translations : [];
  const work = String(data.lang || "").trim().toLowerCase();
  const add = collectionTrMsg("add", "Ajouter une langue…");
  const langs = [...document.querySelectorAll("#collFicheLangs [data-code]")].map((el) => {
    const code = el.getAttribute("data-code") || "";
    if (!code || code.trim().toLowerCase() === work) return "";
    const name = el.getAttribute("data-name") || code;
    const flag = el.getAttribute("data-flag") || "";
    return '<button type="button" class="tree-lang-opt" data-act="cv-tr-add" data-lang="' + escapeHtml(code)
      + '" data-flag="' + escapeHtml(flag) + '" data-name="' + escapeHtml(name) + '" role="option" aria-selected="false">'
      + (flag ? '<span class="tree-lang-opt-flag" aria-hidden="true">' + escapeHtml(flag) + "</span>" : "")
      + '<span class="tree-lang-opt-name">' + escapeHtml(name) + "</span>"
      + '<span class="tree-lang-opt-code">' + escapeHtml(code) + "</span></button>";
  }).join("");
  const seed = translations.map((tr) => {
    const code = String((tr && tr.lang) || "").trim();
    if (!code) return "";
    return '<span data-lang="' + escapeHtml(code) + '" data-value="' + escapeHtml(tr.value || "") + '"></span>';
  }).join("");
  return '<div class="te" id="collTrEditor" data-cv="editor"'
    + ' data-empty="' + escapeHtml(collectionTrMsg("empty", "Aucune traduction. Ajoute une langue ci-dessous.")) + '"'
    + ' data-drop="' + escapeHtml(collectionTrMsg("drop", "Supprimer cette traduction ?")) + '"'
    + ' data-ph="' + escapeHtml(collectionTrMsg("ph", "Nom en {0}…")) + '"'
    + ' data-remove="' + escapeHtml(collectionTrMsg("remove", "Retirer cette langue")) + '"'
    + ' data-no="' + escapeHtml(collectionTrMsg("no", "Non")) + '"'
    + ' data-yes="' + escapeHtml(collectionTrMsg("yes", "Oui")) + '"'
    + ' data-used="' + escapeHtml(collectionTrMsg("used", "Langue déjà ouverte")) + '">'
    + '<div class="re-none" data-cv="empty">' + escapeHtml(collectionTrMsg("empty", "Aucune traduction. Ajoute une langue ci-dessous.")) + "</div>"
    + '<div data-cv="list"></div>'
    + '<div data-cv="seed" hidden="hidden">' + seed + "</div>"
    + '<div class="te-add"><div class="tp-lang-pick te-pick" data-cv="pick">'
    + '<button type="button" class="tree-lang-btn tp-lang-btn" data-cv="btn" data-act="cv-tr-toggle"'
    + ' title="' + escapeHtml(add) + '" aria-label="' + escapeHtml(add) + '" aria-haspopup="listbox" aria-expanded="false">'
    + '<span class="tree-lang-flag" aria-hidden="true">🏳️</span>'
    + '<span class="tree-lang-name">' + escapeHtml(add) + "</span>"
    + '<span class="tree-lang-caret" aria-hidden="true">▾</span></button>'
    + '<div class="tree-lang-menu tp-lang-menu" data-cv="menu" role="listbox" aria-label="' + escapeHtml(add) + '">'
    + '<div class="tree-lang-menu-h">' + escapeHtml(add) + "</div>"
    + langs + "</div></div></div>"
    + '<input type="hidden" data-cv="payload" value="">'
    + "</div>"
    + '<p class="coll-fiche-err" hidden="hidden"></p><div class="cblock-edit-foot">'
    + '<button type="button" class="bo-btn ghost sm" data-act="coll-fiche-cancel"><span>'
    + escapeHtml(collectionFicheMsg("cancel", "Annuler")) + "</span></button>"
    + '<button type="button" class="bo-btn primary sm" data-act="coll-fiche-save" data-block="translations"><span>'
    + escapeHtml(collectionTrMsg("done", "Terminer")) + "</span></button></div>";
}

function collectionNoteAttr(name, fallback) {
  return escapeHtml(collectionNoteMsg(name, fallback));
}

function collectionNoteEditorHtml(data) {
  const notes = Array.isArray(data.notes) ? data.notes : [];
  let types = [...document.querySelectorAll("#collNoteTypeOpts [data-code]")].map((el) => el.outerHTML).join("");
  if (!types) {
    types = COLL_NOTE_TYPES.map((row) => '<span data-code="' + escapeHtml(row[0]) + '" data-label="'
      + escapeHtml(row[1]) + '"></span>').join("");
  }
  const langs = [...document.querySelectorAll("#collFicheLangs [data-code]")].map((el) => {
    const code = el.getAttribute("data-code") || "";
    const name = el.getAttribute("data-name") || code;
    const flag = el.getAttribute("data-flag") || "";
    return '<span data-code="' + escapeHtml(code) + '" data-name="' + escapeHtml(name) + '" data-flag="' + escapeHtml(flag) + '"></span>'
      + '<button type="button" class="tree-lang-opt" data-act="cv-note-add-lang" data-lang="' + escapeHtml(code)
      + '" data-flag="' + escapeHtml(flag) + '" data-name="' + escapeHtml(name) + '" role="option" aria-selected="false">'
      + (flag ? '<span class="tree-lang-opt-flag" aria-hidden="true">' + escapeHtml(flag) + "</span>" : "")
      + '<span class="tree-lang-opt-name">' + escapeHtml(name) + "</span>"
      + '<span class="tree-lang-opt-code">' + escapeHtml(code) + "</span></button>";
  }).join("");
  const seed = notes.map((note) => {
    const type = note.typeCode || "";
    const lang = note.lang || "";
    if (!type || !lang) return "";
    return '<span data-type="' + escapeHtml(type) + '" data-lang="' + escapeHtml(lang)
      + '" data-value="' + escapeHtml(note.value || "") + '" data-source="' + escapeHtml(note.source || "") + '"></span>';
  }).join("");
  return '<div class="note-ed" id="collNoteEditor" data-cv="editor"'
    + ' data-empty="' + collectionNoteAttr("empty", "Aucune note dans cette langue.") + '"'
    + ' data-add="' + collectionNoteAttr("add", "+ Ajouter une note") + '"'
    + ' data-add-lang="' + collectionNoteAttr("add-lang", "+ langue") + '"'
    + ' data-compare="' + collectionNoteAttr("compare", "Comparer") + '"'
    + ' data-remove="' + collectionNoteAttr("remove", "Supprimer cette note") + '"'
    + ' data-text-ph="' + collectionNoteAttr("text-ph", "Texte de la note…") + '"'
    + ' data-source-ph="' + collectionNoteAttr("source-ph", "source (optionnelle)") + '"'
    + ' data-used="' + collectionNoteAttr("used", "Langue déjà ouverte") + '"'
    + ' data-work-lang="' + escapeHtml(data.lang || "") + '">'
    + '<div class="note-ed-bar"><div class="note-ed-tabs" data-cv="tabs" role="tablist"></div>'
    + '<div class="note-ed-actions"><div class="note-ed-pick" data-cv="pick">'
    + '<button type="button" class="note-ed-addl" data-cv="btn" data-act="cv-note-toggle" aria-haspopup="listbox" aria-expanded="false">'
    + collectionNoteAttr("add-lang", "+ langue") + "</button>"
    + '<div class="tree-lang-menu tp-lang-menu" data-cv="menu" role="listbox">'
    + '<div class="tree-lang-menu-h">' + collectionNoteAttr("add-lang", "+ langue") + "</div>"
    + langs + "</div></div>"
    + '<button type="button" class="note-ed-cmp" data-cv="cmp" data-act="cv-note-cmp">'
    + collectionNoteAttr("compare", "Comparer") + "</button></div></div>"
    + '<div data-cv="body"></div>'
    + '<div data-cv="seed" hidden="hidden">' + seed + "</div>"
    + '<div data-cv="types" hidden="hidden">' + types + "</div>"
    + '<div data-cv="langs" hidden="hidden">' + langs.replace(/<button[\s\S]*?<\/button>/g, "") + "</div>"
    + '<input type="hidden" data-cv="payload" value="">'
    + "</div>"
    + '<p class="coll-fiche-err" hidden="hidden"></p><div class="cblock-edit-foot">'
    + '<button type="button" class="bo-btn ghost sm" data-act="coll-fiche-cancel"><span>'
    + escapeHtml(collectionFicheMsg("cancel", "Annuler")) + "</span></button>"
    + '<button type="button" class="bo-btn primary sm" data-act="coll-fiche-save" data-block="notes"><span>'
    + escapeHtml(collectionNoteMsg("done", "Terminer")) + "</span></button></div>";
}

function startCollectionFicheEdit(block) {
  if (!collectionFiche || !collectionCanEdit() || !block) return;
  const current = document.querySelector("#collectionDetail .cblock.is-editing");
  if (current && current.getAttribute("data-coll-block") !== block) {
    renderCollectionDetail(collectionFiche);
  }
  const section = document.querySelector('#collectionDetail .cblock[data-coll-block="' + block + '"]');
  if (!section || section.classList.contains("is-editing")) return;
  section.classList.add("is-editing");
  const body = section.querySelector(".cblock-body");
  if (body) body.innerHTML = collectionFicheEditorHtml(block);
  if (block === "translations") {
    const editor = section.querySelector("#collTrEditor");
    if (editor && typeof seedConceptTrFromHidden === "function") seedConceptTrFromHidden(editor);
    return;
  }
  if (block === "notes") {
    const editor = section.querySelector("#collNoteEditor");
    if (editor && typeof seedConceptNotesFromHidden === "function") seedConceptNotesFromHidden(editor);
    return;
  }
  if (block === "members") {
    seedCollectionMem();
    return;
  }
  const input = body && body.querySelector("input, textarea, select");
  if (input) input.focus();
}

function collectionFicheUsed(selector, attr) {
  const used = new Set();
  document.querySelectorAll(selector).forEach((row) => {
    const select = row.querySelector("select");
    const key = select ? select.value : row.getAttribute(attr);
    if (key) used.add(key);
  });
  return used;
}

function addCollectionFicheTranslation() {
  const box = document.getElementById("collFicheTrRows");
  if (!box) return;
  const used = collectionFicheUsed("#collFicheTrRows .coll-fiche-row", "data-lang");
  const taken = new Set([...used].map((code) => code.toLowerCase()));
  const work = String((collectionFiche && collectionFiche.lang) || "").trim().toLowerCase();
  if (work) taken.add(work);
  const free = collectionFicheLanguages().filter((lang) => !taken.has(lang.code.toLowerCase()));
  if (!free.length) {
    if (typeof toast === "function") toast("Toutes les langues du thésaurus sont déjà utilisées.");
    return;
  }
  const options = free.map((lang) => '<option value="' + escapeHtml(lang.code) + '">'
    + escapeHtml(lang.name || lang.code) + "</option>").join("");
  const row = document.createElement("div");
  row.className = "coll-fiche-row";
  row.innerHTML = '<select class="st-input">' + options + "</select>"
    + '<input class="st-input" value="" autocomplete="off">'
    + '<button type="button" class="align-del" data-act="coll-fiche-tr-drop" aria-label="Supprimer">×</button>';
  box.appendChild(row);
  const input = row.querySelector("input");
  if (input) input.focus();
}

function saveCollectionFiche(block) {
  const save = document.querySelector('#collectionDetail [data-act="coll-fiche-save"]');
  if (save && save.classList.contains("is-busy")) return;
  const err = document.querySelector("#collectionDetail .coll-fiche-err");
  const showErr = (msg) => {
    if (!err) return;
    err.textContent = msg;
    err.hidden = false;
  };
  if (err) err.hidden = true;
  const groupId = collectionFiche && collectionFiche.groupId;
  if (!groupId || typeof clickPreviewJsf !== "function") return;
  let payload = "";
  if (block === "label") {
    const input = document.getElementById("collFicheLabelInput");
    payload = input ? input.value.trim() : "";
    if (!payload) {
      showErr("Le libellé est obligatoire.");
      if (input) input.focus();
      return;
    }
  } else if (block === "notation") {
    const input = document.getElementById("collFicheNotationInput");
    payload = input ? input.value.trim() : "";
  } else if (block === "translations") {
    const editor = document.getElementById("collTrEditor");
    if (editor && typeof seedConceptTrFromHidden === "function") seedConceptTrFromHidden(editor);
    if (typeof syncConceptTrHidden === "function") syncConceptTrHidden();
    const hidden = editor && editor.querySelector("[data-cv='payload']");
    payload = hidden ? hidden.value : "";
  } else if (block === "notes") {
    const editor = document.getElementById("collNoteEditor");
    if (editor && typeof seedConceptNotesFromHidden === "function") seedConceptNotesFromHidden(editor);
    if (typeof syncConceptNoteHidden === "function") syncConceptNoteHidden();
    const hidden = editor && editor.querySelector("[data-cv='payload']");
    payload = hidden ? hidden.value : "";
  } else if (block === "members") {
    if (typeof syncCollectionMemHidden === "function") syncCollectionMemHidden();
    const hidden = document.getElementById("collMemPayload");
    payload = hidden ? hidden.value : "";
  } else {
    return;
  }
  if (save) save.classList.add("is-busy");
  clickPreviewJsf("previewCollFicheGo", {
    previewCollFicheId: groupId,
    previewCollFicheMode: block,
    previewCollFichePayload: payload
  });
}

function patchCollectionTreeLabel(groupId, label, notation) {
  if (!groupId) return;
  document.querySelectorAll("#panelCollectionTree .tn").forEach((tn) => {
    if (tn.getAttribute("data-id") !== groupId) return;
    if (label != null) {
      const text = tn.querySelector(".tn-text");
      if (text) text.textContent = label;
    }
    if (notation != null) {
      tn.setAttribute("data-nota", notation);
      const nota = tn.querySelector(".tn-nota");
      if (nota) nota.textContent = notation;
    }
  });
}

window.onCollFicheAjax = function (data) {
  if (!data || (data.status !== "success" && data.status !== "error")) return;
  const save = document.querySelector('#collectionDetail [data-act="coll-fiche-save"]');
  if (data.status !== "success") {
    if (save) save.classList.remove("is-busy");
    return;
  }
  const status = document.getElementById("previewCollFicheStatus");
  const ok = !!(status && status.classList.contains("is-ok"));
  const msg = status ? (status.textContent || "").trim() : "";
  if (!ok) {
    if (save) save.classList.remove("is-busy");
    const err = document.querySelector("#collectionDetail .coll-fiche-err");
    if (err) {
      err.textContent = msg || "Modification impossible.";
      err.hidden = false;
    }
    return;
  }
  if (typeof toast === "function") toast(msg || "Collection modifiée");
  const mode = (document.getElementById("previewCollFicheMode") || {}).value || "";
  const groupId = (document.getElementById("previewCollFicheId") || {}).value || "";
  if (mode === "label" || mode === "notation") {
    const payload = (document.getElementById("previewCollFichePayload") || {}).value || "";
    patchCollectionTreeLabel(groupId, mode === "label" ? payload : null, mode === "notation" ? payload : null);
  }
  collectionPicker.items = null;
  collectionPicker.key = "";
  loadCollectionDetail(groupId);
};

function logCollectionConsultation(groupId) {
  const idEl = document.getElementById("previewConsultLogForm:groupId");
  const btn = document.getElementById("previewConsultLogForm:logCollectionGo");
  if (!idEl || !btn || !groupId) return;
  idEl.value = groupId;
  btn.click();
}

function loadCollectionDetail(groupId) {
  const empty = $("#collectionEmpty");
  const loading = $("#collectionLoading");
  const box = $("#collectionDetail");
  if (!groupId) {
    renderCollectionDetail(null);
    return;
  }
  if (empty) empty.hidden = true;
  if (box) box.hidden = true;
  if (loading) loading.hidden = false;
  collectionApi("detail", { groupId: groupId }).then(data => {
    if (state.colId !== groupId) return;
    renderCollectionDetail(data);
    if (data && data.groupId) logCollectionConsultation(data.groupId);
  }).catch(() => {
    if (state.colId !== groupId) return;
    if (loading) loading.hidden = true;
    if (empty) empty.hidden = false;
    toast("Impossible de charger la collection.");
  });
}

var COLL_AC_PAGE = 7;
var collectionPicker = { items: null, key: "", selected: [], hint: "", active: -1, showAll: false };

function collectionPickerKey() {
  return thesaurusId() + "|" + thesaurusLang();
}

function loadCollectionPickerItems() {
  const key = collectionPickerKey();
  if (collectionPicker.items && collectionPicker.key === key) {
    return Promise.resolve(collectionPicker.items);
  }
  if (collectionPicker._load && collectionPicker.key === key) {
    return collectionPicker._load;
  }
  collectionPicker.key = key;
  collectionPicker._load = collectionApi("list").then((items) => {
    collectionPicker.items = Array.isArray(items) ? items : [];
    collectionPicker._load = null;
    if (collectionPicker.hint) {
      applyCollectionHint(collectionPicker.hint);
      collectionPicker.hint = "";
    }
    return collectionPicker.items;
  }).catch(() => {
    collectionPicker._load = null;
    collectionPicker.items = [];
    return collectionPicker.items;
  });
  return collectionPicker._load;
}

function scoreCollection(item, q) {
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

function rankCollectionHits(query) {
  const q = norm((query || "").trim());
  return (collectionPicker.items || []).map((item) => {
    const sc = scoreCollection(item, q);
    if (sc.score < 0) return null;
    return { item: item, score: sc.score, via: sc.via };
  }).filter(Boolean).sort((a, b) => a.score - b.score || (a.item.label || "").localeCompare(b.item.label || "", "fr"));
}

function shownCollectionRows() {
  return $$("#draftCollMenu .ac-row.is-shown");
}

function isCollectionSelected(id) {
  return collectionPicker.selected.some((item) => item.id === id);
}

function syncCollectionPickerHidden() {
  const hidden = $("#draftCollIds");
  if (hidden) hidden.value = collectionPicker.selected.map((item) => item.id).join(",");
  const chips = $("#draftCollChips");
  if (!chips) return;
  chips.hidden = collectionPicker.selected.length === 0;
  chips.innerHTML = collectionPicker.selected.map((item) => {
    return '<button type="button" class="coll-chip" data-act="coll-chip-remove" data-id="'
      + escapeHtml(item.id) + '" title="Retirer">'
      + escapeHtml(item.label)
      + '<span aria-hidden="true">×</span></button>';
  }).join("");
}

function syncCollectionClear() {
  const input = $("#draftColl");
  const clear = $("#draftCollClear");
  if (clear) clear.hidden = !(input && input.value);
}

function toggleCollectionPick(id, label) {
  if (!id) return;
  if (isCollectionSelected(id)) {
    collectionPicker.selected = collectionPicker.selected.filter((item) => item.id !== id);
  } else {
    collectionPicker.selected.push({ id: id, label: label || id });
  }
  syncCollectionPickerHidden();
  renderCollectionMenu($("#draftColl") ? $("#draftColl").value : "");
}

function applyCollectionHint(hint) {
  const q = norm((hint || "").trim());
  if (!q || !collectionPicker.items) return;
  const match = collectionPicker.items.find((item) => norm(item.label || "") === q);
  if (match && !isCollectionSelected(match.id)) {
    collectionPicker.selected.push({ id: match.id, label: match.label });
    syncCollectionPickerHidden();
  }
}

function resetCollectionPicker(hint) {
  collectionPicker.selected = [];
  collectionPicker.hint = (hint || "").trim();
  collectionPicker.active = -1;
  collectionPicker.showAll = false;
  const input = $("#draftColl");
  if (input) input.value = "";
  syncCollectionClear();
  syncCollectionPickerHidden();
  closeCollectionPicker();
  loadCollectionPickerItems();
}

function highlightCollectionQuery(query) {
  $$("#draftCollMenu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#draftCollMenu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function collectionItemHtml(hit) {
  const item = hit.item;
  const on = isCollectionSelected(item.id);
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="coll-pick-item" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label) + '">'
    + '<span class="ac-ico">▦</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function setCollectionAcIdx(index) {
  const rows = shownCollectionRows();
  collectionPicker.active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function renderCollectionMenu(query) {
  const menu = $("#draftCollMenu");
  const list = $("#draftCollList");
  if (!menu || !list) return;
  const raw = (query || "").trim();
  const hits = rankCollectionHits(raw);
  const limit = collectionPicker.showAll ? hits.length : COLL_AC_PAGE;
  list.innerHTML = hits.map((hit) => collectionItemHtml(hit)).join("");
  const rows = $$("#draftCollList .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", !!raw && hits.length === 0);
  const qEl = $("#draftCollQuery");
  if (qEl) qEl.textContent = raw;
  const count = $("#draftCollCount");
  if (count) count.textContent = String(hits.length);
  const more = $("#draftCollMore");
  if (more) more.hidden = collectionPicker.showAll || hits.length <= COLL_AC_PAGE;
  collectionPicker.active = -1;
  highlightCollectionQuery(raw);
  syncCollectionCombo();
}

function syncCollectionCombo() {
  const menu = $("#draftCollMenu");
  const input = $("#draftColl");
  const combo = $("#draftCollCombo");
  const pick = $("#draftCollPick");
  const open = !!(menu && !menu.hidden);
  if (pick) pick.classList.toggle("is-open", open);
  if (combo) combo.classList.toggle("is-open", open);
  if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
}

function openCollectionMenu(showAll) {
  const menu = $("#draftCollMenu");
  if (!menu) return;
  if (showAll) collectionPicker.showAll = true;
  menu.hidden = false;
  syncCollectionCombo();
  loadCollectionPickerItems().then(() => renderCollectionMenu($("#draftColl") ? $("#draftColl").value : ""));
}

function toggleCollectionMenu() {
  const menu = $("#draftCollMenu");
  if (menu && !menu.hidden) {
    closeCollectionPicker();
    return;
  }
  const input = $("#draftColl");
  if (input) input.focus();
  openCollectionMenu(true);
}

function closeCollectionPicker() {
  const menu = $("#draftCollMenu");
  const combo = $("#draftCollCombo");
  if (combo) combo.classList.remove("is-focused");
  if (!menu || menu.hidden) return false;
  menu.hidden = true;
  collectionPicker.showAll = false;
  collectionPicker.active = -1;
  syncCollectionCombo();
  return true;
}

function pickActiveCollection() {
  const rows = shownCollectionRows();
  const row = collectionPicker.active >= 0 ? rows[collectionPicker.active] : rows[0];
  if (!row) return false;
  toggleCollectionPick(row.getAttribute("data-id"), row.getAttribute("data-label"));
  const input = $("#draftColl");
  if (input) {
    input.value = "";
    syncCollectionClear();
    input.focus();
  }
  collectionPicker.showAll = false;
  renderCollectionMenu("");
  return true;
}

document.addEventListener("click", (e) => {
  const treeAct = e.target.closest && e.target.closest("#panelCollectionTree [data-act='col-page']");
  if (treeAct) {
    e.preventDefault();
    showMoreCollectionChildren(treeAct.closest(".tn"));
    return;
  }
  if (!e.target.closest("#collMemPick")) hideCollectionMemDrop();
  const act = e.target.closest("[data-act]");
  const action = act && act.getAttribute("data-act");
  if (action === "coll-mem-hit") {
    e.preventDefault();
    addCollectionMem(act.getAttribute("data-id"), act.getAttribute("data-label"));
    return;
  }
  if (action === "coll-mem-clear") {
    e.preventDefault();
    const input = document.getElementById("collMemQ");
    if (input) {
      input.value = "";
      input.focus();
    }
    collectionMemState.hits = [];
    hideCollectionMemDrop();
    syncCollectionMemClear();
    return;
  }
  if (action === "coll-mem-remove") {
    e.preventDefault();
    removeCollectionMem(act.getAttribute("data-index"));
    return;
  }
  if (action === "coll-mem-more") {
    e.preventDefault();
    collectionMembersFreshFrom = collectionMembersVisible;
    collectionMembersVisible += COLL_MEMBER_PAGE;
    if (collectionFiche) renderCollectionDetail(collectionFiche);
    return;
  }
  if (action === "coll-pick-toggle") {
    e.preventDefault();
    toggleCollectionMenu();
    return;
  }
  if (action === "coll-pick-clear") {
    e.preventDefault();
    const input = $("#draftColl");
    if (input) {
      input.value = "";
      input.focus();
    }
    collectionPicker.showAll = false;
    syncCollectionClear();
    closeCollectionPicker();
    return;
  }
  if (action === "coll-pick-more") {
    e.preventDefault();
    collectionPicker.showAll = true;
    renderCollectionMenu($("#draftColl") ? $("#draftColl").value : "");
    return;
  }
  if (action === "coll-pick-item") {
    e.preventDefault();
    toggleCollectionPick(act.getAttribute("data-id"), act.getAttribute("data-label"));
    const input = $("#draftColl");
    if (input) {
      input.value = "";
      syncCollectionClear();
      input.focus();
    }
    collectionPicker.showAll = false;
    renderCollectionMenu("");
    return;
  }
  if (action === "coll-chip-remove") {
    e.preventDefault();
    toggleCollectionPick(act.getAttribute("data-id"), "");
    return;
  }
  if (!$("#draftCollPick") || $("#draftCollPick").contains(e.target)) return;
  closeCollectionPicker();
});

document.addEventListener("input", (e) => {
  if (e.target && e.target.id === "collMemQ") scheduleCollectionMemSearch();
  if (!e.target || e.target.id !== "draftColl") return;
  collectionPicker.showAll = false;
  syncCollectionClear();
  if (e.target.value.trim()) openCollectionMenu(false);
  else closeCollectionPicker();
});

document.addEventListener("focusin", (e) => {
  if (e.target && e.target.id === "draftColl") {
    const combo = $("#draftCollCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) openCollectionMenu(false);
  }
});

document.addEventListener("focusout", (e) => {
  if (e.target && e.target.id === "draftColl") {
    const combo = $("#draftCollCombo");
    requestAnimationFrame(() => {
      const pick = $("#draftCollPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
});

document.addEventListener("keydown", (e) => {
  if (e.target && e.target.id === "collMemQ") {
    const rows = shownCollectionMemRows();
    if (e.key === "ArrowDown") {
      e.preventDefault();
      collectionMemState.active = Math.min(collectionMemState.active + 1, Math.max(rows.length - 1, 0));
      rows.forEach((row, i) => row.classList.toggle("is-active", i === collectionMemState.active));
      return;
    }
    if (e.key === "ArrowUp") {
      e.preventDefault();
      collectionMemState.active = Math.max(collectionMemState.active - 1, 0);
      rows.forEach((row, i) => row.classList.toggle("is-active", i === collectionMemState.active));
      return;
    }
    if (e.key === "Escape") {
      hideCollectionMemDrop();
      return;
    }
    if (e.key === "Enter") {
      const row = rows[collectionMemState.active >= 0 ? collectionMemState.active : 0];
      if (row) {
        e.preventDefault();
        addCollectionMem(row.getAttribute("data-id"), row.getAttribute("data-label"));
      }
      return;
    }
  }
  if (!e.target || e.target.id !== "draftColl") return;
  const menu = $("#draftCollMenu");
  const open = menu && !menu.hidden;
  if (e.key === "ArrowDown") {
    e.preventDefault();
    if (!open) openCollectionMenu(!!e.target.value.trim() ? false : true);
    else setCollectionAcIdx(Math.min(collectionPicker.active + 1, Math.max(shownCollectionRows().length - 1, -1)));
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    setCollectionAcIdx(Math.max(collectionPicker.active - 1, -1));
  } else if (e.key === "Enter") {
    if (open && shownCollectionRows().length) {
      e.preventDefault();
      e.stopPropagation();
      pickActiveCollection();
    }
  }
});
