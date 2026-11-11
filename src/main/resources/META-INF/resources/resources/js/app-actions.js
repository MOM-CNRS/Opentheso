/**
 * OpenTheso — actions de clic (navigation, session, préférences).
 */
"use strict";

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
    if (!e.target.closest("#fcTrPick") && typeof closeFacetCardTrLang === "function") closeFacetCardTrLang();
    if (!e.target.closest("#cvNotePick") && typeof closeConceptNoteLang === "function") closeConceptNoteLang();
    if (!e.target.closest(".note-menu-w") && typeof closeConceptNoteTypeMenu === "function") closeConceptNoteTypeMenu();
    if (!e.target.closest("#fctTrPick") && typeof closeFacetDraftTrLang === "function") closeFacetDraftTrLang();
    if (!e.target.closest(".draft-note-lang")) closeDraftNoteLangMenu();
    if (!e.target.closest("#fctNotePick") && typeof closeFacetDraftNoteLang === "function") closeFacetDraftNoteLang();
    if (!e.target.closest(".note-menu-w") && typeof closeFacetDraftNoteTypeMenu === "function") closeFacetDraftNoteTypeMenu();
    if (!e.target.closest("#fctMemNew .re-field") && typeof hideFacetDraftMemDrop === "function") hideFacetDraftMemDrop();
    if (!e.target.closest("#fcMemNew .re-field") && typeof hideFacetCardMemDrop === "function") hideFacetCardMemDrop();
    if (!e.target.closest(".fc-note-lang") && typeof closeFacetCardNoteLang === "function") closeFacetCardNoteLang();
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
  if (act !== "fc-tr-toggle" && act !== "fc-tr-add" && typeof closeFacetCardTrLang === "function") closeFacetCardTrLang();
  if (act !== "cv-note-toggle" && act !== "cv-note-add-lang" && typeof closeConceptNoteLang === "function") closeConceptNoteLang();
  if (act !== "cv-note-add" && act !== "cv-note-add-type" && typeof closeConceptNoteTypeMenu === "function") closeConceptNoteTypeMenu();
  if (act !== "fct-tr-toggle" && act !== "fct-tr-add" && typeof closeFacetDraftTrLang === "function") closeFacetDraftTrLang();
  if (act !== "draft-note-lang-toggle" && act !== "draft-note-lang") closeDraftNoteLangMenu();
  if (act !== "fct-note-toggle" && act !== "fct-note-add-lang" && typeof closeFacetDraftNoteLang === "function") closeFacetDraftNoteLang();
  if (act !== "fct-note-add" && act !== "fct-note-add-type" && typeof closeFacetDraftNoteTypeMenu === "function") closeFacetDraftNoteTypeMenu();
  if (act !== "fct-mem-hit" && typeof hideFacetDraftMemDrop === "function") hideFacetDraftMemDrop();
  if (act !== "fc-mem-hit" && typeof hideFacetCardMemDrop === "function") hideFacetCardMemDrop();
  if (act !== "fc-note-lang-toggle" && act !== "fc-note-lang" && typeof closeFacetCardNoteLang === "function") closeFacetCardNoteLang();
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
  if (act === "th-dc-save-ask" || act === "th-dc-delete-ask") {
    e.preventDefault();
    askMetadataConfirm(act === "th-dc-delete-ask" ? "delete" : "save", t);
    return;
  }
  if (act === "th-dc-confirm-dismiss") {
    e.preventDefault();
    metadataConfirmTarget = null;
    if (typeof hideConfirm === "function") hideConfirm("#previewMetadataConfirm");
    return;
  }
  if (act === "th-dc-confirm-modal") return;
  if (act === "th-dc-confirm-go") {
    e.preventDefault();
    confirmMetadataAction();
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
    if (typeof window.sessionStay === "function") window.sessionStay();
    return;
  } else if (act === "session-expire") {
    e.preventDefault();
    if (typeof window.sessionExpire === "function") window.sessionExpire();
    return;
  } else if (act === "session-expire-modal") {
    return;
  } else if (act === "logout-ask") {
    closeThesaurus();
    if (askLeaveThen(() => showConfirm("#logoutConfirm"))) return;
    showConfirm("#logoutConfirm");
  } else if (act === "sync-error-dismiss") {
    e.preventDefault();
    hideConfirm("#syncErrorDialog");
  } else if (act === "sync-error-modal" || act === "sync-done-modal") {
    return;
  } else if (act === "sync-done-dismiss") {
    e.preventDefault();
    hideConfirm("#syncDoneDialog");
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
    if (t.classList.contains("is-off") || t.classList.contains("is-busy")) return;
    const page = document.getElementById("viewSync");
    if (page && page.classList.contains("is-soliciting")) return;
    if (!document.getElementById("syncSolicitGo")) return;
    setSyncSolicitBusy(true);
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
  } else if (act === "fc-tr-ask") {
    askRemoveFacetCardTr(t.getAttribute("data-lang"));
  } else if (act === "fc-tr-keep") {
    keepFacetCardTr();
  } else if (act === "fc-tr-drop") {
    removeFacetCardTr(t.getAttribute("data-lang"));
  } else if (act === "fc-tr-toggle") {
    const btn = $("#fcTrBtn");
    setFacetCardTrLangOpen(btn && !btn.classList.contains("is-open"));
  } else if (act === "fc-tr-add") {
    if (t.disabled || t.classList.contains("is-used")) return;
    addFacetCardTr(t.getAttribute("data-lang"));
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
  } else if (act === "pref-save-ask") {
    showConfirm("#prefSaveConfirm");
  } else if (act === "pref-save-go") {
    e.preventDefault();
    if (t.getAttribute("aria-busy") === "true") return;
    t.setAttribute("aria-busy", "true");
    hideConfirm("#prefSaveConfirm");
    const done = () => t.removeAttribute("aria-busy");
    if (typeof flushUserPreferences !== "function") {
      done();
      if (typeof showPrefSaveToast === "function") showPrefSaveToast(false);
      return;
    }
    flushUserPreferences().then(() => {
      done();
      showPrefSaveToast(true);
    }).catch(() => {
      done();
      showPrefSaveToast(false);
    });
  } else if (act === "pref-save-dismiss") {
    hideConfirm("#prefSaveConfirm");
  } else if (act === "pref-save-modal") {
    return;
  } else if (act === "st-save-dismiss") {
    hideConfirm("#stSaveConfirm");
  } else if (act === "st-save-modal") {
    return;
  } else if (act === "st-leave-dismiss") {
    e.preventDefault();
    dismissSettingsLeave();
  } else if (act === "st-leave-confirm") {
    e.preventDefault();
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
    ["#previewView", "#viewHome", "#viewLive", "#viewConcept", "#viewSettings", "main.content", "main.content .view", ".main"].forEach((sel) => {
      const el = $(sel);
      if (el && typeof el.scrollTo === "function") el.scrollTo({ top: 0, behavior });
    });
    window.scrollTo({ top: 0, behavior });
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
  else if (act === "back-prop-list") {
    e.preventDefault();
    const href = t.getAttribute("href");
    if (href) go(href);
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
  } else if (act === "cand-accept-ask") {
    e.preventDefault();
    if (t.disabled || t.classList.contains("is-off")) return;
    showConfirm("#candAcceptConfirm");
  } else if (act === "cand-accept-dismiss") {
    e.preventDefault();
    hideConfirm("#candAcceptConfirm");
  } else if (act === "cand-accept-modal") {
    return;
  } else if (act === "cand-accept-go") {
    return;
  } else if (act === "cand-reject-ask") {
    e.preventDefault();
    if (t.disabled || t.classList.contains("is-off")) return;
    showConfirm("#candRejectConfirm");
  } else if (act === "cand-reject-dismiss") {
    e.preventDefault();
    hideConfirm("#candRejectConfirm");
  } else if (act === "cand-reject-modal") {
    return;
  } else if (act === "cand-reject-go") {
    return;
  } else if (act === "cand-reopen-ask") {
    e.preventDefault();
    if (t.disabled || t.classList.contains("is-off")) return;
    showConfirm("#candReopenConfirm");
  } else if (act === "cand-reopen-dismiss") {
    e.preventDefault();
    hideConfirm("#candReopenConfirm");
  } else if (act === "cand-reopen-modal") {
    return;
  } else if (act === "cand-export") {
    e.preventDefault();
    if (t.disabled || t.classList.contains("is-off")) return;
    const btn = document.getElementById("candBoardForm:candExportSkos");
    if (btn) btn.click();
  } else if (act === "cand-select") {
    e.preventDefault();
    const id = t.getAttribute("data-id") || "";
    const hidden = document.getElementById("candBoardForm:candSelectId");
    const go = document.getElementById("candBoardForm:candSelectGo");
    if (!hidden || !go || !id) return;
    hidden.value = id;
    go.click();
  } else if (act === "cand-tab") {
    const board = t.closest(".cand-board") || $("#candBoard");
    const tab = t.getAttribute("data-tab") || "attente";
    const previous = board ? board.getAttribute("data-tab") : "";
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
    const tools = document.getElementById("candBoardForm:candBoardTools") || document.getElementById("candBoardTools");
    if (tools) {
      const decide = tools.querySelector(".cand-decide");
      if (decide) decide.hidden = tab !== "attente";
      const reopen = tools.querySelector(".cand-reopen");
      if (reopen) reopen.hidden = tab !== "rejete";
    }
    if (previous !== tab && typeof replayAnim === "function") {
      replayAnim(document.getElementById("candPanes"), "is-tab-in");
    }
    syncCandExport(tab);
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
    if (t.closest("#ac") && typeof openSearchRow === "function") {
      openSearchRow(t);
      return;
    }
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
    if (typeof rememberTreeFocus === "function") rememberTreeFocus(t.closest(".tn"), id);
    if (openLiveDetail(id, nodeType)) {
      state.home = false;
      state.draft = false;
      state.collectionDraft = false;
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
      state.collectionDraft = false;
      state.conceptId = id;
      highlightConcept(id);
      return;
    }
    state.home = false;
    state.draft = false;
    state.collectionDraft = false;
    state.conceptId = id;
    highlightConcept(id);
    paint();
  } else if (act === "open-recent") {
    const id = t.getAttribute("data-id");
    if (!id) return;
    state.home = false;
    if (!openLiveDetail(id, "concept")) {
      go(typeof conceptPageHref === "function"
          ? conceptPageHref("idc=" + encodeURIComponent(id))
          : "index.xhtml?idc=" + encodeURIComponent(id));
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
  } else if (typeof window.appHandleLaterAct === "function") {
    window.appHandleLaterAct(e, t, act);
  }
});
