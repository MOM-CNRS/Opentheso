/** Collections — sélecteur. */
var COLL_AC_PAGE = 7;
var collectionPicker = { items: null, key: "", selected: [], hint: "", active: -1, showAll: false };

function collectionPickerKey() {
  return thesaurusId() + "|" + thesaurusLang();
}

function loadCollectionPickerItems() {
  const key = collectionPickerKey();
  if (collectionPicker.items && collectionPicker.key === key) {
    return Promise.resolve(collectionPicker.items);
  }
  if (collectionPicker._load && collectionPicker.key === key) {
    return collectionPicker._load;
  }
  collectionPicker.key = key;
  collectionPicker._load = collectionApi("list").then((items) => {
    collectionPicker.items = Array.isArray(items) ? items : [];
    collectionPicker._load = null;
    if (collectionPicker.hint) {
      applyCollectionHint(collectionPicker.hint);
      collectionPicker.hint = "";
    }
    return collectionPicker.items;
  }).catch(() => {
    collectionPicker._load = null;
    collectionPicker.items = [];
    return collectionPicker.items;
  });
  return collectionPicker._load;
}

function scoreCollection(item, q) {
  if (!q) return { score: 0 };
  const pref = norm(item.label || "");
  if (pref === q) return { score: 0 };
  if (pref.startsWith(q)) return { score: 1 };
  if (pref.includes(q)) return { score: 2 };
  const nota = norm(item.notation || "");
  if (nota && nota.includes(q)) return { score: 3, via: "nota" };
  const id = norm(item.id || "");
  if (id.includes(q)) return { score: 4, via: "id" };
  return { score: -1 };
}

function rankCollectionHits(query) {
  const q = norm((query || "").trim());
  return (collectionPicker.items || []).map((item) => {
    const sc = scoreCollection(item, q);
    if (sc.score < 0) return null;
    return { item: item, score: sc.score, via: sc.via };
  }).filter(Boolean).sort((a, b) => a.score - b.score || (a.item.label || "").localeCompare(b.item.label || "", "fr"));
}

function shownCollectionRows() {
  return $$("#draftCollMenu .ac-row.is-shown");
}

function isCollectionSelected(id) {
  return collectionPicker.selected.some((item) => item.id === id);
}

function syncCollectionPickerHidden() {
  const hidden = $("#draftCollIds");
  if (hidden) hidden.value = collectionPicker.selected.map((item) => item.id).join(",");
  const chips = $("#draftCollChips");
  if (!chips) return;
  chips.hidden = collectionPicker.selected.length === 0;
  chips.innerHTML = collectionPicker.selected.map((item) => {
    return '<button type="button" class="coll-chip" data-act="coll-chip-remove" data-id="'
      + escapeHtml(item.id) + '" title="Retirer">'
      + escapeHtml(item.label)
      + '<span aria-hidden="true">×</span></button>';
  }).join("");
}

function syncCollectionClear() {
  const input = $("#draftColl");
  const clear = $("#draftCollClear");
  if (clear) clear.hidden = !(input && input.value);
}

function toggleCollectionPick(id, label) {
  if (!id) return;
  if (isCollectionSelected(id)) {
    collectionPicker.selected = collectionPicker.selected.filter((item) => item.id !== id);
  } else {
    collectionPicker.selected.push({ id: id, label: label || id });
  }
  syncCollectionPickerHidden();
  renderCollectionMenu($("#draftColl") ? $("#draftColl").value : "");
}

function applyCollectionHint(hint) {
  const q = norm((hint || "").trim());
  if (!q || !collectionPicker.items) return;
  const match = collectionPicker.items.find((item) => norm(item.label || "") === q);
  if (match && !isCollectionSelected(match.id)) {
    collectionPicker.selected.push({ id: match.id, label: match.label });
    syncCollectionPickerHidden();
  }
}

function resetCollectionPicker(hint) {
  collectionPicker.selected = [];
  collectionPicker.hint = (hint || "").trim();
  collectionPicker.active = -1;
  collectionPicker.showAll = false;
  const input = $("#draftColl");
  if (input) input.value = "";
  syncCollectionClear();
  syncCollectionPickerHidden();
  closeCollectionPicker();
  loadCollectionPickerItems();
}

function highlightCollectionQuery(query) {
  $$("#draftCollMenu mark.hl").forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) return;
    while (mark.firstChild) parent.insertBefore(mark.firstChild, mark);
    parent.removeChild(mark);
    parent.normalize();
  });
  if (!query || typeof wrapQueryIn !== "function") return;
  $$("#draftCollMenu .ac-row.is-shown .ac-pref").forEach((el) => wrapQueryIn(el, query));
}

function collectionItemHtml(hit) {
  const item = hit.item;
  const on = isCollectionSelected(item.id);
  return '<button type="button" class="ac-row' + (on ? " is-picked" : "")
    + '" role="option" aria-selected="' + (on ? "true" : "false")
    + '" data-act="coll-pick-item" data-id="' + escapeHtml(item.id)
    + '" data-label="' + escapeHtml(item.label) + '">'
    + '<span class="ac-ico">▦</span><span class="ac-body"><span class="ac-pref">'
    + escapeHtml(item.label) + "</span></span>"
    + (on ? '<span class="coll-pick-check" aria-hidden="true">✓</span>' : "")
    + "</button>";
}

function setCollectionAcIdx(index) {
  const rows = shownCollectionRows();
  collectionPicker.active = index;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === index));
  if (index >= 0 && rows[index]) rows[index].scrollIntoView({ block: "nearest" });
}

function renderCollectionMenu(query) {
  const menu = $("#draftCollMenu");
  const list = $("#draftCollList");
  if (!menu || !list) return;
  const raw = (query || "").trim();
  const hits = rankCollectionHits(raw);
  const limit = collectionPicker.showAll ? hits.length : COLL_AC_PAGE;
  list.innerHTML = hits.map((hit) => collectionItemHtml(hit)).join("");
  const rows = $$("#draftCollList .ac-row");
  rows.forEach((row, i) => {
    row.classList.toggle("is-shown", i < limit);
    row.style.order = String(i);
  });
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", !!raw && hits.length === 0);
  const qEl = $("#draftCollQuery");
  if (qEl) qEl.textContent = raw;
  const count = $("#draftCollCount");
  if (count) count.textContent = String(hits.length);
  const more = $("#draftCollMore");
  if (more) more.hidden = collectionPicker.showAll || hits.length <= COLL_AC_PAGE;
  collectionPicker.active = -1;
  highlightCollectionQuery(raw);
  syncCollectionCombo();
}

function syncCollectionCombo() {
  const menu = $("#draftCollMenu");
  const input = $("#draftColl");
  const combo = $("#draftCollCombo");
  const pick = $("#draftCollPick");
  const open = !!(menu && !menu.hidden);
  if (pick) pick.classList.toggle("is-open", open);
  if (combo) combo.classList.toggle("is-open", open);
  if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
}

function openCollectionMenu(showAll) {
  const menu = $("#draftCollMenu");
  if (!menu) return;
  if (showAll) collectionPicker.showAll = true;
  menu.hidden = false;
  syncCollectionCombo();
  loadCollectionPickerItems().then(() => renderCollectionMenu($("#draftColl") ? $("#draftColl").value : ""));
}

function toggleCollectionMenu() {
  const menu = $("#draftCollMenu");
  if (menu && !menu.hidden) {
    closeCollectionPicker();
    return;
  }
  const input = $("#draftColl");
  if (input) input.focus();
  openCollectionMenu(true);
}

function closeCollectionPicker() {
  const menu = $("#draftCollMenu");
  const combo = $("#draftCollCombo");
  if (combo) combo.classList.remove("is-focused");
  if (!menu || menu.hidden) return false;
  menu.hidden = true;
  collectionPicker.showAll = false;
  collectionPicker.active = -1;
  syncCollectionCombo();
  return true;
}

function pickActiveCollection() {
  const rows = shownCollectionRows();
  const row = collectionPicker.active >= 0 ? rows[collectionPicker.active] : rows[0];
  if (!row) return false;
  toggleCollectionPick(row.getAttribute("data-id"), row.getAttribute("data-label"));
  const input = $("#draftColl");
  if (input) {
    input.value = "";
    syncCollectionClear();
    input.focus();
  }
  collectionPicker.showAll = false;
  renderCollectionMenu("");
  return true;
}

document.addEventListener("click", (e) => {
  const treeAct = e.target.closest && e.target.closest("#panelCollectionTree [data-act='col-page']");
  if (treeAct) {
    e.preventDefault();
    showMoreCollectionChildren(treeAct.closest(".tn"));
    return;
  }
  if (!e.target.closest("#collMemPick")) hideCollectionMemDrop();
  const act = e.target.closest("[data-act]");
  const action = act && act.getAttribute("data-act");
  if (action === "coll-mem-hit") {
    e.preventDefault();
    addCollectionMem(act.getAttribute("data-id"), act.getAttribute("data-label"));
    return;
  }
  if (action === "coll-mem-clear") {
    e.preventDefault();
    const input = document.getElementById("collMemQ");
    if (input) {
      input.value = "";
      input.focus();
    }
    collectionMemState.hits = [];
    hideCollectionMemDrop();
    syncCollectionMemClear();
    return;
  }
  if (action === "coll-mem-remove") {
    e.preventDefault();
    removeCollectionMem(act.getAttribute("data-index"));
    return;
  }
  if (action === "coll-mem-more") {
    e.preventDefault();
    collectionMembersFreshFrom = collectionMembersVisible;
    collectionMembersVisible += COLL_MEMBER_PAGE;
    if (collectionFiche) renderCollectionDetail(collectionFiche);
    return;
  }
  if (action === "coll-pick-toggle") {
    e.preventDefault();
    toggleCollectionMenu();
    return;
  }
  if (action === "coll-pick-clear") {
    e.preventDefault();
    const input = $("#draftColl");
    if (input) {
      input.value = "";
      input.focus();
    }
    collectionPicker.showAll = false;
    syncCollectionClear();
    closeCollectionPicker();
    return;
  }
  if (action === "coll-pick-more") {
    e.preventDefault();
    collectionPicker.showAll = true;
    renderCollectionMenu($("#draftColl") ? $("#draftColl").value : "");
    return;
  }
  if (action === "coll-pick-item") {
    e.preventDefault();
    toggleCollectionPick(act.getAttribute("data-id"), act.getAttribute("data-label"));
    const input = $("#draftColl");
    if (input) {
      input.value = "";
      syncCollectionClear();
      input.focus();
    }
    collectionPicker.showAll = false;
    renderCollectionMenu("");
    return;
  }
  if (action === "coll-chip-remove") {
    e.preventDefault();
    toggleCollectionPick(act.getAttribute("data-id"), "");
    return;
  }
  if (!$("#draftCollPick") || $("#draftCollPick").contains(e.target)) return;
  closeCollectionPicker();
});

document.addEventListener("input", (e) => {
  if (e.target && e.target.id === "collMemQ") scheduleCollectionMemSearch();
  if (!e.target || e.target.id !== "draftColl") return;
  collectionPicker.showAll = false;
  syncCollectionClear();
  if (e.target.value.trim()) openCollectionMenu(false);
  else closeCollectionPicker();
});

document.addEventListener("focusin", (e) => {
  if (e.target && e.target.id === "draftColl") {
    const combo = $("#draftCollCombo");
    if (combo) combo.classList.add("is-focused");
    if (e.target.value.trim()) openCollectionMenu(false);
  }
});

document.addEventListener("focusout", (e) => {
  if (e.target && e.target.id === "draftColl") {
    const combo = $("#draftCollCombo");
    requestAnimationFrame(() => {
      const pick = $("#draftCollPick");
      if (combo && pick && !pick.contains(document.activeElement)) {
        combo.classList.remove("is-focused");
      }
    });
  }
});

document.addEventListener("keydown", (e) => {
  if (e.target && e.target.id === "collMemQ") {
    const rows = shownCollectionMemRows();
    if (e.key === "ArrowDown") {
      e.preventDefault();
      collectionMemState.active = Math.min(collectionMemState.active + 1, Math.max(rows.length - 1, 0));
      rows.forEach((row, i) => row.classList.toggle("is-active", i === collectionMemState.active));
      return;
    }
    if (e.key === "ArrowUp") {
      e.preventDefault();
      collectionMemState.active = Math.max(collectionMemState.active - 1, 0);
      rows.forEach((row, i) => row.classList.toggle("is-active", i === collectionMemState.active));
      return;
    }
    if (e.key === "Escape") {
      hideCollectionMemDrop();
      return;
    }
    if (e.key === "Enter") {
      const row = rows[collectionMemState.active >= 0 ? collectionMemState.active : 0];
      if (row) {
        e.preventDefault();
        addCollectionMem(row.getAttribute("data-id"), row.getAttribute("data-label"));
      }
      return;
    }
  }
  if (!e.target || e.target.id !== "draftColl") return;
  const menu = $("#draftCollMenu");
  const open = menu && !menu.hidden;
  if (e.key === "ArrowDown") {
    e.preventDefault();
    if (!open) openCollectionMenu(!!e.target.value.trim() ? false : true);
    else setCollectionAcIdx(Math.min(collectionPicker.active + 1, Math.max(shownCollectionRows().length - 1, -1)));
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    setCollectionAcIdx(Math.max(collectionPicker.active - 1, -1));
  } else if (e.key === "Enter") {
    if (open && shownCollectionRows().length) {
      e.preventDefault();
      e.stopPropagation();
      pickActiveCollection();
    }
  }
});
