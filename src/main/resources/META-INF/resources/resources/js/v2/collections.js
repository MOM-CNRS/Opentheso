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

function collectionMembersHtml(members) {
  if (!members.length) {
    return '<div class="muted">Aucun concept rattaché à cette collection.</div>';
  }
  let html = '<div class="rel-items">';
  members.forEach(member => {
    const id = member.conceptId || "";
    const label = member.label || id;
    html += '<button type="button" class="rel-term" data-act="open" data-id="'
      + escapeHtml(id) + '" data-type="concept">'
      + escapeHtml(label) + "</button>";
  });
  html += "</div>";
  return html;
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
  const members = Array.isArray(data.members) ? data.members : [];
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
  html += "</div></section>";
  html += '<section class="cblock"><div class="cblock-head">Membres</div><div class="cblock-body">';
  html += collectionMembersHtml(members);
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
  const act = e.target.closest("[data-act]");
  const action = act && act.getAttribute("data-act");
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
