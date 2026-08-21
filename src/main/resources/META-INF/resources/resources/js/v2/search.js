/**
 * OpenTheso V2 — recherche, autocomplétion, filtres de statut.
 */
"use strict";

function scoreEl(el, q) {
  if (!q) return { score: -1 };
  const pref = el.getAttribute("data-pref") || "";
  if (pref === q) return { score: 0 };
  if (pref.startsWith(q)) return { score: 1 };
  if (pref.includes(q)) return { score: 2 };
  const alts = (el.getAttribute("data-alts") || "").split("|").filter(Boolean);
  const alt = alts.find(a => a.includes(q));
  if (alt) return { score: 3, via: "syn", viaQ: alt };
  const def = el.getAttribute("data-def") || "";
  if (def.includes(q)) return { score: 5, via: "note" };
  return { score: -1 };
}

function rankHits(query) {
  const q = norm((query || "").trim());
  if (!q) return [];
  return $$("#resultsList .rl-item").map(el => {
    const sc = scoreEl(el, q);
    return sc.score < 0 ? null : {
      el, id: el.getAttribute("data-id"),
      score: sc.score, via: sc.via, viaQ: sc.viaQ,
      pref: el.getAttribute("data-pref-label") || ""
    };
  }).filter(Boolean).sort((a, b) => a.score - b.score || a.pref.localeCompare(b.pref, "fr"));
}

function shownAcRows() {
  return $$("#ac .ac-row.is-shown");
}

function rangeForQuery(textNode, rawQuery) {
  const text = textNode && textNode.nodeValue;
  const nq = norm((rawQuery || "").trim());
  if (!text || !nq) return null;
  let folded = "";
  const origAt = [];
  for (let i = 0; i < text.length; i++) {
    const piece = norm(text[i]);
    for (let k = 0; k < piece.length; k++) origAt.push(i);
    folded += piece;
  }
  const at = folded.indexOf(nq);
  if (at < 0 || origAt[at] == null || origAt[at + nq.length - 1] == null) return null;
  const range = document.createRange();
  range.setStart(textNode, origAt[at]);
  range.setEnd(textNode, origAt[at + nq.length - 1] + 1);
  return range;
}

function unwrapQueryMarks() {
  $$("#ac mark.hl, #resultsList mark.hl").forEach(m => {
    const p = m.parentNode;
    if (!p) return;
    while (m.firstChild) p.insertBefore(m.firstChild, m);
    p.removeChild(m);
    p.normalize();
  });
}

function wrapQueryIn(el, rawQuery) {
  const nodes = [];
  const walk = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null);
  let node;
  while ((node = walk.nextNode())) nodes.push(node);
  nodes.forEach(n => {
    const range = rangeForQuery(n, rawQuery);
    if (!range || range.collapsed) return;
    const mark = document.createElement("mark");
    mark.className = "hl";
    try { range.surroundContents(mark); } catch (_) {}
  });
}

function paintQueryHighlight() {
  unwrapQueryMarks();
  if (!state.highlight) return;
  const live = (($("#searchInput") && $("#searchInput").value) || "").trim();
  if (live && $("#searchBox") && $("#searchBox").classList.contains("is-open")) {
    $$("#ac .ac-row.is-shown .ac-pref, #ac .ac-row.is-shown .ac-via-syn.is-on em").forEach(el => {
      wrapQueryIn(el, live);
    });
  }
  const committed = (state.committed || "").trim();
  if (committed) {
    $$("#resultsList .rl-item.is-shown .rl-pref, #resultsList .rl-item.is-shown .rl-via-syn.is-on em").forEach(el => {
      wrapQueryIn(el, committed);
    });
  }
}

function setAcIdx(i) {
  const rows = shownAcRows();
  state.acIdx = i;
  rows.forEach((r, n) => r.classList.toggle("is-active", n === i));
}

function filterAc(query) {
  const q = norm((query || "").trim());
  const ac = $("#ac");
  const box = $("#searchBox");
  const rows = $$("#ac .ac-row").map(el => {
    el.classList.remove("is-shown", "is-hit", "match-pref", "match-syn", "match-note", "is-active");
    $$(".ac-via-syn", el).forEach(v => v.classList.remove("is-on"));
    const sc = scoreEl(el, q);
    if (sc.score < 0) return null;
    el.classList.add("is-hit");
    if (sc.score <= 2) el.classList.add("match-pref");
    else if (sc.via === "syn") {
      el.classList.add("match-syn");
      const syn = $$(".ac-via-syn", el).find(v => (v.getAttribute("data-q") || "") === sc.viaQ)
        || $$(".ac-via-syn", el).find(v => (v.getAttribute("data-q") || "").includes(q));
      if (syn) syn.classList.add("is-on");
    } else el.classList.add("match-note");
    return { el, score: sc.score, pref: el.getAttribute("data-pref-label") || "" };
  }).filter(Boolean).sort((a, b) => a.score - b.score || a.pref.localeCompare(b.pref, "fr"));

  rows.forEach((h, i) => {
    h.el.style.order = String(i);
    if (i < 7) h.el.classList.add("is-shown");
  });
  if (ac) {
    ac.classList.toggle("is-empty", !!q && rows.length === 0);
    ac.classList.toggle("has-hits", rows.length > 0);
  }
  if (box) box.classList.toggle("is-open", !!q);
  const acQ = $("#acQuery"); if (acQ) acQ.textContent = (query || "").trim();
  const acN = $("#acCount"); if (acN) acN.textContent = String(rows.length);
  state.acIdx = -1;
  paintQueryHighlight();
  return rows.length;
}

function paintCommittedResults() {
  const hits = rankHits(state.committed);
  const q = norm(state.committed.trim());
  $$("#resultsList .rl-item").forEach(el => {
    el.classList.remove("is-hit", "is-shown", "match-syn", "match-note");
    $$(".rl-via", el).forEach(v => v.classList.remove("is-on"));
  });
  hits.forEach((h, i) => {
    h.el.classList.add("is-hit");
    const li = h.el.closest("li");
    if (li) li.style.order = String(i);
    if (i < state.resultLimit) h.el.classList.add("is-shown");
    if (h.via === "syn") {
      h.el.classList.add("match-syn");
      const syn = $$(".rl-via-syn", h.el).find(v => (v.getAttribute("data-q") || "") === h.viaQ);
      if (syn) syn.classList.add("is-on");
    } else if (h.via === "note") {
      h.el.classList.add("match-note");
      const note = $(".rl-via-note", h.el);
      if (note) note.classList.add("is-on");
    }
  });
  const n = hits.length;
  const rl = $("#resultsList");
  if (rl) {
    rl.classList.toggle("is-idle", false);
    rl.classList.toggle("is-empty", n === 0);
    rl.classList.toggle("is-filled", n > 0);
  }
  const rlN = $("#rlN"); if (rlN) rlN.textContent = String(n);
  const rlQ = $("#rlQuery"); if (rlQ) rlQ.textContent = state.committed;
  const more = $("#rlMore");
  if (more) {
    const rest = Math.max(0, n - state.resultLimit);
    more.hidden = rest === 0;
    const mn = $("#rlMoreN");
    if (mn) mn.textContent = "+" + Math.min(rest, PAGE);
  }
  paintBadges();
  paintQueryHighlight();
}

function syncRechercheConcept() {
  const hits = rankHits(state.committed);
  const ids = hits.map(h => h.id);
  if (state.conceptId && ids.includes(state.conceptId)) highlightConcept(state.conceptId);
  else {
    state.conceptId = ids[0] || null;
    highlightConcept(state.conceptId);
  }
}

function runSearch() {
  const q = ($("#searchInput") && $("#searchInput").value) || "";
  if (!IS_CONSULT) {
    go("thesaurus/consultation.xhtml?q=" + encodeURIComponent(q.trim()));
    return;
  }
  state.committed = q.trim();
  state.resultLimit = PAGE;
  state.home = false;
  state.draft = false;
  state.view = "recherche";
  filterAc(q);
  paintCommittedResults();
  syncRechercheConcept();
  closeSearchUi();
  paint();
}

function applyStatusFilter() {
  const set = state.statusSet;
  function candOk(tn) {
    if ((tn.getAttribute("data-status") || "") !== "candidat") return true;
    const by = tn.getAttribute("data-cand-by") || "";
    const on = tn.getAttribute("data-cand-on") || "";
    if (state.candBy && by !== state.candBy) return false;
    if (state.candFrom && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on < state.candFrom)) return false;
    if (state.candTo && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on > state.candTo)) return false;
    return true;
  }
  const nodes = treeNodes();
  const ownOk = nodes.map(tn => {
    const st = tn.getAttribute("data-status") || "valide";
    return set.has(st) && candOk(tn);
  });
  const vis = ownOk.slice();
  for (let i = 0; i < nodes.length; i++) {
    if (!ownOk[i]) continue;
    let depth = treeDepth(nodes[i]);
    for (let j = i - 1; j >= 0 && depth > 0; j--) {
      const parentDepth = treeDepth(nodes[j]);
      if (parentDepth < depth) {
        vis[j] = true;
        depth = parentDepth;
      }
    }
  }
  nodes.forEach((tn, i) => tn.classList.toggle("is-status-off", !vis[i]));
  if (tableRowsCache.length) {
    state.tblPage = 1;
    renderTablePage();
    return;
  }
  $$("#panelTable tr[data-status]").forEach(tr => {
    const st = tr.getAttribute("data-status") || "valide";
    let on = set.has(st);
    if (on && st === "candidat") {
      const by = tr.getAttribute("data-cand-by") || "";
      const day = tr.getAttribute("data-cand-on") || "";
      if (state.candBy && by !== state.candBy) on = false;
      if (state.candFrom && (!/^\d{4}-\d{2}-\d{2}$/.test(day) || day < state.candFrom)) on = false;
      if (state.candTo && (!/^\d{4}-\d{2}-\d{2}$/.test(day) || day > state.candTo)) on = false;
    }
    tr.classList.toggle("is-status-off", !on);
  });
  updateTableCount();
}
