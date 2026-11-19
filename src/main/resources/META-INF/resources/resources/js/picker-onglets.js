/** Sélecteur — onglets, création et édition. */
window.tpMoveTabGlider = function (tabs, tab, animate) {
  var glider = window.tpEnsureTabGlider(tabs);
  var metrics = window.tpTabGliderMetrics(tabs, tab);
  if (!glider || !metrics) return;
  if (!animate) {
    glider.style.transition = 'none';
  } else {
    glider.style.transition = '';
  }
  glider.style.width = metrics.w + 'px';
  glider.style.height = metrics.h + 'px';
  glider.style.transform = 'translate3d(' + metrics.x + 'px,' + metrics.y + 'px,0)';
  if (!animate) {
    void glider.offsetWidth;
    glider.style.transition = '';
  }
  glider.classList.add('is-ready');
};
window.tpRowMatchesTab = function (row, tab) {
  if (!row) return false;
  if (!tab || tab === 'all') return true;
  if (tab === 'member') return row.getAttribute('data-tp-member') === 'true';
  if (tab === 'public') return row.getAttribute('data-tp-public') === 'true';
  return true;
};
window.tpApplyTabFilter = function (tab) {
  window.tpApplyListFilters && window.tpApplyListFilters();
};
window.tpSelectTab = function (tabEl) {
  if (!tabEl) return false;
  var tabs = tabEl.closest('.tp-tabs');
  if (!tabs || tabs.classList.contains('is-guest')) return false;
  var tab = tabEl.getAttribute('data-tp-tab') || 'all';
  var current = tabs.querySelector('.tp-tab.is-on');
  if (current === tabEl) return false;
  var allTabs = Array.prototype.slice.call(tabs.querySelectorAll('.tp-tab[data-tp-tab]'));
  var fromIdx = current ? allTabs.indexOf(current) : 0;
  var toIdx = allTabs.indexOf(tabEl);
  var dir = toIdx >= fromIdx ? 'right' : 'left';
  allTabs.forEach(function (el) { el.classList.remove('is-on'); });
  tabEl.classList.add('is-on');
  window.tpMoveTabGlider(tabs, tabEl, true);
  var hidden = document.getElementById('tpActiveTab');
  if (hidden) hidden.value = tab;
  window.tpApplyTabFilter(tab);
  var wrap = document.getElementById('tpTableWrap');
  if (wrap) {
    wrap.classList.remove('is-tab-switch', 'is-tab-left', 'is-tab-right');
    void wrap.offsetWidth;
    wrap.classList.add('is-tab-switch', dir === 'right' ? 'is-tab-right' : 'is-tab-left');
  }
  return false;
};
window.tpBindTabs = function () {
  var tabs = document.getElementById('tpTabs') || document.querySelector('.tp-tabs:not(.is-guest)');
  if (!tabs || tabs.classList.contains('is-guest')) return;
  var active = tabs.querySelector('.tp-tab.is-on') || tabs.querySelector('.tp-tab[data-tp-tab]');
  if (!active) return;
  window.tpEnsureTabGlider(tabs);
  window.tpMoveTabGlider(tabs, active, false);
  var tab = active.getAttribute('data-tp-tab') || 'all';
  window.tpApplyTabFilter(tab);
  if (!tabs.dataset.tpTabsBound) {
    tabs.dataset.tpTabsBound = '1';
    window.addEventListener('resize', function () {
      var on = tabs.querySelector('.tp-tab.is-on');
      if (on) window.tpMoveTabGlider(tabs, on, false);
    });
  }
};
window.tpEnsureImportFormatGlider = function (bar) {
  if (!bar) return null;
  var glider = bar.querySelector('.tp-import-formats-glider');
  if (glider) return glider;
  glider = document.createElement('span');
  glider.className = 'tp-import-formats-glider';
  glider.setAttribute('aria-hidden', 'true');
  bar.insertBefore(glider, bar.firstChild);
  return glider;
};
window.tpMoveImportFormatGlider = function (bar, btn, animate) {
  var glider = window.tpEnsureImportFormatGlider(bar);
  var metrics = window.tpTabGliderMetrics(bar, btn);
  if (!glider || !metrics) return;
  if (!animate) glider.style.transition = 'none';
  else glider.style.transition = '';
  glider.style.width = metrics.w + 'px';
  glider.style.height = metrics.h + 'px';
  glider.style.transform = 'translate3d(' + metrics.x + 'px,' + metrics.y + 'px,0)';
  if (!animate) {
    void glider.offsetWidth;
    glider.style.transition = '';
  }
  glider.classList.add('is-ready');
};
window.tpApplyImportFormat = function (format) {
  var root = document.getElementById('tpImportRoot');
  if (!root) return;
  var isSkos = format === 'skos';
  root.querySelectorAll('[data-tp-fmt-panel]').forEach(function (panel) {
    var kinds = (panel.getAttribute('data-tp-fmt-panel') || '')
      .split(',')
      .map(function (k) { return k.trim(); })
      .filter(Boolean);
    var show = kinds.indexOf(format) !== -1;
    panel.classList.toggle('is-off', !show);
  });
  window.tpSyncImportIdType && window.tpSyncImportIdType();
  var bar = document.getElementById('tpImportFormats');
  var zone = document.getElementById('tpImportFileBlock');
  var file = document.getElementById('tpImportFile');
  var hint = zone && zone.querySelector('.tp-drop-s');
  if (bar) {
    var accept = isSkos
      ? bar.getAttribute('data-accept-skos')
      : bar.getAttribute('data-accept-csv');
    var hintTxt = isSkos
      ? bar.getAttribute('data-hint-skos')
      : bar.getAttribute('data-hint-csv');
    if (zone && accept) zone.setAttribute('data-accept', accept);
    if (file && accept) file.setAttribute('accept', accept);
    if (hint && hintTxt) hint.textContent = hintTxt;
  }
};
window.tpSelectImportFormat = function (btn, opts) {
  if (!btn) return false;
  opts = opts || {};
  var bar = btn.closest('.tp-import-formats') || document.getElementById('tpImportFormats');
  if (!bar) return false;
  var format = btn.getAttribute('data-tp-format') || 'skos';
  var current = bar.querySelector('.tp-import-fmt.is-on');
  if (current === btn && !opts.force) return false;
  var all = Array.prototype.slice.call(bar.querySelectorAll('.tp-import-fmt[data-tp-format]'));
  all.forEach(function (el) { el.classList.remove('is-on'); });
  btn.classList.add('is-on');
  window.tpMoveImportFormatGlider(bar, btn, !opts.instant);
  var hidden = document.getElementById('tpImportFormat');
  if (hidden) hidden.value = format;
  window.tpApplyImportFormat(format);
  var zone = document.getElementById('tpImportFileBlock');
  var hadFile = !!(zone && zone.classList.contains('has-file'));
  var preview = document.getElementById('tpImportPreview');
  var existing = document.getElementById('tpImportExisting');
  if (preview) {
    preview.style.display = 'none';
    preview.classList.remove('is-ready');
  }
  if (existing) existing.style.display = 'none';
  var root = document.getElementById('tpImportRoot');
  if (root) {
    root.setAttribute('data-load-done', 'false');
    root.setAttribute('data-import-ready', 'false');
  }
  if (hadFile && !opts.keepFile) {
    var clearBtn = document.getElementById('tpImportClearFile');
    if (clearBtn) {
      clearBtn.click();
    } else if (zone) {
      zone.classList.remove('has-file', 'is-analyzed', 'is-pending', 'needs-reanalyze');
      var file = document.getElementById('tpImportFile');
      if (file) file.value = '';
    }
  }
  window.tpSyncImportSubmit && window.tpSyncImportSubmit();
  return false;
};
window.tpSyncImportIdType = function () {
  var sel = document.getElementById('tpImportIdType');
  var wrap = document.getElementById('tpImportHandlePrefixRow');
  if (!wrap) return;
  var value = sel ? String(sel.value || '').toLowerCase() : '';
  wrap.classList.toggle('is-off', value !== 'handle');
};
window.tpBindImportFormats = function () {
  var bar = document.getElementById('tpImportFormats');
  if (!bar) return;
  var active = bar.querySelector('.tp-import-fmt.is-on') || bar.querySelector('.tp-import-fmt[data-tp-format]');
  if (!active) return;
  window.tpEnsureImportFormatGlider(bar);
  window.tpMoveImportFormatGlider(bar, active, false);
  window.tpApplyImportFormat(active.getAttribute('data-tp-format') || 'skos');
  if (!bar.dataset.tpFmtBound) {
    bar.dataset.tpFmtBound = '1';
    window.addEventListener('resize', function () {
      var on = bar.querySelector('.tp-import-fmt.is-on');
      if (on) window.tpMoveImportFormatGlider(bar, on, false);
    });
  }
};
window.tpBindRowKeyboard = function () {
  document.querySelectorAll('tr.tp-row').forEach(function (row) {
    if (row.dataset.tpKeyBound === '1') return;
    row.dataset.tpKeyBound = '1';
    row.setAttribute('title', row.getAttribute('title') || 'Voir les détails du thésaurus');
    row.addEventListener('click', function (e) {
      if (e.target && e.target.closest('a,button,input,select,textarea,label,.tp-row-acts')) return;
      e.preventDefault();
      window.tpOpenRowThesaurus(row);
    });
    row.addEventListener('keydown', function (e) {
      if (e.key !== 'Enter' && e.key !== ' ') return;
      if (e.target && e.target !== row && e.target.closest('a,button,input,select,textarea,label,.tp-row-acts')) return;
      e.preventDefault();
      window.tpOpenRowThesaurus(row);
    });
  });
};
window.tpConsumeListFlash = function () {
  var root = document.getElementById('tpRoot');
  if (!root || typeof window.toast !== 'function') return;
  var msg = root.getAttribute('data-flash');
  var token = root.getAttribute('data-flash-token');
  var error = root.getAttribute('data-flash-error') === 'true';
  var report = root.getAttribute('data-flash-report') === 'true';
  var reportUrl = root.getAttribute('data-flash-report-url');
  var reportLabel = root.getAttribute('data-flash-report-label');
  if (!msg || !token) return;
  if (window._tpFlashToken === token) return;
  window._tpFlashToken = token;
  var opts = {};
  if (error) opts.error = true;
  if (report && reportUrl) {
    opts.href = reportUrl;
    opts.hrefLabel = reportLabel || 'Rapport';
  }
  window.toast(msg, opts);
};
window.tpSyncCreateSubmit = function () {
  var title = document.getElementById('tpCreateTitle');
  var btn = document.getElementById('tpCreateAsk');
  if (!title || !btn) return;
  var ok = !!(title.value && title.value.trim());
  btn.classList.toggle('is-disabled', !ok);
  btn.setAttribute('aria-disabled', ok ? 'false' : 'true');
  btn.disabled = !ok;
};
window.tpSubmitCreate = function (btn) {
  if (btn && (btn.classList.contains('is-disabled') || btn.disabled || btn.getAttribute('aria-disabled') === 'true')) {
    return false;
  }
  var title = document.getElementById('tpCreateTitle');
  var name = title && title.value ? title.value.trim() : '';
  if (!name) {
    window.tpSyncCreateSubmit && window.tpSyncCreateSubmit();
    return false;
  }
  window.tpSyncLangMultis && window.tpSyncLangMultis();
  window.tpShowLoading && window.tpShowLoading('Création du thésaurus…');
  var go = document.getElementById('tpCreateSubmit');
  if (go) go.click();
  return false;
};
window.tpAskCreate = window.tpSubmitCreate;
window.tpFilterPickMenu = function (input) {
  var menu = input && input.closest ? input.closest('.tp-lang-menu') : null;
  if (!menu) return;
  var needle = tpLangFold(input.value.trim());
  var visible = 0;
  menu.querySelectorAll('.tree-lang-opt, .tp-lang-multi-opt').forEach(function (el) {
    var hay = tpLangFold((el.getAttribute('data-label') || '') + ' ' + (el.getAttribute('data-code') || '') + ' ' + (el.getAttribute('data-value') || ''));
    var hide = !!needle && hay.indexOf(needle) === -1;
    el.classList.toggle('is-hide', hide);
    if (!hide && !el.classList.contains('is-principal')) visible += 1;
  });
  var empty = menu.querySelector('.tp-project-empty');
  if (empty) empty.hidden = visible > 0;
};
window.tpFilterCreateProjects = window.tpFilterPickMenu;
window.tpPickCreateProject = function (btn) {
  if (!btn) return false;
  var value = btn.getAttribute('data-value') || '';
  var label = btn.getAttribute('data-label') || '';
  var hidden = document.getElementById('tpCreateProject');
  if (hidden) hidden.value = value;
  var pick = btn.closest && btn.closest('.tp-project-pick');
  if (pick) {
    var name = pick.querySelector('.tp-lang-btn .tree-lang-name');
    if (name) name.textContent = label;
    pick.querySelectorAll('.tree-lang-opt').forEach(function (el) {
      el.classList.toggle('is-on', (el.getAttribute('data-value') || '') === value);
    });
    var toggle = pick.querySelector('.tp-lang-btn');
    if (toggle) {
      toggle.classList.remove('is-open');
      toggle.setAttribute('aria-expanded', 'false');
    }
    var query = pick.querySelector('.tp-project-q');
    if (query) {
      query.value = '';
      window.tpFilterCreateProjects(query);
    }
  }
  return false;
};
window.tpPickCreateChoice = function (btn) {
  var group = btn && btn.closest ? btn.closest('.tp-form-radios') : null;
  if (!group) return false;
  group.querySelectorAll('.tp-form-radio').forEach(function (el) {
    el.classList.remove('is-on');
  });
  btn.classList.add('is-on');
  var hidden = group.querySelector('input[type="hidden"]');
  if (!hidden) {
    var field = group.getAttribute('data-field');
    hidden = field ? document.getElementById(field) : null;
  }
  if (hidden) hidden.value = btn.getAttribute('data-value') || '';
  return false;
};
window.tpPickCreateLang = function (btn) {
  var code = tpLangOptionCode(btn);
  if (!code) return false;
  var hidden = document.getElementById('tpCreateSourceLang');
  if (hidden) hidden.value = code;
  var pick = btn.closest && btn.closest('.tp-lang-pick');
  if (pick) {
    var flag = pick.querySelector('.tp-lang-btn .tree-lang-flag');
    var name = pick.querySelector('.tp-lang-btn .tree-lang-name');
    var codeEl = pick.querySelector('.tp-lang-btn .tree-lang-code');
    if (flag) flag.textContent = btn.getAttribute('data-flag') || '';
    if (name) name.textContent = btn.getAttribute('data-label') || code;
    if (codeEl) codeEl.textContent = code;
    pick.querySelectorAll('.tree-lang-opt').forEach(function (el) {
      el.classList.toggle('is-on', tpLangOptionCode(el).toLowerCase() === code.toLowerCase());
    });
    var toggle = pick.querySelector('.tp-lang-btn');
    if (toggle) {
      toggle.classList.remove('is-open');
      toggle.setAttribute('aria-expanded', 'false');
    }
    var query = pick.querySelector('.tp-project-q');
    if (query) {
      query.value = '';
      window.tpFilterPickMenu && window.tpFilterPickMenu(query);
    }
  }
  window.tpSyncPrincipalLanguage && window.tpSyncPrincipalLanguage();
  return false;
};
window.tpPickEditLang = function (btn) {
  var code = tpLangOptionCode(btn);
  if (!code) return false;
  var hidden = document.getElementById('tpEditSourceLang');
  var previous = hidden && hidden.value ? String(hidden.value).trim() : '';
  var root = document.getElementById('tpEditLangsUi');
  var titleInput = document.getElementById('tpEditTitle');
  if (root && previous && previous.toLowerCase() !== code.toLowerCase()) {
    root.tpTitles = root.tpTitles || {};
    if (titleInput) root.tpTitles[previous] = titleInput.value || '';
    var stored = root.tpTitles[code];
    if (titleInput && stored != null && String(stored).trim()) {
      titleInput.value = stored;
    }
    delete root.tpTitles[code];
    root.querySelectorAll('.tp-lang-multi-opt').forEach(function (opt) {
      if (tpLangOptionCode(opt).toLowerCase() === previous.toLowerCase()) {
        opt.classList.add('is-on');
        opt.setAttribute('aria-selected', 'true');
      }
    });
  }
  if (hidden) hidden.value = code;
  var pick = btn.closest && btn.closest('.tp-lang-pick');
  if (pick) {
    var flag = pick.querySelector('.tp-lang-btn .tree-lang-flag');
    var name = pick.querySelector('.tp-lang-btn .tree-lang-name');
    var codeEl = pick.querySelector('.tp-lang-btn .tree-lang-code');
    if (flag) flag.textContent = btn.getAttribute('data-flag') || '';
    if (name) name.textContent = btn.getAttribute('data-label') || code;
    if (codeEl) codeEl.textContent = code;
    pick.querySelectorAll('.tree-lang-opt').forEach(function (el) {
      el.classList.toggle('is-on', tpLangOptionCode(el).toLowerCase() === code.toLowerCase());
    });
    var toggle = pick.querySelector('.tp-lang-btn');
    if (toggle) {
      toggle.classList.remove('is-open');
      toggle.setAttribute('aria-expanded', 'false');
    }
    var query = pick.querySelector('.tp-project-q');
    if (query) {
      query.value = '';
      window.tpFilterPickMenu && window.tpFilterPickMenu(query);
    }
  }
  window.tpSyncPrincipalLanguage && window.tpSyncPrincipalLanguage();
  return false;
};
