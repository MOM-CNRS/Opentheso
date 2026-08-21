/**
 * OpenTheso V2 — collections : arbre et fiche.
 */
"use strict";

var collectionTreeState = { key: "", loading: false };

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
  return fetch(ctx + "/v2/api/collection-tree/" + path + "?" + params.toString(), {
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
    paintCollectionVisibility();
    return;
  }
  tn.classList.add("is-open", "is-fetching");
  const parentId = tn.getAttribute("data-id");
  collectionApi("children", { parentId: parentId }).then(nodes => {
    tn.classList.remove("is-fetching");
    collectionDescendants(tn).forEach(el => el.remove());
    const depth = Number(tn.getAttribute("data-depth") || 0) + 1;
    const html = (Array.isArray(nodes) ? nodes : []).map(node => collectionNodeHtml(node, depth)).join("");
    if (html) tn.insertAdjacentHTML("afterend", html);
    tn.setAttribute("data-loaded", "1");
    if (!html) {
      tn.classList.remove("is-open");
      tn.setAttribute("data-has-children", "false");
      const caret = tn.querySelector(".tn-caret");
      if (caret) caret.classList.add("is-empty");
      return;
    }
    paintCollectionVisibility();
  }).catch(() => {
    tn.classList.remove("is-fetching", "is-open");
    toast("Impossible de charger les éléments de la collection.");
  });
}

function setCollectionSort(sort) {
  const next = sort === "nota" ? "nota" : "alpha";
  if (state.colSort === next) return;
  state.colSort = next;
  $$("#voColSortRow .vo-seg-b").forEach(b => {
    b.classList.toggle("is-on", b.getAttribute("data-sort") === next);
  });
  invalidateCollectionTree();
  if (state.view === "collection") loadCollectionRoots();
}

function openCollectionFromTree(id) {
  if (!id) return;
  state.view = "collection";
  state.home = false;
  state.draft = false;
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

function renderCollectionDetail(data) {
  const empty = $("#collectionEmpty");
  const loading = $("#collectionLoading");
  const box = $("#collectionDetail");
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
  const type = data.typeSkosLabel || data.typeLabel || data.typeCode || "";
  let html = '<div class="cv is-on is-collection" data-id="' + escapeHtml(data.groupId) + '">';
  html += '<div class="cv-head"><h1 class="cv-pref"><span>'
    + escapeHtml(dashText(data.label)) + '</span>'
    + '<span class="cv-lang">' + escapeHtml(dashText(data.lang)) + "</span>"
    + '<span class="cv-status is-collection">collection</span></h1></div>';
  html += '<div class="cv-blocks">';
  html += '<section class="cblock"><div class="cblock-head">Libellé</div><div class="cblock-body">';
  html += collectionCrow("Libellé",
    '<span class="val-strong">' + escapeHtml(dashText(data.label)) + "</span>"
    + ' <span class="val-lang">(' + escapeHtml(dashText(data.lang)) + ")</span>");
  html += collectionCrow("Type", escapeHtml(dashText(type)));
  html += collectionCrow("Membres", escapeHtml(String(data.memberCount != null ? data.memberCount : 0)));
  html += "</div></section>";
  html += '<section class="cblock"><div class="cblock-head">Traductions</div><div class="cblock-body">';
  if (!translations.length) {
    html += '<div class="muted">Aucune traduction renseignée.</div>';
  } else {
    translations.forEach(tr => {
      html += collectionCrow(
        '<span class="val-lang">' + escapeHtml(tr.lang || "") + "</span>",
        escapeHtml(tr.value || "")
      );
    });
  }
  html += "</div></section>";
  html += '<section class="cblock"><div class="cblock-head">Notes</div><div class="cblock-body">';
  if (!notes.length) {
    html += '<div class="muted">Aucune note renseignée.</div>';
  } else {
    notes.forEach(note => {
      html += '<div class="note-grp"><div class="note-grp-t">'
        + escapeHtml(note.typeLabel || "Note") + '</div><div class="note-line"><p class="cv-def">'
        + escapeHtml(note.value || "")
        + ' <span class="note-lang">(' + escapeHtml(note.lang || "") + ")</span></p></div></div>";
    });
  }
  html += "</div></section>";
  html += '<section class="cblock"><div class="cblock-head">Identifiants</div><div class="cblock-body">';
  html += collectionCrow("Identifiant", '<span class="is-mono">' + escapeHtml(dashText(data.groupId)) + "</span>");
  html += collectionCrow("Notation", escapeHtml(dashText(data.notation)));
  html += collectionCrow("ARK", '<span class="is-mono">' + escapeHtml(dashText(data.arkId)) + "</span>");
  html += collectionCrow("Handle", '<span class="is-mono">' + escapeHtml(dashText(data.handleId)) + "</span>");
  html += "</div></section>";
  html += '<section class="cblock"><div class="cblock-head">Information temporelle</div><div class="cblock-body">';
  html += collectionCrow("Créé le", '<span class="is-mono">' + escapeHtml(dashText(data.created)) + "</span>");
  html += collectionCrow("Dernière modification", '<span class="is-mono">' + escapeHtml(dashText(data.modified)) + "</span>");
  html += "</div></section></div></div>";
  box.innerHTML = html;
  box.hidden = false;
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
  }).catch(() => {
    if (state.colId !== groupId) return;
    if (loading) loading.hidden = true;
    if (empty) empty.hidden = false;
    toast("Impossible de charger la collection.");
  });
}
