/** Tableau de bord d'instance — branchement des actions. */
(function () {
  const { scopedThesaurusId, closeDrawer, closeFs, closeThesaurusPicker, downloadCsv, fetchJson, fillThesauri, hasThesaurus, loadOverview, loadQuality, mountFsButtons, openThesaurusPicker, paintThesaurusList, renderSearch, root, setThesaurus, state, syncSearchTabs, thesaurusOpen, toggleFs, toggleThesaurusPicker } = window.IaDashParts;
function bind() {
  const el = root();
  if (!el || el.getAttribute("data-bound") === "1") return;
  el.setAttribute("data-bound", "1");
  el.querySelectorAll(".ia-dash-chip[data-days]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.days = Number(btn.getAttribute("data-days")) || 30;
      el.querySelectorAll(".ia-dash-chip[data-days]").forEach((b) => b.classList.toggle("is-on", b === btn));
      if (hasThesaurus()) loadOverview();
    });
  });
  const thBtn = document.getElementById("iaDashThesaurusBtn");
  if (thBtn) thBtn.addEventListener("click", toggleThesaurusPicker);
  const needCta = document.getElementById("iaDashNeedCta");
  if (needCta) needCta.addEventListener("click", openThesaurusPicker);
  const thList = document.getElementById("iaDashThesaurusList");
  if (thList) {
    thList.addEventListener("click", (e) => {
      const opt = e.target && e.target.closest ? e.target.closest("[data-th]") : null;
      if (!opt) return;
      setThesaurus(opt.getAttribute("data-th") || "");
    });
  }
  const thQuery = document.getElementById("iaDashThesaurusQuery");
  if (thQuery) {
    thQuery.addEventListener("input", () => {
      state.thQuery = thQuery.value || "";
      paintThesaurusList();
    });
    thQuery.addEventListener("keydown", (e) => {
      if (e.key === "Enter") e.preventDefault();
      if (e.key === "Escape") {
        e.preventDefault();
        closeThesaurusPicker();
      }
    });
  }
  document.addEventListener("mousedown", (e) => {
    const wrap = document.getElementById("iaDashThesaurus");
    if (wrap && thesaurusOpen() && !wrap.contains(e.target)) closeThesaurusPicker();
  });
  el.addEventListener("click", (e) => {
    const tab = e.target && e.target.closest ? e.target.closest("#iaDashSearchTabs [data-search]") : null;
    if (!tab || !el.contains(tab)) return;
    e.preventDefault();
    state.searchTab = tab.getAttribute("data-search") || "failed";
    syncSearchTabs();
    renderSearch();
  });
  const traffic = document.getElementById("iaDashTraffic");
  if (traffic) {
    traffic.addEventListener("pointerover", (e) => {
      const col = e.target && e.target.closest ? e.target.closest(".ia-dash-col") : null;
      const tip = traffic.querySelector(".ia-dash-spark-tip");
      if (!col || !tip || !traffic.contains(col)) return;
      tip.textContent = col.getAttribute("data-tip") || "";
      tip.hidden = false;
    });
    traffic.addEventListener("pointermove", (e) => {
      const tip = traffic.querySelector(".ia-dash-spark-tip");
      if (!tip || tip.hidden) return;
      const r = traffic.getBoundingClientRect();
      const x = Math.min(r.width - 16, Math.max(16, e.clientX - r.left));
      tip.style.left = x + "px";
      tip.style.top = Math.max(12, e.clientY - r.top - 12) + "px";
    });
    traffic.addEventListener("pointerout", (e) => {
      const next = e.relatedTarget;
      if (next && traffic.contains(next) && next.closest && next.closest(".ia-dash-col")) return;
      const tip = traffic.querySelector(".ia-dash-spark-tip");
      if (tip) tip.hidden = true;
    });
  }
  document.querySelectorAll("[data-dash-close]").forEach((btn) => btn.addEventListener("click", closeDrawer));
  const csv = document.getElementById("iaDashDrawerCsv");
  if (csv) csv.addEventListener("click", downloadCsv);
  document.addEventListener("click", (e) => {
    const btn = e.target && e.target.closest ? e.target.closest("[data-dash-fs]") : null;
    if (!btn) return;
    const card = btn.closest(".ia-dash-card");
    if (card) toggleFs(card);
  });
  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    if (thesaurusOpen()) {
      closeThesaurusPicker();
      return;
    }
    const drawer = document.getElementById("iaDashDrawer");
    if (drawer && !drawer.hidden) {
      closeDrawer();
      return;
    }
    closeFs();
  });
}

function boot() {
  if (!root()) return;
  bind();
  mountFsButtons();
  const scoped = scopedThesaurusId();
  if (scoped) {
    state.thesaurusId = scoped;
    loadOverview();
    loadQuality();
    return;
  }
  fetchJson("filters").then((f) => {
    fillThesauri(f && f.thesauri);
    if (hasThesaurus()) {
      loadOverview();
      loadQuality();
    }
  }).catch(() => fillThesauri([]));
}

window.bootInstanceStats = boot;
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", boot);
} else {
  boot();
}
})();
