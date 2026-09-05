/**
 * OpenTheso V2 — barre de progression ARK / CSV / loop / delete.
 */
"use strict";

let arkProgTimer = 0;
let arkProgStarted = 0;
let arkFinishTimer = 0;
const ARK_MIN_MS = 1000;

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
    go: dlg.querySelector(".cv-ark-go")
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
  const live = dlg.querySelector("[data-flash-ark], [data-flash-csv], [data-flash-loop], [data-flash-del]");
  const msg = live && (live.getAttribute("data-flash-ark") || live.getAttribute("data-flash-csv")
    || live.getAttribute("data-flash-loop") || live.getAttribute("data-flash-del"));
  const token = live && (live.getAttribute("data-flash-ark-token") || live.getAttribute("data-flash-csv-token")
    || live.getAttribute("data-flash-loop-token") || live.getAttribute("data-flash-del-token"));
  if (msg && typeof toast === "function") {
    if (typeof applyConceptLabelUi === "function" && token) {
      if (live && live.hasAttribute("data-flash-del")) applyConceptLabelUi._delToken = token;
      else if (live && live.hasAttribute("data-flash-loop")) applyConceptLabelUi._loopToken = token;
      else if (live && live.hasAttribute("data-flash-csv")) applyConceptLabelUi._csvToken = token;
      else applyConceptLabelUi._arkToken = token;
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
  const src = data.source;
  const dlg = (src && src.closest && src.closest(".confirm-overlay.is-run-dlg"))
    || visibleArkDialog();
  if (data.status === "begin") {
    startArkProgress(dlg);
    return;
  }
  if (data.status === "error") {
    settleArkProgress(dlg, false);
    return;
  }
  if (data.status !== "success") return;
  const live = dlg && dlg.querySelector(".cv-ark-live [data-run-state], .cv-ark-live [data-ark-state]");
  const state = (live && (live.getAttribute("data-run-state") || live.getAttribute("data-ark-state"))) || "";
  settleArkProgress(dlg, state === "done");
}
window.onCvArkAjax = onCvArkAjax;

function onCvCsvLoadAjax(data) {
  if (data.status !== "success") return;
  const src = data.source;
  const dlg = (src && src.closest && src.closest("#cvDlgImportCsv"))
    || document.getElementById("cvDlgImportCsv");
  bindCsvUi(dlg);
  paintCsvGo(dlg);
}
window.onCvCsvLoadAjax = onCvCsvLoadAjax;

function csvDelimInput(dlg) {
  return dlg && dlg.querySelector("[id$='cvCsvDelim']");
}

function csvFileInput(dlg) {
  return dlg && dlg.querySelector("[id$='cvCsvFile']");
}

function paintCsvDelim(dlg) {
  if (!dlg) return;
  const hidden = csvDelimInput(dlg);
  const val = hidden ? String(hidden.value || "0") : "0";
  dlg.querySelectorAll("[data-act='csv-delim']").forEach((btn) => {
    btn.classList.toggle("is-on", btn.getAttribute("data-val") === val);
  });
}

function csvHasLoadedFile(dlg) {
  const file = csvFileInput(dlg);
  if (file && file.files && file.files[0]) return true;
  const live = dlg && dlg.querySelector(".cv-ark-live [data-csv-loaded]");
  return !!(live && live.getAttribute("data-csv-loaded") === "true");
}

function paintCsvGo(dlg) {
  if (!dlg || dlg.classList.contains("is-busy") || dlg.classList.contains("is-done")) return;
  const go = dlg.querySelector("[id$='cvCsvGo']");
  const live = dlg.querySelector(".cv-ark-live [data-import-ready]");
  if (!go || !live) return;
  go.classList.toggle("is-off", live.getAttribute("data-import-ready") !== "true");
}

function bindCsvUi(dlg) {
  const root = dlg || document.getElementById("cvDlgImportCsv");
  if (!root) return;
  paintCsvDelim(root);
  paintCsvGo(root);
  const drop = root.querySelector(".cv-csv-drop");
  const input = csvFileInput(root);
  if (!drop || !input || drop._csvBound) return;
  drop._csvBound = true;
  drop.addEventListener("dragover", (e) => {
    e.preventDefault();
    drop.classList.add("is-over");
  });
  drop.addEventListener("dragleave", () => drop.classList.remove("is-over"));
  drop.addEventListener("drop", (e) => {
    e.preventDefault();
    drop.classList.remove("is-over");
    const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
    if (!file || !window.DataTransfer) return;
    const dt = new DataTransfer();
    dt.items.add(file);
    input.files = dt.files;
    input.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

function clickCsvAnalyze(dlg) {
  const go = dlg && dlg.querySelector("[id$='cvCsvAnalyze']");
  if (go) go.click();
}
