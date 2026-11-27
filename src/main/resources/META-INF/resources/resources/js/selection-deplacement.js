/** Sélection — déplacement. */
function selectedMoveMembers() {
  return selectedCollectionMembers();
}

function resetMove() {
  hideBulkMoveConfirm();
  state.moveTarget = null;
  const tgt = $("#bulkMoveTarget"); if (tgt) tgt.hidden = true;
  const lab = $("#bulkMoveTargetL"); if (lab) lab.textContent = "";
  const q = $("#bulkMoveQ");
  if (q) {
    q.value = "";
    q.hidden = false;
    const field = q.closest(".bap-field");
    if (field) field.hidden = false;
  }
  showMoveErr("");
  paintMoveMembers();
  fillMovePick("");
  syncMoveRun();
  window.setTimeout(() => {
    if (q) q.focus();
  }, 40);
}

function paintMoveMembers() {
  const host = $("#bulkMoveMembers");
  const count = $("#bulkMoveMemberN");
  if (!host) return;
  const members = selectedMoveMembers();
  renderSelectionChips(
    host,
    members.map(id => ({ id: id, label: selectionPrefLabel(id) })),
    '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne sont pas déplacées.</p>'
  );
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
  withSelectionPrefLabels(members, paintMoveMembers);
}

function syncMoveRun() {
  const run = $("#bulkMoveRun");
  if (!run) return;
  run.classList.toggle("is-off", !state.moveTarget || !selectedMoveMembers().length);
}

function showMoveErr(msg) {
  const err = $("#bulkMoveErr");
  if (!err) return;
  err.textContent = msg || "";
  err.hidden = !msg;
}

function isMoveCandidate(tn) {
  if (!tn) return false;
  const id = tn.getAttribute("data-id");
  const type = (tn.getAttribute("data-type") || "").toLowerCase();
  if (!id || type === "facet" || state.selected.has(id)) return false;
  if (typeof previousTreeParent !== "function") return true;
  let parent = previousTreeParent(tn);
  while (parent) {
    const pid = parent.getAttribute("data-id");
    if (pid && state.selected.has(pid)) return false;
    parent = previousTreeParent(parent);
  }
  return true;
}

function fillMovePick(qraw) {
  const pick = $("#bulkMovePick");
  if (!pick || state.moveTarget) {
    if (pick) pick.hidden = true;
    return;
  }
  const q = typeof norm === "function" ? norm((qraw || "").trim()) : String(qraw || "").trim().toLowerCase();
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  const rows = [];
  rows.push(
    '<button type="button" class="bulksel-po is-shown is-root" data-act="bulk-move-pick" data-id="__root" data-pref="À la racine du thésaurus">'
    + '<span class="bulksel-po-l">À la racine du thésaurus</span>'
    + '<span class="bulksel-po-p">aucun terme générique</span></button>'
  );
  let n = 0;
  if (typeof treeNodes === "function") {
    treeNodes().forEach((tn) => {
      if (n >= 12) return;
      if (!isMoveCandidate(tn)) return;
      const id = tn.getAttribute("data-id");
      const labelEl = tn.querySelector(".tn-text");
      const pref = ((labelEl && labelEl.textContent) || "").trim();
      const nota = (tn.getAttribute("data-nota") || "").trim();
      const hay = (typeof norm === "function" ? norm(pref + " " + id + " " + nota) : (pref + " " + id + " " + nota).toLowerCase());
      if (q && !hay.includes(q)) return;
      n += 1;
      rows.push(
        '<button type="button" class="bulksel-po is-shown" data-act="bulk-move-pick" data-id="' + esc(id)
        + '" data-pref="' + esc(pref) + '"><span class="bulksel-po-l">' + esc(pref) + "</span>"
        + '<span class="bulksel-po-p">' + esc(id) + "</span></button>"
      );
    });
  }
  if (q && n === 0) rows.push('<div class="bulksel-empty">Aucun terme trouvé.</div>');
  pick.innerHTML = rows.join("");
  pick.hidden = false;
}

function filterMovePick(qraw) {
  fillMovePick(qraw);
}

function pickMoveTarget(id, pref) {
  state.moveTarget = { id: id, pref: pref };
  const box = $("#bulkMoveTarget");
  const lab = $("#bulkMoveTargetL");
  if (lab) lab.textContent = pref || id;
  if (box) box.hidden = false;
  const pick = $("#bulkMovePick");
  if (pick) pick.hidden = true;
  const q = $("#bulkMoveQ");
  if (q) {
    q.hidden = true;
    const field = q.closest(".bap-field");
    if (field) field.hidden = true;
  }
  showMoveErr("");
  syncMoveRun();
}

function clearMoveTarget() {
  state.moveTarget = null;
  const box = $("#bulkMoveTarget");
  if (box) box.hidden = true;
  const q = $("#bulkMoveQ");
  if (q) {
    q.value = "";
    q.hidden = false;
    const field = q.closest(".bap-field");
    if (field) field.hidden = false;
    q.focus();
  }
  fillMovePick("");
  syncMoveRun();
}

function hideBulkMoveConfirm() {
  if (typeof hideConfirm === "function") hideConfirm("#bulkMoveConfirm");
}

function ensureBulkMoveConfirm() {
  if ($("#bulkMoveConfirm")) return;
  const host = $("#viewBulk") || document.body;
  const box = document.createElement("div");
  box.className = "block-overlay confirm-overlay";
  box.id = "bulkMoveConfirm";
  box.hidden = true;
  box.setAttribute("data-act", "bulk-move-dismiss");
  box.setAttribute("role", "presentation");
  box.innerHTML = '<div class="confirm-modal" role="dialog" aria-modal="true"'
    + ' aria-labelledby="bulkMoveConfirmTitle" data-act="bulk-move-modal">'
    + '<div class="confirm-h" id="bulkMoveConfirmTitle">Déplacer ces concepts ?</div>'
    + '<p class="confirm-p" id="bulkMoveConfirmText">Les concepts sélectionnés changeront de terme générique. La hiérarchie entre eux est conservée.</p>'
    + '<div class="confirm-actions">'
    + '<button type="button" class="bo-btn ghost confirm-cancel" data-act="bulk-move-dismiss">Annuler</button>'
    + '<button type="button" class="bo-btn primary" id="bulkMoveConfirmGo" data-act="bulk-move-go">Déplacer</button>'
    + "</div></div>";
  host.appendChild(box);
}

function runBulkMove() {
  const run = $("#bulkMoveRun");
  if (run && run.classList.contains("is-off")) return;
  const members = selectedMoveMembers();
  showMoveErr("");
  if (!members.length) {
    showMoveErr("Aucun concept à déplacer.");
    return;
  }
  if (!state.moveTarget) {
    showMoveErr("Choisissez une destination.");
    return;
  }
  // Pas de seconde confirmation : le bouton « Déplacer » du formulaire suffit
  confirmBulkMove();
}

function confirmBulkMove() {
  hideBulkMoveConfirm();
  const runBtn = $("#bulkMoveRun");
  const members = selectedMoveMembers();
  if (!members.length || !state.moveTarget) {
    runBulkMove();
    return;
  }
  if (typeof clickPreviewJsf !== "function") {
    showMoveErr("Déplacement indisponible.");
    return;
  }
  if (runBtn) runBtn.classList.add("is-off");
  clickPreviewJsf("previewBulkMoveGo", {
    previewBulkMoveTarget: state.moveTarget.id,
    previewBulkMoveIds: members.join("\n")
  });
}

function onBulkMoveAjax(data) {
  if (!data) return;
  const run = $("#bulkMoveRun");
  if (data.status === "begin") {
    hideBulkMoveConfirm();
    if (run) run.classList.add("is-off");
    return;
  }
  if (data.status !== "success") {
    syncMoveRun();
    return;
  }
  const status = document.getElementById("previewBulkMoveStatus");
  const ok = !!(status && status.classList.contains("is-ok"));
  const msg = status ? (status.textContent || "").trim() : "";
  if (ok) {
    toast(msg || "Concepts déplacés");
    if (typeof syncStatusUi === "function") syncStatusUi();
    clearSelection();
    return;
  }
  showMoveErr(msg || "Déplacement impossible.");
  syncMoveRun();
}

function paintXferSteps() {
  const steps = $("#bulkXferSteps");
  if (!steps) return;
  const step1 = steps.querySelector('[data-step="1"]');
  const step2 = steps.querySelector('[data-step="2"]');
  if (step1) step1.classList.toggle("is-on", !state.xferTarget);
  if (step2) step2.classList.toggle("is-on", !!state.xferTarget);
}

function resetXferUi() {
  hideBulkXferConfirm();
  state.xferTarget = null;
  state.xferParent = null;
  const tgt = $("#bulkXferTarget"); if (tgt) tgt.hidden = true;
  const par = $("#bulkXferParent"); if (par) par.hidden = true;
  const q = $("#bulkXferQ");
  if (q) {
    q.value = "";
    q.hidden = false;
    const qField = q.closest(".bap-field");
    if (qField) qField.hidden = false;
  }
  const pqField = $("#bulkXferPQField"); if (pqField) pqField.hidden = true;
  const pq = $("#bulkXferPQ"); if (pq) { pq.value = ""; pq.hidden = true; }
  const run = $("#bulkXferRun"); if (run) run.classList.add("is-off");
  showXferErr("");
  paintXferSteps();
}

function resetXfer() {
  resetXferUi();
  paintXferMembers();
  fillXferPick("");
  syncXferRun();
  const q = $("#bulkXferQ");
  window.setTimeout(() => {
    if (q) q.focus();
  }, 40);
}

function paintXferMembers() {
  const host = $("#bulkXferMembers");
  const count = $("#bulkXferMemberN");
  if (!host) return;
  const members = selectedMoveMembers();
  renderSelectionChips(
    host,
    members.map(id => ({ id: id, label: selectionPrefLabel(id) })),
    '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne sont pas transférées.</p>'
  );
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
  withSelectionPrefLabels(members, paintXferMembers);
}

function showXferErr(msg) {
  const err = $("#bulkXferErr");
  if (!err) return;
  err.textContent = msg || "";
  err.hidden = !msg;
}

function writableThesauri() {
  const jsonEl = $("#bulkWritableThesauriJson");
  if (jsonEl) {
    try {
      const list = JSON.parse((jsonEl.textContent || "").trim() || "[]");
      if (Array.isArray(list)) return list;
    } catch (e) {}
  }
  const el = $("#bulkWritableThesauri");
  if (!el) return [];
  try {
    const list = JSON.parse(el.getAttribute("data-json") || "[]");
    return Array.isArray(list) ? list : [];
  } catch (e) {
    return [];
  }
}

function xferRootButton(esc) {
  return '<button type="button" class="bulksel-po is-shown is-root" data-act="bulk-xfer-parent-pick" data-id="__root" data-pref="À la racine du thésaurus">'
    + '<span class="bulksel-po-l">À la racine du thésaurus</span>'
    + '<span class="bulksel-po-p">aucun terme générique</span></button>';
}

function fillXferPick(qraw) {
  const pick = $("#bulkXferPick");
  if (!pick) return;
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  if (state.xferParent) {
    pick.hidden = true;
    return;
  }
  if (state.xferTarget) {
    fillXferParentPick(qraw, pick, esc);
    return;
  }
  const current = typeof thesaurusId === "function" ? thesaurusId() : "";
  const q = typeof norm === "function" ? norm((qraw || "").trim()) : String(qraw || "").trim().toLowerCase();
  const hits = writableThesauri().filter((x) => {
    if (!x || !x.id || x.id === current) return false;
    if (!q) return true;
    const hay = (typeof norm === "function" ? norm : (v) => String(v || "").toLowerCase())(
      [x.name, x.id].filter(Boolean).join(" ")
    );
    return hay.includes(q);
  }).slice(0, 20);
  if (!hits.length) {
    pick.innerHTML = '<div class="bulksel-empty">'
      + (q ? "Aucun thésaurus trouvé." : "Aucun thésaurus accessible en écriture.")
      + "</div>";
    pick.hidden = false;
    return;
  }
  pick.innerHTML = hits.map((x) => (
    '<button type="button" class="bulksel-po is-shown" data-act="bulk-xfer-pick" data-id="' + esc(x.id)
    + '" data-name="' + esc(x.name || x.id) + '"><span class="bulksel-po-l">' + esc(x.name || x.id)
    + "</span><span class=\"bulksel-po-p\">" + esc(x.id) + "</span></button>"
  )).join("");
  pick.hidden = false;
}

function fillXferParentPick(qraw, pick, esc) {
  const q = String(qraw || "").trim();
  const root = xferRootButton(esc);
  if (!q) {
    pick.innerHTML = root;
    pick.hidden = false;
    return;
  }
  const seq = (fillXferParentPick._seq = (fillXferParentPick._seq || 0) + 1);
  pick.innerHTML = root + '<div class="bulksel-empty">Recherche…</div>';
  pick.hidden = false;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: state.xferTarget.id,
    q: q
  });
  const lang = typeof thesaurusLang === "function" ? thesaurusLang() : "";
  if (lang) params.set("lang", lang);
  fetch(ctx + "/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" },
    credentials: "same-origin"
  }).then((res) => (res.ok ? res.json() : [])).then((items) => {
    if (seq !== fillXferParentPick._seq || !state.xferTarget || state.xferParent) return;
    const hits = Array.isArray(items) ? items.filter((item) => item && item.id) : [];
    let html = root;
    if (!hits.length) html += '<div class="bulksel-empty">Aucun concept trouvé.</div>';
    else {
      html += hits.slice(0, 12).map((item) => (
        '<button type="button" class="bulksel-po is-shown" data-act="bulk-xfer-parent-pick" data-id="'
        + esc(item.id) + '" data-pref="' + esc(item.label || item.id) + '"><span class="bulksel-po-l">'
        + esc(item.label || item.id) + '</span><span class="bulksel-po-p">' + esc(item.id) + "</span></button>"
      )).join("");
    }
    pick.innerHTML = html;
  }).catch(() => {
    if (seq !== fillXferParentPick._seq) return;
    pick.innerHTML = root + '<div class="bulksel-empty">Recherche indisponible.</div>';
  });
}

function syncXferRun() {
  const run = $("#bulkXferRun");
  if (!run) return;
  run.classList.toggle("is-off", !(state.xferTarget && state.xferParent && selectedMoveMembers().length));
}

function pickXferThesaurus(id, name) {
  state.xferTarget = { id: id, name: name };
  state.xferParent = null;
  const box = $("#bulkXferTarget");
  const lab = $("#bulkXferTargetL");
  if (lab) lab.textContent = name || id;
  if (box) box.hidden = false;
  const q = $("#bulkXferQ");
  if (q) {
    q.hidden = true;
    const qField = q.closest(".bap-field");
    if (qField) qField.hidden = true;
  }
  const pq = $("#bulkXferPQ");
  const pqField = $("#bulkXferPQField");
  if (pqField) pqField.hidden = false;
  if (pq) {
    pq.hidden = false;
    pq.placeholder = "Concept de « " + (name || id) + " » qui accueillera…";
    pq.value = "";
    window.setTimeout(() => pq.focus(), 0);
  }
  const par = $("#bulkXferParent");
  if (par) par.hidden = true;
  showXferErr("");
  fillXferPick("");
  paintXferSteps();
  syncXferRun();
}

function pickXferParent(id, pref) {
  state.xferParent = { id: id, pref: pref };
  const box = $("#bulkXferParent");
  const lab = $("#bulkXferParentL");
  if (lab) lab.textContent = pref || id;
  if (box) box.hidden = false;
  const pq = $("#bulkXferPQ");
  if (pq) pq.hidden = true;
  const pqField = $("#bulkXferPQField");
  if (pqField) pqField.hidden = true;
  const pick = $("#bulkXferPick");
  if (pick) pick.hidden = true;
  showXferErr("");
  syncXferRun();
}

function clearXferParent() {
  state.xferParent = null;
  const par = $("#bulkXferParent");
  if (par) par.hidden = true;
  const pq = $("#bulkXferPQ");
  const pqField = $("#bulkXferPQField");
  if (pqField) pqField.hidden = false;
  if (pq) {
    pq.value = "";
    pq.hidden = false;
    pq.focus();
  }
  fillXferPick("");
  syncXferRun();
}

function hideBulkXferConfirm() {
  if (typeof hideConfirm === "function") hideConfirm("#bulkXferConfirm");
}

function ensureBulkXferConfirm() {
  if ($("#bulkXferConfirm")) return;
  const host = $("#viewBulk") || document.body;
  const box = document.createElement("div");
  box.className = "block-overlay confirm-overlay";
  box.id = "bulkXferConfirm";
  box.hidden = true;
  box.setAttribute("data-act", "bulk-xfer-dismiss");
  box.setAttribute("role", "presentation");
  box.innerHTML = '<div class="confirm-modal" role="dialog" aria-modal="true"'
    + ' aria-labelledby="bulkXferConfirmTitle" data-act="bulk-xfer-modal">'
    + '<div class="confirm-h" id="bulkXferConfirmTitle">Déplacer vers un autre thésaurus ?</div>'
    + '<p class="confirm-p" id="bulkXferConfirmText">Les concepts quitteront ce thésaurus avec leur branche.</p>'
    + '<div class="confirm-actions">'
    + '<button type="button" class="bo-btn ghost confirm-cancel" data-act="bulk-xfer-dismiss">Annuler</button>'
    + '<button type="button" class="bo-btn primary" id="bulkXferConfirmGo" data-act="bulk-xfer-go">Déplacer</button>'
    + "</div></div>";
  host.appendChild(box);
}

function runBulkXfer() {
  const run = $("#bulkXferRun");
  if (run && run.classList.contains("is-off")) return;
  const members = selectedMoveMembers();
  showXferErr("");
  if (!members.length) {
    showXferErr("Aucun concept à déplacer.");
    return;
  }
  if (!state.xferTarget) {
    showXferErr("Choisissez un thésaurus de destination");
    return;
  }
  if (!state.xferParent) {
    showXferErr("Choisissez un emplacement");
    return;
  }
  // Pas de seconde confirmation : le bouton « Déplacer » du formulaire suffit
  confirmBulkXfer();
}

function confirmBulkXfer() {
  hideBulkXferConfirm();
  const runBtn = $("#bulkXferRun");
  const members = selectedMoveMembers();
  if (!members.length || !state.xferTarget || !state.xferParent) {
    runBulkXfer();
    return;
  }
  if (typeof clickPreviewJsf !== "function") {
    showXferErr("Déplacement indisponible.");
    return;
  }
  if (runBtn) runBtn.classList.add("is-off");
  clickPreviewJsf("previewBulkXferGo", {
    previewBulkXferTarget: state.xferTarget.id,
    previewBulkXferParent: state.xferParent.id,
    previewBulkXferIds: members.join("\n")
  });
}

function onBulkXferAjax(data) {
  if (!data) return;
  const run = $("#bulkXferRun");
  if (data.status === "begin") {
    hideBulkXferConfirm();
    if (run) run.classList.add("is-off");
    return;
  }
  if (data.status !== "success") {
    syncXferRun();
    return;
  }
  const status = document.getElementById("previewBulkXferStatus");
  const ok = !!(status && status.classList.contains("is-ok"));
  const msg = status ? (status.textContent || "").trim() : "";
  if (ok) {
    toast(msg || "Concepts déplacés");
    if (typeof syncStatusUi === "function") syncStatusUi();
    clearSelection();
    return;
  }
  showXferErr(msg || "Déplacement impossible.");
  syncXferRun();
}

document.addEventListener("click", (e) => {
  const treeReset = e.target.closest("[data-act='acc-tree-reset']");
  if (treeReset) {
    e.preventDefault();
    resetTreeStatusPref();
    return;
  }
  const tableReset = e.target.closest("[data-act='acc-table-reset']");
  if (tableReset) {
    e.preventDefault();
    resetTableColPref();
  }
});

if (typeof syncStatusUi === "function") {
  syncStatusUi();
}
if ($("#cfByList")) {
  renderCandByOptions([], { pending: true });
}
