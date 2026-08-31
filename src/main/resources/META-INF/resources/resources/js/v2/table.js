/**
 * OpenTheso V2 — tableau paginé.
 */
"use strict";

function applyTableSort() {
  paintTableSortHeaders();
  if (tableRowsCache.length) {
    state.tblPage = 1;
    renderTablePage();
    return;
  }
  const col = state.tblSort;
  const dir = state.tblDir;
  const body = $("#panelTable tbody");
  if (!body) return;
  const rows = $$("#panelTable tbody tr[data-id]");
  rows.sort((a, b) => {
    const va = a.getAttribute("data-s-" + col) || "";
    const vb = b.getAttribute("data-s-" + col) || "";
    return va.localeCompare(vb, "fr", { numeric: true }) * dir;
  });
  rows.forEach(r => body.appendChild(r));
}

function paintTableSortHeaders() {
  const col = state.tblSort;
  const dir = state.tblDir;
  $$("#panelTable .flat-th").forEach(th => {
    const on = th.getAttribute("data-col") === col;
    th.classList.toggle("is-sorted", on);
    const s = th.querySelector(".flat-th-s");
    if (s) s.textContent = on ? (dir > 0 ? "▲" : "▼") : "↕";
  });
}

function applyTableCols() {
  TABLE_COL_ALL.forEach(col => {
    const on = state.tblCols.has(col);
    $$(".col-" + col).forEach(el => el.classList.toggle("is-col-off", !on));
    $$(`.tbl-colrow[data-col="${col}"]`).forEach((lab) => {
      lab.classList.toggle("on", on);
      lab.setAttribute("aria-pressed", on ? "true" : "false");
      const box = lab.querySelector(".tbl-colbox");
      if (box) box.textContent = on ? "✓" : "";
    });
  });
}

const TABLE_STATUS = { valide: 1, candidat: 1, insere: 1, rejete: 1, deprecie: 1 };
const TABLE_PAGE_SIZES = [25, 50, 100, 200];
const TABLE_SORT_KEY = { concept: "label", status: "status", type: "type", notation: "notation", path: "path" };
let tableRowsState = { key: "", loading: false };
var tableRowsCache = [];
let tableTruncated = false;

function tablePageSize() {
  const n = Number(state.tblPageSize);
  return TABLE_PAGE_SIZES.includes(n) ? n : 50;
}

function setTablePageSize(size) {
  const n = Number(size);
  const next = TABLE_PAGE_SIZES.includes(n) ? n : 50;
  const old = tablePageSize();
  if (next === old) return;
  const first = Math.max(0, (state.tblPage - 1) * old);
  state.tblPageSize = next;
  state.tblPage = Math.floor(first / next) + 1;
  try { localStorage.setItem("ot-table-page-size", String(next)); } catch (ex) {}
  const sel = $("#panelTablePageSize");
  if (sel && sel.value !== String(next)) sel.value = String(next);
  if (tableRowsCache.length) renderTablePage();
}


function tableCacheKey() {
  return thesaurusId() + "|" + thesaurusLang();
}

function tableRowMatchesFilter(row) {
  const st = (row && TABLE_STATUS[row.status]) ? row.status : "valide";
  if (!state.statusSet.has(st)) return false;
  if (st !== "candidat") return true;
  const by = row.candidateBy || "";
  const on = row.candidateOn || "";
  if (state.candBy && by !== state.candBy) return false;
  if (state.candFrom && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on < state.candFrom)) return false;
  if (state.candTo && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on > state.candTo)) return false;
  return true;
}

function filteredSortedTableRows() {
  const key = TABLE_SORT_KEY[state.tblSort] || "label";
  const dir = state.tblDir;
  return tableRowsCache.filter(tableRowMatchesFilter).sort((a, b) => {
    const va = a && a[key] != null ? String(a[key]) : "";
    const vb = b && b[key] != null ? String(b[key]) : "";
    return va.localeCompare(vb, "fr", { numeric: true }) * dir;
  });
}

function tablePagerTokens(page, pages) {
  if (pages <= 1) return [];
  if (pages <= 7) {
    const all = [];
    for (let i = 1; i <= pages; i++) all.push(i);
    return all;
  }
  const tokens = [1];
  const from = Math.max(2, page - 1);
  const to = Math.min(pages - 1, page + 1);
  if (from > 2) tokens.push("…");
  for (let n = from; n <= to; n++) tokens.push(n);
  if (to < pages - 1) tokens.push("…");
  tokens.push(pages);
  return tokens;
}

function hideTablePager() {
  const pager = $("#panelTablePager");
  if (pager) {
    pager.hidden = true;
    pager.innerHTML = "";
  }
}

function paintTablePager(filteredTotal) {
  const pager = $("#panelTablePager");
  if (!pager) return;
  const pages = Math.max(1, Math.ceil(filteredTotal / tablePageSize()));
  if (!filteredTotal || pages <= 1) {
    hideTablePager();
    return;
  }
  const page = state.tblPage;
  let html = '<button type="button" class="st-pager-btn' + (page <= 1 ? " is-off" : "")
    + '" data-act="tbl-page-prev" title="Page précédente">‹</button>';
  tablePagerTokens(page, pages).forEach(tok => {
    if (tok === "…") {
      html += '<span class="st-pager-gap">…</span>';
      return;
    }
    html += '<button type="button" class="st-pager-n' + (tok === page ? " is-on" : "")
      + '" data-act="tbl-page" data-page="' + tok + '">' + tok + "</button>";
  });
  html += '<button type="button" class="st-pager-btn' + (page >= pages ? " is-off" : "")
    + '" data-act="tbl-page-next" title="Page suivante">›</button>';
  pager.innerHTML = html;
  pager.hidden = false;
}

function updateTableCount(filteredTotal, allTotal) {
  const el = $("#panelTable .flat-count");
  if (!el) return;
  if (filteredTotal == null) {
    filteredTotal = $$("#panelTable tbody tr[data-id]").length;
    allTotal = filteredTotal;
  }
  if (!allTotal) {
    el.textContent = "";
    return;
  }
  const size = tablePageSize();
  const start = filteredTotal ? (state.tblPage - 1) * size + 1 : 0;
  const end = Math.min(state.tblPage * size, filteredTotal);
  let text = filteredTotal
    ? start.toLocaleString("fr-FR") + "–" + end.toLocaleString("fr-FR") + " sur " + filteredTotal.toLocaleString("fr-FR")
    : "0 ligne";
  if (filteredTotal !== allTotal) {
    text += " / " + allTotal.toLocaleString("fr-FR");
  }
  if (tableTruncated) text += " (aperçu)";
  el.textContent = text;
}

function tableRowHtml(row) {
  const rawId = row && row.id ? String(row.id) : "";
  const id = escapeHtml(rawId);
  const label = escapeHtml(row.label || rawId);
  const status = TABLE_STATUS[row.status] ? row.status : "valide";
  const statusLabel = escapeHtml(row.statusLabel || "Normal");
  const type = escapeHtml(row.type || "Concept");
  const notation = row.notation ? String(row.notation) : "";
  const path = escapeHtml(row.path || "");
  const candBy = escapeHtml(row.candidateBy || "");
  const candOn = escapeHtml(row.candidateOn || "");
  const cls = (status === "candidat" ? "is-candidate" : "")
    + (state.conceptId && state.conceptId === rawId ? " is-active" : "");
  return '<tr data-act="open" data-id="' + id + '" data-status="' + status
    + '" data-cand-by="' + candBy + '" data-cand-on="' + candOn
    + '" data-type="concept" data-s-concept="' + label + '" data-s-status="' + status
    + '" data-s-type="' + type + '" data-s-notation="' + escapeHtml(notation)
    + '" data-s-path="' + path + '" class="' + cls.trim() + '">'
    + '<td class="flat-check-c" data-act="sel-id" data-id="' + id + '"><span class="tn-check"></span></td>'
    + '<td class="flat-name"><span class="flat-text">' + label + "</span></td>"
    + '<td class="col-status"><span class="st-pill st-' + status + '"><span class="st-dot st-'
    + status + '"></span>' + statusLabel + "</span></td>"
    + '<td class="col-type">' + type + "</td>"
    + '<td class="flat-mono col-notation">' + escapeHtml(notation || "—") + "</td>"
    + '<td class="col-path">' + path + "</td>"
    + "</tr>";
}

function renderTablePage() {
  const body = $("#panelTable tbody");
  if (!body || !tableRowsCache.length) return;
  const filtered = filteredSortedTableRows();
  const size = tablePageSize();
  const pages = Math.max(1, Math.ceil(filtered.length / size));
  if (state.tblPage > pages) state.tblPage = pages;
  if (state.tblPage < 1) state.tblPage = 1;
  const start = (state.tblPage - 1) * size;
  const pageRows = filtered.slice(start, start + size);
  if (!filtered.length) {
    body.innerHTML = '<tr class="flat-empty-row" data-empty="1"><td colspan="6">'
      + "Aucun concept ne correspond aux filtres.</td></tr>";
  } else {
    body.innerHTML = pageRows.map(tableRowHtml).join("");
    state.selected.forEach(selId => paintSelectedId(selId, true));
  }
  paintTableSortHeaders();
  applyTableCols();
  updateTableCount(filtered.length, tableRowsCache.length);
  paintTablePager(filtered.length);
}

function goToTablePage(page) {
  const filtered = tableRowsCache.length ? filteredSortedTableRows() : [];
  const pages = Math.max(1, Math.ceil(filtered.length / tablePageSize()));
  const next = Math.min(pages, Math.max(1, Number(page) || 1));
  if (next === state.tblPage && $$("#panelTable tbody tr[data-id]").length) {
    paintTablePager(filtered.length);
    return;
  }
  state.tblPage = next;
  renderTablePage();
  const wrap = $("#panelTable .flat-wrap");
  if (wrap) wrap.scrollTop = 0;
}

function revealTableConcept(id) {
  if (!id || !tableRowsCache.length) return;
  const rows = filteredSortedTableRows();
  const idx = rows.findIndex(row => row.id === id);
  if (idx < 0) return;
  const page = Math.floor(idx / tablePageSize()) + 1;
  if (page !== state.tblPage) {
    state.tblPage = page;
    renderTablePage();
  }
}

function setTableLoading(on) {
  const panel = $("#panelTable");
  const load = $("#panelTableLoading");
  if (panel) {
    panel.classList.toggle("is-loading", !!on);
    if (on) panel.setAttribute("aria-busy", "true");
    else panel.removeAttribute("aria-busy");
  }
  if (load) load.hidden = !on;
}

function setTableMessage(msg) {
  setTableLoading(false);
  tableRowsCache = [];
  hideTablePager();
  const body = $("#panelTable tbody");
  if (!body) return;
  body.innerHTML = '<tr class="flat-empty-row" data-empty="1"><td colspan="6">'
    + escapeHtml(msg) + "</td></tr>";
  updateTableCount(0, 0);
}

function renderTableRows(rows) {
  setTableLoading(false);
  tableRowsCache = Array.isArray(rows) ? rows.slice() : [];
  state.tblPage = 1;
  if (!tableRowsCache.length) {
    setTableMessage("Aucun concept à afficher dans cette langue.");
    return;
  }
  renderTablePage();
  if (state.conceptId) highlightConcept(state.conceptId);
}

function invalidateTableRows() {
  tableRowsState = { key: "", loading: false };
  tableRowsCache = [];
  tableTruncated = false;
  state.tblPage = 1;
}

function tableCacheMatches() {
  return tableRowsState.key === tableCacheKey();
}
function ensureTableRows() {
  if (!$("#panelTable tbody")) return;
  const key = tableCacheKey();
  if (tableRowsState.key === key) return;
  loadTableRows();
}

function loadTableRows() {
  const theso = thesaurusId();
  const lang = thesaurusLang();
  const key = theso + "|" + lang;
  if (!theso) {
    tableRowsState = { key: key, loading: false };
    setTableMessage("Aucun thésaurus sélectionné.");
    return;
  }
  tableRowsState = { key: key, loading: true };
  setTableLoading(true);
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({ thesaurusId: theso, lang: lang });
  fetch(ctx + "/v2/api/table-rows?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then(res => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then(data => {
    if (tableCacheKey() !== key) return;
    tableRowsState.loading = false;
    tableRowsState.key = key;
    tableTruncated = !!(data && data.truncated);
    const rows = data && Array.isArray(data.rows) ? data.rows : [];
    renderTableRows(rows);
  }).catch(() => {
    if (tableCacheKey() !== key) return;
    tableRowsState = { key: "", loading: false };
    tableTruncated = false;
    setTableMessage("Impossible de charger le tableau.");
  });
}
