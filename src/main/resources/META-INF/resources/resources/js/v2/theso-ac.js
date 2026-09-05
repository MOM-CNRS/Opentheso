/**
 * OpenTheso V2 — autocomplete thésaurus (copie / déplacement).
 */
"use strict";

const THESO_AC_PAGE = 7;
const thesoAcState = new WeakMap();
let ignoreThesoClick = false;

function thesoAcSt(root) {
  let st = thesoAcState.get(root);
  if (!st) {
    st = { hits: [], query: "", active: -1, showAll: false, seq: 0, timer: 0 };
    thesoAcState.set(root, st);
  }
  return st;
}

function thesoAcEls(root) {
  if (!root) return {};
  return {
    root,
    menu: root.querySelector(".ac"),
    list: root.querySelector(".coll-ac-list"),
    empty: root.querySelector(".ac-empty"),
    more: root.querySelector(".ac-footer"),
    count: root.querySelector(".cv-theso-ac-count"),
    query: root.querySelector(".cv-theso-ac-q"),
    clear: root.querySelector(".search-clear"),
    combo: root.querySelector(".coll-pick-field"),
    chips: root.querySelector(".coll-pick-chips"),
    idInput: root.querySelector(".cv-theso-parent-id, [id$='ParentId']"),
    labelInput: root.querySelector(".cv-theso-parent-label, [id$='ParentLabel']")
  };
}

function thesoAcBody(root) {
  return root && root.closest(".cv-dlg-body");
}

function thesoEnter(el) {
  if (!el || el.classList.contains("is-off")) return;
  replayAnim(el, "is-enter");
}

function revealThesoAc(root) {
  if (!root) return;
  const body = thesoAcBody(root);
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      if (body) {
        body.scrollTo({ top: body.scrollHeight, behavior: "smooth" });
        return;
      }
      if (root.scrollIntoView) root.scrollIntoView({ block: "end", behavior: "smooth" });
    });
  });
}

function selectedThesoParentId(root) {
  const els = thesoAcEls(root);
  return ((els.idInput && els.idInput.value) || "").trim();
}

function syncThesoAcCombo(root) {
  const els = thesoAcEls(root);
  const open = !!(els.menu && !els.menu.hidden);
  if (root) root.classList.toggle("is-open", open);
  if (els.combo) els.combo.classList.toggle("is-open", open);
  if (els.query) els.query.setAttribute("aria-expanded", open ? "true" : "false");
}

function syncThesoAcClear(root) {
  const els = thesoAcEls(root);
  if (els.clear) els.clear.hidden = !(els.query && els.query.value);
}

function syncThesoParentChip(root) {
  const els = thesoAcEls(root);
  if (!els.chips) return;
  const id = selectedThesoParentId(root);
  const label = ((els.labelInput && els.labelInput.value) || id).trim();
  els.chips.hidden = !id;
  els.chips.innerHTML = id
    ? '<button type="button" class="coll-chip" data-act="theso-parent-chip" title="Retirer">'
      + escapeHtml(label) + '<span aria-hidden="true">×</span></button>'
    : "";
}

function shownThesoAcRows(root) {
  return root ? $$(".ac-row.is-shown", root) : [];
}

function setThesoAcIdx(root, index) {
  const rows = shownThesoAcRows(root);
  const st = thesoAcSt(root);
  st.active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function closeThesoAc(root) {
  if (!root) return false;
  const els = thesoAcEls(root);
  const st = thesoAcSt(root);
  if (els.combo) els.combo.classList.remove("is-focused");
  if (!els.menu || els.menu.hidden) return false;
  els.menu.hidden = true;
  st.showAll = false;
  st.active = -1;
  syncThesoAcCombo(root);
  return true;
}

function closeAnyThesoAc() {
  let closed = false;
  $$(".cv-theso-ac").forEach((root) => {
    if (closeThesoAc(root)) closed = true;
  });
  return closed;
}

function highlightThesoAcQuery(root, query) {
  const els = thesoAcEls(root);
  if (!els.menu) return;
  $$("mark.hl", els.menu).forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$(".ac-row.is-shown .ac-pref", els.menu).forEach((el) => wrapQueryIn(el, query));
}

function thesoAcItemHtml(root, item) {
  const id = item && item.id ? String(item.id) : "";
  const label = item && item.label ? String(item.label) : id;
  const on = id && id.toLowerCase() === selectedThesoParentId(root).toLowerCase();
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="theso-parent-pick" data-id="' + escapeHtml(id)
    + '" data-label="' + escapeHtml(label) + '">'
    + '<span class="ac-ico">◇</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(label) + "</span></span>"
    + (id ? '<span class="ac-notation">' + escapeHtml(id) + "</span>" : "")
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function renderThesoAcMenu(root) {
  const els = thesoAcEls(root);
  const st = thesoAcSt(root);
  if (!els.menu || !els.list) return;
  const raw = st.query || "";
  const hits = st.hits;
  const limit = st.showAll ? hits.length : THESO_AC_PAGE;
  els.list.innerHTML = hits.map((item) => thesoAcItemHtml(root, item)).join("");
  $$(".ac-row", els.list).forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
  });
  els.menu.classList.toggle("has-hits", hits.length > 0);
  els.menu.classList.toggle("is-empty", !!raw && hits.length === 0);
  if (els.empty) {
    els.empty.textContent = raw
      ? ((root.getAttribute("data-empty") || "") + (raw ? " « " + raw + " »" : ""))
      : (root.getAttribute("data-hint") || "");
  }
  if (els.count) els.count.textContent = String(hits.length);
  if (els.more) els.more.hidden = st.showAll || hits.length <= THESO_AC_PAGE;
  st.active = -1;
  highlightThesoAcQuery(root, raw);
  syncThesoAcCombo(root);
}

function openThesoAcMenu(root, showAll) {
  const els = thesoAcEls(root);
  const st = thesoAcSt(root);
  if (!els.menu) return;
  if (showAll) st.showAll = true;
  els.menu.hidden = false;
  syncThesoAcCombo(root);
  renderThesoAcMenu(root);
  revealThesoAc(root);
}

function toggleThesoAcMenu(root) {
  const els = thesoAcEls(root);
  if (els.menu && !els.menu.hidden) {
    closeThesoAc(root);
    return;
  }
  if (els.query) els.query.focus();
  searchThesoAc(root, "%", true);
}

function pickThesoParent(root, id, label) {
  if (!root || !id) return;
  const els = thesoAcEls(root);
  if (!els.idInput) return;
  els.idInput.value = id;
  if (els.labelInput) els.labelInput.value = label || id;
  if (els.query) els.query.value = "";
  const st = thesoAcSt(root);
  st.showAll = false;
  st.hits = [];
  st.query = "";
  syncThesoAcClear(root);
  syncThesoParentChip(root);
  closeThesoAc(root);
  syncThesoSubmit(root.closest(".confirm-modal"));
}

function clearThesoParent(root) {
  if (!root) return;
  const els = thesoAcEls(root);
  if (els.idInput) els.idInput.value = "";
  if (els.labelInput) els.labelInput.value = "";
  syncThesoParentChip(root);
  renderThesoAcMenu(root);
  syncThesoSubmit(root.closest(".confirm-modal"));
  if (els.query) els.query.focus();
}

function clearThesoQuery(root) {
  const els = thesoAcEls(root);
  if (els.query) els.query.value = "";
  const st = thesoAcSt(root);
  st.hits = [];
  st.query = "";
  st.showAll = false;
  syncThesoAcClear(root);
  closeThesoAc(root);
  if (els.query) els.query.focus();
}

function pickActiveThesoAc(root) {
  const rows = shownThesoAcRows(root);
  const st = thesoAcSt(root);
  const row = st.active >= 0 ? rows[st.active] : rows[0];
  if (!row) return false;
  pickThesoParent(root, row.getAttribute("data-id"), row.getAttribute("data-label"));
  return true;
}

function searchThesoAc(root, q, showAll) {
  const st = thesoAcSt(root);
  const seq = ++st.seq;
  const theso = (root.getAttribute("data-theso") || "").trim();
  const lang = (root.getAttribute("data-lang") || "").trim();
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({ thesaurusId: theso, q: q });
  if (lang) params.set("lang", lang);
  fetch(ctx + "/v2/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== st.seq) return;
    const typed = (thesoAcEls(root).query && thesoAcEls(root).query.value.trim()) || "";
    st.query = typed || (q === "%" ? "" : q);
    st.hits = Array.isArray(items) ? items.filter((item) => item && item.id) : [];
    openThesoAcMenu(root, showAll);
  }).catch(() => {
    if (seq !== st.seq) return;
    const typed = (thesoAcEls(root).query && thesoAcEls(root).query.value.trim()) || "";
    st.query = typed || (q === "%" ? "" : q);
    st.hits = [];
    openThesoAcMenu(root, showAll);
  });
}

function scheduleThesoAc(root) {
  if (!root) return;
  const st = thesoAcSt(root);
  const els = thesoAcEls(root);
  const q = els.query ? els.query.value.trim() : "";
  st.showAll = false;
  syncThesoAcClear(root);
  if (st.timer) clearTimeout(st.timer);
  if (!q || !(root.getAttribute("data-theso") || "").trim()) {
    st.hits = [];
    st.query = "";
    st.seq += 1;
    closeThesoAc(root);
    return;
  }
  revealThesoAc(root);
  st.timer = setTimeout(() => searchThesoAc(root, q, false), 220);
}

function onCvThesoTargetAjax(data) {
  if (data.status !== "success") return;
  bindThesoTransferUi({ enterLive: true });
}
window.onCvThesoTargetAjax = onCvThesoTargetAjax;

function bindThesoTransferUi(opts) {
  $$(".cv-theso-ac").forEach((root) => {
    syncThesoParentChip(root);
    syncThesoAcClear(root);
  });
  if (opts && opts.enterLive) $$(".cv-dlg-theso-live").forEach(thesoEnter);
  $$(".confirm-modal.is-theso").forEach(syncThesoSubmit);
}

function thesoSubmitReady(modal) {
  if (!modal) return false;
  const select = modal.querySelector("select");
  if (!select || !String(select.value || "").trim()) return false;
  const dest = modal.querySelector(".cv-theso-dest-mode, [id$='DestMode']");
  const mode = dest && String(dest.value || "").trim();
  if (mode === "root") return true;
  if (mode === "parent") {
    const pid = modal.querySelector(".cv-theso-parent-id, [id$='ParentId']");
    return !!(pid && String(pid.value || "").trim());
  }
  return false;
}

function syncThesoSubmit(modal) {
  if (!modal) return;
  const go = modal.querySelector(".cv-theso-submit");
  if (!go) return;
  const on = thesoSubmitReady(modal);
  go.classList.toggle("is-off", !on);
  go.setAttribute("aria-disabled", on ? "false" : "true");
}

function setThesoDestMode(prefix, mode, modal) {
  const hidden = modal && modal.querySelector("[id$='" + prefix + "DestMode']");
  if (hidden) hidden.value = mode;
  if (modal) {
    modal.querySelectorAll("[data-act='theso-dest'][data-prefix='" + prefix + "']").forEach((btn) => {
      btn.classList.toggle("is-on", btn.getAttribute("data-mode") === mode);
    });
  }
  const box = document.getElementById(prefix + "ParentBox");
  if (box) {
    const show = mode === "parent";
    box.classList.toggle("is-off", !show);
    if (show) {
      thesoEnter(box);
      const q = box.querySelector(".cv-theso-ac-q");
      if (q) q.focus();
      revealThesoAc(box.querySelector(".cv-theso-ac"));
    } else {
      closeThesoAc(box.querySelector(".cv-theso-ac"));
    }
  }
  syncThesoSubmit(modal);
}

function setThesoIdType(modal, type) {
  const hidden = modal && modal.querySelector(".cv-theso-id-type");
  if (hidden) hidden.value = type;
  if (!modal) return;
  modal.querySelectorAll("[data-act='theso-id']").forEach((btn) => {
    btn.classList.toggle("is-on", btn.getAttribute("data-type") === type);
  });
  const warn = modal.querySelector(".cv-dlg-ark-warn");
  if (warn) {
    const show = type === "ark";
    warn.classList.toggle("is-off", !show);
    if (show) thesoEnter(warn);
  }
}

function onThesoAcKeydown(e) {
  if (!e.target.classList || !e.target.classList.contains("cv-theso-ac-q")) return false;
  const root = e.target.closest(".cv-theso-ac");
  if (!root) return false;
  const els = thesoAcEls(root);
  const open = !!(els.menu && !els.menu.hidden);
  const rows = shownThesoAcRows(root);
  const st = thesoAcSt(root);
  if (e.key === "ArrowDown") {
    e.preventDefault();
    if (!open) {
      if (e.target.value.trim()) scheduleThesoAc(root);
      else toggleThesoAcMenu(root);
    } else {
      setThesoAcIdx(root, Math.min(st.active + 1, Math.max(rows.length - 1, -1)));
    }
    return true;
  }
  if (e.key === "ArrowUp") {
    e.preventDefault();
    setThesoAcIdx(root, Math.max(st.active - 1, -1));
    return true;
  }
  if (e.key === "Enter") {
    e.preventDefault();
    e.stopPropagation();
    if (open && rows.length) pickActiveThesoAc(root);
    return true;
  }
  if (e.key === "Escape" && open) {
    e.preventDefault();
    e.stopImmediatePropagation();
    closeThesoAc(root);
    return true;
  }
  return false;
}

document.addEventListener("pointerdown", (e) => {
  const pick = e.target.closest("[data-act='theso-parent-pick']");
  if (pick) {
    e.preventDefault();
    e.stopPropagation();
    ignoreThesoClick = true;
    pickThesoParent(pick.closest(".cv-theso-ac"), pick.getAttribute("data-id"), pick.getAttribute("data-label"));
    return;
  }
  if (!e.target.closest(".cv-theso-ac")) closeAnyThesoAc();
}, true);
