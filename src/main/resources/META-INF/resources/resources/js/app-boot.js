/**
 * OpenTheso — démarrage, navigation et membres.
 */
"use strict";

var params = new URLSearchParams(location.search);

function pageParam(name) {
  const fromQuery = params.get(name);
  if (fromQuery) return fromQuery;
  if (typeof currentConceptParam === "function") return currentConceptParam(name) || "";
  return "";
}

if (SCREEN === "atelier") {
  setBatch(params.get("obj") || "alignements", params.get("op"));
}
if (SCREEN === "synchronisation") {
  syncThesaurusPollTick();
}
if (SCREEN === "portail") {
  portalPublishPollTick();
}
if ((IS_CONSULT || SCREEN === "accueil") && pageParam("create") === "concept") {
  createConceptDraft({
    getAttribute: (name) => {
      if (name === "data-pref") return pageParam("pref") || "";
      if (name === "data-id") return pageParam("idc") || "";
      return "";
    }
  });
}
if ((IS_CONSULT || SCREEN === "accueil") && pageParam("create") === "facet") {
  createFacetDraft({
    getAttribute: (name) => {
      if (name === "data-pref") return pageParam("pref") || "";
      if (name === "data-id") return pageParam("idc") || "";
      return "";
    }
  });
}
if (SCREEN === "candidats" && (params.get("new") === "1" || params.get("pref") || params.get("path"))) {
  createCandidate({
    getAttribute: (name) => {
      if (name === "data-pref") return params.get("pref") || "";
      if (name === "data-path") return params.get("path") || "";
      return "";
    }
  });
}
if (IS_CONSULT || SCREEN === "accueil") {
  const q = pageParam("q");
  const id = pageParam("id") || pageParam("idc");
  const view = pageParam("view");
  if (q) {
    if (input) input.value = q;
    if (clear) clear.hidden = !q;
    runSearch();
  } else if (typeof liveDetailRequested === "function" && liveDetailRequested()) {
    if (typeof showLiveDetail === "function") showLiveDetail();
    paint();
  } else if (id) {
    if (!openLiveDetail(id, pageParam("type") || "")) {
      openConcept(id);
    }
  } else if (IS_CONSULT && view) {
    setView(view);
  } else {
    paint();
  }
} else {
  paint();
}
bindPrefSwitches();
bindSettingsCards();
bindViewRail();
loadStatKpis();
initSettingsLeaveGuard();
requestAnimationFrame(() => {
  syncAboutFold();
  markAboutVisualEmpty();
  maybeRememberAboutBaseline();
  refreshAboutFmtState();
  syncCandSearchClear();
  applyPropBoardFilter();
  if (window.syncViewRail) window.syncViewRail();
  scrollToPrefHash();
  if (SCREEN === "preference") rememberSettingsBaseline();
});
if (SCREEN === "preference" && location.hash) {
  window.addEventListener("load", scrollToPrefHash);
  setTimeout(scrollToPrefHash, 80);
  setTimeout(scrollToPrefHash, 250);
}

/* ── Transitions subtiles : rail admin + bouton retour ── */
(function () {
  const LEAVE_MS = 220;

  function reducedMotion() {
    return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  }

  function leaveRoot() {
    return document.getElementById("iaRoot")
      || document.getElementById("tpRoot")
      || document.getElementById("previewView")
      || document.querySelector(".main");
  }

  /**
   * Anime une sortie légère puis poursuit la navigation (lien ou 2e clic JSF).
   * opts.href : navigation URL après l'animation
   */
  window.iaAnimateLeave = function (el, opts) {
    opts = opts || {};
    if (el && el.getAttribute("data-ia-leaving") === "1") {
      return true;
    }
    if (el) {
      el.classList.add("is-leaving");
      el.setAttribute("data-ia-leaving", "1");
    }
    const root = leaveRoot();
    if (root) root.classList.add("ia--leaving");
    document.body.classList.add("ia-nav-leaving");

    const delay = reducedMotion() ? 0 : LEAVE_MS;
    window.setTimeout(function () {
      document.body.classList.remove("ia-nav-leaving");
      if (opts.href) {
        window.location.href = opts.href;
        return;
      }
      if (!el) return;
      if (typeof el.click === "function") {
        el.click();
      }
    }, delay);
    return false;
  };

  window.iaAnimateRailToAdmin = function (el, evt) {
    if (evt) {
      evt.preventDefault();
      evt.stopPropagation();
    }
    if (!el) return false;
    if (el.getAttribute("data-ia-leaving") === "1") {
      return true;
    }
    el.classList.add("is-nav-pulse", "is-leaving");
    return window.iaAnimateLeave(el, { href: el.href || el.getAttribute("href") });
  };

  window.iaOnBackAjax = function (data) {
    if (!data) return;
    const root = document.getElementById("iaRoot");
    const clearLeaving = function () {
      document.body.classList.remove("ia-nav-leaving");
      document.querySelectorAll(".ia--leaving").forEach(function (el) {
        el.classList.remove("ia--leaving");
      });
    };
    if (data.status === "error") {
      clearLeaving();
      return;
    }
    if (data.status === "success") {
      clearLeaving();
      if (!root) return;
      root.classList.remove("ia--enter");
      void root.offsetWidth;
      root.classList.add("ia--enter");
    }
  };

  window.iaMemberCreateMode = function (btn) {
    const box = btn && btn.closest ? btn.closest(".ia-member-mode") : null;
    if (!box) return false;
    const mode = btn.getAttribute("data-mode") || "EMAIL";
    const input = box.querySelector(".ia-member-mode-value");
    if (input) input.value = mode;
    box.querySelectorAll(".ia-mode-opt").forEach(function (el) {
      el.classList.toggle("is-on", el.getAttribute("data-mode") === mode);
    });
    const email = mode === "EMAIL";
    const pane = box.closest(".ia-member-add") || document;
    pane.querySelectorAll(".ia-member-password").forEach(function (el) {
      el.classList.toggle("is-off", email);
    });
    const hint = box.querySelector(".ia-member-mode-hint");
    if (hint) {
      hint.textContent = email ? (hint.getAttribute("data-email") || "") : (hint.getAttribute("data-direct") || "");
    }
    return false;
  };

  window.iaFilterMoveProjects = function (input) {
    const box = input && input.closest ? input.closest(".ia-move-proj") : null;
    if (!box) return;
    const list = box.querySelector(".ia-move-list");
    const none = box.querySelector(".ia-move-proj-none");
    if (!list) return;
    const fold = function (text) {
      return (text || "").toString().normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
    };
    const needle = fold(input.value);
    let shown = 0;
    list.querySelectorAll(".ia-move-hit").forEach(function (opt) {
      const visible = !needle || fold(opt.textContent).indexOf(needle) >= 0;
      opt.hidden = !visible;
      if (visible) shown += 1;
    });
    if (none) none.hidden = needle === "" || shown !== 0;
  };

  window.iaMarkMoveProject = function (el) {
    const list = el && el.closest ? el.closest(".ia-move-list") : null;
    if (!list || !el) return true;
    const wasOn = el.classList.contains("is-on");
    list.querySelectorAll(".ia-move-hit.is-on").forEach(function (hit) {
      hit.classList.remove("is-on");
    });
    if (!wasOn) el.classList.add("is-on");
    return true;
  };

  window.iaMoveProjectAjax = function (data) {
    if (!data) return;
    const list = document.querySelector(".ia-move .ia-move-list");
    if (data.status === "begin" && list) {
      window._iaMoveListScroll = list.scrollTop;
      return;
    }
    if (data.status !== "success") return;
    const input = document.querySelector(".ia-move .ia-move-proj-q");
    if (input) window.iaFilterMoveProjects(input);
    const fresh = document.querySelector(".ia-move .ia-move-list");
    if (fresh && typeof window._iaMoveListScroll === "number") {
      fresh.scrollTop = window._iaMoveListScroll;
    }
  };

  document.addEventListener("DOMContentLoaded", function () {
    if (document.body && (document.body.getAttribute("data-page") === "admin-instance"
        || document.body.getAttribute("data-page") === "projets")) {
      if (document.body.getAttribute("data-page") === "admin-instance") {
        const adminBtn = document.querySelector('.thesaurus a.thesaurus-btn[title="Administration de l\'instance"]');
        if (adminBtn) adminBtn.classList.add("is-on");
      }
      iaConsumeFlash();
    }
  });

  function iaConsumeFlash() {
    if (typeof window.toast !== "function") return;
    ["iaFlashLive", "mpFlashLive"].forEach(function (id) {
      const live = document.getElementById(id);
      if (!live) return;
      const msg = live.getAttribute("data-ia-flash");
      const token = live.getAttribute("data-ia-flash-token");
      const error = live.getAttribute("data-ia-flash-error") === "true";
      if (!msg || !token) return;
      if (window._iaFlashToken === token) return;
      window._iaFlashToken = token;
      window.toast(msg, error ? { error: true } : undefined);
    });
  }
  window.iaConsumeFlash = iaConsumeFlash;

  function iaUsersListBusy(on) {
    const toolbar = document.getElementById("iaUsersToolbar");
    const table = document.getElementById("iaUsersTable");
    clearTimeout(window._iaUsersBusyTimer);
    [toolbar, table].forEach(function (el) {
      if (!el) return;
      el.classList.toggle("is-busy", !!on);
      if (on) el.setAttribute("aria-busy", "true");
      else el.removeAttribute("aria-busy");
    });
    if (on) {
      window._iaUsersBusyTimer = setTimeout(function () {
        iaUsersListBusy(false);
        window._iaUsersFilterBusy = false;
      }, 8000);
    }
  }

  window.iaMemberCreateAjax = function (data) {
    if (!data || data.status === "begin") return;
    const go = document.getElementById("iaMemberCreateGo");
    if (go) go.removeAttribute("data-ia-busy");
    document.querySelectorAll('[data-ia-proxy="iaMemberCreateGo"]').forEach(function (visible) {
      visible.disabled = false;
      visible.removeAttribute("aria-busy");
    });
  };

  window.iaClickProxy = function (id) {
    const go = document.getElementById(id);
    if (!go || go.getAttribute("data-ia-busy") === "1") return false;
    go.setAttribute("data-ia-busy", "1");
    document.querySelectorAll('[data-ia-proxy="' + id + '"]').forEach(function (visible) {
      visible.disabled = true;
      visible.setAttribute("aria-busy", "true");
    });
    // Déclencher le handler Mojarra du bouton (onclick généré) : un
    // faces.ajax.request manuel sans event 'action' n'invoque pas l'action.
    if (typeof go.click === "function") {
      go.click();
    }
    return false;
  };

  window.iaFocusNewProject = function (data) {
    if (!data || data.status !== "success") return;
    const input = document.getElementById("iaNewProjectName");
    if (!input) return;
    input.focus();
    if (typeof input.select === "function") {
      try { input.select(); } catch (ex) {}
    }
  };

  window.iaNewProjectKeydown = function (event) {
    if (!event || event.key !== "Enter") return true;
    event.preventDefault();
    const go = document.getElementById("iaNewProjectSubmit");
    if (go && typeof go.click === "function") go.click();
    return false;
  };

  window.iaQueuePick = function (el, event) {
    if (event && event.preventDefault) event.preventDefault();
    if (!el) return false;
    const idInput = document.getElementById(el.getAttribute("data-ia-id-field"));
    const nameInput = document.getElementById(el.getAttribute("data-ia-name-field"));
    const emailInput = document.getElementById(el.getAttribute("data-ia-email-field") || "");
    const queryInput = document.getElementById(el.getAttribute("data-ia-query-field") || "");
    const go = document.getElementById(el.getAttribute("data-ia-go"));
    if (!idInput || !go) return false;
    const username = el.getAttribute("data-ia-username") || "";
    idInput.value = el.getAttribute("data-ia-user-id") || "";
    if (nameInput) nameInput.value = username;
    if (emailInput) emailInput.value = el.getAttribute("data-ia-email") || "";
    if (queryInput) queryInput.value = username;
    go.click();
    return false;
  };

  window.iaQueueMemberAction = function (el) {
    if (!el) return false;
    const action = el.getAttribute("data-ia-action");
    const userId = el.getAttribute("data-ia-user-id");
    const actionInput = document.getElementById("iaPendingMemberAction");
    const idInput = document.getElementById("iaPendingMemberUserId");
    const roleInput = document.getElementById("iaPendingMemberRoleId");
    const nameInput = document.getElementById("iaPendingMemberUsername");
    const wideInput = document.getElementById("iaPendingMemberProjectWide");
    const go = document.getElementById("iaMemberRowActionGo");
    if (!action || !userId || !actionInput || !idInput || !go) return false;
    actionInput.value = action;
    idInput.value = String(userId);
    if (roleInput) roleInput.value = el.getAttribute("data-ia-role-id") || "";
    if (nameInput) nameInput.value = el.getAttribute("data-ia-username") || "";
    if (wideInput) wideInput.value = el.getAttribute("data-ia-project-wide") || "false";
    go.click();
    return false;
  };

  window.iaQueueUserAction = function (el) {
    if (!el || window._iaUsersFilterBusy) return false;
    const action = el.getAttribute("data-ia-action");
    const userId = el.getAttribute("data-ia-user-id");
    const username = el.getAttribute("data-ia-username") || "";
    const actionInput = document.getElementById("iaPendingUserAction");
    const idInput = document.getElementById("iaPendingActionUserId");
    const nameInput = document.getElementById("iaPendingActionUsername");
    const go = document.getElementById("iaUserActionGo");
    if (!action || !userId || !actionInput || !idInput || !go) return false;
    actionInput.value = action;
    idInput.value = String(userId);
    if (nameInput) nameInput.value = username;
    go.click();
    return false;
  };

  window.iaUsersListAjaxGate = function (data) {
    if (!data) return;
    const src = data.source;
    const srcId = (src && (src.id || (src.getAttribute && src.getAttribute("id")))) || "";
    if (data.status === "begin") {
      window._iaUsersFilterBusy = true;
      iaUsersListBusy(true);
      if (srcId.indexOf("iaUserQuery") >= 0) {
        const input = document.getElementById("iaUserQuery");
        if (input && typeof input.selectionStart === "number") {
          window._iaUsersQueryCaret = input.selectionStart;
        }
      }
      return;
    }
    if (data.status === "complete" || data.status === "error") {
      window._iaUsersFilterBusy = false;
      iaUsersListBusy(false);
      if (srcId.indexOf("iaUserQuery") >= 0) {
        const input = document.getElementById("iaUserQuery");
        if (input) {
          input.focus();
          const caret = window._iaUsersQueryCaret;
          if (typeof caret === "number" && input.setSelectionRange) {
            try { input.setSelectionRange(caret, caret); } catch (ex) {}
          }
        }
      }
    }
  };

  function iaOnAjaxFlash(data) {
    if (!data || data.status !== "success") return;
    iaConsumeFlash();
  }

  function iaBindFlash() {
    if (window._iaFlashBound) return;
    const api = window.faces && faces.ajax && typeof faces.ajax.addOnEvent === "function"
      ? faces.ajax
      : window.jsf && jsf.ajax && typeof jsf.ajax.addOnEvent === "function"
        ? jsf.ajax
        : null;
    if (!api) return;
    api.addOnEvent(iaOnAjaxFlash);
    window._iaFlashBound = true;
  }
  iaBindFlash();
  document.addEventListener("DOMContentLoaded", iaBindFlash);

  function mpUserMenu() {
    return document.getElementById("mpUserMenu");
  }

  function mpUserRows() {
    const menu = mpUserMenu();
    if (!menu) return [];
    return [...menu.querySelectorAll(".ac-row.is-shown")];
  }

  function mpUserOpen(open) {
    const menu = mpUserMenu();
    const pick = document.getElementById("mpUserPick");
    const combo = document.getElementById("mpUserCombo");
    const input = document.getElementById("mpExistingUserQuery");
    if (menu) menu.hidden = !open;
    if (pick) pick.classList.toggle("is-open", !!open);
    if (combo) combo.classList.toggle("is-open", !!open);
    if (input) input.setAttribute("aria-expanded", open ? "true" : "false");
  }

  function mpUserSetActive(index) {
    const rows = mpUserRows();
    const next = rows.length ? Math.max(0, Math.min(index, rows.length - 1)) : -1;
    rows.forEach((row, i) => row.classList.toggle("is-active", i === next));
    if (next >= 0 && rows[next]) rows[next].scrollIntoView({ block: "nearest" });
    return next;
  }

  window.mpUserAcAjax = function (data) {
    if (!data || data.status !== "success") return;
    const menu = mpUserMenu();
    const input = document.getElementById("mpExistingUserQuery");
    const clear = document.getElementById("mpUserClear");
    if (!menu) return;
    const show = !menu.hidden && (menu.classList.contains("has-hits") || menu.classList.contains("is-empty"));
    mpUserOpen(show);
    if (clear && input) clear.hidden = !input.value;
    if (show) mpUserSetActive(0);
  };

  document.addEventListener("input", function (e) {
    if (!e.target || e.target.id !== "mpExistingUserQuery") return;
    const clear = document.getElementById("mpUserClear");
    if (clear) clear.hidden = !e.target.value;
    clearTimeout(window._mpUserSearchTimer);
    window._mpUserSearchTimer = setTimeout(function () {
      const go = document.getElementById("mpExistingUserSearchGo");
      if (go) go.click();
    }, 200);
  });

  document.addEventListener("click", function (e) {
    const pick = document.getElementById("mpUserPick");
    if (!pick) return;
    if (e.target.closest && e.target.closest("#mpUserClear")) {
      e.preventDefault();
      const go = document.getElementById("mpExistingUserClearGo");
      if (go) go.click();
      return;
    }
    if (e.target.closest && e.target.closest("#mpUserCaret")) {
      e.preventDefault();
      const menu = mpUserMenu();
      const open = pick.classList.contains("is-open");
      if (open) {
        mpUserOpen(false);
        return;
      }
      if (menu && (menu.classList.contains("has-hits") || menu.classList.contains("is-empty"))) {
        mpUserOpen(true);
        mpUserSetActive(0);
      }
      const input = document.getElementById("mpExistingUserQuery");
      if (input) input.focus();
      return;
    }
    if (!pick.contains(e.target)) mpUserOpen(false);
  });

  document.addEventListener("keydown", function (e) {
    if (!e.target || e.target.id !== "mpExistingUserQuery") return;
    const pick = document.getElementById("mpUserPick");
    const open = !!(pick && pick.classList.contains("is-open"));
    const rows = open ? mpUserRows() : [];
    if (e.key === "ArrowDown") {
      if (!rows.length) return;
      e.preventDefault();
      const current = rows.findIndex((row) => row.classList.contains("is-active"));
      mpUserSetActive(current < 0 ? 0 : current + 1);
    } else if (e.key === "ArrowUp") {
      if (!rows.length) return;
      e.preventDefault();
      const current = rows.findIndex((row) => row.classList.contains("is-active"));
      mpUserSetActive(current < 0 ? 0 : current - 1);
    } else if (e.key === "Enter") {
      if (!rows.length) return;
      e.preventDefault();
      e.stopPropagation();
      const row = rows.find((item) => item.classList.contains("is-active")) || rows[0];
      if (window.iaQueuePick) window.iaQueuePick(row, e);
    } else if (e.key === "Escape" && open) {
      e.preventDefault();
      mpUserOpen(false);
    }
  });
})();
