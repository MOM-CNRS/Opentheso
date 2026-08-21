/**
 * OpenTheso V2 — préférences : brouillon, quitter la page.
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
}

function initSettingsLeaveGuard() {
  if (SCREEN !== "preference" || !settingsForm()) return;
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
