/**
 * OpenTheso — recherche de projet (création / édition d'un utilisateur).
 */
"use strict";

function iaProjAcSelect(root) {
  return root ? root.querySelector("select.ia-proj-ac-value") : null;
}

function iaProjAcOptions(root) {
  const select = iaProjAcSelect(root);
  if (!select) return [];
  return [...select.options].filter((opt) => opt.value && opt.value !== "0");
}

function iaProjAcOpen(root, open) {
  if (!root) return;
  const menu = root.querySelector(".ia-proj-ac-menu");
  const field = root.querySelector(".ia-proj-ac-field");
  const input = root.querySelector(".ia-proj-ac-q");
  if (!menu) return;
  if (open) {
    $$(".ia-proj-ac.is-open").forEach((other) => {
      if (other !== root) iaProjAcOpen(other, false);
    });
    iaProjAcPaint(root);
  }
  menu.hidden = !open;
  root.classList.toggle("is-open", !!open);
  if (field) field.classList.toggle("is-open", !!open);
  if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
}

function iaProjAcPaint(root) {
  const select = iaProjAcSelect(root);
  const menu = root.querySelector(".ia-proj-ac-menu");
  const list = root.querySelector(".coll-ac-list");
  const empty = root.querySelector(".ac-empty");
  const input = root.querySelector(".ia-proj-ac-q");
  const clear = root.querySelector(".ia-proj-ac-clear");
  if (!select || !menu || !list || !input) return;
  const query = input.value.trim();
  const folded = typeof norm === "function" ? norm(query) : query.toLowerCase();
  const hits = iaProjAcOptions(root).filter((opt) => {
    if (!folded) return true;
    const label = typeof norm === "function" ? norm(opt.text) : opt.text.toLowerCase();
    return label.includes(folded);
  });
  const selected = select.value;
  list.innerHTML = hits.map((opt) => {
    const on = opt.value === selected;
    return '<button type="button" class="ac-row is-shown' + (on ? " is-picked" : "")
      + '" role="option" aria-selected="' + (on ? "true" : "false")
      + '" data-id="' + escapeHtml(opt.value) + '">'
      + '<span class="ac-body"><span class="ac-pref">' + escapeHtml(opt.text) + "</span></span></button>";
  }).join("");
  if (query && typeof wrapQueryIn === "function") {
    $$(".ac-pref", list).forEach((el) => wrapQueryIn(el, query));
  }
  menu.classList.toggle("has-hits", hits.length > 0);
  menu.classList.toggle("is-empty", hits.length === 0);
  if (empty) {
    const tpl = hits.length === 0
      ? (query ? (root.getAttribute("data-empty") || "") : (root.getAttribute("data-none") || ""))
      : "";
    empty.textContent = tpl.replace("{0}", query);
  }
  if (clear) clear.hidden = !input.value;
  $$(".ac-row", list).forEach((row) => row.classList.remove("is-active"));
}

function iaProjAcCommit(root, id, fire) {
  const select = iaProjAcSelect(root);
  if (!select || select.value === id) return;
  select.value = id;
  if (fire) select.dispatchEvent(new Event("change", { bubbles: true }));
}

function iaProjAcPick(root, id) {
  const select = iaProjAcSelect(root);
  const input = root.querySelector(".ia-proj-ac-q");
  if (!select) return;
  const opt = iaProjAcOptions(root).find((item) => item.value === id);
  if (!opt) return;
  if (input) input.value = opt.text;
  iaProjAcOpen(root, false);
  iaProjAcCommit(root, id, true);
  const clear = root.querySelector(".ia-proj-ac-clear");
  if (clear && input) clear.hidden = !input.value;
}

function iaProjAcClear(root) {
  const input = root.querySelector(".ia-proj-ac-q");
  if (input) input.value = "";
  iaProjAcCommit(root, "0", true);
  iaProjAcOpen(root, false);
  const clear = root.querySelector(".ia-proj-ac-clear");
  if (clear) clear.hidden = true;
  if (input) input.focus();
}

function iaProjAcSyncLabel(root) {
  const select = iaProjAcSelect(root);
  const input = root.querySelector(".ia-proj-ac-q");
  if (!select || !input || input.value) return;
  const opt = iaProjAcOptions(root).find((item) => item.value === select.value);
  if (!opt) return;
  input.value = opt.text;
  const clear = root.querySelector(".ia-proj-ac-clear");
  if (clear) clear.hidden = false;
}

function iaProjAcRefresh() {
  $$(".ia-proj-ac").forEach(iaProjAcSyncLabel);
}

function iaProjAcRows(root) {
  return root ? $$(".ac-row.is-shown", root) : [];
}

function iaProjAcMove(root, dir) {
  const rows = iaProjAcRows(root);
  if (!rows.length) return;
  const current = rows.findIndex((row) => row.classList.contains("is-active"));
  const next = current < 0 ? (dir > 0 ? 0 : rows.length - 1) : (current + dir + rows.length) % rows.length;
  rows.forEach((row, i) => row.classList.toggle("is-active", i === next));
  rows[next].scrollIntoView({ block: "nearest" });
}

document.addEventListener("focusin", (e) => {
  const input = e.target.closest && e.target.closest(".ia-proj-ac-q");
  if (!input) return;
  const root = input.closest(".ia-proj-ac");
  iaProjAcSyncLabel(root);
  iaProjAcOpen(root, true);
});

document.addEventListener("input", (e) => {
  const input = e.target.closest && e.target.closest(".ia-proj-ac-q");
  if (!input) return;
  const root = input.closest(".ia-proj-ac");
  const select = iaProjAcSelect(root);
  const current = select ? iaProjAcOptions(root).find((opt) => opt.value === select.value) : null;
  const typed = typeof norm === "function" ? norm(input.value.trim()) : input.value.trim().toLowerCase();
  const label = current ? (typeof norm === "function" ? norm(current.text) : current.text.toLowerCase()) : "";
  if (!current || typed !== label) iaProjAcCommit(root, "0", true);
  iaProjAcOpen(root, true);
});

document.addEventListener("keydown", (e) => {
  const input = e.target.closest && e.target.closest(".ia-proj-ac-q");
  if (!input) return;
  const root = input.closest(".ia-proj-ac");
  const open = root.classList.contains("is-open");
  if (e.key === "ArrowDown") {
    e.preventDefault();
    if (!open) iaProjAcOpen(root, true);
    iaProjAcMove(root, 1);
  } else if (e.key === "ArrowUp") {
    e.preventDefault();
    if (!open) iaProjAcOpen(root, true);
    iaProjAcMove(root, -1);
  } else if (e.key === "Enter") {
    e.preventDefault();
    const row = iaProjAcRows(root).find((item) => item.classList.contains("is-active"));
    if (row) iaProjAcPick(root, row.getAttribute("data-id"));
  } else if (e.key === "Escape" && open) {
    e.preventDefault();
    iaProjAcOpen(root, false);
  }
});

document.addEventListener("mousedown", (e) => {
  const row = e.target.closest && e.target.closest(".ia-proj-ac .ac-row");
  if (!row) return;
  e.preventDefault();
  iaProjAcPick(row.closest(".ia-proj-ac"), row.getAttribute("data-id"));
});

document.addEventListener("click", (e) => {
  $$(".ia-proj-ac.is-open").forEach((root) => {
    if (!root.contains(e.target)) iaProjAcOpen(root, false);
  });
  const clear = e.target.closest && e.target.closest(".ia-proj-ac-clear");
  if (clear) {
    e.preventDefault();
    iaProjAcClear(clear.closest(".ia-proj-ac"));
    return;
  }
  const caret = e.target.closest && e.target.closest(".ia-proj-ac-caret");
  if (!caret) return;
  e.preventDefault();
  const root = caret.closest(".ia-proj-ac");
  const input = root.querySelector(".ia-proj-ac-q");
  const willOpen = !root.classList.contains("is-open");
  iaProjAcOpen(root, willOpen);
  if (willOpen && input) input.focus();
});

function iaProjAcBindAjax() {
  if (window._iaProjAcBound) return;
  const api = window.faces && faces.ajax && typeof faces.ajax.addOnEvent === "function"
    ? faces.ajax
    : window.jsf && jsf.ajax && typeof jsf.ajax.addOnEvent === "function"
      ? jsf.ajax
      : null;
  if (!api) return;
  api.addOnEvent((data) => {
    if (data && data.status === "success") iaProjAcRefresh();
  });
  window._iaProjAcBound = true;
}

iaProjAcBindAjax();
document.addEventListener("DOMContentLoaded", () => {
  iaProjAcBindAjax();
  iaProjAcRefresh();
});
