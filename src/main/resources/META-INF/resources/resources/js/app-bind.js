/**
 * OpenTheso — branchements, ajax et maintenance.
 */
"use strict";

$("#viewPickBtn") && $("#viewPickBtn").addEventListener("click", () => {
  $("#viewPickBtn").classList.toggle("is-open");
});
$("#voGear") && $("#voGear").addEventListener("click", (e) => {
  e.stopPropagation();
  $("#voGear").classList.toggle("is-on");
});
$("#rlMore") && $("#rlMore").addEventListener("click", () => {
  if (typeof loadMoreSearchResults === "function") loadMoreSearchResults();
});

const input = $("#searchInput"), clear = $("#searchClear"), field = $("#searchField");
if (input && field) {
  input.addEventListener("input", () => {
    if (clear) clear.hidden = !input.value;
    field.classList.add("is-focused");
    filterAc(input.value);
  });
  input.addEventListener("focus", () => {
    field.classList.add("is-focused");
    filterAc(input.value);
  });
  input.addEventListener("keydown", (e) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      const downRows = shownAcRows();
      if (!downRows.length && input.value.trim()) filterAc(input.value);
      setAcIdx(Math.min(state.acIdx + 1, Math.max(shownAcRows().length - 1, -1)));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setAcIdx(Math.max(state.acIdx - 1, -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const rows = shownAcRows();
      if (state.acIdx >= 0 && rows[state.acIdx] && typeof openSearchRow === "function") {
        openSearchRow(rows[state.acIdx]);
      } else runSearch();
    } else if (e.key === "Escape") {
      closeSearchUi();
      input.blur();
    }
  });
}
if (clear && input) {
  clear.addEventListener("click", () => {
    input.value = "";
    clear.hidden = true;
    filterAc("");
    input.focus();
  });
}
$("#searchGo") && $("#searchGo").addEventListener("click", runSearch);
const searchBox = $("#searchBox");
if (searchBox && searchBox.tagName === "FORM") {
  searchBox.addEventListener("submit", (e) => {
    e.preventDefault();
    runSearch();
  });
}
["cfFrom", "cfTo"].forEach(id => {
  const el = $("#" + id);
  if (!el) return;
  el.addEventListener("input", () => {
    state[id === "cfFrom" ? "candFrom" : "candTo"] = el.value || "";
    syncCandFilterUi();
    applyStatusFilter();
  });
});
const cfByQuery = $("#cfByQuery");
if (cfByQuery) {
  cfByQuery.addEventListener("input", () => {
    clearTimeout(cfByQuery._t);
    cfByQuery._t = setTimeout(() => loadCandByUsers(cfByQuery.value), 180);
  });
  cfByQuery.addEventListener("keydown", (e) => {
    e.stopPropagation();
    if (e.key === "Escape") {
      const combo = $("#cfCombo");
      if (combo) combo.classList.remove("open");
    }
  });
}

const addCollQ = $("#bulkAddCollQ");
if (addCollQ) {
  addCollQ.addEventListener("input", () => {
    if (state.addCollTarget) return;
    paintBulkAddCollPick();
  });
}
const moveQ = $("#bulkMoveQ");
if (moveQ) {
  moveQ.addEventListener("input", () => {
    if (state.moveTarget) return;
    filterMovePick(moveQ.value);
  });
  moveQ.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && state.moveTarget) {
      const run = $("#bulkMoveRun");
      if (run) run.click();
    }
  });
}
  const collName = $("#bulkCollName");
  if (collName) {
    collName.addEventListener("input", () => {
      if (typeof syncCollRun === "function") syncCollRun();
      const err = $("#bulkCollErr");
      if (err && !err.hidden) err.hidden = true;
    });
    collName.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && collName.value.trim()) {
        const run = $("#bulkCollRun") || $('[data-act="bulk-coll-run"]');
        if (run) run.click();
      }
    });
  }
const xferQ = $("#bulkXferQ");
if (xferQ) {
  xferQ.addEventListener("input", () => {
    if (state.xferTarget) return;
    fillXferPick(xferQ.value);
  });
}
const xferPQ = $("#bulkXferPQ");
if (xferPQ) {
  xferPQ.addEventListener("input", () => {
    if (!state.xferTarget || state.xferParent) return;
    clearTimeout(xferPQ._t);
    xferPQ._t = setTimeout(() => fillXferPick(xferPQ.value), 180);
  });
}

document.addEventListener("mousedown", (e) => {
  if (e.target.closest(".ac-row, .ac-footer")) e.preventDefault();
  if ($("#searchBox") && !$("#searchBox").contains(e.target)) closeSearchUi();
});

bindNarrowShell();
const resizer = $("#resizer");
if (resizer) {
  const saved = parseInt(localStorage.getItem("ot-sidebar-w"), 10);
  if (saved && saved >= 240) document.documentElement.style.setProperty("--tree-w", saved + "px");
  resizer.addEventListener("mousedown", (e) => {
    if (isNarrowShell()) return;
    e.preventDefault();
    const startX = e.clientX;
    const startW = parseInt(getComputedStyle(document.documentElement).getPropertyValue("--tree-w"), 10) || 328;
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";
    const onMove = (ev) => {
      const w = Math.min(window.innerWidth * 0.75, Math.max(240, startW + (ev.clientX - startX)));
      document.documentElement.style.setProperty("--tree-w", w + "px");
    };
    const onUp = () => {
      document.removeEventListener("mousemove", onMove);
      document.removeEventListener("mouseup", onUp);
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
      const w = parseInt(getComputedStyle(document.documentElement).getPropertyValue("--tree-w"), 10);
      if (w) localStorage.setItem("ot-sidebar-w", String(w));
    };
    document.addEventListener("mousemove", onMove);
    document.addEventListener("mouseup", onUp);
  });
  resizer.addEventListener("dblclick", () => {
    document.documentElement.style.setProperty("--tree-w", "328px");
    localStorage.setItem("ot-sidebar-w", "328");
  });
}

applyStatusFilter();
applySort();
applyTableSort();
applyTableCols();
bulkMode("acts");

if (window.jsf && jsf.ajax && typeof jsf.ajax.addOnEvent === "function") {
  jsf.ajax.addOnEvent(function (data) {
    if (data.status !== "success") return;
    const src = data.source;
    const srcId = src && (src.id || (src.getAttribute && src.getAttribute("id")));
    if (srcId === "termLang" || srcId === "previewTermLangGo") onThesaurusLangChanged();
  });
}

const pageSizeSel = $("#panelTablePageSize");
try {
  const stored = Number(localStorage.getItem("ot-table-page-size"));
  if (TABLE_PAGE_SIZES.includes(stored)) state.tblPageSize = stored;
} catch (ex) {}
if (pageSizeSel) {
  pageSizeSel.value = String(tablePageSize());
  pageSizeSel.addEventListener("change", () => setTablePageSize(pageSizeSel.value));
}

if (SCREEN === "graphe") state.view = "hyper";
function onV2Ajax(data) {
  const treeToggle = isTreeCaretSource(data.source);
  const srcIdBegin = (data.source && data.source.id) || "";
  if (data.status === "begin") {
    if (!treeToggle) syncAboutEditor();
    if (treeToggle) lockTreeToggleScroll(data.source);
    if (srcIdBegin.indexOf("candSearchGo") >= 0 || srcIdBegin.indexOf("candMine") >= 0) {
      const panes = $("#candPanes");
      if (panes) {
        panes.classList.add("is-busy");
        panes.setAttribute("aria-busy", "true");
      }
    }
    return;
  }
  if (data.status === "complete") {
    if (srcIdBegin.indexOf("candSearchGo") >= 0 || srcIdBegin.indexOf("candMine") >= 0) {
      const panes = $("#candPanes");
      if (panes) {
        panes.classList.remove("is-busy");
        panes.removeAttribute("aria-busy");
      }
    }
    if (treeToggle) {
      restoreTreeToggleScroll();
      return;
    }
  }
  if (data.status === "error" && srcIdBegin.indexOf("openBtn") >= 0) {
    const live = $("#viewLive");
    if (live) {
      live.classList.remove("is-loading");
      live.removeAttribute("aria-busy");
    }
    const bar = $("#liveProgress");
    if (bar) bar.setAttribute("aria-hidden", "true");
  }
  if (data.status === "success") {
    const srcId = (data.source && data.source.id) || "";
      if (srcId === "previewCorpusToggleGo" || srcId === "previewAlignToggleGo") {
        markSettingsDraft();
      }
      if (srcId.indexOf("propTab") >= 0) {
        applyPropBoardFilter();
      }
      requestAnimationFrame(() => {
        if (typeof restoreSettingsCards === "function") restoreSettingsCards();
        const srcId = (data.source && data.source.id) || "";
        if (srcId.indexOf("clearRevealBtn") >= 0
          || srcId.indexOf("candSearchGo") >= 0
          || srcId.indexOf("candMine") >= 0) {
        return;
      }
      if (srcId.indexOf("cvCopyThesoTarget") >= 0 || srcId.indexOf("cvMoveThesoTarget") >= 0
          || srcId.indexOf("cvArkGenGo") >= 0
          || srcId.indexOf("cvArkMissingGo") >= 0
          || srcId.indexOf("cvArkBranchGo") >= 0
          || srcId.indexOf("cvArkAllGo") >= 0
          || srcId.indexOf("cvCsvGo") >= 0
          || srcId.indexOf("cvCsvAnalyze") >= 0
          || srcId.indexOf("cvCsvFile") >= 0
          || srcId.indexOf("cvLoopGo") >= 0
          || srcId.indexOf("cvDeleteGo") >= 0
          || (srcId.indexOf("cvTypesAfterClose") < 0
            && data.source && data.source.closest && data.source.closest("#cvDlgManageTypes"))
          || (srcId.indexOf("cvEditTypeAfterClose") < 0
            && data.source && data.source.closest && data.source.closest("#cvDlgEditType"))
          || (srcId.indexOf("cvAddNtAfterClose") < 0
            && data.source && data.source.closest && data.source.closest("#cvDlgAddNt"))) {
        return;
      }
      if (srcId.indexOf("revealBtn") >= 0) {
        const tn = findTreeNodeById(state.conceptId);
        if (tn) {
          const st = tn.getAttribute("data-status");
          if (st && !state.statusSet.has(st)) {
            state.statusSet.add(st);
            syncStatusUi();
          }
        }
        applyStatusFilter();
        restoreSelection();
        highlightConcept(state.conceptId);
        requestAnimationFrame(() => highlightConcept(state.conceptId));
        return;
      }
      if (treeToggle) {
        applyStatusFilter();
        restoreSelection();
        restoreTreeToggleScroll();
        return;
      }
      syncAboutFold();
      restoreThDcPanel();
      markAboutVisualEmpty();
      maybeRememberAboutBaseline();
      refreshAboutFmtState();
      applyStatusFilter();
      applySort();
      restoreSelection();
      const metadataForm = data.source && data.source.closest && data.source.closest("#previewMetadataForm");
      if (srcId.indexOf("previewMetadataForm") >= 0 || metadataForm) {
        if (typeof restoreThDcPanel === "function") restoreThDcPanel();
        showMetadataToast();
        return;
      }
      showLiveDetail();
      if (window.syncViewRail) window.syncViewRail();
    });
  }
}

if (window.faces && faces.ajax) {
  faces.ajax.addOnEvent(onV2Ajax);
} else if (window.jsf && jsf.ajax) {
  jsf.ajax.addOnEvent(onV2Ajax);
}

const TH_DC_OPEN_KEY = "th-dc-open";

function applyThDcOpen(root, open) {
  if (!root) return;
  root.classList.toggle("is-open", open);
  const btn = root.querySelector("[data-act='th-dc-toggle']");
  if (!btn) return;
  btn.setAttribute("aria-expanded", open ? "true" : "false");
  const label = open
    ? (btn.getAttribute("data-label-close") || "Replier")
    : (btn.getAttribute("data-label-open") || "Déplier");
  btn.setAttribute("title", label);
  btn.setAttribute("aria-label", label);
}

function toggleThDcPanel(t) {
  const root = t.closest(".th-dc");
  if (!root) return;
  const open = !root.classList.contains("is-open");
  applyThDcOpen(root, open);
  try {
    sessionStorage.setItem(TH_DC_OPEN_KEY, open ? "1" : "0");
  } catch (ex) {}
}

let metadataConfirmTarget = null;
let metadataPendingToast = "";
let metadataPendingDetail = "";
let metadataPendingKind = "update";

function metadataNoticeText(id) {
  const el = document.getElementById(id) || document.querySelector("[id$='" + id + "']");
  if (!el) return "";
  const raw = el.tagName === "INPUT" || el.tagName === "TEXTAREA" ? el.value : el.textContent;
  return (raw || "").replace(/\s+/g, " ").trim();
}

function publishMetadataResult(text, error) {
  const live = document.getElementById("previewMetadataLive");
  if (live) {
    live.textContent = text || "";
    live.classList.toggle("is-error", !!error);
    live.hidden = !text;
  }
  if (text && typeof toast === "function") toast(text, error ? { error: true } : { soft: true });
}

function showMetadataToast() {
  const box = document.querySelector("[id$='previewMetadataMsgs']");
  const item = box && box.querySelector(".th-dc-msg");
  const fromServer = metadataNoticeText("previewMetadataNotice")
    || (item ? (item.textContent || "").replace(/\s+/g, " ").trim() : "");
  const error = metadataNoticeText("previewMetadataNoticeError") === "true"
    || !!(item && item.classList.contains("is-error"));
  const text = error ? (fromServer || metadataPendingToast) : (metadataPendingToast || fromServer);
  metadataPendingToast = "";
  if (!text) return;
  publishMetadataResult(text, error);
}

function metadataConfirmDetail(row) {
  if (!row) return "";
  const name = row.querySelector("select.th-dc-name");
  const nameText = name && name.selectedIndex >= 0 ? name.options[name.selectedIndex].text.trim() : "";
  const input = row.querySelector("input.th-dc-value");
  const boolSel = row.querySelector("select.th-dc-bool");
  let value = "";
  if (input) value = input.value.trim();
  else if (boolSel && boolSel.selectedIndex >= 0) value = boolSel.options[boolSel.selectedIndex].text.trim();
  if (nameText && value) return nameText + " : " + value;
  return nameText || value;
}

function askMetadataConfirm(kind, source) {
  const row = source.closest("tr");
  const proxy = row && row.querySelector(kind === "delete" ? ".th-dc-delete-go" : ".th-dc-save-go");
  const dlg = document.getElementById("previewMetadataConfirm");
  if (!row || !proxy || !dlg || typeof showConfirm !== "function") return;
  metadataConfirmTarget = proxy;
  metadataPendingKind = kind === "delete" ? "delete" : (source.getAttribute("data-new") === "1" ? "save" : "update");
  metadataPendingDetail = metadataConfirmDetail(row) || "—";
  const mode = metadataPendingKind;
  const title = document.getElementById("previewMetadataConfirmTitle");
  const text = document.getElementById("previewMetadataConfirmText");
  const go = document.getElementById("previewMetadataConfirmGo");
  const detail = metadataConfirmDetail(row) || "—";
  if (title) title.textContent = dlg.getAttribute("data-" + mode + "-title") || "";
  if (text) text.textContent = (dlg.getAttribute("data-" + mode) || "").replace("{0}", detail);
  if (go) {
    go.textContent = dlg.getAttribute(kind === "delete" ? "data-go-delete" : "data-go-save") || go.textContent;
    go.classList.toggle("is-danger", kind === "delete");
  }
  showConfirm("#previewMetadataConfirm");
}

function confirmMetadataAction() {
  const target = metadataConfirmTarget;
  metadataConfirmTarget = null;
  const dlg = document.getElementById("previewMetadataConfirm");
  const pattern = dlg ? (dlg.getAttribute("data-done-" + metadataPendingKind) || "") : "";
  metadataPendingToast = pattern.replace("{0}", metadataPendingDetail || "—");
  if (typeof hideConfirm === "function") hideConfirm("#previewMetadataConfirm");
  if (!target) return;
  if (target.classList.contains("th-dc-delete-go")) {
    const tr = target.closest("tr");
    if (tr) tr.hidden = true;
  }
  target.click();
}

window.onMetadataSaved = function (data) {
  if (!data || data.status === "begin" || data.status === "complete") return;
  if (data.status === "error") {
    metadataPendingToast = "";
    document.querySelectorAll("#previewMetadataForm tr[hidden]").forEach((tr) => { tr.hidden = false; });
    publishMetadataResult("L'opération n'a pas pu être effectuée.", true);
    return;
  }
  if (data.status !== "success") return;
  showMetadataToast();
};

function restoreThDcPanel() {
  const root = document.querySelector("#previewMetadataForm .th-dc");
  if (!root) return;
  let open = root.classList.contains("is-editing");
  if (!open) {
    try {
      open = sessionStorage.getItem(TH_DC_OPEN_KEY) === "1";
    } catch (ex) {}
  }
  applyThDcOpen(root, open);
}

restoreThDcPanel();

function scrollToPrefHash() {
  const id = (location.hash || "").replace(/^#/, "");
  if (!id || SCREEN !== "preference") return false;
  const el = document.getElementById(id);
  const view = $("#previewView") || document.querySelector("main.content .view");
  if (!el || !view) return false;
  const top = el.getBoundingClientRect().top - view.getBoundingClientRect().top + view.scrollTop - 12;
  view.scrollTo({ top: Math.max(0, top) });
  if (window.syncViewRail) window.syncViewRail();
  return true;
}

const maintProgress = new WeakMap();

function maintEls(tool) {
  if (!tool) return {};
  const prog = tool.querySelector(".mc-prog");
  return {
    tool,
    prog,
    fill: tool.querySelector(".mg-bar-fill"),
    rail: tool.querySelector(".xprog-rail-fill"),
    pct: tool.querySelector(".xprog-count"),
    title: tool.querySelector(".xprog-t"),
    detail: tool.querySelector(".xprog-d"),
    steps: tool.querySelectorAll(".xprog-steps li")
  };
}

function maintState(tool) {
  let state = maintProgress.get(tool);
  if (!state) {
    state = { timer: 0, started: 0, phase: -1 };
    maintProgress.set(tool, state);
  }
  return state;
}

function maintStepLabel(li) {
  if (!li) return "";
  const label = li.querySelector("span:not(.xprog-dot)");
  return ((label && label.textContent) || li.textContent || "").trim();
}

function paintMaintProgress(tool, phase, pct, { done = false, detail } = {}) {
  const els = maintEls(tool);
  const state = maintState(tool);
  const n = Math.max(1, els.steps.length);
  const idx = Math.max(0, Math.min(n - 1, phase));
  const step = els.steps[idx];
  const value = Math.max(0, Math.min(100, Math.round(pct)));
  if (els.prog) els.prog.style.setProperty("--mc-n", String(n));
  if (els.fill) els.fill.style.width = value + "%";
  if (els.pct) els.pct.textContent = value + "%";
  if (els.rail) {
    const ratio = done ? 1 : (n <= 1 ? 0 : idx / (n - 1));
    els.rail.style.width = Math.round(ratio * 100) + "%";
  }
  if (els.title) {
    els.title.textContent = done
      ? ((els.prog && els.prog.getAttribute("data-done-title")) || "Correction terminée")
      : ("Étape " + (idx + 1) + " / " + n + " — " + maintStepLabel(step));
  }
  if (els.detail) {
    const doneDetail = (els.prog && els.prog.getAttribute("data-done-detail")) || "Traitement terminé.";
    els.detail.textContent = detail
      || (done ? doneDetail : ((step && step.getAttribute("data-detail")) || ""));
  }
  els.steps.forEach((li, i) => {
    li.classList.toggle("is-done", done || i < idx);
    li.classList.toggle("is-on", !done && i === idx);
    li.classList.toggle("is-enter", !done && i === idx && i !== state.phase);
  });
  state.phase = idx;
}

function stopMaintProgress(tool) {
  const state = maintState(tool);
  if (state.timer) {
    clearInterval(state.timer);
    state.timer = 0;
  }
}

function startMaintProgress(tool) {
  const els = maintEls(tool);
  const state = maintState(tool);
  const n = Math.max(1, els.steps.length);
  const holdPhase = Math.max(0, n - 2);
  stopMaintProgress(tool);
  state.started = Date.now();
  state.phase = -1;
  if (els.tool) {
    els.tool.classList.add("is-busy");
    els.tool.setAttribute("aria-busy", "true");
  }
  if (els.prog) {
    els.prog.hidden = false;
    els.prog.classList.add("is-on");
    els.prog.setAttribute("aria-hidden", "false");
  }
  paintMaintProgress(tool, 0, 8);
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    paintMaintProgress(tool, holdPhase, 55);
    return;
  }
  state.timer = setInterval(() => {
    const elapsed = Date.now() - state.started;
    if (n <= 2) {
      paintMaintProgress(tool, 0, Math.min(86, 8 + elapsed / 30));
      return;
    }
    if (elapsed < 450) {
      paintMaintProgress(tool, 0, 8 + (elapsed / 450) * 22);
    } else if (elapsed < 1600) {
      const mid = Math.min(holdPhase, 1);
      paintMaintProgress(tool, mid, 32 + ((elapsed - 450) / 1150) * 40);
    } else {
      const rest = Math.min(14, (elapsed - 1600) / 400);
      paintMaintProgress(tool, holdPhase, 72 + rest);
    }
  }, 70);
}

function finishMaintProgress(tool, ok) {
  const els = maintEls(tool);
  const state = maintState(tool);
  const n = Math.max(1, els.steps.length);
  stopMaintProgress(tool);
  if (ok) {
    paintMaintProgress(tool, n - 1, 100, { done: true });
  } else {
    paintMaintProgress(tool, state.phase < 0 ? 0 : state.phase, parseInt(els.pct && els.pct.textContent, 10) || 0, {
      detail: "Le traitement a été interrompu."
    });
  }
  window.setTimeout(() => {
    if (els.prog) {
      els.prog.hidden = true;
      els.prog.classList.remove("is-on");
      els.prog.setAttribute("aria-hidden", "true");
    }
    if (els.tool) {
      els.tool.classList.remove("is-busy");
      els.tool.removeAttribute("aria-busy");
    }
    if (els.fill) els.fill.style.width = "0%";
    if (els.rail) els.rail.style.width = "0%";
  }, ok ? 720 : 900);
}

function maintToolFromAjax(data) {
  const src = data && data.source;
  if (src && src.closest) {
    return src.closest(".mc-tool");
  }
  return document.querySelector(".mc-tool.is-busy");
}

window.onMaintAjax = function (data) {
  const tool = maintToolFromAjax(data);
  if (!tool) return;
  if (data.status === "begin") {
    startMaintProgress(tool);
  }
  if (data.status === "success") {
    const flash = tool.querySelector("[data-maint-ok]");
    const ok = !flash || flash.getAttribute("data-maint-ok") !== "false";
    finishMaintProgress(tool, ok);
    const run = tool.querySelector(".bo-btn-hit") || tool.querySelector(".bo-btn-hit-in");
    if (run) requestAnimationFrame(() => run.focus());
  }
  if (data.status === "error") {
    const prog = tool.querySelector(".mc-prog");
    finishMaintProgress(tool, false);
    toast((prog && prog.getAttribute("data-fail-msg")) || "Le traitement a échoué.", { error: true });
  }
};
window.onMaintTopTermAjax = window.onMaintAjax;

window.downloadMaintSitemap = function () {
  const ctx = document.body.getAttribute("data-ctx") || "";
  window.location.href = ctx + "/api/maintenance/sitemap.xml";
};

let pendingMaintBtn = null;
function maintConfirmMessage(btn) {
  const opt = btn.closest("[data-confirm-overwrite]");
  if (opt) {
    const sw = opt.querySelector(".st-sw-input, input[type='checkbox']");
    if (sw && sw.checked) return opt.getAttribute("data-confirm-overwrite") || "";
    return "";
  }
  const tool = btn.closest(".mc-tool[data-confirm]");
  return tool ? (tool.getAttribute("data-confirm") || "") : "";
}
document.addEventListener("click", function (e) {
  const btn = e.target.closest && e.target.closest(".bo-btn-hit-in");
  if (!btn || btn.disabled) return;
  if (btn.dataset.maintSkipConfirm === "1") {
    delete btn.dataset.maintSkipConfirm;
    return;
  }
  const msg = maintConfirmMessage(btn);
  if (!msg) return;
  e.preventDefault();
  e.stopImmediatePropagation();
  pendingMaintBtn = btn;
  const text = document.getElementById("maintConfirmText");
  if (text) text.textContent = msg;
  showConfirm("#maintConfirm");
}, true);
