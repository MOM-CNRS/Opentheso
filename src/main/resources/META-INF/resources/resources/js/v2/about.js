/**
 * OpenTheso V2 — éditeur À propos, corpus, alignements.
 */
"use strict";

function aboutComposer() {
  return $(".abt-composer");
}
function aboutVisual() {
  return $("#aboutVisual");
}
function aboutTextarea() {
  const composer = aboutComposer();
  return composer ? composer.querySelector("textarea.abt-editor") : null;
}
function syncAboutEditor() {
  const composer = aboutComposer();
  const visual = aboutVisual();
  const ta = aboutTextarea();
  if (!composer || !ta || composer.classList.contains("is-source")) return;
  if (visual) ta.value = visual.innerHTML;
}
function markAboutVisualEmpty() {
  const visual = aboutVisual();
  if (!visual) return;
  visual.classList.toggle("is-empty", visual.textContent.trim() === "");
}
function applyAboutFormat(cmd, val) {
  const visual = aboutVisual();
  if (!visual || !cmd) return;
  const composer = aboutComposer();
  if (composer && composer.classList.contains("is-source")) return;
  visual.focus();
  if (cmd === "createLink") {
    const url = window.prompt("Adresse du lien :", "https://");
    if (!url) return;
    document.execCommand("createLink", false, url);
  } else if (cmd === "formatBlock") {
    const tag = val || "p";
    if (!document.execCommand("formatBlock", false, tag)) {
      document.execCommand("formatBlock", false, "<" + tag + ">");
    }
  } else {
    document.execCommand(cmd, false, val || null);
  }
  syncAboutEditor();
  markAboutVisualEmpty();
  refreshAboutSaveState();
  refreshAboutFmtState();
}
function refreshAboutFmtState() {
  const composer = aboutComposer();
  if (!composer || composer.classList.contains("is-source")) return;
  composer.querySelectorAll(".abt-fmt-btn[data-cmd]").forEach((btn) => {
    const cmd = btn.getAttribute("data-cmd");
    let on = false;
    try {
      if (cmd === "formatBlock") {
        const want = (btn.getAttribute("data-val") || "").replace(/[<>]/g, "").toUpperCase();
        const cur = (document.queryCommandValue("formatBlock") || "").replace(/[<>]/g, "").toUpperCase();
        on = !!want && cur === want;
      } else if (cmd === "createLink" || cmd === "unlink" || cmd === "undo" || cmd === "redo"
          || cmd === "indent" || cmd === "outdent" || cmd === "removeFormat" || cmd === "insertHorizontalRule") {
        on = false;
      } else {
        on = document.queryCommandState(cmd);
      }
    } catch (err) {
      on = false;
    }
    btn.classList.toggle("is-on", on);
  });
}
function toggleAboutSource() {
  const composer = aboutComposer();
  const visual = aboutVisual();
  const ta = aboutTextarea();
  if (!composer || !ta) return;
  const on = !composer.classList.contains("is-source");
  if (on) {
    if (visual) ta.value = visual.innerHTML;
    composer.classList.add("is-source");
    ta.focus();
  } else {
    if (visual) visual.innerHTML = ta.value;
    composer.classList.remove("is-source");
    markAboutVisualEmpty();
    if (visual) visual.focus();
  }
  const btn = composer.querySelector("[data-act='about-src']");
  if (btn) btn.setAttribute("aria-pressed", String(on));
  refreshAboutSaveState();
}
function currentAboutHtml() {
  const composer = aboutComposer();
  const ta = aboutTextarea();
  const visual = aboutVisual();
  if (!composer) return "";
  if (composer.classList.contains("is-source") && ta) return ta.value;
  if (visual) return visual.innerHTML;
  return ta ? ta.value : "";
}
let aboutBaseline = null;
let aboutHadComposer = false;
function rememberAboutBaseline() {
  aboutBaseline = currentAboutHtml();
  refreshAboutSaveState();
}
function maybeRememberAboutBaseline() {
  const has = !!aboutComposer();
  const swapped = has !== aboutHadComposer;
  if (has && !aboutHadComposer) rememberAboutBaseline();
  if (!has) aboutBaseline = null;
  aboutHadComposer = has;
  refreshAboutSaveState();
  if (swapped) playAboutEnter();
}
function playAboutEnter() {
  const about = document.querySelector(".th-about");
  if (!about) return;
  const panel = about.querySelector(".abt-composer")
    || about.querySelector(".abt-fold")
    || about.querySelector(".abt-empty");
  if (panel) panel.classList.add("is-enter");
}
function interceptAboutSwap(e) {
  const edit = e.target.closest(".abt-edit-btn");
  const cancel = e.target.closest(".abt-actions .abt-cancel");
  const trigger = edit || cancel;
  if (!trigger) return;
  const about = trigger.closest(".th-about") || document.querySelector(".th-about");
  if (!about) return;
  if (about.classList.contains("is-swap")) return;
  e.preventDefault();
  e.stopImmediatePropagation();
  if (about.classList.contains("is-leaving")) return;
  const leaving = cancel
    ? about.querySelector(".abt-composer")
    : (about.querySelector(".abt-fold") || about.querySelector(".abt-empty"));
  about.classList.add("is-leaving");
  if (leaving) leaving.classList.add("is-leave");
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  let done = false;
  const go = () => {
    if (done) return;
    done = true;
    about.classList.remove("is-leaving");
    about.classList.add("is-swap");
    trigger.click();
  };
  if (reduce || !leaving) {
    go();
    return;
  }
  leaving.addEventListener("animationend", go, { once: true });
  setTimeout(go, 280);
}
function refreshAboutSaveState() {
  const btn = $("#aboutSaveBtn");
  if (!btn) return;
  const dirty = currentAboutHtml() !== aboutBaseline;
  btn.classList.toggle("is-off", !dirty);
  btn.setAttribute("aria-disabled", String(!dirty));
  if (!dirty) btn.setAttribute("tabindex", "-1");
  else btn.removeAttribute("tabindex");
}
window.onPreviewAboutSave = function (data) {
  const btns = document.querySelectorAll(".abt-save");
  if (data.status === "begin") {
    syncAboutEditor();
    hideConfirm("#aboutSaveConfirm");
    btns.forEach((btn) => btn.classList.add("is-busy"));
  }
  if (data.status === "success" || data.status === "complete") {
    btns.forEach((btn) => btn.classList.remove("is-busy", "is-click"));
  }
  if (data.status === "success") {
    requestAnimationFrame(showAboutSaveToast);
  }
};
window.onPreviewCorpusDialog = function (data) {
  const src = data.source;
  const cancel = src && /previewCorpusCancel/.test(src.id || "");
  const creating = src && src.id === "previewCorpusCreateOk";
  if (data.status === "begin") {
    if (src) src.classList.add("is-click");
    if (creating) hideConfirm("#previewCorpusCreateConfirm");
    if (cancel) {
      hideConfirm("#previewCorpusCreateConfirm");
      const overlay = document.getElementById("previewCorpusOverlay");
      if (overlay) overlay.classList.add("is-off");
      const del = document.getElementById("previewCorpusDeleteConfirm");
      if (del) del.classList.add("is-off");
    }
    return;
  }
  if (src) src.classList.remove("is-click");
  if (data.status !== "success") return;
  hideConfirm("#previewCorpusCreateConfirm");
  const overlay = document.getElementById("previewCorpusOverlay");
  if (overlay) {
    const formPane = overlay.querySelector("#previewCorpusFields");
    const formOpen = formPane && !formPane.classList.contains("is-off");
    overlay.classList.toggle("is-off", !formOpen);
    const first = overlay.querySelector("#previewCorpusFields:not(.is-off) .st-input");
    if (first && formOpen) requestAnimationFrame(() => first.focus());
  }
  const saved = src && /previewCorpus(CreateOk|Apply|Delete)$/.test(src.id || "");
  if (!saved) return;
  const msg = $("#previewCorpusSaveMsg");
  if (!msg) return;
  const text = (msg.textContent || "").trim();
  if (!text || msg.getAttribute("data-ok") !== "true") return;
  markSettingsDraft();
  const el = $("#previewCorpusToast");
  const txt = $("#previewCorpusToastTxt");
  if (!el) return;
  if (txt) txt.textContent = text;
  el.hidden = false;
  clearTimeout(el._t);
  el._t = setTimeout(() => { el.hidden = true; }, 2200);
};
window.onPreviewAlignDialog = function (data) {
  const src = data.source;
  const cancel = src && /previewAlignCancel/.test(src.id || "");
  if (data.status === "begin") {
    if (src) src.classList.add("is-click");
    if (src && src.id === "previewAlignCreate") src.classList.add("is-busy");
    if (cancel) {
      const overlay = document.getElementById("previewAlignOverlay");
      if (overlay) overlay.classList.add("is-off");
      const del = document.getElementById("previewAlignDeleteConfirm");
      if (del) del.classList.add("is-off");
    }
    return;
  }
  if (src) src.classList.remove("is-click", "is-busy");
  if (data.status !== "success") return;
  const overlay = document.getElementById("previewAlignOverlay");
  if (overlay) {
    const formPane = overlay.querySelector("#previewAlignFields");
    const formOpen = formPane && !formPane.classList.contains("is-off");
    overlay.classList.toggle("is-off", !formOpen);
    const first = overlay.querySelector("#previewAlignFields:not(.is-off) .st-input");
    if (first && formOpen) requestAnimationFrame(() => first.focus());
  }
  const saved = src && /previewAlign(Create|Apply|Delete)$/.test(src.id || "");
  if (!saved) return;
  const msg = $("#previewAlignSaveMsg");
  if (!msg) return;
  const text = (msg.textContent || "").trim();
  if (!text || msg.getAttribute("data-ok") !== "true") return;
  markSettingsDraft();
  const el = $("#previewAlignToast");
  const txt = $("#previewAlignToastTxt");
  if (!el) return;
  if (txt) txt.textContent = text;
  el.hidden = false;
  clearTimeout(el._t);
  el._t = setTimeout(() => { el.hidden = true; }, 2200);
};
window.onPreviewPrefSave = function (data) {
  if (data.status === "begin") {
    hideConfirm("#stSaveConfirm");
  }
  if (data.status === "error") {
    toast("L'enregistrement a échoué. Réessayez.");
    return;
  }
  if (data.status !== "success") return;
  const msg = $("#previewPrefSaveMsg");
  if (!msg) return;
  const text = (msg.textContent || "").trim();
  if (!text) {
    toast("L'enregistrement n'a pas pu être effectué.");
    return;
  }
  if (msg.getAttribute("data-ok") === "true") {
    requestAnimationFrame(rememberSettingsBaseline);
    const el = $("#stSaveToast");
    if (!el) return;
    el.hidden = false;
    clearTimeout(el._t);
    el._t = setTimeout(() => { el.hidden = true; }, 2200);
    return;
  }
  toast(text);
};
function showAboutSaveToast() {
  const msg = $("#previewAboutSaveMsg");
  if (!msg) return;
  const text = (msg.textContent || "").trim();
  if (!text || msg.getAttribute("data-ok") !== "true") return;
  toast(text, { soft: true });
}
function syncAboutFold() {
  const fold = $("#aboutFold");
  if (!fold) return;
  const html = fold.querySelector(".abt-html");
  const btn = fold.querySelector(".abt-toggle");
  if (!html) return;
  const wasOpen = fold.classList.contains("is-open");
  fold.classList.remove("is-open", "is-expandable", "is-short");
  if (btn) {
    btn.classList.remove("open");
    btn.setAttribute("aria-expanded", "false");
  }
  fold.classList.add("is-measuring");
  void html.offsetHeight;
  const overflowing = html.scrollHeight > html.clientHeight + 4;
  fold.classList.remove("is-measuring");
  fold.classList.toggle("is-expandable", overflowing);
  fold.classList.toggle("is-short", !overflowing);
  if (wasOpen && overflowing) {
    fold.classList.add("is-open");
    if (btn) {
      btn.classList.add("open");
      btn.setAttribute("aria-expanded", "true");
    }
  }
}
