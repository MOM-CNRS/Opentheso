/**
 * OpenTheso — actions de clic (fiche, tableau, lots).
 */
"use strict";

window.appHandleLaterAct = function (e, t, act) {
  if (act === "legal-save") {
    e.preventDefault();
    if (typeof syncAboutEditor === "function") syncAboutEditor();
    const btn = document.getElementById("legalPageForm:legalSave");
    if (btn) window.setTimeout(() => btn.click(), 0);
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
    const msg = t.getAttribute("data-msg") || copied;
    t.title = copied;
    toast(msg, { soft: true });
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
    e.stopPropagation();
    if (t.classList.contains("is-locked") || t.disabled) return;
    pickCreateKind(t.getAttribute("data-kind"));
    return;
  } else if (act === "create-coll-dismiss") {
    e.preventDefault();
    hideCreateCollection();
  } else if (act === "create-coll-modal") {
    return;
  } else if (act === "create-coll-go") {
    e.preventDefault();
    submitCreateCollection();
    return;
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
    e.preventDefault();
    confirmConceptDraftCreate(t.getAttribute("data-kind"));
    return;
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
    e.preventDefault();
    confirmFacetDraftCreate();
    return;
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
  } else if (act === "fct-note-tab") {
    setFacetDraftNoteLang(t.getAttribute("data-lang"));
  } else if (act === "fct-note-cmp") {
    toggleFacetDraftNoteCompare();
  } else if (act === "fct-note-toggle") {
    const btn = $("#fctNoteBtn");
    setFacetDraftNoteLangOpen(btn && !btn.classList.contains("is-open"));
  } else if (act === "fct-note-add-lang") {
    if (t.disabled || t.classList.contains("is-used")) return;
    addFacetDraftNoteLang(t.getAttribute("data-lang"));
  } else if (act === "fct-note-add") {
    addFacetDraftNote();
  } else if (act === "fct-note-add-type") {
    addFacetDraftNoteType(t.getAttribute("data-type"));
  } else if (act === "fct-note-remove") {
    removeFacetDraftNote(Number(t.getAttribute("data-index")));
  } else if (act === "fct-mem-hit") {
    addFacetDraftMem(t.getAttribute("data-id"), t.getAttribute("data-label"));
  } else if (act === "fct-mem-remove") {
    removeFacetDraftMem(t.getAttribute("data-index"));
  } else if (act === "fc-note-add") {
    addFacetCardNote();
  } else if (act === "fc-note-remove") {
    removeFacetCardNote(Number(t.getAttribute("data-index")));
  } else if (act === "fc-note-lang-toggle") {
    setFacetCardNoteLangOpen(Number(t.getAttribute("data-index")));
  } else if (act === "fc-note-lang") {
    setFacetCardNoteLang(Number(t.getAttribute("data-index")), t.getAttribute("data-lang"));
  } else if (act === "fc-mem-hit") {
    addFacetCardMem(t.getAttribute("data-id"), t.getAttribute("data-label"));
  } else if (act === "fc-mem-remove") {
    removeFacetCardMem(t.getAttribute("data-index"));
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
    if (typeof canTreeSelect === "function" && !canTreeSelect()) return;
    const tn = t.closest(".tn");
    if (!tn) return;
    if (typeof rememberTreeFocus === "function") rememberTreeFocus(tn, tn.getAttribute("data-id"));
    const covered = typeof isBranchCovered === "function"
      ? isBranchCovered(tn)
      : t.classList.contains("on");
    selectTreeBranch(tn, !covered);
    if (typeof highlightConcept === "function") highlightConcept(tn.getAttribute("data-id"));
  } else if (act === "sel-id") {
    e.stopPropagation();
    if (typeof canTreeSelect === "function" && !canTreeSelect()) return;
    const id = t.getAttribute("data-id");
    if (id) setSelectedIds([id], !state.selected.has(id));
  } else if (act === "sel-all") {
    e.stopPropagation();
    if (typeof canTreeSelect === "function" && !canTreeSelect()) return;
    const rows = $$("#panelTable tr[data-id]:not(.is-status-off)");
    const ids = rows.map(r => r.getAttribute("data-id"));
    const allOn = ids.length > 0 && ids.every(id => state.selected.has(id));
    setSelectedIds(ids, !allOn);
  } else if (act === "sel-all-visible") {
    e.stopPropagation();
    if (typeof canTreeSelect === "function" && !canTreeSelect()) return;
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
    if (typeof paintStatusForestVisibility === "function") {
      paintStatusForestVisibility(tn.parentElement);
    }
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
  } else if (act === "prop-approve-ask") {
    e.preventDefault();
    openPropDecisionConfirm("approve");
    return;
  } else if (act === "prop-refuse-ask") {
    e.preventDefault();
    openPropDecisionConfirm("refuse");
    return;
  } else if (act === "prop-delete-ask") {
    e.preventDefault();
    showConfirm("#propDeleteConfirm");
    return;
  } else if (act === "prop-decision-dismiss") {
    e.preventDefault();
    hideConfirm("#propDecisionConfirm");
    return;
  } else if (act === "prop-decision-modal") {
    return;
  } else if (act === "prop-delete-dismiss") {
    e.preventDefault();
    hideConfirm("#propDeleteConfirm");
    return;
  } else if (act === "prop-delete-modal") {
    return;
  } else if (act === "prop-send-ask") {
    e.preventDefault();
    if (ensurePropCommentFilled()) showConfirm("#propSendConfirm");
    return;
  } else if (act === "prop-send-dismiss") {
    e.preventDefault();
    hideConfirm("#propSendConfirm");
    return;
  } else if (act === "prop-send-modal") {
    return;
  } else if (act === "fc-del-ask") {
    e.preventDefault();
    showConfirm("#fcDeleteConfirm");
    return;
  } else if (act === "fc-del-dismiss") {
    e.preventDefault();
    hideConfirm("#fcDeleteConfirm");
    return;
  } else if (act === "fc-del-modal") {
    return;
  } else if (act === "fc-del-go") {
    e.preventDefault();
    hideConfirm("#fcDeleteConfirm");
    const btn = document.querySelector("[id$='fcDeleteGo']");
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
  else if (act === "bulk-add-coll") bulkMode("addcoll");
  else if (act === "bulk-move") bulkMode("move");
  else if (act === "bulk-xfer") bulkMode("xfer");
  else if (act === "bulk-status") bulkMode("status");
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
    reopenExportAfterOptionChange();
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
    reopenExportAfterOptionChange();
    closeExportPdfLangPickers();
  } else if (act === "export-run") {
    e.preventDefault();
    startSelectionExport();
  } else if (act === "export-dl") downloadReadyExport();
  else if (act === "export-cancel") cancelSelectionExport(!exportBusy);
  else if (act === "sel-list-open") {
    e.preventDefault();
    openSelectionList(t.closest(".bap-members-list"));
  } else if (act === "sel-list-modal") {
    return;
  } else if (act === "sel-list-dismiss") {
    e.preventDefault();
    closeSelectionList();
  } else if (act === "bulk-back") bulkMode("acts");
  else if (act === "bulk-coll-run") runBulkCollection();
  else if (act === "bulk-coll-dismiss") hideBulkCollConfirm();
  else if (act === "bulk-coll-modal") return;
  else if (act === "bulk-add-coll-run") runBulkAddCollection();
  else if (act === "bulk-add-coll-dismiss") hideBulkAddCollConfirm();
  else if (act === "bulk-add-coll-modal") return;
  else if (act === "bulk-add-coll-pick") {
    e.preventDefault();
    pickAddCollTarget(t.getAttribute("data-id"), t.getAttribute("data-pref"));
  } else if (act === "bulk-add-coll-clear") {
    e.preventDefault();
    clearAddCollTarget();
  } else if (act === "bulk-add-coll-go") {
    e.preventDefault();
    confirmBulkAddCollection();
  }
  else if (act === "bulk-coll-go") {
    e.preventDefault();
    confirmBulkCollection();
    return;
  }
  else if (act === "bulk-move-pick") {
    pickMoveTarget(t.getAttribute("data-id"), t.getAttribute("data-pref"));
  } else if (act === "bulk-move-clear") {
    clearMoveTarget();
  } else if (act === "bulk-move-run") runBulkMove();
  else if (act === "bulk-move-dismiss") hideBulkMoveConfirm();
  else if (act === "bulk-move-modal") return;
  else if (act === "bulk-move-go") {
    e.preventDefault();
    confirmBulkMove();
    return;
  } else if (act === "bulk-xfer-pick") {
    pickXferThesaurus(t.getAttribute("data-id"), t.getAttribute("data-name"));
  } else if (act === "bulk-xfer-clear") {
    resetXfer();
  } else if (act === "bulk-xfer-parent-pick") {
    pickXferParent(t.getAttribute("data-id"), t.getAttribute("data-pref"));
  } else if (act === "bulk-xfer-parent-clear") {
    clearXferParent();
  } else if (act === "bulk-xfer-run") runBulkXfer();
  else if (act === "bulk-xfer-dismiss") hideBulkXferConfirm();
  else if (act === "bulk-xfer-modal") return;
  else if (act === "bulk-xfer-go") {
    e.preventDefault();
    confirmBulkXfer();
    return;
  } else if (act === "bulk-status-pick") {
    pickStatusAction(t.getAttribute("data-status"));
  } else if (act === "bulk-status-run") runBulkStatus();
  else if (act === "bulk-status-dismiss") hideBulkStatusConfirm();
  else if (act === "bulk-status-modal") return;
  else if (act === "bulk-status-go") {
    e.preventDefault();
    confirmBulkStatus();
    return;
  }
};
