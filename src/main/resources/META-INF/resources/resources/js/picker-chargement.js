/** Sélecteur — chargement et colonnes. */
window.tpShowLoading = function (msg, mode) {
  var el = document.getElementById('tpLoading');
  if (!el) return true;
  var asExport = mode === 'export';
  var asDelete = mode === 'delete';
  var asRich = mode === true || mode === 'import' || asExport || asDelete;
  el.classList.toggle('is-import', asRich && !asExport && !asDelete);
  el.classList.toggle('is-export', asExport);
  el.classList.toggle('is-delete', asDelete);
  if (asRich) {
    window.tpApplyProgressSteps(asExport ? 'export' : 'import');
  }
  var title = document.getElementById('tpProgTitle');
  var simpleTxt = el.querySelector('.tp-loading-txt');
  if (asRich) {
    if (title && msg) title.textContent = msg;
  } else if (simpleTxt && msg) {
    simpleTxt.textContent = msg;
  } else if (simpleTxt && !msg) {
    simpleTxt.textContent = 'Ouverture du thésaurus…';
  }
  el.hidden = false;
  el.removeAttribute('hidden');
  el.setAttribute('aria-busy', 'true');
  document.body.classList.add('tp-is-loading');
  if (window._tpLoadingSafety) {
    window.clearTimeout(window._tpLoadingSafety);
  }
  // Filet de sécurité : ne jamais laisser l'overlay bloquer l'UI.
  window._tpLoadingSafety = window.setTimeout(function () {
    window.tpCleanupUiLocks && window.tpCleanupUiLocks();
  }, asRich ? 900000 : 8000);
  return true;
};
window.tpHideLoading = function () {
  window.tpStopImportProgress && window.tpStopImportProgress();
  window._tpProgShown = 0;
  if (window._tpLoadingSafety) {
    window.clearTimeout(window._tpLoadingSafety);
    window._tpLoadingSafety = null;
  }
  var el = document.getElementById('tpLoading');
  if (el) {
    el.hidden = true;
    el.setAttribute('hidden', 'hidden');
    el.setAttribute('aria-busy', 'false');
    el.classList.remove('is-import');
    el.classList.remove('is-export');
    el.classList.remove('is-delete');
    window.tpApplyProgressSteps('import');
  }
  document.body.classList.remove('tp-is-loading');
};
window.tpFmtTpl = function (tpl, a, b) {
  if (!tpl) return '';
  return String(tpl).replace('{0}', a == null ? '' : String(a)).replace('{1}', b == null ? '' : String(b));
};
window.tpFmtEta = function (box, etaSeconds) {
  if (etaSeconds == null || etaSeconds < 0) return '';
  if (etaSeconds < 60) {
    return window.tpFmtTpl(box.getAttribute('data-eta-tpl'),
      window.tpFmtTpl(box.getAttribute('data-eta-sec-tpl'), Math.max(1, Math.round(etaSeconds))));
  }
  var mins = Math.max(1, Math.round(etaSeconds / 60));
  return window.tpFmtTpl(box.getAttribute('data-eta-tpl'),
    window.tpFmtTpl(box.getAttribute('data-eta-min-tpl'), mins));
};
window.tpFmtRate = function (box, rate) {
  if (!rate || rate <= 0) return '';
  var shown = rate >= 10 ? Math.round(rate) : (Math.round(rate * 10) / 10);
  return window.tpFmtTpl(box.getAttribute('data-rate-tpl'), shown);
};
window.tpPaintImportProgress = function (pct, step, message, done, total, etaSeconds, ratePerSec) {
  var fill = document.getElementById('tpProgFill');
  var track = document.getElementById('tpProgTrack');
  var pctEl = document.getElementById('tpProgPct');
  var title = document.getElementById('tpProgTitle');
  var detail = document.getElementById('tpProgDetail');
  var meta = document.getElementById('tpProgMeta');
  var countEl = document.getElementById('tpProgCount');
  var etaEl = document.getElementById('tpProgEta');
  var rateEl = document.getElementById('tpProgRate');
  var box = document.getElementById('tpLoading');
  var clamped = Math.max(0, Math.min(100, Number(pct) || 0));
  if (window._tpProgShown == null || clamped >= window._tpProgShown) {
    window._tpProgShown = clamped;
  } else {
    clamped = window._tpProgShown;
  }
  if (fill) fill.style.width = clamped + '%';
  if (track) track.setAttribute('aria-valuenow', String(Math.round(clamped)));
  if (pctEl) pctEl.textContent = Math.round(clamped) + '%';
  if (title && message) title.textContent = message;
  var hasCount = total > 0 && done >= 0;
  var etaTxt = box ? window.tpFmtEta(box, etaSeconds) : '';
  var rateTxt = box ? window.tpFmtRate(box, ratePerSec) : '';
  if (meta && countEl) {
    if (hasCount) {
      countEl.textContent = window.tpFmtTpl(
        box.getAttribute('data-active-meta-tpl') || box.getAttribute('data-meta-tpl'),
        done,
        total
      );
      if (etaEl) {
        etaEl.textContent = etaTxt;
        etaEl.hidden = !etaTxt;
      }
      if (rateEl) {
        rateEl.textContent = rateTxt;
        rateEl.hidden = !rateTxt;
      }
      meta.querySelectorAll('.tp-prog-sep').forEach(function (sep, idx) {
        if (idx === 0) sep.hidden = !etaTxt && !rateTxt;
        if (idx === 1) sep.hidden = !etaTxt || !rateTxt;
      });
      meta.hidden = false;
      if (detail) {
        detail.textContent = '';
        detail.hidden = true;
      }
    } else {
      meta.hidden = true;
      if (detail) {
        detail.textContent = message && step >= 3 ? message : '';
        detail.hidden = !detail.textContent;
      }
    }
  } else if (detail) {
    if (hasCount) {
      detail.textContent = done + ' / ' + total + (etaTxt ? ' · ' + etaTxt : '') + (rateTxt ? ' · ' + rateTxt : '');
      detail.hidden = false;
    } else {
      detail.textContent = '';
      detail.hidden = true;
    }
  }
  if (box) {
    box.querySelectorAll('.tp-prog-steps li').forEach(function (li) {
      var s = Number(li.getAttribute('data-step'));
      li.classList.toggle('is-done', s < step || clamped >= 100);
      li.classList.toggle('is-on', s === step && clamped < 100);
    });
  }
};
window.tpSetImportControls = function (running, phase) {
  var cancel = document.getElementById('tpProgCancel');
  var report = document.getElementById('tpProgReport');
  if (cancel) {
    var cancelling = phase === 'cancelling' || phase === 'cancelled';
    cancel.hidden = !running;
    cancel.disabled = cancelling;
    if (!running) {
      cancel.textContent = cancel.getAttribute('data-label') || cancel.textContent;
    } else if (cancelling) {
      cancel.textContent = cancel.getAttribute('data-cancelling') || cancel.textContent;
    }
  }
  if (report) {
    report.hidden = !!running;
  }
};
window.tpBindImportCancel = function () {
  var btn = document.getElementById('tpProgCancel');
  if (!btn || btn.getAttribute('data-bound') === '1') return;
  btn.setAttribute('data-bound', '1');
  if (!btn.getAttribute('data-label')) {
    btn.setAttribute('data-label', btn.textContent);
  }
  btn.addEventListener('click', function () {
    var url = btn.getAttribute('data-cancel-url');
    if (!url || btn.disabled) return;
    btn.disabled = true;
    btn.textContent = btn.getAttribute('data-cancelling') || btn.textContent;
    fetch(url, { method: 'POST', credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (!data || !data.accepted) {
          btn.disabled = false;
          btn.textContent = btn.getAttribute('data-label') || btn.textContent;
        }
      })
      .catch(function () {
        btn.disabled = false;
        btn.textContent = btn.getAttribute('data-label') || btn.textContent;
      });
  });
};
window.tpStartImportProgress = function () {
  window.tpStopImportProgress && window.tpStopImportProgress();
  window._tpProgShown = 0;
  var box = document.getElementById('tpLoading');
  if (!box) return;
  window.tpBindImportCancel && window.tpBindImportCancel();
  var cancel = document.getElementById('tpProgCancel');
  if (cancel) {
    cancel.disabled = false;
    cancel.textContent = cancel.getAttribute('data-label') || cancel.textContent;
  }
  window.tpSetImportControls(true, 'prepare');
  var url = box.getAttribute('data-progress-url');
  window.tpPaintImportProgress(2, 0, box.getAttribute('data-step-prepare') || 'Préparation…', 0, 0, -1, 0);
  if (!url) return;
  window._tpImportPoll = window.setInterval(function () {
    fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (!data) return;
        window.tpPaintImportProgress(
          data.percent,
          data.step,
          data.message || '',
          data.done,
          data.total,
          data.etaSeconds,
          data.ratePerSec
        );
        window.tpSetImportControls(!!data.running, data.phase || '');
        if (!data.running && (data.percent >= 100 || data.error || data.phase === 'done' || data.phase === 'error' || data.phase === 'cancelled')) {
          window.tpStopImportProgress && window.tpStopImportProgress();
          window.tpSetImportControls(false, data.phase || '');
        }
      })
      .catch(function () { /* ignore transient poll errors */ });
  }, 400);
};
window.tpStopImportProgress = function () {
  if (window._tpImportPoll) {
    clearInterval(window._tpImportPoll);
    window._tpImportPoll = 0;
  }
};
window.tpPrepareImportSubmit = function (btn) {
  window.tpSyncImportSubmit && window.tpSyncImportSubmit();
  if (!btn || btn.classList.contains('is-disabled')) return false;
  var file = document.getElementById('tpImportFile');
  if (file) file.disabled = true;
  window.tpShowLoading(btn.getAttribute('data-loading-msg') || 'Import…', true);
  window.tpStartImportProgress();
  return true;
};
window.tpImportAnalyzingMsg = function () {
  var root = document.getElementById('tpImportRoot');
  return (root && root.getAttribute('data-msg-analyzing')) || 'Analyse du fichier…';
};
window.tpBeginImportAnalyze = function () {
  try { sessionStorage.setItem('tp.import.focus', '1'); } catch (err) { /* ignore */ }
  window.tpShowLoading(window.tpImportAnalyzingMsg(), false);
  return true;
};
window.tpGuessImportFormat = function (fileName, current) {
  var n = String(fileName || '').toLowerCase();
  if (/\.(csv|tsv|txt)$/.test(n)) {
    return current === 'csvStructured' ? 'csvStructured' : 'csv';
  }
  if (/\.(rdf|xml|ttl|json|jsonld|owl)$/.test(n)) {
    return 'skos';
  }
  return null;
};
window.tpOptimisticImportFile = function (file) {
  var zone = document.getElementById('tpImportFileBlock');
  if (!zone || !file) return;
  zone.classList.add('has-file', 'is-pending');
  zone.classList.remove('is-analyzed', 'needs-reanalyze');
  var n = zone.querySelector('.tp-drop-file-n');
  var m = zone.querySelector('.tp-drop-file-m');
  var root = document.getElementById('tpImportRoot');
  if (n) n.textContent = file.name;
  if (m) m.textContent = (root && root.getAttribute('data-msg-pending')) || 'Analyse en cours…';
  if (root) {
    root.setAttribute('data-has-upload', 'true');
    root.setAttribute('data-load-done', 'false');
  }
  window.tpSyncImportSubmit && window.tpSyncImportSubmit();
};
window.tpMarkImportNeedsReanalyze = function () {
  var zone = document.getElementById('tpImportFileBlock');
  var root = document.getElementById('tpImportRoot');
  if (!zone || !zone.classList.contains('has-file')) return;
  if (!root || root.getAttribute('data-load-done') !== 'true') return;
  zone.classList.add('needs-reanalyze');
  var btn = document.getElementById('tpImportAnalyze');
  if (btn) btn.classList.add('is-attention');
};
window.tpSyncImportSubmit = function () {
  var btn = document.getElementById('tpImportGo');
  var root = document.getElementById('tpImportRoot');
  if (!btn || !root) return;
  var formatEl = document.getElementById('tpImportFormat');
  var format = formatEl && formatEl.value ? formatEl.value : 'skos';
  var zone = document.getElementById('tpImportFileBlock');
  var hasFile = (root.getAttribute('data-has-upload') === 'true')
    || !!(zone && zone.classList.contains('has-file'));
  var loadDone = root.getAttribute('data-load-done') === 'true';
  var preview = document.getElementById('tpImportPreview');
  if (preview && preview.querySelector('.tp-import-stat')) {
    loadDone = true;
  }
  var ok = hasFile && loadDone;
  if (format === 'csv' || format === 'csvStructured') {
    var name = document.getElementById('tpImportName');
    ok = ok && !!(name && name.value && name.value.trim());
  }
  btn.classList.toggle('is-disabled', !ok);
  btn.classList.toggle('is-ready-pulse', ok);
  btn.setAttribute('aria-disabled', ok ? 'false' : 'true');
};
window.tpPolishImportPage = function () {
  var root = document.getElementById('tpImportRoot');
  if (!root) return;
  window.tpSyncImportSubmit && window.tpSyncImportSubmit();
  var more = document.getElementById('tpImportMore');
  if (more && more.classList.contains('is-open')) {
    more.open = true;
  }
  var shouldFocus = false;
  try {
    shouldFocus = sessionStorage.getItem('tp.import.focus') === '1';
    if (shouldFocus) sessionStorage.removeItem('tp.import.focus');
  } catch (err) { /* ignore */ }
  if (root.getAttribute('data-load-done') === 'true') {
    var preview = document.getElementById('tpImportPreview');
    if (preview) {
      preview.classList.add('is-ready');
      if (shouldFocus) {
        window.setTimeout(function () {
          preview.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }, 80);
      }
    }
    var formatEl = document.getElementById('tpImportFormat');
    var format = formatEl && formatEl.value ? formatEl.value : 'skos';
    var name = document.getElementById('tpImportName');
    if (shouldFocus && name && (format === 'csv' || format === 'csvStructured') && !String(name.value || '').trim()) {
      window.setTimeout(function () { name.focus(); }, 120);
    }
  }
  var alert = root.querySelector('.tp-form-alert');
  if (alert) alert.classList.add('is-enter');
};
window.tpMountExportPage = function () {
  var root = document.getElementById('tpExportRoot');
  if (!root) {
    window.closePickerThesaurusExport && window.closePickerThesaurusExport();
    return;
  }
  window.openPickerThesaurusExport && window.openPickerThesaurusExport(
    root.getAttribute('data-thesaurus-id'),
    root.getAttribute('data-thesaurus-title'),
    root.getAttribute('data-concept-count')
  );
};
window.TP_COLS_KEY = 'opentheso.tp.cols.v3';
window.tpReadColumnPrefs = function () {
  try {
    var raw = localStorage.getItem(window.TP_COLS_KEY);
    if (!raw) return null;
    var parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object') return null;
    return {
      order: Array.isArray(parsed.order) ? parsed.order : [],
      on: parsed.on && typeof parsed.on === 'object' ? parsed.on : {}
    };
  } catch (err) {
    return null;
  }
};
window.tpWriteColumnPrefs = function (prefs) {
  try {
    localStorage.setItem(window.TP_COLS_KEY, JSON.stringify(prefs || { order: [], on: {} }));
  } catch (err) { /* ignore quota */ }
};
window.tpCollectColumnPrefs = function () {
  var order = [];
  var on = {};
  document.querySelectorAll('.tp-cols-row[data-col-key]').forEach(function (row) {
    var key = row.getAttribute('data-col-key');
    if (!key) return;
    order.push(key);
    var cb = row.querySelector('input[type="checkbox"]');
    on[key] = cb ? !!cb.checked : true;
  });
  if (!order.length) {
    document.querySelectorAll('.tp-table thead tr:first-child th[data-col]').forEach(function (th) {
      var key = th.getAttribute('data-col');
      if (!key) return;
      order.push(key);
      on[key] = !th.classList.contains('is-col-off');
    });
  }
  return { order: order, on: on };
};
window.tpApplyColumnVisibility = function (prefs) {
  if (!prefs || !prefs.on) return;
  Object.keys(prefs.on).forEach(function (key) {
    var show = !!prefs.on[key];
    document.querySelectorAll('.tp-table [data-col="' + key + '"]').forEach(function (el) {
      el.classList.toggle('is-col-off', !show);
    });
    document.querySelectorAll('.tp-cols-row[data-col-key="' + key + '"] input[type="checkbox"]').forEach(function (cb) {
      if (cb.disabled) return;
      cb.checked = show;
    });
  });
};
