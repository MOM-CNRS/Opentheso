/** Sélecteur — tableau, filtres et ordre des colonnes. */
window.tpReorderByKeys = function (parent, selector, order, attr) {
  if (!parent || !order || !order.length) return;
  var nodes = Array.prototype.slice.call(parent.querySelectorAll(selector));
  if (!nodes.length) return;
  var byKey = {};
  nodes.forEach(function (el) {
    var key = el.getAttribute(attr);
    if (key) byKey[key] = el;
  });
  var anchor = nodes[0];
  order.forEach(function (key) {
    var el = byKey[key];
    if (!el) return;
    parent.insertBefore(el, anchor);
    anchor = el.nextSibling;
  });
};
window.tpApplyColumnOrder = function (prefs) {
  if (!prefs || !prefs.order || !prefs.order.length) return;
  var order = prefs.order;
  var table = document.querySelector('.tp-table');
  if (table) {
    var headRows = table.querySelectorAll('thead tr');
    headRows.forEach(function (tr) {
      window.tpReorderByKeys(tr, 'th[data-col]', order, 'data-col');
    });
    table.querySelectorAll('tbody tr.tp-row').forEach(function (tr) {
      window.tpReorderByKeys(tr, 'td[data-col]', order, 'data-col');
    });
  }
  var menu = document.querySelector('.tp-cols-list');
  if (menu) {
    window.tpReorderByKeys(menu, '.tp-cols-row[data-col-key]', order, 'data-col-key');
  }
};
window.tpPinActionsColumn = function () {
  document.querySelectorAll('.tp-table tr').forEach(function (tr) {
    var cfg = tr.querySelector('.tp-th--cfg, .tp-c-cfg');
    var name = tr.querySelector('[data-col="name"]');
    var edit = tr.querySelector('.tp-th--edit, .tp-c-edit');
    var lang = tr.querySelector('[data-col="lang"]');
    if (name) tr.insertBefore(name, cfg ? cfg.nextSibling : tr.firstChild);
    if (edit) tr.appendChild(edit);
    if (lang) tr.insertBefore(lang, edit || null);
  });
};
window.tpFitColumns = function () {
  var wrap = document.querySelector('.tp-body > .tp-table-wrap');
  var table = wrap && wrap.querySelector('table.tp-table');
  if (!table) return;
  var nameCells = table.querySelectorAll('[data-col="name"]');
  var clear = function () {
    table.style.width = '';
    wrap.classList.remove('is-wide');
    nameCells.forEach(function (el) { el.style.width = ''; });
  };
  if (window.matchMedia('(max-width: 900px)').matches) {
    clear();
    return;
  }
  var head = table.querySelector('thead tr');
  var nameTh = head && head.querySelector('th[data-col="name"]');
  if (!head || !nameTh || nameTh.classList.contains('is-col-off')) {
    clear();
    return;
  }
  var rem = parseFloat(getComputedStyle(document.documentElement).fontSize) || 16;
  var widthOf = {
    access: 9.5 * rem,
    role: 9.5 * rem,
    terms: 6.5 * rem,
    created: 8.5 * rem,
    domain: 14 * rem,
    projects: 14 * rem,
    org: 14 * rem,
    chrono: 14 * rem,
    lang: 14 * rem
  };
  var cfg = 0;
  var edit = 0;
  var lang = 0;
  var middle = [];
  Array.prototype.forEach.call(head.children, function (th) {
    if (th.classList.contains('is-col-off')) return;
    if (th.classList.contains('tp-th--cfg')) { cfg = 46; return; }
    if (th.classList.contains('tp-th--edit')) { edit = 148; return; }
    var col = th.getAttribute('data-col');
    if (!col || col === 'name') return;
    if (col === 'lang') { lang = widthOf.lang; return; }
    if (widthOf[col]) middle.push(widthOf[col]);
  });
  var fixed = cfg + edit + lang + middle.reduce(function (sum, w) { return sum + w; }, 0);
  var apply = function (avail) {
    var room = avail - fixed;
    var nameW;
    var tableW;
    if (room >= 10 * rem) {
      nameW = room;
      tableW = avail;
    } else {
      var budget = avail - cfg - edit - lang;
      var nameMin = 14 * rem;
      var used = 0;
      if (budget > nameMin) {
        middle.forEach(function (w) {
          if (budget - (used + w) >= nameMin) used += w;
        });
        nameW = budget - used;
      } else {
        nameW = Math.max(8 * rem, budget);
      }
      tableW = fixed + nameW;
    }
    var tablePx = Math.ceil(tableW) + 'px';
    var px = Math.max(0, Math.floor(nameW)) + 'px';
    if (table.style.width !== tablePx) table.style.width = tablePx;
    nameCells.forEach(function (el) {
      if (el.style.width !== px) el.style.width = px;
    });
    return tableW > avail + 1;
  };
  var avail = wrap.clientWidth;
  if (avail < 40) return;
  if (apply(avail) && wrap.clientWidth < avail - 1) apply(wrap.clientWidth);
  wrap.classList.toggle('is-wide', table.scrollWidth > wrap.clientWidth + 1);
};
window.tpLoadColumnPrefs = function () {
  if (window._tpColumnMutating) return;
  var prefs = window.tpReadColumnPrefs();
  if (prefs) {
    window.tpApplyColumnOrder(prefs);
    window.tpApplyColumnVisibility(prefs);
  }
  window.tpPinActionsColumn();
  window.tpFitColumns();
  window.tpSyncColumnMoveButtons && window.tpSyncColumnMoveButtons();
};
window.tpSubmitRefresh = function () {
  var btn = document.getElementById('tpRefresh');
  var form = document.getElementById('thesaurusPickerForm');
  if (btn && form && typeof form.requestSubmit === 'function') {
    form.requestSubmit(btn);
    return false;
  }
  if (btn) {
    btn.click();
    return false;
  }
  if (form) {
    form.submit();
    return false;
  }
  return true;
};
window.tpCurrentQuery = function () {
  var input = document.getElementById('tpQuery') || document.querySelector('.tp-search-input');
  return input && input.value != null ? String(input.value).trim().toLowerCase() : '';
};
window.tpFilterFields = function (key) {
  var seen = [];
  var add = function (el) {
    if (!el || seen.indexOf(el) !== -1) return;
    var tag = el.tagName;
    if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') {
      seen.push(el);
      return;
    }
    var inner = el.querySelector && el.querySelector('input, select, textarea');
    if (inner && seen.indexOf(inner) === -1) seen.push(inner);
  };
  document.querySelectorAll('[data-tp-filter="' + key + '"]').forEach(add);
  if (key === 'lang') {
    document.querySelectorAll('input.tp-langf-val').forEach(add);
  }
  return seen;
};
window.tpColFilterValue = function (key) {
  var best = '';
  window.tpFilterFields(key).forEach(function (el) {
    var v = el && el.value != null ? String(el.value).trim() : '';
    if (v) best = v;
  });
  return best.toLowerCase();
};
window.tpOnColFilter = function (el) {
  if (!el) return;
  var key = el.getAttribute('data-tp-filter');
  if (!key) return;
  var val = el.value != null ? el.value : '';
  window.tpFilterFields(key).forEach(function (other) {
    if (other !== el) other.value = val;
  });
  window.tpApplyListFilters && window.tpApplyListFilters();
};
window.tpLangfMenuFor = function (wrap) {
  if (!wrap) return null;
  if (wrap._tpLangfMenu && document.body.contains(wrap._tpLangfMenu)) {
    return wrap._tpLangfMenu;
  }
  return wrap.querySelector('.tp-langf-menu');
};
window.tpLangfCloseAll = function () {
  document.querySelectorAll('.tp-langf.is-open').forEach(function (wrap) {
    wrap.classList.remove('is-open');
    var btn = wrap.querySelector('.tp-langf-btn');
    var menu = window.tpLangfMenuFor(wrap);
    if (btn) btn.setAttribute('aria-expanded', 'false');
    if (menu) {
      menu.hidden = true;
      menu.classList.remove('is-ported');
      menu.style.position = '';
      menu.style.left = '';
      menu.style.top = '';
      menu.style.right = '';
      menu.style.minWidth = '';
      menu.style.maxHeight = '';
      menu.style.zIndex = '';
      if (menu.parentNode !== wrap) {
        wrap.appendChild(menu);
      }
    }
    wrap._tpLangfMenu = null;
  });
};
window.tpLangfPlaceMenu = function (wrap) {
  var btn = wrap && wrap.querySelector('.tp-langf-btn');
  var menu = wrap && window.tpLangfMenuFor(wrap);
  if (!btn || !menu) return;
  wrap._tpLangfMenu = menu;
  if (menu.parentNode !== document.body) {
    document.body.appendChild(menu);
  }
  menu.classList.add('is-ported');
  menu.hidden = false;
  var r = btn.getBoundingClientRect();
  var width = Math.max(Math.round(r.width), 196);
  var maxH = Math.min(280, Math.max(120, window.innerHeight - 16));
  menu.style.position = 'fixed';
  menu.style.right = 'auto';
  menu.style.minWidth = width + 'px';
  menu.style.maxHeight = maxH + 'px';
  menu.style.zIndex = '4000';
  var mh = menu.offsetHeight || Math.min(220, maxH);
  var left = r.left;
  if (left + width > window.innerWidth - 8) {
    left = r.right - width;
  }
  left = Math.max(8, Math.min(left, window.innerWidth - width - 8));
  var top = r.bottom + 4;
  if (top + mh > window.innerHeight - 8 && r.top - 4 - mh >= 8) {
    top = r.top - 4 - mh;
  }
  menu.style.left = Math.round(left) + 'px';
  menu.style.top = Math.round(top) + 'px';
};
window.tpLangfReposition = function () {
  document.querySelectorAll('.tp-langf.is-open').forEach(function (wrap) {
    window.tpLangfPlaceMenu(wrap);
  });
};
window.tpLangfApply = function (lang, flag, label) {
  var code = lang != null ? String(lang) : '';
  document.querySelectorAll('.tp-langf').forEach(function (wrap) {
    var allLbl = wrap.getAttribute('data-all-label') || '';
    var hidden = wrap.querySelector('input.tp-langf-val')
      || wrap.querySelector('[data-tp-filter="lang"]');
    if (hidden) hidden.value = code;
    var btn = wrap.querySelector('.tp-langf-btn');
    var fl = wrap.querySelector('.tp-langf-btn > .tp-langf-fl');
    var lbl = wrap.querySelector('.tp-langf-btn > .tp-langf-lbl');
    if (btn) {
      btn.classList.toggle('has-val', !!code);
    }
    if (fl) {
      fl.textContent = code ? (flag || '') : '';
    }
    if (lbl) {
      lbl.textContent = code ? (label || code) : allLbl;
    }
    wrap.querySelectorAll('.tp-langf-opt').forEach(function (opt) {
      opt.classList.toggle('is-on', !!code && opt.getAttribute('data-lang') === code);
    });
  });
  window.tpApplyListFilters && window.tpApplyListFilters();
};
window.tpRowMatchesQuery = function (row, needle) {
  if (!needle) return true;
  if (!row) return false;
  var hay = row.getAttribute('data-tp-search') || '';
  return hay.indexOf(needle) !== -1;
};
window.tpRowMatchesColFilters = function (row) {
  if (!row) return false;
  var name = window.tpColFilterValue('name');
  if (name) {
    var hayName = row.getAttribute('data-tp-f-name') || '';
    if (hayName.indexOf(name) === -1) return false;
  }
  var domain = window.tpColFilterValue('domain');
  if (domain) {
    var hayDomain = row.getAttribute('data-tp-f-domain') || '';
    if (hayDomain.indexOf(domain) === -1) return false;
  }
  var projects = window.tpColFilterValue('projects');
  if (projects) {
    var hayProjects = row.getAttribute('data-tp-f-projects') || '';
    if (hayProjects.indexOf(projects) === -1) return false;
  }
  var org = window.tpColFilterValue('org');
  if (org) {
    var hayOrg = row.getAttribute('data-tp-f-org') || '';
    if (hayOrg.indexOf(org) === -1) return false;
  }
  var lang = window.tpColFilterValue('lang');
  if (lang) {
    var hayLangs = ' ' + (row.getAttribute('data-tp-f-langs') || '') + ' ';
    if (hayLangs.indexOf(' ' + lang + ' ') === -1) return false;
  }
  return true;
};
window.tpApplyListFilters = function () {
  var tabs = document.getElementById('tpTabs') || document.querySelector('.tp-tabs');
  var active = tabs && tabs.querySelector('.tp-tab.is-on');
  var hidden = document.getElementById('tpActiveTab');
  var tab = (active && active.getAttribute('data-tp-tab'))
    || (hidden && hidden.value)
    || 'all';
  var needle = window.tpCurrentQuery();
  var visible = 0;
  var queryMatches = 0;
  document.querySelectorAll('tr.tp-row').forEach(function (row) {
    var matchQ = window.tpRowMatchesQuery(row, needle);
    var matchC = window.tpRowMatchesColFilters(row);
    var matchT = window.tpRowMatchesTab(row, tab);
    if (matchQ && matchC) queryMatches += 1;
    row.classList.toggle('is-query-off', !matchQ);
    row.classList.toggle('is-col-filter-off', !matchC);
    row.classList.toggle('is-tab-off', !matchT);
    if (matchQ && matchC && matchT) visible += 1;
  });
  var allCount = document.querySelector('.tp-tab[data-tp-tab="all"] .tp-tab-n');
  if (allCount) allCount.textContent = String(queryMatches);
  var guestCount = document.getElementById('tpGuestCountN');
  if (guestCount) guestCount.textContent = String(visible);
  var tabEmpty = document.getElementById('tpTabEmpty');
  var catalogEmpty = document.querySelector('.tp-empty-row--catalog');
  if (tabEmpty) {
    var showTabEmpty = visible === 0 && !catalogEmpty;
    tabEmpty.hidden = !showTabEmpty;
    if (showTabEmpty) tabEmpty.removeAttribute('hidden');
    else tabEmpty.setAttribute('hidden', 'hidden');
  }
};
window.tpClearSearch = function () {
  var input = document.getElementById('tpQuery') || document.querySelector('.tp-search-input');
  if (input) {
    input.value = '';
    input.focus();
  }
  window.tpSyncSearchClear(input);
  return false;
};
window.tpSyncSearchClear = function (input) {
  var box = document.getElementById('tpSearch') || (input && input.closest && input.closest('.tp-search'));
  if (!box) return;
  var val = '';
  if (input && input.value != null) {
    val = input.value;
  } else {
    var field = box.querySelector('.tp-search-input');
    val = field ? field.value : '';
  }
  box.classList.toggle('has-query', String(val).trim().length > 0);
  window.tpApplyListFilters && window.tpApplyListFilters();
};
window.tpSaveColumnPrefs = function () {
  window.tpWriteColumnPrefs(window.tpCollectColumnPrefs());
};
window.tpOnColumnToggle = function (cb) {
  if (!cb) return;
  var key = cb.getAttribute('data-col-key');
  if (!key) {
    var row = cb.closest('[data-col-key]');
    key = row ? row.getAttribute('data-col-key') : '';
  }
  if (!key) return;
  var show = !!cb.checked;
  document.querySelectorAll('.tp-table [data-col="' + key + '"]').forEach(function (el) {
    el.classList.toggle('is-col-off', !show);
  });
  window.tpSaveColumnPrefs();
  window.tpPinActionsColumn && window.tpPinActionsColumn();
  window.tpFitColumns && window.tpFitColumns();
};
window.tpSyncColumnMoveButtons = function () {
  var list = document.querySelector('.tp-cols-list');
  if (!list) return;
  var rows = Array.prototype.filter.call(list.querySelectorAll('.tp-cols-row'), function (row) {
    return row.getAttribute('data-fixed') !== '1';
  });
  rows.forEach(function (row, index) {
    var up = row.querySelector('.tp-cols-mv[data-dir="-1"]');
    var down = row.querySelector('.tp-cols-mv[data-dir="1"]');
    if (up) up.disabled = index === 0;
    if (down) down.disabled = index === rows.length - 1;
  });
};
window.tpApplyMenuOrder = function () {
  var order = [];
  document.querySelectorAll('.tp-cols-row[data-col-key]').forEach(function (row) {
    order.push(row.getAttribute('data-col-key'));
  });
  window.tpApplyColumnOrder({ order: order });
  window.tpPinActionsColumn && window.tpPinActionsColumn();
  window.tpFitColumns && window.tpFitColumns();
  window.tpSaveColumnPrefs();
  window.tpSyncColumnMoveButtons();
};
window.tpMoveColumnRow = function (btn, delta) {
  var row = btn && btn.closest ? btn.closest('.tp-cols-row') : null;
  var list = row && row.parentElement;
  if (!row || !list || row.getAttribute('data-fixed') === '1') return false;
  var rows = Array.prototype.filter.call(list.querySelectorAll('.tp-cols-row'), function (item) {
    return item.getAttribute('data-fixed') !== '1';
  });
  var index = rows.indexOf(row);
  var target = index + delta;
  if (index < 0 || target < 0 || target >= rows.length) return false;
  var other = rows[target];
  if (delta < 0) list.insertBefore(row, other);
  else list.insertBefore(row, other.nextSibling);
  window.tpApplyMenuOrder();
  return false;
};
window.tpResetColumnMenu = function () {
  var list = document.querySelector('.tp-cols-list');
  if (!list) return false;
  var rows = Array.prototype.slice.call(list.querySelectorAll('.tp-cols-row'));
  rows.sort(function (a, b) {
    return Number(a.getAttribute('data-default-index')) - Number(b.getAttribute('data-default-index'));
  });
  rows.forEach(function (row) {
    list.appendChild(row);
    var key = row.getAttribute('data-col-key');
    var show = row.getAttribute('data-default-on') === '1' || row.getAttribute('data-fixed') === '1' && row.querySelector('input[type="checkbox"]:disabled');
    if (row.getAttribute('data-default-on') === '1') show = true;
    if (row.getAttribute('data-default-on') === '0') show = false;
    var cb = row.querySelector('input[type="checkbox"]');
    if (cb && !cb.disabled) cb.checked = show;
    if (cb && cb.disabled) show = true;
    document.querySelectorAll('.tp-table [data-col="' + key + '"]').forEach(function (el) {
      el.classList.toggle('is-col-off', !show);
    });
  });
  window.tpApplyColumnOrder({
    order: rows.map(function (row) { return row.getAttribute('data-col-key'); })
  });
  window.tpPinActionsColumn && window.tpPinActionsColumn();
  window.tpFitColumns && window.tpFitColumns();
  window.tpSyncColumnMoveButtons();
  try { localStorage.removeItem(window.TP_COLS_KEY); } catch (err) {}
  return false;
};
window.tpResetColumnPrefs = function () {
  return window.tpResetColumnMenu();
};
window.tpPlaceColumnMenu = function () {
  var menu = document.getElementById('tpColsMenu');
  var btn = document.getElementById('tpColsBtn');
  if (!menu || !btn || menu.hidden) return;
  var list = menu.querySelector('.tp-cols-list');
  var rect = btn.getBoundingClientRect();
  var spaceBelow = window.innerHeight - rect.bottom - 16;
  var spaceAbove = rect.top - 16;
  var openBelow = spaceBelow >= 220 || spaceBelow >= spaceAbove;
  var available = Math.max(160, (openBelow ? spaceBelow : spaceAbove) - 118);
  if (list) list.style.maxHeight = available + 'px';
  var width = menu.offsetWidth || 320;
  var height = menu.offsetHeight || 280;
  var left = Math.max(8, Math.min(rect.left, window.innerWidth - width - 8));
  var top = openBelow ? rect.bottom + 8 : Math.max(8, rect.top - height - 8);
  menu.style.left = left + 'px';
  menu.style.top = top + 'px';
};
window.tpParkColumnMenu = function () {
  var menu = document.getElementById('tpColsMenu');
  var anchor = document.querySelector('.tp-cols');
  if (menu && anchor && menu.parentElement !== anchor) anchor.appendChild(menu);
};
window.tpCloseColumnMenu = function () {
  var menu = document.getElementById('tpColsMenu');
  var btn = document.getElementById('tpColsBtn');
  if (!menu) return;
  menu.hidden = true;
  if (btn) {
    btn.classList.remove('is-on');
    btn.setAttribute('aria-expanded', 'false');
  }
  window.tpParkColumnMenu();
};
window.tpOpenColumnMenu = function () {
  var menu = document.getElementById('tpColsMenu');
  var btn = document.getElementById('tpColsBtn');
  if (!menu || !btn) return;
  menu.hidden = false;
  btn.classList.add('is-on');
  btn.setAttribute('aria-expanded', 'true');
  document.body.appendChild(menu);
  window.tpPlaceColumnMenu();
};
window.tpToggleColumnMenu = function () {
  var menu = document.getElementById('tpColsMenu');
  if (menu && !menu.hidden) window.tpCloseColumnMenu();
  else window.tpOpenColumnMenu();
};
window.tpBindColumnMenu = function () {
  var btn = document.getElementById('tpColsBtn');
  var menu = document.getElementById('tpColsMenu');
  var list = menu && menu.querySelector('.tp-cols-list');
  if (!btn || !menu || btn.dataset.tpColsBound === '1') return;
  btn.dataset.tpColsBound = '1';
  btn.addEventListener('click', function (ev) {
    ev.preventDefault();
    ev.stopPropagation();
    window.tpToggleColumnMenu();
  });
  menu.addEventListener('click', function (ev) {
    var act = ev.target.closest && ev.target.closest('[data-act]');
    if (!act) return;
    if (act.getAttribute('data-act') === 'tp-cols-close') window.tpCloseColumnMenu();
    if (act.getAttribute('data-act') === 'tp-cols-reset') window.tpResetColumnMenu();
  });
  if (list) {
    list.addEventListener('dragstart', function (ev) {
      var grip = ev.target.closest && ev.target.closest('.tp-cols-grip');
      var row = grip && grip.closest('.tp-cols-row');
      if (!row || row.getAttribute('data-fixed') === '1') {
        ev.preventDefault();
        return;
      }
      row.classList.add('is-dragging');
      ev.dataTransfer.effectAllowed = 'move';
      try { ev.dataTransfer.setData('text/plain', row.getAttribute('data-col-key') || ''); } catch (err) {}
    });
    list.addEventListener('dragover', function (ev) {
      var row = ev.target.closest && ev.target.closest('.tp-cols-row');
      var dragging = list.querySelector('.tp-cols-row.is-dragging');
      if (!row || !dragging || row === dragging || row.getAttribute('data-fixed') === '1') return;
      ev.preventDefault();
      var rect = row.getBoundingClientRect();
      var after = ev.clientY > rect.top + rect.height / 2;
      list.insertBefore(dragging, after ? row.nextSibling : row);
    });
    var finishDrag = function () {
      if (!list.querySelector('.tp-cols-row.is-dragging')) return;
      list.querySelectorAll('.tp-cols-row.is-dragging').forEach(function (row) {
        row.classList.remove('is-dragging');
      });
      window.tpApplyMenuOrder();
    };
    list.addEventListener('drop', function (ev) {
      ev.preventDefault();
      finishDrag();
    });
    list.addEventListener('dragend', finishDrag);
  }
  document.addEventListener('click', function (ev) {
    if (!menu || menu.hidden) return;
    if (menu.contains(ev.target) || btn.contains(ev.target)) return;
    window.tpCloseColumnMenu();
  });
  document.addEventListener('keydown', function (ev) {
    if (ev.key === 'Escape') window.tpCloseColumnMenu();
  });
  var form = document.getElementById('thesaurusPickerForm');
  if (form) {
    form.addEventListener('click', function (ev) {
      if (menu.contains(ev.target) || btn.contains(ev.target)) return;
      window.tpParkColumnMenu();
    }, true);
    form.addEventListener('submit', function () {
      window.tpParkColumnMenu();
    }, true);
  }
  window.addEventListener('resize', window.tpPlaceColumnMenu);
  var wrap = document.querySelector('.tp-body > .tp-table-wrap');
  if (wrap) wrap.addEventListener('scroll', window.tpPlaceColumnMenu);
  window.tpSyncColumnMoveButtons();
};
window.tpToggleMobileFilters = function () {
  var btn = document.getElementById('tpMobileFiltersBtn');
  var panel = document.getElementById('tpMobileFiltersPanel');
  if (!btn || !panel) return false;
  var open = panel.classList.toggle('is-open');
  panel.hidden = !open;
  btn.setAttribute('aria-expanded', open ? 'true' : 'false');
  return false;
};
window.tpOpenRowThesaurus = function (row) {
  if (!row) return;
  var open = row.querySelector('a.tp-c-name-in, a.tp-act--open');
  if (!open) return;
  open.click();
};
window.TP_TAB_GLIDER_KEY = 'opentheso.tp.tabGlider';
window.tpTabGliderMetrics = function (tabs, tab) {
  if (!tabs || !tab) return null;
  var tabsRect = tabs.getBoundingClientRect();
  var tabRect = tab.getBoundingClientRect();
  return {
    x: Math.round((tabRect.left - tabsRect.left + tabs.scrollLeft) * 100) / 100,
    y: Math.round((tabRect.top - tabsRect.top + tabs.scrollTop) * 100) / 100,
    w: Math.round(tabRect.width * 100) / 100,
    h: Math.round(tabRect.height * 100) / 100
  };
};
window.tpEnsureTabGlider = function (tabs) {
  if (!tabs) return null;
  var glider = tabs.querySelector('.tp-tabs-glider');
  if (glider) return glider;
  glider = document.createElement('span');
  glider.className = 'tp-tabs-glider';
  glider.setAttribute('aria-hidden', 'true');
  tabs.insertBefore(glider, tabs.firstChild);
  return glider;
};
