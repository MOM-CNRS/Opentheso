/**
 * OpenTheso V2 — sélection, barre d'actions, déplacement.
 */
"use strict";

function persistTableCols() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  const selected = TABLE_COL_ALL.filter((s) => state.tblCols.has(s));
  const payload = { selected: selected };
  try {
    document.body.setAttribute("data-table-cols", JSON.stringify(payload));
  } catch (ex) {}
  const ctx = document.body.getAttribute("data-ctx") || "";
  clearTimeout(persistTableCols._t);
  persistTableCols._t = setTimeout(() => {
    fetch(ctx + "/v2/api/account/table-cols", {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload)
    }).catch(() => {});
  }, 180);
}

function resetTableColPref() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  state.tblCols = new Set(TABLE_COL_DEFAULT);
  applyTableCols();
  persistTableCols();
  const root = $("#accTableCols");
  const msg = root && root.getAttribute("data-msg-reset-done");
  if (msg && typeof toast === "function") toast(msg);
}

function persistTreeStatus() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  const selected = TREE_STATUS_ALL.filter((s) => state.statusSet.has(s));
  const payload = { selected: selected };
  try {
    document.body.setAttribute("data-tree-status", JSON.stringify(payload));
  } catch (ex) {}
  const ctx = document.body.getAttribute("data-ctx") || "";
  clearTimeout(persistTreeStatus._t);
  persistTreeStatus._t = setTimeout(() => {
    fetch(ctx + "/v2/api/account/tree-status", {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload)
    }).catch(() => {});
  }, 180);
}

function resetTreeStatusPref() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  state.statusSet = new Set(TREE_STATUS_DEFAULT);
  syncStatusUi();
  persistTreeStatus();
  const root = $("#accTreeStatus");
  const msg = root && root.getAttribute("data-msg-reset-done");
  if (msg && typeof toast === "function") toast(msg);
}

function syncStatusUi() {
  Object.keys(GROUPS).forEach(key => {
    const list = GROUPS[key];
    const nOn = list.filter(s => state.statusSet.has(s)).length;
    const gs = nOn === 0 ? "off" : (nOn === list.length ? "on" : "mixed");
    $$(`.stk-group[data-group="${key}"]`).forEach((group) => {
      const head = group.querySelector(".stk-ghead");
      const input = group.querySelector(".stk-ghead input");
      const box = group.querySelector(".stk-ghead .stk-box");
      if (head) head.classList.toggle("on", gs !== "off");
      group.classList.toggle("on", gs !== "off");
      if (input) {
        input.checked = gs === "on";
        input.indeterminate = gs === "mixed";
      }
      if (box) {
        box.classList.toggle("mixed", gs === "mixed");
        box.textContent = gs === "on" ? "✓" : gs === "mixed" ? "–" : "";
      }
      list.forEach(s => {
        const lab = group.querySelector(`.stk-item[data-status="${s}"]`);
        if (!lab) return;
        const on = state.statusSet.has(s);
        const item = lab.querySelector("input");
        if (item) item.checked = on;
        lab.classList.toggle("on", on);
        const ib = lab.querySelector(".stk-box");
        if (ib) ib.textContent = on ? "✓" : "";
      });
    });
  });
  syncCandFilterUi();
  applyStatusFilter();
}

function syncCandFilterUi() {
  const active = !!(state.candBy || state.candFrom || state.candTo);
  const clear = $("#cfClear");
  if (clear) clear.hidden = !active;
}

function currentUsername() {
  return document.body.getAttribute("data-username") || "";
}

function cfMsg(attr, fallback) {
  const combo = $("#cfCombo");
  return (combo && combo.getAttribute(attr)) || fallback;
}

function candByDisplayLabel(by) {
  if (!by) return cfMsg("data-everyone", "Tout le monde");
  if (by === currentUsername()) {
    return cfMsg("data-me", "Moi ({0})").replace("{0}", by);
  }
  return by;
}

function setCandBySelection(by) {
  state.candBy = by || "";
  const lab = $("#cfByLabel");
  if (lab) lab.textContent = candByDisplayLabel(state.candBy);
  $$("#cfByList .cf-opt").forEach((o) => {
    o.classList.toggle("on", (o.getAttribute("data-by") || "") === state.candBy);
  });
  syncCandFilterUi();
}

function candByOptHtml(by, label, hint, on) {
  return '<button type="button" class="cf-opt' + (on ? " on" : "") + '" data-act="cf-by" data-by="'
    + escapeHtml(by) + '" data-label="' + escapeHtml(label) + '">'
    + escapeHtml(label)
    + (hint ? "<small>" + escapeHtml(hint) + "</small>" : "")
    + "</button>";
}

function renderCandByOptions(users, opts) {
  const list = $("#cfByList");
  if (!list) return;
  const pending = !!(opts && opts.pending);
  const everyone = cfMsg("data-everyone", "Tout le monde");
  const everyoneHint = cfMsg("data-everyone-hint", "tous les candidats");
  const meTpl = cfMsg("data-me", "Moi ({0})");
  const meHint = cfMsg("data-me-hint", "mes candidats");
  const empty = cfMsg("data-empty", "Aucun utilisateur");
  const me = currentUsername();
  const selected = state.candBy || "";
  let html = candByOptHtml("", everyone, everyoneHint, selected === "");
  if (me) {
    html += candByOptHtml(me, meTpl.replace("{0}", me), me + " · " + meHint, selected === me);
  }
  const rows = (users || []).filter((u) => u && u.username && u.username !== me);
  if (selected && selected !== me && !rows.some((u) => u.username === selected)) {
    rows.unshift({ username: selected });
  }
  if (!pending && !rows.length) {
    html += '<div class="cf-empty">' + escapeHtml(empty) + "</div>";
  } else {
    rows.forEach((u) => {
      html += candByOptHtml(u.username, u.username, "", selected === u.username);
    });
  }
  list.innerHTML = html;
}

function loadCandByUsers(q) {
  const list = $("#cfByList");
  if (!list) return Promise.resolve();
  const ctx = document.body.getAttribute("data-ctx") || "";
  const query = q ? String(q).trim() : "";
  const url = ctx + "/v2/api/users" + (query ? "?q=" + encodeURIComponent(query) : "");
  return fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((r) => (r.ok ? r.json() : []))
    .then((users) => {
      renderCandByOptions(Array.isArray(users) ? users : []);
    })
    .catch(() => {
      renderCandByOptions([]);
    });
}

function resetCandBySearch() {
  const input = $("#cfByQuery");
  if (input) input.value = "";
  loadCandByUsers("");
}

function collectIds(tn) {
  const ids = [];
  const push = id => { if (id && !ids.includes(id)) ids.push(id); };
  push(tn.getAttribute("data-id"));
  const depth = treeDepth(tn);
  let next = tn.nextElementSibling;
  while (next && next.classList.contains("tn")) {
    if (treeDepth(next) <= depth) break;
    push(next.getAttribute("data-id"));
    next = next.nextElementSibling;
  }
  return ids;
}

function paintSelectedId(id, on) {
  $$(`[data-id="${CSS.escape(id)}"]`).forEach(el => {
    const check = el.matches(".tn-check") ? el : (el.querySelector && el.querySelector(".tn-check"));
    const row = el.closest && (el.closest(".tn-row") || el.closest("tr"));
    if (check) check.classList.toggle("on", on);
    if (row) row.classList.toggle("is-sel", on);
  });
}

function setSelectedIds(ids, on) {
  if (!on) state.selectedAllThesaurus = false;
  ids.forEach(id => {
    if (on) state.selected.add(id);
    else state.selected.delete(id);
    paintSelectedId(id, on);
  });
  updateBulk();
}

function restoreSelection() {
  treeNodes().forEach(tn => {
    const id = tn.getAttribute("data-id");
    if (!id) return;
    let selected = state.selected.has(id);
    if (!selected) {
      let parent = previousTreeParent(tn);
      while (parent && !selected) {
        const parentId = parent.getAttribute("data-id");
        if (parentId && state.selected.has(parentId)) selected = true;
        parent = previousTreeParent(parent);
      }
    }
    if (selected) {
      state.selected.add(id);
      paintSelectedId(id, true);
    }
  });
  updateBulk();
}

function clearSelection(keepUi) {
  if (!keepUi) setSelectedIds([...state.selected], false);
  else {
    $$(".tn-check.on").forEach(c => c.classList.remove("on"));
    $$(".is-sel").forEach(r => r.classList.remove("is-sel"));
  }
  state.selected.clear();
  state.selectedAllThesaurus = false;
  bulkMode("acts");
  updateBulk();
}

function selectionRootIds() {
  const roots = [];
  const seen = new Set();
  treeNodes().forEach(tn => {
    const id = tn.getAttribute("data-id");
    if (!id || !state.selected.has(id) || seen.has(id)) return;
    let parent = previousTreeParent(tn);
    while (parent) {
      const parentId = parent.getAttribute("data-id");
      if (parentId && state.selected.has(parentId)) return;
      parent = previousTreeParent(parent);
    }
    seen.add(id);
    roots.push(id);
  });
  state.selected.forEach(id => {
    if (!seen.has(id) && !$$(`.tn[data-id="${CSS.escape(id)}"]`).length) {
      seen.add(id);
      roots.push(id);
    }
  });
  return roots;
}


function selectedCount() {
  if (state.selectedAllThesaurus) {
    const total = thesaurusConceptCount();
    if (total) return total;
  }
  const roots = selectionRootIds();
  if (!roots.length) return state.selected.size;
  let n = 0;
  let complete = true;
  roots.forEach(id => {
    if (state.subtreeSize.has(id)) n += state.subtreeSize.get(id);
    else complete = false;
  });
  return complete ? n : Math.max(n, state.selected.size);
}

function fetchSubtreeSize(id, nodeType) {
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({ id });
  if (nodeType) params.set("nodeType", nodeType);
  const theso = thesaurusId();
  if (theso) params.set("thesaurusId", theso);
  return fetch(ctx + "/v2/api/subtree-size?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then(res => res.ok ? res.json() : { size: 1 })
    .then(data => {
      const size = Number(data && data.size);
      return Number.isFinite(size) && size > 0 ? size : 1;
    })
    .catch(() => 1);
}

function ensureSubtreeSize(id, nodeType, hasChildren) {
  if (!id) return Promise.resolve(1);
  if (state.subtreeSize.has(id)) return Promise.resolve(state.subtreeSize.get(id));
  if (hasChildren === false) {
    state.subtreeSize.set(id, 1);
    return Promise.resolve(1);
  }
  if (state.subtreeSizePending.has(id)) return state.subtreeSizePending.get(id);
  const pending = fetchSubtreeSize(id, nodeType).then(size => {
    state.subtreeSize.set(id, size);
    state.subtreeSizePending.delete(id);
    updateBulk();
    return size;
  });
  state.subtreeSizePending.set(id, pending);
  return pending;
}

function refreshSubtreeCounts() {
  selectionRootIds().forEach(id => {
    const tn = $(`.tn[data-id="${CSS.escape(id)}"]`);
    ensureSubtreeSize(
      id,
      tn && tn.getAttribute("data-type"),
      tn && tn.getAttribute("data-has-children") === "true"
    );
  });
  updateBulk();
}

function bulkSelectionActive() {
  const n = selectedCount();
  return n > 0 && (state.view === "arbo" || state.view === "tableau" || state.view === "hyper")
      && !state.conceptDraft && !state.facetDraft && !state.draft;
}

function updateBulk() {
  const n = selectedCount();
  const ok = n > 0 && (state.view === "arbo" || state.view === "tableau" || state.view === "hyper");
  const bar = $("#bulkSel");
  if (bar) bar.classList.toggle("is-on", ok);
  const b = $("#bulkN");
  if (b) b.textContent = String(n);
  const main = $("#bulkNMain");
  if (main) main.textContent = String(n);
  const plural = n > 1 ? "s" : "";
  ["bulkPlural", "bulkPlural2", "bulkPlural3"].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.textContent = plural;
  });
  $$(".bap-form-n").forEach((el) => { el.textContent = String(n); });
  $$(".bap-form-plural").forEach((el) => { el.textContent = plural; });
  const coll = $("#bulkColl");
  if (coll && !coll.hidden) {
    paintCollMembers();
    syncCollRun();
  }
  const move = $("#bulkMove");
  if (move && !move.hidden) {
    paintMoveMembers();
    syncMoveRun();
  }
  const xfer = $("#bulkXfer");
  if (xfer && !xfer.hidden) {
    paintXferMembers();
    syncXferRun();
  }
  const allBtn = $("#bulkSel .bulksel-all");
  if (allBtn) {
    const visibleIds = visibleSelectableIds();
    const allOn = state.selectedAllThesaurus
      || (visibleIds.length > 0 && visibleIds.every(id => state.selected.has(id)));
    allBtn.hidden = allOn;
  }
  const exportPanel = $("#bulkExport");
  if (exportPanel && !exportPanel.hidden) refreshExportSummary();
  const statusPanel = $("#bulkStatus");
  if (statusPanel && !statusPanel.hidden) {
    paintStatusMembers();
    syncStatusRun();
  }
  const settingsOn = $("#viewSettings") && $("#viewSettings").classList.contains("is-on");
  if (settingsOn || state.conceptDraft || state.facetDraft || state.draft) return;
  const panel = $("#viewBulk");
  const showing = !!(panel && panel.classList.contains("is-on"));
  const want = bulkSelectionActive();
  if (want !== showing && typeof paintMain === "function") paintMain();
}

function visibleSelectableIds() {
  if (state.view === "tableau") {
    if (tableRowsCache.length) {
      return filteredSortedTableRows().map(row => row.id).filter(Boolean);
    }
    return $$("#panelTable tr[data-id]:not(.is-status-off)")
      .map(r => r.getAttribute("data-id"))
      .filter(Boolean);
  }
  const ids = [];
  treeNodes().forEach(tn => {
    if (tn.classList.contains("is-status-off")) return;
    const id = tn.getAttribute("data-id");
    if (id && !ids.includes(id)) ids.push(id);
  });
  return ids;
}

function selectAllVisible() {
  const ids = visibleSelectableIds();
  if (!ids.length) return;
  setSelectedIds(ids, true);
  if (state.view === "tableau") return;
  state.selectedAllThesaurus = true;
  updateBulk();
}

function bulkMode(mode) {
  if (exportBusy && mode !== "export") return;
  const acts = $("#bulkActs"), coll = $("#bulkColl"), move = $("#bulkMove"), exp = $("#bulkExport"), xfer = $("#bulkXfer"), status = $("#bulkStatus");
  if (acts) acts.hidden = mode !== "acts";
  if (coll) coll.hidden = mode !== "coll";
  if (move) move.hidden = mode !== "move";
  if (xfer) xfer.hidden = mode !== "xfer";
  if (status) status.hidden = mode !== "status";
  if (exp) exp.hidden = mode !== "export";
  const bar = $("#bulkSel");
  if (bar) bar.classList.toggle("is-export", mode === "export");
  const panel = $("#viewBulk");
  if (panel) panel.classList.toggle("is-export", mode === "export");
  paintXferSteps();
  if (mode === "move") resetMove();
  else {
    state.moveTarget = null;
    hideBulkMoveConfirm();
  }
  if (mode === "xfer") resetXfer();
  else resetXferUi();
  if (mode === "export") {
    resetExportPanelForNewExport();
    scrollExportPanelBottom();
  }
  if (mode === "coll") resetColl();
  if (mode === "status") resetStatus();
  else hideBulkStatusConfirm();
}

function selectedCollectionMembers() {
  return [...state.selected].filter((id) => {
    if (typeof treeNodes !== "function") return true;
    const node = treeNodes().find((n) => n.getAttribute("data-id") === id);
    if (!node) return true;
    return (node.getAttribute("data-type") || "").toLowerCase() !== "facet";
  });
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
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  if (!members.length) {
    host.innerHTML = '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne peuvent pas rejoindre une collection.</p>';
  } else {
    host.innerHTML = members.map((id) => {
      const node = typeof treeNodes === "function"
        ? treeNodes().find((n) => n.getAttribute("data-id") === id)
        : null;
      const prefEl = node && node.querySelector(".tn-text");
      const pref = ((prefEl && prefEl.textContent) || "").trim() || id;
      return '<span class="bap-chip" title="' + esc(id) + '">' + esc(pref) + "</span>";
    }).join("");
  }
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
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

function bulkAct(label) {
  const n = state.selected.size;
  const s = n > 1 ? "s" : "";
  toast(label + " · " + n + " concept" + s);
  clearSelection();
}

function selectedStatusMembers(action) {
  const members = selectedCollectionMembers();
  if (!action) return members;
  return members.filter((id) => {
    if (typeof treeNodes !== "function") return true;
    const node = treeNodes().find((n) => n.getAttribute("data-id") === id);
    if (!node) return true;
    const st = (node.getAttribute("data-status") || "").toLowerCase();
    if (action === "approve") return st === "candidat";
    if (action === "deprecate") return !st || st === "valide" || st === "insere";
    return true;
  });
}

function resetStatus() {
  hideBulkStatusConfirm();
  state.bulkStatusAction = "";
  $$("#bulkStatus .bap-choice").forEach((btn) => btn.classList.remove("is-on"));
  showStatusErr("");
  paintStatusMembers();
  syncStatusRun();
}

function paintStatusMembers() {
  const host = $("#bulkStatusMembers");
  const count = $("#bulkStatusMemberN");
  if (!host) return;
  const action = state.bulkStatusAction || "";
  const members = selectedStatusMembers(action);
  const all = selectedCollectionMembers();
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  if (!all.length) {
    host.innerHTML = '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne changent pas de statut.</p>';
  } else if (action && !members.length) {
    host.innerHTML = action === "approve"
      ? '<p class="bap-members-empty">Aucun candidat dans la sélection.</p>'
      : '<p class="bap-members-empty">Aucun concept validé à rendre obsolète.</p>';
  } else {
    const shown = (action ? members : all).slice(0, 28);
    const extra = (action ? members : all).length - shown.length;
    host.innerHTML = shown.map((id) => {
      const node = typeof treeNodes === "function"
        ? treeNodes().find((n) => n.getAttribute("data-id") === id)
        : null;
      const prefEl = node && node.querySelector(".tn-text");
      const pref = ((prefEl && prefEl.textContent) || "").trim() || id;
      const st = ((node && node.getAttribute("data-status")) || "").toLowerCase();
      const cls = st === "candidat" ? " is-candidate" : (st === "deprecie" ? " is-deprecated" : "");
      return '<span class="bap-chip' + cls + '" title="' + esc(id) + '">' + esc(pref) + "</span>";
    }).join("") + (extra > 0 ? '<span class="bap-chip bap-chip-more">+' + extra + "</span>" : "");
  }
  if (count) {
    const n = (action ? members : all).length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
  const hint = $("#bulkStatusHint span");
  if (hint) {
    if (action === "approve") hint.textContent = "Seuls les candidats de la sélection seront validés.";
    else if (action === "deprecate") hint.textContent = "Seuls les concepts validés seront rendus obsolètes. Les candidats et les déjà obsolètes restent inchangés.";
    else hint.textContent = "Choisissez une action. Seuls les concepts au statut correspondant seront modifiés.";
  }
}

function syncStatusRun() {
  const run = $("#bulkStatusRun");
  if (!run) return;
  const action = state.bulkStatusAction || "";
  run.classList.toggle("is-off", !action || !selectedStatusMembers(action).length);
}

function pickStatusAction(action) {
  state.bulkStatusAction = action === "approve" || action === "deprecate" ? action : "";
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
      : "Aucun concept à rendre obsolète.");
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
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  if (!members.length) {
    host.innerHTML = '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne sont pas déplacées.</p>';
  } else {
    host.innerHTML = members.map((id) => {
      const node = typeof treeNodes === "function"
        ? treeNodes().find((n) => n.getAttribute("data-id") === id)
        : null;
      const prefEl = node && node.querySelector(".tn-text");
      const pref = ((prefEl && prefEl.textContent) || "").trim() || id;
      return '<span class="bap-chip" title="' + esc(id) + '">' + esc(pref) + "</span>";
    }).join("");
  }
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
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
    + '<p class="confirm-p" id="bulkMoveConfirmText">Les concepts sélectionnés changeront de terme générique.</p>'
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
  ensureBulkMoveConfirm();
  const text = $("#bulkMoveConfirmText");
  if (text) {
    const n = members.length;
    const s = n > 1 ? "s" : "";
    const dest = state.moveTarget.id === "__root"
      ? "à la racine du thésaurus"
      : "sous « " + state.moveTarget.pref + " »";
    text.textContent = n + " concept" + s + " seront déplacé" + s + " " + dest + ".";
  }
  if (typeof showConfirm === "function") {
    showConfirm("#bulkMoveConfirm");
    return;
  }
  confirmBulkMove();
}

function confirmBulkMove() {
  hideBulkMoveConfirm();
  const members = selectedMoveMembers();
  if (!members.length || !state.moveTarget) {
    runBulkMove();
    return;
  }
  if (typeof clickPreviewJsf !== "function") {
    showMoveErr("Déplacement indisponible.");
    return;
  }
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
  const esc = typeof escapeHtml === "function" ? escapeHtml : (v) => String(v);
  if (!members.length) {
    host.innerHTML = '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne sont pas transférées.</p>';
  } else {
    host.innerHTML = members.map((id) => {
      const node = typeof treeNodes === "function"
        ? treeNodes().find((n) => n.getAttribute("data-id") === id)
        : null;
      const prefEl = node && node.querySelector(".tn-text");
      const pref = ((prefEl && prefEl.textContent) || "").trim() || id;
      return '<span class="bap-chip" title="' + esc(id) + '">' + esc(pref) + "</span>";
    }).join("");
  }
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
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
  fetch(ctx + "/v2/api/concepts/search?" + params.toString(), {
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
  ensureBulkXferConfirm();
  const text = $("#bulkXferConfirmText");
  if (text) {
    const n = members.length;
    const s = n > 1 ? "s" : "";
    const dest = state.xferParent.id === "__root"
      ? "à la racine"
      : "sous « " + state.xferParent.pref + " »";
    text.textContent = n + " concept" + s + " (et leur branche) seront déplacé" + s
      + " vers « " + state.xferTarget.name + " », " + dest + ".";
  }
  if (typeof showConfirm === "function") {
    showConfirm("#bulkXferConfirm");
    return;
  }
  confirmBulkXfer();
}

function confirmBulkXfer() {
  hideBulkXferConfirm();
  const members = selectedMoveMembers();
  if (!members.length || !state.xferTarget || !state.xferParent) {
    runBulkXfer();
    return;
  }
  if (typeof clickPreviewJsf !== "function") {
    showXferErr("Déplacement indisponible.");
    return;
  }
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
