/**
 * OpenTheso V2 — contrat des dialogs concept.
 * is-run-dlg + is-busy  → ne pas fermer
 * is-run-dlg + is-done  → Fermer clique .cv-after-close
 * data-*-dirty=true     → idem pour catalogues
 * sinon                 → juste cacher
 */
"use strict";

function visibleArkDialog() {
  return document.querySelector(".confirm-overlay.is-run-dlg:not([hidden])");
}

function hideCvDialogs() {
  const ark = visibleArkDialog();
  if (ark && ark.classList.contains("is-busy")) return false;
  const open = document.querySelector(".confirm-overlay[id^='cvDlg']:not([hidden])");
  const afterClose = open && open.querySelector(".cv-after-close");
  const refreshArk = !!(ark && ark.classList.contains("is-done"));
  const refreshLive = !!(afterClose && open && !open.classList.contains("is-run-dlg")
    && (open.querySelector("[data-types-dirty='true']")
      || open.querySelector("[data-add-nt-dirty='true']")));
  let closed = false;
  $$(".confirm-overlay[id^='cvDlg']").forEach((el) => {
    if (!el.hidden) {
      el.hidden = true;
      closed = true;
    }
  });
  if (refreshArk) {
    const go = ark.querySelector(".cv-after-close") || document.querySelector("[id$='cvArkAfterClose']");
    if (go) go.click();
  } else if (refreshLive) {
    afterClose.click();
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
      bindCsvUi(dlg);
    }
  }
  bindThesoTransferUi();
  bindFacetComposer();
  bindAddNtUi();
}
window.onCvMenuAjax = onCvMenuAjax;

function cvAjaxSourceId(data) {
  return (data.source && data.source.id) || "";
}

function onCvLiveAjax(data, opts) {
  const dlg = document.getElementById(opts.dlgId);
  const go = dlg && (dlg.querySelector(opts.goSel) || dlg.querySelector(".cv-ark-go"));
  const srcId = cvAjaxSourceId(data);
  const isGo = !opts.goIds || opts.goIds.some((id) => srcId.indexOf(id) >= 0);
  if (data.status === "begin") {
    if (isGo && go) go.classList.add("is-busy");
    return "begin";
  }
  if (data.status !== "success") {
    if (isGo && go) go.classList.remove("is-busy");
    return data.status;
  }
  if (isGo && go) go.classList.remove("is-busy");
  const live = dlg && dlg.querySelector(opts.flashSel);
  if (opts.doneAttr && live && live.getAttribute(opts.doneAttr) === "true" && dlg) {
    dlg.classList.add("is-done");
    if (go) go.classList.add("is-off");
    const cancel = dlg.querySelector(".confirm-cancel");
    if (cancel) {
      const label = cancel.getAttribute("data-done");
      if (label) cancel.textContent = label;
    }
  }
  if (live && opts.flashName && typeof toast === "function") {
    const msg = live.getAttribute(opts.flashName);
    const token = live.getAttribute(opts.flashToken);
    if (msg) {
      if (typeof applyConceptLabelUi === "function" && token && opts.tokenKey) {
        applyConceptLabelUi[opts.tokenKey] = token;
      }
      toast(msg, { soft: true });
    }
  }
  return "success";
}
window.onCvLiveAjax = onCvLiveAjax;

function onCvTypesAjax(data) {
  onCvLiveAjax(data, {
    dlgId: "cvDlgManageTypes",
    flashSel: "[data-flash-type]",
    flashName: "data-flash-type",
    flashToken: "data-flash-type-token",
    tokenKey: "_typeToken",
    goIds: []
  });
}
window.onCvTypesAjax = onCvTypesAjax;

function onCvEditTypeAjax(data) {
  onCvLiveAjax(data, {
    dlgId: "cvDlgEditType",
    goSel: "[id$='cvEditTypeGo']",
    goIds: ["cvEditTypeGo"],
    flashSel: "[data-flash-ctype]",
    flashName: "data-flash-ctype",
    flashToken: "data-flash-ctype-token",
    tokenKey: "_ctypeToken",
    doneAttr: "data-type-done"
  });
}
window.onCvEditTypeAjax = onCvEditTypeAjax;

function facetNameInput(root) {
  return root && root.querySelector("[id$='cvFacetName']");
}

function paintFacetGo(root) {
  const go = root && root.querySelector("[id$='cvFacetGo']");
  if (!go) return;
  const input = facetNameInput(root);
  go.classList.toggle("is-off", !input || !input.value.trim());
}

function bindFacetComposer() {
  const box = document.getElementById("cvFacetComposer");
  if (!box) return;
  const input = facetNameInput(box);
  if (input && !input.dataset.facetBound) {
    input.dataset.facetBound = "1";
    input.addEventListener("input", () => paintFacetGo(box));
    input.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        event.preventDefault();
        const cancel = box.querySelector("[id$='cvFacetCancel']");
        if (cancel) cancel.click();
        return;
      }
      if (event.key !== "Enter") return;
      event.preventDefault();
      const go = box.querySelector("[id$='cvFacetGo']");
      if (go && !go.classList.contains("is-off")) go.click();
    });
  }
  paintFacetGo(box);
  if (input) requestAnimationFrame(() => input.focus());
}
