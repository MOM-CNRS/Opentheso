/** Export — progression et téléchargement. */
let lastExportPhase = -1;
let exportConceptClock = { key: "", stamp: 0 };

function exportPace(data) {
  const done = Number(data && data.done) || 0;
  const total = Number(data && data.total) || 0;
  const phase = (data && data.phase) || "";
  const counting = total > 0 && (phase === "read" || phase === "build" || phase === "write");
  if (!counting) {
    exportConceptClock = { key: "", stamp: 0 };
    return { eta: -1, rate: 0 };
  }
  const key = phase + ":" + total;
  if (exportConceptClock.key !== key) {
    exportConceptClock = { key, stamp: Date.now() };
  }
  if (done <= 0) return { eta: -1, rate: 0 };
  const elapsed = Math.max(1, Date.now() - exportConceptClock.stamp);
  const rate = done * 1000 / elapsed;
  const finished = data.status === "done" || done >= total;
  const eta = !finished && rate > 0 && total > done
    ? Math.max(1, Math.round((total - done) / rate))
    : -1;
  return { eta, rate: finished ? 0 : rate };
}

function fmtExportTpl(tpl, a, b) {
  if (window.tpFmtTpl) return window.tpFmtTpl(tpl, a, b);
  if (!tpl) return "";
  return String(tpl).replace("{0}", a == null ? "" : String(a)).replace("{1}", b == null ? "" : String(b));
}

function paintExportProgress(data) {
  // Page export thesauri : progression uniquement dans tpLoading (même UX que l'import).
  if (usesExportOverlay()) {
    paintExportOverlay(data);
    return;
  }
  const prog = $("#bulkExportProg");
  if (prog) {
    prog.classList.remove("is-wait");
    const show = data && (data.status === "running" || data.status === "done");
    if (show) {
      prog.classList.add("is-on");
      prog.setAttribute("aria-hidden", "false");
    }
  }
  const pct = Math.max(0, Math.min(100, Number(data && data.progress) || 0));
  const fill = $("#bulkExportFill");
  if (fill) fill.style.width = pct + "%";
  const n = $("#bulkExportPct");
  if (n) n.textContent = pct + "%";
  const phaseCount = Math.max(1, Number(data && data.phaseCount) || 4);
  const idx = Number(data && data.phaseIndex);
  const phaseIndex = Number.isFinite(idx) ? Math.max(0, Math.min(phaseCount - 1, idx)) : 0;
  const label = (data && data.phaseLabel) || "Préparer";
  const nextTitle = "Étape " + (phaseIndex + 1) + " / " + phaseCount + " — " + label;
  const nextDetail = (data && data.message) || "";
  const phaseChanged = phaseIndex !== lastExportPhase;
  const t = $("#bulkExportProgT");
  if (t) {
    t.textContent = nextTitle;
    if (phaseChanged) replayExportAnim(t, "is-swap");
  }
  const done = Number(data && data.done) || 0;
  const total = Number(data && data.total) || 0;
  const d = $("#bulkExportProgD");
  if (d) {
    d.textContent = nextDetail;
    if (phaseChanged) replayExportAnim(d, "is-swap");
  }
  const count = $("#bulkExportCount");
  if (count) {
    const showCount = total > 0 && data && (data.status === "running" || data.status === "done");
    count.hidden = !showCount;
    if (showCount) {
      const shown = data.status === "done" ? total : done;
      count.textContent = shown + " / " + total;
    }
  }
  const doneAll = data && data.status === "done";
  const rail = $("#bulkExportRail");
  if (rail) {
    const ratio = doneAll ? 1 : (phaseCount <= 1 ? 0 : phaseIndex / (phaseCount - 1));
    rail.style.width = (ratio * 100) + "%";
  }
  $$("#bulkExportSteps li").forEach(li => {
    const step = Number(li.getAttribute("data-step"));
    const on = !doneAll && step === phaseIndex;
    const becameOn = on && !li.classList.contains("is-on");
    li.classList.toggle("is-on", on);
    li.classList.toggle("is-done", doneAll || step < phaseIndex);
    if (becameOn) replayExportAnim(li, "is-enter");
    else if (!on) li.classList.remove("is-enter");
  });
  lastExportPhase = phaseIndex;
  paintExportOverlay(data);
}

function replayExportAnim(el, cls) {
  if (!el) return;
  el.classList.remove(cls);
  void el.offsetWidth;
  el.classList.add(cls);
}

function stopExportPoll() {
  if (exportPollTimer) {
    window.clearInterval(exportPollTimer);
    exportPollTimer = null;
  }
}

function startExportPoll() {
  if (exportPollTimer) return;
  exportPollStarted = Date.now();
  pollSelectionExport();
  exportPollTimer = window.setInterval(pollSelectionExport, 350);
}

function pollSelectionExport() {
  if (exportPollStarted && Date.now() - exportPollStarted > EXPORT_POLL_MAX_MS) {
    stopExportPoll();
    setExportBusy(false);
    showExportError("L’export prend trop de temps. Réessayez ou réduisez la sélection.");
    return;
  }
  fetch(selectionExportUrl("", { t: Date.now() }), {
    credentials: "same-origin",
    cache: "no-store",
    headers: { Accept: "application/json", "Cache-Control": "no-cache" }
  })
    .then(res => res.ok ? res.json() : Promise.reject())
    .then(onSelectionExportStatus)
    .catch(() => {});
}

function onSelectionExportStatus(data) {
  if (!data) return;
  if (exportRunStartedAt && Number(data.startedAt) && Number(data.startedAt) < exportRunStartedAt) {
    return;
  }
  paintExportProgress(data);
  if (data.status === "running") {
    setExportBusy(true);
    showExportError("");
    startExportPoll();
    return;
  }
  stopExportPoll();
  if (data.status === "done") {
    setExportBusy(false, true);
    if (!exportDownloaded) {
      exportDownloaded = true;
      downloadReadyExport();
    }
    return;
  }
  setExportBusy(false);
  if (data.status === "error") {
    const msg = data.error || data.message || "";
    if (/déjà en cours/i.test(msg)) {
      setExportBusy(true);
      startExportPoll();
      return;
    }
    showExportError(readableExportError(msg));
    return;
  }
  if (data.status === "cancelled") {
    setExportPanes(true, false);
  }
}

function readableExportError(msg) {
  const text = (msg || "").trim();
  if (!text) return "Export impossible";
  return text;
}

function downloadSelectionExport(filename) {
  const stamp = exportRunStartedAt || Date.now();
  return fetch(selectionExportUrl("/file", { v: stamp }), {
    credentials: "same-origin",
    cache: "no-store",
    headers: { "Cache-Control": "no-cache" }
  }).then(res => {
    if (!res.ok) throw new Error("missing");
    const header = res.headers.get("Content-Disposition") || "";
    const star = header.match(/filename\*=UTF-8''([^;]+)/i);
    const plain = header.match(/filename="?([^";]+)"?/i);
    const name = (star && decodeURIComponent(star[1]))
      || (plain && plain[1])
      || filename
      || "export.rdf";
    return res.blob().then(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = name;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1500);
    });
  });
}

function startSelectionExport() {
  if (exportBusy) return;
  const theso = thesaurusId();
  if (!theso) {
    showExportError("Thésaurus manquant");
    return;
  }
  const whole = !!state.selectedAllThesaurus;
  const ids = whole ? [] : exportSelectionIds();
  if (!whole && !ids.length) {
    showExportError("Aucun concept à exporter");
    return;
  }
  const filterByGroup = exportSwitchOn("bulkExportGroup");
  const groupIds = selectedExportGroupIds();
  const groupChoices = $$("#bulkExportGroups .xpick-i");
  if (filterByGroup && groupChoices.length && !groupIds.length) {
    showExportError("Choisissez au moins une collection");
    return;
  }
  if (exportKindOf(exportFormat()) === "csv") {
    const langChoices = csvLangOptions();
    if (langChoices.length && !selectedExportLangCodes().length) {
      showExportError("Choisissez au moins une langue");
      return;
    }
  }
  try {
    startSelectionExportRequest(theso, whole, ids, filterByGroup && groupIds.length > 0, groupIds);
  } catch (err) {
    setExportBusy(false);
    showExportError((err && err.message) || "L’export n’a pas pu démarrer.");
  }
}

function startSelectionExportRequest(theso, whole, ids, filterByGroup, groupIds) {
  exportDownloaded = false;
  exportRunStartedAt = 0;
  exportConceptClock = { key: "", stamp: 0 };
  exportPollStarted = Date.now();
  showExportError("");
  setExportBusy(true);
  paintExportProgress({
    status: "running",
    progress: 1,
    message: "Démarrage de l'export…",
    phaseLabel: "Préparer",
    phaseIndex: 0,
    phaseCount: 4,
    done: 0,
    total: 0
  });
  const lang1 = $("#bulkExportLang1");
  const lang2 = $("#bulkExportLang2");
  fetch(selectionExportUrl(""), {
    method: "POST",
    credentials: "same-origin",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify({
      thesaurusId: theso,
      thesaurusTitle: thesaurusTitle(),
      conceptIds: ids,
      format: exportFormat(),
      includeDescendants: !whole && exportIncludeDescendants(),
      wholeThesaurus: whole,
      clearHtml: exportSwitchOn("bulkExportHtml"),
      includeImages: exportSwitchOn("bulkExportImg"),
      filterByGroup: filterByGroup,
      exportByGroup: whole && exportSwitchOn("bulkExportZip"),
      groupIds: groupIds,
      languageCodes: selectedExportLangCodes(),
      csvDelimiter: exportDelimiter(),
      pdfType: exportPdfType(),
      language1: lang1 ? lang1.value : "",
      language2: lang2 ? lang2.value : ""
    })
  }).then(res => {
    if (res.ok) return res.json();
    if (res.status === 403) {
      throw new Error("Session expirée. Rechargez la page.");
    }
    return res.json().catch(() => ({})).then(data => {
      throw new Error(readableExportError(data.error || data.message || data.detail || "L’export n’a pas pu démarrer."));
    });
  }).then(data => {
    if (data && Number(data.startedAt)) exportRunStartedAt = Number(data.startedAt);
    onSelectionExportStatus(data);
  }).catch(err => {
    setExportBusy(false);
    showExportError((err && err.message) || "L’export n’a pas pu démarrer.");
  });
}

function downloadReadyExport() {
  const dl = $("#bulkExportDl");
  const stamp = exportRunStartedAt;
  if (dl) dl.disabled = true;
  downloadSelectionExport().then(() => {
    toast("Export téléchargé");
    if (exportBusy || exportRunStartedAt !== stamp) return;
    if (usesExportOverlay()) setExportActions("idle");
    else setExportActions("done");
  }).catch(() => {
    setExportActions("retry");
    showExportError("Le fichier n’a pas pu être téléchargé.");
  }).finally(() => {
    if (dl) dl.disabled = false;
  });
}

function cancelSelectionExport(andBack) {
  const wasBusy = exportBusy;
  stopExportPoll();
  setExportBusy(false);
  const finish = () => {
    setExportPanes(true, false);
    if (andBack) {
      if (pickerExportMode) {
        // Page d'export : rester sur l'écran et réafficher les options.
        resetExportPanelForNewExport();
      } else {
        bulkMode("acts");
      }
    }
  };
  if (!wasBusy) {
    finish();
    return;
  }
  fetch(selectionExportUrl("/cancel"), {
    method: "POST",
    credentials: "same-origin",
    headers: { Accept: "application/json" }
  }).catch(() => {}).finally(finish);
}

function syncPickerExportChrome() {
  const panel = $("#bulkExport");
  if (!panel) return;
  const back = panel.querySelector("[data-act='bulk-export-back']");
  const title = panel.querySelector(".bulksel-ft");
  if (pickerExportMode) {
    if (back) back.hidden = true;
    if (title) title.hidden = true;
  } else {
    if (back) back.hidden = false;
    if (title) {
      title.hidden = false;
      title.textContent = "Exporter la sélection";
    }
  }
}

function relocatePickerExportRunBar(toPage) {
  const panel = $("#bulkExport");
  const run = panel && (panel.querySelector(".bulksel-run") || document.querySelector(".tp-export-actions .bulksel-run"));
  const slot = document.getElementById("tpExportActions");
  if (!run) return;
  if (toPage && slot) {
    if (run.parentElement !== slot) slot.appendChild(run);
    return;
  }
  if (panel && run.parentElement !== panel) panel.appendChild(run);
}

function openPickerThesaurusExport(id, title, terms) {
  const thesoId = (id || "").trim();
  if (!thesoId) return false;
  const host = document.getElementById("tpExportHost");
  const meta = document.getElementById("tpExportMeta");
  const panel = $("#bulkExport");
  if (!host || !meta || !panel) return false;

  if (pickerExportMode && exportBusy) return false;

  meta.setAttribute("data-active", "1");
  meta.setAttribute("data-thesaurus-id", thesoId);
  meta.setAttribute("data-thesaurus-title", title || thesoId);
  meta.setAttribute("data-concept-count", String(Number(terms) > 0 ? Number(terms) : 0));

  if (!bulkExportHome) {
    bulkExportHome = { parent: panel.parentElement, next: panel.nextSibling };
  }
  if (panel.parentElement !== host) {
    host.appendChild(panel);
  }
  panel.hidden = false;
  panel.classList.add("tp-export-panel");
  relocatePickerExportRunBar(true);

  pickerExportPrevAll = !!state.selectedAllThesaurus;
  state.selectedAllThesaurus = true;
  pickerExportMode = true;
  syncPickerExportChrome();
  resetExportPanelForNewExport();
  return false;
}

function closePickerThesaurusExport() {
  if (!pickerExportMode) return;
  if (exportBusy) {
    cancelSelectionExport(false);
  }
  const meta = document.getElementById("tpExportMeta");
  const panel = $("#bulkExport");
  if (meta) {
    meta.setAttribute("data-active", "0");
    meta.removeAttribute("data-thesaurus-id");
    meta.removeAttribute("data-thesaurus-title");
    meta.removeAttribute("data-concept-count");
  }
  relocatePickerExportRunBar(false);
  if (panel) {
    panel.classList.remove("tp-export-panel");
    panel.hidden = true;
    if (bulkExportHome && bulkExportHome.parent) {
      if (bulkExportHome.next && bulkExportHome.next.parentNode === bulkExportHome.parent) {
        bulkExportHome.parent.insertBefore(panel, bulkExportHome.next);
      } else {
        bulkExportHome.parent.appendChild(panel);
      }
    }
  }
  bulkExportHome = null;
  state.selectedAllThesaurus = pickerExportPrevAll;
  pickerExportPrevAll = false;
  pickerExportMode = false;
  syncPickerExportChrome();
}

window.openPickerThesaurusExport = openPickerThesaurusExport;
window.closePickerThesaurusExport = closePickerThesaurusExport;
