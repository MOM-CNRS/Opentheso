/** Fiche — création d'un concept, candidat ou collection. */
var createChooserCtx = { id: "", pref: "", path: "", type: "", candidate: "" };

function hideCreateChooser() {
  if (typeof hideConfirm === "function") hideConfirm("#createChooser");
  const add = document.getElementById("viewAdd");
  if (add) add.setAttribute("aria-expanded", "false");
}

function setCreateRowState(row, ok, lockAttr) {
  if (!row) return;
  row.classList.toggle("is-locked", !ok);
  row.disabled = !ok;
  const desc = row.querySelector(".spl-d");
  if (desc) {
    const key = ok ? "data-ok" : (lockAttr || "data-lock");
    desc.textContent = desc.getAttribute(key) || desc.getAttribute("data-lock") || desc.textContent;
  }
  const go = row.querySelector(".spl-go");
  const lock = row.querySelector(".ck-lock");
  if (go) go.hidden = !ok;
  if (lock) lock.hidden = ok;
}

function currentConceptParentId() {
  const el = liveDetailState();
  const kind = ((el && el.getAttribute("data-kind")) || "").toLowerCase();
  const id = ((el && el.getAttribute("data-id")) || (!state.home && state.conceptId) || "").trim();
  if (!id || kind !== "concept") return "";
  return id;
}

function currentConceptParentLabel() {
  const el = liveDetailState();
  return ((el && el.getAttribute("data-pref")) || "").trim();
}

function createChooserParentId() {
  const id = (createChooserCtx.id || "").trim();
  if (!id) return "";
  const type = (createChooserCtx.type || "").toLowerCase();
  if (createChooserCtx.candidate === "1" || type === "facet" || type === "candidat") return "";
  return id;
}

function paintCreateChooserSub() {
  const sub = document.getElementById("createChooserSub");
  if (!sub) return;
  const parentId = createChooserParentId();
  const pref = (createChooserCtx.pref || "").trim();
  const root = sub.getAttribute("data-root") || "";
  const under = sub.getAttribute("data-under") || "";
  if (parentId && pref && under) {
    sub.textContent = under.replace("{0}", pref);
  } else {
    sub.textContent = root;
  }
  sub.hidden = !sub.textContent;
}

function showCreateChooser(src) {
  const dlg = document.getElementById("createChooser");
  if (!dlg) return;
  const fromViewAdd = !!(src && src.id === "viewAdd");
  if (fromViewAdd && typeof state !== "undefined" && state.view === "collection") {
    if (dlg.getAttribute("data-can-collection") === "1") {
      showCreateCollection();
      return;
    }
  }
  createChooserCtx = {
    id: (src && src.getAttribute("data-id")) || "",
    pref: (src && src.getAttribute("data-pref")) || "",
    path: (src && src.getAttribute("data-path")) || "",
    type: (src && src.getAttribute("data-type")) || "",
    candidate: (src && src.getAttribute("data-candidate")) || ""
  };
  paintCreateChooserSub();
  const collectionOnly = fromViewAdd && typeof state !== "undefined" && state.view === "collection";
  if (collectionOnly) {
    const sub = document.getElementById("createChooserSub");
    if (sub) {
      sub.textContent = sub.getAttribute("data-collection") || "";
      sub.hidden = !sub.textContent;
    }
  }
  dlg.querySelectorAll("[data-act='create-pick']").forEach((row) => {
    const kind = row.getAttribute("data-kind");
    if (collectionOnly) {
      row.hidden = kind !== "collection";
      if (kind === "collection") setCreateRowState(row, false, "data-lock");
      return;
    }
    row.hidden = kind === "collection";
    if (kind === "collection") return;
    if (kind === "facet") {
      const can = dlg.getAttribute("data-can-facet") === "1";
      const parentId = createChooserParentId();
      if (!can) setCreateRowState(row, false, "data-lock");
      else if (!parentId) setCreateRowState(row, false, "data-need-parent");
      else setCreateRowState(row, true);
      return;
    }
    const flag = kind === "concept" ? "data-can-concept" : "data-can-cand";
    setCreateRowState(row, dlg.getAttribute(flag) === "1");
  });
  if (typeof showConfirm === "function") showConfirm("#createChooser");
  const add = document.getElementById("viewAdd");
  if (add) add.setAttribute("aria-expanded", "true");
}

function clickCreateConcept(mode) {
  const sel = mode === "top"
    ? '[data-create="concept-top"], [id$="createConceptTopGo"]'
    : '[data-create="concept"], [data-create="concept-child"], [id$="createConceptChildGo"]';
  const btn = document.querySelector(sel);
  if (btn) {
    btn.click();
    return true;
  }
  return false;
}

function runPendingCreate() {
  const kind = window._pendingCreate;
  window._pendingCreate = null;
  if (!kind) return;
  if (kind === "facette") {
    clickCreateFacet();
    return;
  }
  if (clickCreateConcept("child")) return;
  clickCreateConcept("top");
}

window.runPendingCreate = runPendingCreate;

function clickCreateFacet() {
  const btn = document.querySelector('[data-create="facet"], [id$="createFacetGo"]');
  if (btn) {
    btn.click();
    return true;
  }
  return false;
}

function selectedCollectionParent() {
  const id = (typeof state !== "undefined" && state.colId ? String(state.colId) : "").trim();
  if (!id) return { id: "", label: "" };
  let label = "";
  if (window.CSS && CSS.escape) {
    const node = document.querySelector('#panelCollectionTree .tn[data-id="' + CSS.escape(id) + '"] .tn-text');
    if (node) label = node.textContent.trim();
  }
  if (!label) {
    const pref = document.querySelector("#collectionDetail .cv-pref span");
    if (pref) label = pref.textContent.trim();
  }
  return { id: id, label: label || id };
}

function showCreateCollection() {
  if (!document.getElementById("viewCollectionDraft")) return;
  const name = document.getElementById("createCollName");
  const notation = document.getElementById("createCollNotation");
  const err = document.getElementById("createCollErr");
  const sub = document.getElementById("createCollectionSub");
  const parent = selectedCollectionParent();
  if (name) {
    name.value = "";
    name.classList.remove("is-err");
  }
  if (notation) notation.value = "";
  if (err) {
    err.textContent = "";
    err.hidden = true;
  }
  if (sub) {
    if (parent.id) {
      const pattern = sub.getAttribute("data-under") || "Elle sera créée sous « {0} ».";
      sub.textContent = pattern.replace("{0}", parent.label);
    } else {
      sub.textContent = sub.getAttribute("data-root") || sub.textContent;
    }
  }
  const parentField = document.getElementById("previewCreateCollParent");
  if (parentField) parentField.value = parent.id || "";
  state.home = false;
  state.draft = false;
  state.conceptDraft = false;
  state.facetDraft = false;
  state.collectionDraft = true;
  state.view = "collection";
  paint();
  window.setTimeout(() => { if (name) name.focus(); }, 30);
}

function hideCreateCollection() {
  state.collectionDraft = false;
  const name = document.getElementById("createCollName");
  const err = document.getElementById("createCollErr");
  if (name) name.classList.remove("is-err");
  if (err) {
    err.textContent = "";
    err.hidden = true;
  }
  const add = document.getElementById("viewAdd");
  if (add) add.setAttribute("aria-expanded", "false");
  paint();
}

function submitCreateCollection() {
  const nameEl = document.getElementById("createCollName");
  const notationEl = document.getElementById("createCollNotation");
  const err = document.getElementById("createCollErr");
  const name = nameEl ? nameEl.value.trim() : "";
  const notation = notationEl ? notationEl.value.trim() : "";
  if (!name) {
    if (err) {
      err.textContent = err.getAttribute("data-need") || "Le nom de la collection est obligatoire.";
      err.hidden = false;
    }
    if (nameEl) {
      nameEl.classList.add("is-err");
      nameEl.focus();
    }
    return;
  }
  if (nameEl) nameEl.classList.remove("is-err");
  if (err) err.hidden = true;
  if (typeof clickPreviewJsf !== "function") return;
  const parent = selectedCollectionParent();
  clickPreviewJsf("previewCreateCollGo", {
    createCollName: name,
    createCollNotation: notation,
    previewCreateCollParent: parent.id
  });
}

window.onCreateCollAjax = function (data) {
  if (!data || data.status !== "success") return;
  const status = document.getElementById("previewCreateCollStatus");
  const ok = !!(status && status.classList.contains("is-ok"));
  const msg = status ? (status.textContent || "").trim() : "";
  const id = status ? (status.getAttribute("data-id") || "").trim() : "";
  if (!ok) {
    const err = document.getElementById("createCollErr");
    if (err) {
      err.textContent = msg || "Création impossible.";
      err.hidden = false;
    }
    return;
  }
  hideCreateCollection();
  if (typeof toast === "function") toast(msg || "Collection créée");
  const parentId = (document.getElementById("previewCreateCollParent") || {}).value || "";
  if (typeof revealCreatedCollection === "function") {
    revealCreatedCollection(id, parentId.trim());
    return;
  }
  if (typeof invalidateCollectionTree === "function") invalidateCollectionTree();
  if (id && typeof openCollectionFromTree === "function") openCollectionFromTree(id);
  else if (typeof loadCollectionRoots === "function") loadCollectionRoots();
};

function pickCreateKind(kind) {
  hideCreateChooser();
  if (kind === "collection") {
    showCreateCollection();
    return;
  }
  if (kind === "candidat") {
    createCandidate({
      getAttribute: function (name) {
        if (name === "data-pref") return createChooserCtx.pref;
        if (name === "data-path") return createChooserCtx.path;
        if (name === "data-id") return createChooserCtx.id;
        return "";
      }
    });
    return;
  }
  if (kind === "facet" || kind === "facette") {
    const id = createChooserParentId();
    if (!id) return;
    createFacetDraft({
      getAttribute: function (name) {
        if (name === "data-pref") return createChooserCtx.pref || currentConceptParentLabel();
        if (name === "data-id") return id;
        return "";
      }
    });
    return;
  }
  if (kind !== "concept") return;
  const parentId = createChooserParentId();
  createConceptDraft({
    getAttribute: function (name) {
      if (name === "data-pref") return createChooserCtx.pref || (parentId ? currentConceptParentLabel() : "");
      if (name === "data-path") return createChooserCtx.path;
      if (name === "data-id") return parentId;
      return "";
    }
  });
}

