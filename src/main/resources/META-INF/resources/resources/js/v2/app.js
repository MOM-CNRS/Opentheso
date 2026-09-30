/**
 * OpenTheso V2 — câblage UI et démarrage.
 */
"use strict";

function isLoggedInUi() {
  return (document.body && document.body.getAttribute("data-logged-in") === "1")
    || !!document.querySelector('[data-thesaurus="account"].is-avatar')
    || !!document.getElementById("menu-account");
}

function closeAccountMenu() {
  const btn = document.querySelector('[data-thesaurus="account"]');
  if (btn) {
    btn.classList.remove("is-on");
    btn.setAttribute("aria-expanded", "false");
  }
  ["menu-login", "menu-account"].forEach((id) => {
    const pop = document.getElementById(id);
    if (!pop) return;
    pop.classList.remove("is-open", "is-forgot");
  });
}

function openLoginMenu() {
  const btn = document.querySelector('[data-thesaurus="account"]');
  if (btn) {
    btn.classList.add("is-on");
    btn.setAttribute("aria-expanded", "true");
  }
  const login = document.getElementById("menu-login");
  const account = document.getElementById("menu-account");
  if (account) account.classList.remove("is-open", "is-forgot");
  if (login) {
    login.classList.remove("is-forgot");
    login.classList.add("is-open");
  }
}

function openAccountMenu() {
  const btn = document.querySelector('[data-thesaurus="account"]');
  if (btn) {
    btn.classList.add("is-on");
    btn.setAttribute("aria-expanded", "true");
  }
  const login = document.getElementById("menu-login");
  const account = document.getElementById("menu-account");
  if (login) login.classList.remove("is-open", "is-forgot");
  if (account) account.classList.add("is-open");
}

function closeThesaurus() {
  $$(".thesaurus-btn.is-on").forEach(b => {
    if (b.id === "sbOpenBtn") return;
    b.classList.remove("is-on");
    b.setAttribute("aria-expanded", "false");
  });
  closeAccountMenu();
}

function closeTreeLang() {
  const btn = $("#treeLangBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function closeDraftTrLangMenu() {
  if (typeof closeDraftTrLang === "function") return closeDraftTrLang();
  return false;
}

function closeDraftNoteLangMenu() {
  if (typeof closeDraftNoteLang === "function") return closeDraftNoteLang();
  return false;
}

function closePrefLang() {
  let closed = false;
  $$(".st-lang-pick .tp-lang-btn.is-open").forEach((btn) => {
    btn.classList.remove("is-open");
    btn.setAttribute("aria-expanded", "false");
    closed = true;
  });
  return closed;
}

function paintPrefSourceLang(opt) {
  const pick = opt && opt.closest(".st-lang-pick");
  if (!pick) return;
  const lang = opt.getAttribute("data-lang") || "";
  const hidden = document.getElementById("previewPrefSourceLang");
  if (hidden) hidden.value = lang;
  const btn = pick.querySelector(".tp-lang-btn");
  const flag = opt.querySelector(".tree-lang-opt-flag");
  const name = opt.querySelector(".tree-lang-opt-name");
  const code = opt.querySelector(".tree-lang-opt-code");
  if (btn) {
    const flagEl = btn.querySelector(".tree-lang-flag");
    const nameEl = btn.querySelector(".tree-lang-name");
    const codeEl = btn.querySelector(".tree-lang-code");
    if (flagEl && flag) flagEl.textContent = flag.textContent;
    if (nameEl && name) nameEl.textContent = name.textContent;
    if (codeEl && code) codeEl.textContent = code.textContent;
    btn.classList.remove("is-open");
    btn.setAttribute("aria-expanded", "false");
  }
  pick.querySelectorAll(".tree-lang-opt").forEach((item) => {
    const on = item === opt;
    item.classList.toggle("is-on", on);
    item.setAttribute("aria-selected", on ? "true" : "false");
  });
  if (typeof markSettingsDraft === "function") markSettingsDraft();
}

function closeCvCtx() {
  const btn = $("#cvCtxBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}


document.addEventListener("pointerdown", (e) => {
  const go = e.target.closest(".login-go");
  if (go) {
    go.classList.remove("is-click");
    void go.offsetWidth;
    go.classList.add("is-click");
  }
}, true);
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
function triggerCandSearch() {
  const go = document.getElementById("candBoardForm:candSearchGo");
  if (go) go.click();
}

function syncCandSearchClear() {
  const input = document.querySelector(".cand-search");
  const clear = $("#candSearchClear");
  if (clear) clear.hidden = !(input && input.value);
}

function syncPropSearchClear() {
  const input = $("#propSearch");
  const clear = $("#propSearchClear");
  if (clear) clear.hidden = !(input && input.value);
}

function applyPropBoardFilter() {
  const board = $("#propBoard");
  if (!board) return;
  const input = $("#propSearch");
  const q = norm(input && input.value);
  const items = $$(".prop-item", board);
  let visible = 0;
  items.forEach((row) => {
    const hay = norm(row.getAttribute("data-search") || row.textContent);
    const on = !q || hay.indexOf(q) >= 0;
    row.hidden = !on;
    if (on) visible += 1;
  });
  const empty = $("#propFilterEmpty");
  if (empty) empty.hidden = !(q && items.length > 0 && visible === 0);
  syncPropSearchClear();
}

function openPropDecisionConfirm(mode) {
  const dlg = document.getElementById("propDecisionConfirm");
  if (!dlg || typeof showConfirm !== "function") return;
  const approve = mode === "approve";
  dlg.setAttribute("data-mode", approve ? "approve" : "refuse");
  const title = document.getElementById("propDecisionConfirmTitle");
  const text = document.getElementById("propDecisionConfirmText");
  if (title) {
    title.textContent = dlg.getAttribute(approve ? "data-title-approve" : "data-title-refuse") || "";
  }
  if (text) {
    text.textContent = dlg.getAttribute(approve ? "data-text-approve" : "data-text-refuse") || "";
  }
  showConfirm("#propDecisionConfirm");
  const comment = document.getElementById("propReviewReply");
  if (comment && typeof comment.focus === "function") {
    window.setTimeout(() => comment.focus(), 0);
  }
}

function propCommentField() {
  return document.querySelector("#viewLive [id$='propComment']");
}

function clearPropCommentError() {
  const comment = propCommentField();
  const err = document.getElementById("propCommentError");
  const row = comment && comment.closest(".crow");
  if (comment) {
    comment.classList.remove("is-invalid");
    comment.setAttribute("aria-invalid", "false");
  }
  if (row) row.classList.remove("is-invalid");
  if (err) err.classList.remove("is-on");
}

function markPropCommentError() {
  const comment = propCommentField();
  const err = document.getElementById("propCommentError");
  const row = comment && comment.closest(".crow");
  if (comment) {
    comment.classList.add("is-invalid");
    comment.setAttribute("aria-invalid", "true");
    comment.focus();
    if (typeof comment.scrollIntoView === "function") {
      comment.scrollIntoView({ block: "center", behavior: "smooth" });
    }
  }
  if (row) row.classList.add("is-invalid");
  if (err) err.classList.add("is-on");
}

function ensurePropCommentFilled() {
  const comment = propCommentField();
  if (comment && comment.value.trim()) {
    clearPropCommentError();
    return true;
  }
  markPropCommentError();
  return false;
}

function onPropBoardAjax(data) {
  const panes = $("#propPanes");
  if (data.status === "begin") {
    if (panes) {
      panes.classList.add("is-busy");
      panes.setAttribute("aria-busy", "true");
    }
    return;
  }
  if (data.status === "success" || data.status === "complete") {
    if (panes) {
      panes.classList.remove("is-busy");
      panes.removeAttribute("aria-busy");
    }
    applyPropBoardFilter();
  }
}
window.onPropBoardAjax = onPropBoardAjax;

document.addEventListener("keydown", (e) => {
  if (e.repeat || e.isComposing) return;
  if (onThesoAcKeydown(e)) return;
  if (e.key !== "Enter") return;
  if (e.target && e.target.id === "propSearch") {
    e.preventDefault();
    return;
  }
  if (!e.target.classList || !e.target.classList.contains("cand-search")) return;
  e.preventDefault();
  clearTimeout(triggerCandSearch._t);
  triggerCandSearch();
});
document.addEventListener("keydown", (e) => {
  if (e.repeat || e.isComposing) return;
  if (e.key === "Enter" && e.target && e.target.id === "draftRelQ") {
    e.preventDefault();
    if (draftRelState.hits && draftRelState.hits[0]) {
      addDraftRel(draftRelState.hits[0].id, draftRelState.hits[0].label);
    }
    return;
  }
  if (e.key === "Enter" && e.target && e.target.id === "cvRelQ") {
    e.preventDefault();
    if (typeof addCvRel === "function" && cvRelState && cvRelState.hits && cvRelState.hits[0]) {
      addCvRel(cvRelState.hits[0].id, cvRelState.hits[0].label);
    }
    return;
  }
  if (e.key === "Enter" && e.target && e.target.id === "fctMemQ") {
    e.preventDefault();
    if (fctMemState.hits && fctMemState.hits[0]) {
      addFacetDraftMem(fctMemState.hits[0].id, fctMemState.hits[0].label);
    }
    return;
  }
  if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
    const draft = $("#viewDraft");
    if (draft && draft.classList.contains("is-on")) {
      e.preventDefault();
      requestDraftCreate("create");
    }
  }
  if (e.key !== "Escape") return;
  if (typeof closeImgLightbox === "function" && closeImgLightbox()) return;
  if (typeof closeDraftImgLightbox === "function" && closeDraftImgLightbox()) return;
  if (typeof closeGpsLightbox === "function" && closeGpsLightbox()) return;
  if (typeof closeCollectionPicker === "function" && closeCollectionPicker()) return;
  if (closeTreeLang()) return;
  if (closeDraftTrLangMenu()) return;
  if (typeof closeConceptTrLang === "function" && closeConceptTrLang()) return;
  if (typeof closeConceptNoteLang === "function" && closeConceptNoteLang()) return;
  if (typeof closeConceptNoteTypeMenu === "function" && closeConceptNoteTypeMenu()) return;
  if (typeof closeFacetDraftTrLang === "function" && closeFacetDraftTrLang()) return;
  if (closeDraftNoteLangMenu()) return;
  if (typeof closeFacetDraftNoteLang === "function" && closeFacetDraftNoteLang()) return;
  if (closePrefLang()) return;
  if (closeCvCtx()) return;
  if (closeAnyThesoAc()) return;
  if (document.body.classList.contains("is-drawer")) {
    closeSidebarDrawer();
    return;
  }
  if (hideCvDialogs()) return;
  if (e.target && e.target.classList && e.target.classList.contains("cand-search")) {
    if (e.target.value) {
      e.target.value = "";
      syncCandSearchClear();
      triggerCandSearch();
      return;
    }
  }
  if (e.target && e.target.id === "propSearch") {
    if (e.target.value) {
      e.target.value = "";
      applyPropBoardFilter();
      return;
    }
  }
  const draft = $("#viewDraft");
  if (draft && draft.classList.contains("is-on")) {
    if ($("#draftCreateConfirm") && !$("#draftCreateConfirm").hidden) {
      hideDraftCreateConfirm();
      return;
    }
    if ($("#draftLeaveConfirm") && !$("#draftLeaveConfirm").hidden) {
      hideDraftLeave();
      return;
    }
    requestDraftLeave();
    return;
  }
  if (SCREEN !== "candidats") return;
  const live = $("#viewLive");
  if (live && live.classList.contains("is-on") && fromCandList()) {
    backToCandList();
  }
});
document.addEventListener("keydown", (e) => {
  if (e.repeat || e.isComposing) return;
  if (e.key !== "Enter" && e.key !== " ") return;
  const card = e.target.closest(".boc-clickable[data-act='show']");
  if (!card || e.target !== card) return;
  e.preventDefault();
  card.click();
});

function goAfterLogin(url) {
  if (document.body) document.body.setAttribute("data-logged-in", "1");
  closeAccountMenu();
  const next = (url || "").trim() || window.location.href;
  const sep = next.includes("?") ? "&" : "?";
  window.location.replace(next + sep + "_r=" + Date.now());
}

function loginDoneTarget() {
  const done = document.getElementById("previewLoginDone");
  if (done && done.getAttribute("data-ok") === "1") {
    return done.getAttribute("data-after-login") || "";
  }
  return null;
}

window.onPreviewLoginAjax = function (data) {
  const go = document.querySelector("#previewLoginForm .login-go");
  if (data.status === "begin") {
    if (go) go.classList.add("is-busy");
    return;
  }
  if (data.status === "complete") {
    if (loginDoneTarget() !== null) closeAccountMenu();
    return;
  }
  if (data.status === "success") {
    const doneUrl = loginDoneTarget();
    if (doneUrl !== null) {
      goAfterLogin(doneUrl);
      return;
    }
    const form = document.getElementById("previewLoginForm");
    if (!form) {
      goAfterLogin("");
      return;
    }
    if (!form.classList.contains("is-error") && !form.querySelector(".login-alert")) {
      goAfterLogin(form.getAttribute("data-after-login"));
      return;
    }
    if (go) go.classList.remove("is-busy");
    openLoginMenu();
    return;
  }
  if (data.status === "error") {
    const doneUrl = loginDoneTarget();
    if (doneUrl !== null) {
      goAfterLogin(doneUrl);
      return;
    }
    if (go) go.classList.remove("is-busy");
  }
};
document.addEventListener("DOMContentLoaded", function () {
  if (document.body && document.body.getAttribute("data-logged-in") === "1") {
    closeAccountMenu();
  }
});

window.onPreviewForgotAjax = function (data) {
  if (data.status !== "success") return;
  openLoginMenu();
  const pop = document.getElementById("menu-login");
  if (pop) pop.classList.add("is-forgot");
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
var portalPollTimer = null;
function portalPublishPollTick() {
  const state = document.getElementById("previewPortalState");
  const running = !!(state && state.getAttribute("data-running") === "true");
  if (!running) {
    if (portalPollTimer) {
      window.clearInterval(portalPollTimer);
      portalPollTimer = null;
    }
    focusPortalResult();
    return;
  }
  if (portalPollTimer) return;
  portalPollTimer = window.setInterval(() => clickPreviewJsf("previewPortalPollGo"), 1000);
}
function focusPortalResult() {
  const dlg = document.getElementById("previewPortalResult");
  if (!dlg || dlg.classList.contains("is-off") || dlg.hidden) return;
  const close = dlg.querySelector(".confirm-cancel");
  if (close) close.focus();
}
function dismissPortalResult() {
  clickPreviewJsf("previewPortalResultCloseGo");
}
window.onPreviewPortalPoll = function (data) {
  if (data.status === "success") portalPublishPollTick();
};
window.tpToggleSyncDiff = function (btn) {
  if (!btn) return false;
  var box = btn.closest(".sync-diff");
  if (!box) return false;
  var open = box.classList.toggle("is-open");
  btn.setAttribute("aria-expanded", open ? "true" : "false");
  var stack = btn.getAttribute("data-label-stack") || "Empiler";
  var unstack = btn.getAttribute("data-label-unstack") || "Dépiler";
  var label = open ? stack : unstack;
  btn.title = label;
  var act = btn.querySelector(".sync-diff-act");
  if (act) act.textContent = label;
  var hiddenId = btn.getAttribute("data-hidden");
  var hidden = hiddenId ? document.getElementById(hiddenId) : null;
  if (hidden) hidden.value = open ? "true" : "false";
  return false;
};
document.addEventListener("keydown", (e) => {
  if (e.key !== "Escape") return;
  const sessionDlg = document.getElementById("sessionExpireConfirm");
  if (sessionDlg && !sessionDlg.hidden) {
    if (typeof window.v2SessionStay === "function") window.v2SessionStay();
    e.preventDefault();
    return;
  }
  const overlay = $("#cblockOverlay");
  if (overlay && !overlay.hidden) {
    closeConceptBlockOverlay();
    e.preventDefault();
    return;
  }
  if (document.getElementById("previewPortalResult")) {
    dismissPortalResult();
    e.preventDefault();
    return;
  }
  if (hideConfirm("#previewPortalPublishConfirm") || hideConfirm("#previewPortalRemoveConfirm")) {
    e.preventDefault();
    return;
  }
  if (hideConfirm("#createChooser")) {
    const add = document.getElementById("viewAdd");
    if (add) add.setAttribute("aria-expanded", "false");
    e.preventDefault();
    return;
  }
  if (hideConfirm("#aboutSaveConfirm") || hideConfirm("#logoutConfirm") || hideConfirm("#stSaveConfirm")
      || hideConfirm("#previewCorpusCreateConfirm") || hideConfirm("#stLeaveConfirm")
      || hideConfirm("#alignDeleteConfirm") || hideConfirm("#alignReplaceConfirm")
      || hideConfirm("#candReactivateConfirm") || hideConfirm("#candDeleteConfirm")) {
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
  if (ignoreThesoClick) {
    ignoreThesoClick = false;
    e.preventDefault();
    e.stopPropagation();
    return;
  }
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
  if (e.target && e.target.id === "draftTitle") syncDraftPrefMirror();
  if (e.target && e.target.id === "draftRelQ") scheduleDraftRelSearch();
  if (e.target && e.target.id === "cvRelQ" && typeof scheduleCvRelSearch === "function") scheduleCvRelSearch();
  if (e.target && e.target.closest && e.target.closest("#cvTrEditor") && e.target.getAttribute("data-lang")) {
    const lang = e.target.getAttribute("data-lang");
    if (e.target.classList.contains("te-alt")) setConceptTrAlt(lang, e.target.value);
    else setConceptTrValue(lang, e.target.value);
  } else if (e.target && e.target.closest && e.target.closest("#fctTrEditor") && e.target.getAttribute("data-lang")) {
    const lang = e.target.getAttribute("data-lang");
    if (e.target.classList.contains("te-alt")) setFacetDraftTrAlt(lang, e.target.value);
    else setFacetDraftTrValue(lang, e.target.value);
  } else if (e.target && e.target.closest && e.target.closest("#cvNoteEditor") && e.target.getAttribute("data-cv-note")) {
    const field = e.target.getAttribute("data-cv-note");
    if (field && field !== "type") {
      setConceptNoteField(Number(e.target.getAttribute("data-index")), field, e.target.value);
    }
  } else if (e.target && e.target.closest && e.target.closest("#draftTrEditor") && e.target.getAttribute("data-lang")) {
    const lang = e.target.getAttribute("data-lang");
    if (e.target.classList.contains("te-alt")) setDraftTrAlt(lang, e.target.value);
    else setDraftTrValue(lang, e.target.value);
  }
  if (e.target && e.target.id === "fctMemQ") scheduleFacetDraftMemSearch();
  if (e.target && e.target.getAttribute && e.target.getAttribute("data-cv-res")) {
    const kind = e.target.getAttribute("data-cv-res");
    const field = e.target.getAttribute("data-field");
    const index = Number(e.target.getAttribute("data-index"));
    if (kind === "link") setConceptResLink(index, field, e.target.value);
    else if (kind === "image") setConceptResImage(index, field, e.target.value);
    else if (kind === "gps") setConceptResGps(index, field, e.target.value);
  }
  if (e.target && e.target.getAttribute && e.target.getAttribute("data-res")) {
    const kind = e.target.getAttribute("data-res");
    const field = e.target.getAttribute("data-field");
    const index = Number(e.target.getAttribute("data-index"));
    if (kind === "link") setDraftResLink(index, field, e.target.value);
    else if (kind === "image") setDraftResImage(index, field, e.target.value);
    else if (kind === "gps") setDraftResGps(index, field, e.target.value);
  }
  if (e.target && e.target.getAttribute && e.target.getAttribute("data-fct-note")) {
    setFacetDraftNoteField(Number(e.target.getAttribute("data-index")), e.target.getAttribute("data-fct-note"), e.target.value);
  } else if (e.target && e.target.getAttribute && e.target.getAttribute("data-note")) {
    setDraftNoteField(Number(e.target.getAttribute("data-index")), e.target.getAttribute("data-note"), e.target.value);
  }
  if (e.target && e.target.classList && e.target.classList.contains("cand-search")) {
    syncCandSearchClear();
    clearTimeout(triggerCandSearch._t);
    triggerCandSearch._t = setTimeout(triggerCandSearch, 280);
  }
  if (e.target && e.target.id === "propSearch") {
    applyPropBoardFilter();
  }
  if (e.target && e.target.id && String(e.target.id).endsWith("propComment")) {
    if (e.target.value && e.target.value.trim()) clearPropCommentError();
  }
  if (e.target && e.target.classList && e.target.classList.contains("cv-theso-ac-q")) {
    scheduleThesoAc(e.target.closest(".cv-theso-ac"));
  }
});
document.addEventListener("change", (e) => {
  if (e.target && e.target.getAttribute && e.target.getAttribute("data-fct-note") === "type") {
    setFacetDraftNoteField(Number(e.target.getAttribute("data-index")), "type", e.target.value);
  } else if (e.target && e.target.getAttribute && e.target.getAttribute("data-cv-note") === "type") {
    setConceptNoteField(Number(e.target.getAttribute("data-index")), "type", e.target.value);
  } else if (e.target && e.target.getAttribute && e.target.getAttribute("data-note") === "type") {
    setDraftNoteField(Number(e.target.getAttribute("data-index")), "type", e.target.value);
  }
});
document.addEventListener("focusin", (e) => {
  const field = e.target.closest(".cv-theso-ac .coll-pick-field");
  if (field) field.classList.add("is-focused");
  if (e.target && e.target.classList && e.target.classList.contains("cv-theso-ac-q")) {
    revealThesoAc(e.target.closest(".cv-theso-ac"));
  }
});
document.addEventListener("focusout", (e) => {
  const field = e.target.closest(".cv-theso-ac .coll-pick-field");
  if (field && (!e.relatedTarget || !field.contains(e.relatedTarget))) {
    field.classList.remove("is-focused");
  }
});
document.addEventListener("selectionchange", refreshAboutFmtState);

document.addEventListener("mousedown", (e) => {
  if (e.target.closest("[id$='noteEditSave']") && typeof syncConceptNoteHidden === "function") {
    syncConceptNoteHidden();
  }
  if (e.target.closest("[id$='trEditSave']") && typeof syncConceptTrHidden === "function") {
    syncConceptTrHidden();
  }
  if (e.target.closest("[id$='resEditSave']") && typeof syncConceptResHidden === "function") {
    syncConceptResHidden();
  }
  if (e.target.closest("[id$='relEditSave']") && typeof syncCvRelHidden === "function") {
    syncCvRelHidden();
  }
}, true);

document.addEventListener("click", (e) => {
  const t = e.target.closest("[data-act]");
  if (!t) {
    if (e.target.closest("[id$='noteEditSave']") && typeof syncConceptNoteHidden === "function") {
      syncConceptNoteHidden();
    }
    if (e.target.closest("[id$='resEditSave']") && typeof syncConceptResHidden === "function") {
      syncConceptResHidden();
    }
    if (e.target.closest("[id$='relEditSave']") && typeof syncCvRelHidden === "function") {
      syncCvRelHidden();
    }
    if ($("#navThesaurus") && !$("#navThesaurus").contains(e.target)) closeThesaurus();
    if ($("#voWrap") && !$("#voWrap").contains(e.target)) $("#voGear") && $("#voGear").classList.remove("is-on");
    if ($("#viewPick") && !$("#viewPick").contains(e.target)) $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
    if ($("#previewTermLangUi") && !$("#previewTermLangUi").contains(e.target)) closeTreeLang();
    if (!e.target.closest(".st-lang-pick")) closePrefLang();
    if (!e.target.closest(".xpdf-lang-pick")) closeExportPdfLangPickers();
    if (!e.target.closest(".xcsv-lang-pick")) closeExportCsvLangPicker();
    if ($("#cvCtx") && !$("#cvCtx").contains(e.target)) closeCvCtx();
    if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
    if (!e.target.closest("#draftRelNew .re-type")) setDraftRelKindMenu(false);
    if (!e.target.closest("#draftRelNew .re-field")) hideDraftRelDrop();
    if (!e.target.closest("#cvRelEditor .re-type") && typeof setCvRelKindMenu === "function") {
      setCvRelKindMenu(false);
      if (typeof setCvRelRowMenu === "function") setCvRelRowMenu(null);
    }
    if (!e.target.closest("#cvRelNew .re-field") && typeof hideCvRelDrop === "function") hideCvRelDrop();
    if (!e.target.closest("#draftTrPick")) closeDraftTrLangMenu();
    if (!e.target.closest("#cvTrPick") && typeof closeConceptTrLang === "function") closeConceptTrLang();
    if (!e.target.closest("#cvNotePick") && typeof closeConceptNoteLang === "function") closeConceptNoteLang();
    if (!e.target.closest(".note-menu-w") && typeof closeConceptNoteTypeMenu === "function") closeConceptNoteTypeMenu();
    if (!e.target.closest("#fctTrPick") && typeof closeFacetDraftTrLang === "function") closeFacetDraftTrLang();
    if (!e.target.closest(".draft-note-lang")) closeDraftNoteLangMenu();
    if (!e.target.closest(".fct-note-lang") && typeof closeFacetDraftNoteLang === "function") closeFacetDraftNoteLang();
    if (!e.target.closest("#fctMemNew .re-field") && typeof hideFacetDraftMemDrop === "function") hideFacetDraftMemDrop();
    return;
  }
  if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
  const act = t.getAttribute("data-act");
  if (act !== "draft-rel-kind-toggle" && act !== "draft-rel-kind") setDraftRelKindMenu(false);
  if (act !== "draft-rel-hit") hideDraftRelDrop();
  if (act !== "cv-rel-kind-toggle" && act !== "cv-rel-kind"
      && act !== "cv-rel-row-kind-toggle" && act !== "cv-rel-row-kind"
      && typeof setCvRelKindMenu === "function") {
    setCvRelKindMenu(false);
    if (typeof setCvRelRowMenu === "function") setCvRelRowMenu(null);
  }
  if (act !== "cv-rel-hit" && typeof hideCvRelDrop === "function") hideCvRelDrop();
  if (act !== "term-lang-toggle" && act !== "term-lang") closeTreeLang();
  if (act !== "draft-tr-toggle" && act !== "draft-tr-add") closeDraftTrLangMenu();
  if (act !== "cv-tr-toggle" && act !== "cv-tr-add" && typeof closeConceptTrLang === "function") closeConceptTrLang();
  if (act !== "cv-note-toggle" && act !== "cv-note-add-lang" && typeof closeConceptNoteLang === "function") closeConceptNoteLang();
  if (act !== "cv-note-add" && act !== "cv-note-add-type" && typeof closeConceptNoteTypeMenu === "function") closeConceptNoteTypeMenu();
  if (act !== "fct-tr-toggle" && act !== "fct-tr-add" && typeof closeFacetDraftTrLang === "function") closeFacetDraftTrLang();
  if (act !== "draft-note-lang-toggle" && act !== "draft-note-lang") closeDraftNoteLangMenu();
  if (act !== "fct-note-lang-toggle" && act !== "fct-note-lang" && typeof closeFacetDraftNoteLang === "function") closeFacetDraftNoteLang();
  if (act !== "fct-mem-hit" && typeof hideFacetDraftMemDrop === "function") hideFacetDraftMemDrop();
  if (act !== "pref-lang-toggle" && act !== "pref-lang") closePrefLang();
  if (act !== "export-pdf-lang-toggle" && act !== "export-pdf-lang") closeExportPdfLangPickers();
  if (act !== "export-csv-lang-toggle" && act !== "export-lang" && act !== "export-lang-all" && act !== "export-lang-none") {
    closeExportCsvLangPicker();
  }
  if (act !== "cv-ctx-toggle") closeCvCtx();
  if (act === "th-dc-toggle") {
    e.preventDefault();
    toggleThDcPanel(t);
    return;
  }
  if (act === "sb-toggle") {
    e.preventDefault();
    closeThesaurus();
    setSidebarDrawer(!document.body.classList.contains("is-drawer"));
    return;
  } else if (act === "sb-dismiss") {
    e.preventDefault();
    closeSidebarDrawer();
    return;
  } else if (act === "session-stay" || act === "session-expire-dismiss") {
    e.preventDefault();
    if (typeof window.v2SessionStay === "function") window.v2SessionStay();
    return;
  } else if (act === "session-expire") {
    e.preventDefault();
    if (typeof window.v2SessionExpire === "function") window.v2SessionExpire();
    return;
  } else if (act === "session-expire-modal") {
    return;
  } else if (act === "logout-ask") {
    closeThesaurus();
    if (askLeaveThen(() => showConfirm("#logoutConfirm"))) return;
    showConfirm("#logoutConfirm");
  } else if (act === "portal-result-dismiss") {
    e.preventDefault();
    dismissPortalResult();
  } else if (act === "portal-result-modal") {
    return;
  } else if (act === "portal-publish-ask") {
    if (t.classList.contains("is-off")) return;
    showConfirm("#previewPortalPublishConfirm");
  } else if (act === "portal-publish-dismiss") {
    e.preventDefault();
    hideConfirm("#previewPortalPublishConfirm");
  } else if (act === "portal-publish-modal") {
    return;
  } else if (act === "portal-publish-go") {
    e.preventDefault();
    hideConfirm("#previewPortalPublishConfirm");
    clickPreviewJsf("previewPortalPublishGo");
  } else if (act === "portal-remove-ask") {
    if (t.classList.contains("is-off")) return;
    showConfirm("#previewPortalRemoveConfirm");
  } else if (act === "portal-remove-dismiss") {
    e.preventDefault();
    hideConfirm("#previewPortalRemoveConfirm");
  } else if (act === "portal-remove-modal") {
    return;
  } else if (act === "portal-remove-go") {
    e.preventDefault();
    hideConfirm("#previewPortalRemoveConfirm");
    clickPreviewJsf("previewPortalRemoveGo");
  } else if (act === "sync-solicit") {
    e.preventDefault();
    if (t.classList.contains("is-off")) return;
    clickPreviewJsf("syncSolicitGo");
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
  } else if (act === "label-save-ask") {
    showConfirm("#labelSaveConfirm");
  } else if (act === "label-save-dismiss") {
    hideConfirm("#labelSaveConfirm");
  } else if (act === "label-save-modal") {
    return;
  } else if (act === "coll-save-ask") {
    showConfirm("#collSaveConfirm");
  } else if (act === "coll-save-dismiss") {
    hideConfirm("#collSaveConfirm");
  } else if (act === "coll-save-modal") {
    return;
  } else if (act === "rel-save-ask") {
    showConfirm("#relSaveConfirm");
  } else if (act === "rel-save-dismiss") {
    hideConfirm("#relSaveConfirm");
  } else if (act === "rel-save-modal") {
    return;
  } else if (act === "crel-save-ask") {
    showConfirm("#crelSaveConfirm");
  } else if (act === "crel-save-dismiss") {
    hideConfirm("#crelSaveConfirm");
  } else if (act === "crel-save-modal") {
    return;
  } else if (act === "tr-save-ask") {
    showConfirm("#trSaveConfirm");
  } else if (act === "tr-save-dismiss") {
    hideConfirm("#trSaveConfirm");
  } else if (act === "tr-save-modal") {
    return;
  } else if (act === "cv-tr-ask") {
    askRemoveConceptTr(t.getAttribute("data-lang"));
  } else if (act === "cv-tr-keep") {
    keepConceptTr();
  } else if (act === "cv-tr-drop") {
    removeConceptTr(t.getAttribute("data-lang"));
  } else if (act === "cv-tr-toggle") {
    const btn = $("#cvTrBtn");
    setConceptTrLangOpen(btn && !btn.classList.contains("is-open"));
  } else if (act === "cv-tr-add") {
    if (t.disabled || t.classList.contains("is-used")) return;
    addConceptTr(t.getAttribute("data-lang"));
  } else if (act === "cv-note-tab") {
    setConceptNoteLang(t.getAttribute("data-lang"));
  } else if (act === "cv-note-cmp") {
    toggleConceptNoteCompare();
  } else if (act === "cv-note-toggle") {
    const btn = $("#cvNoteBtn");
    setConceptNoteLangOpen(btn && !btn.classList.contains("is-open"));
  } else if (act === "cv-note-add-lang") {
    if (t.disabled || t.classList.contains("is-used")) return;
    addConceptNoteLang(t.getAttribute("data-lang"));
  } else if (act === "cv-note-add") {
    addConceptNote();
  } else if (act === "cv-note-add-type") {
    addConceptNoteType(t.getAttribute("data-type"));
  } else if (act === "cv-note-remove") {
    removeConceptNote(Number(t.getAttribute("data-index")));
  } else if (act === "cv-res-add") {
    e.preventDefault();
    if (typeof addConceptResLink === "function") addConceptResLink();
  } else if (act === "cv-res-remove") {
    e.preventDefault();
    if (typeof removeConceptResLink === "function") removeConceptResLink(Number(t.getAttribute("data-index")));
  } else if (act === "cv-res-img-add") {
    e.preventDefault();
    if (typeof addConceptResImage === "function") addConceptResImage();
  } else if (act === "cv-res-img-remove") {
    e.preventDefault();
    if (typeof removeConceptResImage === "function") removeConceptResImage(Number(t.getAttribute("data-index")));
  } else if (act === "cv-res-gps-add") {
    e.preventDefault();
    if (typeof addConceptResGps === "function") addConceptResGps();
  } else if (act === "cv-res-gps-remove") {
    e.preventDefault();
    if (typeof removeConceptResGps === "function") removeConceptResGps(Number(t.getAttribute("data-index")));
  } else if (act === "cv-res-gps-goto") {
    e.preventDefault();
    if (typeof gotoConceptResGps === "function") gotoConceptResGps(Number(t.getAttribute("data-index")));
  } else if (act === "cv-res-img-open") {
    e.preventDefault();
    if (typeof openConceptResImgLightbox === "function") openConceptResImgLightbox(Number(t.getAttribute("data-index")));
  } else if (act === "cv-res-img-lightbox-prev") {
    e.preventDefault();
    if (typeof showConceptResImgAt === "function") showConceptResImgAt(cvResImgView.index - 1);
  } else if (act === "cv-res-img-lightbox-next") {
    e.preventDefault();
    if (typeof showConceptResImgAt === "function") showConceptResImgAt(cvResImgView.index + 1);
  } else if (act === "cv-res-img-lightbox-dismiss" || act === "cv-res-img-lightbox-close") {
    e.preventDefault();
    if (typeof closeConceptResImgLightbox === "function") closeConceptResImgLightbox();
  } else if (act === "cv-res-img-lightbox-modal") {
    return;
  } else if (act === "align-save-ask") {
    showConfirm("#alignSaveConfirm");
  } else if (act === "align-save-dismiss") {
    hideConfirm("#alignSaveConfirm");
  } else if (act === "align-save-modal") {
    return;
  } else if (act === "note-save-ask") {
    showConfirm("#noteSaveConfirm");
  } else if (act === "note-save-dismiss") {
    hideConfirm("#noteSaveConfirm");
  } else if (act === "note-save-modal") {
    return;
  } else if (act === "res-save-ask") {
    const overlay = t.closest(".crow") && t.closest(".crow").querySelector(".confirm-overlay");
    showConfirm(overlay && overlay.id ? "#" + overlay.id : "#resLinkSaveConfirm");
  } else if (act === "res-save-dismiss") {
    const overlay = t.closest(".confirm-overlay");
    hideConfirm(overlay && overlay.id ? "#" + overlay.id : "#resLinkSaveConfirm");
  } else if (act === "res-save-modal") {
    return;
  } else if (act === "st-save-ask") {
    showConfirm("#stSaveConfirm");
  } else if (act === "st-save-go") {
    e.preventDefault();
    clickPreviewJsf("previewPrefSaveGo");
  } else if (act === "account-save") {
    e.preventDefault();
    clickPreviewJsf("previewAccountSaveGo");
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
    ["#previewView", "#viewHome", "#viewLive", "#viewConcept", "#viewSettings", "main.content .view"].forEach((sel) => {
      const el = $(sel);
      if (el) el.scrollTo({ top: 0, behavior });
    });
  } else if (act === "thesaurus") {
    const on = t.classList.contains("is-on");
    closeThesaurus();
    if (!on) {
      t.classList.add("is-on");
      t.setAttribute("aria-expanded", "true");
      if (t.getAttribute("data-thesaurus") === "account") {
        if (isLoggedInUi()) {
          openAccountMenu();
        } else {
          openLoginMenu();
        }
      } else {
        const pop = t.nextElementSibling;
        if (pop) {
          pop.classList.remove("is-forced-closed");
          pop.hidden = false;
        }
      }
    }
  } else if (act === "login-forgot") {
    e.preventDefault();
    openLoginMenu();
    const pop = $("#menu-login");
    if (pop) pop.classList.add("is-forgot");
    const mail = document.getElementById("previewForgotMail");
    if (mail) window.setTimeout(() => mail.focus(), 0);
  } else if (act === "login-forgot-back") {
    e.preventDefault();
    const pop = $("#menu-login");
    if (pop) pop.classList.remove("is-forgot");
    const user = document.getElementById("previewLoginUser");
    if (user) window.setTimeout(() => user.focus(), 0);
  } else if (act === "home") openHome();
  else if (act === "back-graph") backToGraph();
  else if (act === "back-cand-list") {
    e.preventDefault();
    backToCandList();
  }
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
  } else if (act === "prop-search-clear") {
    const input = $("#propSearch");
    if (input) {
      input.value = "";
      input.focus();
    }
    applyPropBoardFilter();
  } else if (act === "cand-search-clear") {
    const input = document.querySelector(".cand-search");
    if (input) {
      input.value = "";
      input.focus();
    }
    syncCandSearchClear();
    triggerCandSearch();
  } else if (act === "cand-tab") {
    const board = t.closest(".cand-board") || $("#candBoard");
    const tab = t.getAttribute("data-tab") || "attente";
    if (board) {
      board.setAttribute("data-tab", tab);
      board.querySelectorAll(".cand-tab").forEach((btn) => {
        const on = btn.getAttribute("data-tab") === tab;
        btn.classList.toggle("is-on", on);
        btn.setAttribute("aria-selected", on ? "true" : "false");
      });
    }
    const hidden = document.getElementById("candBoardForm:activeTab");
    if (hidden) hidden.value = tab;
  } else if (act === "bo-op") {
    setBatch(t.getAttribute("data-obj"), t.getAttribute("data-op"));
  } else if (act === "bo-acc") {
    const step = t.closest(".bo-acc-step");
    if (step) step.classList.toggle("open");
  } else if (act === "bo-pick" || act === "bo-clear" || act === "bo-check" || act === "bo-reimport" || act === "bo-run") {
    const live = t.closest(".bo-panel[data-live='1']");
    if (live) return; // panneau branché JSF — ne pas simuler
    if (act === "bo-pick") {
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
  }
  else if (act === "toggle") {
    const tn = t.closest(".tn");
    if (tn && !t.classList.contains("is-empty")) {
      const open = !tn.classList.contains("is-open");
      tn.classList.toggle("is-open", open);
      if (open) replayAnim(tn, "is-branch-in");
    }
  } else if (act === "col-toggle") {
    toggleCollectionNode(t.closest(".tn"));
  } else if (act === "col-sort") {
    setCollectionSort(t.getAttribute("data-sort"));
  } else if (act === "open") {
    const candRow = t.closest("a.cand-row");
    const pathLink = t.closest("a.path-link");
    if ((candRow || pathLink) && (e.metaKey || e.ctrlKey || e.shiftKey)) return;
    if (candRow || pathLink) e.preventDefault();
    const id = t.getAttribute("data-id");
    const nodeType = resolveNodeType(t);
    if (nodeType === "group" || nodeType === "subgroup") {
      openCollectionFromTree(id);
      closeSearchUi();
      closeSidebarDrawer();
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
      closeSidebarDrawer();
      return;
    }
    openConcept(id, stay ? "stay" : "jump");
    closeSidebarDrawer();
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
  } else if (act === "open-recent") {
    const id = t.getAttribute("data-id");
    if (!id) return;
    state.home = false;
    if (!openLiveDetail(id, "concept")) {
      go("index.xhtml?idc=" + encodeURIComponent(id));
    }
  } else if (act === "maint-confirm-ok") {
    hideConfirm("#maintConfirm");
    if (pendingMaintBtn) {
      pendingMaintBtn.dataset.maintSkipConfirm = "1";
      pendingMaintBtn.click();
      pendingMaintBtn = null;
    }
  } else if (act === "maint-confirm-dismiss") {
    hideConfirm("#maintConfirm");
    pendingMaintBtn = null;
  } else if (act === "maint-confirm-modal") {
    return;
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
    const value = t.getAttribute("data-copy") || "";
    try { navigator.clipboard.writeText(value); } catch (_) {}
    t.classList.add("is-copied");
    const copied = (document.body && document.body.getAttribute("data-msg-copied")) || "Copié";
    t.title = copied;
    toast(copied, { soft: true });
    setTimeout(() => { t.classList.remove("is-copied"); }, 1400);
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
  else if (act === "create-open") {
    e.preventDefault();
    e.stopPropagation();
    showCreateChooser(t);
  } else if (act === "create-dismiss") {
    e.preventDefault();
    hideCreateChooser();
  } else if (act === "create-modal") {
    return;
  } else if (act === "create-pick") {
    e.preventDefault();
    if (t.classList.contains("is-locked") || t.disabled) return;
    pickCreateKind(t.getAttribute("data-kind"));
  } else if (act === "create") {
    e.stopPropagation();
    showCreateChooser(t);
  } else if (act === "cpt-more") {
    const more = $("#cptMore");
    setConceptDraftMore(more && more.hidden);
  } else if (act === "cpt-create-ask") {
    requestConceptDraftCreate(t.getAttribute("data-kind"));
  } else if (act === "cpt-create-dismiss") {
    hideConceptDraftCreateConfirm();
  } else if (act === "cpt-create-modal") {
    return;
  } else if (act === "cpt-create-go") {
    confirmConceptDraftCreate(t.getAttribute("data-kind"));
  } else if (act === "cpt-leave") {
    requestConceptDraftLeave();
  } else if (act === "cpt-leave-dismiss" || act === "cpt-leave-stay") {
    hideConceptDraftLeave();
  } else if (act === "cpt-leave-modal") {
    return;
  } else if (act === "cpt-leave-quit") {
    resolveConceptDraft("annulé");
  } else if (act === "fct-more") {
    const more = $("#fctMore");
    setFacetDraftMore(more && more.hidden);
  } else if (act === "fct-create-ask") {
    requestFacetDraftCreate();
  } else if (act === "fct-create-dismiss") {
    hideFacetDraftCreateConfirm();
  } else if (act === "fct-create-modal") {
    return;
  } else if (act === "fct-create-go") {
    confirmFacetDraftCreate();
  } else if (act === "fct-leave") {
    requestFacetDraftLeave();
  } else if (act === "fct-leave-dismiss" || act === "fct-leave-stay") {
    hideFacetDraftLeave();
  } else if (act === "fct-leave-modal") {
    return;
  } else if (act === "fct-leave-quit") {
    resolveFacetDraft("annulé");
  } else if (act === "fct-tr-ask") {
    askRemoveFacetDraftTr(t.getAttribute("data-lang"));
  } else if (act === "fct-tr-keep") {
    keepFacetDraftTr();
  } else if (act === "fct-tr-drop") {
    removeFacetDraftTr(t.getAttribute("data-lang"));
  } else if (act === "fct-tr-toggle") {
    const btn = $("#fctTrBtn");
    setFacetDraftTrLangOpen(btn && !btn.classList.contains("is-open"));
  } else if (act === "fct-tr-add") {
    if (t.disabled || t.classList.contains("is-used")) return;
    addFacetDraftTr(t.getAttribute("data-lang"));
  } else if (act === "fct-note-add") {
    addFacetDraftNote();
  } else if (act === "fct-note-remove") {
    removeFacetDraftNote(Number(t.getAttribute("data-index")));
  } else if (act === "fct-note-lang-toggle") {
    setFacetDraftNoteLangOpen(Number(t.getAttribute("data-index")));
  } else if (act === "fct-note-lang") {
    setFacetDraftNoteLang(Number(t.getAttribute("data-index")), t.getAttribute("data-lang"));
  } else if (act === "fct-mem-hit") {
    addFacetDraftMem(t.getAttribute("data-id"), t.getAttribute("data-label"));
  } else if (act === "fct-mem-remove") {
    removeFacetDraftMem(t.getAttribute("data-index"));
  } else if (act === "cpt-nt-rel") {
    const hidden = $("#cptNtRel");
    if (hidden) hidden.value = t.getAttribute("data-val") || "NT";
    $$("#viewConceptDraft .cv-add-rel").forEach((btn) => {
      const on = btn === t;
      btn.classList.toggle("is-on", on);
      btn.setAttribute("aria-selected", on ? "true" : "false");
    });
  } else if (act === "draft-more") {
    const more = $("#draftMore");
    setDraftMore(more && more.hidden);
  } else if (act === "draft-create-ask") {
    requestDraftCreate(t.getAttribute("data-kind"));
  } else if (act === "draft-create-dismiss") {
    hideDraftCreateConfirm();
  } else if (act === "draft-create-modal") {
    return;
  } else if (act === "draft-create-go") {
    confirmDraftCreate();
  } else if (act === "draft-leave") {
    requestDraftLeave();
  } else if (act === "draft-leave-dismiss" || act === "draft-leave-stay") {
    hideDraftLeave();
  } else if (act === "draft-leave-modal") {
    return;
  } else if (act === "draft-leave-quit") {
    resolveDraft("annulé");
  } else if (act === "draft-rel-kind-toggle") {
    const menu = $("#draftRelKindMenu");
    setDraftRelKindMenu(menu && menu.hidden);
  } else if (act === "draft-rel-kind") {
    setDraftRelKind(t.getAttribute("data-kind"));
  } else if (act === "draft-rel-remove") {
    removeDraftRel(t.getAttribute("data-kind"), t.getAttribute("data-index"));
  } else if (act === "draft-rel-hit") {
    addDraftRel(t.getAttribute("data-id"), t.getAttribute("data-label"));
  } else if (act === "cv-rel-kind-toggle") {
    e.preventDefault();
    if (typeof setCvRelRowMenu === "function") setCvRelRowMenu(null);
    const menu = $("#cvRelKindMenu");
    if (typeof setCvRelKindMenu === "function") setCvRelKindMenu(menu && menu.hidden);
  } else if (act === "cv-rel-kind") {
    e.preventDefault();
    if (typeof setCvRelKind === "function") setCvRelKind(t.getAttribute("data-kind"));
  } else if (act === "cv-rel-row-kind-toggle") {
    e.preventDefault();
    if (typeof setCvRelKindMenu === "function") setCvRelKindMenu(false);
    if (typeof setCvRelRowMenu === "function") {
      const key = (t.getAttribute("data-kind") || "") + ":" + (t.getAttribute("data-index") || "");
      setCvRelRowMenu(cvRelState && cvRelState.rowMenu === key ? null : key);
    }
  } else if (act === "cv-rel-row-kind") {
    e.preventDefault();
    if (typeof retypeCvRel === "function") {
      retypeCvRel(t.getAttribute("data-from"), t.getAttribute("data-index"), t.getAttribute("data-kind"));
    }
  } else if (act === "cv-rel-remove") {
    e.preventDefault();
    if (typeof removeCvRel === "function") removeCvRel(t.getAttribute("data-kind"), t.getAttribute("data-index"));
  } else if (act === "cv-rel-hit") {
    e.preventDefault();
    if (typeof addCvRel === "function") addCvRel(t.getAttribute("data-id"), t.getAttribute("data-label"));
  } else if (act === "draft-tr-ask") {
    draftTrState.confirm = t.getAttribute("data-lang") || "";
    paintDraftTr();
  } else if (act === "draft-tr-keep") {
    draftTrState.confirm = "";
    paintDraftTr();
  } else if (act === "draft-tr-drop") {
    removeDraftTr(t.getAttribute("data-lang"));
  } else if (act === "draft-tr-toggle") {
    const btn = $("#draftTrBtn");
    setDraftTrLangOpen(btn && !btn.classList.contains("is-open"));
  } else if (act === "draft-tr-add") {
    if (t.disabled || t.classList.contains("is-used")) return;
    addDraftTr(t.getAttribute("data-lang"));
  } else if (act === "draft-res-add") {
    addDraftResLink();
  } else if (act === "draft-res-remove") {
    removeDraftResLink(Number(t.getAttribute("data-index")));
  } else if (act === "draft-img-add") {
    addDraftResImage();
  } else if (act === "draft-img-remove") {
    removeDraftResImage(Number(t.getAttribute("data-index")));
  } else if (act === "draft-gps-add") {
    addDraftResGps();
  } else if (act === "draft-gps-remove") {
    removeDraftResGps(Number(t.getAttribute("data-index")));
  } else if (act === "draft-gps-goto") {
    gotoDraftGps(Number(t.getAttribute("data-index")));
  } else if (act === "draft-img-open") {
    openDraftImgLightbox(Number(t.getAttribute("data-index")));
  } else if (act === "draft-img-lightbox-prev") {
    showDraftImgAt(draftImgView.index - 1);
  } else if (act === "draft-img-lightbox-next") {
    showDraftImgAt(draftImgView.index + 1);
  } else if (act === "draft-img-lightbox-dismiss" || act === "draft-img-lightbox-close") {
    closeDraftImgLightbox();
  } else if (act === "draft-img-lightbox-modal") {
    return;
  } else if (act === "draft-note-add") {
    addDraftNote();
  } else if (act === "draft-note-remove") {
    removeDraftNote(Number(t.getAttribute("data-index")));
  } else if (act === "draft-note-lang-toggle") {
    setDraftNoteLangOpen(Number(t.getAttribute("data-index")));
  } else if (act === "draft-note-lang") {
    setDraftNoteLang(Number(t.getAttribute("data-index")), t.getAttribute("data-lang"));
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
    persistTreeStatus();
  } else if (act === "st-item") {
    e.preventDefault();
    const s = t.getAttribute("data-status");
    if (state.statusSet.has(s)) state.statusSet.delete(s);
    else state.statusSet.add(s);
    syncStatusUi();
    persistTreeStatus();
  } else if (act === "sf-toggle") {
    e.preventDefault();
    const tn = t.closest(".tn");
    if (!tn || !tn.closest("[data-status-forest]")) return;
    tn.classList.toggle("is-open");
    const box = tn.parentElement;
    const list = box ? Array.from(box.querySelectorAll(":scope > .tn")) : [];
    const openAt = [];
    list.forEach((node) => {
      const depth = treeDepth(node);
      openAt.length = depth;
      node.hidden = openAt.some((on) => !on);
      openAt[depth] = node.classList.contains("is-open");
    });
  } else if (act === "cf-toggle") {
    const combo = t.closest(".cf-combo") || $("#cfCombo");
    if (!combo) return;
    combo.classList.toggle("open");
    if (combo.classList.contains("open")) {
      loadCandByUsers(($("#cfByQuery") && $("#cfByQuery").value) || "");
      const q = $("#cfByQuery");
      if (q) q.focus();
    }
  } else if (act === "cf-by") {
    setCandBySelection(t.getAttribute("data-by") || "");
    const combo = t.closest(".cf-combo");
    if (combo) combo.classList.remove("open");
    applyStatusFilter();
  } else if (act === "cf-clear") {
    state.candFrom = "";
    state.candTo = "";
    const from = $("#cfFrom"), to = $("#cfTo");
    if (from) from.value = "";
    if (to) to.value = "";
    setCandBySelection("");
    resetCandBySearch();
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
    if (!col || TABLE_COL_ALL.indexOf(col) < 0) return;
    if (state.tblCols.has(col)) state.tblCols.delete(col);
    else state.tblCols.add(col);
    applyTableCols();
    persistTableCols();
  } else if (act === "cblock-expand") {
    expandConceptBlock(t);
  } else if (act === "cblock-fold") {
    if (typeof toggleConceptBlockFold === "function") toggleConceptBlockFold(t);
  } else if (act === "cblock-collapse") {
    closeConceptBlockOverlay();
  } else if (act === "align-auto-compare") {
    return;
  } else if (act === "align-delete-ask") {
    const idInput = document.querySelector("[id$='alignDeleteId']");
    const uriEl = document.getElementById("alignDeleteUri");
    if (idInput) idInput.value = t.getAttribute("data-id") || "";
    if (uriEl) uriEl.textContent = t.getAttribute("data-uri") || "";
    showConfirm("#alignDeleteConfirm");
  } else if (act === "align-delete-dismiss") {
    hideConfirm("#alignDeleteConfirm");
  } else if (act === "align-delete-modal") {
    return;
  } else if (act === "align-replace-ask") {
    const idInput = document.querySelector("[id$='alignReplaceIndex']");
    const uriEl = document.getElementById("alignReplaceUri");
    if (idInput) idInput.value = t.getAttribute("data-index") || "";
    if (uriEl) uriEl.textContent = t.getAttribute("data-uri") || "";
    showConfirm("#alignReplaceConfirm");
  } else if (act === "align-replace-dismiss") {
    hideConfirm("#alignReplaceConfirm");
  } else if (act === "align-replace-modal") {
    return;
  } else if (act === "cand-reactivate-ask") {
    e.preventDefault();
    showConfirm("#candReactivateConfirm");
    return;
  } else if (act === "cand-reactivate-dismiss") {
    e.preventDefault();
    hideConfirm("#candReactivateConfirm");
    return;
  } else if (act === "cand-reactivate-modal") {
    return;
  } else if (act === "cand-reactivate-go") {
    e.preventDefault();
    hideConfirm("#candReactivateConfirm");
    const btn = document.querySelector("[id$='candReactivateGo']");
    if (btn) btn.click();
    return;
  } else if (act === "cand-delete-ask") {
    e.preventDefault();
    showConfirm("#candDeleteConfirm");
    return;
  } else if (act === "cand-delete-dismiss") {
    e.preventDefault();
    hideConfirm("#candDeleteConfirm");
    return;
  } else if (act === "cand-delete-modal") {
    return;
  } else if (act === "cand-delete-go") {
    e.preventDefault();
    hideConfirm("#candDeleteConfirm");
    const btn = document.querySelector("[id$='candDeleteGo']");
    if (btn) btn.click();
    return;
  } else if (act === "ui-lang") {
    e.preventDefault();
    const lang = t.getAttribute("data-lang");
    if (!lang || t.classList.contains("is-on")) {
      closeThesaurus();
      return;
    }
    clickPreviewJsf("previewUiLangGo", { previewUiLangCode: lang });
    closeThesaurus();
  } else if (act === "term-lang-toggle") {
    e.preventDefault();
    if (t.classList.contains("is-solo")) return;
    const open = !t.classList.contains("is-open");
    closeThesaurus();
    $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
    t.classList.toggle("is-open", open);
    t.setAttribute("aria-expanded", open ? "true" : "false");
  } else if (act === "term-lang") {
    e.preventDefault();
    const lang = t.getAttribute("data-lang");
    closeTreeLang();
    if (!lang || t.classList.contains("is-on")) return;
    clickPreviewJsf("previewTermLangGo", { termLang: lang });
  } else if (act === "pref-lang-toggle") {
    e.preventDefault();
    const open = !t.classList.contains("is-open");
    closeThesaurus();
    closeTreeLang();
    closePrefLang();
    t.classList.toggle("is-open", open);
    t.setAttribute("aria-expanded", open ? "true" : "false");
  } else if (act === "pref-lang") {
    e.preventDefault();
    if (t.classList.contains("is-on")) {
      closePrefLang();
      return;
    }
    paintPrefSourceLang(t);
  } else if (act === "cv-ctx-toggle") {
    e.preventDefault();
    const open = !t.classList.contains("is-open");
    closeThesaurus();
    $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
    closeTreeLang();
    t.classList.toggle("is-open", open);
    t.setAttribute("aria-expanded", open ? "true" : "false");
  } else if (act === "add-nt-rel") {
    e.preventDefault();
    const dlg = t.closest(".confirm-overlay");
    if (!dlg) return;
    const hidden = dlg.querySelector("[id$='cvAddNtRel']");
    if (hidden) hidden.value = t.getAttribute("data-val") || "NT";
    paintAddNtRel(dlg);
    paintAddNtPreview(dlg);
  } else if (act === "csv-delim") {
    e.preventDefault();
    const dlg = t.closest(".confirm-overlay");
    if (!dlg || dlg.classList.contains("is-busy") || dlg.classList.contains("is-done")) return;
    const hidden = csvDelimInput(dlg);
    if (hidden) hidden.value = t.getAttribute("data-val") || "0";
    paintCsvDelim(dlg);
    if (csvHasLoadedFile(dlg)) clickCsvAnalyze(dlg);
  } else if (act === "csv-browse") {
    e.preventDefault();
    const dlg = t.closest(".confirm-overlay");
    if (!dlg || dlg.classList.contains("is-busy") || dlg.classList.contains("is-done")) return;
    const file = csvFileInput(dlg);
    if (file) file.click();
  } else if (act === "cv-dlg-dismiss") {
    e.preventDefault();
    const ark = visibleArkDialog();
    if (ark && ark.classList.contains("is-busy")) return;
    hideCvDialogs();
  } else if (act === "cv-dlg-modal") {
    return;
  } else if (act === "theso-dest") {
    e.preventDefault();
    setThesoDestMode(t.getAttribute("data-prefix"), t.getAttribute("data-mode"), t.closest(".confirm-modal"));
  } else if (act === "theso-id") {
    e.preventDefault();
    setThesoIdType(t.closest(".confirm-modal"), t.getAttribute("data-type"));
  } else if (act === "theso-parent-clear") {
    e.preventDefault();
    clearThesoParent(t.closest(".cv-theso-ac"));
  } else if (act === "theso-parent-chip") {
    e.preventDefault();
    clearThesoParent(t.closest(".cv-theso-ac"));
  } else if (act === "theso-query-clear") {
    e.preventDefault();
    clearThesoQuery(t.closest(".cv-theso-ac"));
  } else if (act === "theso-parent-toggle") {
    e.preventDefault();
    toggleThesoAcMenu(t.closest(".cv-theso-ac"));
  } else if (act === "theso-parent-more") {
    e.preventDefault();
    const root = t.closest(".cv-theso-ac");
    if (root) {
      thesoAcSt(root).showAll = true;
      renderThesoAcMenu(root);
      revealThesoAc(root);
    }
  } else if (act === "theso-parent-pick") {
    e.preventDefault();
    pickThesoParent(t.closest(".cv-theso-ac"), t.getAttribute("data-id"), t.getAttribute("data-label"));
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
    if (act === "export-lang") {
      e.preventDefault();
      t.setAttribute("aria-selected", t.classList.contains("is-on") ? "true" : "false");
      refreshExportLangMeta();
    }
  } else if (act === "export-lang-all") {
    if (exportBusy) return;
    e.preventDefault();
    setExportLangSelection(true);
  } else if (act === "export-lang-none") {
    if (exportBusy) return;
    e.preventDefault();
    setExportLangSelection(false);
  } else if (act === "export-csv-lang-toggle") {
    if (exportBusy) return;
    e.preventDefault();
    /* Page export : liste CSV déjà ouverte en permanence. */
    if (t.closest(".tp-export-host")) return;
    const open = !t.classList.contains("is-open");
    closeExportCsvLangPicker(open ? t : null);
    closeExportPdfLangPickers();
    t.classList.toggle("is-open", open);
    t.setAttribute("aria-expanded", open ? "true" : "false");
  } else if (act === "export-pdf-lang-toggle") {
    if (exportBusy) return;
    e.preventDefault();
    const open = !t.classList.contains("is-open");
    closeExportPdfLangPickers(open ? t : null);
    closeExportCsvLangPicker();
    t.classList.toggle("is-open", open);
    t.setAttribute("aria-expanded", open ? "true" : "false");
  } else if (act === "export-pdf-lang") {
    if (exportBusy) return;
    e.preventDefault();
    const slot = parseInt(t.getAttribute("data-slot") || "0", 10);
    const code = t.getAttribute("data-code") || "";
    const label = t.getAttribute("data-label") || code;
    if (slot === 1 || slot === 2) setPdfLangValue(slot, code, label);
    closeExportPdfLangPickers();
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
    if (live) live.classList.remove("is-loading");
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

const TH_DC_OPEN_KEY = "v2-th-dc-open";

function applyThDcOpen(root, open) {
  if (!root) return;
  root.classList.toggle("is-open", open);
  const btn = root.querySelector("[data-act='th-dc-toggle']");
  if (btn) btn.setAttribute("aria-expanded", open ? "true" : "false");
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
  window.location.href = ctx + "/v2/api/maintenance/sitemap.xml";
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

var params = new URLSearchParams(location.search);

if (SCREEN === "atelier") {
  setBatch(params.get("obj") || "alignements");
}
if (SCREEN === "synchronisation") {
  syncThesaurusPollTick();
}
if (SCREEN === "portail") {
  portalPublishPollTick();
}
if ((IS_CONSULT || SCREEN === "accueil") && params.get("create") === "concept") {
  createConceptDraft({
    getAttribute: (name) => {
      if (name === "data-pref") return params.get("pref") || "";
      if (name === "data-id") return params.get("idc") || "";
      return "";
    }
  });
}
if ((IS_CONSULT || SCREEN === "accueil") && params.get("create") === "facet") {
  createFacetDraft({
    getAttribute: (name) => {
      if (name === "data-pref") return params.get("pref") || "";
      if (name === "data-id") return params.get("idc") || "";
      return "";
    }
  });
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
if (IS_CONSULT || SCREEN === "accueil") {
  const q = params.get("q");
  const id = params.get("id") || params.get("idc");
  const view = params.get("view");
  if (q) {
    if (input) input.value = q;
    if (clear) clear.hidden = !q;
    runSearch();
  } else if (typeof liveDetailRequested === "function" && liveDetailRequested()) {
    if (typeof showLiveDetail === "function") showLiveDetail();
    paint();
  } else if (id) {
    if (!openLiveDetail(id, params.get("type") || "")) {
      openConcept(id);
    }
  } else if (IS_CONSULT && view) {
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
  syncCandSearchClear();
  applyPropBoardFilter();
  if (window.syncViewRail) window.syncViewRail();
  scrollToPrefHash();
  if (SCREEN === "preference") rememberSettingsBaseline();
});
if (SCREEN === "preference" && location.hash) {
  window.addEventListener("load", scrollToPrefHash);
  setTimeout(scrollToPrefHash, 80);
  setTimeout(scrollToPrefHash, 250);
}

/* ── Transitions subtiles : rail admin + bouton retour ── */
(function () {
  const LEAVE_MS = 220;

  function reducedMotion() {
    return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  }

  function leaveRoot() {
    return document.getElementById("iaRoot")
      || document.getElementById("tpRoot")
      || document.getElementById("previewView")
      || document.querySelector(".main");
  }

  /**
   * Anime une sortie légère puis poursuit la navigation (lien ou 2e clic JSF).
   * opts.href : navigation URL après l'animation
   */
  window.iaAnimateLeave = function (el, opts) {
    opts = opts || {};
    if (el && el.getAttribute("data-ia-leaving") === "1") {
      return true;
    }
    if (el) {
      el.classList.add("is-leaving");
      el.setAttribute("data-ia-leaving", "1");
    }
    const root = leaveRoot();
    if (root) root.classList.add("ia--leaving");
    document.body.classList.add("ia-nav-leaving");

    const delay = reducedMotion() ? 0 : LEAVE_MS;
    window.setTimeout(function () {
      document.body.classList.remove("ia-nav-leaving");
      if (opts.href) {
        window.location.href = opts.href;
        return;
      }
      if (!el) return;
      if (typeof el.click === "function") {
        el.click();
      }
    }, delay);
    return false;
  };

  window.iaAnimateRailToAdmin = function (el, evt) {
    if (evt) {
      evt.preventDefault();
      evt.stopPropagation();
    }
    if (!el) return false;
    if (el.getAttribute("data-ia-leaving") === "1") {
      return true;
    }
    el.classList.add("is-nav-pulse", "is-leaving");
    return window.iaAnimateLeave(el, { href: el.href || el.getAttribute("href") });
  };

  window.iaOnBackAjax = function (data) {
    if (!data) return;
    const root = document.getElementById("iaRoot");
    const clearLeaving = function () {
      document.body.classList.remove("ia-nav-leaving");
      document.querySelectorAll(".ia--leaving").forEach(function (el) {
        el.classList.remove("ia--leaving");
      });
    };
    if (data.status === "error") {
      clearLeaving();
      return;
    }
    if (data.status === "success") {
      clearLeaving();
      if (!root) return;
      root.classList.remove("ia--enter");
      void root.offsetWidth;
      root.classList.add("ia--enter");
    }
  };

  document.addEventListener("DOMContentLoaded", function () {
    if (document.body && document.body.getAttribute("data-page") === "admin-instance") {
      const adminBtn = document.querySelector('.thesaurus a.thesaurus-btn[title="Administration de l\'instance"]');
      if (adminBtn) adminBtn.classList.add("is-on");
      iaConsumeFlash();
    }
  });

  function iaConsumeFlash() {
    const live = document.getElementById("iaFlashLive");
    if (!live || typeof window.toast !== "function") return;
    const msg = live.getAttribute("data-ia-flash");
    const token = live.getAttribute("data-ia-flash-token");
    const error = live.getAttribute("data-ia-flash-error") === "true";
    if (!msg || !token) return;
    if (window._iaFlashToken === token) return;
    window._iaFlashToken = token;
    window.toast(msg, error ? { error: true } : undefined);
  }

  function iaUsersListBusy(on) {
    const toolbar = document.getElementById("iaUsersToolbar");
    const table = document.getElementById("iaUsersTable");
    clearTimeout(window._iaUsersBusyTimer);
    [toolbar, table].forEach(function (el) {
      if (!el) return;
      el.classList.toggle("is-busy", !!on);
      if (on) el.setAttribute("aria-busy", "true");
      else el.removeAttribute("aria-busy");
    });
    if (on) {
      window._iaUsersBusyTimer = setTimeout(function () {
        iaUsersListBusy(false);
        window._iaUsersFilterBusy = false;
      }, 8000);
    }
  }

  window.iaClickProxy = function (id) {
    const go = document.getElementById(id);
    if (!go) return false;
    // Déclencher le handler Mojarra du bouton (onclick généré) : un
    // faces.ajax.request manuel sans event 'action' n'invoque pas l'action.
    if (typeof go.click === "function") {
      go.click();
    }
    return false;
  };

  window.iaFocusNewProject = function (data) {
    if (!data || data.status !== "success") return;
    const input = document.getElementById("iaNewProjectName");
    if (!input) return;
    input.focus();
    if (typeof input.select === "function") {
      try { input.select(); } catch (ex) {}
    }
  };

  window.iaNewProjectKeydown = function (event) {
    if (!event || event.key !== "Enter") return true;
    event.preventDefault();
    const go = document.getElementById("iaNewProjectSubmit");
    if (go && typeof go.click === "function") go.click();
    return false;
  };

  window.iaQueueUserAction = function (el) {
    if (!el || window._iaUsersFilterBusy) return false;
    const action = el.getAttribute("data-ia-action");
    const userId = el.getAttribute("data-ia-user-id");
    const username = el.getAttribute("data-ia-username") || "";
    const actionInput = document.getElementById("iaPendingUserAction");
    const idInput = document.getElementById("iaPendingActionUserId");
    const nameInput = document.getElementById("iaPendingActionUsername");
    const go = document.getElementById("iaUserActionGo");
    if (!action || !userId || !actionInput || !idInput || !go) return false;
    actionInput.value = action;
    idInput.value = String(userId);
    if (nameInput) nameInput.value = username;
    go.click();
    return false;
  };

  window.iaUsersListAjaxGate = function (data) {
    if (!data) return;
    const src = data.source;
    const srcId = (src && (src.id || (src.getAttribute && src.getAttribute("id")))) || "";
    if (data.status === "begin") {
      window._iaUsersFilterBusy = true;
      iaUsersListBusy(true);
      if (srcId.indexOf("iaUserQuery") >= 0) {
        const input = document.getElementById("iaUserQuery");
        if (input && typeof input.selectionStart === "number") {
          window._iaUsersQueryCaret = input.selectionStart;
        }
      }
      return;
    }
    if (data.status === "complete" || data.status === "error") {
      window._iaUsersFilterBusy = false;
      iaUsersListBusy(false);
      if (srcId.indexOf("iaUserQuery") >= 0) {
        const input = document.getElementById("iaUserQuery");
        if (input) {
          input.focus();
          const caret = window._iaUsersQueryCaret;
          if (typeof caret === "number" && input.setSelectionRange) {
            try { input.setSelectionRange(caret, caret); } catch (ex) {}
          }
        }
      }
    }
  };

  function iaOnAjaxFlash(data) {
    if (!data || data.status !== "success") return;
    if (!document.body || document.body.getAttribute("data-page") !== "admin-instance") return;
    iaConsumeFlash();
  }

  if (window.faces && faces.ajax && typeof faces.ajax.addOnEvent === "function") {
    faces.ajax.addOnEvent(iaOnAjaxFlash);
  } else if (window.jsf && jsf.ajax && typeof jsf.ajax.addOnEvent === "function") {
    jsf.ajax.addOnEvent(iaOnAjaxFlash);
  }
})();
