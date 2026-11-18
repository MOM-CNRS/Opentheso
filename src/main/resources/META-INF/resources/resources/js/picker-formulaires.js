/** Sélecteur — langues principales et import. */
function tpSyncOnePrincipal(hiddenId, rootId) {
  var hidden = document.getElementById(hiddenId);
  var root = document.getElementById(rootId);
  if (!hidden || !root) return;
  var code = hidden.value ? hidden.value.trim().toLowerCase() : '';
  root.querySelectorAll('.tp-lang-multi-opt').forEach(function (opt) {
    var same = !!code && tpLangOptionCode(opt).toLowerCase() === code;
    opt.classList.toggle('is-principal', same);
    if (same) {
      opt.classList.remove('is-on');
      opt.setAttribute('aria-selected', 'false');
      if (root.tpTitles) delete root.tpTitles[tpLangOptionCode(opt)];
    }
  });
  if (typeof root.tpSyncLangs === 'function') root.tpSyncLangs();
}
window.tpSyncPrincipalLanguage = function () {
  tpSyncOnePrincipal('tpCreateSourceLang', 'tpCreateLangsUi');
  tpSyncOnePrincipal('tpEditSourceLang', 'tpEditLangsUi');
};
if (document.getElementById('tpCreateLangsUi') || document.getElementById('tpEditLangsUi')) {
  window.tpSyncPrincipalLanguage();
}
window.tpConfirmCreateOk = function () {
  if (typeof hideConfirm === 'function') {
    hideConfirm('#tpCreateConfirm');
  } else {
    var dlg = document.getElementById('tpCreateConfirm');
    if (dlg) dlg.hidden = true;
  }
  window.tpShowLoading('Création du thésaurus…');
  var go = document.getElementById('tpCreateSubmit');
  if (go) go.click();
  return false;
};
window.tpDismissCreateConfirm = function () {
  if (typeof hideConfirm === 'function') {
    hideConfirm('#tpCreateConfirm');
  } else {
    var dlg = document.getElementById('tpCreateConfirm');
    if (dlg) dlg.hidden = true;
  }
  return false;
};
window.tpShowAddChoose = function () {
  var wrap = document.getElementById('tpAdd');
  var menu = document.getElementById('tpAddMenu');
  var btn = document.getElementById('tpAddBtn');
  if (!wrap || !menu) return false;
  wrap.classList.add('is-open');
  menu.hidden = false;
  menu.removeAttribute('hidden');
  if (btn) btn.setAttribute('aria-expanded', 'true');
  var first = menu.querySelector('.tp-add-opt');
  if (first && first.focus) first.focus();
  return false;
};
window.tpHideAddChoose = function () {
  var wrap = document.getElementById('tpAdd');
  var menu = document.getElementById('tpAddMenu');
  var btn = document.getElementById('tpAddBtn');
  if (wrap) wrap.classList.remove('is-open');
  if (menu) {
    menu.hidden = true;
    menu.setAttribute('hidden', 'hidden');
  }
  if (btn) btn.setAttribute('aria-expanded', 'false');
  return false;
};
window.tpToggleAddChoose = function () {
  var wrap = document.getElementById('tpAdd');
  if (wrap && wrap.classList.contains('is-open')) {
    return window.tpHideAddChoose();
  }
  return window.tpShowAddChoose();
};
window.tpShowDeleteConfirm = function () {
  var dlg = document.getElementById('tpDeleteConfirm');
  if (!dlg || dlg.getAttribute('data-ready') !== 'true') return;
  dlg.classList.remove('is-off');
  dlg.removeAttribute('hidden');
  dlg.hidden = false;
  if (typeof showConfirm === 'function') {
    showConfirm('#tpDeleteConfirm');
  }
};
window.tpHideDeleteConfirm = function () {
  var dlg = document.getElementById('tpDeleteConfirm');
  if (dlg) {
    dlg.hidden = true;
    dlg.setAttribute('hidden', 'hidden');
    dlg.classList.add('is-off');
    dlg.setAttribute('data-ready', 'false');
  }
  if (typeof hideConfirm === 'function') {
    hideConfirm('#tpDeleteConfirm');
  }
};
window.tpCleanupUiLocks = function () {
  if (window._tpLoadingSafety) {
    window.clearTimeout(window._tpLoadingSafety);
    window._tpLoadingSafety = null;
  }
  if (window._tpLeavingSafety) {
    window.clearTimeout(window._tpLeavingSafety);
    window._tpLeavingSafety = null;
  }
  // Ne pas fermer la confirmation de suppression : elle est pilotée par le bean
  // (data-ready) et serait sinon refermée juste après le postback (pageshow).
  window.tpDismissCreateConfirm && window.tpDismissCreateConfirm();
  window.tpHideLoading && window.tpHideLoading();
  document.body.classList.remove('tp-is-loading', 'is-export-lock', 'tp-export-open');
  document.body.removeAttribute('aria-busy');
  var loading = document.getElementById('tpLoading');
  if (loading) {
    loading.hidden = true;
    loading.setAttribute('hidden', 'hidden');
    loading.classList.remove('is-import');
    loading.classList.remove('is-export');
    loading.classList.remove('is-delete');
    loading.setAttribute('aria-busy', 'false');
  }
  document.querySelectorAll('.tp.tp--leaving, .tp--leaving').forEach(function (node) {
    node.classList.remove('tp--leaving');
    node.style.pointerEvents = '';
  });
  document.querySelectorAll('[data-tp-leaving="1"], .is-leaving').forEach(function (node) {
    node.removeAttribute('data-tp-leaving');
    node.classList.remove('is-leaving');
  });
  var createConfirm = document.getElementById('tpCreateConfirm');
  if (createConfirm) {
    createConfirm.hidden = true;
    createConfirm.setAttribute('hidden', 'hidden');
  }
};
window.tpOpenDeleteConfirmIfReady = function () {
  var panel = document.getElementById('tpDeleteConfirm');
  if (!panel || panel.getAttribute('data-ready') !== 'true') return false;
  window.tpShowDeleteConfirm && window.tpShowDeleteConfirm();
  return true;
};
window.tpDismissDeleteConfirm = function () {
  window.tpHideDeleteConfirm && window.tpHideDeleteConfirm();
  var cancel = document.getElementById('tpDeleteCancel');
  if (cancel) cancel.click();
  return false;
};
window.tpShowDeleteProgress = function () {
  var box = document.getElementById('tpLoading');
  if (!box || !window.tpShowLoading) return;
  var nameEl = document.querySelector('#tpDeleteConfirm strong');
  var name = nameEl ? nameEl.textContent.trim() : '';
  var msg = box.getAttribute('data-delete-running') || 'Suppression du thésaurus…';
  if (name) msg = msg + ' « ' + name + ' »';
  window.tpShowLoading(msg, 'delete');
  var detail = document.getElementById('tpProgDetail');
  var hint = box.getAttribute('data-delete-detail') || '';
  if (detail) {
    detail.hidden = !hint;
    detail.textContent = hint;
  }
  var meta = document.getElementById('tpProgMeta');
  if (meta) meta.hidden = true;
  var steps = document.getElementById('tpProgSteps');
  if (steps) steps.hidden = true;
  var fill = document.getElementById('tpProgFill');
  if (fill) {
    fill.classList.add('is-indet');
    fill.style.width = '';
  }
  var pct = document.getElementById('tpProgPct');
  if (pct) pct.textContent = '';
  var cancel = document.getElementById('tpProgCancel');
  if (cancel) cancel.hidden = true;
};
window.tpConfirmDeleteOk = function () {
  // Fermer le calque, afficher la progression, puis lancer le postback.
  window.tpHideDeleteConfirm && window.tpHideDeleteConfirm();
  window.tpShowDeleteProgress && window.tpShowDeleteProgress();
  var go = document.getElementById('tpDeleteSubmit');
  if (go) go.click();
  return false;
};
window.tpAnimateBackToList = function (el, submitEl) {
  if (!el) return true;
  if (el.getAttribute('data-tp-leaving') === '1') return true;
  if (window.exportBusy) return false;
  window.closePickerThesaurusExport && window.closePickerThesaurusExport();
  var root = (el.closest && el.closest('.tp'))
    || document.getElementById('tpImportRoot')
    || document.getElementById('tpExportRoot')
    || document.getElementById('tpRoot');
  if (root) root.classList.add('tp--leaving');
  el.classList.add('is-leaving');
  el.setAttribute('data-tp-leaving', '1');
  var target = submitEl || el;
  if (window._tpLeavingSafety) {
    window.clearTimeout(window._tpLeavingSafety);
  }
  window._tpLeavingSafety = window.setTimeout(function () {
    window.tpCleanupUiLocks && window.tpCleanupUiLocks();
  }, 2500);
  window.setTimeout(function () {
    if (!target || !document.body.contains(target)) {
      window.tpCleanupUiLocks && window.tpCleanupUiLocks();
      return;
    }
    // Déclencher le cancel en postback complet (pas d'ajax sur cette page).
    if (root) root.style.pointerEvents = 'auto';
    try {
      target.click();
    } catch (err) {
      window.tpCleanupUiLocks && window.tpCleanupUiLocks();
    }
  }, 220);
  return false;
};
function tpLangPaint(btn, expanded) {
  btn.setAttribute('aria-expanded', expanded ? 'true' : 'false');
  btn.setAttribute('title', expanded ? 'Réduire la liste des langues' : 'Afficher toutes les langues');
  btn.textContent = expanded ? '−' : (btn.getAttribute('data-more') || '+');
}
function tpLangFinish(wrap, expanded) {
  if (wrap._tpLangTimer) {
    window.clearTimeout(wrap._tpLangTimer);
    wrap._tpLangTimer = 0;
  }
  if (wrap._tpLangEnd) {
    wrap.removeEventListener('transitionend', wrap._tpLangEnd);
    wrap._tpLangEnd = null;
  }
  wrap.style.transition = '';
  wrap.style.height = '';
  wrap.style.overflow = '';
  wrap.classList.remove('is-anim', 'is-folding');
  wrap.classList.toggle('is-expanded', !!expanded);
}
window.tpToggleLangs = function (btn, evt) {
  if (evt) {
    evt.preventDefault();
    evt.stopPropagation();
  }
  if (!btn) return false;
  var wrap = btn.closest('.tp-langs-in');
  if (!wrap) return false;
  var opening = !wrap.classList.contains('is-expanded') || wrap.classList.contains('is-folding');
  var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (reduce) {
    tpLangFinish(wrap, opening);
    tpLangPaint(btn, opening);
    return false;
  }
  if (wrap._tpLangEnd) {
    wrap.removeEventListener('transitionend', wrap._tpLangEnd);
    wrap._tpLangEnd = null;
  }
  if (wrap._tpLangTimer) {
    window.clearTimeout(wrap._tpLangTimer);
    wrap._tpLangTimer = 0;
  }
  var from = wrap.getBoundingClientRect().height;
  wrap._tpLangGen = (wrap._tpLangGen || 0) + 1;
  var gen = wrap._tpLangGen;
  wrap.style.transition = 'none';
  wrap.style.height = 'auto';
  wrap.classList.remove('is-folding');
  wrap.classList.toggle('is-expanded', opening);
  var to = wrap.offsetHeight;
  if (!opening) wrap.classList.add('is-expanded', 'is-folding');
  wrap.style.height = from + 'px';
  wrap.style.overflow = 'hidden';
  wrap.classList.add('is-anim');
  tpLangPaint(btn, opening);
  void wrap.offsetHeight;
  function settle(ev) {
    if (wrap._tpLangGen !== gen) return;
    if (ev && ev.propertyName && ev.propertyName !== 'height') return;
    tpLangFinish(wrap, opening);
  }
  wrap._tpLangEnd = settle;
  wrap.addEventListener('transitionend', settle);
  wrap._tpLangTimer = window.setTimeout(function () { settle({propertyName: 'height'}); }, 420);
  window.requestAnimationFrame(function () {
    if (wrap._tpLangGen !== gen) return;
    wrap.style.transition = 'height .24s cubic-bezier(.22,1,.36,1)';
    wrap.style.height = to + 'px';
  });
  return false;
};
document.addEventListener('click', function (e) {
  var actEl = e.target.closest && e.target.closest('[data-act]');
  var act = actEl ? actEl.getAttribute('data-act') : '';
  if (act === 'tp-langf-toggle') {
    e.preventDefault();
    e.stopPropagation();
    var wrap = actEl.closest && actEl.closest('.tp-langf');
    var wasOpen = wrap && wrap.classList.contains('is-open');
    window.tpLangfCloseAll && window.tpLangfCloseAll();
    if (wrap && !wasOpen) {
      wrap.classList.add('is-open');
      actEl.setAttribute('aria-expanded', 'true');
      var menu = wrap.querySelector('.tp-langf-menu');
      if (menu) menu.hidden = false;
      window.tpLangfPlaceMenu && window.tpLangfPlaceMenu(wrap);
    }
    return;
  }
  if (act === 'tp-langf-pick') {
    e.preventDefault();
    e.stopPropagation();
    window.tpLangfApply && window.tpLangfApply(
      actEl.getAttribute('data-lang') || '',
      actEl.getAttribute('data-flag') || '',
      actEl.getAttribute('data-label') || ''
    );
    window.tpLangfCloseAll && window.tpLangfCloseAll();
    return;
  }
  if (!(e.target.closest && (e.target.closest('.tp-langf') || e.target.closest('.tp-langf-menu')))) {
    window.tpLangfCloseAll && window.tpLangfCloseAll();
  }
  if (act === 'tp-add-open') {
    e.preventDefault();
    e.stopPropagation();
    window.tpToggleAddChoose();
    return;
  }
  if (e.target.closest && e.target.closest('.tp-add-menu')) {
    return;
  }
  if (!e.target.closest || !e.target.closest('.tp-add')) {
    window.tpHideAddChoose();
  }
  if (act === 'tp-create-dismiss') {
    e.preventDefault();
    window.tpDismissCreateConfirm();
    return;
  }
  if (act === 'tp-delete-dismiss') {
    e.preventDefault();
    window.tpDismissDeleteConfirm && window.tpDismissDeleteConfirm();
    return;
  }
  if (act === 'tp-delete-ok') {
    e.preventDefault();
    window.tpConfirmDeleteOk && window.tpConfirmDeleteOk();
    return;
  }
  if (act === 'tp-create-ok') {
    e.preventDefault();
    window.tpConfirmCreateOk();
    return;
  }
  if (act === 'tp-create-modal') {
    e.stopPropagation();
    return;
  }
  if (act === 'tp-delete-modal') {
    e.stopPropagation();
    return;
  }
  var toggle = e.target.closest && e.target.closest('[data-act="tp-lang-toggle"]');
  if (toggle) {
    e.preventDefault();
    e.stopPropagation();
    var open = !toggle.classList.contains('is-open');
    document.querySelectorAll('.tp-lang-btn.is-open').forEach(function (b) {
      b.classList.remove('is-open');
      b.setAttribute('aria-expanded', 'false');
    });
    if (open) {
      toggle.classList.add('is-open');
      toggle.setAttribute('aria-expanded', 'true');
      var projectSearch = toggle.parentElement && toggle.parentElement.querySelector('.tp-project-q');
      if (projectSearch) window.setTimeout(function () { projectSearch.focus(); }, 0);
    }
    return;
  }
  if (e.target.closest && e.target.closest('.tp-lang-menu')) {
    return;
  }
  document.querySelectorAll('.tp-lang-btn.is-open').forEach(function (b) {
    b.classList.remove('is-open');
    b.setAttribute('aria-expanded', 'false');
  });
});
document.addEventListener('keydown', function (e) {
  if (e.key !== 'Escape') return;
  if (document.querySelector('.tp-langf.is-open')) {
    window.tpLangfCloseAll && window.tpLangfCloseAll();
    return;
  }
  var add = document.getElementById('tpAdd');
  if (add && add.classList.contains('is-open')) {
    window.tpHideAddChoose();
    return;
  }
  var del = document.getElementById('tpDeleteConfirm');
  if (del && !del.hidden) {
    window.tpDismissDeleteConfirm && window.tpDismissDeleteConfirm();
    return;
  }
  var dlg = document.getElementById('tpCreateConfirm');
  if (dlg && !dlg.hidden) {
    window.tpDismissCreateConfirm();
  }
});
window.addEventListener('scroll', function () {
  window.tpLangfReposition && window.tpLangfReposition();
}, true);
window.addEventListener('resize', function () {
  window.tpLangfReposition && window.tpLangfReposition();
  window.tpFitColumns && window.tpFitColumns();
});
document.addEventListener('DOMContentLoaded', function () {
  window.tpCleanupUiLocks && window.tpCleanupUiLocks();
  window.tpSyncCreateSubmit && window.tpSyncCreateSubmit();
  window.tpSyncImportSubmit && window.tpSyncImportSubmit();
  window.tpBindImportDrop && window.tpBindImportDrop();
  window.tpApplyPlaceholders && window.tpApplyPlaceholders();
  window.tpLoadColumnPrefs && window.tpLoadColumnPrefs();
  window.tpBindColumnMenu && window.tpBindColumnMenu();
  if (window.ResizeObserver) {
    var tpWrap = document.querySelector('.tp-body > .tp-table-wrap');
    if (tpWrap && !tpWrap.dataset.tpFitBound) {
      tpWrap.dataset.tpFitBound = '1';
      var tpFitLock = false;
      new ResizeObserver(function () {
        if (tpFitLock) return;
        tpFitLock = true;
        window.tpFitColumns && window.tpFitColumns();
        tpFitLock = false;
      }).observe(tpWrap);
    }
  }
  window.tpBindRowKeyboard && window.tpBindRowKeyboard();
  window.tpBindTabs && window.tpBindTabs();
  window.tpBindImportFormats && window.tpBindImportFormats();
  window.tpPolishImportPage && window.tpPolishImportPage();
  window.tpSyncSearchClear && window.tpSyncSearchClear();
  window.tpConsumeListFlash && window.tpConsumeListFlash();
  window.tpMountExportPage && window.tpMountExportPage();
  window.tpBindLangMultis && window.tpBindLangMultis();
  window.tpOpenDeleteConfirmIfReady && window.tpOpenDeleteConfirmIfReady();
  var loadingEl = document.getElementById('tpLoading');
  if (loadingEl && !loadingEl.dataset.tpDismissBound) {
    loadingEl.dataset.tpDismissBound = '1';
    loadingEl.addEventListener('click', function (ev) {
      if (loadingEl.classList.contains('is-import')) return;
      if (ev.target === loadingEl) {
        window.tpCleanupUiLocks && window.tpCleanupUiLocks();
      }
    });
  }
});
window.addEventListener('pageshow', function () {
  window.tpCleanupUiLocks && window.tpCleanupUiLocks();
  window.tpOpenDeleteConfirmIfReady && window.tpOpenDeleteConfirmIfReady();
});
window.tpApplyPlaceholders = function () {
  document.querySelectorAll('input[data-ph], textarea[data-ph]').forEach(function (el) {
    var ph = el.getAttribute('data-ph');
    if (ph) el.setAttribute('placeholder', ph);
  });
};
window.tpBindImportDrop = function () {
  document.querySelectorAll('[data-tp-drop="1"]').forEach(function (zone) {
    var wrap = zone.closest('.tp-import-dropzone');
    var input = wrap && wrap.querySelector('input[type="file"]');
    if (!input) {
      var forId = zone.getAttribute('for');
      input = forId ? document.getElementById(forId) : null;
    }
    if (!input) return;
    var accept = wrap && wrap.getAttribute('data-accept');
    if (accept) input.setAttribute('accept', accept);
    if (input.dataset.tpFileBound !== '1') {
      input.dataset.tpFileBound = '1';
      input.addEventListener('change', function () {
        if (!input.files || !input.files.length) return;
        var file = input.files[0];
        var formatEl = document.getElementById('tpImportFormat');
        var current = formatEl && formatEl.value ? formatEl.value : 'skos';
        var guessed = window.tpGuessImportFormat && window.tpGuessImportFormat(file.name, current);
        if (guessed && guessed !== current) {
          var bar = document.getElementById('tpImportFormats');
          var fmtBtn = bar && bar.querySelector('.tp-import-fmt[data-tp-format="' + guessed + '"]');
          if (fmtBtn) {
            window.tpSelectImportFormat(fmtBtn, { keepFile: true, force: true });
          }
        }
        window.tpOptimisticImportFile && window.tpOptimisticImportFile(file);
        var commit = document.getElementById('tpImportFileCommit');
        if (commit) commit.click();
      });
    }
    if (zone.dataset.dropBound === '1') return;
    zone.dataset.dropBound = '1';
    ['dragenter', 'dragover'].forEach(function (ev) {
      zone.addEventListener(ev, function (e) {
        e.preventDefault();
        e.stopPropagation();
        zone.classList.add('is-drag');
      });
    });
    ['dragleave', 'drop'].forEach(function (ev) {
      zone.addEventListener(ev, function (e) {
        e.preventDefault();
        e.stopPropagation();
        zone.classList.remove('is-drag');
      });
    });
    zone.addEventListener('drop', function (e) {
      var files = e.dataTransfer && e.dataTransfer.files;
      if (!files || !files.length) return;
      try {
        var dt = new DataTransfer();
        dt.items.add(files[0]);
        input.files = dt.files;
      } catch (err) {
        return;
      }
      input.dispatchEvent(new Event('change', { bubbles: true }));
    });
  });
};
