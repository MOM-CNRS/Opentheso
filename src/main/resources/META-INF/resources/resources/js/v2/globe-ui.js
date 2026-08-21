/**
 * OpenTheso V2 — pont vers le globe WebGL.
 */
"use strict";

function globeHost() {
  return {
    isHyper: () => state.view === "hyper",
    thesaurusId: thesaurusId,
    thesaurusLang: thesaurusLang,
    conceptId: () => state.conceptId,
    ctx: () => document.body.getAttribute("data-ctx") || "",
    toast: toast,
    onPick: (id) => {
      state.conceptId = id;
      highlightConcept(id);
      revealTreeConcept(id);
    },
    onOpen: (id) => {
      state.home = false;
      state.draft = false;
      state.conceptId = id;
      state.graphFront = false;
      highlightConcept(id);
      revealTreeConcept(id);
      if (openLiveDetail(id, "concept")) {
        paintGraphBack();
        return;
      }
      paint();
    }
  };
}

function otGlobe() {
  if (window.OTGlobe) window.OTGlobe.attach(globeHost());
  return window.OTGlobe;
}

function ensureGlobe() {
  const api = otGlobe();
  if (api) api.ensure();
}

function stopGlobe() {
  if (window.OTGlobe) window.OTGlobe.stop();
}

function invalidateGlobe() {
  if (window.OTGlobe) window.OTGlobe.invalidate();
}

function globeSelectId(id, draw) {
  if (window.OTGlobe) window.OTGlobe.selectId(id, draw);
}

function onThesaurusLangChanged() {
  invalidateCollectionTree();
  invalidateGlobe();
  if (state.view === "collection") {
    loadCollectionRoots();
    if (state.colId) loadCollectionDetail(state.colId);
  }
  if (state.view === "hyper" && window.OTGlobe) window.OTGlobe.reload();
  if (state.view !== "tableau") {
    invalidateTableRows();
    return;
  }
  if (tableCacheMatches()) return;
  loadTableRows();
}
