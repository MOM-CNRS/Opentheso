/** Actions groupées — état du panneau. */
/** Applique les classes d'état renvoyées par le bean après une action AJAX. */
function boSyncPanel(obj, op, cssClasses) {
  const panel = document.querySelector(`.bo-panel[data-obj="${CSS.escape(obj)}"][data-op="${CSS.escape(op)}"]`);
  if (!panel) return;
  const keepOn = panel.classList.contains("is-on");
  panel.className = ("bo-panel " + (cssClasses || "")).trim();
  if (keepOn) panel.classList.add("is-on");
  panel.setAttribute("data-live", "1");
  panel.setAttribute("data-obj", obj);
  panel.setAttribute("data-op", op);
  const form = panel.querySelector("form.bo-live");
  if (form) {
    const extra = (cssClasses || "").split(/\s+/).filter(Boolean);
    form.className = ["bo-card-b", "bo-live"].concat(extra).join(" ");
  }
  if (panel.classList.contains("has-errors")) {
    panel.classList.add("bo-shake");
    setTimeout(() => panel.classList.remove("bo-shake"), 420);
  }
}
window.boSyncPanel = boSyncPanel;

/** Le rendu ajax du formulaire ne réécrit pas le jeton de vue. On le recopie. */
function boKeepViewState() {
  const donor = document.querySelector("input[name='jakarta.faces.ViewState']");
  if (!donor || !donor.value) return;
  document.querySelectorAll("form.bo-live").forEach((form) => {
    let input = form.querySelector("input[name='jakarta.faces.ViewState']");
    if (!input) {
      input = document.createElement("input");
      input.type = "hidden";
      input.name = "jakarta.faces.ViewState";
      input.autocomplete = "off";
      form.appendChild(input);
    }
    if (input !== donor) input.value = donor.value;
  });
}
window.boKeepViewState = boKeepViewState;

function boAjaxBusy(data) {
  if (!data) return;
  const src = data.source;
  const panel = src && src.closest ? src.closest(".bo-panel") : null;
  if (data.status === "begin" && panel) panel.classList.add("is-busy");
  if (data.status === "complete" && panel) panel.classList.remove("is-busy");
  // "complete" arrive avant le remplacement du DOM : ne pas y relire data-cls.
  if (data.status === "success") {
    if (panel) panel.classList.remove("is-busy");
    boKeepViewState();
    document.querySelectorAll(".bo-state[data-cls]").forEach((el) => {
      const o = el.getAttribute("data-obj");
      const p = el.getAttribute("data-op");
      const cls = el.getAttribute("data-cls") || "";
      if (o && p) boSyncPanel(o, p, cls);
    });
  }
}
window.boAjaxBusy = boAjaxBusy;

function bindBoDropZones() {
  document.querySelectorAll(".bo-panel[data-live='1'] .bo-drop-empty").forEach((zone) => {
    if (zone.dataset.dropBound === "1") return;
    zone.dataset.dropBound = "1";
    const inputId = zone.getAttribute("for");
    const input = inputId ? document.getElementById(inputId) : null;
    if (!input) return;
    ["dragenter", "dragover"].forEach((ev) => {
      zone.addEventListener(ev, (e) => {
        e.preventDefault();
        zone.classList.add("is-drag");
      });
    });
    ["dragleave", "drop"].forEach((ev) => {
      zone.addEventListener(ev, (e) => {
        e.preventDefault();
        zone.classList.remove("is-drag");
      });
    });
    zone.addEventListener("drop", (e) => {
      const file = e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0];
      if (!file) return;
      const dt = new DataTransfer();
      dt.items.add(file);
      input.files = dt.files;
      input.dispatchEvent(new Event("change", { bubbles: true }));
    });
  });
}
window.bindBoDropZones = bindBoDropZones;
document.addEventListener("DOMContentLoaded", bindBoDropZones);
if (window.faces && faces.ajax) {
  faces.ajax.addOnEvent(function (data) {
    if (data.status === "success") {
      boKeepViewState();
      bindBoDropZones();
    }
  });
} else if (window.jsf && jsf.ajax) {
  jsf.ajax.addOnEvent(function (data) {
    if (data.status === "success") {
      boKeepViewState();
      bindBoDropZones();
    }
  });
}

function closeConceptBlockOverlay() {
  const ov = $("#cblockOverlay");
  if (!ov) return;
  ov.hidden = true;
  ov.setAttribute("aria-hidden", "true");
  const body = ov.querySelector(".block-modal-body");
  if (body) body.innerHTML = "";
}

function expandConceptBlock(gear) {
  const block = gear && gear.closest(".cblock");
  if (!block) return;
  const body = block.querySelector(".cblock-body");
  if (!body) return;
  let ov = $("#cblockOverlay");
  if (!ov) {
    ov = document.createElement("div");
    ov.id = "cblockOverlay";
    ov.className = "block-overlay";
    ov.innerHTML = '<div class="block-modal" role="dialog" aria-modal="true">'
      + '<div class="block-modal-head"><span class="cblock-overlay-title"></span>'
      + '<button type="button" data-act="cblock-collapse" title="Fermer" aria-label="Fermer">×</button></div>'
      + '<div class="block-modal-body"></div></div>';
    ov.addEventListener("click", function (e) {
      if (e.target === ov) closeConceptBlockOverlay();
    });
    document.body.appendChild(ov);
  }
  const titleEl = ov.querySelector(".cblock-overlay-title");
  const dest = ov.querySelector(".block-modal-body");
  if (titleEl) {
    const named = block.querySelector(".cblock-title");
    titleEl.textContent = (named && named.textContent || "").trim();
  }
  if (dest) dest.replaceChildren(body.cloneNode(true));
  ov.hidden = false;
  ov.setAttribute("aria-hidden", "false");
}

