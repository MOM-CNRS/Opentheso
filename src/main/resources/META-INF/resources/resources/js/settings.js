/**
 * OpenTheso — préférences : brouillon, quitter la page.
 */
"use strict";

let settingsDraftDirty = false;
let settingsBaseline = "";
let settingsAllowLeave = false;
var settingsLeaveAction = null;

function settingsForm() {
  return $("#previewPrefForm");
}

function serializeSettingsForm() {
  const form = settingsForm();
  if (!form) return "";
  const parts = [];
  form.querySelectorAll("input, select, textarea").forEach((el) => {
    if (!el.name) return;
    if (el.closest("#previewCorpusBox, #previewAlignBox, #stSaveConfirm, #stLeaveConfirm")) return;
    if (el.classList.contains("st-jsf-go")) return;
    if (el.type === "button" || el.type === "submit") return;
    if (el.type === "hidden" && /ViewState|csrfToken|previewCorpus|previewAlign/i.test(el.id || el.name || "")) return;
    if ((el.type === "checkbox" || el.type === "radio") && !el.checked) return;
    if (el.type === "password" && !el.value) return;
    parts.push(el.name + "=" + (el.value || "on"));
  });
  parts.sort();
  return parts.join("\n");
}

function isSettingsDirty() {
  if (SCREEN !== "preference" || !settingsForm()) return false;
  return settingsDraftDirty || serializeSettingsForm() !== settingsBaseline;
}

function markSettingsDraft() {
  settingsDraftDirty = true;
}

function snapshotPreferenceFlags() {
  if (typeof captureSettingsFieldDrafts === "function") captureSettingsFieldDrafts();
  const box = document.getElementById("previewPrefFlags");
  const root = document.getElementById("previewSettingsBody");
  if (!box || !root) return;
  const lines = [];
  root.querySelectorAll("input[data-flag]").forEach((el) => {
    if (!el || el.type !== "checkbox") return;
    lines.push(el.getAttribute("data-flag") + "=" + (el.checked ? "true" : "false"));
  });
  box.value = lines.join("\n");
}
window.snapshotPreferenceFlags = snapshotPreferenceFlags;

function rememberSettingsBaseline() {
  settingsDraftDirty = false;
  settingsBaseline = serializeSettingsForm();
}

function allowSettingsLeave() {
  settingsAllowLeave = true;
}

function isHashOnlyNavigation(url) {
  if (!url) return false;
  const raw = String(url);
  if (raw.charAt(0) === "#") return true;
  try {
    const next = new URL(raw, location.href);
    return next.origin === location.origin
      && next.pathname === location.pathname
      && next.search === location.search
      && !!next.hash;
  } catch (ex) {
    return false;
  }
}

function askLeaveThen(proceed) {
  if (settingsAllowLeave || !isSettingsDirty()) return false;
  settingsLeaveAction = proceed;
  showConfirm("#stLeaveConfirm");
  return true;
}

function dismissSettingsLeave() {
  hideConfirm("#stLeaveConfirm");
  settingsLeaveAction = null;
}

function confirmSettingsLeave() {
  hideConfirm("#stLeaveConfirm");
  const action = settingsLeaveAction;
  settingsLeaveAction = null;
  if (typeof action === "function") action();
}

window.dismissSettingsLeave = dismissSettingsLeave;
window.confirmSettingsLeave = confirmSettingsLeave;

function go(url) {
  if (askLeaveThen(() => {
    allowSettingsLeave();
    location.href = url;
  })) return;
  location.href = url;
}

function bindPrefSwitches() {
  const sync = (input) => {
    if (!input || input.type !== "checkbox") return;
    const sw = input.closest(".st-sw");
    if (!sw || sw.closest(".st-table")) return;
    sw.classList.toggle("on", input.checked);
    const integ = input.closest(".st-integ");
    if (integ) integ.classList.toggle("on", input.checked);
  };
  document.addEventListener("change", (e) => {
    const input = e.target;
    if (!input || !input.closest || !input.closest(".st-sw")) return;
    sync(input);
  });
  $$(".st-sw input[type='checkbox']").forEach(sync);
  bindLangMark();
}

function langMarkBoxes(root) {
  if (!root || root === document) return document.querySelectorAll(".st-mark");
  if (root.classList && root.classList.contains("st-mark")) return [root];
  return root.querySelectorAll(".st-mark");
}

function syncLangMark(root) {
  langMarkBoxes(root).forEach((box) => {
    const input = box.querySelector("input[data-flag='languageCodes']");
    if (!input) return;
    const codes = !!input.checked;
    box.querySelectorAll("[data-act='lang-mark']").forEach((btn) => {
      const on = (btn.getAttribute("data-codes") === "true") === codes;
      btn.classList.toggle("is-on", on);
      btn.setAttribute("aria-pressed", on ? "true" : "false");
    });
  });
}

function bindLangMark() {
  if (bindLangMark.done) {
    syncLangMark();
    return;
  }
  bindLangMark.done = true;
  document.addEventListener("click", (e) => {
    const btn = e.target.closest && e.target.closest("[data-act='lang-mark']");
    if (!btn) return;
    const box = btn.closest(".st-mark");
    const input = box && box.querySelector("input[data-flag='languageCodes']");
    if (!input) return;
    const next = btn.getAttribute("data-codes") === "true";
    if (input.checked !== next) {
      input.checked = next;
      input.dispatchEvent(new Event("change", { bubbles: true }));
    }
    syncLangMark(box);
  });
  syncLangMark();
}

const settingsOpenCards = new Set();
const arkLocalFieldIds = ["previewNaanArkLocal", "previewPrefixArkLocal", "previewSizeIdArkLocal", "previewServerOpenArk", "previewNaanOpenArk", "previewPrefixOpenArk", "previewApiKeyOpenArk", "previewPassArk"];
const arkLocalDraft = {};

function rememberSettingsFieldDraft(el) {
  if (!el || arkLocalFieldIds.indexOf(el.id) < 0) return;
  arkLocalDraft[el.id] = el.value;
}

document.addEventListener("input", (e) => rememberSettingsFieldDraft(e.target));
document.addEventListener("change", (e) => rememberSettingsFieldDraft(e.target));

function captureSettingsFieldDrafts() {
  arkLocalFieldIds.forEach((id) => rememberSettingsFieldDraft(document.getElementById(id)));
}

function restoreArkLocalDraft() {
  arkLocalFieldIds.forEach((id) => {
    if (!Object.prototype.hasOwnProperty.call(arkLocalDraft, id)) return;
    const el = document.getElementById(id);
    if (el && el.value !== arkLocalDraft[id]) el.value = arkLocalDraft[id];
  });
}

function settingsCardTitle(card) {
  const title = card && card.querySelector(".st-card-tog .bo-card-t");
  return title ? (title.textContent || "").trim() : "";
}

function applySettingsCardOpen(card, open) {
  if (!card) return;
  card.classList.toggle("is-closed", !open);
  const tog = card.querySelector(".bo-card-h .st-card-tog");
  if (tog) tog.setAttribute("aria-expanded", open ? "true" : "false");
}

function toggleSettingsCard(tog) {
  const card = tog && tog.closest(".bo-card");
  if (!card) return;
  const open = card.classList.contains("is-closed");
  applySettingsCardOpen(card, open);
  const title = settingsCardTitle(card);
  if (!title) return;
  if (open) settingsOpenCards.add(title);
  else settingsOpenCards.delete(title);
}

function settingsSectionCards(head) {
  const cards = [];
  let node = head.nextElementSibling;
  while (node && !(node.classList && node.classList.contains("st-group-head"))) {
    if (node.classList && node.classList.contains("bo-card")) cards.push(node);
    if (node.querySelectorAll) {
      node.querySelectorAll(".bo-card").forEach((card) => {
        if (cards.indexOf(card) < 0) cards.push(card);
      });
    }
    node = node.nextElementSibling;
  }
  return cards;
}

function applySettingsSectionFromHash() {
  if (SCREEN !== "preference") return false;
  const id = (location.hash || "").replace(/^#/, "");
  const head = id && document.getElementById(id);
  if (!head || !head.classList.contains("st-group-head")) return false;
  const openCards = settingsSectionCards(head);
  $$(".st-scope .bo-card").forEach((card) => {
    const open = openCards.indexOf(card) >= 0;
    applySettingsCardOpen(card, open);
    const title = settingsCardTitle(card);
    if (!title) return;
    if (open) settingsOpenCards.add(title);
    else settingsOpenCards.delete(title);
  });
  return true;
}
window.applySettingsSectionFromHash = applySettingsSectionFromHash;

function restoreSettingsCards() {
  if (SCREEN !== "preference") return;
  $$(".st-scope .bo-card .st-card-tog").forEach((tog) => {
    const card = tog.closest(".bo-card");
    applySettingsCardOpen(card, settingsOpenCards.has(settingsCardTitle(card)));
  });
  restoreArkLocalDraft();
}

window.addEventListener("hashchange", () => {
  if (typeof scrollToPrefHash === "function") scrollToPrefHash();
});

function bindSettingsCards() {
  if (SCREEN !== "preference") return;
  document.addEventListener("click", (e) => {
    const tog = e.target.closest && e.target.closest(".st-card-tog");
    if (!tog || !tog.closest(".st-scope")) return;
    e.preventDefault();
    toggleSettingsCard(tog);
  });
}

function isSettingsPageUrl(url) {
  try {
    const path = new URL(url, location.origin).pathname;
    return /\/setting\/preference(?:\.xhtml)?$/.test(path) || /\/preference(?:\.xhtml)?$/.test(path);
  } catch (ex) {
    return false;
  }
}

function rememberSettingsReturn() {
  const referrer = document.referrer || "";
  if (referrer.indexOf(location.origin) !== 0 || isSettingsPageUrl(referrer)) return;
  try {
    sessionStorage.setItem("opentheso.settingsReturn", referrer);
  } catch (ex) {
    /* navigation privée */
  }
}

function leaveSettingsPage() {
  if (typeof allowSettingsLeave === "function") allowSettingsLeave();
  let stored = "";
  try {
    stored = sessionStorage.getItem("opentheso.settingsReturn") || "";
  } catch (ex) {
    stored = "";
  }
  if (stored && stored.indexOf(location.origin) === 0 && !isSettingsPageUrl(stored)) {
    location.href = stored;
    return;
  }
  if (history.length > 1) {
    history.back();
    return;
  }
  const back = document.querySelector("#viewSettings .bo-back");
  const href = back && back.getAttribute("href");
  if (href) location.href = href;
}
window.leaveSettingsPage = leaveSettingsPage;

function initSettingsLeaveGuard() {
  if (SCREEN !== "preference" || !settingsForm()) return;
  rememberSettingsReturn();
  rememberSettingsBaseline();
  window.addEventListener("beforeunload", (e) => {
    if (settingsAllowLeave || !isSettingsDirty()) return;
    e.preventDefault();
    e.returnValue = "";
  });
  document.addEventListener("click", (e) => {
    if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
    if (e.target.closest("[data-act]")) return;
    if (e.target.closest("#previewLogoutForm a, #previewLogoutForm .bo-btn")) {
      allowSettingsLeave();
      return;
    }
    const a = e.target.closest("a[href]");
    if (!a || a.target === "_blank" || a.hasAttribute("download")) return;
    if (a.closest("#stLeaveConfirm, #stSaveConfirm, #logoutConfirm")) return;
    const href = a.getAttribute("href");
    if (!href || href.charAt(0) === "#" || /^javascript:/i.test(href)) return;
    if (isHashOnlyNavigation(a.href)) return;
    if (!askLeaveThen(() => {
      allowSettingsLeave();
      location.href = a.href;
    })) return;
    e.preventDefault();
    e.stopPropagation();
  }, true);
}
