/**
 * OpenTheso — pont vers la sphère de voisinage.
 */
"use strict";

/* Graphe choisi : vue Graphe, ou liste de résultats d'une recherche lancée depuis le graphe. */
function graphMode() {
  return state.view === "hyper" || (state.view === "recherche" && !!state.graphSearch);
}

/* La sphère occupe le panneau principal (sinon c'est la fiche, avec « Retour au graphe »). */
function graphOnScreen() {
  return graphMode() && state.graphFront && !!$("#viewHyper");
}

function graphConceptId() {
  return state.conceptId || "";
}

/* Nature du centre : la fiche fait foi quand elle est chargée, sinon le dernier clic. */
function graphConceptKind() {
  const live = liveDetailState();
  if (live && live.getAttribute("data-id") === state.conceptId && live.getAttribute("data-kind")) {
    return live.getAttribute("data-kind") === "facet" ? "facet" : "concept";
  }
  return state.graphKind || "concept";
}

function graphPageUrl() {
  const id = graphConceptId();
  if (!id) return "graph/graphe.xhtml";
  return "graph/graphe.xhtml?id=" + encodeURIComponent(id)
    + (graphConceptKind() === "facet" ? "&type=facet" : "");
}

/* Ouvre un concept sans quitter le graphe : sphère recentrée, fiche chargée à côté. */
function openGraphConcept(id, nodeType, label) {
  state.home = false;
  state.draft = false;
  state.conceptDraft = false;
  state.facetDraft = false;
  state.collectionDraft = false;
  state.conceptId = id;
  state.graphKind = nodeType === "facet" ? "facet" : "concept";
  openLiveDetail(id, nodeType || "concept", label);
  syncConceptUrl(id, state.graphKind);
  highlightConcept(id);
  paint();
}

function globeHost() {
  return {
    isHyper: graphOnScreen,
    thesaurusId: thesaurusId,
    thesaurusLang: thesaurusLang,
    conceptId: graphConceptId,
    conceptKind: graphConceptKind,
    ctx: () => document.body.getAttribute("data-ctx") || "",
    toast: toast,
    onPick: (id, label, kind) => openGraphConcept(id, kind, label),
    onOpen: (id, kind) => {
      state.graphFront = false;
      const live = liveDetailState();
      const ready = live && live.getAttribute("data-requested") === "1" && live.getAttribute("data-id") === id;
      if (ready) {
        paint();
        showLiveDetail();
        return;
      }
      if (state.pendingOpenId === id) {
        beginLiveOpen();
        return;
      }
      openGraphConcept(id, kind);
    }
  };
}

function otGlobe() {
  if (window.OTGlobe) window.OTGlobe.attach(globeHost());
  return window.OTGlobe;
}

function ensureGlobe() {
  if (window.OTGlobe) {
    window.OTGlobe.attach(globeHost()).ensure();
    return;
  }
  if (document.querySelector("script[data-globe]")) return;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const prep = document.createElement("script");
  prep.src = ctx + "/resources/js/globe-interaction.js?v=20261124-facettes";
  prep.dataset.globe = "1";
  prep.onload = () => {
    const script = document.createElement("script");
    script.src = ctx + "/resources/js/globe.js?v=20261124-facettes";
    script.onload = () => {
      if (window.OTGlobe) window.OTGlobe.attach(globeHost()).ensure();
    };
    document.head.appendChild(script);
  };
  document.head.appendChild(prep);
}

function stopGlobe() {
  if (window.OTGlobe) window.OTGlobe.stop();
}

function invalidateGlobe() {
  if (window.OTGlobe) window.OTGlobe.invalidate();
}

function globeSelectId(id, kind) {
  if (window.OTGlobe) window.OTGlobe.selectId(id, kind);
}

function onThesaurusLangChanged() {
  if (typeof refreshOpenSelectionLabels === "function") refreshOpenSelectionLabels();
  invalidateCollectionTree();
  invalidateGlobe();
  if (state.view === "collection") {
    loadCollectionRoots();
    if (state.colId) loadCollectionDetail(state.colId);
  }
  if (graphOnScreen()) {
    /* La réponse a remplacé le panneau : il faut le réafficher avant de recharger la sphère. */
    paintMain();
    if (window.OTGlobe) window.OTGlobe.reload();
  }
  if (state.view !== "tableau") {
    invalidateTableRows();
    return;
  }
  if (tableCacheMatches()) return;
  loadTableRows();
}
