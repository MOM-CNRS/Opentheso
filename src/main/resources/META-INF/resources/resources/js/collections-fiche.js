/** Collections — fiche et édition. */
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

