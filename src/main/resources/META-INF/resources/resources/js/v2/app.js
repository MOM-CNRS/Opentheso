/**
 * OpenTheso V2 — câblage UI et démarrage.
 */
"use strict";

function closeThesaurus() {
  $$(".thesaurus-btn.is-on").forEach(b => {
    b.classList.remove("is-on");
    b.setAttribute("aria-expanded", "false");
  });
  const account = $("#menu-account");
  if (account) account.classList.remove("is-forgot");
}

document.addEventListener("pointerdown", (e) => {
  const go = e.target.closest(".login-go");
  if (!go) return;
  go.classList.remove("is-click");
  void go.offsetWidth;
  go.classList.add("is-click");
});
document.addEventListener("click", (e) => {
  const go = e.target.closest(".login-go");
  if (go) go.classList.add("is-busy");
}, true);
document.addEventListener("keydown", (e) => {
  if (e.key !== "Enter" || e.repeat || e.isComposing) return;
  const form = e.target.closest("#previewLoginForm, #previewForgotForm");
  if (!form) return;
  const go = form.querySelector(".login-go");
  if (!go || go.classList.contains("is-busy")) return;
  e.preventDefault();
  go.click();
});
document.addEventListener("keydown", (e) => {
  if (e.repeat || e.isComposing) return;
  if (e.key !== "Enter" && e.key !== " ") return;
  const card = e.target.closest(".boc-clickable[data-act='show']");
  if (!card || e.target !== card) return;
  e.preventDefault();
  card.click();
});

window.onPreviewLoginAjax = function (data) {
  if (data.status !== "success") return;
  const form = document.getElementById("previewLoginForm");
  const btn = document.querySelector('[data-thesaurus="account"]');
  if (!form || !btn) return;
  btn.classList.add("is-on");
  btn.setAttribute("aria-expanded", "true");
};
window.onPreviewForgotAjax = function (data) {
  if (data.status !== "success") return;
  const pop = document.getElementById("menu-account");
  const btn = document.querySelector('[data-thesaurus="account"]');
  if (pop) pop.classList.add("is-forgot");
  if (btn) {
    btn.classList.add("is-on");
    btn.setAttribute("aria-expanded", "true");
  }
};

let syncPollTimer = null;
function syncThesaurusPollTick() {
  const state = document.getElementById("previewSyncState");
  const running = !!(state && state.getAttribute("data-running") === "true");
  if (!running) {
    if (syncPollTimer) {
      window.clearInterval(syncPollTimer);
      syncPollTimer = null;
    }
    return;
  }
  if (syncPollTimer) return;
  syncPollTimer = window.setInterval(() => clickPreviewJsf("previewSyncPollGo"), 1000);
}
window.onPreviewSyncPoll = function (data) {
  if (data.status === "success") syncThesaurusPollTick();
};
document.addEventListener("keydown", (e) => {
  if (e.key !== "Escape") return;
  if (hideConfirm("#aboutSaveConfirm") || hideConfirm("#logoutConfirm") || hideConfirm("#stSaveConfirm")
      || hideConfirm("#previewCorpusCreateConfirm") || hideConfirm("#stLeaveConfirm")) {
    settingsLeaveAction = null;
    e.preventDefault();
  }
});

document.addEventListener("mousedown", (e) => {
  if (e.target.closest(".abt-fmt-btn, [data-act='about-src']")) e.preventDefault();
  const save = e.target.closest(".abt-save:not(.is-off):not(.is-busy)");
  if (save) {
    save.classList.remove("is-click");
    void save.offsetWidth;
    save.classList.add("is-click");
  }
  const corpusBtn = e.target.closest(".st-corpus-btn");
  if (corpusBtn) {
    corpusBtn.classList.remove("is-click");
    void corpusBtn.offsetWidth;
    corpusBtn.classList.add("is-click");
  }
});
document.addEventListener("click", (e) => {
  const save = e.target.closest(".abt-save");
  if (save && (save.classList.contains("is-off") || save.classList.contains("is-busy"))) {
    e.preventDefault();
    e.stopPropagation();
  }
  interceptAboutSwap(e);
}, true);

document.addEventListener("input", (e) => {
  if (e.target && (e.target.id === "aboutVisual" || e.target.classList.contains("abt-editor"))) {
    syncAboutEditor();
    markAboutVisualEmpty();
    refreshAboutSaveState();
  }
});
document.addEventListener("selectionchange", refreshAboutFmtState);

document.addEventListener("click", (e) => {
  const t = e.target.closest("[data-act]");
  if (!t) {
    if ($("#navThesaurus") && !$("#navThesaurus").contains(e.target)) closeThesaurus();
    if ($("#voWrap") && !$("#voWrap").contains(e.target)) $("#voGear") && $("#voGear").classList.remove("is-on");
    if ($("#viewPick") && !$("#viewPick").contains(e.target)) $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
    if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
    return;
  }
  if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
  const act = t.getAttribute("data-act");
  if (act === "logout-ask") {
    closeThesaurus();
    if (askLeaveThen(() => showConfirm("#logoutConfirm"))) return;
    showConfirm("#logoutConfirm");
  } else if (act === "logout-dismiss") {
    hideConfirm("#logoutConfirm");
  } else if (act === "logout-modal") {
    return;
  } else if (act === "about-save-ask") {
    const btn = $("#aboutSaveBtn");
    if (!btn || btn.classList.contains("is-off") || btn.classList.contains("is-busy")) return;
    syncAboutEditor();
    showConfirm("#aboutSaveConfirm");
  } else if (act === "about-save-dismiss") {
    hideConfirm("#aboutSaveConfirm");
  } else if (act === "about-save-modal") {
    return;
  } else if (act === "st-save-ask") {
    showConfirm("#stSaveConfirm");
  } else if (act === "st-save-go") {
    e.preventDefault();
    clickPreviewJsf("previewPrefSaveGo");
  } else if (act === "st-save-dismiss") {
    hideConfirm("#stSaveConfirm");
  } else if (act === "st-save-modal") {
    return;
  } else if (act === "st-leave-dismiss") {
    settingsLeaveAction = null;
  } else if (act === "st-leave-confirm") {
    confirmSettingsLeave();
  } else if (act === "st-leave-modal") {
    return;
  } else if (act === "corpus-create-ask") {
    const name = ($("#previewCorpusName") && $("#previewCorpusName").value || "").trim();
    const label = $("#previewCorpusCreateName");
    if (label) label.textContent = name ? "« " + name + " »" : "ce corpus";
    showConfirm("#previewCorpusCreateConfirm");
  } else if (act === "corpus-create-dismiss") {
    hideConfirm("#previewCorpusCreateConfirm");
  } else if (act === "corpus-create-modal") {
    return;
  } else if (act === "preview-corpus-new") {
    clickPreviewJsf("previewCorpusNewGo");
  } else if (act === "preview-corpus-toggle") {
    clickPreviewJsf("previewCorpusToggleGo", {
      previewCorpusTarget: t.getAttribute("data-name") || ""
    });
  } else if (act === "preview-corpus-edit") {
    clickPreviewJsf("previewCorpusEditGo", {
      previewCorpusTarget: t.getAttribute("data-name") || ""
    });
  } else if (act === "preview-corpus-del") {
    clickPreviewJsf("previewCorpusDelGo", {
      previewCorpusTarget: t.getAttribute("data-name") || ""
    });
  } else if (act === "preview-corpus-prev") {
    if (!t.classList.contains("is-off")) clickPreviewJsf("previewCorpusPrevGo");
  } else if (act === "preview-corpus-next") {
    if (!t.classList.contains("is-off")) clickPreviewJsf("previewCorpusNextGo");
  } else if (act === "preview-corpus-page") {
    clickPreviewJsf("previewCorpusPageGo", {
      previewCorpusPage: t.getAttribute("data-page") || "1"
    });
  } else if (act === "preview-align-toggle") {
    clickPreviewJsf("previewAlignToggleGo", {
      previewAlignSourceId: t.getAttribute("data-id") || ""
    });
  } else if (act === "preview-align-new") {
    clickPreviewJsf("previewAlignNewGo");
  } else if (act === "preview-align-edit") {
    clickPreviewJsf("previewAlignEditGo", {
      previewAlignSourceId: t.getAttribute("data-id") || ""
    });
  } else if (act === "preview-align-del") {
    clickPreviewJsf("previewAlignDelGo", {
      previewAlignSourceId: t.getAttribute("data-id") || ""
    });
  } else if (act === "preview-align-prev") {
    if (!t.classList.contains("is-off")) clickPreviewJsf("previewAlignPrevGo");
  } else if (act === "preview-align-next") {
    if (!t.classList.contains("is-off")) clickPreviewJsf("previewAlignNextGo");
  } else if (act === "preview-align-page") {
    clickPreviewJsf("previewAlignPageGo", {
      previewAlignPage: t.getAttribute("data-page") || "1"
    });
  } else if (act === "go-top") {
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const behavior = reduce ? "auto" : "smooth";
    ["#previewView", "#viewHome", "#viewSettings", "main.content .view"].forEach((sel) => {
      const el = $(sel);
      if (el) el.scrollTo({ top: 0, behavior });
    });
  } else if (act === "thesaurus") {
    const on = t.classList.contains("is-on");
    closeThesaurus();
    if (!on) {
      t.classList.add("is-on");
      t.setAttribute("aria-expanded", "true");
    }
  } else if (act === "login-forgot") {
    e.preventDefault();
    const pop = $("#menu-account");
    const btn = document.querySelector('[data-thesaurus="account"]');
    if (pop) pop.classList.add("is-forgot");
    if (btn) {
      btn.classList.add("is-on");
      btn.setAttribute("aria-expanded", "true");
    }
    const mail = document.getElementById("previewForgotMail");
    if (mail) window.setTimeout(() => mail.focus(), 0);
  } else if (act === "login-forgot-back") {
    e.preventDefault();
    const pop = $("#menu-account");
    if (pop) pop.classList.remove("is-forgot");
    const user = document.getElementById("previewLoginUser");
    if (user) window.setTimeout(() => user.focus(), 0);
  } else if (act === "home") openHome();
  else if (act === "back-graph") backToGraph();
  else if (act === "set-view") setView(t.getAttribute("data-view"));
  else if (act === "show") {
    e.preventDefault();
    showHomePanel(t.getAttribute("data-panel"));
  }
  else if (act === "settings-open") {
    const pages = {
      prefs: "setting/preference.xhtml#stPrefs",
      servers: "setting/preference.xhtml#stServers",
      corpus: "setting/preference.xhtml#stCorpus"
    };
    go(pages[t.getAttribute("data-section")] || "setting/preference.xhtml");
  }
  else if (act === "bo-open") {
    const obj = t.getAttribute("data-obj");
    go("toolbox/atelier.xhtml" + (obj ? "?obj=" + encodeURIComponent(obj) : ""));
  } else if (act === "cand-tab") {
    const board = t.closest(".cand-board") || $("#candBoard");
    if (board) board.setAttribute("data-tab", t.getAttribute("data-tab") || "attente");
  } else if (act === "bo-op") {
    setBatch(t.getAttribute("data-obj"), t.getAttribute("data-op"));
  } else if (act === "bo-acc") {
    const step = t.closest(".bo-acc-step");
    if (step) step.classList.toggle("open");
  } else if (act === "bo-pick") {
    const p = t.closest(".bo-panel");
    if (p) { p.classList.add("has-file"); p.classList.remove("is-checked", "is-done"); }
  } else if (act === "bo-clear") {
    boReset(t.closest(".bo-panel"));
  } else if (act === "bo-check") {
    const p = t.closest(".bo-panel");
    if (p && p.classList.contains("has-file") && !p.classList.contains("is-checked")) p.classList.add("is-checked");
  } else if (act === "bo-reimport") {
    const p = t.closest(".bo-panel");
    if (!p) return;
    p.classList.add("has-file", "is-corrected");
    p.classList.remove("is-checked", "is-done");
    toast("Fichier corrigé réimporté");
  } else if (act === "bo-run") {
    const p = t.closest(".bo-panel");
    if (p) p.classList.add("is-done");
  }
  else if (act === "toggle") {
    const tn = t.closest(".tn");
    if (tn && !t.classList.contains("is-empty")) tn.classList.toggle("is-open");
  } else if (act === "col-toggle") {
    toggleCollectionNode(t.closest(".tn"));
  } else if (act === "col-sort") {
    setCollectionSort(t.getAttribute("data-sort"));
  } else if (act === "open") {
    const id = t.getAttribute("data-id");
    const nodeType = resolveNodeType(t);
    if (nodeType === "group" || nodeType === "subgroup") {
      openCollectionFromTree(id);
      closeSearchUi();
      return;
    }
    if (nodeType === "more") return;
    const stay = !!(t.closest("#panelTable") || t.closest("#panelResults") || t.closest("#resultsList")
      || t.closest("#panelCollection") || t.closest("#collectionDetail") || state.view === "collection");
    if (openLiveDetail(id, nodeType)) {
      state.home = false;
      state.draft = false;
      state.conceptId = id;
      if (stay && (state.view === "collection" || t.closest("#panelCollection") || t.closest("#collectionDetail"))) {
        state.colId = null;
        state.view = "collection";
      } else if (!stay) {
        state.view = "arbo";
      }
      highlightConcept(id);
      closeSearchUi();
      return;
    }
    openConcept(id, stay ? "stay" : "jump");
  } else if (act === "hyper-pick") {
    const id = t.getAttribute("data-id");
    if (!id) return;
    const nodeType = resolveNodeType(t);
    if (openLiveDetail(id, nodeType)) {
      state.home = false;
      state.draft = false;
      state.conceptId = id;
      highlightConcept(id);
      return;
    }
    state.home = false;
    state.draft = false;
    state.conceptId = id;
    highlightConcept(id);
    paint();
  } else if (act === "about") {
    const fold = t.closest(".abt-fold") || $("#aboutFold");
    if (!fold) return;
    const open = fold.classList.toggle("is-open");
    t.classList.toggle("open", open);
    t.setAttribute("aria-expanded", String(open));
    if (!open) {
      const title = document.querySelector("#viewHome .cv-head") || document.querySelector("#viewHome .cv-pref");
      const scroller = $("#previewView") || document.querySelector("main.content .view") || document.querySelector(".view");
      const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      const behavior = reduce ? "auto" : "smooth";
      if (title) title.scrollIntoView({ block: "start", behavior });
      else if (scroller) scroller.scrollTo({ top: 0, behavior });
    }
    if (window.syncViewRail) {
      window.syncViewRail();
      const html = fold.querySelector(".abt-html");
      if (html) html.addEventListener("transitionend", () => window.syncViewRail(), { once: true });
    }
  } else if (act === "about-fmt") {
    e.preventDefault();
    applyAboutFormat(t.getAttribute("data-cmd"), t.getAttribute("data-val"));
  } else if (act === "about-src") {
    e.preventDefault();
    toggleAboutSource();
  } else if (act === "toast") {
    toast(t.getAttribute("data-msg"));
    $("#bulkStatusMenu") && $("#bulkStatusMenu").classList.remove("is-on");
  } else if (act === "bulk-act") {
    bulkAct(t.getAttribute("data-msg") || "Action");
  } else if (act === "copy") {
    try { navigator.clipboard.writeText(t.getAttribute("data-copy")); } catch (_) {}
    t.classList.add("is-copied");
    t.title = "Copié";
    setTimeout(() => { t.classList.remove("is-copied"); t.title = "Copier"; }, 1400);
  } else if (act === "st-save") {
    hideConfirm("#stSaveConfirm");
    const el = $("#stSaveToast");
    if (!el) return;
    el.hidden = false;
    clearTimeout(el._t);
    el._t = setTimeout(() => { el.hidden = true; }, 2200);
  } else if (act === "toggle-sw") {
    t.classList.toggle("on");
    t.setAttribute("aria-pressed", String(t.classList.contains("on")));
    const wrap = t.closest(".st-integ");
    if (wrap) wrap.classList.toggle("on", t.classList.contains("on"));
  }
  else if (act === "toggle-path") {
    state.showPath = !state.showPath;
    t.classList.toggle("on", state.showPath);
    t.setAttribute("aria-checked", String(state.showPath));
    paintSidebar();
  } else if (act === "toggle-hl") {
    state.highlight = !state.highlight;
    t.classList.toggle("on", state.highlight);
    t.setAttribute("aria-checked", String(state.highlight));
    document.body.classList.toggle("hl-off", !state.highlight);
    paintQueryHighlight();
  } else if (act === "density") {
    state.density = t.getAttribute("data-density");
    $$("[data-act='density']").forEach(b => b.classList.toggle("is-on", b === t));
    paintSidebar();
  }     else if (act === "see-all") runSearch();
  else if (act === "create") {
    e.stopPropagation();
    createCandidate(t);
  } else if (act === "cand-resolve") {
    resolveDraft(t.getAttribute("data-kind"));
  }
  else if (act === "sel-node") {
    e.stopPropagation();
    const tn = t.closest(".tn");
    if (!tn) return;
    const ids = collectIds(tn);
    const on = !t.classList.contains("on");
    if (!on) {
      let parent = previousTreeParent(tn);
      while (parent) {
        const parentId = parent.getAttribute("data-id");
        if (parentId) ids.push(parentId);
        parent = previousTreeParent(parent);
      }
    }
    setSelectedIds(ids, on);
    refreshSubtreeCounts();
  } else if (act === "sel-id") {
    e.stopPropagation();
    const id = t.getAttribute("data-id");
    if (id) setSelectedIds([id], !state.selected.has(id));
  } else if (act === "sel-all") {
    e.stopPropagation();
    const rows = $$("#panelTable tr[data-id]:not(.is-status-off)");
    const ids = rows.map(r => r.getAttribute("data-id"));
    const allOn = ids.length > 0 && ids.every(id => state.selected.has(id));
    setSelectedIds(ids, !allOn);
  } else if (act === "sel-all-visible") {
    e.stopPropagation();
    selectAllVisible();
  } else if (act === "clear-sel") {
    clearSelection();
  } else if (act === "st-group") {
    e.preventDefault();
    const key = t.getAttribute("data-group");
    const list = GROUPS[key] || [];
    const allOn = list.every(s => state.statusSet.has(s));
    list.forEach(s => allOn ? state.statusSet.delete(s) : state.statusSet.add(s));
    syncStatusUi();
  } else if (act === "st-item") {
    e.preventDefault();
    const s = t.getAttribute("data-status");
    if (state.statusSet.has(s)) state.statusSet.delete(s);
    else state.statusSet.add(s);
    syncStatusUi();
  } else if (act === "cf-toggle") {
    const combo = t.closest(".cf-combo") || $("#cfCombo");
    if (combo) combo.classList.toggle("open");
  } else if (act === "cf-by") {
    state.candBy = t.getAttribute("data-by") || "";
    $$(".cf-opt").forEach(o => o.classList.toggle("on", o === t));
    const lab = $("#cfByLabel");
    if (lab) lab.textContent = t.getAttribute("data-label") || "Tout le monde";
    const combo = t.closest(".cf-combo");
    if (combo) combo.classList.remove("open");
    syncCandFilterUi();
    applyStatusFilter();
  } else if (act === "cf-clear") {
    state.candBy = "";
    state.candFrom = "";
    state.candTo = "";
    const from = $("#cfFrom"), to = $("#cfTo");
    if (from) from.value = "";
    if (to) to.value = "";
    $$(".cf-opt").forEach(o => o.classList.toggle("on", !o.getAttribute("data-by")));
    const lab = $("#cfByLabel");
    if (lab) lab.textContent = "Tout le monde";
    syncCandFilterUi();
    applyStatusFilter();
  } else if (act === "tbl-sort") {
    const col = t.getAttribute("data-col");
    if (state.tblSort === col) state.tblDir *= -1;
    else { state.tblSort = col; state.tblDir = 1; }
    applyTableSort();
  } else if (act === "tbl-page") {
    e.stopPropagation();
    goToTablePage(t.getAttribute("data-page"));
  } else if (act === "tbl-page-prev") {
    e.stopPropagation();
    goToTablePage(state.tblPage - 1);
  } else if (act === "tbl-page-next") {
    e.stopPropagation();
    goToTablePage(state.tblPage + 1);
  } else if (act === "tbl-col") {
    e.preventDefault();
    const col = t.getAttribute("data-col");
    if (state.tblCols.has(col)) state.tblCols.delete(col);
    else state.tblCols.add(col);
    applyTableCols();
  } else if (act === "ui-lang") {
    $$(".lang-opt").forEach(o => o.classList.toggle("is-on", o === t));
    const flag = t.querySelector(".lang-opt-flag");
    const btn = $('[data-thesaurus="lang"] .thesaurus-flag');
    if (flag && btn) btn.textContent = flag.textContent;
    closeThesaurus();
  } else if (act === "bulk-coll") bulkMode("coll");
  else if (act === "bulk-move") bulkMode("move");
  else if (act === "bulk-export") bulkMode("export");
  else if (act === "bulk-export-back") {
    if (exportBusy) return;
    cancelSelectionExport(true);
  }
  else if (act === "export-kind") {
    if (!exportBusy) {
      setExportKind(t.getAttribute("data-kind"));
      scrollExportPanelBottom();
    }
  } else if (act === "export-fmt") {
    if (!exportBusy) {
      setExportFormat(t.getAttribute("data-fmt"));
      scrollExportPanelBottom();
    }
  } else if (act === "export-delim") {
    if (exportBusy) return;
    $$("[data-act='export-delim']").forEach(btn => {
      btn.classList.toggle("is-on", btn === t);
    });
    saveExportPrefs();
  } else if (act === "export-pdf-type") {
    if (exportBusy) return;
    $$("[data-act='export-pdf-type']").forEach(btn => {
      btn.classList.toggle("is-on", btn === t);
    });
    saveExportPrefs();
  } else if (act === "export-desc") {
    if (exportBusy) return;
    setExportSwitch("bulkExportDesc", !exportSwitchOn("bulkExportDesc"));
    saveExportPrefs();
    refreshExportSummary();
  } else if (act === "export-html") {
    if (exportBusy) return;
    setExportSwitch("bulkExportHtml", !exportSwitchOn("bulkExportHtml"));
    saveExportPrefs();
  } else if (act === "export-zip") {
    if (exportBusy) return;
    setExportSwitch("bulkExportZip", !exportSwitchOn("bulkExportZip"));
    saveExportPrefs();
  } else if (act === "export-img") {
    if (exportBusy) return;
    setExportSwitch("bulkExportImg", !exportSwitchOn("bulkExportImg"));
    saveExportPrefs();
  } else if (act === "export-group") {
    if (exportBusy) return;
    setExportSwitch("bulkExportGroup", !exportSwitchOn("bulkExportGroup"));
    saveExportPrefs();
    applyExportOptionVisibility();
    scrollExportPanelBottom();
  } else if (act === "export-lang" || act === "export-group-id") {
    if (exportBusy) return;
    t.classList.toggle("is-on");
  } else if (act === "export-run") {
    e.preventDefault();
    startSelectionExport();
  } else if (act === "export-dl") downloadReadyExport();
  else if (act === "export-cancel") cancelSelectionExport(!exportBusy);
  else if (act === "bulk-back") bulkMode("acts");
  else if (act === "bulk-status-menu") {
    $("#bulkStatusMenu") && $("#bulkStatusMenu").classList.toggle("is-on");
  } else if (act === "bulk-coll-run") {
    const name = ($("#bulkCollName") && $("#bulkCollName").value.trim()) || "";
    if (name) bulkAct("Collection « " + name + " » créée");
  } else if (act === "bulk-move-pick") {
    state.moveTarget = { id: t.getAttribute("data-id"), pref: t.getAttribute("data-pref") };
    const box = $("#bulkMoveTarget");
    const lab = $("#bulkMoveTargetL");
    if (lab) lab.textContent = state.moveTarget.pref;
    if (box) box.hidden = false;
    $("#bulkMovePick") && ($("#bulkMovePick").hidden = true);
    const run = $("#bulkMoveRun");
    if (run) run.classList.remove("is-off");
  } else if (act === "bulk-move-clear") {
    state.moveTarget = null;
    $("#bulkMoveTarget") && ($("#bulkMoveTarget").hidden = true);
    const q = $("#bulkMoveQ"); if (q) { q.value = ""; q.focus(); }
    $("#bulkMoveRun") && $("#bulkMoveRun").classList.add("is-off");
  } else if (act === "bulk-move-run") {
    if (!state.moveTarget || $("#bulkMoveRun").classList.contains("is-off")) return;
    const n = state.selected.size;
    const s = n > 1 ? "s" : "";
    bulkAct(n + " concept" + s + " déplacé" + s + " sous « " + state.moveTarget.pref + " »");
  }
});

$("#viewPickBtn") && $("#viewPickBtn").addEventListener("click", () => {
  $("#viewPickBtn").classList.toggle("is-open");
});
$("#voGear") && $("#voGear").addEventListener("click", (e) => {
  e.stopPropagation();
  $("#voGear").classList.toggle("is-on");
});
$("#rlMore") && $("#rlMore").addEventListener("click", () => {
  state.resultLimit += PAGE;
  paintCommittedResults();
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
    if (input.value) filterAc(input.value);
  });
  input.addEventListener("keydown", (e) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (input.value.trim()) filterAc(input.value);
      const downRows = shownAcRows();
      setAcIdx(Math.min(state.acIdx + 1, Math.max(downRows.length - 1, -1)));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setAcIdx(Math.max(state.acIdx - 1, -1));
    } else if (e.key === "Enter") {
      const rows = shownAcRows();
      if (state.acIdx >= 0 && rows[state.acIdx]) {
        openConcept(rows[state.acIdx].getAttribute("data-id"), "jump");
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
  collName.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && collName.value.trim()) {
      const run = $('[data-act="bulk-coll-run"]');
      if (run) run.click();
    }
  });
}

document.addEventListener("mousedown", (e) => {
  if (e.target.closest(".ac-row, .ac-footer")) e.preventDefault();
  if ($("#searchBox") && !$("#searchBox").contains(e.target)) closeSearchUi();
});

const resizer = $("#resizer");
if (resizer) {
  const saved = parseInt(localStorage.getItem("ot-sidebar-w"), 10);
  if (saved && saved >= 240) document.documentElement.style.setProperty("--tree-w", saved + "px");
  resizer.addEventListener("mousedown", (e) => {
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

const termLangSel = document.getElementById("termLang");
if (termLangSel) {
  termLangSel.addEventListener("change", onThesaurusLangChanged);
}
if (window.jsf && jsf.ajax && typeof jsf.ajax.addOnEvent === "function") {
  jsf.ajax.addOnEvent(function (data) {
    if (data.status !== "success") return;
    const src = data.source;
    const srcId = src && (src.id || (src.getAttribute && src.getAttribute("id")));
    if (srcId === "termLang") onThesaurusLangChanged();
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
  if (data.status === "begin") {
    if (!treeToggle) syncAboutEditor();
    if (treeToggle) lockTreeToggleScroll(data.source);
    return;
  }
  if (data.status === "complete" && treeToggle) {
    restoreTreeToggleScroll();
    return;
  }
  if (data.status === "success") {
    const srcId = (data.source && data.source.id) || "";
    if (srcId === "previewCorpusToggleGo" || srcId === "previewAlignToggleGo") {
      markSettingsDraft();
    }
    requestAnimationFrame(() => {
      const srcId = (data.source && data.source.id) || "";
      if (srcId.indexOf("revealBtn") >= 0) {
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
      markAboutVisualEmpty();
      maybeRememberAboutBaseline();
      refreshAboutFmtState();
      applyStatusFilter();
      applySort();
      restoreSelection();
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

var params = new URLSearchParams(location.search);

if (SCREEN === "atelier") {
  setBatch(params.get("obj") || "alignements");
}
if (SCREEN === "synchronisation") {
  syncThesaurusPollTick();
}
if (SCREEN === "candidats" && (params.get("new") === "1" || params.get("pref") || params.get("path"))) {
  createCandidate({
    getAttribute: (name) => {
      if (name === "data-pref") return params.get("pref") || "";
      if (name === "data-path") return params.get("path") || "";
      return "";
    }
  });
}
if (IS_CONSULT) {
  const q = params.get("q");
  const id = params.get("id");
  const view = params.get("view");
  if (q) {
    if (input) input.value = q;
    if (clear) clear.hidden = !q;
    runSearch();
  } else if (id) {
    if (!openLiveDetail(id, params.get("type") || "")) {
      openConcept(id);
    }
  } else if (view) {
    setView(view);
  } else {
    paint();
  }
} else {
  paint();
}
bindPrefSwitches();
bindViewRail();
loadStatKpis();
initSettingsLeaveGuard();
requestAnimationFrame(() => {
  syncAboutFold();
  markAboutVisualEmpty();
  maybeRememberAboutBaseline();
  refreshAboutFmtState();
  if (window.syncViewRail) window.syncViewRail();
  scrollToPrefHash();
  if (SCREEN === "preference") rememberSettingsBaseline();
});
if (SCREEN === "preference" && location.hash) {
  window.addEventListener("load", scrollToPrefHash);
  setTimeout(scrollToPrefHash, 80);
  setTimeout(scrollToPrefHash, 250);
}
