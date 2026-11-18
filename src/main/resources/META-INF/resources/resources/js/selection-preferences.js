/** Sélection — préférences de colonnes et de statuts. */
/**
 * OpenTheso — sélection, barre d'actions, déplacement.
 */
"use strict";

function persistTableCols() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  const selected = TABLE_COL_ALL.filter((s) => state.tblCols.has(s));
  const payload = { selected: selected };
  try {
    document.body.setAttribute("data-table-cols", JSON.stringify(payload));
  } catch (ex) {}
  const ctx = document.body.getAttribute("data-ctx") || "";
  clearTimeout(persistTableCols._t);
  persistTableCols._t = setTimeout(() => {
    fetch(ctx + "/api/account/table-cols", {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload)
    }).catch(() => {});
  }, 180);
}

function resetTableColPref() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  state.tblCols = new Set(TABLE_COL_DEFAULT);
  applyTableCols();
  persistTableCols();
  const root = $("#accTableCols");
  const msg = root && root.getAttribute("data-msg-reset-done");
  if (msg && typeof toast === "function") toast(msg);
}

function persistTreeStatus() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  const selected = TREE_STATUS_ALL.filter((s) => state.statusSet.has(s));
  const payload = { selected: selected };
  try {
    document.body.setAttribute("data-tree-status", JSON.stringify(payload));
  } catch (ex) {}
  const ctx = document.body.getAttribute("data-ctx") || "";
  clearTimeout(persistTreeStatus._t);
  persistTreeStatus._t = setTimeout(() => {
    fetch(ctx + "/api/account/tree-status", {
      method: "PUT",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload)
    }).catch(() => {});
  }, 180);
}

function flushUserPreferences() {
  if (document.body.getAttribute("data-logged-in") !== "1") {
    return Promise.reject(new Error("auth"));
  }
  clearTimeout(persistTableCols._t);
  clearTimeout(persistTreeStatus._t);
  if (typeof persistCardLayout === "function") clearTimeout(persistCardLayout._t);
  const ctx = document.body.getAttribute("data-ctx") || "";
  const headers = { "Content-Type": "application/json", Accept: "application/json" };
  const calls = [
    ["data-tree-status", "/api/account/tree-status"],
    ["data-table-cols", "/api/account/table-cols"],
    ["data-card-layout", "/api/account/concept-blocks"]
  ];
  return Promise.all(calls.map(([attr, path]) => {
    const body = document.body.getAttribute(attr);
    if (!body) return Promise.resolve();
    return fetch(ctx + path, {
      method: "PUT",
      credentials: "same-origin",
      headers: headers,
      body: body
    }).then((res) => {
      if (!res.ok) throw new Error(String(res.status));
    });
  }));
}

function showPrefSaveToast(ok) {
  const root = $("#prefSaveToast");
  if (!ok) {
    const fail = (root && root.getAttribute("data-msg-error")) || "L'enregistrement a échoué. Réessayez.";
    if (typeof toast === "function") toast(fail);
    return;
  }
  if (!root) return;
  const txt = $("#prefSaveToastTxt");
  const message = root.getAttribute("data-msg-ok");
  if (txt && message) txt.textContent = message;
  root.hidden = false;
  clearTimeout(root._t);
  root._t = setTimeout(() => { root.hidden = true; }, 2200);
}

function resetTreeStatusPref() {
  if (document.body.getAttribute("data-logged-in") !== "1") return;
  state.statusSet = new Set(TREE_STATUS_DEFAULT);
  syncStatusUi();
  persistTreeStatus();
  const root = $("#accTreeStatus");
  const msg = root && root.getAttribute("data-msg-reset-done");
  if (msg && typeof toast === "function") toast(msg);
}

function syncStatusUi() {
  Object.keys(GROUPS).forEach(key => {
    const list = GROUPS[key];
    const nOn = list.filter(s => state.statusSet.has(s)).length;
    const gs = nOn === 0 ? "off" : (nOn === list.length ? "on" : "mixed");
    $$(`.stk-group[data-group="${key}"]`).forEach((group) => {
      const head = group.querySelector(".stk-ghead");
      const input = group.querySelector(".stk-ghead input");
      const box = group.querySelector(".stk-ghead .stk-box");
      if (head) head.classList.toggle("on", gs !== "off");
      group.classList.toggle("on", gs !== "off");
      if (input) {
        input.checked = gs === "on";
        input.indeterminate = gs === "mixed";
      }
      if (box) {
        box.classList.toggle("mixed", gs === "mixed");
        box.textContent = gs === "on" ? "✓" : gs === "mixed" ? "–" : "";
      }
      list.forEach(s => {
        const lab = group.querySelector(`.stk-item[data-status="${s}"]`);
        if (!lab) return;
        const on = state.statusSet.has(s);
        const item = lab.querySelector("input");
        if (item) item.checked = on;
        lab.classList.toggle("on", on);
        const ib = lab.querySelector(".stk-box");
        if (ib) ib.textContent = on ? "✓" : "";
      });
    });
  });
  syncCandFilterUi();
  applyStatusFilter();
}

function syncCandFilterUi() {
  const active = !!(state.candBy || state.candFrom || state.candTo);
  const clear = $("#cfClear");
  if (clear) clear.hidden = !active;
}

function currentUsername() {
  return document.body.getAttribute("data-username") || "";
}

function cfMsg(attr, fallback) {
  const combo = $("#cfCombo");
  return (combo && combo.getAttribute(attr)) || fallback;
}

function candByDisplayLabel(by) {
  if (!by) return cfMsg("data-everyone", "Tout le monde");
  if (by === currentUsername()) {
    return cfMsg("data-me", "Moi ({0})").replace("{0}", by);
  }
  return by;
}

function setCandBySelection(by) {
  state.candBy = by || "";
  const lab = $("#cfByLabel");
  if (lab) lab.textContent = candByDisplayLabel(state.candBy);
  $$("#cfByList .cf-opt").forEach((o) => {
    o.classList.toggle("on", (o.getAttribute("data-by") || "") === state.candBy);
  });
  syncCandFilterUi();
}

function candByOptHtml(by, label, hint, on) {
  return '<button type="button" class="cf-opt' + (on ? " on" : "") + '" data-act="cf-by" data-by="'
    + escapeHtml(by) + '" data-label="' + escapeHtml(label) + '">'
    + escapeHtml(label)
    + (hint ? "<small>" + escapeHtml(hint) + "</small>" : "")
    + "</button>";
}

function renderCandByOptions(users, opts) {
  const list = $("#cfByList");
  if (!list) return;
  const pending = !!(opts && opts.pending);
  const everyone = cfMsg("data-everyone", "Tout le monde");
  const everyoneHint = cfMsg("data-everyone-hint", "tous les candidats");
  const meTpl = cfMsg("data-me", "Moi ({0})");
  const meHint = cfMsg("data-me-hint", "mes candidats");
  const empty = cfMsg("data-empty", "Aucun utilisateur");
  const me = currentUsername();
  const selected = state.candBy || "";
  let html = candByOptHtml("", everyone, everyoneHint, selected === "");
  if (me) {
    html += candByOptHtml(me, meTpl.replace("{0}", me), me + " · " + meHint, selected === me);
  }
  const rows = (users || []).filter((u) => u && u.username && u.username !== me);
  if (selected && selected !== me && !rows.some((u) => u.username === selected)) {
    rows.unshift({ username: selected });
  }
  if (!pending && !rows.length) {
    html += '<div class="cf-empty">' + escapeHtml(empty) + "</div>";
  } else {
    rows.forEach((u) => {
      html += candByOptHtml(u.username, u.username, "", selected === u.username);
    });
  }
  list.innerHTML = html;
}

function loadCandByUsers(q) {
  const list = $("#cfByList");
  if (!list) return Promise.resolve();
  const ctx = document.body.getAttribute("data-ctx") || "";
  const query = q ? String(q).trim() : "";
  const url = ctx + "/api/users" + (query ? "?q=" + encodeURIComponent(query) : "");
  return fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } })
    .then((r) => (r.ok ? r.json() : []))
    .then((users) => {
      renderCandByOptions(Array.isArray(users) ? users : []);
    })
    .catch(() => {
      renderCandByOptions([]);
    });
}

function resetCandBySearch() {
  const input = $("#cfByQuery");
  if (input) input.value = "";
  loadCandByUsers("");
}

