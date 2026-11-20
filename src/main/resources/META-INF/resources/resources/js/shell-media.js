/** Fiche — images et coordonnées. */
const imgGallery = { index: 0, items: [], lightbox: false, conceptId: "" };

function imgGalleryCaption(item) {
  if (!item) return "";
  const clean = (part) => {
    const value = (part || "").trim();
    if (!value || value.toLowerCase() === "null" || /_(sparql|rest)$/i.test(value)) return "";
    return value;
  };
  return [item.name, item.copyright, item.creator]
      .flatMap((part) => clean(part).split(" · "))
      .map(clean)
      .filter(Boolean)
      .join(" · ");
}

function closeImgLightbox(opts) {
  const open = imgGallery.lightbox;
  imgGallery.lightbox = false;
  document.body.classList.remove("is-img-lightbox");
  const box = $("#cvImgLightbox");
  if (box) {
    box.hidden = true;
    box.setAttribute("aria-hidden", "true");
  }
  if (open && !(opts && opts.restoreFocus === false)) {
    const opener = $("#cvImgGalleryOpen");
    if (opener) opener.focus();
  }
  return open;
}

function showImgAt(index, alsoLightbox) {
  const items = imgGallery.items;
  if (!items.length) return;
  const n = items.length;
  imgGallery.index = ((index % n) + n) % n;
  const item = items[imgGallery.index];
  const main = $("#cvImgGalleryMain");
  if (main) {
    main.src = item.src || "";
    main.alt = item.name || "Image du concept";
  }
  const cap = $("#cvImgGalleryCap");
  if (cap) cap.textContent = imgGalleryCaption(item);
  const count = $("#cvImgGalleryCount");
  if (count) {
    count.hidden = n < 2;
    count.textContent = (imgGallery.index + 1) + " / " + n;
  }
  $$("#cvImgGalleryThumbs [data-act='img-gallery-thumb']").forEach((btn, i) => {
    btn.classList.toggle("is-on", i === imgGallery.index);
  });
  const root = $("#cvImgGallery");
  if (root) root.classList.toggle("is-single", n < 2);
  if (alsoLightbox || imgGallery.lightbox) {
    const light = $("#cvImgLightboxImg");
    if (light) {
      light.src = item.src || "";
      light.alt = item.name || "Image du concept";
    }
    const lightCap = $("#cvImgLightboxCap");
    if (lightCap) lightCap.textContent = imgGalleryCaption(item);
    const orig = $("#cvImgLightboxOrig");
    if (orig) orig.href = item.src || "#";
    const box = $("#cvImgLightbox");
    if (box) box.classList.toggle("is-single", n < 2);
  }
}

function openImgLightbox() {
  if (!imgGallery.items.length) return;
  closeGpsLightbox({ restoreFocus: false });
  imgGallery.lightbox = true;
  document.body.classList.add("is-img-lightbox");
  const box = $("#cvImgLightbox");
  if (box) {
    box.hidden = false;
    box.setAttribute("aria-hidden", "false");
  }
  showImgAt(imgGallery.index, true);
  const closeBtn = box && box.querySelector("[data-act='img-lightbox-close']");
  if (closeBtn) closeBtn.focus();
}

function bindImgGallery() {
  const cv = document.querySelector("#viewLive .cv.is-on");
  const conceptId = cv ? (cv.getAttribute("data-id") || "") : "";
  if (conceptId !== imgGallery.conceptId) {
    closeImgLightbox({ restoreFocus: false });
    imgGallery.index = 0;
    imgGallery.conceptId = conceptId;
  }
  const root = $("#cvImgGallery");
  if (!root) {
    closeImgLightbox({ restoreFocus: false });
    imgGallery.items = [];
    imgGallery.index = 0;
    return;
  }
  const items = $$("#cvImgGalleryThumbs [data-act='img-gallery-thumb']").map((btn) => ({
    src: btn.getAttribute("data-src") || "",
    name: btn.getAttribute("data-name") || "",
    copyright: btn.getAttribute("data-copy") || "",
    creator: btn.getAttribute("data-creator") || ""
  }));
  const same = items.length === imgGallery.items.length
      && items.every((item, i) => item.src === imgGallery.items[i].src);
  imgGallery.items = items;
  if (!same) imgGallery.index = 0;
  root.classList.toggle("is-single", items.length < 2);
  if (imgGallery.lightbox && items.length) {
    const box = $("#cvImgLightbox");
    if (box) {
      box.hidden = false;
      box.setAttribute("aria-hidden", "false");
    }
    document.body.classList.add("is-img-lightbox");
    showImgAt(imgGallery.index, true);
  } else {
    closeImgLightbox({ restoreFocus: false });
    showImgAt(imgGallery.index, false);
  }
}

const gpsMap = {
  points: [],
  mode: "point",
  index: 0,
  lightbox: false,
  conceptId: "",
  gen: 0,
  edit: false,
  ignoreClick: false,
  mini: null,
  big: null,
  miniEl: null,
  bigEl: null,
  markersMini: [],
  markersBig: []
};

function gpsVisibleCount(mode, n) {
  return mode === "polygon" && n > 1 ? n - 1 : n;
}

function gpsModeOf(points) {
  if (points.length < 2) return "point";
  const a = points[0];
  const b = points[points.length - 1];
  if (points.length >= 3
      && Math.abs(a.lat - b.lat) < 1e-9
      && Math.abs(a.lng - b.lng) < 1e-9) {
    return "polygon";
  }
  return "polyline";
}

function gpsBadgeText() {
  if (gpsMap.edit && !gpsMap.points.length) return "Cliquez pour poser un point";
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  if (gpsMap.mode === "polygon") return "Polygone · " + n + " sommet" + (n > 1 ? "s" : "");
  if (gpsMap.mode === "polyline") return "Polyligne · " + n + " point" + (n > 1 ? "s" : "");
  return gpsMap.edit ? (n + " point" + (n > 1 ? "s" : "")) : "Point";
}

function gpsOsmUrl(pt) {
  if (!pt) return "#";
  const lat = encodeURIComponent(pt.lat);
  const lng = encodeURIComponent(pt.lng);
  return "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lng + "#map=16/" + lat + "/" + lng;
}

function gpsAccent() {
  return getComputedStyle(document.documentElement).getPropertyValue("--accent").trim() || "#1f7a5c";
}

function gpsDrawLatLngs() {
  if (gpsMap.edit) {
    return gpsMap.points.map((p) => [p.lat, p.lng]);
  }
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  return gpsMap.points.slice(0, n).map((p) => [p.lat, p.lng]);
}

function formatGpsNum(n) {
  const text = String(Math.round(Number(n) * 1e6) / 1e6);
  return text.indexOf(".") >= 0 ? text : text + ".0";
}

function writeGpsEditCoord(index, lat, lng) {
  const latIn = document.querySelector("[data-gps-axis='lat'][data-index='" + index + "']");
  const lngIn = document.querySelector("[data-gps-axis='lng'][data-index='" + index + "']");
  const latText = formatGpsNum(lat);
  const lngText = formatGpsNum(lng);
  if (latIn) latIn.value = latText;
  if (lngIn) lngIn.value = lngText;
  const btn = document.querySelector("#cvGpsPts [data-act='gps-map-goto'][data-index='" + index + "']");
  if (btn) {
    btn.setAttribute("data-lat", latText);
    btn.setAttribute("data-lng", lngText);
  }
}

function bindGpsEditHandlers(map) {
  const canvas = map.getContainer();
  if (canvas) canvas.style.cursor = "crosshair";
  map.on("dragstart", () => { gpsMap.ignoreClick = true; });
  map.on("dragend", () => {
    window.setTimeout(() => { gpsMap.ignoreClick = false; }, 220);
  });
  map.on("click", (e) => {
    if (gpsMap.ignoreClick) return;
    const latEl = document.querySelector("[id$='gpsClickLat']");
    const lngEl = document.querySelector("[id$='gpsClickLng']");
    const go = document.querySelector("[id$='gpsClickAdd']");
    if (!latEl || !lngEl || !go) return;
    latEl.value = formatGpsNum(e.latlng.lat);
    lngEl.value = formatGpsNum(e.latlng.lng);
    go.click();
  });
}

function ensureLeaflet() {
  if (window.L) return Promise.resolve();
  if (ensureLeaflet._p) return ensureLeaflet._p;
  const cssHref = document.body.getAttribute("data-leaflet-css") || "";
  const jsHref = document.body.getAttribute("data-leaflet-js") || "";
  ensureLeaflet._p = new Promise((resolve, reject) => {
    if (!jsHref) {
      reject(new Error("leaflet"));
      return;
    }
    if (cssHref && !document.querySelector("link[data-leaflet]")) {
      const css = document.createElement("link");
      css.rel = "stylesheet";
      css.href = cssHref;
      css.dataset.leaflet = "1";
      document.head.appendChild(css);
    }
    const script = document.createElement("script");
    script.src = jsHref;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("leaflet"));
    document.head.appendChild(script);
  });
  return ensureLeaflet._p;
}

function destroyGpsMap(slot) {
  try {
    if (gpsMap[slot]) gpsMap[slot].remove();
  } catch (err) { /* ajax already replaced the map container */ }
  gpsMap[slot] = null;
  if (slot === "mini") {
    gpsMap.miniEl = null;
    gpsMap.markersMini = [];
  } else {
    gpsMap.bigEl = null;
    gpsMap.markersBig = [];
  }
}

function destroyGpsMaps() {
  destroyGpsMap("mini");
  destroyGpsMap("big");
}

function closeGpsLightbox(opts) {
  const open = gpsMap.lightbox;
  gpsMap.lightbox = false;
  document.body.classList.remove("is-gps-lightbox");
  const box = $("#cvGpsLightbox");
  if (box) {
    box.hidden = true;
    box.setAttribute("aria-hidden", "true");
  }
  destroyGpsMap("big");
  if (open && !(opts && opts.restoreFocus === false)) {
    const fab = $("[data-act='gps-map-expand']");
    if (fab) fab.focus();
  }
  return open;
}

function updateGpsMeta() {
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  const pt = gpsMap.points[Math.min(gpsMap.index, Math.max(n - 1, 0))];
  const badge = $("#cvGpsBadge");
  if (badge) badge.textContent = gpsBadgeText();
  const cap = $("#cvGpsLightboxCap");
  if (cap) {
    cap.textContent = pt
        ? gpsBadgeText() + " · " + pt.lat + "  " + pt.lng
        : gpsBadgeText();
  }
  const href = gpsOsmUrl(pt);
  const osm = $("#cvGpsOsm");
  if (osm) osm.href = href;
  const osmBig = $("#cvGpsLightboxOsm");
  if (osmBig) osmBig.href = href;
  $$("#cvGpsPts .gps-pt").forEach((btn, i) => {
    const row = btn.closest("li");
    if (row) row.hidden = !gpsMap.edit && i >= n;
    btn.classList.toggle("is-on", i === gpsMap.index);
  });
}

function paintGpsPins(markers) {
  (markers || []).forEach((marker, i) => {
    const el = marker.getElement();
    if (el) el.classList.toggle("is-on", i === gpsMap.index);
  });
}

function selectGpsPoint(index) {
  const n = gpsVisibleCount(gpsMap.mode, gpsMap.points.length);
  if (!n) return;
  gpsMap.index = ((index % n) + n) % n;
  updateGpsMeta();
  paintGpsPins(gpsMap.markersMini);
  paintGpsPins(gpsMap.markersBig);
  const pt = gpsMap.points[gpsMap.index];
  if (!pt) return;
  const ll = [pt.lat, pt.lng];
  if (gpsMap.mini) gpsMap.mini.panTo(ll);
  if (gpsMap.big) gpsMap.big.panTo(ll);
}

function mountGpsLayer(map, markerSlot) {
  const latlngs = gpsDrawLatLngs();
  if (!latlngs.length || !window.L) return;
  const accent = gpsAccent();
  if (gpsMap.mode === "polygon" && latlngs.length >= 3) {
    L.polygon(latlngs, { color: accent, weight: 2.5, fillColor: accent, fillOpacity: 0.18 }).addTo(map);
  } else if (gpsMap.mode === "polyline" && latlngs.length >= 2) {
    L.polyline(latlngs, { color: accent, weight: 2.5 }).addTo(map);
  }
  const markers = latlngs.map((ll, i) => {
    const marker = L.marker(ll, {
      draggable: !!gpsMap.edit,
      icon: L.divIcon({
        className: "gps-pin" + (i === gpsMap.index ? " is-on" : ""),
        html: '<span class="gps-pin-n">' + (i + 1) + "</span>",
        iconSize: [22, 22],
        iconAnchor: [11, 11]
      })
    }).addTo(map);
    marker.on("click", () => selectGpsPoint(i));
    if (gpsMap.edit) {
      marker.on("dragstart", () => { gpsMap.ignoreClick = true; });
      marker.on("dragend", (ev) => {
        const next = ev.target.getLatLng();
        writeGpsEditCoord(i, next.lat, next.lng);
        gpsMap.points[i] = { lat: next.lat, lng: next.lng };
        gpsMap.mode = gpsModeOf(gpsMap.points);
        mountGpsMini();
        window.setTimeout(() => { gpsMap.ignoreClick = false; }, 220);
      });
    }
    return marker;
  });
  gpsMap[markerSlot] = markers;
  if (latlngs.length === 1) map.setView(latlngs[0], 14);
  else map.fitBounds(L.latLngBounds(latlngs).pad(0.14));
}

function mountGpsMini() {
  const canvas = $("#cvGpsCanvas");
  if (!canvas || !window.L) return;
  if (!gpsMap.points.length && !gpsMap.edit) return;
  destroyGpsMap("mini");
  gpsMap.miniEl = canvas;
  gpsMap.mini = L.map(canvas, {
    scrollWheelZoom: false,
    doubleClickZoom: !gpsMap.edit,
    attributionControl: true
  });
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "© OpenStreetMap"
  }).addTo(gpsMap.mini);
  if (gpsMap.points.length) mountGpsLayer(gpsMap.mini, "markersMini");
  else gpsMap.mini.setView([46.7, 2.5], 5);
  if (gpsMap.edit) bindGpsEditHandlers(gpsMap.mini);
  const refreshMini = () => gpsMap.mini && gpsMap.mini.invalidateSize();
  requestAnimationFrame(refreshMini);
  setTimeout(refreshMini, 260);
}

function mountGpsBig() {
  const canvas = $("#cvGpsLightboxCanvas");
  if (!canvas || !window.L || !gpsMap.points.length) return;
  destroyGpsMap("big");
  gpsMap.bigEl = canvas;
  gpsMap.big = L.map(canvas, { scrollWheelZoom: true, attributionControl: true });
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "© OpenStreetMap"
  }).addTo(gpsMap.big);
  mountGpsLayer(gpsMap.big, "markersBig");
  const refreshBig = () => gpsMap.big && gpsMap.big.invalidateSize();
  requestAnimationFrame(refreshBig);
  setTimeout(refreshBig, 80);
}

function openGpsLightbox() {
  if (!gpsMap.points.length) return;
  if (typeof closeImgLightbox === "function") closeImgLightbox({ restoreFocus: false });
  gpsMap.lightbox = true;
  document.body.classList.add("is-gps-lightbox");
  const box = $("#cvGpsLightbox");
  if (box) {
    box.hidden = false;
    box.setAttribute("aria-hidden", "false");
  }
  updateGpsMeta();
  ensureLeaflet().then(() => {
    if (!gpsMap.lightbox) return;
    mountGpsBig();
    const closeBtn = box && box.querySelector("[data-act='gps-lightbox-close']");
    if (closeBtn) closeBtn.focus();
  }).catch(() => {});
}

function bindGpsMap() {
  const cv = document.querySelector("#viewLive .cv.is-on");
  const conceptId = cv ? (cv.getAttribute("data-id") || "") : "";
  if (conceptId !== gpsMap.conceptId) {
    closeGpsLightbox({ restoreFocus: false });
    gpsMap.index = 0;
    gpsMap.conceptId = conceptId;
  }
  const root = $("#cvGpsMap");
  const gen = ++gpsMap.gen;
  if (!root) {
    closeGpsLightbox({ restoreFocus: false });
    destroyGpsMaps();
    gpsMap.points = [];
    gpsMap.index = 0;
    gpsMap.edit = false;
    return;
  }
  gpsMap.edit = root.getAttribute("data-edit") === "1";
  const points = $$("#cvGpsPts [data-act='gps-map-goto']").map((btn) => ({
    lat: parseFloat(btn.getAttribute("data-lat") || ""),
    lng: parseFloat(btn.getAttribute("data-lng") || "")
  })).filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng));
  gpsMap.points = points;
  gpsMap.mode = gpsModeOf(points);
  const n = gpsVisibleCount(gpsMap.mode, points.length);
  if (gpsMap.index >= n) gpsMap.index = 0;
  updateGpsMeta();
  if (gpsMap.miniEl && gpsMap.miniEl !== $("#cvGpsCanvas")) destroyGpsMap("mini");
  if (gpsMap.bigEl && gpsMap.bigEl !== $("#cvGpsLightboxCanvas")) destroyGpsMap("big");
  if (!points.length && !gpsMap.edit) {
    destroyGpsMaps();
    return;
  }
  ensureLeaflet().then(() => {
    if (gen !== gpsMap.gen) return;
    mountGpsMini();
    if (gpsMap.lightbox) {
      const box = $("#cvGpsLightbox");
      if (box) {
        box.hidden = false;
        box.setAttribute("aria-hidden", "false");
      }
      document.body.classList.add("is-gps-lightbox");
      mountGpsBig();
    }
  }).catch(() => {
    if (gen !== gpsMap.gen) return;
    const canvas = $("#cvGpsCanvas");
    if (canvas) {
      canvas.classList.add("is-off");
      canvas.textContent = "Carte indisponible";
    }
  });
}

function applyDndFlash() {
  const host = document.getElementById("previewDetailState");
  if (!host) return;
  [
    ["data-flash-dnd", "data-flash-dnd-token", "_dndToken"],
    ["data-flash-copy", "data-flash-copy-token", "_copyToken"],
    ["data-flash-move", "data-flash-move-token", "_moveToken"],
    ["data-flash-ark", "data-flash-ark-token", "_arkToken"],
    ["data-flash-csv", "data-flash-csv-token", "_csvToken"],
    ["data-flash-loop", "data-flash-loop-token", "_loopToken"],
    ["data-flash-del", "data-flash-del-token", "_delToken"],
    ["data-flash-facet", "data-flash-facet-token", "_facetToken"],
    ["data-flash-type", "data-flash-type-token", "_typeToken"],
    ["data-flash-ctype", "data-flash-ctype-token", "_ctypeToken"],
    ["data-flash-nt", "data-flash-nt-token", "_ntToken"]
  ].forEach(([msgAttr, tokenAttr, key]) => {
    const msg = host.getAttribute(msgAttr);
    const token = host.getAttribute(tokenAttr);
    if (!msg || !token || token === applyConceptLabelUi[key]) return;
    applyConceptLabelUi[key] = token;
    if (typeof toast === "function") toast(msg, { soft: true });
  });
}

function paintCutNode() {
  const host = document.getElementById("previewDetailState");
  const cutId = ((host && host.getAttribute("data-cut-id")) || "").trim();
  document.querySelectorAll("#panelTree .tn.is-cut").forEach((el) => el.classList.remove("is-cut"));
  if (!cutId) return;
  const escaped = window.CSS && CSS.escape ? CSS.escape(cutId) : cutId.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
  document.querySelectorAll('#panelTree .tn[data-id="' + escaped + '"]').forEach((el) => el.classList.add("is-cut"));
}

function syncSourceLangBadge(textEl, fromSource, code) {
  const line = textEl.closest(".tn-line") || textEl.parentElement;
  if (!line) return;
  let badge = null;
  for (const child of line.children) {
    if (child.classList && child.classList.contains("tn-lang")) {
      badge = child;
      break;
    }
  }
  if (!fromSource || !code) {
    if (badge) badge.remove();
    return;
  }
  if (!badge) {
    badge = document.createElement("span");
    badge.className = "tn-lang";
    textEl.insertAdjacentElement("afterend", badge);
  }
  badge.textContent = code;
}

function applyConceptLabelUi(source) {
  applyDndFlash();
  paintCutNode();
  const cv = document.querySelector("#viewLive .cv.is-on");
  if (!cv) {
    closeImgLightbox({ restoreFocus: false });
    closeGpsLightbox({ restoreFocus: false });
    return;
  }
  const flashes = [
    { msg: cv.getAttribute("data-flash"), token: cv.getAttribute("data-flash-token"), kind: "label" },
    { msg: cv.getAttribute("data-flash-coll"), token: cv.getAttribute("data-flash-coll-token"), kind: "coll" },
    { msg: cv.getAttribute("data-flash-rel"), token: cv.getAttribute("data-flash-rel-token"), kind: "rel" },
    { msg: cv.getAttribute("data-flash-tr"), token: cv.getAttribute("data-flash-tr-token"), kind: "tr" },
    { msg: cv.getAttribute("data-flash-note"), token: cv.getAttribute("data-flash-note-token"), kind: "note" },
    { msg: cv.getAttribute("data-flash-res"), token: cv.getAttribute("data-flash-res-token"), kind: "res" },
    { msg: cv.getAttribute("data-flash-align"), token: cv.getAttribute("data-flash-align-token"), kind: "align" },
    { msg: cv.getAttribute("data-flash-align-edit"), token: cv.getAttribute("data-flash-align-edit-token"), kind: "alignEdit" },
    { msg: cv.getAttribute("data-flash-cand"), token: cv.getAttribute("data-flash-cand-token"), kind: "cand" }
  ];
  flashes.forEach((item) => {
    if (!item.msg || !item.token) return;
    const seenKey = "_" + item.kind + "Token";
    if (item.token === applyConceptLabelUi[seenKey]) return;
    applyConceptLabelUi[seenKey] = item.token;
    if (typeof toast === "function") toast(item.msg, { soft: true });
    if (item.kind === "coll" && typeof invalidateCollectionTree === "function") {
      invalidateCollectionTree();
      if (typeof ensureCollectionTree === "function") ensureCollectionTree();
    }
    if (item.kind === "rel" && cv.getAttribute("data-rel-tree") === "1"
        && typeof revealTreeConcept === "function") {
      revealTreeConcept(cv.getAttribute("data-id"));
    }
  });
  const id = cv.getAttribute("data-id");
  const pref = cv.getAttribute("data-pref");
  if (id && pref && pref !== "(" + id + ")") {
    const escaped = window.CSS && CSS.escape ? CSS.escape(id) : id.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
    const fromSource = cv.getAttribute("data-pref-source") === "1";
    const lang = (cv.getAttribute("data-pref-lang") || "").trim();
    document.querySelectorAll('.tn-label[data-id="' + escaped + '"] .tn-text').forEach((el) => {
      el.textContent = pref;
      el.classList.toggle("is-source", fromSource);
      syncSourceLangBadge(el, fromSource, lang);
    });
  }
  try { bindImgGallery(); } catch (err) { /* gallery DOM already swapped by ajax */ }
  try { bindGpsMap(); } catch (err) { /* leaflet container already swapped by ajax */ }
  const editing = document.querySelector("#viewLive .cblock.is-editing");
  if (!editing) {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    playLabelBlockEnter(false);
    return;
  }
  const card = editing.getAttribute("data-card-id");
  if (card === "contexte") {
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    bindLabelFacetPicker();
    bindTermTags();
  } else if (card === "collections") {
    closeLabelFacetPicker();
    closeAllRelPickers();
    closeCrelPicker();
    bindCvCollPicker();
  } else if (card === "relations") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeCrelPicker();
    bindRelPickers();
    requestAnimationFrame(bindRelPickers);
  } else if (card === "traductions") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    seedConceptTrFromHidden();
  } else if (card === "f-tr") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    seedFacetCardTrFromHidden();
  } else if (card === "f-mem") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    seedFacetCardMemFromHidden();
  } else if (card === "f-notes") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    seedFacetCardNotesFromHidden();
  } else if (card === "notes") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    seedConceptNotesFromHidden();
  } else if (card === "ressources") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
    try { closeImgLightbox({ restoreFocus: false }); } catch (err) { /* ignore */ }
    try { closeGpsLightbox({ restoreFocus: false }); } catch (err) { /* ignore */ }
    seedConceptResFromHidden();
    requestAnimationFrame(seedConceptResFromHidden);
  } else if (card === "alignement") {
    closeLabelFacetPicker();
    closeCvCollPicker();
    closeAllRelPickers();
    closeCrelPicker();
  }
  if (card !== "ressources") {
    try { if (typeof destroyConceptResGpsMap === "function") destroyConceptResGpsMap(); } catch (err) { /* ignore */ }
    try { if (typeof closeConceptResImgLightbox === "function") closeConceptResImgLightbox(); } catch (err) { /* ignore */ }
  }
  playLabelBlockEnter(true);
  const sourceId = source && source.id ? String(source.id) : "";
  if (/gpsClickAdd|alignAutoStart|alignCompareStart|alignAutoAdd|alignDeleteSave|alignReplaceSave/.test(sourceId)) return;
  if (/trRemove|noteRemove|resRemove|imgRemove|gpsRemove/.test(sourceId)) return;
  if (editing.contains(document.activeElement)) return;
  const input = editing.querySelector("#cvRelQ")
      || editing.querySelector("#cvTrList .st-input")
      || editing.querySelector("#cvNoteBody textarea")
      || editing.querySelector("#cvResLinks .st-input")
      || editing.querySelector(".crow.is-editing .st-input")
      || editing.querySelector(".cblock-edit-row .st-input")
      || editing.querySelector(".search-input");
  if (input) input.focus({ preventScroll: true });
}

function playLabelBlockEnter(nowEditing) {
  const editingEl = document.querySelector("#viewLive .cblock.is-editing");
  const id = editingEl
      ? editingEl.getAttribute("data-card-id")
      : playLabelBlockEnter._id;
  const block = editingEl
      || (id ? document.querySelector("#viewLive .cblock[data-card-id='" + id + "']") : null);
  if (id) playLabelBlockEnter._id = id;
  if (!block) return;
  const was = playLabelBlockEnter._editing;
  playLabelBlockEnter._editing = nowEditing;
  if (typeof was !== "boolean" || was === nowEditing) return;
  const body = block.querySelector(".cblock-body");
  if (body) body.classList.add("is-enter");
  block.classList.remove("is-swap", "is-leaving");
}

function interceptLabelEditSwap(e) {
  const start = e.target.closest("[id$='labelEditStart'], [id$='collEditStart'], [id$='relEditStart'], [id$='relEditBt'], [id$='relEditNt'], [id$='relEditRt'], [id$='crelEditStart'], [id$='trEditStart'], [id$='noteEditStart'], [id$='resEditStart'], [id$='alignEditStart']");
  const cancel = e.target.closest("[id$='labelEditCancel'], [id$='collEditCancel'], [id$='relEditCancel'], [id$='crelEditCancel'], [id$='trEditCancel'], [id$='noteEditCancel'], [id$='resEditCancel'], [id$='alignAutoClose'], [id$='alignAutoCancelFoot']");
  const trigger = start || cancel;
  if (!trigger) return;
  const block = trigger.closest(".cblock")
    || document.querySelector("#viewLive .cblock[data-card-id='contexte']");
  if (!block || block.classList.contains("is-swap")) return;
  e.preventDefault();
  e.stopImmediatePropagation();
  if (block.classList.contains("is-leaving")) return;
  const leaving = block.querySelector(".cblock-body");
  block.classList.add("is-leaving");
  if (leaving) leaving.classList.add("is-leave");
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  let done = false;
  const go = () => {
    if (done) return;
    done = true;
    block.classList.remove("is-leaving");
    block.classList.add("is-swap");
    trigger.click();
  };
  if (reduce || !leaving) {
    go();
    return;
  }
  leaving.addEventListener("animationend", go, { once: true });
  setTimeout(go, 260);
}

