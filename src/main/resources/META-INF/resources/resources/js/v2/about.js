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
let aboutHistory = [];
let aboutHistoryIdx = -1;

function aboutPushHistory() {
  const visual = aboutVisual();
  if (!visual) return;
  const html = visual.innerHTML;
  aboutHistory = aboutHistory.slice(0, aboutHistoryIdx + 1);
  aboutHistory.push(html);
  aboutHistoryIdx = aboutHistory.length - 1;
  if (aboutHistory.length > 50) {
    aboutHistory.shift();
    aboutHistoryIdx -= 1;
  }
}
function aboutRestoreHistory(nextIdx) {
  const visual = aboutVisual();
  if (!visual || nextIdx < 0 || nextIdx >= aboutHistory.length) return;
  aboutHistoryIdx = nextIdx;
  visual.innerHTML = aboutHistory[aboutHistoryIdx];
}
function aboutSelectionNode() {
  const visual = aboutVisual();
  const sel = window.getSelection();
  if (!visual || !sel || !sel.rangeCount) return null;
  let node = sel.anchorNode;
  if (node && node.nodeType === 3) node = node.parentElement;
  if (!node || !visual.contains(node)) return null;
  return node;
}
function aboutClosest(tagNames) {
  const node = aboutSelectionNode();
  const visual = aboutVisual();
  if (!node || !visual) return null;
  const found = node.closest(tagNames);
  return found && visual.contains(found) ? found : null;
}
function aboutWrapInline(tag) {
  const visual = aboutVisual();
  const sel = window.getSelection();
  if (!visual || !sel || !sel.rangeCount) return;
  const existing = aboutClosest(tag + ", " + (tag === "strong" ? "b" : tag === "em" ? "i" : tag));
  if (existing) {
    const parent = existing.parentNode;
    while (existing.firstChild) parent.insertBefore(existing.firstChild, existing);
    parent.removeChild(existing);
    parent.normalize();
    return;
  }
  const range = sel.getRangeAt(0);
  if (range.collapsed) {
    const el = document.createElement(tag);
    el.appendChild(document.createTextNode("\u200b"));
    range.insertNode(el);
    const r = document.createRange();
    r.selectNodeContents(el);
    sel.removeAllRanges();
    sel.addRange(r);
    return;
  }
  const el = document.createElement(tag);
  try {
    range.surroundContents(el);
  } catch (err) {
    el.appendChild(range.extractContents());
    range.insertNode(el);
  }
}
function aboutFormatBlock(tag) {
  const visual = aboutVisual();
  const sel = window.getSelection();
  if (!visual || !sel || !sel.rangeCount) return;
  const node = aboutSelectionNode();
  if (!node) return;
  const block = node.closest("p,h1,h2,h3,h4,blockquote,div,li");
  const next = document.createElement(tag);
  if (block && visual.contains(block) && block !== visual) {
    next.innerHTML = block.innerHTML;
    block.replaceWith(next);
  } else {
    next.innerHTML = visual.innerHTML;
    visual.innerHTML = "";
    visual.appendChild(next);
  }
}
function aboutToggleList(ordered) {
  const visual = aboutVisual();
  if (!visual) return;
  const tag = ordered ? "ol" : "ul";
  const list = aboutClosest("ul,ol");
  if (list) {
    const frag = document.createDocumentFragment();
    Array.from(list.children).forEach((li) => {
      const p = document.createElement("p");
      p.innerHTML = li.innerHTML;
      frag.appendChild(p);
    });
    list.replaceWith(frag);
    return;
  }
  const block = aboutClosest("p,h2,h3,blockquote,div");
  const source = block && block !== visual ? block : visual;
  const listEl = document.createElement(tag);
  const li = document.createElement("li");
  li.innerHTML = source === visual ? visual.innerHTML : source.innerHTML;
  listEl.appendChild(li);
  if (source === visual) {
    visual.innerHTML = "";
    visual.appendChild(listEl);
  } else {
    source.replaceWith(listEl);
  }
}
function aboutIndent(outdent) {
  const listItem = aboutClosest("li");
  if (!listItem) return;
  if (outdent) {
    const list = listItem.parentElement;
    if (!list || (list.tagName !== "UL" && list.tagName !== "OL")) return;
    const parentLi = list.parentElement && list.parentElement.closest("li");
    if (parentLi && parentLi.parentElement) {
      parentLi.parentElement.insertBefore(listItem, parentLi.nextSibling);
      if (!list.children.length) list.remove();
    }
    return;
  }
  const prev = listItem.previousElementSibling;
  if (!prev) return;
  let nested = prev.querySelector(":scope > ul, :scope > ol");
  if (!nested) {
    nested = document.createElement(listItem.parentElement.tagName.toLowerCase());
    prev.appendChild(nested);
  }
  nested.appendChild(listItem);
}
function applyAboutFormat(cmd, val) {
  const visual = aboutVisual();
  if (!visual || !cmd) return;
  const composer = aboutComposer();
  if (composer && composer.classList.contains("is-source")) return;
  visual.focus();
  if (aboutHistoryIdx < 0) aboutPushHistory();
  if (cmd === "undo") {
    aboutRestoreHistory(aboutHistoryIdx - 1);
  } else if (cmd === "redo") {
    aboutRestoreHistory(aboutHistoryIdx + 1);
  } else {
    if (cmd === "createLink") {
      const promptMsg = (document.body && document.body.getAttribute("data-msg-link")) || "Adresse du lien :";
      const url = window.prompt(promptMsg, "https://");
      if (!url) return;
      aboutWrapInline("a");
      const link = aboutClosest("a");
      if (link) link.setAttribute("href", url);
    } else if (cmd === "unlink") {
      const link = aboutClosest("a");
      if (link) {
        const parent = link.parentNode;
        while (link.firstChild) parent.insertBefore(link.firstChild, link);
        parent.removeChild(link);
      }
    } else if (cmd === "formatBlock") {
      aboutFormatBlock((val || "p").replace(/[<>]/g, ""));
    } else if (cmd === "insertUnorderedList") {
      aboutToggleList(false);
    } else if (cmd === "insertOrderedList") {
      aboutToggleList(true);
    } else if (cmd === "indent") {
      aboutIndent(false);
    } else if (cmd === "outdent") {
      aboutIndent(true);
    } else if (cmd === "insertHorizontalRule") {
      const sel = window.getSelection();
      if (sel && sel.rangeCount) sel.getRangeAt(0).insertNode(document.createElement("hr"));
    } else if (cmd === "removeFormat") {
      const sel = window.getSelection();
      if (sel && !sel.isCollapsed && sel.rangeCount) {
        const text = sel.toString();
        sel.deleteContents();
        sel.getRangeAt(0).insertNode(document.createTextNode(text));
      }
    } else if (cmd === "bold") {
      aboutWrapInline("strong");
    } else if (cmd === "italic") {
      aboutWrapInline("em");
    } else if (cmd === "underline") {
      aboutWrapInline("u");
    } else if (cmd === "strikeThrough") {
      aboutWrapInline("s");
    }
    aboutPushHistory();
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
    if (cmd === "formatBlock") {
      const want = (btn.getAttribute("data-val") || "").replace(/[<>]/g, "").toUpperCase();
      const block = aboutClosest("p,h2,h3,blockquote,div");
      on = !!want && !!block && block.tagName === want;
    } else if (cmd === "bold") {
      on = !!aboutClosest("strong,b");
    } else if (cmd === "italic") {
      on = !!aboutClosest("em,i");
    } else if (cmd === "underline") {
      on = !!aboutClosest("u");
    } else if (cmd === "strikeThrough") {
      on = !!aboutClosest("s,strike");
    } else if (cmd === "insertUnorderedList") {
      on = !!aboutClosest("ul");
    } else if (cmd === "insertOrderedList") {
      on = !!aboutClosest("ol");
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
    requestAnimationFrame(() => {
      showAboutSaveToast();
      const edit = document.querySelector(".abt-edit-btn");
      if (edit) edit.focus();
    });
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
window.onPreviewAccountSave = function (data) {
  if (data.status === "error") {
    toast("L'enregistrement a échoué. Réessayez.");
    return;
  }
  if (data.status !== "success") return;
  const msg = $("#previewAccountSaveMsg");
  if (!msg) return;
  const text = (msg.textContent || "").trim();
  if (!text) {
    toast("L'enregistrement n'a pas pu être effectué.");
    return;
  }
  if (msg.getAttribute("data-ok") === "true") {
    const el = $("#accountSaveToast");
    const txt = $("#accountSaveToastTxt");
    if (txt) txt.textContent = text;
    if (!el) {
      toast(text);
      return;
    }
    el.hidden = false;
    clearTimeout(el._t);
    el._t = setTimeout(() => { el.hidden = true; }, 2200);
    return;
  }
  toast(text);
};
window.onPreviewUiLang = function (data) {
  if (data.status === "error") {
    toast("Le changement de langue a échoué.");
    return;
  }
  if (data.status === "success") {
    window.location.reload();
  }
};
window.onPreviewTermLang = function (data) {
  if (data.status === "error") {
    toast("Le changement de langue du thésaurus a échoué.");
    return;
  }
  if (data.status === "success") {
    onThesaurusLangChanged();
  }
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
