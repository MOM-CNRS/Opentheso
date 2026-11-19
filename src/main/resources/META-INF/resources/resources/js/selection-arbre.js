/** Sélection — coches de l'arbre. */
const impliedSelected = new Set();
const treeBranchGen = new Map();
const branchMemberCache = new Map();
const explicitBranches = new Set();
const branchExtras = new Map();
const looseSelected = new Set();

function branchKey(tn) {
  if (!tn) return "";
  const path = tn.getAttribute("data-key") || "";
  const id = tn.getAttribute("data-id") || "";
  const ids = [];
  let node = tn;
  const seen = new Set();
  while (node && !seen.has(node)) {
    seen.add(node);
    ids.push(node.getAttribute("data-id") || "");
    node = typeof previousTreeParent === "function" ? previousTreeParent(node) : null;
  }
  return ids.reverse().join("/") + "\u0001" + path + "\u0001" + id;
}

function isBranchCovered(tn) {
  let node = tn;
  const seen = new Set();
  while (node && !seen.has(node)) {
    seen.add(node);
    const key = branchKey(node);
    if (key && explicitBranches.has(key)) return true;
    node = typeof previousTreeParent === "function" ? previousTreeParent(node) : null;
  }
  return false;
}

function branchNodes(tn) {
  const nodes = [];
  if (!tn) return nodes;
  const depth = treeDepth(tn);
  nodes.push(tn);
  let next = tn.nextElementSibling;
  while (next && next.classList.contains("tn")) {
    if (treeDepth(next) <= depth) break;
    nodes.push(next);
    next = next.nextElementSibling;
  }
  return nodes;
}

function paintTreeChecks() {
  const nodes = typeof treeNodes === "function" ? treeNodes() : [];
  nodes.forEach(tn => {
    const on = isBranchCovered(tn);
    const row = tn.querySelector(".tn-row");
    const check = row ? row.querySelector(".tn-check") : tn.querySelector(".tn-check");
    if (check) check.classList.toggle("on", on);
    if (row) row.classList.toggle("is-sel", on);
  });
}

function rebuildSelectedIds() {
  const next = new Set(looseSelected);
  const nodes = typeof treeNodes === "function" ? treeNodes() : [];
  nodes.forEach(tn => {
    if (!isBranchCovered(tn)) return;
    const id = tn.getAttribute("data-id");
    if (id) next.add(id);
  });
  explicitBranches.forEach(key => {
    (branchExtras.get(key) || []).forEach(id => {
      if (id) next.add(id);
    });
  });
  state.selected.forEach(id => {
    if (!next.has(id)) {
      state.selected.delete(id);
      paintSelectedId(id, false);
    }
  });
  next.forEach(id => {
    if (state.selected.has(id)) return;
    state.selected.add(id);
    paintSelectedId(id, true);
  });
  impliedSelected.clear();
  explicitBranches.forEach(key => {
    (branchExtras.get(key) || []).forEach(id => {
      if (id) impliedSelected.add(id);
    });
  });
}

function collectIds(tn) {
  const ids = [];
  const push = id => { if (id && !ids.includes(id)) ids.push(id); };
  branchNodes(tn).forEach(node => push(node.getAttribute("data-id")));
  return ids;
}

function fetchSubtreeMemberIds(id) {
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams();
  const theso = typeof thesaurusId === "function" ? thesaurusId() : "";
  const lang = typeof thesaurusLang === "function" ? thesaurusLang() : "fr";
  if (theso) params.set("thesaurusId", theso);
  if (lang) params.set("lang", lang);
  params.set("ids", id);
  return fetch(ctx + "/api/subtree-members?" + params.toString(), {
    credentials: "same-origin",
    cache: "no-store",
    headers: { Accept: "application/json" }
  }).then(res => (res.ok ? res.json() : []))
    .then(rows => (Array.isArray(rows) ? rows : [])
      .map(row => row && row.id)
      .filter(memberId => memberId && memberId !== id))
    .catch(() => []);
}

function selectTreeBranch(tn, on) {
  const id = tn && tn.getAttribute("data-id");
  if (!id) return;
  if (on && typeof canTreeSelect === "function" && !canTreeSelect()) return;
  const key = branchKey(tn);
  if (!key) return;
  if (!on) state.selectedAllThesaurus = false;
  const dropGen = keys => {
    keys.forEach(k => {
      if (!k) return;
      explicitBranches.delete(k);
      branchExtras.delete(k);
      treeBranchGen.set(k, (treeBranchGen.get(k) || 0) + 1);
    });
  };
  if (on) {
    explicitBranches.add(key);
    looseSelected.delete(id);
    dropGen(branchNodes(tn).slice(1).map(branchKey));
  } else {
    const drop = branchNodes(tn).map(branchKey);
    let parent = typeof previousTreeParent === "function" ? previousTreeParent(tn) : null;
    while (parent) {
      drop.push(branchKey(parent));
      parent = typeof previousTreeParent === "function" ? previousTreeParent(parent) : null;
    }
    dropGen(drop);
  }
  rebuildSelectedIds();
  paintTreeChecks();
  updateBulk();
  refreshSubtreeCounts();
  const type = (tn.getAttribute("data-type") || "").toLowerCase();
  if (type === "facet" || !on) return;
  const gen = (treeBranchGen.get(key) || 0) + 1;
  treeBranchGen.set(key, gen);
  fetchSubtreeMemberIds(id).then(extra => {
    if (treeBranchGen.get(key) !== gen) return;
    if (!explicitBranches.has(key)) return;
    branchExtras.set(key, extra);
    branchMemberCache.set(id, extra);
    rebuildSelectedIds();
    paintTreeChecks();
    updateBulk();
  });
}

function paintSelectedId(id, on) {
  $$(`tr[data-id="${CSS.escape(id)}"]`).forEach(row => {
    row.classList.toggle("is-sel", on);
    const check = row.querySelector(".tn-check");
    if (check) check.classList.toggle("on", on);
  });
}

function setSelectedIds(ids, on) {
  if (on && typeof canTreeSelect === "function" && !canTreeSelect()) return;
  if (!on) state.selectedAllThesaurus = false;
  const nodes = typeof treeNodes === "function" ? treeNodes() : [];
  ids.forEach(id => {
    if (!id) return;
    const occurrences = nodes.filter(tn => tn.getAttribute("data-id") === id);
    if (on) {
      if (occurrences.length) {
        occurrences.forEach(tn => explicitBranches.add(branchKey(tn)));
        looseSelected.delete(id);
      } else looseSelected.add(id);
    } else {
      occurrences.forEach(tn => {
        const key = branchKey(tn);
        explicitBranches.delete(key);
        branchExtras.delete(key);
        treeBranchGen.set(key, (treeBranchGen.get(key) || 0) + 1);
      });
      looseSelected.delete(id);
      impliedSelected.delete(id);
    }
  });
  rebuildSelectedIds();
  paintTreeChecks();
  updateBulk();
}

function restoreSelection() {
  const nodes = typeof treeNodes === "function" ? treeNodes() : [];
  nodes.forEach(tn => {
    const id = tn.getAttribute("data-id");
    if (!id || !looseSelected.has(id)) return;
    explicitBranches.add(branchKey(tn));
    looseSelected.delete(id);
  });
  rebuildSelectedIds();
  paintTreeChecks();
  updateBulk();
}

function clearSelection(keepUi) {
  const ids = [...state.selected];
  impliedSelected.clear();
  treeBranchGen.clear();
  branchMemberCache.clear();
  explicitBranches.clear();
  branchExtras.clear();
  looseSelected.clear();
  if (!keepUi) {
    ids.forEach(id => paintSelectedId(id, false));
    paintTreeChecks();
  } else {
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
  const nodes = typeof treeNodes === "function" ? treeNodes() : [];
  nodes.forEach(tn => {
    if (!explicitBranches.has(branchKey(tn))) return;
    const id = tn.getAttribute("data-id");
    if (!id || seen.has(id)) return;
    seen.add(id);
    roots.push(id);
  });
  state.selected.forEach(id => {
    if (seen.has(id) || impliedSelected.has(id)) return;
    if (!nodes.some(tn => tn.getAttribute("data-id") === id)) {
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
  return fetch(ctx + "/api/subtree-size?" + params.toString(), {
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
  if (typeof canTreeSelect === "function" && !canTreeSelect()) return false;
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
    loadStatusMembers();
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
  if (typeof canTreeSelect === "function" && !canTreeSelect()) return;
  const ids = visibleSelectableIds();
  if (!ids.length) return;
  setSelectedIds(ids, true);
  if (state.view === "tableau") return;
  state.selectedAllThesaurus = true;
  updateBulk();
}

function bulkMode(mode) {
  closeSelectionList();
  if (exportBusy && mode !== "export") return;
  const acts = $("#bulkActs"), coll = $("#bulkColl"), addColl = $("#bulkAddColl"), move = $("#bulkMove"), exp = $("#bulkExport"), xfer = $("#bulkXfer"), status = $("#bulkStatus");
  if (acts) acts.hidden = mode !== "acts";
  if (coll) coll.hidden = mode !== "coll";
  if (addColl) addColl.hidden = mode !== "addcoll";
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
  if (mode === "addcoll") resetAddColl();
  else hideBulkAddCollConfirm();
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

