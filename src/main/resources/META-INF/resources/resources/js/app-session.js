/**
 * OpenTheso — menus, session, connexion et publication.
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

function setThesaurusMenu(open) {
  const nav = document.getElementById("navThesaurus");
  const btn = document.getElementById("thesaurusMenuBtn");
  const narrow = !!(window.matchMedia && window.matchMedia("(max-width: 640px)").matches);
  const on = !!open && narrow;
  if (nav) nav.classList.toggle("is-mobile-open", on);
  if (btn) {
    btn.setAttribute("aria-expanded", on ? "true" : "false");
    const label = btn.getAttribute(on ? "data-close" : "data-open");
    if (label) {
      btn.setAttribute("aria-label", label);
      btn.setAttribute("title", label);
    }
  }
  if (!on) closeThesaurus();
}
window.setThesaurusMenu = setThesaurusMenu;

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
function onLegalPageSave(data) {
  if (!data) return;
  if (data.status === "begin" && typeof syncAboutEditor === "function") syncAboutEditor();
  if (data.status === "success" && typeof showAboutSaveToast === "function") {
    requestAnimationFrame(showAboutSaveToast);
  }
}

function onCandDecisionAjax(data) {
  if (!data || data.status !== "begin") return;
  if (typeof hideConfirm === "function") {
    hideConfirm("#candAcceptConfirm");
    hideConfirm("#candRejectConfirm");
    hideConfirm("#candReopenConfirm");
  }
}

function showCandBoardNotice() {
  const n = document.getElementById("candBoardNotice");
  if (!n || typeof toast !== "function") return;
  const msg = String(n.getAttribute("data-msg") || "").trim();
  const token = n.getAttribute("data-token") || "";
  if (!msg || !token) return;
  try {
    if (sessionStorage.getItem("candBoardNotice") === token) return;
    sessionStorage.setItem("candBoardNotice", token);
  } catch (e) {
    /* navigation privée : la notification s'affiche quand même */
  }
  toast(msg, { error: n.getAttribute("data-error") === "true" });
}
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", showCandBoardNotice);
} else {
  showCandBoardNotice();
}

function syncCandExport(tab) {
  const btn = document.getElementById("candExportBtn");
  if (!btn) return;
  const pane = document.querySelector('#candBoard .cand-pane[data-tab="' + tab + '"]');
  const empty = !pane || !pane.querySelector(".cand-line");
  btn.disabled = empty;
  btn.classList.toggle("is-off", empty);
}

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

function onPropAjaxBusy(data, selector) {
  const btns = document.querySelectorAll(selector);
  if (data.status === "begin") {
    if (typeof hideConfirm === "function") {
      hideConfirm("#propDecisionConfirm");
      hideConfirm("#propDeleteConfirm");
      hideConfirm("#propSendConfirm");
    }
    btns.forEach((btn) => btn.classList.add("is-busy"));
    return;
  }
  if (data.status === "success" || data.status === "complete") {
    btns.forEach((btn) => btn.classList.remove("is-busy", "is-click"));
  }
}

function onPropReview(data) {
  onPropAjaxBusy(data, "#propDecisionConfirm .abt-save, #propDeleteConfirm .abt-save");
}
window.onPropReview = onPropReview;

function onPropSend(data) {
  onPropAjaxBusy(data, "#propSendConfirm .abt-save, [data-act='prop-send-ask']");
}
window.onPropSend = onPropSend;

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
  if (e.key === "Enter" && e.target && e.target.id === "fcMemQ") {
    e.preventDefault();
    if (typeof addFacetCardMem === "function" && fcMemState && fcMemState.hits && fcMemState.hits[0]) {
      addFacetCardMem(fcMemState.hits[0].id, fcMemState.hits[0].label);
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
  if (e.key === "Enter" && !e.shiftKey && !e.altKey && !e.ctrlKey && !e.metaKey) {
    const panel = $("#viewConceptDraft");
    const target = e.target;
    if (panel && panel.classList.contains("is-on") && target && panel.contains(target)) {
      const tag = String(target.tagName || "").toLowerCase();
      const type = String(target.type || "").toLowerCase();
      const leave = $("#cptLeaveConfirm");
      const chain = panel.querySelector("[data-act='cpt-create-ask'][data-kind='chain']");
      const multiline = tag === "textarea" || target.isContentEditable;
      const control = tag === "button" || tag === "a" || tag === "select"
        || type === "button" || type === "submit" || type === "checkbox" || type === "radio";
      if (!multiline && !control && chain && !(leave && !leave.hidden)
          && typeof requestConceptDraftCreate === "function") {
        e.preventDefault();
        requestConceptDraftCreate("chain");
        return;
      }
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
function syncNoticeText(selector) {
  const box = document.getElementById("messageIndex");
  if (!box) return "";
  const parts = [];
  box.querySelectorAll(selector).forEach((node) => {
    const text = (node.textContent || "").replace(/\s+/g, " ").trim();
    if (text) parts.push(text);
    node.hidden = true;
  });
  return parts.join("\n");
}
function showSyncDialog(selector, textId, dialogId) {
  const msg = syncNoticeText(selector);
  const text = document.getElementById(textId);
  if (!msg || !text) return false;
  text.textContent = msg;
  showConfirm(dialogId);
  return true;
}
function showSyncErrorIfAny() {
  return showSyncDialog(".is-error", "syncErrorText", "#syncErrorDialog");
}
function showSyncDoneIfAny() {
  return showSyncDialog(".st-corpus-msg:not(.is-error)", "syncDoneText", "#syncDoneDialog");
}
window.onPreviewSyncNotice = function (data) {
  if (data.status === "success") showSyncErrorIfAny();
};
window.onPreviewSyncPoll = function (data) {
  if (data.status === "success") {
    if (!showSyncErrorIfAny()) showSyncDoneIfAny();
    syncThesaurusPollTick();
  }
};
function setSyncSolicitBusy(on) {
  const page = document.getElementById("viewSync");
  const overlay = document.getElementById("syncSolicitBusy");
  const btn = document.querySelector("[data-act='sync-solicit']");
  if (page) {
    page.classList.toggle("is-soliciting", !!on);
    page.setAttribute("aria-busy", on ? "true" : "false");
  }
  if (overlay) overlay.hidden = !on;
  if (btn) {
    btn.classList.toggle("is-busy", !!on);
    btn.setAttribute("aria-busy", on ? "true" : "false");
    btn.setAttribute("aria-disabled", on ? "true" : (btn.classList.contains("is-off") ? "true" : "false"));
  }
}
window.onPreviewSyncSolicit = function (data) {
  if (data.status === "begin") setSyncSolicitBusy(true);
  if (data.status === "success") {
    setSyncSolicitBusy(false);
    showSyncErrorIfAny();
  }
  if (data.status === "complete") {
    window.setTimeout(() => setSyncSolicitBusy(false), 30);
  }
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
    if (typeof window.sessionStay === "function") window.sessionStay();
    e.preventDefault();
    return;
  }
  const overlay = $("#cblockOverlay");
  if (overlay && !overlay.hidden) {
    closeConceptBlockOverlay();
    e.preventDefault();
    return;
  }
  if (hideConfirm("#syncErrorDialog") || hideConfirm("#syncDoneDialog")) {
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
      || hideConfirm("#prefSaveConfirm")
      || hideConfirm("#previewMetadataConfirm")
      || hideConfirm("#previewCorpusCreateConfirm") || hideConfirm("#stLeaveConfirm")
      || hideConfirm("#alignDeleteConfirm") || hideConfirm("#alignReplaceConfirm")
      || hideConfirm("#candReactivateConfirm") || hideConfirm("#candDeleteConfirm")
      || hideConfirm("#candAcceptConfirm") || hideConfirm("#candRejectConfirm")
      || hideConfirm("#candReopenConfirm")
      || hideConfirm("#fcDeleteConfirm")
      || hideConfirm("#propDecisionConfirm") || hideConfirm("#propDeleteConfirm") || hideConfirm("#propSendConfirm")
      || hideConfirm("#cptCreateConfirm") || hideConfirm("#cptLeaveConfirm")
      || hideConfirm("#fctCreateConfirm") || hideConfirm("#fctLeaveConfirm")
      || hideConfirm("#selListModal")
      || hideConfirm("#bulkCollConfirm") || hideConfirm("#bulkAddCollConfirm") || hideConfirm("#bulkMoveConfirm")
      || hideConfirm("#bulkXferConfirm") || hideConfirm("#bulkStatusConfirm")) {
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
  } else if (e.target && e.target.closest && e.target.closest("#fcTrEditor") && e.target.getAttribute("data-lang")) {
    setFacetCardTrValue(e.target.getAttribute("data-lang"), e.target.value);
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
  if (e.target && e.target.id === "fcMemQ" && typeof scheduleFacetCardMemSearch === "function") scheduleFacetCardMemSearch();
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
  } else if (e.target && e.target.getAttribute && e.target.getAttribute("data-fc-note")) {
    setFacetCardNoteField(Number(e.target.getAttribute("data-index")), e.target.getAttribute("data-fc-note"), e.target.value);
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
  } else if (e.target && e.target.getAttribute && e.target.getAttribute("data-fc-note") === "type") {
    setFacetCardNoteField(Number(e.target.getAttribute("data-index")), "type", e.target.value);
    if (typeof paintFacetCardNotes === "function") paintFacetCardNotes();
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
  if (e.target.closest("[id$='fcTrEditSave']") && typeof syncFacetCardTrHidden === "function") {
    syncFacetCardTrHidden();
  }
  if (e.target.closest("[id$='fcMemEditSave']") && typeof syncFacetCardMemHidden === "function") {
    syncFacetCardMemHidden();
  }
  if (e.target.closest("[id$='fcNoteEditSave']") && typeof syncFacetCardNotesHidden === "function") {
    syncFacetCardNotesHidden();
  }
  if (e.target.closest("[id$='resEditSave']") && typeof syncConceptResHidden === "function") {
    syncConceptResHidden();
  }
  if (e.target.closest("[id$='relEditSave']") && typeof syncCvRelHidden === "function") {
    syncCvRelHidden();
  }
}, true);
