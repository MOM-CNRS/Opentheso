/**
 * OpenTheso V2 — création d'un concept (add-nt).
 */
"use strict";

function addNtNameInput(dlg) {
  return dlg && dlg.querySelector(".cv-add-nt-name");
}

function addNtGroupSelect(dlg) {
  return dlg && dlg.querySelector(".cv-add-nt-group, [id$='cvAddNtGroup']");
}

function paintAddNtPreview(dlg) {
  if (!dlg) return;
  const preview = dlg.querySelector("#cvAddNtPreview");
  if (!preview) return;
  const empty = preview.getAttribute("data-empty") || "";
  const input = addNtNameInput(dlg);
  const val = input ? input.value.trim() : "";
  preview.textContent = val || empty;
  preview.classList.toggle("is-empty", !val);
  const hidden = dlg.querySelector("[id$='cvAddNtRel']");
  const rel = hidden ? String(hidden.value || "NT") : "NT";
  const group = addNtGroupSelect(dlg);
  let coll = "";
  if (group && group.selectedIndex >= 0) {
    const opt = group.options[group.selectedIndex];
    if (opt && opt.value) coll = (opt.text || "").trim();
  }
  const meta = dlg.querySelector("#cvAddNtMeta");
  if (meta) {
    meta.textContent = [rel, coll].filter(Boolean).join(" · ");
    meta.hidden = !val;
  }
  const ready = dlg.querySelector(".cv-add-ready");
  if (ready) ready.hidden = !val;
}

function paintAddNtGo(dlg) {
  if (!dlg) return;
  const go = dlg.querySelector("[id$='cvAddNtGo']");
  if (!go) return;
  const input = addNtNameInput(dlg);
  go.classList.toggle("is-off", !input || !input.value.trim());
}

function paintAddNtRel(dlg) {
  if (!dlg) return;
  const hidden = dlg.querySelector("[id$='cvAddNtRel']");
  const val = hidden ? String(hidden.value || "NT") : "NT";
  dlg.querySelectorAll("[data-act='add-nt-rel']").forEach((btn) => {
    const on = btn.getAttribute("data-val") === val;
    btn.classList.toggle("is-on", on);
    btn.setAttribute("aria-selected", on ? "true" : "false");
    btn.tabIndex = on ? 0 : -1;
  });
}

function restoreAddNtMore(root) {
  const more = root && root.querySelector(".cv-add-more");
  if (!more) return;
  const notation = root.querySelector("[id$='cvAddNtNotation']");
  const id = root.querySelector("[id$='cvAddNtId']");
  const filled = !!(notation && notation.value.trim()) || !!(id && id.value.trim());
  more.open = !!(root._addNtMoreOpen || filled);
}

function bindAddNtUi(dlg) {
  const root = dlg || document.getElementById("cvDlgAddNt");
  if (!root) return;
  restoreAddNtMore(root);
  paintAddNtPreview(root);
  paintAddNtGo(root);
  paintAddNtRel(root);
  if (!root._addNtBound) {
    root._addNtBound = true;
    root.addEventListener("input", (event) => {
      if (!event.target) return;
      if (event.target.classList.contains("cv-add-nt-name")) {
        paintAddNtPreview(root);
        paintAddNtGo(root);
      }
    });
    root.addEventListener("change", (event) => {
      if (!event.target) return;
      if (event.target.classList.contains("cv-add-nt-group") || (event.target.id && event.target.id.indexOf("cvAddNtGroup") >= 0)) {
        paintAddNtPreview(root);
      }
    });
    root.addEventListener("toggle", (event) => {
      if (event.target && event.target.classList.contains("cv-add-more")) {
        root._addNtMoreOpen = !!event.target.open;
      }
    }, true);
    root.addEventListener("keydown", (event) => {
      if (event.repeat || event.isComposing) return;
      const rel = event.target && event.target.closest && event.target.closest("[data-act='add-nt-rel']");
      if (rel && root.contains(rel)) {
        const chips = Array.prototype.slice.call(root.querySelectorAll("[data-act='add-nt-rel']"));
        const i = chips.indexOf(rel);
        let next = -1;
        if (event.key === "ArrowRight" || event.key === "ArrowDown") next = Math.min(chips.length - 1, i + 1);
        else if (event.key === "ArrowLeft" || event.key === "ArrowUp") next = Math.max(0, i - 1);
        else if (event.key === "Home") next = 0;
        else if (event.key === "End") next = chips.length - 1;
        if (next >= 0 && next !== i && chips[next]) {
          event.preventDefault();
          chips[next].click();
          chips[next].focus();
        }
        return;
      }
      if (event.key !== "Enter") return;
      if (!event.target || event.target.tagName === "SELECT" || event.target.tagName === "TEXTAREA") return;
      if (!event.target.classList.contains("st-input")) return;
      event.preventDefault();
      const force = root.querySelector("[id$='cvAddNtForce']");
      const go = root.querySelector("[id$='cvAddNtGo']");
      if (force && !force.classList.contains("is-off") && !force.classList.contains("is-busy")) {
        force.click();
        return;
      }
      if (go && !go.classList.contains("is-off") && !go.classList.contains("is-busy")) go.click();
    });
  }
  const input = addNtNameInput(root);
  if (input && !root.hidden) requestAnimationFrame(() => input.focus());
}

function onCvAddNtAjax(data) {
  const dlg = document.getElementById("cvDlgAddNt");
  const go = dlg && (dlg.querySelector("[id$='cvAddNtGo']") || dlg.querySelector("[id$='cvAddNtForce']"));
  const srcId = (data.source && data.source.id) || "";
  const isGo = srcId.indexOf("cvAddNtGo") >= 0 || srcId.indexOf("cvAddNtForce") >= 0;
  if (data.status === "begin") {
    if (isGo && go) go.classList.add("is-busy");
    return;
  }
  if (data.status !== "success") {
    if (isGo && go) go.classList.remove("is-busy");
    return;
  }
  bindAddNtUi(dlg);
  const live = dlg && dlg.querySelector("[data-flash-nt]");
  const dirty = !!(live && live.getAttribute("data-add-nt-dirty") === "true");
  const dup = !!(live && live.getAttribute("data-add-nt-dup") === "true");
  if (dirty) {
    const cancel = dlg.querySelector(".confirm-cancel");
    if (cancel) {
      const label = cancel.getAttribute("data-done");
      if (label) cancel.textContent = label;
    }
  }
  const body = dlg && dlg.querySelector(".cv-dlg-body");
  const warn = dlg && dlg.querySelector(".confirm-p.is-warn");
  if (dup && warn && typeof warn.scrollIntoView === "function") {
    warn.scrollIntoView({ block: "nearest" });
  } else if (body && !dup) {
    body.scrollTop = 0;
  }
  const msg = live && live.getAttribute("data-flash-nt");
  const token = live && live.getAttribute("data-flash-nt-token");
  if (msg && typeof toast === "function") {
    if (typeof applyConceptLabelUi === "function" && token) {
      applyConceptLabelUi._ntToken = token;
    }
    toast(msg, { soft: true });
  }
}
window.onCvAddNtAjax = onCvAddNtAjax;
