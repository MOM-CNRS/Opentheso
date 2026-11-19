/**
 * OpenTheso — pont vers le globe WebGL.
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
  if (window.OTGlobe) {
    window.OTGlobe.attach(globeHost()).ensure();
    return;
  }
  if (document.querySelector("script[data-globe]")) return;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const prep = document.createElement("script");
  prep.src = ctx + "/resources/js/globe-interaction.js?v=20261118-split";
  prep.dataset.globe = "1";
  prep.onload = () => {
    const script = document.createElement("script");
    script.src = ctx + "/resources/js/globe.js?v=20261118-split";
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

function globeSelectId(id, draw) {
  if (window.OTGlobe) window.OTGlobe.selectId(id, draw);
}

function onThesaurusLangChanged() {
  if (typeof refreshOpenSelectionLabels === "function") refreshOpenSelectionLabels();
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
