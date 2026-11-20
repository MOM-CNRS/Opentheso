/** Fiche — ordre des blocs et libellés. */
const CARD_IDS = ["contexte", "traductions", "notes", "relations", "collections", "alignement", "ressources", "usage", "identifiants", "temporel", "exportSkos"];
const CARD_LAYOUT_VERSION = 2;
/** Blocs gouvernance candidat : toujours juste après la bannière (hors réordonnancement fiche). */
const GOVERNANCE_CARD_ORDER = { candVotes: -2, candDiscussion: -1 };

function isCardLayoutEditable() {
  return document.body.getAttribute("data-logged-in") === "1";
}

function defaultCardLayout() {
  return { order: CARD_IDS.slice(), collapsed: {} };
}

function parseCardLayout() {
  const layout = defaultCardLayout();
  try {
    const raw = JSON.parse(document.body.getAttribute("data-card-layout") || "");
    if (raw.v === CARD_LAYOUT_VERSION && Array.isArray(raw.order)) {
      const next = raw.order.filter((id) => CARD_IDS.indexOf(id) >= 0);
      CARD_IDS.forEach((id) => {
        if (next.indexOf(id) >= 0) return;
        if (id === "usage") {
          const at = next.indexOf("ressources");
          next.splice(at < 0 ? next.length : at + 1, 0, id);
        } else if (id === "exportSkos") {
          const at = next.indexOf("identifiants");
          next.splice(at < 0 ? next.length : at + 1, 0, id);
        } else {
          next.push(id);
        }
      });
      layout.order = next;
    }
    layout.collapsed = {};
    if (Array.isArray(raw.collapsed)) {
      raw.collapsed.forEach((id) => {
        if (CARD_IDS.indexOf(id) >= 0) layout.collapsed[id] = true;
      });
    }
  } catch (ex) {}
  return layout;
}

let cardLayout = null;

function currentCardLayout() {
  if (!cardLayout) cardLayout = parseCardLayout();
  return cardLayout;
}

function readCardOrder() {
  return currentCardLayout().order.slice();
}

function readCardFold() {
  return Object.assign({}, currentCardLayout().collapsed);
}

function persistCardLayout() {
  if (!isCardLayoutEditable()) return;
  const layout = currentCardLayout();
  const body = document.body;
  const payload = {
    v: CARD_LAYOUT_VERSION,
    order: layout.order,
    collapsed: Object.keys(layout.collapsed)
  };
  try {
    body.setAttribute("data-card-layout", JSON.stringify(payload));
  } catch (ex) {}
  const ctx = body.getAttribute("data-ctx") || "";
  clearTimeout(persistCardLayout._t);
  persistCardLayout._t = setTimeout(() => {
    fetch(ctx + "/api/account/concept-blocks", {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload)
    }).catch(() => {});
  }, 180);
}

function accountBlockLabels() {
  const map = {};
  $$("#accBlockLabelSrc [data-id]").forEach((el) => {
    map[el.getAttribute("data-id")] = (el.textContent || "").trim();
  });
  return map;
}

function accountBlockMsg(name, fallback) {
  const root = $("#accBlockLayout");
  return (root && root.getAttribute("data-msg-" + name)) || fallback;
}

function accBlockReduceMotion() {
  return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function accBlockGripHtml(editable) {
  const label = escapeHtml(accountBlockMsg("drag", "Glisser pour réordonner"));
  return '<span class="cblock-drag acc-block-drag" role="button" tabindex="' + (editable ? "0" : "-1") + '"'
    + ' title="' + label + '" aria-label="' + label + '">'
    + '<svg class="cblock-grip" viewBox="0 0 8 14" aria-hidden="true" focusable="false">'
    + '<circle cx="2" cy="2" r="1.35"/><circle cx="6" cy="2" r="1.35"/>'
    + '<circle cx="2" cy="7" r="1.35"/><circle cx="6" cy="7" r="1.35"/>'
    + '<circle cx="2" cy="12" r="1.35"/><circle cx="6" cy="12" r="1.35"/>'
    + "</svg></span>";
}

function paintAccountBlockRow(row, id, index) {
  if (!row) return;
  const open = !currentCardLayout().collapsed[id];
  const openLbl = accountBlockMsg("open", "Ouvert");
  const closedLbl = accountBlockMsg("closed", "Fermé");
  const n = row.querySelector(".acc-block-n");
  const state = row.querySelector(".acc-block-state");
  const sw = row.querySelector("[data-act='acc-block-toggle']");
  if (n) n.textContent = String(index + 1);
  row.classList.toggle("is-closed", !open);
  if (state) {
    state.textContent = open ? openLbl : closedLbl;
    state.classList.toggle("is-open", open);
  }
  if (sw) {
    sw.classList.toggle("on", open);
    sw.setAttribute("aria-checked", open ? "true" : "false");
    sw.setAttribute("title", open ? openLbl : closedLbl);
    sw.setAttribute("aria-label", open ? openLbl : closedLbl);
  }
}

function renderAccountBlockList() {
  const box = $("#accBlockRows");
  const root = $("#accBlockLayout");
  if (!box || !root) return;
  const layout = currentCardLayout();
  const labels = accountBlockLabels();
  const editable = isCardLayoutEditable();
  const resetBtn = root.querySelector("[data-act='acc-block-reset']");
  if (resetBtn) resetBtn.disabled = !editable;
  box.innerHTML = layout.order.map((id, i) => {
    const open = !layout.collapsed[id];
    return '<div class="acc-block-row' + (open ? "" : " is-closed") + '" data-id="' + escapeHtml(id) + '">'
      + accBlockGripHtml(editable)
      + '<span class="acc-block-n">' + (i + 1) + "</span>"
      + '<span class="acc-block-t">' + escapeHtml(labels[id] || id) + "</span>"
      + '<span class="acc-block-state' + (open ? " is-open" : "") + '">'
      + escapeHtml(open ? accountBlockMsg("open", "Ouvert") : accountBlockMsg("closed", "Fermé"))
      + "</span>"
      + '<button type="button" class="vo-sw acc-block-sw' + (open ? " on" : "") + '" data-act="acc-block-toggle"'
      + ' role="switch" aria-checked="' + (open ? "true" : "false") + '"'
      + ' title="' + escapeHtml(open ? accountBlockMsg("open", "Ouvert") : accountBlockMsg("closed", "Fermé")) + '"'
      + (editable ? "" : " disabled") + "></button>"
      + "</div>";
  }).join("");
}

function applyAccountBlockDomOrder() {
  const box = $("#accBlockRows");
  if (!box) return;
  currentCardLayout().order.forEach((id, i) => {
    const row = box.querySelector('.acc-block-row[data-id="' + CSS.escape(id) + '"]');
    if (!row) return;
    box.appendChild(row);
    paintAccountBlockRow(row, id, i);
  });
}

function flipAccountBlockRows(run) {
  const box = $("#accBlockRows");
  if (!box) {
    run();
    return;
  }
  const rows = $$(".acc-block-row", box);
  if (accBlockReduceMotion() || !rows.length) {
    run();
    return;
  }
  const first = new Map(rows.map((el) => [el, el.getBoundingClientRect()]));
  run();
  $$(".acc-block-row", box).forEach((el) => {
    const prev = first.get(el);
    if (!prev) return;
    const next = el.getBoundingClientRect();
    const dy = prev.top - next.top;
    if (Math.abs(dy) < 1) return;
    el.style.transition = "none";
    el.style.transform = "translateY(" + dy + "px)";
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        el.style.transition = "transform .22s cubic-bezier(.2,.8,.2,1)";
        el.style.transform = "";
        const clear = () => {
          el.style.transition = "";
          el.style.transform = "";
          el.removeEventListener("transitionend", clear);
        };
        el.addEventListener("transitionend", clear);
      });
    });
  });
}

function toggleAccountBlock(id) {
  if (!isCardLayoutEditable() || !id) return;
  const layout = currentCardLayout();
  if (layout.collapsed[id]) delete layout.collapsed[id];
  else layout.collapsed[id] = true;
  persistCardLayout();
  const row = $("#accBlockRows") && $("#accBlockRows").querySelector('.acc-block-row[data-id="' + CSS.escape(id) + '"]');
  const index = currentCardLayout().order.indexOf(id);
  paintAccountBlockRow(row, id, index);
}

function resetAccountBlocks() {
  if (!isCardLayoutEditable()) return;
  cardLayout = defaultCardLayout();
  persistCardLayout();
  renderAccountBlockList();
  const msg = accountBlockMsg("reset-done", "");
  if (msg) toast(msg);
}

function clearAccountDropMarks() {
  $$(".acc-block-row.drop-before, .acc-block-row.drop-after, .acc-block-row.is-dragging").forEach((el) => {
    el.classList.remove("drop-before", "drop-after", "is-dragging");
  });
}

function bindAccountBlockLayout() {
  const root = $("#accBlockLayout");
  if (!root) return;
  if (!bindAccountBlockLayout._on) {
    bindAccountBlockLayout._on = true;
    let handleId = null;
    let dragId = null;
    root.addEventListener("click", (e) => {
      const btn = e.target.closest("[data-act]");
      if (!btn || btn.disabled || !root.contains(btn)) return;
      const act = btn.getAttribute("data-act");
      const row = btn.closest(".acc-block-row");
      const id = row && row.getAttribute("data-id");
      if (act === "acc-block-reset") {
        e.preventDefault();
        resetAccountBlocks();
        return;
      }
      if (act === "acc-block-toggle") {
        e.preventDefault();
        toggleAccountBlock(id);
      }
    });
    root.addEventListener("pointerdown", (e) => {
      if (!isCardLayoutEditable()) return;
      const handle = e.target.closest(".acc-block-drag");
      if (!handle || handle.getAttribute("tabindex") === "-1") return;
      const row = handle.closest(".acc-block-row[data-id]");
      if (!row) return;
      handleId = row.getAttribute("data-id");
      row.setAttribute("draggable", "true");
    });
    root.addEventListener("pointerup", () => {
      if (dragId) return;
      handleId = null;
      $$('.acc-block-row[draggable="true"]', root).forEach((el) => el.removeAttribute("draggable"));
    });
    root.addEventListener("dragstart", (e) => {
      const row = e.target.closest && e.target.closest(".acc-block-row[data-id]");
      if (!row || row.getAttribute("data-id") !== handleId) {
        if (row) e.preventDefault();
        return;
      }
      dragId = handleId;
      row.classList.add("is-dragging");
      e.dataTransfer.effectAllowed = "move";
      try { e.dataTransfer.setData("text/plain", dragId); } catch (ex) {}
    });
    root.addEventListener("dragover", (e) => {
      if (!dragId) return;
      const row = e.target.closest && e.target.closest(".acc-block-row[data-id]");
      if (!row) return;
      e.preventDefault();
      e.dataTransfer.dropEffect = "move";
      const id = row.getAttribute("data-id");
      const pos = cardDropPos(e, row);
      $$(".acc-block-row.drop-before, .acc-block-row.drop-after", root).forEach((el) => {
        if (el !== row) el.classList.remove("drop-before", "drop-after");
      });
      if (id === dragId) {
        row.classList.remove("drop-before", "drop-after");
        return;
      }
      row.classList.toggle("drop-before", pos === "before");
      row.classList.toggle("drop-after", pos === "after");
    });
    root.addEventListener("drop", (e) => {
      const row = e.target.closest && e.target.closest(".acc-block-row[data-id]");
      if (!row || !dragId) return;
      e.preventDefault();
      const toId = row.getAttribute("data-id");
      const pos = cardDropPos(e, row);
      flipAccountBlockRows(() => {
        dropCardAt(dragId, toId, pos);
        applyAccountBlockDomOrder();
      });
    });
    root.addEventListener("dragend", () => {
      dragId = null;
      handleId = null;
      $$('.acc-block-row[draggable="true"]', root).forEach((el) => el.removeAttribute("draggable"));
      clearAccountDropMarks();
    });
  }
  renderAccountBlockList();
}

function applyConceptCardOrder(root) {
  const order = readCardOrder();
  const scope = root || document;
  scope.querySelectorAll(".cv-blocks .cblock[data-card-id]").forEach((el) => {
    const id = el.getAttribute("data-card-id");
    if (Object.prototype.hasOwnProperty.call(GOVERNANCE_CARD_ORDER, id)) {
      el.style.order = String(GOVERNANCE_CARD_ORDER[id]);
      return;
    }
    const i = order.indexOf(id);
    el.style.order = String(i < 0 ? 99 : i);
  });
  applyConceptCardFold(scope);
}

function foldLabels() {
  const cv = document.querySelector("#viewLive .cv");
  return {
    collapse: (cv && cv.getAttribute("data-msg-fold")) || "Replier",
    expand: (cv && cv.getAttribute("data-msg-unfold")) || "Déplier"
  };
}

function syncFoldButton(block, collapsed) {
  const btn = block.querySelector(":scope > .cblock-head .cblock-fold");
  if (!btn) return;
  const labels = foldLabels();
  const label = collapsed ? labels.expand : labels.collapse;
  btn.setAttribute("aria-expanded", collapsed ? "false" : "true");
  btn.setAttribute("title", label);
  btn.setAttribute("aria-label", label);
}

function applyConceptCardFold(root) {
  const map = readCardFold();
  const scope = root || document;
  scope.querySelectorAll(".cv-blocks .cblock[data-card-id]").forEach((el) => {
    const id = el.getAttribute("data-card-id");
    const editing = el.classList.contains("is-editing");
    const collapsed = !editing && !!map[id];
    el.classList.toggle("is-collapsed", collapsed);
    syncFoldButton(el, collapsed);
  });
}

function toggleConceptBlockFold(el) {
  if (!isCardLayoutEditable()) return;
  const block = el && el.closest && el.closest(".cblock[data-card-id]");
  if (!block || block.classList.contains("is-editing")) return;
  const id = block.getAttribute("data-card-id");
  if (!id) return;
  const layout = currentCardLayout();
  const collapsed = !block.classList.contains("is-collapsed");
  if (collapsed) layout.collapsed[id] = true;
  else delete layout.collapsed[id];
  persistCardLayout();
  block.classList.toggle("is-collapsed", collapsed);
  syncFoldButton(block, collapsed);
  if (!collapsed && id === "ressources") {
    requestAnimationFrame(() => {
      if (typeof bindGpsMap === "function") bindGpsMap();
    });
  }
  if (window.syncViewRail) window.syncViewRail();
}

function clearCardDropMarks() {
  $$(".cblock.drop-before, .cblock.drop-after, .cblock.is-dragging").forEach((el) => {
    el.classList.remove("drop-before", "drop-after", "is-dragging");
  });
}

function dropCardAt(fromId, toId, pos) {
  if (!isCardLayoutEditable() || !fromId || !toId || fromId === toId) return;
  const layout = currentCardLayout();
  const order = layout.order;
  const fi = order.indexOf(fromId);
  if (fi < 0) return;
  order.splice(fi, 1);
  let ti = order.indexOf(toId);
  if (ti < 0) return;
  if (pos === "after") ti += 1;
  order.splice(ti, 0, fromId);
  persistCardLayout();
  applyConceptCardOrder();
}

function cardDropPos(e, el) {
  const r = el.getBoundingClientRect();
  return e.clientY < r.top + r.height / 2 ? "before" : "after";
}

function bindConceptCardOrder() {
  try {
    localStorage.removeItem("ot.conceptCardOrder");
    localStorage.removeItem("ot.conceptCardFold");
  } catch (ex) {}
  applyConceptCardOrder();
  bindAccountBlockLayout();
  if (bindConceptCardOrder._on) return;
  bindConceptCardOrder._on = true;
  document.addEventListener("click", (e) => {
    if (e.target.closest(".cblock-fold, .cblock-drag, input, button, a, textarea, select")) return;
    const title = e.target.closest("#viewLive .cblock-title");
    if (!title) return;
    toggleConceptBlockFold(title);
  });
  let handleId = null;
  let dragId = null;
  document.addEventListener("pointerdown", (e) => {
    if (!isCardLayoutEditable()) return;
    const handle = e.target.closest(".cblock-drag");
    if (!handle) return;
    const block = handle.closest(".cblock[data-card-id]");
    if (!block) return;
    handleId = block.getAttribute("data-card-id");
    block.setAttribute("draggable", "true");
  });
  document.addEventListener("pointerup", () => {
    if (dragId) return;
    handleId = null;
    $$('.cblock[draggable="true"]').forEach((el) => el.removeAttribute("draggable"));
  });
  document.addEventListener("dragstart", (e) => {
    const block = e.target.closest && e.target.closest(".cblock[data-card-id]");
    if (!block || block.getAttribute("data-card-id") !== handleId) {
      if (block) e.preventDefault();
      return;
    }
    dragId = handleId;
    block.classList.add("is-dragging");
    e.dataTransfer.effectAllowed = "move";
    try { e.dataTransfer.setData("text/plain", dragId); } catch (ex) {}
  });
  document.addEventListener("dragover", (e) => {
    if (!dragId) return;
    const block = e.target.closest && e.target.closest(".cblock[data-card-id]");
    if (!block) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    const id = block.getAttribute("data-card-id");
    const pos = cardDropPos(e, block);
    $$(".cblock.drop-before, .cblock.drop-after").forEach((el) => {
      if (el !== block) el.classList.remove("drop-before", "drop-after");
    });
    if (id === dragId) {
      block.classList.remove("drop-before", "drop-after");
      return;
    }
    block.classList.toggle("drop-before", pos === "before");
    block.classList.toggle("drop-after", pos === "after");
  });
  document.addEventListener("drop", (e) => {
    const block = e.target.closest && e.target.closest(".cblock[data-card-id]");
    if (!block || !dragId) return;
    e.preventDefault();
    const toId = block.getAttribute("data-card-id");
    dropCardAt(dragId, toId, cardDropPos(e, block));
  });
  document.addEventListener("dragend", () => {
    dragId = null;
    handleId = null;
    $$('.cblock[draggable="true"]').forEach((el) => el.removeAttribute("draggable"));
    clearCardDropMarks();
  });
}

window.applyConceptCardOrder = applyConceptCardOrder;
window.applyConceptCardFold = applyConceptCardFold;
window.toggleConceptBlockFold = toggleConceptBlockFold;
window.bindConceptCardOrder = bindConceptCardOrder;
window.applyConceptLabelUi = applyConceptLabelUi;
window.closeImgLightbox = closeImgLightbox;
window.closeGpsLightbox = closeGpsLightbox;
document.addEventListener("click", interceptLabelEditSwap, true);
document.addEventListener("click", (e) => {
  const act = e.target.closest("[data-act]");
  const action = act && act.getAttribute("data-act");
  if (action === "img-gallery-open") {
    e.preventDefault();
    openImgLightbox();
    return;
  }
  if (action === "img-gallery-prev" || action === "img-lightbox-prev") {
    e.preventDefault();
    showImgAt(imgGallery.index - 1, action === "img-lightbox-prev" || imgGallery.lightbox);
    return;
  }
  if (action === "img-gallery-next" || action === "img-lightbox-next") {
    e.preventDefault();
    showImgAt(imgGallery.index + 1, action === "img-lightbox-next" || imgGallery.lightbox);
    return;
  }
  if (action === "img-gallery-thumb") {
    e.preventDefault();
    const idx = parseInt(act.getAttribute("data-index") || "0", 10);
    showImgAt(Number.isFinite(idx) ? idx : 0, imgGallery.lightbox);
    return;
  }
  if (action === "img-lightbox-modal") {
    return;
  }
  if (action === "img-lightbox-dismiss" || action === "img-lightbox-close") {
    e.preventDefault();
    closeImgLightbox();
    return;
  }
  if (action === "gps-map-expand") {
    e.preventDefault();
    openGpsLightbox();
    return;
  }
  if (action === "gps-map-goto") {
    e.preventDefault();
    const idx = parseInt(act.getAttribute("data-index") || "0", 10);
    selectGpsPoint(Number.isFinite(idx) ? idx : 0);
    return;
  }
  if (action === "gps-lightbox-modal") {
    return;
  }
  if (action === "gps-lightbox-dismiss" || action === "gps-lightbox-close") {
    e.preventDefault();
    closeGpsLightbox();
    return;
  }
  if (action === "term-tag-add") {
    e.preventDefault();
    const root = act.closest(".term-tags");
    const input = root && root.querySelector(".term-tags-add input");
    if (input) {
      if (addTermTag(root, input.value)) input.value = "";
      input.focus();
    }
    return;
  }
  if (action === "term-tag-remove") {
    e.preventDefault();
    removeTermTag(act.closest(".term-tags"), act.getAttribute("data-index"));
    return;
  }
  if (action === "facet-pick-toggle") {
    e.preventDefault();
    toggleLabelFacetMenu();
    return;
  }
  if (action === "facet-pick-clear") {
    e.preventDefault();
    const input = $("#labelFacetQuery");
    if (input) {
      input.value = "";
      input.focus();
    }
    labelFacetPicker.showAll = false;
    syncLabelFacetClear();
    closeLabelFacetPicker();
    return;
  }
  if (action === "facet-pick-more") {
    e.preventDefault();
    labelFacetPicker.showAll = true;
    renderLabelFacetMenu($("#labelFacetQuery") ? $("#labelFacetQuery").value : "");
    return;
  }
  if (action === "facet-pick-item") {
    e.preventDefault();
    toggleLabelFacetPick(act.getAttribute("data-id"), act.getAttribute("data-label"));
    const input = $("#labelFacetQuery");
    if (input) {
      input.value = "";
      syncLabelFacetClear();
      input.focus();
    }
    labelFacetPicker.showAll = false;
    renderLabelFacetMenu("");
    return;
  }
  if (action === "facet-chip-remove") {
    e.preventDefault();
    toggleLabelFacetPick(act.getAttribute("data-id"), "");
    return;
  }
  if (action === "cv-coll-pick-toggle") {
    e.preventDefault();
    toggleCvCollMenu();
    return;
  }
  if (action === "cv-coll-pick-clear") {
    e.preventDefault();
    const input = $("#cvCollQuery");
    if (input) {
      input.value = "";
      input.focus();
    }
    cvCollPicker.showAll = false;
    syncCvCollClear();
    closeCvCollPicker();
    return;
  }
  if (action === "cv-coll-pick-more") {
    e.preventDefault();
    cvCollPicker.showAll = true;
    renderCvCollMenu($("#cvCollQuery") ? $("#cvCollQuery").value : "");
    return;
  }
  if (action === "cv-coll-pick-item") {
    e.preventDefault();
    toggleCvCollPick(act.getAttribute("data-id"), act.getAttribute("data-label"));
    const input = $("#cvCollQuery");
    if (input) {
      input.value = "";
      syncCvCollClear();
      input.focus();
    }
    cvCollPicker.showAll = false;
    renderCvCollMenu("");
    return;
  }
  if (action === "cv-coll-chip-remove") {
    e.preventDefault();
    toggleCvCollPick(act.getAttribute("data-id"), "");
    return;
  }
  if (action === "rel-pick-toggle") {
    e.preventDefault();
    toggleRelMenu(act.getAttribute("data-kind"));
    return;
  }
  if (action === "rel-pick-clear") {
    e.preventDefault();
    const kind = act.getAttribute("data-kind");
    const els = relEls(kind);
    if (els.query) {
      els.query.value = "";
      els.query.focus();
    }
    if (relPickers[kind]) {
      relPickers[kind].showAll = false;
      relPickers[kind].hits = [];
      relPickers[kind].query = "";
      relPickers[kind].seq += 1;
    }
    syncRelClear(kind);
    closeRelPicker(kind);
    return;
  }
  if (action === "rel-pick-more") {
    e.preventDefault();
    const kind = act.getAttribute("data-kind");
    if (relPickers[kind]) relPickers[kind].showAll = true;
    renderRelMenu(kind);
    return;
  }
  if (action === "rel-pick-item") {
    e.preventDefault();
    toggleRelPick(act.getAttribute("data-kind"), act.getAttribute("data-id"), act.getAttribute("data-label"));
    return;
  }
  if (action === "rel-chip-remove") {
    e.preventDefault();
    toggleRelPick(act.getAttribute("data-kind"), act.getAttribute("data-id"), "");
    return;
  }
  if (action === "crel-pick-toggle") {
    e.preventDefault();
    toggleCrelMenu();
    return;
  }
  if (action === "crel-pick-clear") {
    e.preventDefault();
    const input = $("#cvCrelQuery");
    if (input) {
      input.value = "";
      input.focus();
    }
    crelPicker.showAll = false;
    crelPicker.hits = [];
    crelPicker.query = "";
    crelPicker.seq += 1;
    syncCrelClear();
    closeCrelPicker();
    return;
  }
  if (action === "crel-pick-more") {
    e.preventDefault();
    crelPicker.showAll = true;
    renderCrelMenu();
    return;
  }
  if (action === "crel-pick-item") {
    e.preventDefault();
    toggleCrelPick(act.getAttribute("data-id"), act.getAttribute("data-label"), act.getAttribute("data-type"));
    return;
  }
  if (action === "crel-chip-remove") {
    e.preventDefault();
    toggleCrelPick(act.getAttribute("data-id"), "", "");
    return;
  }
  const inFacet = $("#labelFacetPick") && $("#labelFacetPick").contains(e.target);
  const inColl = $("#cvCollPick") && $("#cvCollPick").contains(e.target);
  const inRel = e.target.closest("[data-rel-kind]");
  const inCrel = $("#cvCrelPick") && $("#cvCrelPick").contains(e.target);
  if (!inFacet) closeLabelFacetPicker();
  if (!inColl) closeCvCollPicker();
  if (!inRel) closeAllRelPickers();
  if (!inCrel) closeCrelPicker();
});
document.addEventListener("input", (e) => {
  if (!e.target) return;
  if (e.target.id === "labelFacetQuery") {
    labelFacetPicker.showAll = false;
    syncLabelFacetClear();
    if (e.target.value.trim()) openLabelFacetMenu(false);
    else closeLabelFacetPicker();
    return;
  }
  if (e.target.id === "cvCollQuery") {
    cvCollPicker.showAll = false;
    syncCvCollClear();
    if (e.target.value.trim()) openCvCollMenu(false);
    else closeCvCollPicker();
    return;
  }
  if (e.target.id === "cvCrelQuery") {
    scheduleCrelSearch();
    return;
  }
  const relKind = relKindFromQueryId(e.target.id);
  if (relKind) scheduleRelSearch(relKind);
});
document.addEventListener("focusin", (e) => {
  if (e.target && e.target.id === "labelFacetQuery") {
    const combo = $("#labelFacetCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) openLabelFacetMenu(false);
  }
  if (e.target && e.target.id === "cvCollQuery") {
    const combo = $("#cvCollCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) openCvCollMenu(false);
  }
  if (e.target && e.target.id === "cvCrelQuery") {
    const combo = $("#cvCrelCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) scheduleCrelSearch();
  }
  const relKind = e.target && relKindFromQueryId(e.target.id);
  if (relKind) {
    const combo = relEls(relKind).combo;
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) scheduleRelSearch(relKind);
  }
});
document.addEventListener("focusout", (e) => {
  if (e.target && e.target.id === "labelFacetQuery") {
    const combo = $("#labelFacetCombo");
    requestAnimationFrame(() => {
      const pick = $("#labelFacetPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
  if (e.target && e.target.id === "cvCollQuery") {
    const combo = $("#cvCollCombo");
    requestAnimationFrame(() => {
      const pick = $("#cvCollPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
  if (e.target && e.target.id === "cvCrelQuery") {
    const combo = $("#cvCrelCombo");
    requestAnimationFrame(() => {
      const pick = $("#cvCrelPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
  const relKind = e.target && relKindFromQueryId(e.target.id);
  if (relKind) {
    const els = relEls(relKind);
    requestAnimationFrame(() => {
      if (els.combo && els.pick && !els.pick.contains(document.activeElement)) {
        els.combo.classList.remove("is-focused");
      }
    });
  }
});
document.addEventListener("keydown", (e) => {
  if (!e.target) return;
  if (imgGallery.lightbox) {
    if (e.key === "Escape") {
      e.preventDefault();
      e.stopPropagation();
      closeImgLightbox();
      return;
    }
    if (e.key === "ArrowLeft") {
      e.preventDefault();
      showImgAt(imgGallery.index - 1, true);
      return;
    }
    if (e.key === "ArrowRight") {
      e.preventDefault();
      showImgAt(imgGallery.index + 1, true);
      return;
    }
  }
  if (gpsMap.lightbox) {
    if (e.key === "Escape") {
      e.preventDefault();
      e.stopPropagation();
      closeGpsLightbox();
      return;
    }
    if (e.key === "ArrowLeft") {
      e.preventDefault();
      selectGpsPoint(gpsMap.index - 1);
      return;
    }
    if (e.key === "ArrowRight") {
      e.preventDefault();
      selectGpsPoint(gpsMap.index + 1);
      return;
    }
  }
  if (e.target.id === "labelFacetQuery") {
    const menu = $("#labelFacetMenu");
    const open = menu && !menu.hidden;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!open) openLabelFacetMenu(!!e.target.value.trim() ? false : true);
      else setLabelFacetAcIdx(Math.min(labelFacetPicker.active + 1, Math.max(shownLabelFacetRows().length - 1, -1)));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setLabelFacetAcIdx(Math.max(labelFacetPicker.active - 1, -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      if (open && shownLabelFacetRows().length) pickActiveLabelFacet();
      else saveEditingLabelBlock(e.target);
    } else if (e.key === "Escape") {
      if (open) {
        e.preventDefault();
        closeLabelFacetPicker();
      }
    }
    return;
  }
  if (e.target.id === "cvCrelQuery") {
    const menu = $("#cvCrelMenu");
    const open = menu && !menu.hidden;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!open) {
        if (e.target.value.trim()) scheduleCrelSearch();
        else {
          crelPicker.hits = [];
          crelPicker.query = "";
          openCrelMenu();
        }
      } else {
        setCrelAcIdx(Math.min(crelPicker.active + 1, Math.max(shownCrelRows().length - 1, -1)));
      }
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setCrelAcIdx(Math.max(crelPicker.active - 1, -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      if (open && shownCrelRows().length) pickActiveCrel();
    } else if (e.key === "Escape") {
      if (open) {
        e.preventDefault();
        closeCrelPicker();
      }
    }
    return;
  }
  const relKind = relKindFromQueryId(e.target.id);
  if (relKind) {
    const els = relEls(relKind);
    const open = els.menu && !els.menu.hidden;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (!open) {
        if (e.target.value.trim()) scheduleRelSearch(relKind);
        else {
          relPickers[relKind].hits = [];
          relPickers[relKind].query = "";
          openRelMenu(relKind);
        }
      } else {
        setRelAcIdx(relKind, Math.min(relPickers[relKind].active + 1, Math.max(shownRelRows(relKind).length - 1, -1)));
      }
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setRelAcIdx(relKind, Math.max(relPickers[relKind].active - 1, -1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      e.stopPropagation();
      if (open && shownRelRows(relKind).length) pickActiveRel(relKind);
    } else if (e.key === "Escape") {
      if (open) {
        e.preventDefault();
        closeRelPicker(relKind);
      }
    }
    return;
  }
  if (e.target.id !== "cvCollQuery") return;
  const menu = $("#cvCollMenu");
  const open = menu && !menu.hidden;
  if (e.key === "ArrowDown") {
    e.preventDefault();
    if (!open) openCvCollMenu(!!e.target.value.trim() ? false : true);
    else setCvCollAcIdx(Math.min(cvCollPicker.active + 1, Math.max(shownCvCollRows().length - 1, -1)));
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    setCvCollAcIdx(Math.max(cvCollPicker.active - 1, -1));
  } else if (e.key === "Enter") {
    e.preventDefault();
    e.stopPropagation();
    if (open && shownCvCollRows().length) pickActiveCvColl();
  } else if (e.key === "Escape") {
    if (open) {
      e.preventDefault();
      closeCvCollPicker();
    }
  }
});
if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", bindConceptCardOrder);
else bindConceptCardOrder();
if (window.faces && faces.ajax) {
  faces.ajax.addOnEvent(function (data) {
    if (data.status === "begin") lockFicheScroll(data.source);
    if (data.status === "success") {
      applyConceptCardOrder();
      applyConceptLabelUi(data.source);
      restoreFicheScroll();
    }
  });
} else if (window.jsf && jsf.ajax) {
  jsf.ajax.addOnEvent(function (data) {
    if (data.status === "begin") lockFicheScroll(data.source);
    if (data.status === "success") {
      applyConceptCardOrder();
      applyConceptLabelUi(data.source);
      restoreFicheScroll();
    }
  });
}
