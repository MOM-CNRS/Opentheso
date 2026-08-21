/**
 * OpenTheso V2 — tableau de bord et rail de défilement.
 */
"use strict";

function bindViewRail() {
  const view = $("#previewView") || document.querySelector("main.content .view");
  const home = $("#viewHome");
  const settings = $("#viewSettings");
  const rail = $("#previewRail");
  const thumb = $("#previewRailThumb");
  const goTop = $("#previewGoTop");
  const shell = view && view.closest(".content");
  if (!view) return;
  const scrollers = [view, home, settings].filter((el, i, arr) => el && arr.indexOf(el) === i);
  let idle;
  const mark = () => {
    if (shell) shell.classList.add("is-scrolling");
    clearTimeout(idle);
    idle = setTimeout(() => { if (shell) shell.classList.remove("is-scrolling"); }, 900);
  };
  const currentScroll = () => scrollers.reduce((max, el) => Math.max(max, el.scrollTop || 0), 0);
  const sync = () => {
    const st = currentScroll();
    if (goTop) {
      const off = st < 72;
      goTop.classList.toggle("is-off", off);
      goTop.setAttribute("aria-hidden", String(off));
      if (off) goTop.setAttribute("tabindex", "-1");
      else goTop.removeAttribute("tabindex");
    }
    if (!rail || !thumb) return;
    const ch = view.clientHeight;
    const sh = view.scrollHeight;
    if (sh <= ch + 4) {
      rail.classList.add("is-off");
      return;
    }
    rail.classList.remove("is-off");
    const track = rail.clientHeight;
    const h = Math.max(40, Math.round((ch / sh) * track));
    const max = Math.max(0, track - h);
    const y = max === 0 || sh === ch ? 0 : (st / (sh - ch)) * max;
    thumb.style.height = h + "px";
    thumb.style.transform = "translate3d(0," + y + "px,0)";
  };
  const scrollFromY = (clientY) => {
    if (!rail || !thumb) return;
    const rect = rail.getBoundingClientRect();
    const track = rail.clientHeight;
    const h = thumb.offsetHeight;
    const y = Math.min(Math.max(0, clientY - rect.top - h / 2), Math.max(0, track - h));
    const max = track - h;
    const span = view.scrollHeight - view.clientHeight;
    view.scrollTop = max <= 0 ? 0 : (y / max) * span;
  };
  scrollers.forEach((el) => el.addEventListener("scroll", () => { mark(); sync(); }, { passive: true }));
  window.addEventListener("resize", sync);
  if (rail && thumb) {
    rail.addEventListener("pointerdown", (e) => {
      e.preventDefault();
      rail.classList.add("is-drag");
      if (shell) shell.classList.add("is-scrolling");
      scrollFromY(e.clientY);
      rail.setPointerCapture(e.pointerId);
    });
    rail.addEventListener("pointermove", (e) => {
      if (!rail.classList.contains("is-drag")) return;
      scrollFromY(e.clientY);
    });
    rail.addEventListener("pointerup", () => rail.classList.remove("is-drag"));
    rail.addEventListener("pointercancel", () => rail.classList.remove("is-drag"));
  }
  window.syncViewRail = sync;
  requestAnimationFrame(sync);
}
function fetchStat(path) {
  const ctx = document.body.getAttribute("data-ctx") || "";
  return fetch(ctx + "/v2/api/stats/" + path, {
    headers: { Accept: "application/json" }
  }).then((res) => (res.ok ? res.json() : Promise.reject()));
}

function formatInt(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toLocaleString("fr-FR") : "—";
}

function capitalizeLabel(value) {
  const text = String(value == null ? "" : value);
  if (!text) return text;
  return text.charAt(0).toUpperCase() + text.slice(1);
}

function monthLabel(key) {
  const parts = String(key || "").split("-");
  if (parts.length < 2) return "—";
  const date = new Date(Number(parts[0]), Number(parts[1]) - 1, 1);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("fr-FR", { month: "short", year: "2-digit" }).format(date)
    .replace(/\./g, "")
    .replace(/\u00a0/g, " ")
    .replace(/\u202f/g, " ")
    .trim();
}

function fillKpi(el, value, formatted) {
  if (!el) return 0;
  return fillStatNode(el, {
    value: value,
    formatted: formatted != null ? formatted : formatInt(value)
  });
}

function fillOverviewKpis(kpis) {
  const map = kpis || {};
  $$("[data-stat]").forEach((el) => {
    const key = el.getAttribute("data-stat");
    if (!key || key === "max-depth" || key === "without-definition") return;
    if (Object.prototype.hasOwnProperty.call(map, key)) fillKpi(el, map[key]);
  });
}

function dashStatNodes(nodes) {
  nodes.forEach((el) => {
    el.textContent = "—";
    el.removeAttribute("aria-busy");
  });
}

function fillStatNode(el, data) {
  const formatted = data && data.formatted;
  const value = Number(data && data.value);
  el.textContent = formatted
    || (Number.isFinite(value) ? value.toLocaleString("fr-FR") : "—");
  el.removeAttribute("aria-busy");
  el.classList.add("is-ready");
  return Number.isFinite(value) ? value : 0;
}


function coveragePercent(translatedCount, total) {
  if (!total) return 0;
  return Math.max(0, Math.min(100, Math.round((Number(translatedCount) || 0) * 100 / total)));
}

function animateBarFills(container) {
  const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const fills = $$(".mg-bar-fill", container);
  const paint = () => {
    fills.forEach((fill, i) => {
      const pct = fill.getAttribute("data-width") || "0";
      const display = fill.getAttribute("data-display") || pct + "%";
      const labelN = fill.closest(".mg-bar-row") && fill.closest(".mg-bar-row").querySelector(".mg-bar-n");
      if (reduceMotion) {
        fill.style.width = pct + "%";
        if (labelN) labelN.textContent = display;
        return;
      }
      fill.style.transitionDelay = (i * 70) + "ms";
      fill.style.width = pct + "%";
      if (labelN) {
        window.setTimeout(() => { labelN.textContent = display; }, 70 * i + 180);
      }
    });
  };
  requestAnimationFrame(() => requestAnimationFrame(paint));
}

function renderStatBars(container, items) {
  if (!items.length) {
    container.innerHTML = '<div class="mg-bar-row"><span class="mg-bar-l">—</span></div>';
    container.removeAttribute("aria-busy");
    if (window.syncViewRail) window.syncViewRail();
    return;
  }
  container.innerHTML = items.map((item) => {
    const label = escapeHtml(item.label || "—");
    const start = escapeHtml(item.startDisplay || "0");
    return '<div class="mg-bar-row">'
      + '<span class="mg-bar-l">' + label + "</span>"
      + '<span class="mg-bar-track"><span class="mg-bar-fill" data-width="' + item.widthPct
      + '" data-display="' + escapeHtml(item.display) + '"></span></span>'
      + '<span class="mg-bar-n">' + start + "</span>"
      + "</div>";
  }).join("");
  container.removeAttribute("aria-busy");
  if (window.syncViewRail) window.syncViewRail();
  animateBarFills(container);
}

function renderLanguageCoverage(container, total, languages) {
  renderStatBars(container, (languages || []).map((item) => {
    const pct = coveragePercent(item.translatedCount, total);
    return {
      label: capitalizeLabel(item.label || item.code || "—"),
      widthPct: pct,
      display: pct + "%",
      startDisplay: "0%"
    };
  }));
}

function renderCollectionCoverage(container, collections) {
  const rows = collections || [];
  const max = rows.reduce((acc, row) => Math.max(acc, Number(row.memberCount) || 0), 0);
  renderStatBars(container, rows.map((item) => {
    const count = Number(item.memberCount) || 0;
    return {
      label: item.label || item.id || "—",
      widthPct: max ? Math.round(count * 100 / max) : 0,
      display: count.toLocaleString("fr-FR"),
      startDisplay: "0"
    };
  }));
}

function metricValue(body, key) {
  if (body && body[key] && typeof body[key] === "object" && body[key].value != null) {
    return Number(body[key].value) || 0;
  }
  return Number(body && body[key]) || 0;
}

function renderCandidateOutcome(container, body) {
  const accepted = metricValue(body, "accepted");
  const pendingCount = metricValue(body, "pending");
  const rejected = metricValue(body, "rejected");
  const total = accepted + pendingCount + rejected;
  const cap = $("[data-stat-outcome-cap]");
  if (cap) {
    cap.textContent = total === 1
      ? "1 terme proposé"
      : total.toLocaleString("fr-FR") + " termes proposés";
  }
  if (!total) {
    container.innerHTML = '<p class="cs-foot">Aucune proposition pour le moment.</p>';
    container.removeAttribute("aria-busy");
    if (window.syncViewRail) window.syncViewRail();
    return;
  }
  const pct = (n) => Math.round(n * 1000 / total) / 10;
  const segs = [
    { cls: "t-ins", count: accepted, label: "Insérés" },
    { cls: "t-cand", count: pendingCount, label: "En attente" },
    { cls: "t-rej", count: rejected, label: "Rejetés" }
  ];
  const pipe = segs.filter((s) => s.count > 0).map((s) => (
    '<span class="cs-seg ' + s.cls + '" data-width="' + pct(s.count) + '"><b>'
    + s.count.toLocaleString("fr-FR") + "</b></span>"
  )).join("");
  const legend = segs.map((s) => (
    '<span class="cs-lg"><i class="cs-dot ' + s.cls + '"></i>'
    + s.label + " — " + s.count.toLocaleString("fr-FR") + "</span>"
  )).join("");
  const decided = accepted + rejected;
  const decidedRate = decided ? Math.round(accepted * 100 / decided) : 0;
  const medianRaw = body && body.medianDecisionDays;
  const median = medianRaw && typeof medianRaw === "object"
    ? (medianRaw.formatted !== "—" ? Number(medianRaw.value) : null)
    : (medianRaw == null ? null : Number(medianRaw));
  let foot;
  if (!decided) {
    foot = "Aucune proposition n'a encore été tranchée.";
  } else {
    foot = "Sur <b>" + decided.toLocaleString("fr-FR") + "</b> proposition"
      + (decided > 1 ? "s" : "") + " déjà tranchée" + (decided > 1 ? "s" : "")
      + ", <b>" + decidedRate + "\u202f%</b> ont été acceptées.";
    if (median != null && Number.isFinite(median)) {
      foot += " Délai médian entre proposition et décision : <b>"
        + median.toLocaleString("fr-FR") + "\u202fjour" + (median > 1 ? "s" : "")
        + "</b>.";
    }
  }
  container.innerHTML = '<div class="cs-pipe">' + pipe + "</div>"
    + '<div class="cs-legend">' + legend + "</div>"
    + '<p class="cs-foot">' + foot + "</p>";
  container.removeAttribute("aria-busy");
  const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const paint = () => {
    $$(".cs-seg", container).forEach((seg, i) => {
      if (!reduceMotion) seg.style.transitionDelay = (i * 70) + "ms";
      seg.style.width = (seg.getAttribute("data-width") || "0") + "%";
    });
  };
  requestAnimationFrame(() => requestAnimationFrame(paint));
  if (window.syncViewRail) window.syncViewRail();
}

function monthCount(row, key) {
  return Math.max(0, Number(row && row[key]) || 0);
}

function monthTooltip(row) {
  const total = monthCount(row, "total");
  const parts = [];
  const accepted = monthCount(row, "accepted");
  const pendingCount = monthCount(row, "pending");
  const rejected = monthCount(row, "rejected");
  if (accepted) parts.push(accepted.toLocaleString("fr-FR") + " inséré" + (accepted > 1 ? "s" : ""));
  if (pendingCount) parts.push(pendingCount.toLocaleString("fr-FR") + " en attente");
  if (rejected) parts.push(rejected.toLocaleString("fr-FR") + " rejeté" + (rejected > 1 ? "s" : ""));
  const head = total === 1 ? "1 proposé" : total.toLocaleString("fr-FR") + " proposés";
  return parts.length ? head + " · " + parts.join(", ") : head;
}

function renderCandidateMonths(container, months) {
  const rows = months || [];
  const max = rows.reduce((acc, row) => Math.max(acc, monthCount(row, "total")), 0);
  const cols = rows.map((row) => {
    const total = monthCount(row, "total");
    const height = max ? Math.max(8, Math.round(total * 100 / max)) : 0;
    const segs = [
      { cls: "t-ins", count: monthCount(row, "accepted") },
      { cls: "t-cand", count: monthCount(row, "pending") },
      { cls: "t-rej", count: monthCount(row, "rejected") }
    ].filter((s) => s.count > 0).map((s) => (
      '<span class="cs-col-seg ' + s.cls + '" style="flex:' + s.count + '"></span>'
    )).join("");
    return '<div class="cs-col">'
      + '<div class="cs-col-plot"><div class="cs-col-stack'
      + (total ? "" : " is-empty")
      + '" data-height="' + (total ? height : 0) + '" title="' + escapeHtml(monthTooltip(row)) + '">'
      + segs + "</div></div>"
      + '<span class="cs-col-n" data-count="' + total + '">0</span>'
      + '<span class="cs-col-lbl">' + escapeHtml(row.label || monthLabel(row.key) || "—") + "</span>"
      + "</div>";
  }).join("");
  const legend = '<div class="cs-legend">'
    + '<span class="cs-lg"><i class="cs-dot t-ins"></i>Insérés</span>'
    + '<span class="cs-lg"><i class="cs-dot t-cand"></i>En attente</span>'
    + '<span class="cs-lg"><i class="cs-dot t-rej"></i>Rejetés</span>'
    + "</div>";
  container.innerHTML = '<div class="cs-months">' + (cols || "") + "</div>" + legend;
  container.removeAttribute("aria-busy");
  const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const paint = () => {
    $$(".cs-col-stack", container).forEach((stack, i) => {
      if (stack.classList.contains("is-empty")) return;
      const height = stack.getAttribute("data-height") || "0";
      if (!reduceMotion) stack.style.transitionDelay = (i * 70) + "ms";
      stack.style.height = height + "%";
    });
    $$(".cs-col-n", container).forEach((label, i) => {
      const count = Number(label.getAttribute("data-count")) || 0;
      const text = count.toLocaleString("fr-FR");
      if (reduceMotion) {
        label.textContent = text;
        return;
      }
      window.setTimeout(() => { label.textContent = text; }, 70 * i + 180);
    });
  };
  requestAnimationFrame(() => requestAnimationFrame(paint));
  if (window.syncViewRail) window.syncViewRail();
}

function fillCandidateLife(body) {
  const life = body || {};
  const rate = Number(life.acceptanceRatePercent);
  const median = life.medianDecisionDays;
  $$("[data-stat-life]").forEach((el) => {
    const key = el.getAttribute("data-stat-life");
    if (key === "pending") fillKpi(el, life.pending);
    else if (key === "accepted12m") fillKpi(el, life.acceptedLast12Months);
    else if (key === "rejected12m") fillKpi(el, life.rejectedLast12Months);
    else if (key === "acceptanceRate") {
      fillKpi(el, rate, Number.isFinite(rate) ? formatInt(rate) + "\u202f%" : "—");
    } else if (key === "medianDecisionDays") {
      fillKpi(
        el,
        median,
        median == null ? "—" : formatInt(median) + "\u202fj"
      );
    } else if (key === "activeContributors") fillKpi(el, life.activeContributors);
  });
}

function applyOverview(body) {
  const overview = body || {};
  fillOverviewKpis(overview.kpis);
  const languageCoverage = $("[data-stat-coverage='language']");
  if (languageCoverage) {
    renderLanguageCoverage(
      languageCoverage,
      Number(overview.kpis && overview.kpis.concepts) || 0,
      overview.languages
    );
  }
  const collectionCoverage = $("[data-stat-coverage='collections']");
  if (collectionCoverage) {
    renderCollectionCoverage(collectionCoverage, overview.collections);
  }
  const cap = $("[data-stat-collections-cap]");
  if (cap) {
    cap.textContent = overview.collectionsTruncated
      ? "12 micro-thésaurus les plus peuplés"
      : "";
  }
  fillCandidateLife(overview.candidates);
  const outcome = $("[data-stat-outcome]");
  if (outcome) renderCandidateOutcome(outcome, overview.candidates || {});
  const months = $("[data-stat-months]");
  if (months) renderCandidateMonths(months, overview.months);
}

function applyCompleteness(body) {
  fillKpi($("[data-stat='max-depth']"), body && body.maxDepth);
  fillKpi($("[data-stat='without-definition']"), body && body.withoutDefinition);
}

function loadStatKpis() {
  const kpiNodes = $$("[data-stat]");
  const dashboard = $("[data-stat-life]")
    || $("[data-stat-coverage='language']")
    || $("[data-stat-coverage='collections']")
    || $("[data-stat-outcome]")
    || $("[data-stat-months]");
  if (!kpiNodes.length && !dashboard) return;

  if (dashboard) {
    fetchStat("overview")
      .then(applyOverview)
      .catch(() => {
        dashStatNodes(kpiNodes.filter((el) => {
          const key = el.getAttribute("data-stat");
          return key !== "max-depth" && key !== "without-definition";
        }));
        dashStatNodes($$("[data-stat-life]"));
        ["language", "collections"].forEach((kind) => {
          const el = $("[data-stat-coverage='" + kind + "']");
          if (el) {
            el.innerHTML = '<div class="mg-bar-row"><span class="mg-bar-l">—</span></div>';
            el.removeAttribute("aria-busy");
          }
        });
        const outcome = $("[data-stat-outcome]");
        if (outcome) {
          outcome.innerHTML = '<p class="cs-foot">—</p>';
          outcome.removeAttribute("aria-busy");
        }
        const months = $("[data-stat-months]");
        if (months) {
          months.innerHTML = '<p class="cs-foot">—</p>';
          months.removeAttribute("aria-busy");
        }
      });
    fetchStat("completeness")
      .then(applyCompleteness)
      .catch(() => {
        dashStatNodes($$("[data-stat='max-depth'], [data-stat='without-definition']"));
      });
    return;
  }

  fetchStat("kpis")
    .then(fillOverviewKpis)
    .catch(() => dashStatNodes(kpiNodes));
}
