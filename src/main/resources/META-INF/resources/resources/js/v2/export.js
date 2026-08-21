/**
 * OpenTheso V2 — export de la sélection.
 */
"use strict";


let exportPollTimer = null;
var exportBusy = false;
let exportDownloaded = false;
let exportPollStarted = 0;
let exportOptionsCache = { languages: [], groups: [], workLanguage: "fr" };
const EXPORT_PREFS_KEY = "ot-export-prefs";
const EXPORT_POLL_MAX_MS = 15 * 60 * 1000;

window.addEventListener("beforeunload", (e) => {
  if (!exportBusy) return;
  e.preventDefault();
  e.returnValue = "";
});


function selectionExportUrl(path) {
  const ctx = document.body.getAttribute("data-ctx") || "";
  let url = ctx + "/v2/api/selection-export" + (path || "");
  const token = csrfToken();
  if (token) url += (url.indexOf("?") >= 0 ? "&" : "?") + "csrfToken=" + encodeURIComponent(token);
  return url;
}

function exportFormat() {
  const kindBtn = $("#bulkExportKinds .xkind-b.is-on");
  const kind = kindBtn && kindBtn.getAttribute("data-kind");
  if (kind === "pdf") return "pdf";
  const on = $(".xchip[data-fmt].is-on", $("#bulkExport"));
  if (on) return on.getAttribute("data-fmt");
  return kind === "csv" ? "csv" : "rdf";
}

function exportKindOf(fmt) {
  if ((fmt || "").indexOf("csv") === 0) return "csv";
  if (fmt === "pdf") return "pdf";
  return "skos";
}

function exportIncludeDescendants() {
  const sw = $("#bulkExportDesc");
  return !!(sw && sw.classList.contains("on"));
}

function exportSwitchOn(id) {
  const sw = $("#" + id);
  return !!(sw && sw.classList.contains("on"));
}

function setExportSwitch(id, on) {
  const sw = $("#" + id);
  if (!sw) return;
  sw.classList.toggle("on", !!on);
  sw.setAttribute("aria-checked", String(!!on));
}

function loadExportPrefs() {
  try {
    return JSON.parse(localStorage.getItem(EXPORT_PREFS_KEY) || "{}") || {};
  } catch (err) {
    return {};
  }
}

function saveExportPrefs() {
  const prefs = {
    format: exportFormat(),
    descendants: exportIncludeDescendants(),
    clearHtml: exportSwitchOn("bulkExportHtml"),
    delimiter: exportDelimiter(),
    pdfType: exportPdfType(),
    zip: exportSwitchOn("bulkExportZip"),
    images: exportSwitchOn("bulkExportImg"),
    filterGroup: exportSwitchOn("bulkExportGroup")
  };
  try { localStorage.setItem(EXPORT_PREFS_KEY, JSON.stringify(prefs)); } catch (err) {}
}

function exportDelimiter() {
  const on = $("[data-act='export-delim'].is-on");
  return (on && on.getAttribute("data-delim")) || ",";
}

function exportPdfType() {
  const on = $("[data-act='export-pdf-type'].is-on");
  return (on && on.getAttribute("data-pdf")) || "hierarchical";
}

function selectedExportLangCodes() {
  return $$("#bulkExportLangs .xpick-i.is-on").map(el => el.getAttribute("data-code")).filter(Boolean);
}

function selectedExportGroupIds() {
  return $$("#bulkExportGroups .xpick-i.is-on").map(el => el.getAttribute("data-id")).filter(Boolean);
}

function initExportPanel() {
  exportDownloaded = false;
  showExportError("");
  setExportPanes(true, false);
  const fill = $("#bulkExportFill");
  if (fill) fill.style.width = "0%";
  const pct = $("#bulkExportPct");
  if (pct) pct.textContent = "0%";
  const title = $("#bulkExportProgT");
  if (title) title.textContent = "Étape 1 / 4 — Préparer";
  const detail = $("#bulkExportProgD");
  if (detail) detail.textContent = "";
  const count = $("#bulkExportCount");
  if (count) {
    count.hidden = true;
    count.textContent = "";
  }
  $$("#bulkExportSteps li").forEach(li => li.classList.remove("is-on", "is-done", "is-enter"));
  const rail = $("#bulkExportRail");
  if (rail) rail.style.width = "0%";
  lastExportPhase = -1;
  const prefs = loadExportPrefs();
  setExportFormat(prefs.format || "rdf");
  setExportSwitch("bulkExportDesc", prefs.descendants !== false);
  setExportSwitch("bulkExportHtml", !!prefs.clearHtml);
  setExportSwitch("bulkExportGroup", false);
  setExportSwitch("bulkExportZip", !!prefs.zip);
  setExportSwitch("bulkExportImg", !!prefs.images);
  $$("[data-act='export-delim']").forEach(btn => {
    btn.classList.toggle("is-on", btn.getAttribute("data-delim") === (prefs.delimiter || ","));
  });
  $$("[data-act='export-pdf-type']").forEach(btn => {
    btn.classList.toggle("is-on", btn.getAttribute("data-pdf") === (prefs.pdfType || "hierarchical"));
  });
  const panel = $("#bulkExport");
  if (panel) panel.classList.remove("is-busy");
  setExportActions("idle");
  refreshExportSummary();
  loadExportOptions();
}

function resetExportPanelForNewExport() {
  stopExportPoll();
  exportDownloaded = false;
  exportPollStarted = 0;
  setExportBusy(false);
  initExportPanel();
}

function loadExportOptions() {
  const theso = thesaurusId();
  if (!theso) return;
  fetch(selectionExportUrl("/options") + (selectionExportUrl("/options").indexOf("?") >= 0 ? "&" : "?") + "thesaurusId=" + encodeURIComponent(theso), {
    credentials: "same-origin",
    headers: { Accept: "application/json" }
  }).then(res => res.ok ? res.json() : Promise.reject())
    .then(data => {
      exportOptionsCache = data || exportOptionsCache;
      renderExportOptions();
      applyExportOptionVisibility();
    })
    .catch(() => applyExportOptionVisibility());
}

function renderExportOptions() {
  const langs = exportOptionsCache.languages || [];
  const groups = exportOptionsCache.groups || [];
  const langBox = $("#bulkExportLangs");
  if (langBox) {
    langBox.innerHTML = langs.map(lang =>
      "<button type=\"button\" class=\"xpick-i is-on\" data-act=\"export-lang\" data-code=\"" +
      escapeHtml(lang.code || "") + "\">" + escapeHtml(lang.label || lang.code || "") + "</button>"
    ).join("");
  }
  const groupBox = $("#bulkExportGroups");
  if (groupBox) {
    groupBox.innerHTML = groups.map(group =>
      "<button type=\"button\" class=\"xpick-i\" data-act=\"export-group-id\" data-id=\"" +
      escapeHtml(group.id || "") + "\">" + escapeHtml(group.label || group.id || "") + "</button>"
    ).join("");
  }
  fillLangSelect($("#bulkExportLang1"), langs, exportOptionsCache.workLanguage, false);
  fillLangSelect($("#bulkExportLang2"), langs, "", true);
}

function fillLangSelect(sel, langs, current, withEmpty) {
  if (!sel) return;
  const opts = (withEmpty ? [{ code: "", label: "—" }] : []).concat(langs);
  sel.innerHTML = opts.map(lang =>
    "<option value=\"" + escapeHtml(lang.code || "") + "\"" +
    ((lang.code || "") === (current || "") ? " selected=\"selected\"" : "") + ">" +
    escapeHtml(lang.label || lang.code || "—") + "</option>"
  ).join("");
}

function showExportFold(el, on) {
  if (!el) return;
  el.classList.toggle("is-off", !on);
  el.setAttribute("aria-hidden", on ? "false" : "true");
}

function setExportPanes(optsOn, workOn) {
  const opts = $("#bulkExportOpts");
  const prog = $("#bulkExportProg");
  if (opts) opts.classList.toggle("is-on", !!optsOn);
  if (prog) {
    prog.classList.toggle("is-on", !!workOn);
    prog.setAttribute("aria-hidden", workOn ? "false" : "true");
  }
}

function applyExportOptionVisibility() {
  const fmt = exportFormat();
  const kind = exportKindOf(fmt);
  const whole = !!state.selectedAllThesaurus;
  const csv = kind === "csv";
  const pdf = kind === "pdf";
  const skosOrCsvFull = kind === "skos" || fmt === "csv";
  showExportFold($("#bulkExportSkosFmts"), kind === "skos");
  showExportFold($("#bulkExportCsvFmts"), kind === "csv");
  showExportFold($("#bulkExportDescRow"), !whole && fmt !== "csv-structured");
  showExportFold($("#bulkExportHtmlRow"), skosOrCsvFull || pdf);
  showExportFold($("#bulkExportZipRow"), whole && skosOrCsvFull);
  showExportFold($("#bulkExportDelimRow"), csv);
  showExportFold($("#bulkExportLangRow"), fmt === "csv");
  showExportFold($("#bulkExportPdfRow"), pdf);
  showExportFold($("#bulkExportPdfLangRow"), pdf);
  showExportFold($("#bulkExportImgRow"), pdf);
  const groupFilter = exportSwitchOn("bulkExportGroup");
  showExportFold($("#bulkExportGroupsWrap"), groupFilter);
  const help = $("#bulkExportHelp");
  if (help) {
    if (whole) help.textContent = "L’export porte sur l’ensemble du thésaurus.";
    else if (fmt === "csv-structured") help.textContent = "CSV structuré : arborescence des labels du thésaurus.";
    else help.textContent = "Choisissez un format, les options, puis générez le fichier.";
  }
}

function scrollExportPanelBottom() {
  const bar = $("#bulkSel");
  if (!bar) return;
  const apply = () => { bar.scrollTop = bar.scrollHeight; };
  apply();
  requestAnimationFrame(() => {
    apply();
    window.setTimeout(apply, 380);
  });
}

function refreshExportSummary() {
  const n = selectedCount();
  const exact = state.selected.size;
  const whole = !!state.selectedAllThesaurus;
  const desc = exportIncludeDescendants();
  const sum = $("#bulkExportSum");
  const est = $("#bulkExportEst");
  applyExportOptionVisibility();
  if (sum) {
    if (whole) sum.textContent = n + " concept" + (n > 1 ? "s" : "") + " · thésaurus entier";
    else if (desc) sum.textContent = n + " concept" + (n > 1 ? "s" : "") + " à exporter";
    else sum.textContent = exact + " concept" + (exact > 1 ? "s" : "") + " sélectionné" + (exact > 1 ? "s" : "");
  }
  if (est) {
    const count = whole ? Math.max(n, thesaurusConceptCount()) : n;
    let hint = formatLabelUi(exportFormat());
    if (count >= 500) hint += " · volume élevé, quelques instants";
    else if (count >= 80) hint += " · quelques secondes";
    else hint += " · rapide";
    est.textContent = hint;
  }
}

function formatLabelUi(fmt) {
  return ({
    rdf: "RDF/XML", jsonld: "JSON-LD", turtle: "Turtle", json: "JSON",
    csv: "CSV", "csv-id": "CSV réduit", "csv-structured": "CSV structuré",
    "csv-deprecated": "CSV dépréciés", pdf: "PDF"
  })[fmt] || fmt;
}

function setExportKind(kind) {
  $$("#bulkExportKinds .xkind-b").forEach(btn => {
    btn.classList.toggle("is-on", btn.getAttribute("data-kind") === kind);
  });
  if (kind === "csv" && exportKindOf(exportFormat()) !== "csv") setExportFormat("csv");
  else if (kind === "pdf") setExportFormat("pdf");
  else if (kind === "skos" && exportKindOf(exportFormat()) !== "skos") setExportFormat("rdf");
  applyExportOptionVisibility();
  saveExportPrefs();
}

function setExportFormat(fmt) {
  const kind = exportKindOf(fmt);
  $$("#bulkExportKinds .xkind-b").forEach(btn => {
    btn.classList.toggle("is-on", btn.getAttribute("data-kind") === kind);
  });
  $$("#bulkExport .xchip[data-fmt]").forEach(chip => {
    chip.classList.toggle("is-on", chip.getAttribute("data-fmt") === fmt);
  });
  applyExportOptionVisibility();
  saveExportPrefs();
}

function setExportBusy(on, keepProgress) {
  exportBusy = !!on;
  document.body.classList.toggle("is-export-lock", exportBusy);
  document.body.setAttribute("aria-busy", exportBusy ? "true" : "false");
  const panel = $("#bulkExport");
  if (panel) panel.classList.toggle("is-busy", exportBusy);
  setExportPanes(!exportBusy, exportBusy || !!keepProgress);
  if (exportBusy) setExportActions("busy");
  else if (keepProgress) setExportActions("done");
  else setExportActions("idle");
  if (exportBusy) {
    const prog = $("#bulkExportProg");
    requestAnimationFrame(() => {
      if (prog) prog.scrollIntoView({ block: "nearest", behavior: "smooth" });
    });
  } else {
    refreshExportSummary();
  }
}

function setExportActions(mode) {
  const run = $("#bulkExportRun");
  const dl = $("#bulkExportDl");
  const cancel = $("#bulkExportCancel");
  if (cancel) {
    cancel.hidden = false;
    cancel.disabled = false;
    if (mode === "done" || mode === "retry") {
      cancel.textContent = "Fermer";
      cancel.classList.toggle("primary", mode === "done");
      cancel.classList.toggle("ghost", mode !== "done");
    } else {
      cancel.textContent = "Annuler";
      cancel.classList.add("ghost");
      cancel.classList.remove("primary");
    }
  }
  if (run) {
    run.hidden = mode === "done" || mode === "retry";
    run.disabled = mode === "busy";
    run.textContent = mode === "busy" ? "Génération…" : "Générer";
    run.classList.add("primary");
    run.classList.remove("ghost");
  }
  if (dl) {
    dl.hidden = mode !== "retry";
    dl.disabled = false;
  }
}

function showExportError(msg) {
  const err = $("#bulkExportErr");
  if (!err) return;
  if (msg) {
    err.hidden = false;
    err.textContent = msg;
    toast(msg);
    requestAnimationFrame(() => {
      err.scrollIntoView({ block: "nearest", behavior: "smooth" });
    });
  } else {
    err.hidden = true;
    err.textContent = "";
  }
}

let lastExportPhase = -1;

function paintExportProgress(data) {
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
  fetch(selectionExportUrl(""), { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then(res => res.ok ? res.json() : Promise.reject())
    .then(onSelectionExportStatus)
    .catch(() => {});
}

function onSelectionExportStatus(data) {
  if (!data) return;
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
  return fetch(selectionExportUrl("/file"), { credentials: "same-origin" }).then(res => {
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
  const ids = whole
    ? []
    : (exportIncludeDescendants() ? selectionRootIds() : Array.from(state.selected));
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
  try {
    startSelectionExportRequest(theso, whole, ids, filterByGroup && groupIds.length > 0, groupIds);
  } catch (err) {
    setExportBusy(false);
    showExportError((err && err.message) || "L’export n’a pas pu démarrer.");
  }
}

function startSelectionExportRequest(theso, whole, ids, filterByGroup, groupIds) {
  exportDownloaded = false;
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
      throw new Error(readableExportError(data.error || data.message || "L’export n’a pas pu démarrer."));
    });
  }).then(data => {
    onSelectionExportStatus(data);
  }).catch(err => {
    setExportBusy(false);
    showExportError((err && err.message) || "L’export n’a pas pu démarrer.");
  });
}

function downloadReadyExport() {
  const dl = $("#bulkExportDl");
  if (dl) dl.disabled = true;
  downloadSelectionExport().then(() => {
    toast("Export téléchargé");
    setExportActions("done");
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
    if (andBack) bulkMode("acts");
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
