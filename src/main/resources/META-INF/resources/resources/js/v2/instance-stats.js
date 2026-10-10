/**
 * OpenTheso V2 — dashboard d'instance (admin).
 */
"use strict";

(function () {
  const state = {
    days: 30,
    thesaurusId: "",
    overview: null,
    quality: null,
    searchTab: "failed",
    drawerRows: [],
    thesauri: [],
    thQuery: "",
    fsFocus: null,
    fsHome: null,
    drawerHome: null
  };

  function root() {
    return document.getElementById("iaDash");
  }

  function msg(key, fallback) {
    const el = root();
    if (!el) return fallback || "";
    return el.getAttribute("data-msg-" + key) || fallback || "";
  }

  function ctx() {
    const el = root();
    return (el && el.getAttribute("data-ctx")) || document.body.getAttribute("data-ctx") || "";
  }

  function fmt(n) {
    const v = Number(n);
    return Number.isFinite(v) ? v.toLocaleString("fr-FR") : "—";
  }

  function fetchJson(path) {
    return fetch(ctx() + "/v2/api/instance-stats/" + path, {
      headers: { Accept: "application/json" },
      credentials: "same-origin"
    }).then((res) => {
      if (!res.ok) return Promise.reject(res.status);
      return res.json();
    });
  }

  function prefersReducedMotion() {
    return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  }

  function loadingHtml() {
    return '<div class="ia-dash-loading"><span class="mg-kpi-spin" aria-hidden="true"></span></div>';
  }

  function setLoading(ids) {
    ids.forEach((id) => {
      const el = document.getElementById(id);
      if (!el) return;
      el.setAttribute("aria-busy", "true");
      el.classList.remove("is-ready");
      el.style.animationDelay = "";
      el.innerHTML = loadingHtml();
    });
  }

  function resetKpis() {
    document.querySelectorAll("[data-dash-kpi]").forEach((el) => {
      el.classList.remove("is-ready");
      el.style.animationDelay = "";
      el.setAttribute("aria-busy", "true");
      el.innerHTML = '<span class="mg-kpi-spin" aria-hidden="true"></span>';
    });
  }

  function paintAfterLayout(fn) {
    requestAnimationFrame(() => requestAnimationFrame(fn));
  }

  function blankHtml() {
    return '<div class="ia-dash-blank">' + escapeHtml(msg("none", "—")) + "</div>";
  }

  function langFlag(code) {
    if (typeof exportLangFlag === "function") {
      return exportLangFlag(code);
    }
    return "🏳️";
  }

  function langChip(code) {
    const raw = String(code || "").trim();
    const text = raw ? raw.toUpperCase() : "—";
    return '<span class="ia-dash-lang">'
      + '<span class="ia-dash-flag" aria-hidden="true">' + langFlag(raw) + "</span>"
      + "<span>" + escapeHtml(text) + "</span></span>";
  }

  function langFlagsHtml(value) {
    const codes = String(value || "").split(/[,;|\s]+/).map((code) => code.trim()).filter(Boolean);
    if (!codes.length) return escapeHtml("—");
    return '<span class="ia-dash-langs-cell">' + codes.map(langChip).join("") + "</span>";
  }

  function drawerCell(row, col) {
    const raw = row[col.key];
    if (col.html === "langs") return langFlagsHtml(raw);
    return escapeHtml(raw == null || raw === "" ? "—" : String(raw));
  }

  function ready(el, delay, animate) {
    if (!el) return;
    el.removeAttribute("aria-busy");
    if (animate === false) {
      el.classList.remove("is-ready");
      el.style.animationDelay = "";
    } else {
      el.classList.remove("is-ready");
      el.style.animationDelay = prefersReducedMotion() ? "0ms" : ((delay || 0) + "ms");
      void el.offsetWidth;
      el.classList.add("is-ready");
    }
    if (window.syncViewRail) window.syncViewRail();
  }

  function fillKpis(kpis) {
    const map = {
      views: kpis && kpis.views,
      searches: kpis && kpis.searches,
      api: kpis && kpis.apiCalls,
      thesauri: kpis && kpis.activeThesauri
    };
    const reduce = prefersReducedMotion();
    document.querySelectorAll("[data-dash-kpi]").forEach((el, i) => {
      const key = el.getAttribute("data-dash-kpi");
      el.textContent = fmt(map[key]);
      el.removeAttribute("aria-busy");
      el.classList.remove("is-ready");
      el.style.animationDelay = reduce ? "0ms" : (i * 50) + "ms";
      void el.offsetWidth;
      el.classList.add("is-ready");
    });
  }

  function renderBars(container, items, delay, animate) {
    if (!container) return;
    const rows = items || [];
    if (!rows.length) {
      container.innerHTML = blankHtml();
      ready(container, delay, animate);
      return;
    }
    container.innerHTML = rows.map((item) => {
      const clickable = typeof item.onClick === "function";
      const tag = clickable ? "button" : "div";
      const type = clickable ? ' type="button"' : "";
      return "<" + tag + type + ' class="ia-dash-bar">'
        + '<span class="ia-dash-bar-l">' + (item.labelHtml || escapeHtml(item.label || "—")) + "</span>"
        + '<span class="ia-dash-bar-track"><i></i></span>'
        + '<span class="ia-dash-bar-n">' + escapeHtml(item.display || "") + "</span>"
        + "</" + tag + ">";
    }).join("");
    ready(container, delay, animate);
    const reduce = prefersReducedMotion();
    paintAfterLayout(() => {
      container.querySelectorAll(".ia-dash-bar").forEach((row, i) => {
        const fill = row.querySelector("i");
        const pct = Math.max(0, Math.min(100, Number(rows[i] && rows[i].widthPct) || 0));
        if (fill) {
          if (!reduce) fill.style.transitionDelay = (i * 70) + "ms";
          fill.style.width = pct + "%";
        }
        if (typeof rows[i].onClick === "function") {
          row.addEventListener("click", rows[i].onClick);
        }
      });
    });
  }

  function renderHits(box, rows, opts) {
    if (!box) return;
    const list = rows || [];
    const options = opts || {};
    const delay = options.delay || 0;
    if (!list.length) {
      box.innerHTML = blankHtml();
      ready(box, delay);
      return;
    }
    const max = list.reduce((acc, r) => Math.max(acc, Number(r.n) || 0), 0);
    box.innerHTML = list.map((r, i) => {
      const pct = max ? Math.round((Number(r.n) || 0) * 100 / max) : 0;
      const meth = r.method
        ? '<span class="ia-dash-meth is-' + escapeHtml(String(r.method).toLowerCase()) + '">'
          + escapeHtml(r.method) + "</span>"
        : "";
      const sub = r.sub
        ? '<div class="ia-dash-hit-s">' + escapeHtml(r.sub) + "</div>"
        : "";
      const mini = options.mini !== false
        ? '<span class="ia-dash-mini"><i data-width="' + pct + '"></i></span>'
        : "";
      return '<div class="ia-dash-hit" style="--i:' + i + '">'
        + '<span class="ia-dash-rank">' + (i + 1) + "</span>"
        + '<div class="ia-dash-hit-m">'
        + '<div class="ia-dash-hit-t">' + meth + escapeHtml(r.title || "—") + "</div>"
        + sub
        + "</div>"
        + '<div class="ia-dash-hit-v">'
        + '<span class="ia-dash-n">' + fmt(r.n) + "</span>"
        + mini
        + "</div></div>";
    }).join("");
    ready(box, delay);
    const reduce = prefersReducedMotion();
    paintAfterLayout(() => {
      box.querySelectorAll(".ia-dash-mini i").forEach((fill, i) => {
        if (!reduce) fill.style.transitionDelay = (i * 40) + "ms";
        fill.style.width = (fill.getAttribute("data-width") || "0") + "%";
      });
    });
  }

  function renderTraffic(points, delay) {
    const box = document.getElementById("iaDashTraffic");
    if (!box) return;
    const rows = points || [];
    const max = rows.reduce((acc, p) => Math.max(acc, Number(p.views) || 0), 0);
    const total = rows.reduce((acc, p) => acc + (Number(p.views) || 0), 0);
    if (!rows.length || !total) {
      box.innerHTML = blankHtml();
      ready(box, delay);
      return;
    }
    const unit = msg("traffic-unit", "consultation(s)");
    const peak = rows.reduce((best, p) => (Number(p.views) || 0) > (Number(best.views) || 0) ? p : best, rows[0]);
    const showAllValues = rows.length <= 14;
    const tickEvery = rows.length <= 8 ? 1 : (rows.length <= 40 ? 7 : Math.ceil(rows.length / 6));
    const trafficCard = document.getElementById("iaDashTrafficCard");
    const trackH = trafficTrackH(trafficCard, box);
    const cols = rows.map((p, i) => {
      const v = Number(p.views) || 0;
      const h = max && v ? Math.max(12, Math.round(v * trackH / max)) : 3;
      const tip = dateFull(p.date) + " · " + fmt(v) + " " + unit;
      const showN = v > 0 && (showAllValues || v === max);
      const zero = v ? "" : " is-zero";
      return '<div class="ia-dash-col' + zero + (showN ? " is-peak" : "") + '" data-tip="' + escapeHtml(tip) + '" style="--i:' + i + '">'
        + (showN ? '<span class="ia-dash-col-n">' + fmt(v) + "</span>" : "")
        + '<span class="ia-dash-col-bar" data-h="' + h + '"></span>'
        + "</div>";
    }).join("");
    const axis = rows.map((p, i) => {
      const show = i === 0 || i === rows.length - 1 || (i % tickEvery === 0 && i !== rows.length - 1);
      return '<span>' + (show ? escapeHtml(dateLabel(p.date)) : "") + "</span>";
    }).join("");
    const legend = total
      ? fmt(total) + " " + unit + " " + msg("traffic-total", "au total")
        + " · " + msg("traffic-peak", "Pic") + " " + dateFull(peak.date) + " (" + fmt(peak.views) + ")"
      : msg("none", "—");
    box.innerHTML = '<div class="ia-dash-spark-frame">'
      + '<div class="ia-dash-spark-y" aria-hidden="true"><span>' + fmt(max) + "</span><span>0</span></div>"
      + '<div class="ia-dash-spark-plot">'
      + '<div class="ia-dash-spark-track">' + cols + "</div>"
      + '<div class="ia-dash-spark-axis">' + axis + "</div>"
      + "</div></div>"
      + '<p class="ia-dash-spark-legend">' + escapeHtml(legend) + "</p>"
      + '<div class="ia-dash-spark-tip" hidden="hidden"></div>';
    ready(box, delay);
    const reduce = prefersReducedMotion();
    paintAfterLayout(() => {
      box.querySelectorAll(".ia-dash-col-bar").forEach((bar, i) => {
        if (!reduce) bar.style.transitionDelay = (i * 18) + "ms";
        bar.style.height = (bar.getAttribute("data-h") || "0") + "px";
      });
    });
  }

  const PIE_COLORS = [
    "#2f6f4e", "#c4a35a", "#3d6b99", "#b85c38", "#6b5b95",
    "#5a8f7b", "#d4a017", "#7a4e2d", "#4a90a4", "#9b6b6b"
  ];

  function pieColor(i) {
    return PIE_COLORS[i % PIE_COLORS.length];
  }

  function renderShare(byThesaurus, byLanguage, delay) {
    const card = document.getElementById("iaDashShareCard");
    const box = document.getElementById("iaDashShare");
    const title = document.getElementById("iaDashShareTitle");
    const cap = document.getElementById("iaDashShareCap");
    if (!card || !box) return;
    card.hidden = false;
    const langMode = !!state.thesaurusId;
    if (title) {
      title.textContent = langMode
        ? msg("share-langs", "Consultations par langue")
        : msg("share", "Répartition");
    }
    if (cap) {
      cap.textContent = langMode
        ? msg("share-langs-hint", "Langue d'ouverture des fiches concept")
        : msg("share-hint", "Part de chaque thésaurus");
    }
    const source = langMode ? (byLanguage || []) : (byThesaurus || []);
    const slices = source.map((row, i) => {
      const value = Number(row.views) || 0;
      if (langMode) {
        const lang = String(row.lang || "").trim();
        return { value: value, color: pieColor(i), lang: lang, label: lang.toUpperCase() || "—" };
      }
      return {
        value: value,
        color: pieColor(i),
        lang: "",
        label: row.label || row.thesaurusId || "—"
      };
    }).filter((slice) => slice.value > 0);
    if (!slices.length) {
      box.innerHTML = blankHtml();
      ready(box, delay);
      return;
    }
    const total = slices.reduce((acc, slice) => acc + slice.value, 0);
    let acc = 0;
    const stops = slices.map((slice) => {
      const from = acc;
      acc += (slice.value / total) * 100;
      slice.pct = Math.round((slice.value * 100) / total);
      return slice.color + " " + from.toFixed(2) + "% " + acc.toFixed(2) + "%";
    }).join(", ");
    const top = slices[0];
    const legend = slices.map((slice) => {
      const name = langMode ? langChip(slice.lang) : "<span>" + escapeHtml(slice.label) + "</span>";
      return '<li class="ia-dash-pie-item">'
        + '<span class="ia-dash-pie-swatch" style="background:' + slice.color + '"></span>'
        + '<span class="ia-dash-pie-copy">' + name + "</span>"
        + '<span class="ia-dash-pie-n">' + fmt(slice.value) + "</span>"
        + '<span class="ia-dash-pie-p">' + slice.pct + " %</span>"
        + "</li>";
    }).join("");
    box.innerHTML = '<div class="ia-dash-pie-wrap">'
      + '<div class="ia-dash-pie" style="background:conic-gradient(' + stops + ')">'
      + '<div class="ia-dash-pie-core"><strong>' + top.pct + " %</strong>"
      + "<span>" + escapeHtml(top.label) + "</span></div></div>"
      + '<ul class="ia-dash-pie-leg">' + legend + "</ul>"
      + "</div>";
    ready(box, delay);
  }

  function renderTop(concepts, delay) {
    renderHits(document.getElementById("iaDashTop"), (concepts || []).map((c) => {
      const langs = (c.languages || []).map((l) => (l.lang || "").toUpperCase()).filter(Boolean).join(" · ");
      const place = (c.thesaurusLabel || c.thesaurusId || "") + (langs ? " · " + langs : "");
      return {
        title: c.label || c.conceptId || "—",
        sub: place,
        n: c.views
      };
    }), { delay: delay });
  }

  function searchPack() {
    return (state.overview && state.overview.searches) || {};
  }

  function searchList(tab) {
    const searches = searchPack();
    if (tab === "synonyms") return searches.synonyms || [];
    if (tab === "global") return searches.global || [];
    return searches.failed || [];
  }

  function syncSearchTabs() {
    document.querySelectorAll("#iaDashSearchTabs [data-search]").forEach((btn) => {
      btn.classList.toggle("is-on", btn.getAttribute("data-search") === state.searchTab);
    });
  }

  function pickSearchTab() {
    const order = ["failed", "synonyms", "global"];
    if (!searchList(state.searchTab).length) {
      const next = order.find((tab) => searchList(tab).length);
      if (next) state.searchTab = next;
    }
    syncSearchTabs();
  }

  function searchRows() {
    if (state.searchTab === "synonyms") {
      return searchList("synonyms").map((s) => ({
        title: (s.searchedTerm || s.searched_term || "—") + " → " + (s.selectedTerm || s.selected_term || "—"),
        sub: s.thesaurusLabel || s.thesaurus_label || s.thesaurusId || s.thesaurus_id || "",
        n: s.occurrences
      }));
    }
    return searchList(state.searchTab).map((s) => ({
      title: s.term || s.searchedTerm || s.searched_term || "—",
      sub: s.thesaurusLabel || s.thesaurus_label || s.thesaurusId || s.thesaurus_id || "",
      n: s.occurrences
    }));
  }

  function renderSearch(delay) {
    renderHits(document.getElementById("iaDashSearch"), searchRows(), { delay: delay });
  }

  function renderApi(items, delay) {
    renderHits(document.getElementById("iaDashApi"), (items || []).map((r) => ({
      title: shortUrl(r.url),
      method: r.method || "",
      n: r.calls
    })), { mini: true, delay: delay });
  }

  function dateParts(d) {
    if (typeof d === "string" && d.length >= 10) {
      return { y: Number(d.slice(0, 4)), m: Number(d.slice(5, 7)), day: Number(d.slice(8, 10)) };
    }
    if (Array.isArray(d) && d.length >= 3) {
      return { y: Number(d[0]), m: Number(d[1]), day: Number(d[2]) };
    }
    if (d && typeof d === "object" && d.year != null && d.month != null && d.day != null) {
      return { y: Number(d.year), m: Number(d.month), day: Number(d.day) };
    }
    return null;
  }

  function dateLabel(d) {
    const p = dateParts(d);
    if (!p || !p.day || !p.m) return "";
    return String(p.day).padStart(2, "0") + "/" + String(p.m).padStart(2, "0");
  }

  function dateFull(d) {
    const p = dateParts(d);
    if (!p || !p.day || !p.m) return dateLabel(d);
    const dt = new Date(p.y, p.m - 1, p.day);
    if (Number.isNaN(dt.getTime())) return dateLabel(d);
    const loc = (document.documentElement.lang || "fr").slice(0, 2);
    return dt.toLocaleDateString(loc === "en" ? "en-GB" : "fr-FR", {
      weekday: "short",
      day: "numeric",
      month: "short"
    });
  }

  function shortUrl(url) {
    if (!url) return "—";
    try {
      const u = new URL(url);
      return u.pathname + u.search;
    } catch (e) {
      return String(url);
    }
  }

  function zoneLabel(zone) {
    if (zone === "excellent") return msg("zone-excellent", "Excellent");
    if (zone === "good") return msg("zone-good", "Bon niveau");
    if (zone === "ok") return msg("zone-ok", "Correct");
    return msg("zone-improve", "À améliorer");
  }

  function hideQuality() {
    const wrap = document.getElementById("iaDashQuality");
    if (!wrap) return;
    wrap.hidden = true;
    wrap.classList.remove("is-ready");
    wrap.style.animationDelay = "";
  }

  function renderQuality(q) {
    const wrap = document.getElementById("iaDashQuality");
    if (!wrap) return;
    if (!q) {
      hideQuality();
      return;
    }
    wrap.hidden = false;
    const score = q.score || {};
    const overall = Number(score.overall) || 0;
    const n = document.getElementById("iaDashScoreN");
    const z = document.getElementById("iaDashScoreZ");
    const meter = document.getElementById("iaDashMeter");
    if (n) {
      n.textContent = overall.toLocaleString("fr-FR", { maximumFractionDigits: 1 }) + " %";
      n.classList.remove("is-ready");
      void n.offsetWidth;
      n.classList.add("is-ready");
    }
    if (z) {
      z.textContent = zoneLabel(score.zone);
      z.setAttribute("data-zone", score.zone || "improve");
    }
    if (meter) {
      meter.style.setProperty("--p", "0");
      meter.setAttribute("data-zone", score.zone || "improve");
      paintAfterLayout(() => {
        meter.style.setProperty("--p", String(Math.max(0, Math.min(100, overall))));
      });
    }
    renderBars(document.getElementById("iaDashCriteria"), (score.criteria || []).map((c) => ({
      label: c.label,
      widthPct: Math.max(0, Math.min(100, Number(c.scorePercent) || 0)),
      display: Math.round(Number(c.scorePercent) || 0) + " %"
    })), 0, false);
    const langs = document.getElementById("iaDashLangs");
    if (langs) {
      langs.innerHTML = (q.languages || []).map((lg) =>
        '<span class="ia-dash-badge">' + langChip(lg) + "</span>"
      ).join("") || "";
    }
    const coverage = q.languageCoverage || [];
    const covMax = coverage.reduce((a, b) => Math.max(a, Number(b.conceptCount) || 0), 0);
    renderBars(document.getElementById("iaDashLangCov"), coverage.map((b) => ({
      label: b.languageCount + " langue" + (b.languageCount > 1 ? "s" : ""),
      widthPct: covMax ? Math.round((Number(b.conceptCount) || 0) * 100 / covMax) : 0,
      display: fmt(b.conceptCount),
      onClick: () => openTranslate(b.languageCount)
    })), 0, false);
    const avg = document.getElementById("iaDashLangAvg");
    if (avg) {
      avg.textContent = (Number(q.averageLanguagesPerConcept) || 0).toLocaleString("fr-FR", { maximumFractionDigits: 2 })
        + " " + msg("avg", "langue(s) / concept");
    }
    renderBars(document.getElementById("iaDashDefs"), (q.definitions || []).map((d) => ({
      labelHtml: langChip(d.lang),
      widthPct: Math.round(Number(d.percent) || 0),
      display: Math.round(Number(d.percent) || 0) + " % · " + fmt(d.withDefinition) + "/" + fmt(d.total),
      onClick: () => openMissing(d.lang)
    })), 0, false);
    ready(wrap, 0);
  }

  function parkOnBody(el, homeKey) {
    if (!el || el.parentNode === document.body) return;
    state[homeKey] = { parent: el.parentNode, next: el.nextSibling };
    document.body.appendChild(el);
  }

  function restoreHome(el, homeKey) {
    const home = state[homeKey];
    state[homeKey] = null;
    if (!el || !home || !home.parent) return;
    if (home.next && home.next.parentNode === home.parent) {
      home.parent.insertBefore(el, home.next);
    } else {
      home.parent.appendChild(el);
    }
  }

  function openDrawer(title, sub, rows, columns) {
    state.drawerRows = { columns: columns, rows: rows };
    const drawer = document.getElementById("iaDashDrawer");
    document.getElementById("iaDashDrawerTitle").textContent = title;
    document.getElementById("iaDashDrawerSub").textContent = sub;
    const body = document.getElementById("iaDashDrawerBody");
    if (!rows.length) {
      body.innerHTML = blankHtml();
    } else {
      body.innerHTML = '<table class="ia-tab"><thead><tr>'
        + columns.map((c) => "<th>" + escapeHtml(c.label) + "</th>").join("")
        + "</tr></thead><tbody>"
        + rows.map((row) => "<tr>" + columns.map((c) =>
          "<td>" + drawerCell(row, c) + "</td>"
        ).join("") + "</tr>").join("")
        + "</tbody></table>";
    }
    if (document.body.classList.contains("ia-dash-fs-lock")) parkOnBody(drawer, "drawerHome");
    drawer.hidden = false;
  }

  function closeDrawer() {
    const drawer = document.getElementById("iaDashDrawer");
    if (drawer) drawer.hidden = true;
    restoreHome(drawer, "drawerHome");
  }

  function fsIconHtml() {
    return '<svg class="ia-dash-fs-in" viewBox="0 0 24 24" aria-hidden="true">'
      + '<path d="M15 3h6v6"/><path d="M9 21H3v-6"/><path d="m21 3-7 7"/><path d="m3 21 7-7"/></svg>'
      + '<svg class="ia-dash-fs-out" viewBox="0 0 24 24" aria-hidden="true">'
      + '<path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>';
  }

  function fsLabel(on) {
    return on ? msg("fs-exit", "Fermer") : msg("fs", "Plein écran");
  }

  function trafficTrackH(card, box) {
    if (!card || !card.classList.contains("is-fs") || !box) return 112;
    return Math.max(200, Math.round((box.clientHeight || 280) - 64));
  }

  function syncFsButton(card) {
    const btn = card && card.querySelector(".ia-dash-fs");
    if (!btn) return;
    const on = card.classList.contains("is-fs");
    const label = fsLabel(on);
    btn.setAttribute("aria-pressed", on ? "true" : "false");
    btn.setAttribute("aria-label", label);
    btn.title = label;
  }

  function setFsDialog(card, on) {
    if (!card) return;
    if (on) {
      card.setAttribute("role", "dialog");
      card.setAttribute("aria-modal", "true");
    } else {
      card.removeAttribute("role");
      card.removeAttribute("aria-modal");
    }
  }

  function clearFsPlaceholder() {
    const ph = document.querySelector(".ia-dash-fs-ph");
    if (ph) ph.remove();
  }

  function refreshFsContent(card) {
    if (!card || !state.overview) return;
    if (card.id === "iaDashTrafficCard") renderTraffic(state.overview.traffic);
  }

  function fsKind(card) {
    if (!card) return "list";
    if (card.id === "iaDashQuality") return "quality";
    if (card.id === "iaDashTrafficCard") return "chart";
    if (card.id === "iaDashShareCard") return "pie";
    return "list";
  }

  function ensureFsOverlay() {
    let ov = document.getElementById("iaDashFsOv");
    if (ov) return ov;
    ov = document.createElement("div");
    ov.id = "iaDashFsOv";
    ov.className = "block-overlay";
    ov.hidden = true;
    ov.classList.add("is-off");
    ov.setAttribute("aria-hidden", "true");
    ov.innerHTML = '<div class="block-modal ia-dash-fs-modal" role="dialog" aria-modal="true">'
      + '<button type="button" class="ia-dash-fs-x" data-dash-fs-close="1" title="'
      + escapeHtml(msg("fs-exit", "Fermer")) + '" aria-label="'
      + escapeHtml(msg("fs-exit", "Fermer")) + '">'
      + '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>'
      + "</button></div>";
    ov.addEventListener("click", (e) => {
      if (e.target === ov) closeFs();
      const closeBtn = e.target && e.target.closest ? e.target.closest("[data-dash-fs-close]") : null;
      if (closeBtn && ov.contains(closeBtn)) closeFs();
    });
    document.body.appendChild(ov);
    return ov;
  }

  function closeFs(opts) {
    const silent = !!(opts && opts.silent);
    const ov = document.getElementById("iaDashFsOv");
    const card = document.querySelector(".ia-dash-card.is-fs");
    const restore = state.fsFocus;
    document.body.classList.remove("ia-dash-fs-lock");
    if (ov) {
      ov.hidden = true;
      ov.classList.add("is-off");
      ov.setAttribute("aria-hidden", "true");
      ov.classList.remove("is-quality", "is-chart", "is-pie", "is-list");
    }
    if (!card) {
      clearFsPlaceholder();
      return;
    }
    card.classList.remove("is-fs");
    setFsDialog(card, false);
    restoreHome(card, "fsHome");
    clearFsPlaceholder();
    syncFsButton(card);
    refreshFsContent(card);
    state.fsFocus = null;
    if (!silent && restore && typeof restore.focus === "function") restore.focus();
    if (window.syncViewRail) window.syncViewRail();
  }

  function openFs(card) {
    if (!card || card.classList.contains("is-fs")) return;
    closeThesaurusPicker();
    closeDrawer();
    closeFs({ silent: true });
    const ov = ensureFsOverlay();
    const modal = ov.querySelector(".ia-dash-fs-modal");
    const ph = document.createElement("div");
    ph.className = "ia-dash-fs-ph";
    ph.style.height = Math.max(card.offsetHeight, 1) + "px";
    card.after(ph);
    parkOnBody(card, "fsHome");
    if (modal) modal.appendChild(card);
    ov.classList.add("is-" + fsKind(card));
    ov.classList.remove("is-off");
    ov.hidden = false;
    ov.setAttribute("aria-hidden", "false");
    card.classList.add("is-fs");
    setFsDialog(card, true);
    document.body.classList.add("ia-dash-fs-lock");
    syncFsButton(card);
    const btn = card.querySelector(".ia-dash-fs");
    state.fsFocus = btn;
    window.requestAnimationFrame(function () {
      window.requestAnimationFrame(function () {
        refreshFsContent(card);
        if (btn) btn.focus();
        if (window.syncViewRail) window.syncViewRail();
      });
    });
  }

  function toggleFs(card) {
    if (!card) return;
    if (card.classList.contains("is-fs")) closeFs();
    else openFs(card);
  }

  function mountFsButtons() {
    const el = root();
    if (!el) return;
    el.querySelectorAll(".ia-dash-card").forEach((card) => {
      const header = card.querySelector(".ia-dash-h");
      if (!header || header.querySelector(".ia-dash-fs")) return;
      const tools = document.createElement("div");
      tools.className = "ia-dash-fs-tools";
      const kbd = document.createElement("span");
      kbd.className = "ia-dash-fs-kbd";
      kbd.textContent = msg("fs-esc", "Échap");
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "ia-dash-fs";
      btn.setAttribute("data-dash-fs", "1");
      btn.innerHTML = fsIconHtml();
      tools.appendChild(kbd);
      tools.appendChild(btn);
      header.appendChild(tools);
      syncFsButton(card);
    });
  }

  function downloadCsv() {
    const pack = state.drawerRows;
    if (!pack || !pack.rows || !pack.rows.length) return;
    const lines = [pack.columns.map((c) => c.label).join(";")];
    pack.rows.forEach((row) => {
      lines.push(pack.columns.map((c) => '"' + String(row[c.key] == null ? "" : row[c.key]).replace(/"/g, '""') + '"').join(";"));
    });
    const blob = new Blob(["\uFEFF" + lines.join("\n")], { type: "text/csv;charset=utf-8" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "statistiques.csv";
    a.click();
    URL.revokeObjectURL(a.href);
  }

  function drawerPage(body) {
    if (Array.isArray(body)) {
      return { total: body.length, items: body };
    }
    const items = (body && body.items) || [];
    const total = Number(body && body.total);
    return { total: Number.isFinite(total) ? total : items.length, items: items };
  }

  function drawerSub(total, shown, prefix) {
    const extra = shown < total ? " · aperçu " + fmt(shown) : "";
    return prefix + fmt(total) + " concept(s)" + extra;
  }

  function openTranslate(n) {
    if (!state.thesaurusId || !n) return;
    fetchJson("quality/to-translate?thesaurusId=" + encodeURIComponent(state.thesaurusId) + "&languages=" + n)
      .then((body) => {
        const page = drawerPage(body);
        openDrawer(
          "Concepts à traduire",
          drawerSub(page.total, page.items.length, n + " langue(s) — "),
          page.items,
          [
            { key: "label", label: "Libellé" },
            { key: "conceptId", label: "ID" },
            { key: "existingLangs", label: "Langues", html: "langs" }
          ]
        );
      })
      .catch(() => openDrawer("Concepts à traduire", "", [], []));
  }

  function openMissing(lang) {
    if (!state.thesaurusId || !lang) return;
    fetchJson("quality/missing-definitions?thesaurusId=" + encodeURIComponent(state.thesaurusId) + "&lang=" + encodeURIComponent(lang))
      .then((body) => {
        const page = drawerPage(body);
        openDrawer(
          "Sans définition",
          drawerSub(page.total, page.items.length, String(lang).toUpperCase() + " — "),
          page.items,
          [
            { key: "label", label: "Libellé" },
            { key: "conceptId", label: "ID" }
          ]
        );
      })
      .catch(() => openDrawer("Sans définition", "", [], []));
  }

  function applyOverview(body) {
    state.overview = body || {};
    fillKpis(state.overview.kpis);
    const empty = document.getElementById("iaDashEmpty");
    const loadError = !!(state.overview.loadError);
    if (empty) {
      empty.hidden = !loadError;
      if (loadError) {
        const title = empty.querySelector(".ia-empty-t");
        const sub = empty.querySelector(".ia-empty-d");
        if (title) title.textContent = msg("load-title", title.textContent);
        if (sub) sub.textContent = msg("load-sub", sub.textContent);
      }
    }
    renderTraffic(state.overview.traffic, 60);
    renderShare(state.overview.byThesaurus, state.overview.byLanguage, 120);
    renderTop(state.overview.topConcepts, 180);
    pickSearchTab();
    renderSearch(220);
    renderApi(state.overview.api, 260);
  }

  function loadOverview() {
    if (!hasThesaurus()) {
      setNeedState(true);
      return Promise.resolve();
    }
    setNeedState(false);
    resetKpis();
    setLoading(["iaDashTraffic", "iaDashShare", "iaDashTop", "iaDashSearch", "iaDashApi"]);
    const q = "overview?days=" + state.days
      + "&thesaurusId=" + encodeURIComponent(state.thesaurusId);
    return fetchJson(q).then(applyOverview).catch(() => applyOverview({
      kpis: { views: 0, searches: 0, apiCalls: 0, activeThesauri: 0 },
      traffic: [], byThesaurus: [], byLanguage: [], topConcepts: [], api: [],
      searches: { failed: [], synonyms: [], global: [] },
      noActivity: true,
      loadError: true
    }));
  }

  function loadQuality() {
    if (!state.thesaurusId) {
      state.quality = null;
      hideQuality();
      return Promise.resolve();
    }
    hideQuality();
    return fetchJson("quality?thesaurusId=" + encodeURIComponent(state.thesaurusId))
      .then((q) => { state.quality = q; renderQuality(q); })
      .catch(() => { state.quality = null; hideQuality(); });
  }

  function hasThesaurus() {
    return !!(state.thesaurusId && String(state.thesaurusId).trim());
  }

  function setNeedState(on) {
    const el = root();
    if (el) el.classList.toggle("is-need", !!on);
    const need = document.getElementById("iaDashNeed");
    if (need) need.hidden = !on;
    if (on) {
      hideQuality();
      closeFs({ silent: true });
    }
  }

  function chooseThesaurusLabel() {
    return msg("th-choose", "Choisir un thésaurus");
  }

  function currentThesaurus() {
    return state.thesauri.find((t) => t.id === state.thesaurusId) || null;
  }

  function thesaurusOpen() {
    const wrap = document.getElementById("iaDashThesaurus");
    return !!(wrap && wrap.classList.contains("is-open"));
  }

  function closeThesaurusPicker() {
    const wrap = document.getElementById("iaDashThesaurus");
    const menu = document.getElementById("iaDashThesaurusMenu");
    const btn = document.getElementById("iaDashThesaurusBtn");
    if (wrap) wrap.classList.remove("is-open");
    if (menu) menu.hidden = true;
    if (btn) btn.setAttribute("aria-expanded", "false");
  }

  function openThesaurusPicker() {
    const wrap = document.getElementById("iaDashThesaurus");
    const menu = document.getElementById("iaDashThesaurusMenu");
    const btn = document.getElementById("iaDashThesaurusBtn");
    const query = document.getElementById("iaDashThesaurusQuery");
    if (wrap) wrap.classList.add("is-open");
    if (menu) menu.hidden = false;
    if (btn) btn.setAttribute("aria-expanded", "true");
    paintThesaurusList();
    if (query) {
      query.value = state.thQuery || "";
      window.setTimeout(() => query.focus(), 0);
    }
  }

  function toggleThesaurusPicker() {
    if (thesaurusOpen()) closeThesaurusPicker();
    else openThesaurusPicker();
  }

  function setThesaurus(id) {
    const next = (id || "").trim();
    closeThesaurusPicker();
    if (!next || state.thesaurusId === next) return;
    state.thesaurusId = next;
    setNeedState(false);
    paintThesaurusButton();
    loadOverview();
    loadQuality();
  }

  function paintThesaurusButton() {
    const title = document.getElementById("iaDashThesaurusTitle");
    const sub = document.getElementById("iaDashThesaurusSub");
    const current = currentThesaurus();
    if (title) title.textContent = current ? (current.label || current.id) : chooseThesaurusLabel();
    if (sub) {
      sub.textContent = current
        ? [current.id, current.projectName].filter(Boolean).join(" · ")
        : "";
    }
  }

  function paintThesaurusList() {
    const box = document.getElementById("iaDashThesaurusList");
    if (!box) return;
    const q = (state.thQuery || "").trim().toLowerCase();
    const filtered = state.thesauri.filter((t) => {
      if (!q) return true;
      return [t.label, t.id, t.projectName].join(" ").toLowerCase().indexOf(q) >= 0;
    });
    if (!filtered.length) {
      box.innerHTML = '<div class="ia-dash-th-empty">' + escapeHtml(msg("th-empty", "—")) + "</div>";
      return;
    }
    const lock = '<span class="ia-dash-th-lock" title="' + escapeHtml(msg("th-private", "Privé")) + '">'
      + '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>'
      + "</span>";
    box.innerHTML = filtered.map((t) => {
      const on = (t.id || "") === (state.thesaurusId || "");
      const sub = [t.id, t.projectName].filter(Boolean).join(" · ");
      return '<button type="button" class="ia-dash-th-opt' + (on ? " is-on" : "") + '" data-th="'
        + escapeHtml(t.id || "") + '" role="option" aria-selected="' + on + '">'
        + '<span class="ia-dash-th-opt-t"><span class="ia-dash-th-opt-name">'
        + escapeHtml(t.label || t.id || "—") + "</span>"
        + (t.privateThesaurus ? lock : "") + "</span>"
        + (sub ? '<span class="ia-dash-th-opt-s">' + escapeHtml(sub) + "</span>" : "")
        + "</button>";
    }).join("");
  }

  function fillThesauri(list) {
    state.thesauri = (list || []).filter((t) => t && String(t.id || "").trim());
    if (state.thesaurusId && !currentThesaurus()) state.thesaurusId = "";
    paintThesaurusButton();
    paintThesaurusList();
    setNeedState(!hasThesaurus());
  }

  function bind() {
    const el = root();
    if (!el || el.getAttribute("data-bound") === "1") return;
    el.setAttribute("data-bound", "1");
    el.querySelectorAll(".ia-dash-chip[data-days]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.days = Number(btn.getAttribute("data-days")) || 30;
        el.querySelectorAll(".ia-dash-chip[data-days]").forEach((b) => b.classList.toggle("is-on", b === btn));
        if (hasThesaurus()) loadOverview();
      });
    });
    const thBtn = document.getElementById("iaDashThesaurusBtn");
    if (thBtn) thBtn.addEventListener("click", toggleThesaurusPicker);
    const needCta = document.getElementById("iaDashNeedCta");
    if (needCta) needCta.addEventListener("click", openThesaurusPicker);
    const thList = document.getElementById("iaDashThesaurusList");
    if (thList) {
      thList.addEventListener("click", (e) => {
        const opt = e.target && e.target.closest ? e.target.closest("[data-th]") : null;
        if (!opt) return;
        setThesaurus(opt.getAttribute("data-th") || "");
      });
    }
    const thQuery = document.getElementById("iaDashThesaurusQuery");
    if (thQuery) {
      thQuery.addEventListener("input", () => {
        state.thQuery = thQuery.value || "";
        paintThesaurusList();
      });
      thQuery.addEventListener("keydown", (e) => {
        if (e.key === "Enter") e.preventDefault();
        if (e.key === "Escape") {
          e.preventDefault();
          closeThesaurusPicker();
        }
      });
    }
    document.addEventListener("mousedown", (e) => {
      const wrap = document.getElementById("iaDashThesaurus");
      if (wrap && thesaurusOpen() && !wrap.contains(e.target)) closeThesaurusPicker();
    });
    el.addEventListener("click", (e) => {
      const tab = e.target && e.target.closest ? e.target.closest("#iaDashSearchTabs [data-search]") : null;
      if (!tab || !el.contains(tab)) return;
      e.preventDefault();
      state.searchTab = tab.getAttribute("data-search") || "failed";
      syncSearchTabs();
      renderSearch();
    });
    const traffic = document.getElementById("iaDashTraffic");
    if (traffic) {
      traffic.addEventListener("pointerover", (e) => {
        const col = e.target && e.target.closest ? e.target.closest(".ia-dash-col") : null;
        const tip = traffic.querySelector(".ia-dash-spark-tip");
        if (!col || !tip || !traffic.contains(col)) return;
        tip.textContent = col.getAttribute("data-tip") || "";
        tip.hidden = false;
      });
      traffic.addEventListener("pointermove", (e) => {
        const tip = traffic.querySelector(".ia-dash-spark-tip");
        if (!tip || tip.hidden) return;
        const r = traffic.getBoundingClientRect();
        const x = Math.min(r.width - 16, Math.max(16, e.clientX - r.left));
        tip.style.left = x + "px";
        tip.style.top = Math.max(12, e.clientY - r.top - 12) + "px";
      });
      traffic.addEventListener("pointerout", (e) => {
        const next = e.relatedTarget;
        if (next && traffic.contains(next) && next.closest && next.closest(".ia-dash-col")) return;
        const tip = traffic.querySelector(".ia-dash-spark-tip");
        if (tip) tip.hidden = true;
      });
    }
    document.querySelectorAll("[data-dash-close]").forEach((btn) => btn.addEventListener("click", closeDrawer));
    const csv = document.getElementById("iaDashDrawerCsv");
    if (csv) csv.addEventListener("click", downloadCsv);
    document.addEventListener("click", (e) => {
      const btn = e.target && e.target.closest ? e.target.closest("[data-dash-fs]") : null;
      if (!btn) return;
      const card = btn.closest(".ia-dash-card");
      if (card) toggleFs(card);
    });
    document.addEventListener("keydown", (e) => {
      if (e.key !== "Escape") return;
      if (thesaurusOpen()) {
        closeThesaurusPicker();
        return;
      }
      const drawer = document.getElementById("iaDashDrawer");
      if (drawer && !drawer.hidden) {
        closeDrawer();
        return;
      }
      closeFs();
    });
  }

  function boot() {
    if (!root()) return;
    bind();
    mountFsButtons();
    fetchJson("filters").then((f) => {
      fillThesauri(f && f.thesauri);
      if (hasThesaurus()) {
        loadOverview();
        loadQuality();
      }
    }).catch(() => fillThesauri([]));
  }

  window.bootInstanceStats = boot;
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
