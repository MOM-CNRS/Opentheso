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

var STATUS_FOREST = { insere: 1, candidat: 1, rejete: 1, deprecie: 1 };

function candFilterOk(status, by, on) {
  if (status !== "candidat") return true;
  if (state.candBy && by !== state.candBy) return false;
  if (state.candFrom && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on < state.candFrom)) return false;
  if (state.candTo && (!/^\d{4}-\d{2}-\d{2}$/.test(on) || on > state.candTo)) return false;
  return true;
}

function wantsStatusForest() {
  const set = state.statusSet;
  return !set.has("valide") && Object.keys(STATUS_FOREST).some((s) => set.has(s));
}

function statusForestHost() {
  const tree = $("#previewTree") || treePanel();
  if (!tree) return null;
  let box = $("#statusForest");
  if (!box) {
    box = document.createElement("div");
    box.id = "statusForest";
    box.setAttribute("data-status-forest", "1");
    tree.appendChild(box);
  }
  return box;
}

function hideStatusForest() {
  const box = $("#statusForest");
  if (box) {
    box.hidden = true;
    box.innerHTML = "";
  }
}

function statusForestNodeHtml(node) {
  const status = node.status || "valide";
  const type = node.nodeType || "concept";
  const depth = Number(node.depth || 0);
  const inactive = !!node.inactive;
  const open = !!node.hasChildren;
  const pad = 6 + depth * 18;
  const rowCls = "tn-row"
    + (status === "candidat" ? " is-candidate" : "")
    + (status === "rejete" ? " is-rejected" : "")
    + (status === "deprecie" ? " is-deprecated" : "")
    + (inactive ? " is-status-inactive" : "");
  const tags = (status === "candidat" || status === "rejete"
    ? '<span class="tn-dot" title="candidat"></span>' : "")
    + (status === "rejete" ? '<span class="tn-tag">rejeté</span>' : "");
  return '<div class="tn' + (open ? " is-open" : "") + (inactive ? " is-status-inactive" : "") + '"'
    + ' data-id="' + escapeHtml(node.id) + '"'
    + ' data-type="' + escapeHtml(type) + '"'
    + ' data-status="' + escapeHtml(status) + '"'
    + ' data-depth="' + depth + '"'
    + ' data-has-children="' + (node.hasChildren ? "true" : "false") + '"'
    + ' data-nota="' + escapeHtml(node.notation || "") + '"'
    + ' data-cand-by="' + escapeHtml(node.candidateBy || "") + '"'
    + ' data-cand-on="' + escapeHtml(node.candidateOn || "") + '"'
    + ' data-key="' + escapeHtml(node.label || node.id) + '">'
    + '<div class="' + rowCls + '">'
    + (inactive ? '<span class="tn-check is-off"></span>'
      : '<span class="tn-check" data-act="sel-node" data-id="' + escapeHtml(node.id) + '"></span>')
    + '<div class="tn-rowmain" style="padding-left:' + pad + 'px">'
    + '<button type="button" class="tn-caret' + (node.hasChildren ? "" : " is-empty") + '"'
    + ' data-act="sf-toggle" aria-label="Déplier">'
    + '<span class="caret-open">▾</span><span class="caret-shut">▸</span></button>'
    + '<button type="button" class="tn-label" data-act="open" data-id="' + escapeHtml(node.id)
    + '" data-type="' + escapeHtml(type) + '">'
    + '<span class="tn-textwrap"><span class="tn-text">' + escapeHtml(node.label || node.id) + "</span></span>"
    + tags
    + "</button></div></div></div>";
}

function renderStatusForest(nodes) {
  const box = statusForestHost();
  if (!box) return;
  const set = state.statusSet;
  const keep = new Set();
  (nodes || []).forEach((node, i) => {
    if (node.inactive) return;
    if (!set.has(node.status || "valide")) return;
    if (!candFilterOk(node.status, node.candidateBy || "", node.candidateOn || "")) return;
    keep.add(i);
    let depth = Number(node.depth || 0);
    for (let j = i - 1; j >= 0 && depth > 0; j--) {
      const parentDepth = Number(nodes[j].depth || 0);
      if (parentDepth < depth) {
        keep.add(j);
        depth = parentDepth;
      }
    }
  });
  const filtered = (nodes || []).filter((_, i) => keep.has(i));
  if (!filtered.length) {
    box.innerHTML = '<div class="tree-empty">Aucun concept pour les statuts sélectionnés.</div>';
    box.hidden = false;
    return;
  }
  box.innerHTML = filtered.map(statusForestNodeHtml).join("");
  box.hidden = false;
}

function loadStatusForest() {
  const box = statusForestHost();
  if (!box) return;
  const statuses = Object.keys(STATUS_FOREST).filter((s) => state.statusSet.has(s)).join(",");
  const key = thesaurusId() + "|" + thesaurusLang() + "|" + statuses
    + "|" + (state.candBy || "") + "|" + (state.candFrom || "") + "|" + (state.candTo || "");
  if (loadStatusForest._key === key && box.childElementCount && !box.hidden) return;
  box.hidden = false;
  box.innerHTML = '<div class="tree-empty">Chargement…</div>';
  const ctx = document.body.getAttribute("data-ctx") || "";
  const url = ctx + "/v2/api/tree-status-forest?thesaurusId=" + encodeURIComponent(thesaurusId())
    + "&lang=" + encodeURIComponent(thesaurusLang() || "fr")
    + "&statuses=" + encodeURIComponent(statuses);
  fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((r) => (r.ok ? r.json() : []))
    .then((nodes) => {
      loadStatusForest._key = key;
      renderStatusForest(Array.isArray(nodes) ? nodes : []);
    })
    .catch(() => {
      box.innerHTML = '<div class="tree-empty">Impossible de charger l’arbre filtré.</div>';
    });
}

function applyStatusFilter() {
  const set = state.statusSet;
  function candOk(tn) {
    return candFilterOk(
      tn.getAttribute("data-status") || "",
      tn.getAttribute("data-cand-by") || "",
      tn.getAttribute("data-cand-on") || ""
    );
  }
  const forestOn = wantsStatusForest();
  const nodes = treeNodes().filter((tn) => !tn.closest("[data-status-forest]"));
  if (forestOn) {
    nodes.forEach((tn) => tn.classList.add("is-status-off"));
    const empty = ($("#previewTree") || treePanel() || document).querySelector(".tree-status-empty");
    if (empty) empty.hidden = true;
    loadStatusForest();
  } else {
    hideStatusForest();
    loadStatusForest._key = "";
    const ownOk = nodes.map((tn) => {
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
    const host = $("#previewTree") || treePanel();
    if (host) {
      let empty = host.querySelector(".tree-status-empty");
      const anyOn = vis.some(Boolean);
      if (!anyOn && nodes.length) {
        if (!empty) {
          empty = document.createElement("div");
          empty.className = "tree-empty tree-status-empty";
          empty.textContent = "Aucun concept pour les statuts sélectionnés.";
          host.appendChild(empty);
        }
        empty.hidden = false;
      } else if (empty) {
        empty.hidden = true;
      }
    }
  }
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
