/** Sélecteur — langues et intitulés. */
function tpLangFold(value) {
  return String(value || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}
function tpLangOptionCode(el) {
  if (!el) return '';
  var fromAttr = el.getAttribute('data-code');
  if (fromAttr && String(fromAttr).trim()) return String(fromAttr).trim();
  if (el.value && String(el.value).trim()) return String(el.value).trim();
  var node = el.querySelector('.tp-lang-multi-code');
  return node ? String(node.textContent || '').trim() : '';
}
function tpLangHidden(root) {
  if (!root) return null;
  return root.querySelector('#tpCreateLangs, #tpEditLangs, input.tp-lang-multi-value, .tp-lang-multi-value');
}
function tpBindLangMulti(root) {
  if (!root || root.getAttribute('data-bound') === '1') return;
  root.setAttribute('data-bound', '1');
  var hidden = tpLangHidden(root);
  var list = root.querySelector('.tp-lang-multi-list');
  var chips = root.querySelector('.tp-lang-multi-chips');
  var query = root.querySelector('.tp-lang-multi-q');
  if (!list || !chips) return;
  var titles = root.querySelector('.tp-lang-titles');
  root.tpTitles = root.tpTitles || {};
  function titlesBox() {
    return root.querySelector('#tpCreateTitles, #tpEditTitles');
  }
  root.querySelectorAll('.tp-lang-title-i').forEach(function (input) {
    var code = input.getAttribute('data-code');
    if (code && String(input.value || '').trim() && root.tpTitles[code] == null) {
      root.tpTitles[code] = input.value;
    }
  });
  var titlesHidden = titlesBox();
  if (titlesHidden && titlesHidden.value) {
    titlesHidden.value.split(/\n|\|\|/).forEach(function (line) {
      var eq = line.indexOf('=');
      if (eq <= 0) return;
      var code = line.slice(0, eq).trim();
      if (code && root.tpTitles[code] == null) root.tpTitles[code] = line.slice(eq + 1);
    });
  }
  Object.keys(root.tpTitles).forEach(function (code) {
    if (!String(root.tpTitles[code] || '').trim()) return;
    list.querySelectorAll('.tp-lang-multi-opt').forEach(function (opt) {
      if (opt.classList.contains('is-principal')) return;
      if (tpLangOptionCode(opt).toLowerCase() !== String(code).toLowerCase()) return;
      opt.classList.add('is-on');
      opt.setAttribute('aria-selected', 'true');
    });
  });
  function writeTitles(selected) {
    var box = titlesBox();
    if (!box) return;
    var lines = [];
    selected.forEach(function (el) {
      var code = tpLangOptionCode(el);
      if (!code) return;
      var value = root.tpTitles[code] || '';
      if (String(value).trim()) lines.push(code + '=' + String(value).replace(/[\r\n]/g, ' '));
    });
    box.value = lines.join('\n');
  }
  function paintTitles(selected) {
    if (!titles) return;
    var active = document.activeElement;
    var keepCode = active && active.classList && active.classList.contains('tp-lang-title-i')
      ? active.getAttribute('data-code') : '';
    var keepStart = keepCode && typeof active.selectionStart === 'number' ? active.selectionStart : null;
    titles.replaceChildren();
    if (!selected.length) {
      var emptyTitle = document.createElement('span');
      emptyTitle.className = 'tp-lang-multi-empty';
      emptyTitle.textContent = 'Aucune langue ajoutée';
      titles.appendChild(emptyTitle);
      return;
    }
    selected.forEach(function (el) {
      var code = tpLangOptionCode(el);
      if (!code) return;
      var label = el.getAttribute('data-label') || code;
      var row = document.createElement('div');
      row.className = 'tp-lang-title';
      var remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'tp-lang-title-x';
      remove.setAttribute('aria-label', 'Retirer ' + label);
      remove.textContent = '×';
      remove.addEventListener('click', function () {
        el.classList.remove('is-on');
        el.setAttribute('aria-selected', 'false');
        if (root.tpTitles) delete root.tpTitles[code];
        paint();
      });
      var meta = document.createElement('span');
      meta.className = 'tp-lang-title-meta';
      var flag = document.createElement('span');
      flag.setAttribute('aria-hidden', 'true');
      flag.textContent = el.getAttribute('data-flag') || '';
      var name = document.createElement('span');
      name.className = 'tp-lang-multi-name';
      name.textContent = label;
      meta.appendChild(flag);
      meta.appendChild(name);
      var input = document.createElement('input');
      input.type = 'text';
      input.className = 'tp-form-i tp-lang-title-i';
      input.setAttribute('data-code', code);
      input.setAttribute('aria-label', 'Nom en ' + label);
      input.placeholder = 'Nom en ' + label;
      input.value = root.tpTitles[code] || '';
      input.addEventListener('input', function () {
        root.tpTitles[code] = input.value;
        writeTitles(Array.from(list.querySelectorAll('.tp-lang-multi-opt.is-on:not(.is-principal)')));
      });
      input.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') event.preventDefault();
      });
      row.appendChild(remove);
      row.appendChild(meta);
      row.appendChild(input);
      titles.appendChild(row);
      if (keepCode && keepCode === code) {
        input.focus();
        if (keepStart != null && input.setSelectionRange) {
          var pos = Math.min(keepStart, input.value.length);
          input.setSelectionRange(pos, pos);
        }
      }
    });
  }
  function paint() {
    hidden = tpLangHidden(root);
    var selected = Array.from(list.querySelectorAll('.tp-lang-multi-opt.is-on:not(.is-principal)'));
    if (hidden) {
      hidden.value = selected.map(tpLangOptionCode).filter(Boolean).join(',');
    }
    chips.replaceChildren();
    if (titles) {
      paintTitles(selected);
      writeTitles(selected);
      return;
    }
    if (!selected.length) {
      var empty = document.createElement('span');
      empty.className = 'tp-lang-multi-empty';
      empty.textContent = 'Aucune langue ajoutée';
      chips.appendChild(empty);
      paintTitles(selected);
      writeTitles(selected);
      return;
    }
    selected.forEach(function (el) {
      var label = el.getAttribute('data-label') || el.getAttribute('data-code') || '';
      var chip = document.createElement('span');
      chip.className = 'tp-lang-multi-chip';
      var flag = document.createElement('span');
      flag.setAttribute('aria-hidden', 'true');
      flag.textContent = el.getAttribute('data-flag') || '';
      var name = document.createElement('span');
      name.textContent = label;
      var remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'tp-lang-multi-x';
      remove.setAttribute('aria-label', 'Retirer ' + label);
      remove.textContent = '×';
      remove.addEventListener('click', function () {
        el.classList.remove('is-on');
        el.setAttribute('aria-selected', 'false');
        paint();
      });
      chip.appendChild(flag);
      chip.appendChild(name);
      chip.appendChild(remove);
      chips.appendChild(chip);
    });
    paintTitles(selected);
    writeTitles(selected);
  }
  list.addEventListener('click', function (event) {
    var opt = event.target.closest && event.target.closest('.tp-lang-multi-opt');
    if (!opt || opt.classList.contains('is-principal')) return;
    event.preventDefault();
    var on = !opt.classList.contains('is-on');
    opt.classList.toggle('is-on', on);
    opt.setAttribute('aria-selected', on ? 'true' : 'false');
    paint();
  });
  if (query) {
    query.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') event.preventDefault();
    });
    query.addEventListener('input', function () {
      var needle = tpLangFold(query.value.trim());
      list.querySelectorAll('.tp-lang-multi-opt').forEach(function (el) {
        var hay = tpLangFold((el.getAttribute('data-label') || '') + ' ' + (el.getAttribute('data-code') || ''));
        el.classList.toggle('is-hide', !!needle && hay.indexOf(needle) === -1);
      });
    });
  }
  paint();
  root.tpSyncLangs = paint;
}
window.tpSyncLangMultis = function () {
  document.querySelectorAll('.tp-lang-multi').forEach(function (root) {
    if (typeof root.tpSyncLangs === 'function') root.tpSyncLangs();
  });
};
window.tpBindLangMultis = function () {
  document.querySelectorAll('.tp-lang-multi').forEach(tpBindLangMulti);
  var form = document.getElementById('thesaurusPickerForm');
  if (form && form.getAttribute('data-lang-sync') !== '1') {
    form.setAttribute('data-lang-sync', '1');
    form.addEventListener('click', function (event) {
      if (event.target.closest && event.target.closest('.tp-lang-multi')) return;
      window.tpSyncLangMultis && window.tpSyncLangMultis();
    }, true);
    form.addEventListener('submit', function () {
      window.tpSyncLangMultis && window.tpSyncLangMultis();
    }, true);
  }
  window.tpSyncPrincipalLanguage && window.tpSyncPrincipalLanguage();
};
if (document.readyState !== 'loading') window.tpBindLangMultis();
window.tpApplyProgressSteps = function (mode) {
  var box = document.getElementById('tpLoading');
  if (!box) return;
  var exportMode = mode === 'export';
  var labels = exportMode
    ? [
        box.getAttribute('data-export-step-0'),
        box.getAttribute('data-export-step-1'),
        box.getAttribute('data-export-step-2'),
        box.getAttribute('data-export-step-3')
      ]
    : [
        box.getAttribute('data-step-prepare'),
        box.getAttribute('data-step-create'),
        box.getAttribute('data-step-concepts'),
        box.getAttribute('data-step-finalize')
      ];
  box.querySelectorAll('.tp-prog-steps li').forEach(function (li, idx) {
    if (labels[idx]) li.textContent = labels[idx];
  });
  var list = box.querySelector('.tp-prog-steps');
  if (list) {
    list.setAttribute(
      'aria-label',
      box.getAttribute(exportMode ? 'data-export-steps' : 'data-import-steps') || ''
    );
  }
  box.setAttribute(
    'data-active-meta-tpl',
    box.getAttribute(exportMode ? 'data-export-meta-tpl' : 'data-meta-tpl') || box.getAttribute('data-meta-tpl') || ''
  );
};
