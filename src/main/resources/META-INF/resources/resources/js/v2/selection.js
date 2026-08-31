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

function updateBulk() {
  const bar = $("#bulkSel");
  const n = selectedCount();
  const ok = n > 0 && (state.view === "arbo" || state.view === "tableau" || state.view === "hyper");
  if (bar) bar.classList.toggle("is-on", ok);
  const b = $("#bulkN");
  if (b) b.textContent = String(n);
  const plural = $("#bulkPlural");
  if (plural) plural.textContent = n > 1 ? "s" : "";
  const plural2 = $("#bulkPlural2");
  if (plural2) plural2.textContent = n > 1 ? "s" : "";
  const allBtn = $("#bulkSel .bulksel-all");
  if (allBtn) {
    const visibleIds = visibleSelectableIds();
    const allOn = state.selectedAllThesaurus
      || (visibleIds.length > 0 && visibleIds.every(id => state.selected.has(id)));
    allBtn.hidden = allOn;
  }
  const exportPanel = $("#bulkExport");
  if (exportPanel && !exportPanel.hidden) refreshExportSummary();
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
  const acts = $("#bulkActs"), coll = $("#bulkColl"), move = $("#bulkMove"), exp = $("#bulkExport");
  if (acts) acts.hidden = mode !== "acts";
  if (coll) coll.hidden = mode !== "coll";
  if (move) move.hidden = mode !== "move";
  if (exp) exp.hidden = mode !== "export";
  const bar = $("#bulkSel");
  if (bar) bar.classList.toggle("is-export", mode === "export");
  $("#bulkStatusMenu") && $("#bulkStatusMenu").classList.remove("is-on");
  if (mode !== "move") {
    state.moveTarget = null;
    const tgt = $("#bulkMoveTarget"); if (tgt) tgt.hidden = true;
    const pick = $("#bulkMovePick"); if (pick) pick.hidden = true;
    const q = $("#bulkMoveQ"); if (q) q.value = "";
    const run = $("#bulkMoveRun"); if (run) run.classList.add("is-off");
  }
  if (mode === "export") {
    resetExportPanelForNewExport();
    scrollExportPanelBottom();
  }
}

function bulkAct(label) {
  const n = state.selected.size;
  const s = n > 1 ? "s" : "";
  toast(label + " · " + n + " concept" + s);
  clearSelection();
}

function filterMovePick(qraw) {
  const q = norm(qraw.trim());
  const pick = $("#bulkMovePick");
  const empty = $("#bulkMoveEmpty");
  if (state.moveTarget) {
    if (pick) pick.hidden = true;
    return;
  }
  if (pick) pick.hidden = !q;
  if (!q) { if (empty) empty.hidden = true; return; }
  let n = 0;
  $$("#bulkMovePick .bulksel-po").forEach(b => {
    const id = b.getAttribute("data-id");
    const hit = !state.selected.has(id) && (b.getAttribute("data-q") || "").includes(q);
    if (hit && n < 6) {
      b.classList.add("is-shown");
      n += 1;
    } else {
      b.classList.remove("is-shown");
    }
  });
  if (empty) empty.hidden = n > 0;
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
