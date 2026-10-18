/**
 * OpenTheso — export de la sélection.
 */
"use strict";


let exportPollTimer = null;
var exportBusy = false;
let exportDownloaded = false;
let exportPollStarted = 0;
let exportRunStartedAt = 0;
let exportOptionsCache = { languages: [], groups: [], workLanguage: "fr" };
const EXPORT_PREFS_KEY = "ot-export-prefs";
const EXPORT_POLL_MAX_MS = 15 * 60 * 1000;
let pickerExportMode = false;
let bulkExportHome = null;
let pickerExportPrevAll = false;

window.addEventListener("beforeunload", (e) => {
  if (!exportBusy) return;
  e.preventDefault();
  e.returnValue = "";
});


function selectionExportUrl(path, extra) {
  const ctx = document.body.getAttribute("data-ctx") || "";
  let url = ctx + "/api/selection-export" + (path || "");
  const params = [];
  const token = csrfToken();
  if (token) params.push("csrfToken=" + encodeURIComponent(token));
  if (extra) {
    Object.keys(extra).forEach(key => {
      if (extra[key] == null || extra[key] === "") return;
      params.push(encodeURIComponent(key) + "=" + encodeURIComponent(extra[key]));
    });
  }
  if (params.length) url += "?" + params.join("&");
  return url;
}

function exportFormat() {
  const kindBtn = $("#bulkExportKinds .xkind-b.is-on");
  const kind = kindBtn && kindBtn.getAttribute("data-kind");
  if (kind === "pdf") return "pdf";
  const fold = kind === "csv" ? $("#bulkExportCsvFmts") : $("#bulkExportSkosFmts");
  const on = fold && $(".xchip[data-fmt].is-on", fold);
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

function selectedExportMembers() {
  if (typeof selectedCollectionMembers === "function") return selectedCollectionMembers();
  return Array.from((state && state.selected) || []);
}

function exportSelectionIds() {
  const members = new Set(selectedExportMembers());
  if (!members.size) return [];
  if (exportIncludeDescendants() && typeof selectionRootIds === "function") {
    return selectionRootIds().filter((id) => members.has(id));
  }
  return Array.from(members);
}

function canRunSelectionExport() {
  if (pickerExportMode || (state && state.selectedAllThesaurus)) return true;
  return selectedExportMembers().length > 0;
}

function paintExportMembers() {
  const host = $("#bulkExportMembers");
  const count = $("#bulkExportMemberN");
  if (!host) return;
  const whole = !!(state && state.selectedAllThesaurus);
  if (whole) {
    const n = Math.max(selectedCount(), typeof thesaurusConceptCount === "function" ? thesaurusConceptCount() : 0);
    host.innerHTML = '<p class="bap-members-empty">L’export portera sur l’ensemble du thésaurus.</p>';
    if (count) count.textContent = n > 0 ? n.toLocaleString("fr-FR") + " concept" + (n > 1 ? "s" : "") : "thésaurus entier";
    return;
  }
  const members = selectedExportMembers();
  if (typeof renderSelectionChips === "function") {
    renderSelectionChips(
      host,
      members.map(id => ({
        id: id,
        label: typeof selectionPrefLabel === "function" ? selectionPrefLabel(id) : id
      })),
      '<p class="bap-members-empty">Aucun concept dans la sélection. Les facettes ne sont pas exportées.</p>'
    );
  }
  if (typeof withSelectionPrefLabels === "function") withSelectionPrefLabels(members, paintExportMembers);
  if (count) {
    const n = members.length;
    count.textContent = n + " concept" + (n > 1 ? "s" : "");
  }
}

function syncExportRun() {
  const run = $("#bulkExportRun");
  if (!run || exportBusy || run.hidden) return;
  run.classList.toggle("is-off", !canRunSelectionExport());
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
  reopenExportAfterOptionChange();
}

function reopenExportAfterOptionChange() {
  if (exportBusy) return;
  setExportActions("idle");
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
  return $$("#bulkExportLangs .tree-lang-opt.is-on").map(el => el.getAttribute("data-code")).filter(Boolean);
}

function selectedExportGroupIds() {
  return $$("#bulkExportGroups .xpick-i.is-on").map(el => el.getAttribute("data-id")).filter(Boolean);
}

/** Drapeau emoji à partir d’un code ISO 639-1 (même mapping que ThesaurusPickerBean). */
function exportLangFlag(code) {
  let c = String(code || "").trim().toLowerCase();
  if (c.indexOf("-") >= 0) c = c.slice(0, c.indexOf("-"));
  if (!c) return "🏳️";
  const region = ({
    en: "gb", ar: "sa", zh: "cn", ja: "jp", ko: "kr", el: "gr", uk: "ua",
    cs: "cz", da: "dk", sv: "se", nb: "no", nn: "no", no: "no", he: "il",
    fa: "ir", hi: "in", eu: "es", ca: "es", gl: "es", cy: "gb", pt: "pt", la: "va"
  })[c] || (c.length === 2 ? c : "");
  if (!/^[a-z]{2}$/.test(region)) return "🏳️";
  return String.fromCodePoint(0x1F1E6 + (region.charCodeAt(0) - 97), 0x1F1E6 + (region.charCodeAt(1) - 97));
}

function csvLangOptions() {
  return $$("#bulkExportLangs .tree-lang-opt[data-code]");
}

function setExportLangSelection(all) {
  csvLangOptions().forEach(btn => {
    btn.classList.toggle("is-on", !!all);
    btn.setAttribute("aria-selected", all ? "true" : "false");
  });
  refreshExportLangMeta();
  reopenExportAfterOptionChange();
}

function closeExportCsvLangPicker(exceptBtn) {
  /* Sur la page export picker, la liste CSV reste toujours ouverte. */
  if (pickerExportMode) return;
  $$(".xcsv-lang-btn.is-open").forEach(btn => {
    if (exceptBtn && btn === exceptBtn) return;
    btn.classList.remove("is-open");
    btn.setAttribute("aria-expanded", "false");
  });
}

function paintCsvLangSummary() {
  const flagsEl = $("#bulkExportLangFlags");
  const summaryEl = $("#bulkExportLangSummary");
  const btn = $("#bulkExportLangBtn");
  const opts = csvLangOptions();
  const selected = opts.filter(el => el.classList.contains("is-on"));
  const total = opts.length;
  const n = selected.length;
  if (flagsEl) {
    if (!n) {
      flagsEl.textContent = "🏳️";
    } else {
      flagsEl.innerHTML = selected.slice(0, 3).map(el => {
        const code = el.getAttribute("data-code") || "";
        return "<span class=\"xcsv-lang-fl\">" + exportLangFlag(code) + "</span>";
      }).join("") + (n > 3 ? "<span class=\"xcsv-lang-more\">+" + (n - 3) + "</span>" : "");
    }
  }
  if (summaryEl) {
    if (!total) summaryEl.textContent = "Aucune langue";
    else if (!n) summaryEl.textContent = "Aucune sélectionnée";
    else if (n === total) summaryEl.textContent = "Toutes les langues";
    else if (n === 1) {
      const only = selected[0];
      summaryEl.textContent = only.getAttribute("data-label") || only.getAttribute("data-code") || "1 langue";
    } else {
      summaryEl.textContent = n + " langues sélectionnées";
    }
  }
  if (btn) {
    btn.classList.toggle("is-empty", !n);
    btn.setAttribute("aria-label", summaryEl ? summaryEl.textContent : "Langues");
  }
}

function refreshExportLangMeta() {
  const meta = $("#bulkExportLangMeta");
  const total = csvLangOptions().length;
  const n = selectedExportLangCodes().length;
  if (meta) {
    if (!total) meta.textContent = "";
    else meta.textContent = n + " / " + total;
  }
  paintCsvLangSummary();
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
  const work = String(exportOptionsCache.workLanguage || "").toLowerCase();
  const langBox = $("#bulkExportLangs");
  if (langBox) {
    langBox.innerHTML =
      "<div class=\"tree-lang-menu-h\">Langues à exporter</div>" +
      langs.map(lang => {
        const code = String(lang.code || "").trim();
        const label = String(lang.label || code || "").trim();
        const isWork = code.toLowerCase() === work && !!work;
        return "<button type=\"button\" class=\"tree-lang-opt is-on\"" +
          " data-act=\"export-lang\" data-code=\"" + escapeHtml(code) + "\"" +
          " data-label=\"" + escapeHtml(label || code) + "\"" +
          " role=\"option\" aria-selected=\"true\"" +
          " title=\"" + escapeHtml(label + (code ? " (" + code + ")" : "")) + "\">" +
          "<span class=\"tree-lang-opt-flag\" aria-hidden=\"true\">" + exportLangFlag(code) + "</span>" +
          "<span class=\"tree-lang-opt-name\">" + escapeHtml(label || code) + "</span>" +
          "<span class=\"tree-lang-opt-code\">" + escapeHtml((code || "").toUpperCase()) + "</span>" +
          (isWork ? "<span class=\"xlang-work\">travail</span>" : "") +
          "<span class=\"tree-lang-opt-check\" aria-hidden=\"true\">✓</span>" +
          "</button>";
      }).join("");
    refreshExportLangMeta();
  }
  const groupBox = $("#bulkExportGroups");
  if (groupBox) {
    groupBox.innerHTML = groups.map(group =>
      "<button type=\"button\" class=\"xpick-i\" data-act=\"export-group-id\" data-id=\"" +
      escapeHtml(group.id || "") + "\">" + escapeHtml(group.label || group.id || "") + "</button>"
    ).join("");
  }
  fillPdfLangPickers(langs, exportOptionsCache.workLanguage);
}

function closeExportPdfLangPickers(exceptBtn) {
  $$(".xpdf-lang-btn.is-open").forEach(btn => {
    if (exceptBtn && btn === exceptBtn) return;
    btn.classList.remove("is-open");
    btn.setAttribute("aria-expanded", "false");
  });
}

function pdfLangPickerEls(slot) {
  const n = String(slot || "");
  return {
    hidden: $("#bulkExportLang" + n),
    btn: $("#bulkExportLang" + n + "Btn"),
    menu: $("#bulkExportLang" + n + "Menu"),
    pick: $("#bulkExportLang" + n + "Pick")
  };
}

function paintPdfLangButton(slot, code, label) {
  const { btn } = pdfLangPickerEls(slot);
  if (!btn) return;
  const flag = btn.querySelector(".tree-lang-flag");
  const name = btn.querySelector(".tree-lang-name");
  const codeEl = btn.querySelector(".tree-lang-code");
  const empty = !code;
  if (flag) flag.textContent = empty ? "🏳️" : exportLangFlag(code);
  if (name) name.textContent = empty ? "—" : (label || code);
  if (codeEl) {
    codeEl.textContent = empty ? "" : String(code).toUpperCase();
    codeEl.hidden = empty;
  }
  btn.classList.toggle("is-empty", empty);
  btn.setAttribute("aria-label", empty
    ? (slot === 2 ? "Langue 2 — aucune" : "Langue 1")
    : ((label || code) + " (" + code + ")"));
}

function setPdfLangValue(slot, code, label) {
  const { hidden, menu } = pdfLangPickerEls(slot);
  if (hidden) hidden.value = code || "";
  paintPdfLangButton(slot, code || "", label || "");
  if (menu) {
    $$(".tree-lang-opt", menu).forEach(opt => {
      const on = (opt.getAttribute("data-code") || "") === (code || "");
      opt.classList.toggle("is-on", on);
      opt.setAttribute("aria-selected", on ? "true" : "false");
    });
  }
}

function fillPdfLangPickers(langs, workLanguage) {
  const list = langs || [];
  const work = String(workLanguage || "").toLowerCase();
  const default1 = list.find(l => String(l.code || "").toLowerCase() === work) || list[0] || { code: "", label: "—" };
  const prev1 = ($("#bulkExportLang1") && $("#bulkExportLang1").value) || "";
  const prev2 = ($("#bulkExportLang2") && $("#bulkExportLang2").value) || "";
  const keep1 = list.find(l => String(l.code || "") === prev1) || default1;
  const keep2 = list.find(l => String(l.code || "") === prev2);

  [1, 2].forEach(slot => {
    const { menu } = pdfLangPickerEls(slot);
    if (!menu) return;
    const withEmpty = slot === 2;
    const opts = (withEmpty ? [{ code: "", label: "Aucune" }] : []).concat(list);
    menu.innerHTML =
      "<div class=\"tree-lang-menu-h\">Langue " + slot + "</div>" +
      opts.map(lang => {
        const code = String(lang.code || "");
        const label = String(lang.label || (code ? code : "Aucune"));
        return "<button type=\"button\" class=\"tree-lang-opt\"" +
          " data-act=\"export-pdf-lang\" data-slot=\"" + slot + "\"" +
          " data-code=\"" + escapeHtml(code) + "\"" +
          " data-label=\"" + escapeHtml(label) + "\"" +
          " role=\"option\" aria-selected=\"false\">" +
          "<span class=\"tree-lang-opt-flag\" aria-hidden=\"true\">" +
          (code ? exportLangFlag(code) : "—") + "</span>" +
          "<span class=\"tree-lang-opt-name\">" + escapeHtml(label) + "</span>" +
          (code ? "<span class=\"tree-lang-opt-code\">" + escapeHtml(code.toUpperCase()) + "</span>" : "") +
          "<span class=\"tree-lang-opt-check\" aria-hidden=\"true\">✓</span>" +
          "</button>";
      }).join("");
  });

  setPdfLangValue(1, keep1.code || "", keep1.label || keep1.code || "");
  setPdfLangValue(2, keep2 ? (keep2.code || "") : "", keep2 ? (keep2.label || keep2.code || "") : "");
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
  showExportFold($("#bulkExportPdfFmts"), pdf);
  showExportFold($("#bulkExportDescRow"), !whole && fmt !== "csv-structured");
  showExportFold($("#bulkExportHtmlRow"), skosOrCsvFull || pdf);
  showExportFold($("#bulkExportZipRow"), whole && skosOrCsvFull);
  showExportFold($("#bulkExportDelimRow"), csv);
  showExportFold($("#bulkExportLangRow"), fmt === "csv");
  showExportFold($("#bulkExportPdfLangRow"), pdf);
  showExportFold($("#bulkExportImgRow"), pdf);
  const groupFilter = exportSwitchOn("bulkExportGroup");
  showExportFold($("#bulkExportGroupsWrap"), groupFilter);
  const more = $("#bulkExportMore");
  if (more && groupFilter) more.open = true;
  const help = $("#bulkExportHelp");
  if (help) {
    if (whole) help.textContent = "L’export porte sur l’ensemble du thésaurus.";
    else if (fmt === "csv-structured") help.textContent = "CSV structuré : arborescence des labels, sans option de descendants.";
    else if (exportIncludeDescendants()) help.textContent = "Les termes spécifiques des concepts cochés seront inclus.";
    else help.textContent = "Choisissez un format, les options, puis générez le fichier.";
  }
}

function scrollExportPanelBottom() {
  const bar = $("#previewView") || $("#viewBulk") || $("#bulkSel");
  if (!bar) return;
  const apply = () => { bar.scrollTop = bar.scrollHeight; };
  apply();
  requestAnimationFrame(() => {
    apply();
    window.setTimeout(apply, 380);
  });
}

function refreshExportSummary() {
  const whole = !!state.selectedAllThesaurus;
  const exact = selectedExportMembers().length;
  const n = whole ? Math.max(selectedCount(), thesaurusConceptCount()) : selectedCount();
  const desc = exportIncludeDescendants();
  const sum = $("#bulkExportSum");
  const est = $("#bulkExportEst");
  applyExportOptionVisibility();
  paintExportMembers();
  syncExportRun();
  if (sum) {
    if (whole) {
      if (n > 0) sum.textContent = n.toLocaleString("fr-FR") + " concept" + (n > 1 ? "s" : "") + " · thésaurus entier";
      else sum.textContent = "Export du thésaurus entier";
    } else if (desc) sum.textContent = n + " concept" + (n > 1 ? "s" : "") + " à exporter";
    else sum.textContent = exact + " concept" + (exact > 1 ? "s" : "") + " sélectionné" + (exact > 1 ? "s" : "");
  }
  if (est) {
    const count = whole ? Math.max(n, thesaurusConceptCount()) : n;
    let hint = formatLabelUi(exportFormat());
    if (count >= 500) hint += " · volume élevé";
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
  if (on) ensureExportProgressOverlay();
  exportBusy = !!on;
  const overlay = usesExportOverlay();
  // Sur la page thesauri : uniquement l'overlay tpLoading (comme l'import).
  // Pas de barre inline ni de flou is-export-lock en plus.
  document.body.classList.toggle("is-export-lock", exportBusy && !overlay);
  document.body.setAttribute("aria-busy", exportBusy ? "true" : "false");
  const panel = $("#bulkExport");
  if (panel) panel.classList.toggle("is-busy", exportBusy);
  if (overlay) {
    setExportPanes(true, false);
  } else {
    setExportPanes(!exportBusy, exportBusy || !!keepProgress);
  }
  if (exportBusy) setExportActions("busy");
  else if (keepProgress) setExportActions("done");
  else setExportActions("idle");
  if (exportBusy) {
    showExportOverlay();
    if (!overlay) {
      const prog = $("#bulkExportProg");
      requestAnimationFrame(() => {
        if (prog) prog.scrollIntoView({ block: "nearest", behavior: "smooth" });
      });
    }
  } else {
    hideExportOverlay(!!keepProgress);
    refreshExportSummary();
  }
}

function usesExportOverlay() {
  return !!document.getElementById("tpLoading");
}

function ensureExportProgressOverlay() {
  if (document.getElementById("tpLoading")) return;
  const box = document.createElement("div");
  box.id = "tpLoading";
  box.className = "tp-loading";
  box.hidden = true;
  box.setAttribute("hidden", "hidden");
  box.setAttribute("aria-live", "polite");
  box.setAttribute("aria-busy", "false");
  box.setAttribute("data-export-running", "Export du thésaurus…");
  box.setAttribute("data-export-done", "Export terminé");
  box.setAttribute("data-export-meta-tpl", "{0} / {1} concepts");
  box.setAttribute("data-meta-tpl", "{0} / {1} concepts");
  box.setAttribute("data-active-meta-tpl", "{0} / {1} concepts");
  box.setAttribute("data-eta-tpl", "≈ {0} restantes");
  box.setAttribute("data-eta-sec-tpl", "{0} s");
  box.setAttribute("data-eta-min-tpl", "{0} min");
  box.setAttribute("data-rate-tpl", "{0}/s");
  box.setAttribute("data-export-step-0", "Préparer");
  box.setAttribute("data-export-step-1", "Lire");
  box.setAttribute("data-export-step-2", "Construire");
  box.setAttribute("data-export-step-3", "Fichier");
  box.innerHTML = [
    '<div class="tp-loading-card tp-prog-card">',
    '<div class="tp-prog-import">',
    '<div class="tp-prog-h"><span class="tp-prog-t" id="tpProgTitle">Export du thésaurus…</span><span class="tp-prog-pct" id="tpProgPct">0%</span></div>',
    '<div class="tp-prog-track" id="tpProgTrack" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0"><span class="tp-prog-fill" id="tpProgFill"></span></div>',
    '<div class="tp-prog-meta" id="tpProgMeta" hidden="hidden"><span class="tp-prog-count" id="tpProgCount"></span><span class="tp-prog-sep" aria-hidden="true">·</span><span class="tp-prog-eta" id="tpProgEta"></span><span class="tp-prog-sep" aria-hidden="true">·</span><span class="tp-prog-rate" id="tpProgRate"></span></div>',
    '<div class="tp-prog-detail" id="tpProgDetail"></div>',
    '<ol class="tp-prog-steps" id="tpProgSteps">',
    '<li data-step="0">Préparer</li><li data-step="1">Lire</li><li data-step="2">Construire</li><li data-step="3">Fichier</li>',
    '</ol>',
    '<div class="tp-prog-actions"><button type="button" class="bo-btn ghost" id="tpProgCancel" hidden="hidden">Annuler</button></div>',
    '</div></div>'
  ].join("");
  document.body.appendChild(box);
  const cancel = box.querySelector("#tpProgCancel");
  if (cancel) {
    cancel.addEventListener("click", () => cancelSelectionExport(false));
  }
}

function bindExportOverlayCancel() {
  const btn = document.getElementById("tpProgCancel");
  if (!btn || btn.getAttribute("data-export-bound") === "1") return;
  btn.setAttribute("data-export-bound", "1");
  btn.addEventListener("click", event => {
    const box = document.getElementById("tpLoading");
    if (!box || !box.classList.contains("is-export")) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    cancelSelectionExport(false);
  }, true);
}

function showExportOverlay(msg) {
  ensureExportProgressOverlay();
  if (!usesExportOverlay()) return;
  const box = document.getElementById("tpLoading");
  const title = msg
    || (box && box.getAttribute("data-export-running"))
    || "Export du thésaurus…";
  if (window.tpShowLoading) {
    if (!box.classList.contains("is-export") || box.hidden) {
      window.tpShowLoading(title, "export");
    } else if (title) {
      const t = document.getElementById("tpProgTitle");
      if (t) t.textContent = title;
    }
  } else {
    box.classList.add("is-export");
    box.hidden = false;
    box.removeAttribute("hidden");
    box.setAttribute("aria-busy", "true");
    document.body.classList.add("tp-is-loading");
    const t = document.getElementById("tpProgTitle");
    if (t && title) t.textContent = title;
  }
  bindExportOverlayCancel();
  const cancel = document.getElementById("tpProgCancel");
  if (cancel) {
    if (!cancel.getAttribute("data-import-label")) {
      cancel.setAttribute("data-import-label", cancel.textContent || "Annuler");
    }
    cancel.hidden = false;
    cancel.disabled = false;
    cancel.textContent = "Annuler";
  }
  const report = document.getElementById("tpProgReport");
  if (report) report.hidden = true;
}

function hideExportOverlay(keepBriefly) {
  if (!usesExportOverlay()) return;
  const box = document.getElementById("tpLoading");
  if (!box || !box.classList.contains("is-export")) return;
  if (window._tpExportOverlayHide) {
    window.clearTimeout(window._tpExportOverlayHide);
    window._tpExportOverlayHide = 0;
  }
  const hide = () => {
    if (window.tpHideLoading) window.tpHideLoading();
    else {
      box.hidden = true;
      box.setAttribute("hidden", "hidden");
      box.setAttribute("aria-busy", "false");
      box.classList.remove("is-export");
      document.body.classList.remove("tp-is-loading");
    }
    const cancel = document.getElementById("tpProgCancel");
    if (cancel) {
      cancel.hidden = true;
      const stored = cancel.getAttribute("data-label") || cancel.getAttribute("data-import-label");
      if (stored) cancel.textContent = stored;
    }
  };
  if (keepBriefly) {
    window._tpExportOverlayHide = window.setTimeout(() => {
      hide();
      window._tpExportOverlayHide = 0;
    }, 700);
    return;
  }
  hide();
}

function paintExportOverlay(data) {
  if (!usesExportOverlay()) return;
  const box = document.getElementById("tpLoading");
  const pct = Math.max(0, Math.min(100, Number(data && data.progress) || 0));
  const phaseCount = Math.max(1, Number(data && data.phaseCount) || 4);
  const idx = Number(data && data.phaseIndex);
  const phaseIndex = Number.isFinite(idx) ? Math.max(0, Math.min(phaseCount - 1, idx)) : 0;
  const done = Number(data && data.done) || 0;
  const total = Number(data && data.total) || 0;
  const doneAll = data && data.status === "done";
  const message = doneAll
    ? ((box && box.getAttribute("data-export-done")) || "Export terminé")
    : ((data && data.message) || (data && data.phaseLabel) || "");
  const pace = exportPace(data || {});
  showExportOverlay(message || ((box && box.getAttribute("data-export-running")) || "Export…"));
  if (window.tpPaintImportProgress) {
    window.tpPaintImportProgress(
      doneAll ? 100 : pct,
      doneAll ? phaseCount - 1 : phaseIndex,
      message,
      done,
      total,
      pace.eta,
      pace.rate
    );
    return;
  }
  paintExportCardFallback(box, doneAll ? 100 : pct, doneAll ? phaseCount - 1 : phaseIndex, message, done, total, pace);
}

function paintExportCardFallback(box, pct, step, message, done, total, pace) {
  const clamped = Math.max(0, Math.min(100, Number(pct) || 0));
  const fill = document.getElementById("tpProgFill");
  const track = document.getElementById("tpProgTrack");
  const pctEl = document.getElementById("tpProgPct");
  const title = document.getElementById("tpProgTitle");
  const meta = document.getElementById("tpProgMeta");
  const countEl = document.getElementById("tpProgCount");
  const etaEl = document.getElementById("tpProgEta");
  const rateEl = document.getElementById("tpProgRate");
  if (fill) fill.style.width = clamped + "%";
  if (track) track.setAttribute("aria-valuenow", String(Math.round(clamped)));
  if (pctEl) pctEl.textContent = Math.round(clamped) + "%";
  if (title && message) title.textContent = message;
  const hasCount = total > 0 && done >= 0;
  const etaTxt = box && pace ? formatExportEta(box, pace.eta) : "";
  const rateTxt = box && pace ? formatExportRate(box, pace.rate) : "";
  if (meta && countEl) {
    if (hasCount) {
      countEl.textContent = fmtExportTpl(box.getAttribute("data-active-meta-tpl") || box.getAttribute("data-export-meta-tpl"), done, total);
      if (etaEl) {
        etaEl.textContent = etaTxt;
        etaEl.hidden = !etaTxt;
      }
      if (rateEl) {
        rateEl.textContent = rateTxt;
        rateEl.hidden = !rateTxt;
      }
      meta.querySelectorAll(".tp-prog-sep").forEach((sep, idx) => {
        if (idx === 0) sep.hidden = !etaTxt && !rateTxt;
        if (idx === 1) sep.hidden = !etaTxt || !rateTxt;
      });
      meta.hidden = false;
    } else {
      meta.hidden = true;
    }
  }
  if (box) {
    box.querySelectorAll(".tp-prog-steps li").forEach(li => {
      const s = Number(li.getAttribute("data-step"));
      li.classList.toggle("is-done", s < step || clamped >= 100);
      li.classList.toggle("is-on", s === step && clamped < 100);
    });
  }
}

function formatExportEta(box, etaSeconds) {
  if (window.tpFmtEta) return window.tpFmtEta(box, etaSeconds);
  if (etaSeconds == null || etaSeconds < 0) return "";
  const amount = etaSeconds < 60
    ? fmtExportTpl(box.getAttribute("data-eta-sec-tpl"), Math.max(1, Math.round(etaSeconds)))
    : fmtExportTpl(box.getAttribute("data-eta-min-tpl"), Math.max(1, Math.round(etaSeconds / 60)));
  return fmtExportTpl(box.getAttribute("data-eta-tpl"), amount);
}

function formatExportRate(box, rate) {
  if (window.tpFmtRate) return window.tpFmtRate(box, rate);
  if (!rate || rate <= 0) return "";
  const shown = rate >= 10 ? Math.round(rate) : (Math.round(rate * 10) / 10);
  return fmtExportTpl(box.getAttribute("data-rate-tpl"), shown);
}

function setExportActions(mode) {
  const run = $("#bulkExportRun");
  const dl = $("#bulkExportDl");
  const cancel = $("#bulkExportCancel");
  if (cancel) {
    cancel.hidden = false;
    cancel.disabled = false;
    cancel.textContent = (mode === "done" || mode === "retry") ? "Fermer" : "Annuler";
    cancel.classList.add("ghost");
    cancel.classList.remove("primary");
  }
  if (run) {
    run.hidden = mode === "done" || mode === "retry";
    run.disabled = mode === "busy";
    run.textContent = mode === "busy" ? "Génération…" : "Générer";
    run.classList.add("primary");
    run.classList.remove("ghost");
  }
  if (dl) {
    dl.hidden = mode !== "retry" && mode !== "done";
    dl.disabled = false;
    dl.classList.add("primary");
  }
  if (mode === "idle") syncExportRun();
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
