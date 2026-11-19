/** Collections — arbre. */
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
  box.innerHTML = nodes.map(node => collectionNodeHtml(node, 0, "")).join("");
  paintCollectionVisibility();
  if (state.colId) highlightConcept(state.colId);
  else if (state.conceptId) highlightConcept(state.conceptId);
}

function collectionNodeHtml(node, depth, parentKey) {
  const id = node && node.id != null ? String(node.id) : "";
  const key = (parentKey ? parentKey + "/" : "") + id;
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
    + ' data-key="' + escapeHtml(key) + '"'
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
  const parentKey = parent.getAttribute("data-key") || parent.getAttribute("data-id") || "";
  const start = page.shown;
  const end = Math.min(page.nodes.length, start + COLL_TREE_PAGE);
  const html = page.nodes.slice(start, end).map((node) => collectionNodeHtml(node, depth, parentKey)).join("");
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

