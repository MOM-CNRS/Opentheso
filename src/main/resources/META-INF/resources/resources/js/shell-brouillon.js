/** Brouillon concept — relations, traductions et ressources. */
var DRAFT_REL_META = {
  bt: { code: "TG" },
  nt: { code: "TS" },
  rt: { code: "TA" }
};
var draftRelState = { bt: [], nt: [], rt: [], kind: "nt", seq: 0, timer: null, hits: [] };

function draftRelCsv(kind) {
  return (draftRelState[kind] || []).map((item) => item.label || item.id).filter(Boolean).join(", ");
}

function syncDraftRelHidden() {
  ["bt", "nt", "rt"].forEach((kind) => {
    const el = $("#draft" + kind.charAt(0).toUpperCase() + kind.slice(1));
    if (el) el.value = draftRelCsv(kind);
  });
  syncDraftLocation();
}

function parseDraftRelCsv(id) {
  return String(($("#" + id) && $("#" + id).value) || "")
    .split(/[,;]/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map((label) => ({ id: label, label: label }));
}

function seedDraftRelFromHidden() {
  draftRelState.bt = parseDraftRelCsv("draftBt");
  draftRelState.nt = parseDraftRelCsv("draftNt");
  draftRelState.rt = parseDraftRelCsv("draftRt");
  paintDraftRel();
  paintDraftRelKind();
  hideDraftRelDrop();
  syncDraftLocation();
}

function resetDraftRel(parentName) {
  const parent = String(parentName || "").trim();
  draftRelState.bt = parent ? [{ id: parent, label: parent }] : [];
  draftRelState.nt = [];
  draftRelState.rt = [];
  draftRelState.kind = "nt";
  draftRelState.hits = [];
  const q = $("#draftRelQ");
  if (q) q.value = "";
  hideDraftRelDrop();
  setDraftRelMsg("");
  syncDraftRelHidden();
  paintDraftRel();
  paintDraftRelKind();
}

function draftRelAlready(id, label) {
  const key = String(id || label || "").toLowerCase();
  const name = String(label || "").toLowerCase();
  return ["bt", "nt", "rt"].some((kind) => (draftRelState[kind] || []).some((item) => {
    const itemId = String(item.id || "").toLowerCase();
    const itemLabel = String(item.label || "").toLowerCase();
    return (key && (itemId === key || itemLabel === key)) || (name && itemLabel === name);
  }));
}

function paintDraftRelKind() {
  const btn = $("#draftRelKindBtn");
  const menu = $("#draftRelKindMenu");
  const kind = draftRelState.kind;
  const meta = DRAFT_REL_META[kind] || DRAFT_REL_META.nt;
  if (btn) {
    btn.className = "re-pill rel-" + kind;
    btn.innerHTML = meta.code + '<span class="re-caret" aria-hidden="true"></span>';
  }
  if (menu) {
    menu.querySelectorAll("[data-kind]").forEach((mi) => {
      mi.classList.toggle("on", mi.getAttribute("data-kind") === kind);
    });
  }
}

function setDraftRelKindMenu(open) {
  const menu = $("#draftRelKindMenu");
  const btn = $("#draftRelKindBtn");
  if (menu) menu.hidden = !open;
  if (btn) btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function setDraftRelKind(kind) {
  if (!DRAFT_REL_META[kind]) return;
  draftRelState.kind = kind;
  paintDraftRelKind();
  setDraftRelKindMenu(false);
}

function paintDraftRel() {
  const box = $("#draftRelRows");
  const editor = $("#draftRelEditor");
  if (!box) return;
  const remove = (editor && editor.getAttribute("data-remove")) || "Retirer";
  const rows = ["bt", "nt", "rt"].flatMap((k) => (draftRelState[k] || []).map((item, i) => ({ k, i, item })));
  box.innerHTML = rows.map((row) => {
    const code = (DRAFT_REL_META[row.k] || {}).code || row.k;
    const label = escapeHtml(row.item.label || row.item.id || "");
    return '<div class="re-row">'
      + '<span class="re-type"><span class="re-pill rel-' + row.k + '">' + code + "</span></span>"
      + '<span class="re-lbl" title="' + label + '">' + label + "</span>"
      + '<button type="button" class="re-x" data-act="draft-rel-remove" data-kind="' + row.k
      + '" data-index="' + row.i + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>'
      + "</div>";
  }).join("");
}

function setDraftRelMsg(text) {
  const el = $("#draftRelMsg");
  if (!el) return;
  el.textContent = text || "";
  el.hidden = !text;
}

function hideDraftRelDrop() {
  const drop = $("#draftRelDrop");
  const empty = $("#draftRelEmpty");
  if (drop) {
    drop.hidden = true;
    drop.innerHTML = "";
  }
  if (empty) empty.hidden = true;
}

function addDraftRel(id, label) {
  const kind = draftRelState.kind || "nt";
  const name = String(label || id || "").trim();
  const key = String(id || name).trim();
  if (!name) return;
  if (draftRelAlready(key, name)) {
    const editor = $("#draftRelEditor");
    setDraftRelMsg((editor && editor.getAttribute("data-exists")) || "Cette relation existe déjà.");
    return;
  }
  draftRelState[kind].push({ id: key, label: name });
  const q = $("#draftRelQ");
  if (q) q.value = "";
  draftRelState.hits = [];
  hideDraftRelDrop();
  setDraftRelMsg("");
  syncDraftRelHidden();
  paintDraftRel();
}

function removeDraftRel(kind, index) {
  if (!draftRelState[kind]) return;
  draftRelState[kind].splice(Number(index), 1);
  syncDraftRelHidden();
  paintDraftRel();
}

function searchDraftRel(q) {
  const seq = ++draftRelState.seq;
  const ctx = document.body.getAttribute("data-ctx") || "";
  const params = new URLSearchParams({
    thesaurusId: thesaurusId() || "",
    lang: thesaurusLang(),
    q: q
  });
  fetch(ctx + "/api/concepts/search?" + params.toString(), {
    headers: { Accept: "application/json" }
  }).then((res) => {
    if (!res.ok) throw new Error("http");
    return res.json();
  }).then((items) => {
    if (seq !== draftRelState.seq) return;
    draftRelState.hits = (Array.isArray(items) ? items : []).filter((item) => {
      const id = item && item.id ? String(item.id) : "";
      const label = item && item.label ? String(item.label) : "";
      return id && !draftRelAlready(id, label);
    }).slice(0, 6);
    paintDraftRelHits(q);
  }).catch(() => {
    if (seq !== draftRelState.seq) return;
    draftRelState.hits = [];
    paintDraftRelHits(q);
  });
}

function paintDraftRelHits(q) {
  const drop = $("#draftRelDrop");
  const empty = $("#draftRelEmpty");
  if (!drop) return;
  if (draftRelState.hits.length) {
    drop.hidden = false;
    if (empty) empty.hidden = true;
    drop.innerHTML = draftRelState.hits.map((hit) => (
      '<button type="button" class="re-hit" data-act="draft-rel-hit" data-id="'
      + escapeHtml(hit.id || "") + '" data-label="' + escapeHtml(hit.label || hit.id || "") + '">'
      + '<span class="re-hit-l">' + escapeHtml(hit.label || hit.id || "") + "</span>"
      + "</button>"
    )).join("");
    return;
  }
  drop.hidden = true;
  drop.innerHTML = "";
  if (empty) empty.hidden = String(q || "").trim().length < 2;
}

function scheduleDraftRelSearch() {
  const q = ($("#draftRelQ") && $("#draftRelQ").value.trim()) || "";
  setDraftRelMsg("");
  if (draftRelState.timer) clearTimeout(draftRelState.timer);
  if (q.length < 2) {
    draftRelState.seq += 1;
    draftRelState.hits = [];
    hideDraftRelDrop();
    return;
  }
  draftRelState.timer = setTimeout(() => searchDraftRel(q), 220);
}

var draftTrState = { order: [], values: {}, alts: {}, confirm: "" };

function draftTrOpt(code) {
  return $("#draftTrMenu") && $("#draftTrMenu").querySelector('.tree-lang-opt[data-lang="' + code + '"]');
}

function draftTrMeta(code) {
  const opt = draftTrOpt(code);
  return {
    code: code,
    flag: (opt && (opt.getAttribute("data-flag") || "").trim()) || "",
    name: (opt && (opt.getAttribute("data-name") || "").trim()) || code
  };
}

function closeDraftTrLang() {
  const btn = $("#draftTrBtn");
  if (!btn || !btn.classList.contains("is-open")) return false;
  btn.classList.remove("is-open");
  btn.setAttribute("aria-expanded", "false");
  return true;
}

function setDraftTrLangOpen(open) {
  const btn = $("#draftTrBtn");
  if (!btn) return;
  btn.classList.toggle("is-open", !!open);
  btn.setAttribute("aria-expanded", open ? "true" : "false");
}

function syncDraftTrHidden() {
  const el = $("#draftTrPayload");
  if (!el) return;
  el.value = draftTrState.order.map((code) => {
    return code + "\t" + String(draftTrState.values[code] || "").replace(/\r?\n/g, " ");
  }).join("\n");
}

function resetDraftTr() {
  draftTrState = { order: [], values: {}, alts: {}, confirm: "" };
  const el = $("#draftTrPayload");
  if (el) el.value = "";
  closeDraftTrLang();
  paintDraftTr();
}

function seedDraftTrFromHidden() {
  draftTrState = { order: [], values: {}, alts: {}, confirm: "" };
  const el = $("#draftTrPayload");
  String(el && el.value || "").split(/\r?\n/).forEach((line) => {
    const tab = line.indexOf("\t");
    if (tab <= 0) return;
    const code = line.slice(0, tab).trim();
    const value = line.slice(tab + 1);
    if (!code) return;
    draftTrState.order.push(code);
    draftTrState.values[code] = value;
  });
  paintDraftTr();
}

function addDraftTr(code) {
  if (!code || trLangAlready(draftTrState.order, code)) return;
  draftTrState.order.push(code);
  if (draftTrState.values[code] == null) draftTrState.values[code] = "";
  draftTrState.confirm = "";
  closeDraftTrLang();
  syncDraftTrHidden();
  paintDraftTr();
  requestAnimationFrame(() => {
    const input = $("#draftTrVal-" + code);
    if (input) input.focus();
  });
}

function removeDraftTr(code) {
  draftTrState.order = draftTrState.order.filter((item) => item !== code);
  draftTrState.values[code] = "";
  draftTrState.alts[code] = "";
  draftTrState.confirm = "";
  syncDraftTrHidden();
  paintDraftTr();
}

function setDraftTrValue(code, value) {
  draftTrState.values[code] = value;
  syncDraftTrHidden();
}

function setDraftTrAlt(code, value) {
  draftTrState.alts[code] = value;
}

function paintDraftTr() {
  const list = $("#draftTrList");
  const empty = $("#draftTrEmpty");
  const sel = $("#draftTrSel");
  const editor = $("#draftTrEditor");
  if (!list || !editor) return;
  const ph = editor.getAttribute("data-ph") || "Nom en {0}…";
  const altPh = editor.getAttribute("data-alt") || "";
  const remove = editor.getAttribute("data-remove") || "";
  const drop = editor.getAttribute("data-drop") || "";
  const no = editor.getAttribute("data-no") || "Non";
  const yes = editor.getAttribute("data-yes") || "Oui";
  if (empty) empty.hidden = draftTrState.order.length > 0;
  list.innerHTML = draftTrState.order.map((code) => {
    const meta = draftTrMeta(code);
    const name = escapeHtml(meta.name);
    const placeholder = escapeHtml(ph.replace("{0}", String(meta.name || code).toLowerCase()));
    const confirming = draftTrState.confirm === code;
    const actions = confirming
      ? '<span class="te-confirm">' + escapeHtml(drop)
        + '<button type="button" class="bo-btn ghost sm" data-act="draft-tr-keep">' + escapeHtml(no) + "</button>"
        + '<button type="button" class="bo-btn primary sm" data-act="draft-tr-drop" data-lang="' + code + '">' + escapeHtml(yes) + "</button>"
        + "</span>"
      : '<button type="button" class="re-x te-x" data-act="draft-tr-ask" data-lang="' + code
        + '" title="' + escapeHtml(remove) + '" aria-label="' + escapeHtml(remove) + '">×</button>';
    return '<div class="te-lang">'
      + '<div class="te-head">'
      + (meta.flag ? '<span class="tr-flag">' + meta.flag + "</span>" : "")
      + '<span class="te-name">' + name + "</span>"
      + '<span class="te-code">' + escapeHtml(code) + "</span>"
      + actions
      + "</div>"
      + '<input type="text" class="st-input" id="draftTrVal-' + code + '" lang="' + code + '" data-lang="' + code
      + '" value="' + escapeHtml(draftTrState.values[code] || "") + '" placeholder="' + placeholder + '" autocomplete="off"/>'
      + '<input type="text" class="st-input te-alt" lang="' + code + '" data-lang="' + code
      + '" value="' + escapeHtml(draftTrState.alts[code] || "") + '" placeholder="' + escapeHtml(altPh) + '" autocomplete="off"/>'
      + "</div>";
  }).join("");
  markUsedTrLangOpts("#draftTrMenu", draftTrState.order, editor.getAttribute("data-used") || "");
  const pick = $("#draftTrPick");
  if (pick) pick.hidden = $$("#draftTrMenu .tree-lang-opt").length === 0;
}

var draftResState = { links: [], images: [], gps: [] };

function draftResAttr(name, fallback) {
  const root = $("#draftResEditor");
  return (root && root.getAttribute(name)) || fallback || "";
}

function looksLikeUrl(value) {
  return /^https?:\/\//i.test(String(value || "").trim());
}

function resetDraftRes() {
  draftResState = { links: [], images: [], gps: [] };
  closeDraftImgLightbox();
  destroyDraftGpsMap();
  paintDraftRes();
}

function addDraftResLink() {
  draftResState.links.push({ uri: "", label: "" });
  paintDraftRes();
  requestAnimationFrame(() => {
    const input = $("#draftResUri-" + (draftResState.links.length - 1));
    if (input) input.focus();
  });
}

function addDraftResImage() {
  draftResState.images.push({ uri: "", name: "", creator: "", copyright: "" });
  paintDraftRes();
  requestAnimationFrame(() => {
    const input = $("#draftImgUri-" + (draftResState.images.length - 1));
    if (input) input.focus();
  });
}

function addDraftResGps() {
  draftResState.gps.push({ lat: "", lng: "" });
  paintDraftRes();
  requestAnimationFrame(() => {
    const input = $("#draftGpsLat-" + (draftResState.gps.length - 1));
    if (input) input.focus();
  });
}

function removeDraftResLink(index) {
  draftResState.links.splice(index, 1);
  paintDraftRes();
}

function removeDraftResImage(index) {
  draftResState.images.splice(index, 1);
  paintDraftRes();
}

function removeDraftResGps(index) {
  draftResState.gps.splice(index, 1);
  paintDraftRes();
}

function setDraftResLink(index, field, value) {
  const item = draftResState.links[index];
  if (item) item[field] = value;
}

function setDraftResImage(index, field, value) {
  const item = draftResState.images[index];
  if (!item) return;
  item[field] = value;
  if (field === "uri") {
    const preview = $("#draftImgPreview-" + index);
    const img = preview && preview.querySelector("img");
    const ok = looksLikeUrl(value);
    if (preview) preview.hidden = !ok;
    if (img && ok) img.src = String(value || "").trim();
  }
  const cap = $("#draftImgCap-" + index);
  if (cap) cap.textContent = draftImgCaption(draftResState.images[index]);
}

function setDraftResGps(index, field, value) {
  const item = draftResState.gps[index];
  if (item) item[field] = value;
  const btn = $("#draftGpsGoto-" + index);
  if (btn && (field === "lat" || field === "lng")) {
    btn.setAttribute("data-" + field, String(value || "").trim());
  }
  scheduleDraftGpsRefresh();
}

function paintDraftRes() {
  const links = $("#draftResLinks");
  const images = $("#draftResImgs");
  const gps = $("#draftGpsPts");
  if (!links || !images || !gps) return;
  const linkPh = escapeHtml(draftResAttr("data-link-ph", "https://…"));
  const linkLabel = escapeHtml(draftResAttr("data-link-label", "libellé optionnel"));
  const linkRemove = escapeHtml(draftResAttr("data-link-remove", ""));
  const imgPh = escapeHtml(draftResAttr("data-img-ph", "https://…"));
  const imgName = escapeHtml(draftResAttr("data-img-name", "nom"));
  const imgCreator = escapeHtml(draftResAttr("data-img-creator", "auteur"));
  const imgCopy = escapeHtml(draftResAttr("data-img-copy", "copyright"));
  const imgRemove = escapeHtml(draftResAttr("data-img-remove", ""));
  const gpsLat = escapeHtml(draftResAttr("data-gps-lat", "latitude"));
  const gpsLng = escapeHtml(draftResAttr("data-gps-lng", "longitude"));
  const gpsRemove = escapeHtml(draftResAttr("data-gps-remove", ""));
  links.innerHTML = draftResState.links.map((item, index) => {
    return '<div class="media-edit-card media-edit-link">'
      + '<div class="media-edit-val">'
      + '<input type="text" class="st-input media-edit-uri" id="draftResUri-' + index + '" data-res="link" data-field="uri" data-index="' + index
      + '" value="' + escapeHtml(item.uri || "") + '" placeholder="' + linkPh + '" autocomplete="off"/>'
      + '<button type="button" class="note-edit-remove" data-act="draft-res-remove" data-index="' + index
      + '" title="' + linkRemove + '" aria-label="' + linkRemove + '">×</button>'
      + "</div>"
      + '<input type="text" class="st-input" data-res="link" data-field="label" data-index="' + index
      + '" value="' + escapeHtml(item.label || "") + '" placeholder="' + linkLabel + '" autocomplete="off"/>'
      + "</div>";
  }).join("");
  const imgOpen = escapeHtml(draftResAttr("data-img-open", "Agrandir l'image"));
  images.innerHTML = draftResState.images.map((item, index) => {
    const src = String(item.uri || "").trim();
    const show = looksLikeUrl(src);
    const cap = escapeHtml(draftImgCaption(item));
    return '<div class="media-edit-card media-edit-img is-preview">'
      + '<button type="button" class="draft-img-preview" id="draftImgPreview-' + index + '" data-act="draft-img-open" data-index="' + index
      + '" title="' + imgOpen + '" aria-label="' + imgOpen + '"' + (show ? "" : " hidden=\"hidden\"") + ">"
      + '<img src="' + escapeHtml(show ? src : "") + '" alt="" onerror="this.parentNode.hidden=true"/>'
      + "</button>"
      + (cap ? '<div class="draft-img-cap" id="draftImgCap-' + index + '">' + cap + "</div>"
        : '<div class="draft-img-cap" id="draftImgCap-' + index + '"></div>')
      + '<div class="media-edit-img-fields">'
      + '<div class="media-edit-val">'
      + '<input type="text" class="st-input media-edit-uri" id="draftImgUri-' + index + '" data-res="image" data-field="uri" data-index="' + index
      + '" value="' + escapeHtml(item.uri || "") + '" placeholder="' + imgPh + '" autocomplete="off"/>'
      + '<button type="button" class="note-edit-remove" data-act="draft-img-remove" data-index="' + index
      + '" title="' + imgRemove + '" aria-label="' + imgRemove + '">×</button>'
      + "</div>"
      + '<div class="media-edit-img-meta">'
      + '<input type="text" class="st-input" data-res="image" data-field="name" data-index="' + index
      + '" value="' + escapeHtml(item.name || "") + '" placeholder="' + imgName + '" autocomplete="off"/>'
      + '<input type="text" class="st-input" data-res="image" data-field="creator" data-index="' + index
      + '" value="' + escapeHtml(item.creator || "") + '" placeholder="' + imgCreator + '" autocomplete="off"/>'
      + '<input type="text" class="st-input" data-res="image" data-field="copyright" data-index="' + index
      + '" value="' + escapeHtml(item.copyright || "") + '" placeholder="' + imgCopy + '" autocomplete="off"/>'
      + "</div></div></div>";
  }).join("");
  gps.innerHTML = draftResState.gps.map((item, index) => {
    return '<li class="gps-edit-row">'
      + '<button type="button" class="gps-pt" id="draftGpsGoto-' + index + '" data-act="draft-gps-goto" data-index="' + index
      + '" data-lat="' + escapeHtml(item.lat || "") + '" data-lng="' + escapeHtml(item.lng || "") + '">'
      + '<span class="gps-pt-n" aria-hidden="true">' + (index + 1) + "</span></button>"
      + '<input type="text" class="st-input is-mono gps-edit-coord" id="draftGpsLat-' + index + '" data-res="gps" data-field="lat" data-index="' + index
      + '" value="' + escapeHtml(item.lat || "") + '" placeholder="' + gpsLat + '" autocomplete="off"/>'
      + '<input type="text" class="st-input is-mono gps-edit-coord" id="draftGpsLng-' + index + '" data-res="gps" data-field="lng" data-index="' + index
      + '" value="' + escapeHtml(item.lng || "") + '" placeholder="' + gpsLng + '" autocomplete="off"/>'
      + '<button type="button" class="note-edit-remove" data-act="draft-gps-remove" data-index="' + index
      + '" title="' + gpsRemove + '" aria-label="' + gpsRemove + '">×</button>'
      + "</li>";
  }).join("");
  scheduleDraftGpsRefresh();
}

function draftImgCaption(item) {
  return imgGalleryCaption(item);
}

function draftVisibleImages() {
  return draftResState.images
    .map((item, index) => ({ item: item, index: index }))
    .filter((row) => looksLikeUrl(row.item.uri));
}

var draftImgView = { index: 0 };

function closeDraftImgLightbox() {
  const box = $("#draftImgLightbox");
  if (!box || box.hidden) return false;
  box.hidden = true;
  box.setAttribute("aria-hidden", "true");
  document.body.classList.remove("is-img-lightbox");
  return true;
}

function showDraftImgAt(index) {
  const items = draftVisibleImages();
  if (!items.length) return;
  const n = items.length;
  draftImgView.index = ((index % n) + n) % n;
  const row = items[draftImgView.index];
  const src = String(row.item.uri || "").trim();
  const img = $("#draftImgLightboxImg");
  if (img) {
    img.src = src;
    img.alt = row.item.name || "";
  }
  const cap = $("#draftImgLightboxCap");
  if (cap) cap.textContent = draftImgCaption(row.item);
  const orig = $("#draftImgLightboxOrig");
  if (orig) orig.href = src || "#";
  const box = $("#draftImgLightbox");
  if (box) box.classList.toggle("is-single", n < 2);
}

function openDraftImgLightbox(cardIndex) {
  const items = draftVisibleImages();
  if (!items.length) return;
  const pos = items.findIndex((row) => row.index === Number(cardIndex));
  draftImgView.index = pos >= 0 ? pos : 0;
  const box = $("#draftImgLightbox");
  if (!box) return;
  document.body.classList.add("is-img-lightbox");
  box.hidden = false;
  box.setAttribute("aria-hidden", "false");
  showDraftImgAt(draftImgView.index);
}

var draftGpsView = { map: null, layer: null, el: null, ignoreClick: false };

function draftGpsPoints() {
  return draftResState.gps.map((item, index) => ({
    index: index,
    lat: parseFloat(String(item.lat || "").replace(",", ".")),
    lng: parseFloat(String(item.lng || "").replace(",", "."))
  })).filter((pt) => Number.isFinite(pt.lat) && Number.isFinite(pt.lng));
}

function destroyDraftGpsMap() {
  if (draftGpsView.layer && draftGpsView.map) {
    draftGpsView.map.removeLayer(draftGpsView.layer);
  }
  if (draftGpsView.map) {
    draftGpsView.map.remove();
  }
  draftGpsView.map = null;
  draftGpsView.layer = null;
  draftGpsView.el = null;
}

function ensureDraftGpsMap() {
  const canvas = $("#draftGpsCanvas");
  if (!canvas || !window.L) return null;
  if (draftGpsView.map && draftGpsView.el === canvas) return draftGpsView.map;
  destroyDraftGpsMap();
  canvas.classList.remove("is-off");
  canvas.textContent = "";
  draftGpsView.el = canvas;
  draftGpsView.map = L.map(canvas, {
    scrollWheelZoom: false,
    doubleClickZoom: false,
    attributionControl: true
  });
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "© OpenStreetMap"
  }).addTo(draftGpsView.map);
  draftGpsView.map.setView([46.7, 2.5], 5);
  draftGpsView.map.on("dragstart", () => { draftGpsView.ignoreClick = true; });
  draftGpsView.map.on("dragend", () => {
    window.setTimeout(() => { draftGpsView.ignoreClick = false; }, 220);
  });
  draftGpsView.map.on("click", (e) => {
    if (draftGpsView.ignoreClick) return;
    const lat = formatGpsNum(e.latlng.lat);
    const lng = formatGpsNum(e.latlng.lng);
    const last = draftResState.gps[draftResState.gps.length - 1];
    if (last && !String(last.lat || "").trim() && !String(last.lng || "").trim()) {
      last.lat = lat;
      last.lng = lng;
    } else {
      draftResState.gps.push({ lat: lat, lng: lng });
    }
    paintDraftRes();
  });
  return draftGpsView.map;
}

function refreshDraftGpsMap() {
  const badge = $("#draftGpsBadge");
  const points = draftGpsPoints();
  if (badge) {
    badge.textContent = points.length
      ? (points.length + " point" + (points.length > 1 ? "s" : ""))
      : (draftResAttr("data-gps-empty", "Cliquez pour poser un point"));
  }
  const canvas = $("#draftGpsCanvas");
  if (!canvas || ($("#draftMore") && $("#draftMore").hidden)) return;
  ensureLeaflet().then(() => {
    const map = ensureDraftGpsMap();
    if (!map) return;
    if (draftGpsView.layer) {
      map.removeLayer(draftGpsView.layer);
      draftGpsView.layer = null;
    }
    const layer = L.layerGroup().addTo(map);
    draftGpsView.layer = layer;
    const accent = gpsAccent();
    if (points.length >= 2) {
      L.polyline(points.map((pt) => [pt.lat, pt.lng]), { color: accent, weight: 2.5 }).addTo(layer);
    }
    points.forEach((pt, i) => {
      const marker = L.marker([pt.lat, pt.lng], {
        draggable: true,
        icon: L.divIcon({
          className: "gps-pin",
          html: '<span class="gps-pin-n">' + (pt.index + 1) + "</span>",
          iconSize: [22, 22],
          iconAnchor: [11, 11]
        })
      }).addTo(layer);
      marker.on("click", () => gotoDraftGps(pt.index));
      marker.on("dragstart", () => { draftGpsView.ignoreClick = true; });
      marker.on("dragend", (ev) => {
        const next = ev.target.getLatLng();
        const row = draftResState.gps[pt.index];
        if (row) {
          row.lat = formatGpsNum(next.lat);
          row.lng = formatGpsNum(next.lng);
        }
        const latIn = $("#draftGpsLat-" + pt.index);
        const lngIn = $("#draftGpsLng-" + pt.index);
        if (latIn) latIn.value = formatGpsNum(next.lat);
        if (lngIn) lngIn.value = formatGpsNum(next.lng);
        window.setTimeout(() => { draftGpsView.ignoreClick = false; }, 220);
        scheduleDraftGpsRefresh();
      });
    });
    if (!points.length) map.setView([46.7, 2.5], 5);
    else if (points.length === 1) map.setView([points[0].lat, points[0].lng], 14);
    else map.fitBounds(L.latLngBounds(points.map((pt) => [pt.lat, pt.lng])).pad(0.14));
    requestAnimationFrame(() => map.invalidateSize());
    setTimeout(() => map.invalidateSize(), 260);
  }).catch(() => {
    if (canvas) {
      canvas.classList.add("is-off");
      canvas.textContent = "Carte indisponible";
    }
  });
}

function scheduleDraftGpsRefresh() {
  clearTimeout(scheduleDraftGpsRefresh._t);
  scheduleDraftGpsRefresh._t = setTimeout(refreshDraftGpsMap, 80);
}

function gotoDraftGps(index) {
  const item = draftResState.gps[index];
  if (!item || !draftGpsView.map) return;
  const lat = parseFloat(String(item.lat || "").replace(",", "."));
  const lng = parseFloat(String(item.lng || "").replace(",", "."));
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
  draftGpsView.map.panTo([lat, lng]);
}

