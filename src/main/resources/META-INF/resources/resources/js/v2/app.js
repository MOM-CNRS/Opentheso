/**
 * OpenTheso V2 — câblage UI et démarrage.
 */
"use strict";

function closeThesaurus() {
  $$(".thesaurus-btn.is-on").forEach(b => {
    b.classList.remove("is-on");
    b.setAttribute("aria-expanded", "false");
  });
  const account = $("#menu-account");
  if (account) account.classList.remove("is-forgot");
}

function closeTreeLang() {
  const btn = $("#treeLangBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function closeCvCtx() {
  const btn = $("#cvCtxBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function hideCvDialogs() {
  const ark = document.getElementById("cvDlgArkGen");
  if (ark && !ark.hidden && ark.classList.contains("is-busy")) return false;
  const refreshArk = ark && !ark.hidden && ark.classList.contains("is-done");
  let closed = false;
  $$(".confirm-overlay[id^='cvDlg']").forEach((el) => {
    if (!el.hidden) {
      el.hidden = true;
      closed = true;
    }
  });
  if (refreshArk) {
    const go = document.querySelector("[id$='cvArkAfterClose']");
    if (go) go.click();
  }
  return closed;
}

function onCvMenuAjax(data) {
  if (data.status !== "success") return;
  closeCvCtx();
  hideCvDialogs();
  const keep = document.querySelector("[data-cv-dlg]");
  const id = keep && keep.getAttribute("data-cv-dlg");
  if (keep) keep.remove();
  if (id) {
    const dlg = document.getElementById(id);
    if (dlg) {
      dlg.hidden = false;
      thesoEnter(dlg.querySelector(".cv-dlg-body"));
    }
  }
  bindThesoTransferUi();
}
window.onCvMenuAjax = onCvMenuAjax;

let arkProgTimer = 0;
let arkProgStarted = 0;
let arkFinishTimer = 0;
const ARK_MIN_MS = 1000;

function arkGenGo() {
  return document.querySelector("#cvDlgArkGen [id$='cvArkGenGo']");
}

function arkProgEls(dlg) {
  if (!dlg) return {};
  return {
    box: dlg.querySelector(".cv-ark-prog"),
    fill: dlg.querySelector(".cv-ark-prog-fill"),
    track: dlg.querySelector(".cv-ark-prog-track"),
    pct: dlg.querySelector(".cv-ark-prog-pct"),
    title: dlg.querySelector(".cv-ark-prog-t"),
    steps: dlg.querySelectorAll(".cv-ark-prog-steps li"),
    cancel: dlg.querySelector(".confirm-cancel"),
    go: arkGenGo()
  };
}

function paintArkProgress(dlg, idx, pct, done) {
  const els = arkProgEls(dlg);
  const n = els.steps.length || 3;
  const clamped = Math.max(0, Math.min(100, pct));
  if (els.fill) els.fill.style.width = clamped + "%";
  if (els.track) els.track.setAttribute("aria-valuenow", String(Math.round(clamped)));
  if (els.pct) els.pct.textContent = Math.round(clamped) + "%";
  const step = els.steps[Math.min(Math.max(idx, 0), n - 1)];
  if (els.title) {
    els.title.textContent = done
      ? ((els.box && els.box.getAttribute("data-done")) || "Terminé")
      : ((step && (step.getAttribute("data-detail") || step.textContent.trim())) || "");
  }
  els.steps.forEach((li, i) => {
    li.classList.toggle("is-done", !!done || i < idx);
    li.classList.toggle("is-on", !done && i === idx);
  });
}

function stopArkProgress() {
  if (arkProgTimer) {
    clearInterval(arkProgTimer);
    arkProgTimer = 0;
  }
}

function startArkProgress(dlg) {
  if (!dlg) return;
  stopArkProgress();
  if (arkFinishTimer) {
    clearTimeout(arkFinishTimer);
    arkFinishTimer = 0;
  }
  dlg.classList.add("is-busy");
  dlg.classList.remove("is-done");
  const els = arkProgEls(dlg);
  if (els.box) {
    els.box.hidden = false;
    els.box.setAttribute("aria-hidden", "false");
  }
  if (els.go) {
    els.go.classList.add("is-busy");
    els.go.classList.remove("is-off");
  }
  arkProgStarted = Date.now();
  paintArkProgress(dlg, 0, 8);
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    paintArkProgress(dlg, 0, 22);
    return;
  }
  arkProgTimer = window.setInterval(() => {
    const t = Date.now() - arkProgStarted;
    if (t < 400) {
      paintArkProgress(dlg, 0, 8 + (t / 400) * 16);
    } else if (t < 1000) {
      paintArkProgress(dlg, 1, 24 + ((t - 400) / 600) * 28);
    } else {
      paintArkProgress(dlg, 2, Math.min(90, 52 + (t - 1000) / 80));
    }
  }, 70);
}

function finishArkProgress(dlg, ok) {
  stopArkProgress();
  if (arkFinishTimer) {
    clearTimeout(arkFinishTimer);
    arkFinishTimer = 0;
  }
  if (!dlg) return;
  dlg.classList.remove("is-busy");
  const els = arkProgEls(dlg);
  if (!ok) {
    dlg.classList.remove("is-done");
    if (els.box) els.box.hidden = true;
    if (els.go) els.go.classList.remove("is-busy", "is-off");
    if (els.cancel) {
      const idle = els.cancel.getAttribute("data-idle");
      if (idle) els.cancel.textContent = idle;
    }
    return;
  }
  dlg.classList.add("is-done");
  if (els.box) {
    els.box.hidden = false;
    els.box.setAttribute("aria-hidden", "false");
  }
  if (els.go) {
    els.go.classList.remove("is-busy");
    els.go.classList.add("is-off");
  }
  if (els.cancel) {
    const done = els.cancel.getAttribute("data-done");
    if (done) els.cancel.textContent = done;
  }
  paintArkProgress(dlg, 2, 100, true);
  const live = dlg.querySelector("[data-flash-ark]");
  const msg = live && live.getAttribute("data-flash-ark");
  const token = live && live.getAttribute("data-flash-ark-token");
  if (msg && typeof toast === "function") {
    if (typeof applyConceptLabelUi === "function" && token) {
      applyConceptLabelUi._arkToken = token;
    }
    toast(msg, { soft: true });
  }
}

function settleArkProgress(dlg, ok) {
  const elapsed = arkProgStarted ? Date.now() - arkProgStarted : ARK_MIN_MS;
  const wait = ok ? Math.max(0, ARK_MIN_MS - elapsed) : 0;
  if (arkFinishTimer) clearTimeout(arkFinishTimer);
  arkFinishTimer = window.setTimeout(() => {
    arkFinishTimer = 0;
    finishArkProgress(dlg, ok);
  }, wait);
}

function onCvArkAjax(data) {
  const dlg = document.getElementById("cvDlgArkGen");
  if (data.status === "begin") {
    startArkProgress(dlg);
    return;
  }
  if (data.status === "error") {
    settleArkProgress(dlg, false);
    return;
  }
  if (data.status !== "success") return;
  const live = dlg && dlg.querySelector(".cv-ark-live [data-ark-state]");
  const state = (live && live.getAttribute("data-ark-state")) || "";
  settleArkProgress(dlg, state === "done");
}
window.onCvArkAjax = onCvArkAjax;

function onCvThesoTargetAjax(data) {
  if (data.status !== "success") return;
  bindThesoTransferUi({ enterLive: true });
}
window.onCvThesoTargetAjax = onCvThesoTargetAjax;

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
  el.classList.remove("is-enter");
  void el.offsetWidth;
  el.classList.add("is-enter");
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
  const go = e.target.closest(".login-go");
  if (go) {
    go.classList.remove("is-click");
    void go.offsetWidth;
    go.classList.add("is-click");
  }
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
function triggerCandSearch() {
  const go = document.getElementById("candBoardForm:candSearchGo");
  if (go) go.click();
}

function syncCandSearchClear() {
  const input = document.querySelector(".cand-search");
  const clear = $("#candSearchClear");
  if (clear) clear.hidden = !(input && input.value);
}

document.addEventListener("keydown", (e) => {
  if (e.repeat || e.isComposing) return;
  if (onThesoAcKeydown(e)) return;
  if (e.key !== "Enter") return;
  if (!e.target.classList || !e.target.classList.contains("cand-search")) return;
  e.preventDefault();
  clearTimeout(triggerCandSearch._t);
  triggerCandSearch();
});
document.addEventListener("keydown", (e) => {
  if (e.repeat || e.isComposing) return;
  if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
    const draft = $("#viewDraft");
    if (draft && draft.classList.contains("is-on")) {
      const create = $("#draftCreate");
      if (create && !create.disabled) create.click();
    }
  }
  if (e.key !== "Escape") return;
  if (typeof closeImgLightbox === "function" && closeImgLightbox()) return;
  if (typeof closeGpsLightbox === "function" && closeGpsLightbox()) return;
  if (typeof closeCollectionPicker === "function" && closeCollectionPicker()) return;
  if (closeTreeLang()) return;
  if (closeCvCtx()) return;
  if (closeAnyThesoAc()) return;
  if (hideCvDialogs()) return;
  if (e.target && e.target.classList && e.target.classList.contains("cand-search")) {
    if (e.target.value) {
      e.target.value = "";
      syncCandSearchClear();
      triggerCandSearch();
      return;
    }
  }
  if (SCREEN !== "candidats") return;
  const draft = $("#viewDraft");
  if (draft && draft.classList.contains("is-on")) {
    resolveDraft("annulé");
    return;
  }
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

window.onPreviewLoginAjax = function (data) {
  if (data.status !== "success") return;
  const form = document.getElementById("previewLoginForm");
  const btn = document.querySelector('[data-thesaurus="account"]');
  if (!form || !btn) return;
  btn.classList.add("is-on");
  btn.setAttribute("aria-expanded", "true");
};
window.onPreviewForgotAjax = function (data) {
  if (data.status !== "success") return;
  const pop = document.getElementById("menu-account");
  const btn = document.querySelector('[data-thesaurus="account"]');
  if (pop) pop.classList.add("is-forgot");
  if (btn) {
    btn.classList.add("is-on");
    btn.setAttribute("aria-expanded", "true");
  }
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
window.onPreviewSyncPoll = function (data) {
  if (data.status === "success") syncThesaurusPollTick();
};
document.addEventListener("keydown", (e) => {
  if (e.key !== "Escape") return;
  const overlay = $("#cblockOverlay");
  if (overlay && !overlay.hidden) {
    closeConceptBlockOverlay();
    e.preventDefault();
    return;
  }
  if (hideConfirm("#aboutSaveConfirm") || hideConfirm("#logoutConfirm") || hideConfirm("#stSaveConfirm")
      || hideConfirm("#previewCorpusCreateConfirm") || hideConfirm("#stLeaveConfirm")
      || hideConfirm("#alignDeleteConfirm") || hideConfirm("#alignReplaceConfirm")) {
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
  if (e.target && e.target.classList && e.target.classList.contains("cand-search")) {
    syncCandSearchClear();
    clearTimeout(triggerCandSearch._t);
    triggerCandSearch._t = setTimeout(triggerCandSearch, 280);
  }
  if (e.target && e.target.classList && e.target.classList.contains("cv-theso-ac-q")) {
    scheduleThesoAc(e.target.closest(".cv-theso-ac"));
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

document.addEventListener("click", (e) => {
  const t = e.target.closest("[data-act]");
  if (!t) {
    if ($("#navThesaurus") && !$("#navThesaurus").contains(e.target)) closeThesaurus();
    if ($("#voWrap") && !$("#voWrap").contains(e.target)) $("#voGear") && $("#voGear").classList.remove("is-on");
    if ($("#viewPick") && !$("#viewPick").contains(e.target)) $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
    if ($("#previewTermLangUi") && !$("#previewTermLangUi").contains(e.target)) closeTreeLang();
    if ($("#cvCtx") && !$("#cvCtx").contains(e.target)) closeCvCtx();
    if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
    return;
  }
  if ($("#cfCombo") && !$("#cfCombo").contains(e.target)) $("#cfCombo").classList.remove("open");
  const act = t.getAttribute("data-act");
  if (act !== "term-lang-toggle" && act !== "term-lang") closeTreeLang();
  if (act !== "cv-ctx-toggle") closeCvCtx();
  if (act === "logout-ask") {
    closeThesaurus();
    if (askLeaveThen(() => showConfirm("#logoutConfirm"))) return;
    showConfirm("#logoutConfirm");
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
  } else if (act === "st-save-dismiss") {
    hideConfirm("#stSaveConfirm");
  } else if (act === "st-save-modal") {
    return;
  } else if (act === "st-leave-dismiss") {
    settingsLeaveAction = null;
  } else if (act === "st-leave-confirm") {
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
    ["#previewView", "#viewHome", "#viewLive", "#viewConcept", "#viewSettings", "main.content .view"].forEach((sel) => {
      const el = $(sel);
      if (el) el.scrollTo({ top: 0, behavior });
    });
  } else if (act === "thesaurus") {
    const on = t.classList.contains("is-on");
    closeThesaurus();
    if (!on) {
      t.classList.add("is-on");
      t.setAttribute("aria-expanded", "true");
    }
  } else if (act === "login-forgot") {
    e.preventDefault();
    const pop = $("#menu-account");
    const btn = document.querySelector('[data-thesaurus="account"]');
    if (pop) pop.classList.add("is-forgot");
    if (btn) {
      btn.classList.add("is-on");
      btn.setAttribute("aria-expanded", "true");
    }
    const mail = document.getElementById("previewForgotMail");
    if (mail) window.setTimeout(() => mail.focus(), 0);
  } else if (act === "login-forgot-back") {
    e.preventDefault();
    const pop = $("#menu-account");
    if (pop) pop.classList.remove("is-forgot");
    const user = document.getElementById("previewLoginUser");
    if (user) window.setTimeout(() => user.focus(), 0);
  } else if (act === "home") openHome();
  else if (act === "back-graph") backToGraph();
  else if (act === "back-cand-list") {
    e.preventDefault();
    backToCandList();
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
  } else if (act === "cand-search-clear") {
    const input = document.querySelector(".cand-search");
    if (input) {
      input.value = "";
      input.focus();
    }
    syncCandSearchClear();
    triggerCandSearch();
  } else if (act === "cand-tab") {
    const board = t.closest(".cand-board") || $("#candBoard");
    const tab = t.getAttribute("data-tab") || "attente";
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
    if (tn && !t.classList.contains("is-empty")) tn.classList.toggle("is-open");
  } else if (act === "col-toggle") {
    toggleCollectionNode(t.closest(".tn"));
  } else if (act === "col-sort") {
    setCollectionSort(t.getAttribute("data-sort"));
  } else if (act === "open") {
    const candRow = t.closest("a.cand-row");
    if (candRow && (e.metaKey || e.ctrlKey || e.shiftKey)) return;
    if (candRow) e.preventDefault();
    const id = t.getAttribute("data-id");
    const nodeType = resolveNodeType(t);
    if (nodeType === "group" || nodeType === "subgroup") {
      openCollectionFromTree(id);
      closeSearchUi();
      return;
    }
    if (nodeType === "more") return;
    const stay = !!(t.closest("#panelTable") || t.closest("#panelResults") || t.closest("#resultsList")
      || t.closest("#panelCollection") || t.closest("#collectionDetail") || state.view === "collection");
    if (openLiveDetail(id, nodeType)) {
      state.home = false;
      state.draft = false;
      state.conceptId = id;
      if (stay && (state.view === "collection" || t.closest("#panelCollection") || t.closest("#collectionDetail"))) {
        state.colId = null;
        state.view = "collection";
      } else if (!stay) {
        state.view = "arbo";
      }
      highlightConcept(id);
      closeSearchUi();
      return;
    }
    openConcept(id, stay ? "stay" : "jump");
  } else if (act === "hyper-pick") {
    const id = t.getAttribute("data-id");
    if (!id) return;
    const nodeType = resolveNodeType(t);
    if (openLiveDetail(id, nodeType)) {
      state.home = false;
      state.draft = false;
      state.conceptId = id;
      highlightConcept(id);
      return;
    }
    state.home = false;
    state.draft = false;
    state.conceptId = id;
    highlightConcept(id);
    paint();
  } else if (act === "open-recent") {
    const id = t.getAttribute("data-id");
    if (!id) return;
    state.home = false;
    if (!openLiveDetail(id, "concept")) {
      go("index.xhtml?id=" + encodeURIComponent(id));
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
    t.title = copied;
    toast(copied, { soft: true });
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
  else if (act === "create") {
    e.stopPropagation();
    createCandidate(t);
  } else if (act === "cand-resolve") {
    resolveDraft(t.getAttribute("data-kind"));
  }
  else if (act === "sel-node") {
    e.stopPropagation();
    const tn = t.closest(".tn");
    if (!tn) return;
    const ids = collectIds(tn);
    const on = !t.classList.contains("on");
    if (!on) {
      let parent = previousTreeParent(tn);
      while (parent) {
        const parentId = parent.getAttribute("data-id");
        if (parentId) ids.push(parentId);
        parent = previousTreeParent(parent);
      }
    }
    setSelectedIds(ids, on);
    refreshSubtreeCounts();
  } else if (act === "sel-id") {
    e.stopPropagation();
    const id = t.getAttribute("data-id");
    if (id) setSelectedIds([id], !state.selected.has(id));
  } else if (act === "sel-all") {
    e.stopPropagation();
    const rows = $$("#panelTable tr[data-id]:not(.is-status-off)");
    const ids = rows.map(r => r.getAttribute("data-id"));
    const allOn = ids.length > 0 && ids.every(id => state.selected.has(id));
    setSelectedIds(ids, !allOn);
  } else if (act === "sel-all-visible") {
    e.stopPropagation();
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
    const box = tn.parentElement;
    const list = box ? Array.from(box.querySelectorAll(":scope > .tn")) : [];
    const openAt = [];
    list.forEach((node) => {
      const depth = treeDepth(node);
      openAt.length = depth;
      node.hidden = openAt.some((on) => !on);
      openAt[depth] = node.classList.contains("is-open");
    });
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
  } else if (act === "cv-ctx-toggle") {
    e.preventDefault();
    const open = !t.classList.contains("is-open");
    closeThesaurus();
    $("#viewPickBtn") && $("#viewPickBtn").classList.remove("is-open");
    closeTreeLang();
    t.classList.toggle("is-open", open);
    t.setAttribute("aria-expanded", open ? "true" : "false");
  } else if (act === "cv-dlg-dismiss") {
    e.preventDefault();
    const ark = document.getElementById("cvDlgArkGen");
    if (ark && !ark.hidden && ark.classList.contains("is-busy")) return;
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
  else if (act === "bulk-move") bulkMode("move");
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
  } else if (act === "export-run") {
    e.preventDefault();
    startSelectionExport();
  } else if (act === "export-dl") downloadReadyExport();
  else if (act === "export-cancel") cancelSelectionExport(!exportBusy);
  else if (act === "bulk-back") bulkMode("acts");
  else if (act === "bulk-status-menu") {
    $("#bulkStatusMenu") && $("#bulkStatusMenu").classList.toggle("is-on");
  } else if (act === "bulk-coll-run") {
    const name = ($("#bulkCollName") && $("#bulkCollName").value.trim()) || "";
    if (name) bulkAct("Collection « " + name + " » créée");
  } else if (act === "bulk-move-pick") {
    state.moveTarget = { id: t.getAttribute("data-id"), pref: t.getAttribute("data-pref") };
    const box = $("#bulkMoveTarget");
    const lab = $("#bulkMoveTargetL");
    if (lab) lab.textContent = state.moveTarget.pref;
    if (box) box.hidden = false;
    $("#bulkMovePick") && ($("#bulkMovePick").hidden = true);
    const run = $("#bulkMoveRun");
    if (run) run.classList.remove("is-off");
  } else if (act === "bulk-move-clear") {
    state.moveTarget = null;
    $("#bulkMoveTarget") && ($("#bulkMoveTarget").hidden = true);
    const q = $("#bulkMoveQ"); if (q) { q.value = ""; q.focus(); }
    $("#bulkMoveRun") && $("#bulkMoveRun").classList.add("is-off");
  } else if (act === "bulk-move-run") {
    if (!state.moveTarget || $("#bulkMoveRun").classList.contains("is-off")) return;
    const n = state.selected.size;
    const s = n > 1 ? "s" : "";
    bulkAct(n + " concept" + s + " déplacé" + s + " sous « " + state.moveTarget.pref + " »");
  }
});

$("#viewPickBtn") && $("#viewPickBtn").addEventListener("click", () => {
  $("#viewPickBtn").classList.toggle("is-open");
});
$("#voGear") && $("#voGear").addEventListener("click", (e) => {
  e.stopPropagation();
  $("#voGear").classList.toggle("is-on");
});
$("#rlMore") && $("#rlMore").addEventListener("click", () => {
  state.resultLimit += PAGE;
  paintCommittedResults();
});

const input = $("#searchInput"), clear = $("#searchClear"), field = $("#searchField");
if (input && field) {
  input.addEventListener("input", () => {
    if (clear) clear.hidden = !input.value;
    field.classList.add("is-focused");
    filterAc(input.value);
  });
  input.addEventListener("focus", () => {
    field.classList.add("is-focused");
    if (input.value) filterAc(input.value);
  });
  input.addEventListener("keydown", (e) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (input.value.trim()) filterAc(input.value);
      const downRows = shownAcRows();
      setAcIdx(Math.min(state.acIdx + 1, Math.max(downRows.length - 1, -1)));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setAcIdx(Math.max(state.acIdx - 1, -1));
    } else if (e.key === "Enter") {
      const rows = shownAcRows();
      if (state.acIdx >= 0 && rows[state.acIdx]) {
        openConcept(rows[state.acIdx].getAttribute("data-id"), "jump");
      } else runSearch();
    } else if (e.key === "Escape") {
      closeSearchUi();
      input.blur();
    }
  });
}
if (clear && input) {
  clear.addEventListener("click", () => {
    input.value = "";
    clear.hidden = true;
    filterAc("");
    input.focus();
  });
}
$("#searchGo") && $("#searchGo").addEventListener("click", runSearch);
const searchBox = $("#searchBox");
if (searchBox && searchBox.tagName === "FORM") {
  searchBox.addEventListener("submit", (e) => {
    e.preventDefault();
    runSearch();
  });
}
["cfFrom", "cfTo"].forEach(id => {
  const el = $("#" + id);
  if (!el) return;
  el.addEventListener("input", () => {
    state[id === "cfFrom" ? "candFrom" : "candTo"] = el.value || "";
    syncCandFilterUi();
    applyStatusFilter();
  });
});
const cfByQuery = $("#cfByQuery");
if (cfByQuery) {
  cfByQuery.addEventListener("input", () => {
    clearTimeout(cfByQuery._t);
    cfByQuery._t = setTimeout(() => loadCandByUsers(cfByQuery.value), 180);
  });
  cfByQuery.addEventListener("keydown", (e) => {
    e.stopPropagation();
    if (e.key === "Escape") {
      const combo = $("#cfCombo");
      if (combo) combo.classList.remove("open");
    }
  });
}

const moveQ = $("#bulkMoveQ");
if (moveQ) {
  moveQ.addEventListener("input", () => {
    if (state.moveTarget) return;
    filterMovePick(moveQ.value);
  });
  moveQ.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && state.moveTarget) {
      const run = $("#bulkMoveRun");
      if (run) run.click();
    }
  });
}
const collName = $("#bulkCollName");
if (collName) {
  collName.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && collName.value.trim()) {
      const run = $('[data-act="bulk-coll-run"]');
      if (run) run.click();
    }
  });
}

document.addEventListener("mousedown", (e) => {
  if (e.target.closest(".ac-row, .ac-footer")) e.preventDefault();
  if ($("#searchBox") && !$("#searchBox").contains(e.target)) closeSearchUi();
});

const resizer = $("#resizer");
if (resizer) {
  const saved = parseInt(localStorage.getItem("ot-sidebar-w"), 10);
  if (saved && saved >= 240) document.documentElement.style.setProperty("--tree-w", saved + "px");
  resizer.addEventListener("mousedown", (e) => {
    e.preventDefault();
    const startX = e.clientX;
    const startW = parseInt(getComputedStyle(document.documentElement).getPropertyValue("--tree-w"), 10) || 328;
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";
    const onMove = (ev) => {
      const w = Math.min(window.innerWidth * 0.75, Math.max(240, startW + (ev.clientX - startX)));
      document.documentElement.style.setProperty("--tree-w", w + "px");
    };
    const onUp = () => {
      document.removeEventListener("mousemove", onMove);
      document.removeEventListener("mouseup", onUp);
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
      const w = parseInt(getComputedStyle(document.documentElement).getPropertyValue("--tree-w"), 10);
      if (w) localStorage.setItem("ot-sidebar-w", String(w));
    };
    document.addEventListener("mousemove", onMove);
    document.addEventListener("mouseup", onUp);
  });
  resizer.addEventListener("dblclick", () => {
    document.documentElement.style.setProperty("--tree-w", "328px");
    localStorage.setItem("ot-sidebar-w", "328");
  });
}

applyStatusFilter();
applySort();
applyTableSort();
applyTableCols();
bulkMode("acts");

if (window.jsf && jsf.ajax && typeof jsf.ajax.addOnEvent === "function") {
  jsf.ajax.addOnEvent(function (data) {
    if (data.status !== "success") return;
    const src = data.source;
    const srcId = src && (src.id || (src.getAttribute && src.getAttribute("id")));
    if (srcId === "termLang" || srcId === "previewTermLangGo") onThesaurusLangChanged();
  });
}

const pageSizeSel = $("#panelTablePageSize");
try {
  const stored = Number(localStorage.getItem("ot-table-page-size"));
  if (TABLE_PAGE_SIZES.includes(stored)) state.tblPageSize = stored;
} catch (ex) {}
if (pageSizeSel) {
  pageSizeSel.value = String(tablePageSize());
  pageSizeSel.addEventListener("change", () => setTablePageSize(pageSizeSel.value));
}

if (SCREEN === "graphe") state.view = "hyper";
function onV2Ajax(data) {
  const treeToggle = isTreeCaretSource(data.source);
  const srcIdBegin = (data.source && data.source.id) || "";
  if (data.status === "begin") {
    if (!treeToggle) syncAboutEditor();
    if (treeToggle) lockTreeToggleScroll(data.source);
    if (srcIdBegin.indexOf("candSearchGo") >= 0 || srcIdBegin.indexOf("candMine") >= 0) {
      const panes = $("#candPanes");
      if (panes) {
        panes.classList.add("is-busy");
        panes.setAttribute("aria-busy", "true");
      }
    }
    return;
  }
  if (data.status === "complete") {
    if (srcIdBegin.indexOf("candSearchGo") >= 0 || srcIdBegin.indexOf("candMine") >= 0) {
      const panes = $("#candPanes");
      if (panes) {
        panes.classList.remove("is-busy");
        panes.removeAttribute("aria-busy");
      }
    }
    if (treeToggle) {
      restoreTreeToggleScroll();
      return;
    }
  }
  if (data.status === "error" && srcIdBegin.indexOf("openBtn") >= 0) {
    const live = $("#viewLive");
    if (live) live.classList.remove("is-loading");
  }
  if (data.status === "success") {
    const srcId = (data.source && data.source.id) || "";
      if (srcId === "previewCorpusToggleGo" || srcId === "previewAlignToggleGo") {
        markSettingsDraft();
      }
      requestAnimationFrame(() => {
        const srcId = (data.source && data.source.id) || "";
        if (srcId.indexOf("clearRevealBtn") >= 0
          || srcId.indexOf("candSearchGo") >= 0
          || srcId.indexOf("candMine") >= 0) {
        return;
      }
      if (srcId.indexOf("cvCopyThesoTarget") >= 0 || srcId.indexOf("cvMoveThesoTarget") >= 0
          || srcId.indexOf("cvArkGenGo") >= 0) {
        return;
      }
      if (srcId.indexOf("revealBtn") >= 0) {
        const tn = findTreeNodeById(state.conceptId);
        if (tn) {
          const st = tn.getAttribute("data-status");
          if (st && !state.statusSet.has(st)) {
            state.statusSet.add(st);
            syncStatusUi();
          }
        }
        applyStatusFilter();
        restoreSelection();
        highlightConcept(state.conceptId);
        requestAnimationFrame(() => highlightConcept(state.conceptId));
        return;
      }
      if (treeToggle) {
        applyStatusFilter();
        restoreSelection();
        restoreTreeToggleScroll();
        return;
      }
      syncAboutFold();
      markAboutVisualEmpty();
      maybeRememberAboutBaseline();
      refreshAboutFmtState();
      applyStatusFilter();
      applySort();
      restoreSelection();
      showLiveDetail();
      if (window.syncViewRail) window.syncViewRail();
    });
  }
}

if (window.faces && faces.ajax) {
  faces.ajax.addOnEvent(onV2Ajax);
} else if (window.jsf && jsf.ajax) {
  jsf.ajax.addOnEvent(onV2Ajax);
}

function scrollToPrefHash() {
  const id = (location.hash || "").replace(/^#/, "");
  if (!id || SCREEN !== "preference") return false;
  const el = document.getElementById(id);
  const view = $("#previewView") || document.querySelector("main.content .view");
  if (!el || !view) return false;
  const top = el.getBoundingClientRect().top - view.getBoundingClientRect().top + view.scrollTop - 12;
  view.scrollTo({ top: Math.max(0, top) });
  if (window.syncViewRail) window.syncViewRail();
  return true;
}

const maintProgress = new WeakMap();

function maintEls(tool) {
  if (!tool) return {};
  const prog = tool.querySelector(".mc-prog");
  return {
    tool,
    prog,
    fill: tool.querySelector(".mg-bar-fill"),
    rail: tool.querySelector(".xprog-rail-fill"),
    pct: tool.querySelector(".xprog-count"),
    title: tool.querySelector(".xprog-t"),
    detail: tool.querySelector(".xprog-d"),
    steps: tool.querySelectorAll(".xprog-steps li")
  };
}

function maintState(tool) {
  let state = maintProgress.get(tool);
  if (!state) {
    state = { timer: 0, started: 0, phase: -1 };
    maintProgress.set(tool, state);
  }
  return state;
}

function maintStepLabel(li) {
  if (!li) return "";
  const label = li.querySelector("span:not(.xprog-dot)");
  return ((label && label.textContent) || li.textContent || "").trim();
}

function paintMaintProgress(tool, phase, pct, { done = false, detail } = {}) {
  const els = maintEls(tool);
  const state = maintState(tool);
  const n = Math.max(1, els.steps.length);
  const idx = Math.max(0, Math.min(n - 1, phase));
  const step = els.steps[idx];
  const value = Math.max(0, Math.min(100, Math.round(pct)));
  if (els.prog) els.prog.style.setProperty("--mc-n", String(n));
  if (els.fill) els.fill.style.width = value + "%";
  if (els.pct) els.pct.textContent = value + "%";
  if (els.rail) {
    const ratio = done ? 1 : (n <= 1 ? 0 : idx / (n - 1));
    els.rail.style.width = Math.round(ratio * 100) + "%";
  }
  if (els.title) {
    els.title.textContent = done
      ? ((els.prog && els.prog.getAttribute("data-done-title")) || "Correction terminée")
      : ("Étape " + (idx + 1) + " / " + n + " — " + maintStepLabel(step));
  }
  if (els.detail) {
    const doneDetail = (els.prog && els.prog.getAttribute("data-done-detail")) || "Traitement terminé.";
    els.detail.textContent = detail
      || (done ? doneDetail : ((step && step.getAttribute("data-detail")) || ""));
  }
  els.steps.forEach((li, i) => {
    li.classList.toggle("is-done", done || i < idx);
    li.classList.toggle("is-on", !done && i === idx);
    li.classList.toggle("is-enter", !done && i === idx && i !== state.phase);
  });
  state.phase = idx;
}

function stopMaintProgress(tool) {
  const state = maintState(tool);
  if (state.timer) {
    clearInterval(state.timer);
    state.timer = 0;
  }
}

function startMaintProgress(tool) {
  const els = maintEls(tool);
  const state = maintState(tool);
  const n = Math.max(1, els.steps.length);
  const holdPhase = Math.max(0, n - 2);
  stopMaintProgress(tool);
  state.started = Date.now();
  state.phase = -1;
  if (els.tool) {
    els.tool.classList.add("is-busy");
    els.tool.setAttribute("aria-busy", "true");
  }
  if (els.prog) {
    els.prog.hidden = false;
    els.prog.classList.add("is-on");
    els.prog.setAttribute("aria-hidden", "false");
  }
  paintMaintProgress(tool, 0, 8);
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    paintMaintProgress(tool, holdPhase, 55);
    return;
  }
  state.timer = setInterval(() => {
    const elapsed = Date.now() - state.started;
    if (n <= 2) {
      paintMaintProgress(tool, 0, Math.min(86, 8 + elapsed / 30));
      return;
    }
    if (elapsed < 450) {
      paintMaintProgress(tool, 0, 8 + (elapsed / 450) * 22);
    } else if (elapsed < 1600) {
      const mid = Math.min(holdPhase, 1);
      paintMaintProgress(tool, mid, 32 + ((elapsed - 450) / 1150) * 40);
    } else {
      const rest = Math.min(14, (elapsed - 1600) / 400);
      paintMaintProgress(tool, holdPhase, 72 + rest);
    }
  }, 70);
}

function finishMaintProgress(tool, ok) {
  const els = maintEls(tool);
  const state = maintState(tool);
  const n = Math.max(1, els.steps.length);
  stopMaintProgress(tool);
  if (ok) {
    paintMaintProgress(tool, n - 1, 100, { done: true });
  } else {
    paintMaintProgress(tool, state.phase < 0 ? 0 : state.phase, parseInt(els.pct && els.pct.textContent, 10) || 0, {
      detail: "Le traitement a été interrompu."
    });
  }
  window.setTimeout(() => {
    if (els.prog) {
      els.prog.hidden = true;
      els.prog.classList.remove("is-on");
      els.prog.setAttribute("aria-hidden", "true");
    }
    if (els.tool) {
      els.tool.classList.remove("is-busy");
      els.tool.removeAttribute("aria-busy");
    }
    if (els.fill) els.fill.style.width = "0%";
    if (els.rail) els.rail.style.width = "0%";
  }, ok ? 720 : 900);
}

function maintToolFromAjax(data) {
  const src = data && data.source;
  if (src && src.closest) {
    return src.closest(".mc-tool");
  }
  return document.querySelector(".mc-tool.is-busy");
}

window.onMaintAjax = function (data) {
  const tool = maintToolFromAjax(data);
  if (!tool) return;
  if (data.status === "begin") {
    startMaintProgress(tool);
  }
  if (data.status === "success") {
    const flash = tool.querySelector("[data-maint-ok]");
    const ok = !flash || flash.getAttribute("data-maint-ok") !== "false";
    finishMaintProgress(tool, ok);
    const run = tool.querySelector(".bo-btn-hit") || tool.querySelector(".bo-btn-hit-in");
    if (run) requestAnimationFrame(() => run.focus());
  }
  if (data.status === "error") {
    const prog = tool.querySelector(".mc-prog");
    finishMaintProgress(tool, false);
    toast((prog && prog.getAttribute("data-fail-msg")) || "Le traitement a échoué.", { error: true });
  }
};
window.onMaintTopTermAjax = window.onMaintAjax;

window.downloadMaintSitemap = function () {
  const ctx = document.body.getAttribute("data-ctx") || "";
  window.location.href = ctx + "/v2/api/maintenance/sitemap.xml";
};

let pendingMaintBtn = null;
function maintConfirmMessage(btn) {
  const opt = btn.closest("[data-confirm-overwrite]");
  if (opt) {
    const sw = opt.querySelector(".st-sw-input, input[type='checkbox']");
    if (sw && sw.checked) return opt.getAttribute("data-confirm-overwrite") || "";
    return "";
  }
  const tool = btn.closest(".mc-tool[data-confirm]");
  return tool ? (tool.getAttribute("data-confirm") || "") : "";
}
document.addEventListener("click", function (e) {
  const btn = e.target.closest && e.target.closest(".bo-btn-hit-in");
  if (!btn || btn.disabled) return;
  if (btn.dataset.maintSkipConfirm === "1") {
    delete btn.dataset.maintSkipConfirm;
    return;
  }
  const msg = maintConfirmMessage(btn);
  if (!msg) return;
  e.preventDefault();
  e.stopImmediatePropagation();
  pendingMaintBtn = btn;
  const text = document.getElementById("maintConfirmText");
  if (text) text.textContent = msg;
  showConfirm("#maintConfirm");
}, true);

var params = new URLSearchParams(location.search);

if (SCREEN === "atelier") {
  setBatch(params.get("obj") || "alignements");
}
if (SCREEN === "synchronisation") {
  syncThesaurusPollTick();
}
if (SCREEN === "candidats" && (params.get("new") === "1" || params.get("pref") || params.get("path"))) {
  createCandidate({
    getAttribute: (name) => {
      if (name === "data-pref") return params.get("pref") || "";
      if (name === "data-path") return params.get("path") || "";
      return "";
    }
  });
}
if (IS_CONSULT) {
  const q = params.get("q");
  const id = params.get("id");
  const view = params.get("view");
  if (q) {
    if (input) input.value = q;
    if (clear) clear.hidden = !q;
    runSearch();
  } else if (id) {
    if (!openLiveDetail(id, params.get("type") || "")) {
      openConcept(id);
    }
  } else if (view) {
    setView(view);
  } else {
    paint();
  }
} else {
  paint();
}
bindPrefSwitches();
bindViewRail();
loadStatKpis();
initSettingsLeaveGuard();
requestAnimationFrame(() => {
  syncAboutFold();
  markAboutVisualEmpty();
  maybeRememberAboutBaseline();
  refreshAboutFmtState();
  syncCandSearchClear();
  if (window.syncViewRail) window.syncViewRail();
  scrollToPrefHash();
  if (SCREEN === "preference") rememberSettingsBaseline();
});
if (SCREEN === "preference" && location.hash) {
  window.addEventListener("load", scrollToPrefHash);
  setTimeout(scrollToPrefHash, 80);
  setTimeout(scrollToPrefHash, 250);
}
