/** Sélection — libellés et liste détaillée. */
const selectionPrefLabels = new Map();

function selectionLabelLang() {
  return (typeof thesaurusLang === "function" ? thesaurusLang() : "") || "fr";
}

function selectionPrefKey(id, lang) {
  return (lang || selectionLabelLang()) + "\0" + id;
}

function selectionPrefLabel(id) {
  const cached = selectionPrefLabels.get(selectionPrefKey(id));
  if (cached) return cached;
  const node = typeof treeNodes === "function"
    ? treeNodes().find((n) => n.getAttribute("data-id") === id)
    : null;
  const prefEl = node && node.querySelector(".tn-text");
  const text = ((prefEl && prefEl.textContent) || "").trim();
  return text || id;
}

function rememberSelectionPrefLabel(id, label, lang) {
  const text = (label || "").trim();
  if (!id || !text) return;
  selectionPrefLabels.set(selectionPrefKey(id, lang), text);
}

function loadSelectionPrefLabels(ids) {
  const lang = selectionLabelLang();
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  const missing = [...new Set((ids || []).filter(Boolean))]
    .filter(id => !selectionPrefLabels.has(selectionPrefKey(id, lang)));
  if (!theso || !missing.length) return Promise.resolve(false);
  const ctx = document.body.getAttribute("data-ctx") || "";
  const chunks = [];
  for (let i = 0; i < missing.length; i += 80) chunks.push(missing.slice(i, i + 80));
  return Promise.all(chunks.map(chunk => {
    const params = new URLSearchParams();
    params.set("thesaurusId", theso);
    params.set("lang", lang);
    params.set("ids", chunk.join(","));
    return fetch(ctx + "/api/concept-labels?" + params.toString(), {
      credentials: "same-origin",
      cache: "no-store",
      headers: { Accept: "application/json" }
    }).then(res => {
      if (!res.ok) return null;
      return res.json();
    }).then(rows => {
      if (!Array.isArray(rows)) return false;
      const seen = new Set();
      rows.forEach(row => {
        if (!row || !row.id) return;
        seen.add(row.id);
        rememberSelectionPrefLabel(row.id, row.label || row.id, lang);
      });
      chunk.forEach(id => {
        if (!seen.has(id)) rememberSelectionPrefLabel(id, id, lang);
      });
      return true;
    }).catch(() => false);
  })).then(flags => flags.some(Boolean) && selectionLabelLang() === lang);
}

function withSelectionPrefLabels(ids, repaint) {
  const lang = selectionLabelLang();
  const missing = (ids || []).filter(id => id && !selectionPrefLabels.has(selectionPrefKey(id, lang)));
  if (!missing.length) return;
  loadSelectionPrefLabels(missing).then(sameLang => {
    if (!sameLang || selectionLabelLang() !== lang) return;
    repaint();
  });
}

const SELECTION_PREVIEW = 10;
const selectionLists = new Map();
let selectionListHostId = "";
let selectionListOpen = [];

function renderSelectionChips(host, items, emptyHtml) {
  if (!host) return;
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  if (!items || !items.length) {
    if (emptyHtml) host.innerHTML = emptyHtml;
    selectionLists.delete(host.id);
    if (selectionListHostId === host.id) closeSelectionList();
    return;
  }
  const heading = host.parentElement && host.parentElement.querySelector(".bap-field-l");
  host.setAttribute("data-list-title", (heading && heading.textContent.trim()) || "Concepts sélectionnés");
  selectionLists.set(host.id, items);
  const shown = items.slice(0, SELECTION_PREVIEW);
  const extra = items.length - shown.length;
  host.innerHTML = shown.map(item => (
    '<span class="bap-chip' + (item.cls || "") + '" title="' + esc(item.title || item.id) + '">'
    + esc(item.label || item.id) + "</span>"
  )).join("") + (extra > 0
    ? '<button type="button" class="bap-chip bap-chip-more" data-act="sel-list-open">Voir les '
      + items.length.toLocaleString("fr-FR") + " concepts</button>"
    : "");
  if (selectionListHostId === host.id) {
    const modal = $("#selListModal");
    if (modal && !modal.hidden) {
      selectionListOpen = items;
      paintSelectionListModal();
    }
  }
}

function ensureSelectionListModal() {
  if ($("#selListModal")) return;
  const box = document.createElement("div");
  box.className = "block-overlay confirm-overlay";
  box.id = "selListModal";
  box.hidden = true;
  box.setAttribute("data-act", "sel-list-dismiss");
  box.setAttribute("role", "presentation");
  box.innerHTML = '<div class="confirm-modal sel-list-modal" role="dialog" aria-modal="true"'
    + ' aria-labelledby="selListTitle" data-act="sel-list-modal">'
    + '<div class="confirm-h" id="selListTitle">Concepts sélectionnés</div>'
    + '<p class="confirm-p" id="selListCount"></p>'
    + '<input class="bulksel-input bap-input" id="selListQuery" type="search" placeholder="Filtrer par nom ou identifiant…" autocomplete="off"/>'
    + '<div class="sel-list-body" id="selListBody"></div>'
    + '<div class="confirm-actions">'
    + '<button type="button" class="bo-btn ghost confirm-cancel" data-act="sel-list-dismiss">Fermer</button>'
    + "</div></div>";
  document.body.appendChild(box);
  const query = box.querySelector("#selListQuery");
  if (query) query.addEventListener("input", paintSelectionListModal);
}

function openSelectionList(host) {
  if (!host) return;
  const items = selectionLists.get(host.id) || [];
  if (!items.length) return;
  ensureSelectionListModal();
  selectionListHostId = host.id;
  selectionListOpen = items;
  const title = $("#selListTitle");
  if (title) title.textContent = host.getAttribute("data-list-title") || "Concepts sélectionnés";
  const query = $("#selListQuery");
  if (query) query.value = "";
  paintSelectionListModal();
  if (typeof showConfirm === "function") showConfirm("#selListModal");
  if (query) query.focus();
  const ids = items.map(item => item.id);
  withSelectionPrefLabels(ids, () => {
    if (selectionListHostId !== host.id) return;
    selectionListOpen = items.map(item => ({
      ...item,
      label: selectionPrefLabel(item.id) || item.label
    }));
    selectionLists.set(host.id, selectionListOpen);
    paintSelectionListModal();
  });
}

function closeSelectionList() {
  selectionListHostId = "";
  selectionListOpen = [];
  if (typeof hideConfirm === "function") hideConfirm("#selListModal");
}

function paintSelectionListModal() {
  const body = $("#selListBody");
  const count = $("#selListCount");
  if (!body) return;
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  const raw = ($("#selListQuery") && $("#selListQuery").value) || "";
  const q = typeof norm === "function" ? norm(raw.trim()) : raw.trim().toLowerCase();
  const rows = selectionListOpen.filter(item => {
    if (!q) return true;
    const hay = typeof norm === "function"
      ? norm((item.label || "") + " " + (item.id || ""))
      : ((item.label || "") + " " + (item.id || "")).toLowerCase();
    return hay.includes(q);
  });
  if (count) {
    count.textContent = q
      ? rows.length.toLocaleString("fr-FR") + " sur " + selectionListOpen.length.toLocaleString("fr-FR")
      : selectionListOpen.length.toLocaleString("fr-FR") + " concept" + (selectionListOpen.length > 1 ? "s" : "");
  }
  if (!rows.length) {
    body.innerHTML = '<p class="bap-members-empty">Aucun concept ne correspond.</p>';
    return;
  }
  body.innerHTML = '<ul class="sel-list">' + rows.map(item => (
    '<li class="sel-list-row' + (item.cls || "") + '">'
    + '<span class="sel-list-name">' + esc(item.label || item.id) + "</span>"
    + '<span class="sel-list-id">' + esc(item.id) + "</span></li>"
  )).join("") + "</ul>";
}

function refreshOpenSelectionLabels() {
  const coll = $("#bulkColl");
  if (coll && !coll.hidden) paintCollMembers();
  const addColl = $("#bulkAddColl");
  if (addColl && !addColl.hidden) paintAddCollMembers();
  const move = $("#bulkMove");
  if (move && !move.hidden) paintMoveMembers();
  const xfer = $("#bulkXfer");
  if (xfer && !xfer.hidden) paintXferMembers();
  const status = $("#bulkStatus");
  if (status && !status.hidden) {
    state.statusMemberKey = "";
    state.statusMemberList = [];
    loadStatusMembers();
  }
  const exp = $("#bulkExport");
  if (exp && !exp.hidden && typeof paintExportMembers === "function") paintExportMembers();
}

function resetColl() {
  hideBulkCollConfirm();
  const input = $("#bulkCollName");
  if (input) input.value = "";
  showCollErr("");
  const status = $("#previewBulkCollStatus");
  if (status) {
    status.textContent = "";
    status.classList.remove("is-ok", "is-err");
  }
  paintCollMembers();
  syncCollRun();
  window.setTimeout(() => {
    if (input) input.focus();
  }, 40);
}

function paintCollMembers() {
  const host = $("#bulkCollMembers");
  const count = $("#bulkCollMemberN");
  if (!host) return;
  const members = selectedCollectionMembers();
  renderSelectionChips(
    host,
    members.map(id => ({ id: id, label: selectionPrefLabel(id) })),
    '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne peuvent pas rejoindre une collection.</p>'
  );
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
  withSelectionPrefLabels(members, paintCollMembers);
}

function syncCollRun() {
  const run = $("#bulkCollRun");
  if (!run) return;
  const input = $("#bulkCollName");
  const name = input ? input.value.trim() : "";
  run.classList.toggle("is-off", !name || !selectedCollectionMembers().length);
}

function showCollErr(msg) {
  const err = $("#bulkCollErr");
  if (!err) return;
  err.textContent = msg || "";
  err.hidden = !msg;
}

function ensureBulkCollConfirm() {
  if ($("#bulkCollConfirm")) return;
  const host = $("#viewBulk") || document.body;
  const box = document.createElement("div");
  box.className = "block-overlay confirm-overlay";
  box.id = "bulkCollConfirm";
  box.hidden = true;
  box.setAttribute("data-act", "bulk-coll-dismiss");
  box.setAttribute("role", "presentation");
  box.innerHTML = '<div class="confirm-modal" role="dialog" aria-modal="true"'
    + ' aria-labelledby="bulkCollConfirmTitle" data-act="bulk-coll-modal">'
    + '<div class="confirm-h" id="bulkCollConfirmTitle">Créer cette collection ?</div>'
    + '<p class="confirm-p" id="bulkCollConfirmText">La collection sera créée et les concepts sélectionnés y seront rattachés.</p>'
    + '<div class="confirm-actions">'
    + '<button type="button" class="bo-btn ghost confirm-cancel" data-act="bulk-coll-dismiss">Annuler</button>'
    + '<button type="button" class="bo-btn primary" id="bulkCollConfirmGo" data-act="bulk-coll-go">Créer la collection</button>'
    + "</div></div>";
  host.appendChild(box);
}

function hideBulkCollConfirm() {
  if (typeof hideConfirm === "function") hideConfirm("#bulkCollConfirm");
}

function runBulkCollection() {
  const run = $("#bulkCollRun");
  if (run && run.classList.contains("is-off")) return;
  const name = (($("#bulkCollName") && $("#bulkCollName").value) || "").trim();
  const members = selectedCollectionMembers();
  showCollErr("");
  if (!name) {
    showCollErr("Le nom de la collection est obligatoire.");
    const input = $("#bulkCollName");
    if (input) input.focus();
    return;
  }
  if (!members.length) {
    showCollErr("Aucun concept à rattacher.");
    return;
  }
  ensureBulkCollConfirm();
  const text = $("#bulkCollConfirmText");
  if (text) {
    const n = members.length;
    const s = n > 1 ? "s" : "";
    text.textContent = "La collection « " + name + " » regroupera " + n
      + " concept" + s + ". Ils gardent leur place dans l'arbre.";
  }
  if (typeof showConfirm === "function") {
    showConfirm("#bulkCollConfirm");
    return;
  }
  confirmBulkCollection();
}

function confirmBulkCollection() {
  hideBulkCollConfirm();
  const name = (($("#bulkCollName") && $("#bulkCollName").value) || "").trim();
  const members = selectedCollectionMembers();
  if (!name || !members.length) {
    runBulkCollection();
    return;
  }
  if (typeof clickPreviewJsf !== "function") {
    showCollErr("Création indisponible.");
    return;
  }
  clickPreviewJsf("previewBulkCollGo", {
    previewBulkCollName: name,
    previewBulkCollIds: members.join("\n")
  });
}

function onBulkCollAjax(data) {
  if (!data) return;
  const run = $("#bulkCollRun");
  if (data.status === "begin") {
    hideBulkCollConfirm();
    if (run) run.classList.add("is-off");
    return;
  }
  if (data.status !== "success") {
    syncCollRun();
    return;
  }
  const status = document.getElementById("previewBulkCollStatus");
  const ok = !!(status && status.classList.contains("is-ok"));
  const msg = status ? (status.textContent || "").trim() : "";
  if (ok) {
    toast(msg || "Collection créée");
    clearSelection();
    return;
  }
  showCollErr(msg || "Création impossible.");
  syncCollRun();
}

