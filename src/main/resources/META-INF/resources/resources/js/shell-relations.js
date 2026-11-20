/** Fiche — relations sémantiques. */
var CV_REL_META = {
  bt: { code: "TG" },
  nt: { code: "TS" },
  rt: { code: "TA" }
};
var cvRelState = {
  bt: [], nt: [], rt: [], kind: "nt", seq: 0, timer: null, hits: [],
  painted: false, bound: null, added: {}, rowMenu: null, flash: null
};

function cvRelHiddenEl(kind) {
  return jsField("cvRel" + REL_CAP[kind] + "Json");
}

function parseCvRelSeed(kind) {
  const seed = $("#cvRelSeed");
  if (!seed) return [];
  const seen = new Set();
  const out = [];
  seed.querySelectorAll('[data-kind="' + kind + '"]').forEach((el) => {
    const id = el.getAttribute("data-id") || "";
    const key = id.toLowerCase();
    if (!id || seen.has(key)) return;
    seen.add(key);
    out.push({ id: id, label: el.getAttribute("data-label") || id });
  });
  return out;
}

function parseCvRelFromDom(kind) {
  const box = $("#cvRelRows");
  if (!box) return [];
  const seen = new Set();
  const out = [];
  box.querySelectorAll('.re-row[data-kind="' + kind + '"]').forEach((el) => {
    const id = el.getAttribute("data-id") || "";
    const key = id.toLowerCase();
    if (!id || seen.has(key)) return;
    seen.add(key);
    out.push({ id: id, label: el.getAttribute("data-label") || el.textContent.trim() || id });
  });
  return out;
}

function parseCvRelKind(kind) {
  const fromDom = parseCvRelFromDom(kind);
  if (fromDom.length) return fromDom;
  return parseCvRelJson(kind);
}

function parseCvRelJson(kind) {
  const hidden = cvRelHiddenEl(kind);
  if (!hidden || hidden.value == null || String(hidden.value).trim() === "") {
    return parseCvRelSeed(kind);
  }
  try {
    const rows = JSON.parse(hidden.value);
    if (!Array.isArray(rows)) return parseCvRelSeed(kind);
    const seen = new Set();
    const out = [];
    rows.forEach((row) => {
      const id = row && row.id ? String(row.id) : "";
      const key = id.toLowerCase();
      if (!id || seen.has(key)) return;
      seen.add(key);
      out.push({ id: id, label: row.label || id });
    });
    return out;
  } catch (ex) {
    return parseCvRelSeed(kind);
  }
}

function syncCvRelHidden() {
  ["bt", "nt", "rt"].forEach((kind) => {
    const el = cvRelHiddenEl(kind);
    if (el) {
      el.value = JSON.stringify((cvRelState[kind] || []).map((item) => ({
        id: item.id,
        label: item.label || item.id
      })));
    }
  });
}

function seedConceptRelFromHidden() {
  const editor = $("#cvRelEditor");
  if (!editor) {
    cvRelState.bound = null;
    cvRelState.painted = false;
    cvRelState.added = {};
    cvRelState.rowMenu = null;
    return;
  }
  const box = $("#cvRelRows");
  if (cvRelState.bound === editor && cvRelState.painted && box && box.children.length) return;
  cvRelState.bt = parseCvRelKind("bt");
  cvRelState.nt = parseCvRelKind("nt");
  cvRelState.rt = parseCvRelKind("rt");
  cvRelState.kind = "nt";
  cvRelState.hits = [];
  cvRelState.added = {};
  cvRelState.rowMenu = null;
  cvRelState.bound = editor;
  cvRelState.painted = true;
  const q = $("#cvRelQ");
  if (q) q.value = "";
  hideCvRelDrop();
  setCvRelMsg("");
  paintCvRel();
  paintCvRelKind();
  syncCvRelHidden();
}

function cvRelNorm(value) {
  return String(value || "").trim().toLowerCase();
}

function cvRelItemMatch(item, id, label) {
  const itemId = cvRelNorm(item && item.id);
  const itemLabel = cvRelNorm(item && item.label);
  const key = cvRelNorm(id);
  const name = cvRelNorm(label);
  return !!(key && (itemId === key || itemLabel === key)) || !!(name && itemLabel === name);
}

function cvRelKindLabel(kind) {
  const editor = $("#cvRelEditor");
  const fromDom = editor && editor.getAttribute("data-kind-" + kind);
  if (fromDom) return fromDom;
  return (CV_REL_META[kind] || {}).code || kind;
}

function cvRelKindCode(kind) {
  const editor = $("#cvRelEditor");
  const fromDom = editor && editor.getAttribute("data-code-" + kind);
  if (fromDom) return fromDom;
  return (CV_REL_META[kind] || {}).code || kind;
}

function cvRelMsg(key, fallback, vars) {
  const editor = $("#cvRelEditor");
  let text = (editor && editor.getAttribute("data-" + key)) || fallback;
  (vars || []).forEach((value, index) => {
    text = text.split("{" + index + "}").join(value == null ? "" : String(value));
  });
  return text;
}

function cvRelListsWithout(skip) {
  const out = {
    bt: (cvRelState.bt || []).slice(),
    nt: (cvRelState.nt || []).slice(),
    rt: (cvRelState.rt || []).slice()
  };
  if (skip && skip.kind && out[skip.kind]) {
    out[skip.kind] = out[skip.kind].filter((_, index) => index !== Number(skip.index));
  }
  return out;
}

function cvRelAncestorKeys() {
  const keys = new Set();
  document.querySelectorAll("#viewLive .cv.is-on .cv-path .path-link").forEach((link) => {
    const id = cvRelNorm(link.getAttribute("data-id"));
    const label = cvRelNorm(link.textContent);
    if (id) keys.add(id);
    if (label) keys.add(label);
  });
  (cvRelState.bt || []).forEach((item) => {
    const id = cvRelNorm(item.id);
    const label = cvRelNorm(item.label);
    if (id) keys.add(id);
    if (label) keys.add(label);
  });
  return keys;
}

function checkCvRel(kind, id, label, skip) {
  const name = String(label || id || "").trim();
  const key = String(id || name).trim();
  const selfId = cvRelNorm(currentConceptId());
  const cv = document.querySelector("#viewLive .cv.is-on");
  const selfPref = cvRelNorm(cv && cv.getAttribute("data-pref"));
  if (selfId && (cvRelNorm(key) === selfId || cvRelNorm(name) === selfId
      || (selfPref && (cvRelNorm(key) === selfPref || cvRelNorm(name) === selfPref)))) {
    return { level: "error", msg: cvRelMsg("self", "Un concept ne peut pas être en relation avec lui-même.") };
  }
  const lists = cvRelListsWithout(skip);
  for (let i = 0; i < 3; i += 1) {
    const other = ["bt", "nt", "rt"][i];
    const hit = (lists[other] || []).find((item) => cvRelItemMatch(item, key, name));
    if (!hit) continue;
    if (other === kind) {
      return { level: "error", msg: cvRelMsg("exists", "Cette relation existe déjà.") };
    }
    return {
      level: "error",
      msg: cvRelMsg(
        "other",
        "« {0} » est déjà {1} de ce concept — une même paire ne peut pas porter deux relations.",
        [hit.label || name, cvRelKindLabel(other).toLowerCase()]
      )
    };
  }
  if (kind === "nt") {
    const ancestors = cvRelAncestorKeys();
    if (ancestors.has(cvRelNorm(key)) || ancestors.has(cvRelNorm(name))) {
      return {
        level: "error",
        msg: cvRelMsg(
          "loop-nt",
          "« {0} » est un ancêtre de ce concept : le mettre en spécifique créerait une boucle.",
          [name]
        )
      };
    }
  }
  if (kind === "bt" && (lists.nt || []).some((item) => cvRelItemMatch(item, key, name))) {
    return {
      level: "error",
      msg: cvRelMsg(
        "loop-bt",
        "« {0} » est déjà un spécifique de ce concept : inverser la relation créerait une boucle.",
        [name]
      )
    };
  }
  if (kind === "bt" && (lists.bt || []).length >= 1) {
    return {
      level: "warn",
      msg: cvRelMsg("poly", "Ce concept aura plusieurs génériques (polyhiérarchie). Autorisé, mais à vérifier.")
    };
  }
  return { level: "ok" };
}

function cvRelAlready(id, label) {
  return checkCvRel(cvRelState.kind || "nt", id, label).level === "error";
}

function paintCvRelKind() {
  const btn = $("#cvRelKindBtn");
  const menu = $("#cvRelKindMenu");
  const kind = cvRelState.kind;
  if (btn) {
    btn.className = "re-pill rel-" + kind;
    btn.innerHTML = cvRelKindCode(kind) + '<span class="re-caret" aria-hidden="true"></span>';
  }
  if (menu) {
    menu.querySelectorAll("[data-kind]").forEach((mi) => {
      mi.classList.toggle("on", mi.getAttribute("data-kind") === kind);
    });
  }
}

function setCvRelKindMenu(open) {
  const menu = $("#cvRelKindMenu");
  const btn = $("#cvRelKindBtn");
  if (menu) menu.hidden = !open;
  if (btn) btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function setCvRelKind(kind) {
  if (!CV_REL_META[kind]) return;
  cvRelState.kind = kind;
  cvRelState.rowMenu = null;
  paintCvRelKind();
  setCvRelKindMenu(false);
  paintCvRel();
}

function setCvRelRowMenu(key) {
  cvRelState.rowMenu = key || null;
  paintCvRel();
}

function cvRelKindMenuHtml(currentKind, act, extraAttrs, open) {
  return '<div class="re-menu" role="listbox"' + (open ? "" : " hidden=\"hidden\"") + ">"
    + ["bt", "nt", "rt"].map((kind) => {
      const on = kind === currentKind ? " on" : "";
      return '<button type="button" class="re-mi' + on + '" role="option" data-act="' + act
        + '" data-kind="' + kind + '"' + (extraAttrs || "") + ">"
        + '<span class="rel-code rel-' + kind + '">' + cvRelKindCode(kind) + "</span>"
        + escapeHtml(cvRelKindLabel(kind))
        + "</button>";
    }).join("")
    + "</div>";
}

function cvRelRowHtml(row, remove) {
  const code = cvRelKindCode(row.k);
  const label = escapeHtml(row.item.label || row.item.id || "");
  const rawId = String(row.item.id || "");
  const key = rawId.toLowerCase();
  const saved = cvRelState.added && cvRelState.added[row.k + ":" + key] ? " is-saved" : "";
  const menuKey = row.k + ":" + row.i;
  const open = cvRelState.rowMenu === menuKey;
  return '<div class="re-row' + saved + '" data-kind="' + row.k
    + '" data-id="' + escapeHtml(rawId) + '" data-label="' + label + '">'
    + '<span class="re-type">'
    + '<button type="button" class="re-pill rel-' + row.k + '" data-act="cv-rel-row-kind-toggle"'
    + ' data-kind="' + row.k + '" data-index="' + row.i + '"'
    + ' aria-haspopup="listbox" aria-expanded="' + (open ? "true" : "false") + '">'
    + code + '<span class="re-caret" aria-hidden="true"></span></button>'
    + cvRelKindMenuHtml(row.k, "cv-rel-row-kind",
        ' data-from="' + row.k + '" data-index="' + row.i + '"', open)
    + "</span>"
    + '<span class="re-lbl" title="' + label + '">' + label + "</span>"
    + '<button type="button" class="re-x" data-act="cv-rel-remove" data-kind="' + row.k
    + '" data-index="' + row.i + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
    + "</div>";
}

function paintCvRelGroup(kind, remove) {
  const items = cvRelState[kind] || [];
  const title = escapeHtml(cvRelKindLabel(kind));
  const count = items.length ? '<span class="rel-count">' + items.length + "</span>" : "";
  const rows = items.length
    ? items.map((item, i) => cvRelRowHtml({ k: kind, i: i, item: item }, remove)).join("")
    : '<div class="re-empty"><span class="muted">—</span></div>';
  return '<div class="re-group" data-kind="' + kind + '">'
    + '<div class="re-group-h"><span class="rel-code rel-' + kind + '">' + cvRelKindCode(kind) + "</span>"
    + title + count + "</div>"
    + rows
    + "</div>";
}

function paintCvRel() {
  const box = $("#cvRelRows");
  const editor = $("#cvRelEditor");
  if (!box) return;
  const remove = (editor && editor.getAttribute("data-remove")) || "Retirer";
  box.innerHTML = ["bt", "nt", "rt"].map((kind) => paintCvRelGroup(kind, remove)).join("");
}

function setCvRelMsg(text, level) {
  const el = $("#cvRelMsg");
  if (!el) return;
  el.textContent = text || "";
  el.hidden = !text;
  el.className = "re-msg" + (text ? " re-" + (level || "error") : "");
}

function hideCvRelDrop() {
  const drop = $("#cvRelDrop");
  const empty = $("#cvRelEmpty");
  if (drop) {
    drop.hidden = true;
    drop.innerHTML = "";
  }
  if (empty) empty.hidden = true;
}

function flashCvRelRow(kind, id) {
  const key = String(id || "").toLowerCase();
  cvRelState.added = {};
  if (kind && key) cvRelState.added[kind + ":" + key] = true;
  if (cvRelState.flash) clearTimeout(cvRelState.flash);
  cvRelState.flash = setTimeout(() => {
    cvRelState.added = {};
    paintCvRel();
  }, 1200);
}

function addCvRel(id, label) {
  const kind = cvRelState.kind || "nt";
  const name = String(label || id || "").trim();
  const key = String(id || name).trim();
  if (!name) return;
  const check = checkCvRel(kind, key, name);
  if (check.level === "error") {
    setCvRelMsg(check.msg, "error");
    return;
  }
  cvRelState[kind].push({ id: key, label: name });
  const q = $("#cvRelQ");
  if (q) q.value = "";
  cvRelState.hits = [];
  cvRelState.rowMenu = null;
  hideCvRelDrop();
  setCvRelMsg(check.level === "warn" ? check.msg : "", check.level);
  syncCvRelHidden();
  flashCvRelRow(kind, key);
  paintCvRel();
}

function retypeCvRel(fromKind, index, toKind) {
  if (!CV_REL_META[toKind] || !cvRelState[fromKind]) return;
  const item = cvRelState[fromKind][Number(index)];
  if (!item) return;
  if (fromKind === toKind) {
    cvRelState.rowMenu = null;
    paintCvRel();
    return;
  }
  const check = checkCvRel(toKind, item.id, item.label, { kind: fromKind, index: Number(index) });
  if (check.level === "error") {
    setCvRelMsg(check.msg, "error");
    cvRelState.rowMenu = null;
    paintCvRel();
    return;
  }
  cvRelState[fromKind].splice(Number(index), 1);
  cvRelState[toKind].push(item);
  cvRelState.rowMenu = null;
  setCvRelMsg(check.level === "warn" ? check.msg : "", check.level);
  syncCvRelHidden();
  flashCvRelRow(toKind, item.id);
  paintCvRel();
}

function removeCvRel(kind, index) {
  if (!cvRelState[kind]) return;
  cvRelState[kind].splice(Number(index), 1);
  cvRelState.rowMenu = null;
  setCvRelMsg("");
  syncCvRelHidden();
  paintCvRel();
}

function searchCvRel(q) {
  const seq = ++cvRelState.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    q: q,
    excludeId: currentConceptId() || ""
  });
  fetch(ctx + "/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== cvRelState.seq) return;
    const selfId = cvRelNorm(currentConceptId());
    cvRelState.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id) : "";
      return id && cvRelNorm(id) !== selfId;
    }).slice(0, 8);
    paintCvRelHits(q);
  }).catch(() => {
    if (seq !== cvRelState.seq) return;
    cvRelState.hits = [];
    paintCvRelHits(q);
  });
}

function paintCvRelHits(q) {
  const drop = $("#cvRelDrop");
  const empty = $("#cvRelEmpty");
  if (!drop) return;
  if (cvRelState.hits.length) {
    drop.hidden = false;
    if (empty) empty.hidden = true;
    drop.innerHTML = cvRelState.hits.map((hit) => (
      '<button type="button" class="re-hit" data-act="cv-rel-hit" data-id="'
      + escapeHtml(hit.id || "") + '" data-label="' + escapeHtml(hit.label || hit.id || "") + '">'
      + '<span class="re-hit-l">' + escapeHtml(hit.label || hit.id || "") + "</span>"
      + "</button>"
    )).join("");
    return;
  }
  drop.hidden = true;
  drop.innerHTML = "";
  if (empty) empty.hidden = String(q || "").trim().length < 2;
}

function scheduleCvRelSearch() {
  const q = ($("#cvRelQ") && $("#cvRelQ").value.trim()) || "";
  setCvRelMsg("");
  if (cvRelState.timer) clearTimeout(cvRelState.timer);
  if (q.length < 2) {
    cvRelState.seq += 1;
    cvRelState.hits = [];
    hideCvRelDrop();
    return;
  }
  cvRelState.timer = setTimeout(() => searchCvRel(q), 220);
}

function shownRelRows(kind) {
  const cap = REL_CAP[kind];
  return $$("#cvRel" + cap + "Menu .ac-row.is-shown");
}

function relItemHtml(kind, item) {
  const on = isRelSelected(kind, item.id);
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="rel-pick-item" data-kind="' + kind
    + '" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label) + '">'
    + '<span class="ac-ico">◇</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function highlightRelQuery(kind, query) {
  const cap = REL_CAP[kind];
  $$("#cvRel" + cap + "Menu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#cvRel" + cap + "Menu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function setRelAcIdx(kind, index) {
  const rows = shownRelRows(kind);
  relPickers[kind].active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function syncRelCombo(kind) {
  const els = relEls(kind);
  const open = !!(els.menu && !els.menu.hidden);
  if (els.pick) els.pick.classList.toggle("is-open", open);
  if (els.combo) els.combo.classList.toggle("is-open", open);
  if (els.query) els.query.setAttribute("aria-expanded", open ? "true" : "false");
}

function renderRelMenu(kind) {
  const els = relEls(kind);
  if (!els.menu || !els.list) return;
  const st = relPickers[kind];
  const raw = st.query || "";
  const hits = st.hits || [];
  const limit = st.showAll ? hits.length : REL_AC_PAGE;
  els.list.innerHTML = hits.map((item) => relItemHtml(kind, item)).join("");
  const rows = $$("#cvRel" + REL_CAP[kind] + "List .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  els.menu.classList.toggle("has-hits", hits.length > 0);
  els.menu.classList.toggle("is-empty", hits.length === 0);
  if (els.empty) {
    els.empty.textContent = raw
      ? ("Aucun concept pour « " + raw + " »")
      : "Tapez pour rechercher un concept…";
  }
  if (els.count) els.count.textContent = String(hits.length);
  if (els.more) els.more.hidden = st.showAll || hits.length <= REL_AC_PAGE;
  st.active = -1;
  highlightRelQuery(kind, raw);
  syncRelCombo(kind);
}

function closeRelPicker(kind) {
  const els = relEls(kind);
  if (els.combo) els.combo.classList.remove("is-focused");
  if (!els.menu || els.menu.hidden) return false;
  els.menu.hidden = true;
  relPickers[kind].showAll = false;
  relPickers[kind].active = -1;
  syncRelCombo(kind);
  return true;
}

function closeAllRelPickers(except) {
  REL_KINDS.forEach((kind) => {
    if (kind === except) return;
    closeRelPicker(kind);
  });
}

function openRelMenu(kind) {
  const els = relEls(kind);
  if (!els.menu) return;
  closeAllRelPickers(kind);
  closeCrelPicker();
  els.menu.hidden = false;
  syncRelCombo(kind);
  renderRelMenu(kind);
}

function toggleRelMenu(kind) {
  if (!kind || !relPickers[kind]) return;
  const els = relEls(kind);
  if (els.menu && !els.menu.hidden) {
    closeRelPicker(kind);
    return;
  }
  if (els.query) els.query.focus();
  const q = els.query ? els.query.value.trim() : "";
  if (q) scheduleRelSearch(kind);
  else searchRel(kind, "%");
}

function toggleRelPick(kind, id, label) {
  if (!kind || !id) return;
  const removing = isRelSelected(kind, id);
  if (removing) {
    const key = String(id).toLowerCase();
    relPickers[kind].selected = relPickers[kind].selected.filter(
      (item) => String(item.id).toLowerCase() !== key
    );
  } else {
    if (otherRelSelectedIds(kind).has(String(id).toLowerCase())) return;
    relPickers[kind].selected.push({ id: id, label: label || id });
  }
  syncRelHidden(kind);
  const els = relEls(kind);
  if (!removing && els.query) {
    els.query.value = "";
    syncRelClear(kind);
    relPickers[kind].hits = [];
    relPickers[kind].query = "";
    closeRelPicker(kind);
    els.query.focus();
    return;
  }
  if (els.menu && !els.menu.hidden) renderRelMenu(kind);
}

function searchRel(kind, q) {
  const st = relPickers[kind];
  const seq = ++st.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    q: q,
    excludeId: currentConceptId()
  });
  fetch(ctx + "/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== st.seq) return;
    const exclude = otherRelSelectedIds(kind);
    const els = relEls(kind);
    const typed = els.query ? els.query.value.trim() : "";
    st.query = typed || (q === "%" ? "" : q);
    st.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id).toLowerCase() : "";
      return id && !exclude.has(id);
    });
    openRelMenu(kind);
  }).catch(() => {
    if (seq !== st.seq) return;
    const els = relEls(kind);
    const typed = els.query ? els.query.value.trim() : "";
    st.query = typed || (q === "%" ? "" : q);
    st.hits = [];
    openRelMenu(kind);
  });
}

function scheduleRelSearch(kind) {
  if (!kind || !relPickers[kind]) return;
  const st = relPickers[kind];
  const els = relEls(kind);
  const q = els.query ? els.query.value.trim() : "";
  st.showAll = false;
  syncRelClear(kind);
  if (st.timer) clearTimeout(st.timer);
  if (!q) {
    st.hits = [];
    st.query = "";
    st.seq += 1;
    closeRelPicker(kind);
    return;
  }
  st.timer = setTimeout(() => searchRel(kind, q), REL_DEBOUNCE);
}

function pickActiveRel(kind) {
  const rows = shownRelRows(kind);
  const row = relPickers[kind].active >= 0 ? rows[relPickers[kind].active] : rows[0];
  if (!row) return false;
  toggleRelPick(kind, row.getAttribute("data-id"), row.getAttribute("data-label"));
  return true;
}

var CREL_AC_PAGE = 7;
var crelPicker = { selected: [], hits: [], query: "", active: -1, showAll: false, timer: null, seq: 0 };

function crelHidden() {
  return document.querySelector("[id$='cvCrelJson']");
}

function isCrelSelected(id) {
  const key = String(id || "").toLowerCase();
  return crelPicker.selected.some((item) => String(item.id).toLowerCase() === key);
}

function syncCrelHidden() {
  const hidden = crelHidden();
  if (hidden) hidden.value = JSON.stringify(crelPicker.selected.map((item) => ({
    id: item.id,
    label: item.label || item.id,
    role: item.role || "",
    roleLabel: item.roleLabel || item.role || "",
    reciprocal: !!item.reciprocal
  })));
  const chips = $("#cvCrelChips");
  if (!chips) return;
  chips.hidden = crelPicker.selected.length === 0;
  chips.innerHTML = crelPicker.selected.map((item) => {
    const role = item.roleLabel || item.role || "";
    const text = role ? (role + " · " + (item.label || item.id)) : (item.label || item.id);
    return '<button type="button" class="coll-chip" data-act="crel-chip-remove" data-id="'
      + escapeHtml(item.id) + '" title="Retirer">'
      + escapeHtml(text)
      + '<span aria-hidden="true">×</span></button>';
  }).join("");
}

function syncCrelClear() {
  const input = $("#cvCrelQuery");
  const clear = $("#cvCrelClear");
  if (clear) clear.hidden = !(input && input.value);
}

function seedCrelSelection() {
  const hidden = crelHidden();
  crelPicker.selected = [];
  if (!hidden || !hidden.value) {
    syncCrelHidden();
    return;
  }
  try {
    const rows = JSON.parse(hidden.value);
    if (!Array.isArray(rows)) return;
    const seen = new Set();
    rows.forEach((row) => {
      const id = row && row.id ? String(row.id) : "";
      const key = id.toLowerCase();
      if (!id || seen.has(key)) return;
      seen.add(key);
      crelPicker.selected.push({
        id: id,
        label: row.label || id,
        role: row.role || "",
        roleLabel: row.roleLabel || row.role || "",
        reciprocal: !!row.reciprocal
      });
    });
  } catch (ex) {
    crelPicker.selected = [];
  }
  syncCrelHidden();
}

function bindCrelPicker() {
  if (!$("#cvCrelPick")) {
    closeCrelPicker();
    return;
  }
  if (crelPicker.timer) {
    clearTimeout(crelPicker.timer);
    crelPicker.timer = null;
  }
  crelPicker.hits = [];
  crelPicker.query = "";
  crelPicker.active = -1;
  crelPicker.showAll = false;
  seedCrelSelection();
  syncCrelClear();
}

function shownCrelRows() {
  return $$("#cvCrelMenu .ac-row.is-shown");
}

function crelItemHtml(item) {
  const on = isCrelSelected(item.id);
  const type = item.type ? '<span class="ac-type">' + escapeHtml(item.type) + "</span>" : "";
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="crel-pick-item" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label)
    + '" data-type="' + escapeHtml(item.type || "") + '">'
    + '<span class="ac-ico">◇</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + (type ? " " + type : "") + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function highlightCrelQuery(query) {
  $$("#cvCrelMenu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#cvCrelMenu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function setCrelAcIdx(index) {
  const rows = shownCrelRows();
  crelPicker.active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function syncCrelCombo() {
  const menu = $("#cvCrelMenu");
  const input = $("#cvCrelQuery");
  const combo = $("#cvCrelCombo");
  const pick = $("#cvCrelPick");
  const open = !!(menu && !menu.hidden);
  if (pick) pick.classList.toggle("is-open", open);
  if (combo) combo.classList.toggle("is-open", open);
  if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
}

function renderCrelMenu() {
  const menu = $("#cvCrelMenu");
  const list = $("#cvCrelList");
  if (!menu || !list) return;
  const raw = crelPicker.query || "";
  const hits = crelPicker.hits || [];
  const limit = crelPicker.showAll ? hits.length : CREL_AC_PAGE;
  list.innerHTML = hits.map((item) => crelItemHtml(item)).join("");
  const rows = $$("#cvCrelList .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", hits.length === 0);
  const empty = $("#cvCrelEmpty");
  if (empty) {
    empty.textContent = raw
      ? ("Aucun concept pour « " + raw + " »")
      : "Tapez pour rechercher un concept…";
  }
  const count = $("#cvCrelCount");
  if (count) count.textContent = String(hits.length);
  const more = $("#cvCrelMore");
  if (more) more.hidden = crelPicker.showAll || hits.length <= CREL_AC_PAGE;
  crelPicker.active = -1;
  highlightCrelQuery(raw);
  syncCrelCombo();
}

function closeCrelPicker() {
  const menu = $("#cvCrelMenu");
  const combo = $("#cvCrelCombo");
  if (combo) combo.classList.remove("is-focused");
  if (!menu || menu.hidden) return false;
  menu.hidden = true;
  crelPicker.showAll = false;
  crelPicker.active = -1;
  syncCrelCombo();
  return true;
}

function openCrelMenu() {
  const menu = $("#cvCrelMenu");
  if (!menu) return;
  closeAllRelPickers();
  menu.hidden = false;
  syncCrelCombo();
  renderCrelMenu();
}

function toggleCrelMenu() {
  const menu = $("#cvCrelMenu");
  if (menu && !menu.hidden) {
    closeCrelPicker();
    return;
  }
  const input = $("#cvCrelQuery");
  if (input) input.focus();
  const q = input ? input.value.trim() : "";
  if (q) scheduleCrelSearch();
  else {
    crelPicker.hits = [];
    crelPicker.query = "";
    openCrelMenu();
  }
}

function toggleCrelPick(id, label, type) {
  if (!id) return;
  const removing = isCrelSelected(id);
  if (removing) {
    const key = String(id).toLowerCase();
    crelPicker.selected = crelPicker.selected.filter((item) => String(item.id).toLowerCase() !== key);
  } else {
    const selfId = (currentConceptId() || "").toLowerCase();
    if (selfId && String(id).toLowerCase() === selfId) return;
    crelPicker.selected.push({
      id: id,
      label: label || id,
      role: type || "",
      roleLabel: type || "",
      reciprocal: false
    });
  }
  syncCrelHidden();
  const input = $("#cvCrelQuery");
  if (!removing && input) {
    input.value = "";
    syncCrelClear();
    crelPicker.hits = [];
    crelPicker.query = "";
    closeCrelPicker();
    input.focus();
    return;
  }
  const menu = $("#cvCrelMenu");
  if (menu && !menu.hidden) renderCrelMenu();
}

function searchCrel(q) {
  const seq = ++crelPicker.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    q: q,
    excludeId: currentConceptId(),
    customOnly: "true"
  });
  fetch(ctx + "/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== crelPicker.seq) return;
    const selfId = (currentConceptId() || "").toLowerCase();
    crelPicker.query = q;
    crelPicker.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id).toLowerCase() : "";
      return id && id !== selfId;
    });
    openCrelMenu();
  }).catch(() => {
    if (seq !== crelPicker.seq) return;
    crelPicker.query = q;
    crelPicker.hits = [];
    openCrelMenu();
  });
}

function scheduleCrelSearch() {
  const input = $("#cvCrelQuery");
  const q = input ? input.value.trim() : "";
  crelPicker.showAll = false;
  syncCrelClear();
  if (crelPicker.timer) clearTimeout(crelPicker.timer);
  if (!q) {
    crelPicker.hits = [];
    crelPicker.query = "";
    crelPicker.seq += 1;
    closeCrelPicker();
    return;
  }
  crelPicker.timer = setTimeout(() => searchCrel(q), REL_DEBOUNCE);
}

function pickActiveCrel() {
  const rows = shownCrelRows();
  const row = crelPicker.active >= 0 ? rows[crelPicker.active] : rows[0];
  if (!row) return false;
  toggleCrelPick(row.getAttribute("data-id"), row.getAttribute("data-label"), row.getAttribute("data-type"));
  return true;
}

