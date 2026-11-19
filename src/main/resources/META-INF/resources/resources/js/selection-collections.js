/** Sélection — ajout aux collections et changement de statut. */
var bulkAddCollItems = null;
var bulkAddCollKey = "";

function showAddCollErr(msg) {
  const err = $("#bulkAddCollErr");
  if (!err) return;
  err.textContent = msg || "";
  err.hidden = !msg;
}

function paintAddCollMembers() {
  const host = $("#bulkAddCollMembers");
  const count = $("#bulkAddCollMemberN");
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
  withSelectionPrefLabels(members, paintAddCollMembers);
  syncAddCollRun();
}

function syncAddCollRun() {
  const run = $("#bulkAddCollRun");
  if (!run) return;
  run.classList.toggle("is-off", !(state.addCollTarget && state.addCollTarget.id) || !selectedCollectionMembers().length);
}

function paintBulkAddCollPick() {
  const pick = $("#bulkAddCollPick");
  if (!pick || (state.addCollTarget && state.addCollTarget.id)) {
    if (pick) pick.hidden = true;
    return;
  }
  const qEl = $("#bulkAddCollQ");
  const raw = qEl ? qEl.value : "";
  const q = typeof norm === "function" ? norm(raw.trim()) : String(raw || "").trim().toLowerCase();
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  const items = (bulkAddCollItems || []).filter((item) => {
    if (!item || !item.id) return false;
    if (!q) return true;
    const hay = typeof norm === "function"
      ? norm((item.label || "") + " " + item.id + " " + (item.notation || ""))
      : ((item.label || "") + " " + item.id).toLowerCase();
    return hay.includes(q);
  });
  if (!bulkAddCollItems) {
    pick.innerHTML = '<div class="bulksel-empty">Chargement des collections…</div>';
    pick.hidden = false;
    return;
  }
  if (!items.length) {
    pick.innerHTML = '<div class="bulksel-empty">'
      + (q ? "Aucune collection ne correspond." : "Aucune collection dans ce thésaurus.")
      + "</div>";
    pick.hidden = false;
    return;
  }
  pick.innerHTML = items.slice(0, 12).map((item) => (
    '<button type="button" class="bulksel-po is-shown" data-act="bulk-add-coll-pick" data-id="'
    + esc(item.id) + '" data-pref="' + esc(item.label || item.id) + '">'
    + '<span class="bulksel-po-l">' + esc(item.label || item.id) + "</span>"
    + (item.notation ? '<span class="bulksel-po-p">' + esc(item.notation) + "</span>" : "")
    + "</button>"
  )).join("");
  pick.hidden = false;
}

function loadBulkAddCollections() {
  const key = (typeof thesaurusId === "function" ? thesaurusId() : "") + "|"
    + (typeof thesaurusLang === "function" ? thesaurusLang() : "");
  if (bulkAddCollItems && bulkAddCollKey === key) {
    paintBulkAddCollPick();
    return;
  }
  bulkAddCollItems = null;
  bulkAddCollKey = key;
  paintBulkAddCollPick();
  if (typeof collectionApi !== "function") {
    bulkAddCollItems = [];
    showAddCollErr("Liste des collections indisponible.");
    paintBulkAddCollPick();
    return;
  }
  collectionApi("list").then((rows) => {
    if (bulkAddCollKey !== key) return;
    bulkAddCollItems = Array.isArray(rows) ? rows : [];
    paintBulkAddCollPick();
  }).catch(() => {
    if (bulkAddCollKey !== key) return;
    bulkAddCollItems = [];
    showAddCollErr("Impossible de charger les collections.");
    paintBulkAddCollPick();
  });
}

function pickAddCollTarget(id, label) {
  if (!id) return;
  state.addCollTarget = { id: id, label: label || id };
  const box = $("#bulkAddCollTarget");
  const lab = $("#bulkAddCollTargetL");
  if (lab) lab.textContent = label || id;
  if (box) box.hidden = false;
  const q = $("#bulkAddCollQ");
  if (q) {
    q.value = "";
    const field = q.closest(".bap-field");
    if (field) field.hidden = true;
  }
  const pick = $("#bulkAddCollPick");
  if (pick) pick.hidden = true;
  showAddCollErr("");
  syncAddCollRun();
}

function clearAddCollTarget() {
  state.addCollTarget = null;
  const box = $("#bulkAddCollTarget");
  if (box) box.hidden = true;
  const q = $("#bulkAddCollQ");
  if (q) {
    const field = q.closest(".bap-field");
    if (field) field.hidden = false;
    q.focus();
  }
  showAddCollErr("");
  paintBulkAddCollPick();
  syncAddCollRun();
}

function resetAddColl() {
  hideBulkAddCollConfirm();
  state.addCollTarget = null;
  const box = $("#bulkAddCollTarget");
  if (box) box.hidden = true;
  const q = $("#bulkAddCollQ");
  if (q) {
    q.value = "";
    const field = q.closest(".bap-field");
    if (field) field.hidden = false;
  }
  showAddCollErr("");
  const status = $("#previewBulkAddCollStatus");
  if (status) {
    status.textContent = "";
    status.classList.remove("is-ok", "is-err");
  }
  paintAddCollMembers();
  syncAddCollRun();
  loadBulkAddCollections();
  window.setTimeout(() => {
    if (q && !q.closest(".bap-field")?.hidden) q.focus();
  }, 40);
}

function hideBulkAddCollConfirm() {
  if (typeof hideConfirm === "function") hideConfirm("#bulkAddCollConfirm");
}

function runBulkAddCollection() {
  const run = $("#bulkAddCollRun");
  if (run && run.classList.contains("is-off")) return;
  const target = state.addCollTarget;
  const members = selectedCollectionMembers();
  showAddCollErr("");
  if (!target || !target.id) {
    showAddCollErr("Choisissez une collection.");
    const q = $("#bulkAddCollQ");
    if (q) q.focus();
    return;
  }
  if (!members.length) {
    showAddCollErr("Aucun concept à rattacher.");
    return;
  }
  const text = $("#bulkAddCollConfirmText");
  if (text) {
    const n = members.length;
    const s = n > 1 ? "s" : "";
    text.textContent = n + " concept" + s + " "
      + (n > 1 ? "seront ajoutés" : "sera ajouté")
      + " à « " + (target.label || target.id) + " ».";
  }
  if (typeof showConfirm === "function") showConfirm("#bulkAddCollConfirm");
}

function confirmBulkAddCollection() {
  hideBulkAddCollConfirm();
  const target = state.addCollTarget;
  const members = selectedCollectionMembers();
  if (!target || !target.id || !members.length) {
    runBulkAddCollection();
    return;
  }
  if (typeof clickPreviewJsf !== "function") {
    showAddCollErr("Ajout indisponible.");
    return;
  }
  clickPreviewJsf("previewBulkAddCollGo", {
    previewBulkAddCollId: target.id,
    previewBulkAddCollIds: members.join("\n")
  });
}

function onBulkAddCollAjax(data) {
  if (!data) return;
  const run = $("#bulkAddCollRun");
  if (data.status === "begin") {
    hideBulkAddCollConfirm();
    if (run) run.classList.add("is-off");
    return;
  }
  if (data.status !== "success") {
    syncAddCollRun();
    return;
  }
  const status = document.getElementById("previewBulkAddCollStatus");
  const ok = !!(status && status.classList.contains("is-ok"));
  const msg = status ? (status.textContent || "").trim() : "";
  if (ok) {
    if (typeof toast === "function") toast(msg || "Concepts ajoutés à la collection");
    if (typeof invalidateCollectionTree === "function") invalidateCollectionTree();
    clearSelection();
    return;
  }
  showAddCollErr(msg || "Ajout impossible.");
  syncAddCollRun();
}

function bulkAct(label) {
  const n = state.selected.size;
  const s = n > 1 ? "s" : "";
  toast(label + " · " + n + " concept" + s);
  clearSelection();
}

function statusMemberKey() {
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  const lang = typeof thesaurusLang === "function" ? thesaurusLang() : "fr";
  if (state.selectedAllThesaurus) return theso + "|" + lang + "|*";
  const roots = typeof selectionRootIds === "function" ? selectionRootIds() : selectedCollectionMembers();
  return theso + "|" + lang + "|" + roots.slice().sort().join("\n");
}

function statusMatchesAction(status, action) {
  const st = String(status || "").toLowerCase();
  if (action === "approve" || action === "reject") return st === "candidat";
  if (action === "deprecate") return !st || st === "valide" || st === "insere";
  return true;
}

function fallbackStatusMembers() {
  return selectedCollectionMembers().map((id) => {
    const node = typeof treeNodes === "function"
      ? treeNodes().find((n) => n.getAttribute("data-id") === id)
      : null;
    return {
      id: id,
      label: selectionPrefLabel(id),
      status: ((node && node.getAttribute("data-status")) || "").toLowerCase(),
      placement: "direct"
    };
  });
}

function currentStatusMembers() {
  if (state.statusMemberKey === statusMemberKey() && Array.isArray(state.statusMemberList)) {
    return state.statusMemberList;
  }
  return fallbackStatusMembers();
}

function selectedStatusMembers(action) {
  const seen = new Set();
  const ids = [];
  currentStatusMembers().forEach((member) => {
    if (!member || !member.id || seen.has(member.id)) return;
    if (action && !statusMatchesAction(member.status, action)) return;
    seen.add(member.id);
    ids.push(member.id);
  });
  return ids;
}

function statusMemberChips(members) {
  return (members || []).filter(member => member && member.id).map(member => {
    const apiLabel = (member.label || "").trim();
    return {
      id: member.id,
      label: apiLabel || selectionPrefLabel(member.id),
      status: member.status,
      placement: member.placement || "direct",
      title: member.id
    };
  });
}

function loadStatusMembers() {
  const host = $("#bulkStatusMembers");
  if (!host) return;
  const key = statusMemberKey();
  if (state.statusMemberKey === key && Array.isArray(state.statusMemberList)) {
    paintStatusMembers();
    syncStatusRun();
    return;
  }
  const ctx = document.body.getAttribute("data-ctx") || "";
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  const lang = typeof thesaurusLang === "function" ? thesaurusLang() : "fr";
  const params = new URLSearchParams();
  if (theso) params.set("thesaurusId", theso);
  if (lang) params.set("lang", lang);
  if (state.selectedAllThesaurus) {
    params.set("all", "true");
  } else {
    const roots = typeof selectionRootIds === "function" ? selectionRootIds() : selectedCollectionMembers();
    if (roots.length) params.set("ids", roots.join(","));
  }
  const req = (state.statusMemberReq || 0) + 1;
  state.statusMemberReq = req;
  paintStatusMembers();
  syncStatusRun();
  if (!theso || (!state.selectedAllThesaurus && !params.get("ids"))) {
    state.statusMemberKey = key;
    state.statusMemberList = fallbackStatusMembers();
    paintStatusMembers();
    syncStatusRun();
    return;
  }
  fetch(ctx + "/api/subtree-members?" + params.toString(), {
    credentials: "same-origin",
    headers: { Accept: "application/json" }
  }).then((res) => (res.ok ? res.json() : []))
    .then((rows) => {
      if (state.statusMemberReq !== req) return;
      const list = Array.isArray(rows) ? rows : [];
      const byId = new Map();
      list.forEach((row) => {
        if (!row || !row.id) return;
        const label = (row.label || "").trim() || row.id;
        rememberSelectionPrefLabel(row.id, label, lang);
        byId.set(row.id, {
          id: row.id,
          label: label,
          status: String(row.status || "").toLowerCase(),
          placement: row.placement === "transversal" ? "transversal" : "direct"
        });
      });
      fallbackStatusMembers().forEach((member) => {
        if (!byId.has(member.id)) byId.set(member.id, member);
      });
      state.statusMemberKey = key;
      state.statusMemberList = [...byId.values()];
      paintStatusMembers();
      syncStatusRun();
    })
    .catch(() => {
      if (state.statusMemberReq !== req) return;
      state.statusMemberKey = key;
      state.statusMemberList = fallbackStatusMembers();
      paintStatusMembers();
      syncStatusRun();
    });
}

function resetStatus() {
  hideBulkStatusConfirm();
  state.bulkStatusAction = "";
  state.statusMemberKey = "";
  state.statusMemberList = [];
  $$("#bulkStatus .bap-choice").forEach((btn) => btn.classList.remove("is-on"));
  showStatusErr("");
  loadStatusMembers();
}

function paintStatusMembers() {
  const host = $("#bulkStatusMembers");
  const count = $("#bulkStatusMemberN");
  if (!host) return;
  const action = state.bulkStatusAction || "";
  const all = currentStatusMembers();
  const members = action ? all.filter((member) => statusMatchesAction(member.status, action)) : all;
  if (!all.length) {
    renderSelectionChips(host, [], '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne changent pas de statut.</p>');
  } else if (action && !members.length) {
    renderSelectionChips(host, [], action === "approve" || action === "reject"
      ? '<p class="bap-members-empty">Aucun candidat dans la sélection.</p>'
      : '<p class="bap-members-empty">Aucun concept validé à rendre obsolète.</p>');
  } else {
    renderSelectionChips(host, statusMemberChips(members).map(chip => {
      const st = String(chip.status || "").toLowerCase();
      const cls = (st === "candidat" ? " is-candidate" : (st === "deprecie" ? " is-deprecated" : ""))
        + (chip.placement === "transversal" ? " is-transvers" : "");
      return { id: chip.id, label: chip.label, cls: cls, title: chip.title || chip.id };
    }), "");
  }
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
  const hint = $("#bulkStatusHint span");
  if (hint) {
    if (action === "approve") hint.textContent = "Seuls les candidats de la sélection seront validés.";
    else if (action === "reject") hint.textContent = "Seuls les candidats de la sélection seront refusés.";
    else if (action === "deprecate") hint.textContent = "Seuls les concepts validés seront rendus obsolètes. Les candidats et les déjà obsolètes restent inchangés.";
    else hint.textContent = "Choisissez une action. Seuls les concepts au statut correspondant seront modifiés.";
  }
}

function statusMembersReady() {
  return state.statusMemberKey === statusMemberKey() && Array.isArray(state.statusMemberList);
}

function syncStatusRun() {
  const run = $("#bulkStatusRun");
  if (!run) return;
  const action = state.bulkStatusAction || "";
  run.classList.toggle("is-off", !action || !statusMembersReady() || !selectedStatusMembers(action).length);
}

function pickStatusAction(action) {
  state.bulkStatusAction = action === "approve" || action === "reject" || action === "deprecate" ? action : "";
  $$("#bulkStatus .bap-choice").forEach((btn) => {
    btn.classList.toggle("is-on", btn.getAttribute("data-status") === state.bulkStatusAction);
  });
  showStatusErr("");
  paintStatusMembers();
  syncStatusRun();
}

function showStatusErr(msg) {
  const err = $("#bulkStatusErr");
  if (!err) return;
  if (msg) {
    err.hidden = false;
    err.textContent = msg;
  } else {
    err.hidden = true;
    err.textContent = "";
  }
}

function hideBulkStatusConfirm() {
  if (typeof hideConfirm === "function") hideConfirm("#bulkStatusConfirm");
}

function ensureBulkStatusConfirm() {
  if ($("#bulkStatusConfirm")) return;
  const host = $("#viewBulk") || document.body;
  const box = document.createElement("div");
  box.className = "block-overlay confirm-overlay";
  box.id = "bulkStatusConfirm";
  box.hidden = true;
  box.setAttribute("data-act", "bulk-status-dismiss");
  box.setAttribute("role", "presentation");
  box.innerHTML = '<div class="confirm-modal" role="dialog" aria-modal="true"'
    + ' aria-labelledby="bulkStatusConfirmTitle" data-act="bulk-status-modal">'
    + '<div class="confirm-h" id="bulkStatusConfirmTitle">Changer le statut ?</div>'
    + '<p class="confirm-p" id="bulkStatusConfirmText">Le statut des concepts concernés sera modifié.</p>'
    + '<div class="confirm-actions">'
    + '<button type="button" class="bo-btn ghost confirm-cancel" data-act="bulk-status-dismiss">Annuler</button>'
    + '<button type="button" class="bo-btn primary" id="bulkStatusConfirmGo" data-act="bulk-status-go">Appliquer</button>'
    + "</div></div>";
  host.appendChild(box);
}

function runBulkStatus() {
  const run = $("#bulkStatusRun");
  if (run && run.classList.contains("is-off")) return;
  const action = state.bulkStatusAction || "";
  const members = selectedStatusMembers(action);
  showStatusErr("");
  if (!action) {
    showStatusErr("Choisissez un statut.");
    return;
  }
  if (!members.length) {
    showStatusErr(action === "approve"
      ? "Aucun candidat à valider."
      : (action === "reject" ? "Aucun candidat à refuser." : "Aucun concept à rendre obsolète."));
    return;
  }
  ensureBulkStatusConfirm();
  const title = $("#bulkStatusConfirmTitle");
  const text = $("#bulkStatusConfirmText");
  const n = members.length;
  const s = n > 1 ? "s" : "";
  if (action === "approve") {
    if (title) title.textContent = "Valider ces candidats ?";
    if (text) text.textContent = n + " candidat" + s + " " + (n > 1 ? "passeront" : "passera")
      + " au statut de concept validé.";
  } else if (action === "reject") {
    if (title) title.textContent = "Refuser ces candidats ?";
    if (text) text.textContent = n + " candidat" + s + " " + (n > 1 ? "seront refusés" : "sera refusé") + ".";
  } else {
    if (title) title.textContent = "Rendre ces concepts obsolètes ?";
    if (text) text.textContent = n + " concept" + s + " " + (n > 1 ? "seront dépréciés" : "sera déprécié")
      + " sans être " + (n > 1 ? "supprimés" : "supprimé") + ".";
  }
  if (typeof showConfirm === "function") showConfirm("#bulkStatusConfirm");
}

function confirmBulkStatus() {
  hideBulkStatusConfirm();
  const action = state.bulkStatusAction || "";
  const members = selectedStatusMembers(action);
  if (!action || !members.length) {
    runBulkStatus();
    return;
  }
  if (typeof clickPreviewJsf !== "function") {
    showStatusErr("Changement de statut indisponible.");
    return;
  }
  clickPreviewJsf("previewBulkStatusGo", {
    previewBulkStatusAction: action,
    previewBulkStatusIds: members.join("\n")
  });
}

function onBulkStatusAjax(data) {
  if (!data) return;
  const run = $("#bulkStatusRun");
  if (data.status === "begin") {
    hideBulkStatusConfirm();
    if (run) run.classList.add("is-off");
    return;
  }
  if (data.status !== "success") {
    syncStatusRun();
    return;
  }
  const status = document.getElementById("previewBulkStatusStatus");
  const ok = !!(status && status.classList.contains("is-ok"));
  const msg = status ? (status.textContent || "").trim() : "";
  if (ok) {
    toast(msg || "Statut modifié");
    if (typeof syncStatusUi === "function") syncStatusUi();
    clearSelection();
    return;
  }
  showStatusErr(msg || "Changement de statut impossible.");
  syncStatusRun();
}

