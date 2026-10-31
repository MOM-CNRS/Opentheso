// ──────────────────────────────────────────────────────────────────────────
// Composants de recherche OpenTheso : autocomplétion (variantes), vue résultats,
// vue concept. Logique de matching dans un thésaurus unique.
// Exporte vers window pour partage entre scripts Babel.
// ──────────────────────────────────────────────────────────────────────────
const { useState, useRef, useEffect, useMemo, useContext } = React;

// ---- normalisation accents-insensible ----
function norm(s) {
  return (s || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

// ──────────────────────────────────────────────────────────────────────────
//  i18n — deux axes indépendants :
//   • LBL()  → langue d'AFFICHAGE des termes du thésaurus (fallback fr)
//   • UI()   → langue de l'INTERFACE du logiciel (chrome)
// ──────────────────────────────────────────────────────────────────────────
let DISPLAY_LANG = "fr";              // langue des termes
function setDisplayLang(l) { DISPLAY_LANG = l || "fr"; }
let _LBLMAP = null;
function lblMap() {
  if (_LBLMAP) return _LBLMAP;
  _LBLMAP = {};
  const cs = (window.THESAURUS && window.THESAURUS.concepts) || [];
  cs.forEach(c => { _LBLMAP[c.pref] = c.tr || {}; });
  return _LBLMAP;
}
// Traduit un libellé français vers la langue d'affichage (sinon renvoie le fr).
function LBL(label) {
  if (!label || DISPLAY_LANG === "fr") return label;
  const m = lblMap()[label];
  return (m && m[DISPLAY_LANG]) || label;
}
function displayLang() { return DISPLAY_LANG; }
// Langues disponibles pour l'affichage des termes (celles réellement traduites).
function termLangs() {
  const set = new Set(["fr"]);
  const cs = (window.THESAURUS && window.THESAURUS.concepts) || [];
  cs.forEach(c => Object.keys(c.tr || {}).forEach(k => set.add(k)));
  return [...set];
}

const UI_STR = {
  fr: { account: "Compte", logout: "Déconnexion", settings: "Paramètres",
    ifaceLang: "Langue de l'interface", termLang: "Langue des termes", termLangHint: "Affichage des libellés du thésaurus",
    views: "Vues\u00a0Thésaurus", arbo: "Arborescence", collection: "Collection", graph: "Graph", recherche: "Recherche", tableau: "Tableau", hyper: "Graphe", historique: "Historique",
    searchBtn: "Rechercher", clear: "Effacer", placeholder: "Rechercher…", concepts: "concepts",
    sort: "Trier", relevance: "Pertinence", alpha: "A → Z", resultOne: "résultat", resultMany: "résultats", forQ: "pour",
    noneTitle: "Aucun concept ne correspond à «\u00a0%s\u00a0» dans ce thésaurus.", noneSub: "Vérifiez l'orthographe ou essayez un terme plus général.",
    viaSyn: "trouvé via le synonyme", viaNote: "mentionné dans la note d'application",
    seeMore: "Voir plus", shown: "%s / %s affichés", allShown: "Tous les résultats sont affichés (%s)" },
  en: { account: "Account", logout: "Sign out", settings: "Settings",
    ifaceLang: "Interface language", termLang: "Term language", termLangHint: "Display of thesaurus labels",
    views: "Thesaurus views", arbo: "Tree", collection: "Collection", graph: "Graph", recherche: "Search", tableau: "Table", hyper: "Graph", historique: "History",
    searchBtn: "Search", clear: "Clear", placeholder: "Search…", concepts: "concepts",
    sort: "Sort", relevance: "Relevance", alpha: "A → Z", resultOne: "result", resultMany: "results", forQ: "for",
    noneTitle: "No concept matches \u201c%s\u201d in this thesaurus.", noneSub: "Check the spelling or try a broader term.",
    viaSyn: "found via synonym", viaNote: "mentioned in the scope note",
    seeMore: "Show more", shown: "%s / %s shown", allShown: "All results shown (%s)" },
  de: { account: "Konto", logout: "Abmelden", settings: "Einstellungen",
    ifaceLang: "Sprache der Oberfläche", termLang: "Sprache der Begriffe", termLangHint: "Anzeige der Thesaurus-Begriffe",
    views: "Thesaurus-Ansichten", arbo: "Baum", collection: "Sammlung", graph: "Graph", recherche: "Suche", tableau: "Tabelle", hyper: "Graph", historique: "Verlauf",
    searchBtn: "Suchen", clear: "Löschen", placeholder: "Suchen…", concepts: "Begriffe",
    sort: "Sortieren", relevance: "Relevanz", alpha: "A → Z", resultOne: "Treffer", resultMany: "Treffer", forQ: "für",
    noneTitle: "Kein Begriff entspricht \u201e%s\u201c in diesem Thesaurus.", noneSub: "Rechtschreibung prüfen oder einen allgemeineren Begriff versuchen.",
    viaSyn: "über Synonym gefunden", viaNote: "in der Anwendungsnotiz erwähnt",
    seeMore: "Mehr anzeigen", shown: "%s / %s angezeigt", allShown: "Alle Ergebnisse angezeigt (%s)" },
  es: { account: "Cuenta", logout: "Cerrar sesión", settings: "Ajustes",
    ifaceLang: "Idioma de la interfaz", termLang: "Idioma de los términos", termLangHint: "Visualización de los términos del tesauro",
    views: "Vistas del tesauro", arbo: "Árbol", collection: "Colección", graph: "Grafo", recherche: "Búsqueda", tableau: "Tabla", hyper: "Grafo", historique: "Historial",
    searchBtn: "Buscar", clear: "Borrar", placeholder: "Buscar…", concepts: "conceptos",
    sort: "Ordenar", relevance: "Relevancia", alpha: "A → Z", resultOne: "resultado", resultMany: "resultados", forQ: "para",
    noneTitle: "Ningún concepto coincide con «%s» en este tesauro.", noneSub: "Compruebe la ortografía o pruebe un término más general.",
    viaSyn: "encontrado por sinónimo", viaNote: "mencionado en la nota de aplicación",
    seeMore: "Ver más", shown: "%s / %s mostrados", allShown: "Todos los resultados mostrados (%s)" },
  it: { account: "Account", logout: "Esci", settings: "Impostazioni",
    ifaceLang: "Lingua dell'interfaccia", termLang: "Lingua dei termini", termLangHint: "Visualizzazione dei termini del tesauro",
    views: "Viste del tesauro", arbo: "Albero", collection: "Collezione", graph: "Grafo", recherche: "Ricerca", tableau: "Tabella", hyper: "Grafo", historique: "Cronologia",
    searchBtn: "Cerca", clear: "Cancella", placeholder: "Cerca…", concepts: "concetti",
    sort: "Ordina", relevance: "Pertinenza", alpha: "A → Z", resultOne: "risultato", resultMany: "risultati", forQ: "per",
    noneTitle: "Nessun concetto corrisponde a «%s» in questo tesauro.", noneSub: "Controlla l'ortografia o prova un termine più generale.",
    viaSyn: "trovato tramite sinonimo", viaNote: "menzionato nella nota d'uso",
    seeMore: "Mostra altri", shown: "%s / %s mostrati", allShown: "Tutti i risultati mostrati (%s)" }
};
const UI_LANGS = ["fr", "en", "de", "es", "it"];

// Chaînes de la navigation (rail) — fusionnées avec UI_STR via UI().
const NAV_STR = {
  fr: { projects: "Mes projets", admin: "Administration", superAdmin: "Super-admin",
    myProfile: "Mon profil", prefs: "Préférences", manageAccess: "Gérer mes accès",
    admUsers: "Utilisateurs", admProjects: "Projets", admThes: "Thésaurus", admStats: "Statistiques",
    apiRest: "API REST", apiGraph: "API graphe",
    apiRestDesc: "Accès programmatique aux concepts (JSON / RDF).",
    apiGraphDesc: "Point d'accès GraphQL pour interroger le graphe sémantique.",
    apiDocs: "Voir la documentation", openExplorer: "Ouvrir l'explorateur", copy: "Copier", copied: "Copié" },
  en: { projects: "My projects", admin: "Administration", superAdmin: "Super-admin",
    myProfile: "My profile", prefs: "Preferences", manageAccess: "Manage my access",
    admUsers: "Users", admProjects: "Projects", admThes: "Thesauri", admStats: "Statistics",
    apiRest: "REST API", apiGraph: "Graph API",
    apiRestDesc: "Programmatic access to concepts (JSON / RDF).",
    apiGraphDesc: "GraphQL endpoint to query the semantic graph.",
    apiDocs: "View documentation", openExplorer: "Open explorer", copy: "Copy", copied: "Copied" },
  de: { projects: "Meine Projekte", admin: "Verwaltung", superAdmin: "Super-Admin",
    myProfile: "Mein Profil", prefs: "Einstellungen", manageAccess: "Zugänge verwalten",
    admUsers: "Benutzer", admProjects: "Projekte", admThes: "Thesauri", admStats: "Statistiken",
    apiRest: "REST-API", apiGraph: "Graph-API",
    apiRestDesc: "Programmatischer Zugriff auf Begriffe (JSON / RDF).",
    apiGraphDesc: "GraphQL-Endpunkt zur Abfrage des semantischen Graphen.",
    apiDocs: "Dokumentation ansehen", openExplorer: "Explorer öffnen", copy: "Kopieren", copied: "Kopiert" },
  es: { projects: "Mis proyectos", admin: "Administración", superAdmin: "Superadmin",
    myProfile: "Mi perfil", prefs: "Preferencias", manageAccess: "Gestionar mis accesos",
    admUsers: "Usuarios", admProjects: "Proyectos", admThes: "Tesauros", admStats: "Estadísticas",
    apiRest: "API REST", apiGraph: "API de grafo",
    apiRestDesc: "Acceso programático a los conceptos (JSON / RDF).",
    apiGraphDesc: "Punto de acceso GraphQL para consultar el grafo semántico.",
    apiDocs: "Ver la documentación", openExplorer: "Abrir el explorador", copy: "Copiar", copied: "Copiado" },
  it: { projects: "I miei progetti", admin: "Amministrazione", superAdmin: "Super-admin",
    myProfile: "Il mio profilo", prefs: "Preferenze", manageAccess: "Gestisci i miei accessi",
    admUsers: "Utenti", admProjects: "Progetti", admThes: "Tesauri", admStats: "Statistiche",
    apiRest: "API REST", apiGraph: "API grafo",
    apiRestDesc: "Accesso programmatico ai concetti (JSON / RDF).",
    apiGraphDesc: "Endpoint GraphQL per interrogare il grafo semantico.",
    apiDocs: "Vedi la documentazione", openExplorer: "Apri l'explorer", copy: "Copia", copied: "Copiato" }
};
let UI_LANG = "fr";
function setUiLang(l) { UI_LANG = UI_STR[l] ? l : "fr"; }
function UI(key, ...args) {
  let s = (UI_STR[UI_LANG] && UI_STR[UI_LANG][key])
    ?? (NAV_STR[UI_LANG] && NAV_STR[UI_LANG][key])
    ?? UI_STR.fr[key] ?? (NAV_STR.fr && NAV_STR.fr[key]) ?? key;
  args.forEach(a => { s = s.replace("%s", a); });
  return s;
}

// Renvoie les résultats classés. Chaque hit porte sa "raison" de match.
// Codes (« note:… », « id:… ») : voir search-codes.jsx.
function searchConcepts(query, concepts) {
  const { text, codes } = parseSearch(query);
  const q = norm(text), active = codes.filter(c => c.v);
  if (!q && !active.length) return [];
  const hits = [];
  for (const c of concepts) {
    const why = active.map(code => matchCode(c, code));
    if (why.includes(null)) continue;
    const byCode = why.find(w => w.label);
    const pref = norm(c.pref);
    let score = -1, via = null, viaText = null;
    if (!q) { score = 0; }
    else if (pref === q) { score = 0; }
    else if (pref.startsWith(q)) { score = 1; }
    else if (pref.includes(q)) { score = 2; }
    else {
      // synonyme / forme rejetée
      const altHit = (c.alts || []).find(a => norm(a).includes(q));
      if (altHit) { score = 3; via = "synonyme"; viaText = altHit; }
      else if (c.def && norm(c.def).includes(q)) { score = 5; via = "note"; viaText = c.matchAlt || null; }
    }
    if (score < 0) continue;
    hits.push(byCode
      ? { ...c, _score: score, _via: "code", _viaLabel: byCode.label, _viaText: byCode.text }
      : { ...c, _score: score, _via: via, _viaText: viaText });
  }
  hits.sort((a, b) => a._score - b._score || a.pref.localeCompare(b.pref, "fr"));
  return hits;
}

// Surligne la sous-chaîne matchée (accents-insensible) dans un libellé.
function Highlight({ text, query, on }) {
  if (!on || !query) return text;
  const q = parseSearch(query).text;   // le texte libre, sans les codes
  const nt = norm(text), nq = norm(q);
  const i = nq ? nt.indexOf(nq) : -1;
  if (i < 0) return text;
  // Un seul span : dans un parent flex (.ac-pref, .res-pref…), trois nœuds
  // nus deviendraient trois items séparés par le gap (« Ép ée »).
  return (
    <span>
      {text.slice(0, i)}
      <mark className="hl">{text.slice(i, i + q.length)}</mark>
      {text.slice(i + q.length)}
    </span>
  );
}

function Path({ parts, last }) {
  const all = last ? [...parts, last] : parts;
  return (
    <span className="path">
      {all.map((p, i) => (
        <span key={i} className="path-seg">
          {LBL(p)}
          {i < all.length - 1 && <span className="path-sep">›</span>}
        </span>
      ))}
    </span>
  );
}

function StatusDot({ status }) {
  if (!status || status === "valide") return null;
  const m = STATUS_META[status];
  return <span className={"dot-status st-" + status} title={m ? m.label + " — " + m.hint : status} />;
}

// ── Système de statuts (pattern réutilisable de gestion d'affichage) ──
// « valide » = « Concept ajouté », entré directement (édition) ; « candidat » =
// proposé, en cours de validation ; « insere » = « Candidat ajouté », entré
// via le circuit candidat → validation ; « rejete » = proposition refusée ;
// « deprecie » = retiré. Renommé le 22-09 (retours métier :
// "Normal"/"Inséré" d'abord, puis "Initial"/"Ajouté" ne voulaient toujours
// rien dire) — le libellé seul devant rester ambigu, un hover (m.hint,
// StatusPill/StatusChecklist) porte l'explication à côté.
const STATUS_META = {
  valide:   { label: "Concept ajouté",  plural: "Concepts ajoutés",  hint: "entré directement, par édition" },
  candidat: { label: "Candidat",        plural: "Candidats",         hint: "proposé, en cours de validation" },
  insere:   { label: "Candidat ajouté", plural: "Candidats ajoutés", hint: "proposé comme candidat, puis validé" },
  rejete:   { label: "Rejeté",          plural: "Rejetés",           hint: "proposition refusée" },
  deprecie: { label: "Déprécié",        plural: "Dépréciés",         hint: "retiré de l'usage" }
};
const STATUS_ORDER = ["valide", "candidat", "insere", "rejete", "deprecie"];
// Regroupement par « stade de vie » du concept : actifs (en usage) / candidats
// (à valider) / inactifs (hors usage). Donne une case de groupe = 1 clic.
const STATUS_GROUPS = [
  { key: "actif",    label: "Actifs",    statuses: ["valide", "insere"] },
  { key: "candidat", label: "Candidats", statuses: ["candidat"] },
  { key: "inactif",  label: "Inactifs",  statuses: ["rejete", "deprecie"] }
];
const allStatuses = (perm = true) => STATUS_ORDER.filter(s => perm || s !== "candidat");
const statusOf = (c) => c.status || "valide";

// Pastille + libellé d'un statut (cellule de tableau, ligne d'arbre étendue).
function StatusPill({ status }) {
  const m = STATUS_META[status] || { label: status };
  return (
    <span className={"st-pill st-" + status}>
      <span className={"st-dot st-" + status} />{m.label}
      {m.hint && <span className="st-info" role="img" title={m.hint} aria-label={m.hint}>?</span>}
    </span>
  );
}

// Liste de statuts cochables, regroupés par stade (actifs / candidats / inactifs).
// Case de groupe = coche/décoche tout le groupe en un clic. Sous « Candidats »,
// un moteur de filtrage avancé (déposant, mes dépôts, plage de dates).
function StatusChecklist({ set, onChange, perm = true, counts, candFilter, onCandFilter, depositors }) {
  const avail = allStatuses(perm);
  const groups = STATUS_GROUPS
    .map(g => ({ ...g, statuses: g.statuses.filter(s => avail.includes(s)) }))
    .filter(g => g.statuses.length);
  const toggle = (s) => { const n = new Set(set); n.has(s) ? n.delete(s) : n.add(s); onChange(n); };
  const setMany = (list, on) => { const n = new Set(set); list.forEach(s => on ? n.add(s) : n.delete(s)); onChange(n); };
  const groupState = (g) => {
    const on = g.statuses.filter(s => set.has(s)).length;
    return on === 0 ? "off" : (on === g.statuses.length ? "on" : "mixed");
  };
  return (
    <div className="stk">
      <div className="stk-title">Statuts affichés</div>
      {groups.map(g => {
        const gs = groupState(g);
        const gcount = g.statuses.reduce((a, s) => a + ((counts && counts[s]) || 0), 0);
        const isCandGroup = g.key === "candidat";
        return (
          <div className="stk-group" key={g.key}>
            <label className={"stk-ghead" + (gs !== "off" ? " on" : "")}>
              <input type="checkbox" checked={gs === "on"}
                     ref={el => { if (el) el.indeterminate = gs === "mixed"; }}
                     onChange={() => setMany(g.statuses, gs !== "on")} />
              <span className={"stk-box" + (gs === "mixed" ? " mixed" : "")} aria-hidden="true">{gs === "on" ? <Icon name="check" /> : gs === "mixed" ? "–" : ""}</span>
              <span className="stk-glabel">{g.label}</span>
              {counts && <span className="stk-n">{gcount}</span>}
            </label>
            {g.statuses.length > 1 && (
              <div className="stk-list">
                {g.statuses.map(s => {
                  const m = STATUS_META[s];
                  return (
                    <label key={s} className={"stk-item" + (set.has(s) ? " on" : "")}>
                      <input type="checkbox" checked={set.has(s)} onChange={() => toggle(s)} />
                      <span className="stk-box" aria-hidden="true">{set.has(s) ? <Icon name="check" /> : ""}</span>
                      <span className={"st-dot st-" + s} />
                      <span className="stk-l">{m.plural}</span>
                      {m.hint && <span className="st-info" role="img" title={m.hint} aria-label={m.hint} onClick={e => e.stopPropagation()}>?</span>}
                      {counts && counts[s] != null && <span className="stk-n">{counts[s]}</span>}
                    </label>
                  );
                })}
              </div>
            )}
            {isCandGroup && candFilter && set.has("candidat") && (
              <CandFilter cf={candFilter} onChange={onCandFilter} depositors={depositors || []} />
            )}
          </div>
        );
      })}
    </div>
  );
}

// Moteur de recherche candidats (interne au statut « Candidats »).
// « Déposé par » = combobox recherchable : Tout le monde / Moi / autres personnes.
function CandFilter({ cf, onChange, depositors }) {
  const set = (p) => onChange({ ...cf, ...p });
  const [q, setQ] = useState("");
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);
  const meLabel = SESSION.user.name;
  const others = depositors.filter(d => d !== ME);
  const match = (d) => !q.trim() || norm(d).includes(norm(q));
  const meMatch = !q.trim() || norm("moi " + meLabel + " " + ME).includes(norm(q));
  const curLabel = !cf.by ? "Tout le monde" : (cf.by === ME ? "Moi (" + meLabel + ")" : cf.by);
  const pick = (val) => { set({ by: val }); setQ(""); setOpen(false); };
  const active = cf.by || cf.from || cf.to;
  return (
    <div className="cf">
      <div className="cf-fld" ref={ref}>
        <span className="cf-l">Déposé par</span>
        <div className={"cf-combo" + (open ? " open" : "")}>
          <input className="cf-in" value={open ? q : curLabel}
                 placeholder="Rechercher une personne…"
                 onFocus={() => { setOpen(true); setQ(""); }}
                 onChange={e => { setQ(e.target.value); setOpen(true); }} />
          <span className="cf-combo-caret" aria-hidden="true">▾</span>
          {open && (
            <div className="cf-menu">
              <button className={"cf-opt" + (!cf.by ? " on" : "")} onClick={() => pick("")}>Tout le monde<small>tous les candidats</small></button>
              {meMatch && (
                <button className={"cf-opt" + (cf.by === ME ? " on" : "")} onClick={() => pick(ME)}>
                  Moi<small>{meLabel} · mes candidats</small></button>
              )}
              {others.filter(match).map(d => (
                <button key={d} className={"cf-opt" + (cf.by === d ? " on" : "")} onClick={() => pick(d)}>{d}</button>
              ))}
              {!meMatch && others.filter(match).length === 0 && <div className="cf-empty">Aucune personne</div>}
            </div>
          )}
        </div>
      </div>
      <div className="cf-fld">
        <span className="cf-l">Déposé entre</span>
        <div className="cf-range">
          <input type="date" className="cf-in" value={cf.from} onChange={e => set({ from: e.target.value })} />
          <span className="cf-arrow">→</span>
          <input type="date" className="cf-in" value={cf.to} onChange={e => set({ to: e.target.value })} />
        </div>
      </div>
      {active && <button className="cf-clear" onClick={() => onChange({ by: "", from: "", to: "" })}>Réinitialiser les filtres candidats</button>}
    </div>
  );
}

function TypeTag({ type }) {
  if (type === "collection") return <span className="tag tag-col">Collection</span>;
  return null;
}

// ════════════════════════════════════════════════════════════════════════
//  AUTOCOMPLÉTION — 4 variantes comparables (réglable via Tweaks)
// ════════════════════════════════════════════════════════════════════════
function Autocomplete({ query, results, variant, showPath, highlight, onPick, onSeeAll, activeIdx, onCode }) {
  if (!query.trim()) return null;
  const shown = results.slice(0, 7);
  const codes = <SearchCodes query={query} onCode={onCode} />;

  if (results.length === 0) {
    return (
      <div className="ac">
        {hasSearch(query) && <div className="ac-empty">Aucune entrée pour « {query} »</div>}
        {codes}
      </div>
    );
  }

  // Variante C : sections par type
  if (variant === "sections") {
    const concepts = shown.filter(r => r.type === "concept");
    const cols = shown.filter(r => r.type === "collection");
    let idx = -1;
    const Row = (r) => {
      idx += 1; const myIdx = idx;
      return <ACRow key={r.id} r={r} query={query} showPath={showPath} highlight={highlight}
                    active={myIdx === activeIdx} onPick={onPick} />;
    };
    return (
      <div className="ac">
        {concepts.length > 0 && <div className="ac-group">Concepts</div>}
        {concepts.map(Row)}
        {cols.length > 0 && <div className="ac-group">Collections</div>}
        {cols.map(Row)}
        <ACFooter count={results.length} onSeeAll={onSeeAll} />
        {codes}
      </div>
    );
  }

  return (
    <div className="ac">
      {shown.map((r, i) => (
        <ACRow key={r.id} r={r} query={query} showPath={variant !== "simple" && showPath}
               compact={variant === "simple"} via={variant === "rich"}
               highlight={highlight} active={i === activeIdx} onPick={onPick} />
      ))}
      <ACFooter count={results.length} onSeeAll={onSeeAll} />
      {codes}
    </div>
  );
}

function ACRow({ r, query, showPath, compact, via, highlight, active, onPick }) {
  return (
    <button className={"ac-row" + (active ? " is-active" : "")}
            onMouseDown={(e) => { e.preventDefault(); onPick(r); }}>
      <span className="ac-ico" aria-hidden="true">
        {r.type === "collection" ? "▣" : "◆"}
      </span>
      <span className="ac-body">
        <span className="ac-pref">
          <Highlight text={LBL(r.pref)} query={query} on={highlight} />
          <StatusDot status={r.status} />
        </span>
        {via && r._via && (
          <span className="ac-via">
            {r._via === "code" ? r._viaLabel.toLowerCase() + " : "
              : r._via === "synonyme" ? "synonyme : " : "note : "}
            <em>{r._viaText || "…"}</em>
          </span>
        )}
        {showPath && !compact && <Path parts={r.path} />}
      </span>
      {via && <span className="ac-notation">{r.notation}</span>}
    </button>
  );
}

function ACFooter({ count, onSeeAll }) {
  return (
    <button className="ac-footer" onMouseDown={(e) => { e.preventDefault(); onSeeAll(); }}>
      Voir les <strong>{count}</strong> résultats
      <span className="ac-enter">↵</span>
    </button>
  );
}

// ════════════════════════════════════════════════════════════════════════
//  VUE RÉSULTATS — compteur toujours visible, limite + « voir plus »
// ════════════════════════════════════════════════════════════════════════
function ResultsView({ query, results, density, showPath, highlight, pageSize, sort, onSort, onOpen }) {
  const [limit, setLimit] = useState(pageSize);
  useEffect(() => { setLimit(pageSize); }, [pageSize, query, sort]);

  const sorted = useMemo(() => {
    const arr = [...results];
    if (sort === "alpha") arr.sort((a, b) => a.pref.localeCompare(b.pref, "fr"));
    return arr; // "pertinence" = ordre natif
  }, [results, sort]);

  const visible = sorted.slice(0, limit);
  const remaining = sorted.length - visible.length;

  return (
    <div className="rv">
      <div className="rv-head">
        <div className="rv-count">
          <span className="rv-n">{results.length}</span>
          <span className="rv-label">
            {results.length > 1 ? UI("resultMany") : UI("resultOne")} {UI("forQ")} «&nbsp;<strong>{query}</strong>&nbsp;»
          </span>
        </div>
        <div className="rv-sort">
          <span className="rv-sort-lbl">{UI("sort")}&nbsp;:</span>
          {[["pertinence", UI("relevance")], ["alpha", UI("alpha")]].map(([v, l]) => (
            <button key={v} className={"chip" + (sort === v ? " is-on" : "")}
                    onClick={() => onSort(v)}>{l}</button>
          ))}
        </div>
      </div>

      {results.length === 0 ? (
        <div className="rv-empty">
          <div className="rv-empty-ico">⌕</div>
          <p>{UI("noneTitle", query)}</p>
          <p className="rv-empty-sub">{UI("noneSub")}</p>
        </div>
      ) : (
        <>
          <ul className={"rv-list density-" + density}>
            {visible.map((r) => (
              <li key={r.id}>
                <button className="res" onClick={() => onOpen(r)}>
                  <span className="res-main">
                    <span className="res-pref">
                      <Highlight text={LBL(r.pref)} query={query} on={highlight} />
                      <StatusDot status={r.status} />
                      <TypeTag type={r.type} />
                    </span>
                    {r._via && (
                      <span className="res-via">
                        {r._via === "code" ? <ViaCode r={r} />
                          : r._via === "synonyme"
                          ? <>{UI("viaSyn")} «&nbsp;<em>{r._viaText}</em>&nbsp;»</>
                          : <>{UI("viaNote")}</>}
                      </span>
                    )}
                    {showPath && <Path parts={r.path} />}
                  </span>
                  <span className="res-meta">
                    <span className="res-notation">{r.notation}</span>
                    <span className="res-go" aria-hidden="true">→</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>

          {remaining > 0 ? (
            <button className="see-more" onClick={() => setLimit((l) => l + pageSize)}>
              {UI("seeMore")} <span className="see-more-n">+{Math.min(remaining, pageSize)}</span>
              <span className="see-more-tot">{UI("shown", visible.length, sorted.length)}</span>
            </button>
          ) : (
            sorted.length > pageSize && (
              <div className="see-end">{UI("allShown", sorted.length)}</div>
            )
          )}
        </>
      )}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════════
//  LISTE DE RÉSULTATS COMPACTE — panneau gauche (à la place de l'arbo).
//  Le résultat sélectionné s'affiche en Vue concept dans le panneau droit.
// ════════════════════════════════════════════════════════════════════════
function ResultsList({ query, results, selectedId, sort, onSort, pageSize, highlight, showPath, onSelect }) {
  const [limit, setLimit] = useState(pageSize);
  useEffect(() => { setLimit(pageSize); }, [pageSize, query, sort]);

  const sorted = useMemo(() => {
    const arr = [...results];
    if (sort === "alpha") arr.sort((a, b) => a.pref.localeCompare(b.pref, "fr"));
    return arr;
  }, [results, sort]);

  const visible = sorted.slice(0, limit);
  const remaining = sorted.length - visible.length;
  const selRef = useRef(null);

  useEffect(() => {
    if (!selRef.current) return;
    const row = selRef.current, cont = row.closest(".rl");
    if (!cont) return;
    const rb = row.getBoundingClientRect(), cb = cont.getBoundingClientRect();
    if (rb.top < cb.top + 48 || rb.bottom > cb.bottom - 6) {
      cont.scrollTop += (rb.top - cb.top) - cont.clientHeight / 2 + row.offsetHeight / 2;
    }
  }, [selectedId]);

  return (
    <div className="rl">
      {results.length === 0 ? (
        <div className="rl-empty">
          <div className="rl-empty-ico" aria-hidden="true">⌕</div>
          {query.trim() && <p>{UI("noneTitle", query)}</p>}
          {query.trim() && <p className="rl-empty-sub">{UI("noneSub")}</p>}
        </div>
      ) : (
        <>
          <ul className="rl-list">
            {visible.map((r) => (
              <li key={r.id}>
                <button ref={selectedId === r.id ? selRef : null}
                        className={"rl-item" + (selectedId === r.id ? " is-sel" : "")}
                        onClick={() => onSelect(r)}>
                  <span className="rl-pref">
                    <Highlight text={LBL(r.pref)} query={query} on={highlight} />
                    <StatusDot status={r.status} />
                    <TypeTag type={r.type} />
                  </span>
                  {r._via && (
                    <span className="rl-via">
                      {r._via === "code" ? <ViaCode r={r} />
                        : r._via === "synonyme"
                        ? <>{UI("viaSyn")} «&nbsp;<em>{r._viaText}</em>&nbsp;»</>
                        : UI("viaNote")}
                    </span>
                  )}
                  {showPath && <Path parts={r.path} />}
                </button>
              </li>
            ))}
          </ul>
          {remaining > 0 && (
            <button className="rl-more" onClick={() => setLimit((l) => l + pageSize)}>
              {UI("seeMore")} <span className="rl-more-n">+{Math.min(remaining, pageSize)}</span>
            </button>
          )}
        </>
      )}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════════
//  VUE CONCEPT — ensemble de blocs (Contexte, Relations, Traductions,
//  Notes, Identifiants, Temporel), façon fiche OpenTheso.
// ════════════════════════════════════════════════════════════════════════
// Pas de drapeau pour les langues qui n'en ont pas un défendable (l'arabe n'est pas
// un pays ; catalan et basque n'ont pas de rendu emoji) : le code ISO les identifie.
const FLAG = { fr: "🇫🇷", en: "🇬🇧", de: "🇩🇪", es: "🇪🇸", it: "🇮🇹", nl: "🇳🇱", pt: "🇵🇹", el: "🇬🇷" };
const LANGNAME = { fr: "Français", en: "Anglais", de: "Allemand", es: "Espagnol", it: "Italien", nl: "Néerlandais", pt: "Portugais",
  ar: "Arabe", ca: "Catalan", eu: "Basque", el: "Grec" };
const ENDONYM = { fr: "Français", en: "English", de: "Deutsch", es: "Español", it: "Italiano", nl: "Nederlands", pt: "Português",
  ar: "العربية", ca: "Català", eu: "Euskara", el: "Ελληνικά" };

// Fournit un gestionnaire d'agrandissement « sur place » (panneau de droite).
// Si absent (ex. accueil thésaurus), Block retombe sur une modale.
// ══════════════════════════════════════════════════════════════
//  CLS-1..4 — Classes de concept. Opt-in par thésaurus : tant que
//  l'activation est à off, AUCUNE trace de classe ailleurs dans l'UI.
//  Un concept peut porter plusieurs classes. Configuration : Paramètres.
// ══════════════════════════════════════════════════════════════
const CLS_K_ON = "ot.clsEnabled", CLS_K_LIST = "ot.clsList";
let CLS_ON = (() => { try { const v = localStorage.getItem(CLS_K_ON); if (v !== null) return v === "1"; } catch (e) {} return !!(window.CLS_CONFIG || {}).enabled; })();
let CLS_LIST = (() => { try { const v = JSON.parse(localStorage.getItem(CLS_K_LIST)); if (Array.isArray(v) && v.length) return v; } catch (e) {} return ((window.CLS_CONFIG || {}).classes || []).slice(); })();
const clsEnabled = () => CLS_ON;
const clsList = () => CLS_LIST;
const clsMeta = (k) => CLS_LIST.find(x => x.key === k) || null;
const clsOf = (c) => CLS_ON ? ((c && c.cls) || []).map(clsMeta).filter(Boolean) : [];
function otSync() { if (typeof window.__otSync === "function") window.__otSync(); }
// Formulaire de création en cours de saisie : lu par app.jsx pour bloquer une
// navigation qui ferait perdre le brouillon (modale de confirmation).
let DRAFT_DIRTY = false;
function setDraftDirty(v) { DRAFT_DIRTY = !!v; }
function isDraftDirty() { return DRAFT_DIRTY; }
function setClsEnabled(v) { CLS_ON = !!v; try { localStorage.setItem(CLS_K_ON, v ? "1" : "0"); } catch (e) {} otSync(); }
function setClsList(l) { CLS_LIST = l.slice(); try { localStorage.setItem(CLS_K_LIST, JSON.stringify(CLS_LIST)); } catch (e) {} otSync(); }

function ClassBadges({ c, max = 3, compact }) {
  const list = clsOf(c);
  if (!list.length) return null;
  return (
    <span className={"cls-badges" + (compact ? " is-compact" : "")}>
      {list.slice(0, max).map(m => (
        <span key={m.key} className="cls-badge" title={"Classe de concept : " + m.label}
              style={{ color: m.color, borderColor: m.color + "55", background: m.color + "14" }}>
          {compact ? (m.short || m.label) : m.label}
        </span>
      ))}
      {list.length > max && <span className="cls-more">+{list.length - max}</span>}
    </span>
  );
}

// CLS-4 — assignation multi-classes, édition en place dans la fiche.
function ClassPicker({ value, onChange }) {
  const v = value || [];
  if (!clsList().length) return <div className="re-none">Aucune classe configurée dans les paramètres du thésaurus.</div>;
  return (
    <div className="cls-pick">
      {clsList().map(m => (
        <button type="button" key={m.key} className={"cls-opt" + (v.includes(m.key) ? " on" : "")}
                onClick={() => onChange(v.includes(m.key) ? v.filter(x => x !== m.key) : v.concat([m.key]))}>
          <span className="cls-dot" style={{ background: m.color }} />{m.label}
        </button>
      ))}
    </div>
  );
}

// EDT-4 / EDT-5 — type de la relation hiérarchique, porté par le lien.
const REL_HIER = window.REL_HIER || [{ k: "BTG", label: "Générale", short: "gén." }];
const hierMeta = (k) => REL_HIER.find(h => h.k === k) || REL_HIER[0];
function initHTypes(c) {
  const all = (window.THESAURUS || {}).concepts || [];
  const m = {};
  (c.bt || []).forEach(p => { m[p] = c.btType || "BTG"; });
  (c.nt || []).forEach(n => { const k = all.find(x => x.pref === n); m[n] = (k && k.btType) || "BTG"; });
  return m;
}
function HierSelect({ value, onChange }) {
  return (
    <select className="re-hsel" value={value || "BTG"} onChange={e => onChange(e.target.value)}
            title="Type de la relation hiérarchique">
      {REL_HIER.map(h => <option key={h.k} value={h.k}>{h.label}</option>)}
    </select>
  );
}

const ExpandCtx = React.createContext(null);

function Block({ title, children, editable, fullContent, edit, id, drag, unitary, onSave, onCancel }) {
  const onExpand = useContext(ExpandCtx);
  const [full, setFull] = useState(false);
  const [editing, setEditing] = useState(false);
  const close = () => setFull(false);
  const content = fullContent || children;
  const doExpand = () => { if (onExpand) onExpand({ title, content }); else setFull(true); };
  const s = drag ? drag.section : null;
  return (
    <>
      <section className={"cblock" + (editing ? " is-editing" : "") + (drag && drag.dragging ? " is-dragging" : "") + (drag && drag.dropBefore ? " drop-before" : "") + (drag && drag.dropAfter ? " drop-after" : "")}
        style={s ? { order: s.order } : undefined}
        draggable={s ? s.draggable : undefined}
        onDragStart={s ? s.onDragStart : undefined} onDragEnd={s ? s.onDragEnd : undefined}
        onDragOver={s ? s.onDragOver : undefined} onDrop={s ? s.onDrop : undefined}>
        <div className="cblock-head">
          {drag && (
            <button type="button" className="cblock-drag" title="Glisser pour réordonner" aria-label="Réordonner ce bloc"
              onMouseDown={drag.handle.onMouseDown} onMouseUp={drag.handle.onMouseUp} onClick={(e) => e.preventDefault()}>
              <span className="cblock-grip"></span>
            </button>
          )}
          {title}
          <div className="cblock-tools">
            {editable && !editing && <button className="cblock-gear" title="Éditer" onClick={() => setEditing(true)}><Icon name="edit" className="ico-inline"/></button>}
            {!editing && <button className="cblock-gear" title="Agrandir" onClick={doExpand}>⤢</button>}
          </div>
        </div>
        <div className="cblock-body">
          {editing ? (
            <>
              {edit || children}
              <div className="cblock-edit-foot">
                {unitary ? <>
                  <button className="bo-btn primary sm" onClick={() => { onSave && onSave(); setEditing(false); }}>Terminer</button>
                </> : <>
                  <button className="bo-btn primary sm" onClick={() => { onSave && onSave(); setEditing(false); }}>Enregistrer</button>
                  <button className="bo-btn ghost sm" onClick={() => { onCancel && onCancel(); setEditing(false); }}>Annuler</button>
                </>}
              </div>
            </>
          ) : children}
        </div>
      </section>
      {full && (
        <div className="block-overlay" onClick={close}>
          <div className="block-modal" onClick={(e) => e.stopPropagation()}>
            <div className="block-modal-head">
              {title}
              <button title="Fermer" onClick={close}><Icon name="x" className="ico-inline"/></button>
            </div>
            <div className="block-modal-body">
              {typeof content === "function" ? content(close) : content}
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// Graphe de relations façon Obsidian — couleur/forme selon la nature TG/TS/TA.
function RelationGraph({ concept, onTerm }) {
  const c = concept;
  const W = 1000, H = 640, cx = W / 2, cy = H / 2;
  const D2R = Math.PI / 180;

  // Place n items le long d'un secteur [a0,a1]°, sur 1 ou 2 anneaux selon le nombre.
  const placeArc = (items, a0, a1, R0, R1) => {
    const n = items.length;
    if (!n) return [];
    const ring = (arr, R) => arr.map((name, i) => {
      const f = arr.length === 1 ? 0.5 : i / (arr.length - 1);
      return { name, ang: (a0 + (a1 - a0) * f) * D2R, R };
    });
    if (n <= 6) return ring(items, R0);
    const inner = [], outer = [];
    items.forEach((it, i) => (i % 2 === 0 ? inner : outer).push(it));
    return [...ring(inner, R0), ...ring(outer, R1)];
  };

  const bt = c.bt || [], nt = c.nt || [], rt = c.rt || [];
  // TA réparti gauche + droite pour aérer
  const rtRight = rt.filter((_, i) => i % 2 === 0);
  const rtLeft = rt.filter((_, i) => i % 2 === 1);

  const groups = [
    { color: "#6b7886", dash: false, pts: placeArc(bt, -150, -30, 165, 235) },   // TG haut
    { color: "#1f7a5c", dash: false, pts: placeArc(nt, 30, 150, 170, 245) },     // TS bas
    { color: "#a98b3e", dash: true,  pts: placeArc(rtRight, -38, 38, 215, 290) },// TA droite
    { color: "#a98b3e", dash: true,  pts: placeArc(rtLeft, 142, 218, 215, 290) } // TA gauche
  ];

  const nodes = [];
  groups.forEach((g) => g.pts.forEach((p) => {
    const x = cx + p.R * Math.cos(p.ang), y = cy + p.R * Math.sin(p.ang);
    const cos = Math.cos(p.ang), sin = Math.sin(p.ang);
    const anchor = cos > 0.34 ? "start" : cos < -0.34 ? "end" : "middle";
    const lx = x + cos * 15, ly = y + sin * 15 + (sin > 0.3 ? 12 : sin < -0.3 ? -8 : 4);
    nodes.push({ name: p.name, x, y, color: g.color, dash: g.dash, anchor, lx, ly });
  }));

  return (
    <div className="rgraph">
      <svg viewBox={`0 0 ${W} ${H}`} className="rgraph-svg">
        {nodes.map((nd, i) => (
          <line key={"e" + i} x1={cx} y1={cy} x2={nd.x} y2={nd.y}
                stroke={nd.color} strokeWidth="2.5" opacity="0.4"
                strokeDasharray={nd.dash ? "6 6" : "0"} />
        ))}
        <circle cx={cx} cy={cy} r="17" fill="#16191c" />
        <text x={cx} y={cy + 5} textAnchor="middle" className="rgraph-code">●</text>
        <text x={cx} y={cy + 40} textAnchor="middle" className="rgraph-center-t">{LBL(c.pref)}</text>
        {nodes.map((nd, i) => (
          <g key={"n" + i} className="rgraph-node" onClick={() => onTerm(nd.name)}>
            <circle cx={nd.x} cy={nd.y} r="14" fill={nd.color} opacity="0" className="rgraph-hit" />
            <circle cx={nd.x} cy={nd.y} r="9" fill={nd.color} />
            <circle cx={nd.x} cy={nd.y} r="9" fill="none" stroke={nd.color} strokeWidth="3" className="rgraph-halo" />
            <text x={nd.lx} y={nd.ly} textAnchor={nd.anchor} className="rgraph-label">{LBL(nd.name)}</text>
          </g>
        ))}
      </svg>
      <div className="rgraph-legend">
        <span className="lg"><i style={{ background: "#6b7886" }} />TG · terme générique</span>
        <span className="lg"><i style={{ background: "#1f7a5c" }} />TS · terme spécifique</span>
        <span className="lg"><i className="dashed" />TA · terme associé</span>
      </div>
    </div>
  );
}

// ═════════════════════════════════════════════════════════════════
//  CRE-1 — Que peut-on créer ? Dépend du rôle sur le thésaurus :
//  un gestionnaire crée des concepts directement, les autres ne peuvent
//  que proposer des candidats, qui passent par le cycle de validation.
// ═════════════════════════════════════════════════════════════════
const CREATE_KINDS = [
  { k: "concept", cap: "th.editConcept", icon: "tag", title: "Concept",
    desc: "Entre directement dans le thésaurus, sans validation.",
    lock: "Réservé aux gestionnaires du thésaurus." },
  { k: "candidat", cap: "th.proposeCandidate", icon: "plus", title: "Candidat",
    desc: "Proposition soumise à validation avant d'entrer dans le thésaurus.",
    lock: "Vous n'avez pas de droit de contribution sur ce thésaurus." },
  { k: "facette", cap: "th.editConcept", icon: "layers", title: "Facette",
    desc: "Découpe l'arborescence sans être un concept indexable.",
    lock: "Réservé aux gestionnaires du thésaurus." }
];
function CreateChooser({ perm, parent, onPick, onClose }) {
  // Pas de facette à la racine : une facette découpe l'arborescence sous un
  // concept, elle n'a pas de sens comme 1er niveau du thésaurus.
  const kinds = CREATE_KINDS
    .filter(x => parent || x.k !== "facette")
    .map(x => ({ ...x, ok: !perm || perm.can(x.cap) }));
  return (
    <Modal title="Créer" sub={parent ? "Créer sous « " + LBL(parent.pref) + " »" : "Créer un top concept"}
           onClose={onClose}>
      <div className="spl ck">
        {kinds.map(x => (
          <button className={"spl-row" + (x.ok ? "" : " is-locked")} key={x.k} disabled={!x.ok}
                  onClick={() => { onClose(); onPick(x.k); }}>
            <span className="spl-ico"><Icon name={x.icon} /></span>
            <span className="spl-tx">
              <span className="spl-t">{x.title}</span>
              <span className="spl-d">{x.ok ? x.desc : x.lock}</span>
            </span>
            {x.ok ? <span className="spl-go"><Icon name="chevron-right" /></span>
                  : <span className="ck-lock"><Icon name="lock" /></span>}
          </button>
        ))}
      </div>
    </Modal>
  );
}

// ── Cartes propres au candidat (un candidat = un concept en statut particulier) ──
function CandidateBanner({ c, onResolve }) {
  const k = c.cand || {};
  const v = k.votes || { up: 0, down: 0 };
  const [mode, setMode] = useState(null);   // null | reject
  const [msg, setMsg] = useState("");
  return (
    <div className={"cand-banner" + (mode === "reject" ? " is-open" : "")}>
      <div className="cand-banner-row">
        <span className="cand-banner-dot" />
        <div className="cand-banner-main">
          <div className="cand-banner-t">Concept candidat — en attente de validation</div>
          <div className="cand-banner-s">Proposé par <b>{k.by}</b> le {k.on}
            <span className="cand-banner-sep">·</span>
            <span title="Votes pour / contre">▲ {v.up} ▼ {v.down}</span>
            <span className="cand-banner-sep">·</span>
            <span title="Messages">✉ {(k.discussion || []).length}</span>
          </div>
        </div>
        <div className="cand-banner-actions">
          <button className="bo-btn primary sm" onClick={() => onResolve && onResolve("ajouté")}><Icon name="check" className="ico-inline"/> Ajouter au thésaurus</button>
          <button className={"bo-btn ghost danger sm" + (mode === "reject" ? " is-on" : "")}
                  onClick={() => setMode(m => m === "reject" ? null : "reject")}><Icon name="x" className="ico-inline"/> Rejeter</button>
        </div>
      </div>
      {mode === "reject" && (
        <div className="cand-banner-reject">
          <textarea className="disc-input" placeholder="Motif du rejet (visible par le proposant)…"
                    value={msg} onChange={e => setMsg(e.target.value)} />
          <div className="cand-decide-actions">
            <button className="bo-btn danger sm" onClick={() => onResolve && onResolve("rejeté")}>Confirmer le rejet</button>
            <button className="bo-btn ghost sm" onClick={() => setMode(null)}>Annuler</button>
          </div>
        </div>
      )}
    </div>
  );
}

function VotesBlock({ c }) {
  const k = c.cand || {};
  const [mine, setMine] = useState(k.mine || null);
  const base = k.votes || { up: 0, down: 0 };
  const up = base.up + (mine === "up" ? 1 : 0) - (k.mine === "up" ? 1 : 0);
  const down = base.down + (mine === "down" ? 1 : 0) - (k.mine === "down" ? 1 : 0);
  const total = up + down;
  return (
    <Block title="Votes">
      <div className="votes">
        <div className="votes-bar">
          <span className="votes-fill up" style={{ width: (total ? up / total * 100 : 50) + "%" }} />
          <span className="votes-fill down" style={{ width: (total ? down / total * 100 : 50) + "%" }} />
        </div>
        <div className="votes-counts">
          <span className="votes-c up">▲ {up} pour</span>
          <span className="votes-c down">▼ {down} contre</span>
        </div>
        <div className="votes-actions">
          <button className={"vote-btn up" + (mine === "up" ? " is-on" : "")}
                  onClick={() => setMine(m => m === "up" ? null : "up")}>▲ Voter pour</button>
          <button className={"vote-btn down" + (mine === "down" ? " is-on" : "")}
                  onClick={() => setMine(m => m === "down" ? null : "down")}>▼ Voter contre</button>
        </div>
        <div className="votes-note">Le vote oriente la décision ; un gestionnaire tranche au moment du traitement.</div>
      </div>
    </Block>
  );
}

function DiscussionBlock({ c }) {
  const k = c.cand || {};
  const [msgs, setMsgs] = useState(k.discussion || []);
  const [draft, setDraft] = useState("");
  const send = () => {
    const t = draft.trim();
    if (!t) return;
    setMsgs(m => [...m, { u: "c.roussel", d: "à l'instant", t }]);
    setDraft("");
  };
  return (
    <Block title={"Discussion" + (msgs.length ? " (" + msgs.length + ")" : "")}>
      <div className="disc">
        {msgs.length ? (
          <div className="disc-thread">
            {msgs.map((m, i) => (
              <div key={i} className="disc-msg">
                <div className="disc-msg-h"><span className="disc-u">{m.u}</span><span className="disc-d">{m.d}</span></div>
                <p className="disc-t">{m.t}</p>
              </div>
            ))}
          </div>
        ) : <div className="muted">Aucun message. Lancez la discussion sur ce candidat.</div>}
        <div className="disc-compose">
          <textarea className="disc-input" placeholder="Écrire un message…" value={draft}
                    onChange={e => setDraft(e.target.value)}
                    onKeyDown={e => { if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) send(); }} />
          <div className="disc-foot">
            <span className="disc-parts">{(k.participants || []).length} participant{(k.participants || []).length > 1 ? "s" : ""}</span>
            <button className="bo-btn primary sm" onClick={send}>Envoyer</button>
          </div>
        </div>
      </div>
    </Block>
  );
}

function ValidationBlock({ c, onResolve }) {
  const [mode, setMode] = useState(null);   // null | reject
  const [msg, setMsg] = useState("");
  return (
    <section className="cblock cand-decide">
      <div className="cblock-head">Traiter le candidat</div>
      <div className="cblock-body">
        {mode === "reject" ? (
          <div className="cand-decide-form">
            <textarea className="disc-input" placeholder="Motif du rejet (visible par le proposant)…"
                      value={msg} onChange={e => setMsg(e.target.value)} />
            <div className="cand-decide-actions">
              <button className="bo-btn danger" onClick={() => onResolve && onResolve("rejeté")}>Confirmer le rejet</button>
              <button className="bo-btn ghost" onClick={() => setMode(null)}>Annuler</button>
            </div>
          </div>
        ) : (
          <div className="cand-decide-actions">
            <button className="bo-btn primary" onClick={() => onResolve && onResolve("ajouté")}><Icon name="check" className="ico-inline"/> Ajouter au thésaurus</button>
            <button className="bo-btn ghost danger" onClick={() => setMode("reject")}><Icon name="x" className="ico-inline"/> Rejeter</button>
          </div>
        )}
      </div>
    </section>
  );
}

// Historique de validation conservé sur un concept validé (issu d'une candidature).
function HistoryBlock({ c }) {
  const h = c.hist || {};
  const v = h.votes || { up: 0, down: 0 };
  return (
    <Block title="Historique de validation">
      <Row label="Proposé par">{h.by} <span className="muted">le {h.on}</span></Row>
      <Row label="Validé par"><b>{h.valBy}</b> <span className="muted">le {h.valOn}</span></Row>
      <Row label="Votes (à la candidature)">
        <span className="votes-c up">▲ {v.up}</span> <span className="votes-c down">▼ {v.down}</span>
        <span className="muted"> · archivés</span>
      </Row>
      <Row label="Participants">{(h.participants || []).join(", ") || "—"}</Row>
    </Block>
  );
}

// ── Paramètres de vue (options propres à la vue active) ──
// Deux patterns comparables : "popover" (bouton ⚙ sur la barre de vues) | "bar"
// (barre contextuelle sous le switcheur). Mêmes options, présentation différente.
function VOControl({ d }) {
  if (d.type === "seg") {
    return (
      <div className="vo-seg">
        {d.options.map(o => (
          <button key={o.v} className={"vo-seg-b" + (d.val === o.v ? " is-on" : "")}
                  onClick={() => d.set(o.v)}>{o.l}</button>
        ))}
      </div>
    );
  }
  return (
    <button className={"vo-sw" + (d.val ? " on" : "")} role="switch" aria-checked={!!d.val}
            onClick={() => d.set(!d.val)} />
  );
}

function ViewOptions({ ui, defs, viewLabel }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    if (ui !== "popover") return;
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, [ui]);
  if (!defs.length) return null;

  if (ui === "bar") {
    return (
      <div className="vo-bar">
        {defs.map(d => d.type === "statuses" ? (
          <div key={d.key} className="vo-bar-stk">
            <StatusChecklist set={d.val} onChange={d.set} perm={d.perm} counts={d.counts}
              candFilter={d.candFilter} onCandFilter={d.onCandFilter} depositors={d.depositors} />
          </div>
        ) : d.type === "custom" ? (
          <div key={d.key} className="vo-bar-stk">{d.render()}</div>
        ) : (
          <div key={d.key} className="vo-bar-item">
            <span className="vo-bar-l">{d.label}{d.badge != null && <span className="vo-badge">{d.badge}</span>}</span>
            <VOControl d={d} />
          </div>
        ))}
      </div>
    );
  }

  // popover
  return (
    <div className="vo-pop-wrap" ref={ref}>
      <button className={"vo-gear" + (open ? " is-on" : "")} title="Options de la vue"
              aria-expanded={open} onClick={() => setOpen(o => !o)}>
        <Icon name="settings" />
      </button>
      {open && (
        <div className="vo-pop">
          <div className="vo-pop-h">Options · {viewLabel}</div>
          {defs.map(d => d.type === "statuses" ? (
            <div key={d.key} className="vo-pop-stk">
              <StatusChecklist set={d.val} onChange={d.set} perm={d.perm} counts={d.counts}
                candFilter={d.candFilter} onCandFilter={d.onCandFilter} depositors={d.depositors} />
            </div>
          ) : d.type === "custom" ? (
            <div key={d.key} className="vo-pop-stk">{d.render()}</div>
          ) : (
            <div key={d.key} className="vo-pop-row">
              <span className="vo-pop-l">{d.label}{d.badge != null && <span className="vo-badge">{d.badge}</span>}</span>
              <VOControl d={d} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// FAC-2 — pseudo-concept de facette : l'état de sélection de l'appli reste
// un seul objet, quelle que soit la nature de ce qui est ouvert.
function facetConcept(f, path) {
  return { id: "§" + f.id, isFacet: true, facet: f, pref: f.label, path: path || [], type: "facette" };
}

// ══════════════════════════════════════════════════════════════
//  EDT-8 — Notes : type × langue, dans le bloc (jamais de popup).
//  Lecture : groupé par type, une ligne par langue.
//  Édition : onglets de langue à l'intérieur du bloc + « Comparer » pour
//  traduire une note en regardant l'autre langue, sans quitter l'écran.
// ══════════════════════════════════════════════════════════════
const NT_KEY = "ot.noteTypes";
let NTYPES = (() => {
  try { const v = JSON.parse(localStorage.getItem(NT_KEY)); if (Array.isArray(v) && v.length) return v; } catch (e) {}
  return (window.NOTE_TYPES || []).slice();
})();
const noteTypes = () => NTYPES;
function setNoteTypes(l) { NTYPES = l.slice(); try { localStorage.setItem(NT_KEY, JSON.stringify(NTYPES)); } catch (e) {} otSync(); }
const noteType = (k) => NTYPES.find(t => t.k === k) || { k, label: k };

function NotesRead({ notes }) {
  const list = (notes || []).filter(n => (n.text || "").trim());
  if (!list.length) return <div className="muted">Aucune note.</div>;
  return (
    <>
      {noteTypes().map(t => {
        const rows = list.filter(n => n.type === t.k);
        if (!rows.length) return null;
        return (
          <div className="note-grp" key={t.k}>
            <div className="note-grp-t">{t.label}</div>
            {rows.map((n, i) => (
              <div key={i} className="note-line">
                <span className="tr-code">{n.lang}</span>
                <div className="note-line-b">
                  <p className="cv-def" lang={n.lang} dir={dirOf(n.lang)}>{n.text}</p>
                  {n.source && <div className="note-src">{n.source}</div>}
                </div>
              </div>
            ))}
          </div>
        );
      })}
    </>
  );
}

// Une note = une carte : son TYPE est un choix, pas une case pré-imprimée, et
// toute note porte une langue. Les langues s'ajoutent depuis le bloc.
// « Comparer » aligne la langue de travail sur une langue de référence et
// montre ce qui manque.
// Traduction automatique : le proto n'embarque pas de moteur. On réutilise la
// traduction du jeu de démo quand elle existe, sinon on insère le texte source
// INCHANGÉ — jamais de charabia mi-français mi-allemand, qui se lirait comme un
// bug en revue. Ce qui porte le sens, c'est le statut « à relire ».
const machineTranslate = (text, to, concept, type) => {
  const src = String(text || "");
  const real = ((concept && concept.notes) || []).find(n => n.lang === to && n.type === type && (n.text || "").trim());
  return real ? real.text : src;
};

function NotesEditor({ concept, value, onChange }) {
  const list = value || [];
  const types = noteTypes();
  const [extra, setExtra] = useState([]);
  const langs = Array.from(new Set(["fr", ...Object.keys(concept.tr || {}),
    ...list.map(n => n.lang), ...extra]));
  const [lang, setLang] = useState("fr");
  const [cmp, setCmp] = useState(null);
  const [menu, setMenu] = useState(null);          // "lang" | "type"
  const upd = (i, patch) => onChange(list.map((n, j) => j === i ? { ...n, ...patch } : n));
  const drop = (i) => onChange(list.filter((_, j) => j !== i));
  const add = (k, l, text, source, mt) => onChange(list.concat([
    { type: k, lang: l, text: text || "", source: source || "", ...(mt ? { mt: true } : {}) }]));
  const rows = (l) => list.map((n, i) => ({ n, i })).filter(({ n }) => n.lang === l);
  const count = (l) => rows(l).filter(({ n }) => (n.text || "").trim()).length;
  const pickLang = (l) => { setLang(l); if (cmp === l) setCmp(langs.find(x => x !== l) || null); };

  const card = ({ n, i }) => (
    <div className="note-c" key={i}>
      <div className="note-c-h">
        <select className="note-c-type" value={n.type} onChange={e => upd(i, { type: e.target.value })}>
          {types.map(x => <option key={x.k} value={x.k}>{x.label}</option>)}
        </select>
        <input className="note-c-src" value={n.source || ""} placeholder="Source…"
               onChange={e => upd(i, { source: e.target.value })} />
        <button className="note-ed-x" title="Supprimer cette note" onClick={() => drop(i)}><Icon name="x" className="ico-inline"/></button>
      </div>
      <textarea className="disc-input" value={n.text} lang={n.lang} dir={dirOf(n.lang)}
                placeholder={noteType(n.type).label + "…"} onChange={e => upd(i, { text: e.target.value })}></textarea>
      {n.mt && (
        <div className="note-mt">
          <span className="note-mt-l">Traduction automatique, à relire</span>
          <button className="note-mt-ok" onClick={() => upd(i, { mt: false })}>Relu</button>
        </div>
      )}
    </div>
  );

  const addBtn = (l) => (
    <div className="note-menu-w">
      <button className="note-ed-add" onClick={() => setMenu(menu === "type" ? null : "type")}>+ Ajouter une note</button>
      {menu === "type" && (
        <div className="note-menu">
          {types.map(t => (
            <button key={t.k} onClick={() => { add(t.k, l); setMenu(null); }}>{t.label}</button>
          ))}
        </div>
      )}
    </div>
  );

  // Vue comparée : chaque note de la langue de référence est alignée sur sa
  // contrepartie ; ce qui manque dans la langue de travail se voit.
  const pairs = () => {
    const ref = rows(cmp), act = rows(lang), taken = new Set();
    const out = ref.map(r => {
      const m = act.find(a => a.n.type === r.n.type && !taken.has(a.i));
      if (m) taken.add(m.i);
      return { ref: r, act: m || null };
    });
    act.filter(a => !taken.has(a.i)).forEach(a => out.push({ ref: null, act: a }));
    return out;
  };

  return (
    <div className="note-ed">
      <div className="note-ed-bar">
        <div className="note-ed-tabs">
          {langs.map(l => (
            <button key={l} className={"note-ed-tab" + (l === lang ? " on" : "")} onClick={() => pickLang(l)}>
              {ENDONYM[l] || l}
              {count(l) ? <span className="note-ed-n">{count(l)}</span>
                        : <span className="note-ed-void" title="Aucune note dans cette langue" />}
            </button>
          ))}
          <div className="note-menu-w">
            <button className="note-ed-addl" onClick={() => setMenu(menu === "lang" ? null : "lang")}>+ Langue</button>
            {menu === "lang" && (
              <div className="note-menu">
                {Object.keys(LANGNAME).filter(l => !langs.includes(l)).map(l => (
                  <button key={l} onClick={() => { setExtra(extra.concat([l])); setLang(l); setMenu(null); }}>
                    {ENDONYM[l] || LANGNAME[l]}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
        <button className={"note-ed-cmp" + (cmp ? " on" : "")}
                onClick={() => setCmp(cmp ? null : (langs.find(l => l !== lang) || null))}>
          {cmp ? "Masquer" : "Comparer avec…"}</button>
      </div>

      {!cmp ? (
        <div className="note-ed-col">
          {rows(lang).map(r => card(r))}
          {addBtn(lang)}
        </div>
      ) : (
        <div className="note-cmp">
          <div className="note-cmp-h">
            <div className="note-ed-colh">{ENDONYM[lang] || lang}</div>
            <div className="note-ed-colh">
              Comparé à{" "}
              <select className="re-hsel" value={cmp} onChange={e => setCmp(e.target.value)}>
                {langs.filter(l => l !== lang).map(l => <option key={l} value={l}>{ENDONYM[l] || l}</option>)}
              </select>
            </div>
          </div>
          {pairs().map((p, k) => (
            <div className="note-cmp-r" key={k}>
              <div>
                {p.act ? card(p.act) : (
                  <div className="note-ghost">
                    <div className="note-c-h"><span className="note-ghost-t">{noteType(p.ref.n.type).label}</span></div>
                    <div className="note-ghost-a">
                      <button className="note-gh-b primary"
                              onClick={() => add(p.ref.n.type, lang, machineTranslate(p.ref.n.text, lang, concept, p.ref.n.type), p.ref.n.source, true)}>
                        Traduire automatiquement</button>
                      <button className="note-gh-b"
                              onClick={() => add(p.ref.n.type, lang, p.ref.n.text, p.ref.n.source)}>
                        Copier le texte {ENDONYM[cmp] ? "(" + ENDONYM[cmp] + ")" : ""}</button>
                      <button className="note-gh-b"
                              onClick={() => add(p.ref.n.type, lang)}>Rédiger moi-même</button>
                    </div>
                  </div>
                )}
              </div>
              <div>
                {p.ref ? (
                  <div className="note-ref">
                    <div className="note-c-h"><span className="note-ghost-t">{noteType(p.ref.n.type).label}</span></div>
                    <p className="cv-def" lang={cmp} dir={dirOf(cmp)}>{p.ref.n.text}</p>
                    {p.ref.n.source && <div className="note-src">{p.ref.n.source}</div>}
                  </div>
                ) : <div className="note-ref is-empty">—</div>}
              </div>
            </div>
          ))}
          {addBtn(lang)}
        </div>
      )}
    </div>
  );
}

function Row({ label, children, mono }) {
  return (
    <div className="crow">
      <div className="crow-lbl">{label}</div>
      <div className={"crow-val" + (mono ? " is-mono" : "")}>{children}</div>
    </div>
  );
}

// EDT-3 — unicité du NOM (libellé préférentiel) SEUL, langue par langue :
// un nom déjà porté par un autre concept dans la même langue est refusé. Les
// formes alternatives ne sont pas concernées (décision du 02/09).
function checkLabel(label, lang, selfId) {
  const s = (label || "").trim();
  if (!s) return null;
  const all = (window.THESAURUS || {}).concepts || [];
  const nameIn = (x, l) => l === "fr" ? x.pref : ((x.tr || {})[l] || "");
  const hit = all.find(x => x.id !== selfId && norm(nameIn(x, lang)) === norm(s));
  if (hit) return { level: "error", target: hit,
    msg: "Ce nom est déjà celui de « " + LBL(hit.pref) + " » (" + hit.notation + ") en " + (LANGNAME[lang] || lang).toLowerCase() + "." };
  return null;
}

// Champ de nom avec contrôle d'unicité joué à la saisie.
function LabelField({ value, lang = "fr", selfId, placeholder, onTerm }) {
  const [v, setV] = useState(value || "");
  const r = useMemo(() => checkLabel(v, lang, selfId), [v, lang, selfId]);
  return (
    <div className="lf">
      <input className={"st-input" + (r ? " is-" + r.level : "")} value={v} placeholder={placeholder}
             onChange={e => setV(e.target.value)} />
      {r && (
        <div className={"re-msg re-" + r.level}>{r.msg}
          {onTerm && <button className="lf-see" onClick={() => onTerm(r.target.pref)}>Voir ce concept</button>}
        </div>
      )}
    </div>
  );
}

// EDT-6 — listes longues : au-delà du seuil, on n'affiche que les premiers
// et on filtre au lieu de scroller.
function RelRow({ label, code, kind, items, types, onTerm, cap = 10 }) {
  const [all, setAll] = useState(false);
  const [f, setF] = useState("");
  const list = items || [];
  const long = list.length > cap;
  const filtered = f.trim() ? list.filter(t => norm(LBL(t)).includes(norm(f))) : list;
  const shown = all || f.trim() ? filtered : filtered.slice(0, cap);
  return (
    <div className="crow crow-rel">
      <div className="crow-lbl">
        <span className={"rel-code rel-" + kind}>{code}</span>{label}
        {long && <span className="rel-count">{list.length}</span>}
      </div>
      <div className="crow-val">
        {list.length > 0 ? (
          <>
            {long && all && (
              <input className="st-input rel-filter" value={f} onChange={e => setF(e.target.value)}
                     placeholder={"Filtrer parmi " + list.length + " termes…"} />
            )}
            <div className="rel-items">
              {shown.map((t, i) => {
                const ht = types && types[t];
                return (
                  <span key={i} className="rel-item">
                    <button className="rel-term" onClick={() => onTerm(t)}>{LBL(t)}</button>
                    {ht && ht !== "BTG" && <span className="rel-btype" title={"Relation " + hierMeta(ht).label.toLowerCase()}>{hierMeta(ht).short}</span>}
                  </span>
                );
              })}
            </div>
            {long && (
              <button className="rel-more" onClick={() => { setAll(a => !a); setF(""); }}>
                {all ? "Réduire" : "Voir les " + list.length}
              </button>
            )}
            {f.trim() && filtered.length === 0 && <div className="re-none">Aucun terme ne correspond.</div>}
          </>
        ) : <span className="muted">—</span>}
      </div>
    </div>
  );
}

// ═════════════════════════════════════════════════════════════════
//  EDT-2 — Édition des relations : UNE famille à la fois, UNE relation
//  à la fois. Chaque ajout se fait par RECHERCHE dans les termes du
//  thésaurus et déclenche un contrôle de cohérence avant enregistrement.
//  Composant partagé entre l'édition d'un concept et sa création.
// ═════════════════════════════════════════════════════════════════
const REL_KINDS = [
  { k: "bt", code: "TG", label: "Concept générique", one: true },
  { k: "nt", code: "TS", label: "Concept spécifique" },
  { k: "rt", code: "TA", label: "Concept associé" }
];
const REL_LABEL = Object.fromEntries(REL_KINDS.map(r => [r.k, r]));

// Contrôle de cohérence joué à CHAQUE ajout (simulé sur les données de démo).
function checkRelation(concept, kind, targetLabel, current) {
  const all = (window.THESAURUS || {}).concepts || [];
  const t = all.find(x => norm(LBL(x.pref)) === norm(targetLabel) || norm(x.pref) === norm(targetLabel));
  const me = concept || {};
  if (!t) return { level: "error", msg: "Ce terme n'existe pas dans le thésaurus. Crée-le d'abord, ou choisis-en un dans la liste." };
  if (t.id === me.id) return { level: "error", msg: "Un concept ne peut pas être en relation avec lui-même." };
  const already = Object.entries(current).find(([k, arr]) => (arr || []).some(x => norm(x) === norm(t.pref)));
  if (already) {
    const k = already[0];
    return k === kind
      ? { level: "error", msg: "Cette relation existe déjà." }
      : { level: "error", msg: "« " + LBL(t.pref) + " » est déjà " + REL_LABEL[k].label.toLowerCase() + " de ce concept — une même paire ne peut pas porter deux relations." };
  }
  if (kind === "nt" && (me.path || []).some(p => norm(p) === norm(t.pref)))
    return { level: "error", msg: "« " + LBL(t.pref) + " » est un ancêtre de ce concept : le mettre en spécifique créerait une boucle." };
  if (kind === "bt" && (current.nt || []).some(x => norm(x) === norm(t.pref)))
    return { level: "error", msg: "« " + LBL(t.pref) + " » est déjà un spécifique de ce concept : inverser la relation créerait une boucle." };
  if (kind === "bt" && (current.bt || []).length >= 1)
    return { level: "warn", msg: "Ce concept aura plusieurs génériques (polyhiérarchie). Autorisé, mais à vérifier.", target: t };
  if (t.status === "deprecie" || t.status === "rejete")
    return { level: "warn", msg: "« " + LBL(t.pref) + " » est " + (t.status === "rejete" ? "rejeté" : "déprécié") + " : la relation sera créée mais signalée.", target: t };
  return { level: "ok", target: t };
}

function RelationEditor({ concept, value, onChange }) {
  const [q, setQ] = useState("");
  const [kind, setKind] = useState("nt");
  const [msg, setMsg] = useState(null);
  const [saved, setSaved] = useState(null);
  const [up, setUp] = useState(false);
  const [filter, setFilter] = useState("");
  const [newH, setNewH] = useState("BTG");
  const boxRef = useRef(null);
  const all = (window.THESAURUS || {}).concepts || [];
  const cur = value || {};

  // Une seule liste, toutes familles confondues : le type est une colonne.
  const rows = REL_KINDS.flatMap(r => (cur[r.k] || []).map(label => ({ k: r.k, label })));

  const hits = useMemo(() => {
    if (q.trim().length < 2) return [];
    return searchConcepts(q, all).filter(x => x.id !== (concept || {}).id).slice(0, 6);
  }, [q, all, concept]);

  const flash = (label) => { setSaved(label); setTimeout(() => setSaved(s => s === label ? null : s), 1600); };

  const add = (label) => {
    const r = checkRelation(concept, kind, label, cur);
    if (r.level === "error") { setMsg(r); return; }
    onChange({ ...cur, [kind]: (cur[kind] || []).concat([r.target.pref]),
               htype: kind === "rt" ? (cur.htype || {}) : { ...(cur.htype || {}), [r.target.pref]: newH } });
    setQ(""); setMsg(r.level === "warn" ? r : null); flash(r.target.pref);
  };
  const remove = (row) => onChange({ ...cur, [row.k]: (cur[row.k] || []).filter(x => x !== row.label) });
  const retype = (row, k2) => {
    if (k2 === row.k) return;
    const r = checkRelation(concept, k2, row.label, { ...cur, [row.k]: (cur[row.k] || []).filter(x => x !== row.label) });
    if (r.level === "error") { setMsg(r); return; }
    onChange({ ...cur, [row.k]: (cur[row.k] || []).filter(x => x !== row.label),
                       [k2]: (cur[k2] || []).concat([row.label]) });
    setMsg(r.level === "warn" ? r : null); flash(row.label);
  };
  const placeSide = () => {
    const el = boxRef.current; if (!el) return;
    const r = el.getBoundingClientRect();
    const sc = el.closest(".view");
    const b = sc ? sc.getBoundingClientRect().bottom : window.innerHeight;
    const t = sc ? sc.getBoundingClientRect().top : 0;
    setUp(b - r.bottom < 240 && r.top - t > 240);
  };

  const TypeSelect = ({ v, onPick }) => {
    const [open, setOpen] = useState(false);
    const ref = useRef(null);
    useEffect(() => {
      if (!open) return;
      const away = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
      document.addEventListener("mousedown", away);
      return () => document.removeEventListener("mousedown", away);
    }, [open]);
    const m = REL_LABEL[v];
    return (
      <span className="re-type" ref={ref}>
        <button type="button" className={"re-pill rel-" + v} title={m.label} aria-haspopup="listbox"
                aria-expanded={open} onClick={() => setOpen(o => !o)}>
          {m.code}<span className="re-caret" aria-hidden="true"></span>
        </button>
        {open && (
          <div className="re-menu" role="listbox">
            {REL_KINDS.map(r => (
              <button key={r.k} role="option" aria-selected={r.k === v}
                      className={"re-mi" + (r.k === v ? " on" : "")}
                      onClick={() => { setOpen(false); onPick(r.k); }}>
                <span className={"rel-code rel-" + r.k}>{r.code}</span>{r.label}
              </button>
            ))}
          </div>
        )}
      </span>
    );
  };

  const long = rows.length > 10;
  const vis = filter.trim() ? rows.filter(r => norm(LBL(r.label)).includes(norm(filter))) : rows;

  return (
    <div className="re">
      {long && (
        <input className="st-input re-filter" value={filter} onChange={e => setFilter(e.target.value)}
               placeholder={"Filtrer parmi " + rows.length + " relations…"} />
      )}
      <div className="re-rows">
        {vis.map(row => (
          <div className={"re-row" + (saved === row.label ? " is-saved" : "")} key={row.k + "|" + row.label}>
            <TypeSelect v={row.k} onPick={k2 => retype(row, k2)} />
            {row.k !== "rt" &&
              <HierSelect value={(cur.htype || {})[row.label]}
                          onChange={v2 => { onChange({ ...cur, htype: { ...(cur.htype || {}), [row.label]: v2 } }); flash(row.label); }} />}
            <span className="re-lbl" title={LBL(row.label)}>{LBL(row.label)}</span>
            <button className="re-x" onClick={() => remove(row)} title="Retirer cette relation" aria-label="Retirer"><Icon name="x" className="ico-inline"/></button>
          </div>
        ))}
        <div className={"re-row re-new" + (up ? " is-up" : "")} ref={boxRef}>
          <TypeSelect v={kind} onPick={setKind} />
          {kind !== "rt" && <HierSelect value={newH} onChange={setNewH} />}
          <div className="re-field">
            <input className="st-input re-q" value={q} autoComplete="off" placeholder="Ajouter un concept…"
                   onChange={e => { setQ(e.target.value); setMsg(null); placeSide(); }}
                   onKeyDown={e => { if (e.key === "Enter" && hits[0]) { e.preventDefault(); add(hits[0].pref); } }} />
            {hits.length > 0 && (
              <div className="re-drop">
                {hits.map(h => (
                  <button className="re-hit" key={h.id} onClick={() => add(h.pref)}>
                    <span className="re-hit-tx">
                      <span className="re-hit-l">{LBL(h.pref)}</span>
                      <span className="re-hit-p">{(h.path || []).join(" › ")}</span>
                    </span>
                    <span className="re-hit-n">{h.notation}</span>
                  </button>
                ))}
              </div>
            )}
            {q.trim().length >= 2 && hits.length === 0 &&
              <div className="re-none">Aucun terme ne correspond dans {(window.THESAURUS || {}).name}.</div>}
          </div>
          <span className="re-x re-x-ghost" aria-hidden="true"></span>
        </div>
      </div>
      {msg && <div className={"re-msg re-" + msg.level}>{msg.msg}</div>}
    </div>
  );
}

// ═════════════════════════════════════════════════════════════════
//  EDT-9 — Traductions : une langue à la fois, ajout / retrait de langue,
//  contrôle d'unicité du nom traduit (EDT-3) langue par langue.
// ═════════════════════════════════════════════════════════════════
const TR_LANGS = ["en", "de", "es", "it", "nl", "pt", "el", "ar", "ca", "eu"];
const RTL = new Set(["ar", "he", "fa", "ur"]);
const dirOf = (l) => RTL.has(l) ? "rtl" : "ltr";

function TranslationEditor({ concept, value, onChange, onTerm }) {
  const [pick, setPick] = useState("");
  const [confirm, setConfirm] = useState(null);
  const v = value || { tr: {}, trAlt: {} };
  const langs = Object.keys(v.tr || {});
  const free = TR_LANGS.filter(l => !langs.includes(l));

  const setTr = (l, label) => onChange({ ...v, tr: { ...v.tr, [l]: label } });
  const setAlt = (l, s) => onChange({ ...v, trAlt: { ...v.trAlt, [l]: s.split(",").map(x => x.trim()).filter(Boolean) } });
  const addLang = (l) => { if (!l) return; onChange({ ...v, tr: { ...v.tr, [l]: "" } }); setPick(""); };
  const dropLang = (l) => {
    const tr = { ...v.tr }, trAlt = { ...v.trAlt };
    delete tr[l]; delete trAlt[l];
    onChange({ ...v, tr, trAlt }); setConfirm(null);
  };

  return (
    <div className="te">
      {langs.length === 0 && <div className="re-none">Aucune traduction. Ajoute une langue ci-dessous.</div>}
      {langs.map(l => {
        const dup = checkLabel(v.tr[l], l, (concept || {}).id);
        return (
          <div className="te-lang" key={l}>
            <div className="te-head">
              {FLAG[l] && <span className="tr-flag">{FLAG[l]}</span>}
              <span className="te-name">{LANGNAME[l] || l}</span>
              <span className="te-code">{l}</span>
              {confirm === l ? (
                <span className="te-confirm">
                  Supprimer cette traduction ?
                  <button className="bo-btn ghost sm" onClick={() => setConfirm(null)}>Non</button>
                  <button className="bo-btn primary sm" onClick={() => dropLang(l)}>Oui</button>
                </span>
              ) : (
                <button className="re-x te-x" title={"Retirer le " + (LANGNAME[l] || l)}
                        aria-label="Retirer cette langue" onClick={() => setConfirm(l)}><Icon name="x" className="ico-inline"/></button>
              )}
            </div>
            <input className={"st-input" + (dup ? " is-" + dup.level : "")} value={v.tr[l] || ""}
                   lang={l} dir={dirOf(l)}
                   placeholder={"Nom en " + (LANGNAME[l] || l).toLowerCase() + "…"}
                   onChange={e => setTr(l, e.target.value)} />
            {dup && (
              <div className={"re-msg re-" + dup.level}>{dup.msg}
                {onTerm && <button className="lf-see" onClick={() => onTerm(dup.target.pref)}>Voir ce concept</button>}
              </div>
            )}
            <input className="st-input te-alt" value={((v.trAlt || {})[l] || []).join(", ")}
                   lang={l} dir={dirOf(l)}
                   placeholder="Formes alternatives, séparées par des virgules"
                   onChange={e => setAlt(l, e.target.value)} />
          </div>
        );
      })}
      {free.length > 0 && (
        <div className="te-add">
          <select className="st-input te-sel" value={pick} onChange={e => addLang(e.target.value)}>
            <option value="">Ajouter une langue…</option>
            {free.map(l => <option key={l} value={l}>{ENDONYM[l] || LANGNAME[l] || l}</option>)}
          </select>
        </div>
      )}
    </div>
  );
}

// Formulaire de saisie d'un candidat brouillon (tout est renseignable).
function FormBlock({ title, children }) {
  return <section className="cblock"><div className="cblock-head">{title}</div><div className="cblock-body">{children}</div></section>;
}
function DField({ label, children }) {
  return <div className="crow"><div className="crow-lbl">{label}</div><div className="crow-val">{children}</div></div>;
}
// CRE-2/3/EDT-9 — Formulaire de création : mêmes blocs que la fiche d'un
// concept existant (décision 22-09, "les mêmes champs, point final"), en une
// seule page — obligatoire toujours visible en haut, le reste en divulgation
// progressive. Barre flottante : Créer / Créer et créer le suivant (chaîne) /
// Annuler, avec confirmation si des champs sont déjà remplis.
function DraftForm({ c, onResolve }) {
  const kind = c.draftKind || "candidat";
  const isFacette = kind === "facette";
  const needDef = kind === "concept";   // CRE-2 : concepts seulement

  const [pref, setPref] = useState("");
  const [errPref, setErrPref] = useState(false);
  const [def, setDef] = useState("");
  const [errDef, setErrDef] = useState(false);
  const [alts, setAlts] = useState("");
  const [altsHidden, setAltsHidden] = useState("");
  const [collection, setCollection] = useState(c.collection !== "—" ? c.collection : "");
  const [note, setNote] = useState("");
  const [resUrl, setResUrl] = useState("");
  const [resImg, setResImg] = useState("");
  const [resGps, setResGps] = useState("");
  const [draftRel, setDraftRel] = useState({ bt: (c.bt || []).slice(0, 1), nt: [], rt: [],
    htype: Object.fromEntries((c.bt || []).slice(0, 1).map(p => [p, "BTG"])) });
  const [draftTrad, setDraftTrad] = useState({ tr: {}, trAlt: {} });
  const [draftCls, setDraftCls] = useState([]);
  const [more, setMore] = useState(false);
  const [confirmLeave, setConfirmLeave] = useState(false);

  // Nouveau brouillon (chaîne, ou parent différent) : repart d'une page vierge —
  // même pattern que ConceptFiche pour resynchroniser l'état sur un changement de c.id.
  useEffect(() => {
    setPref(""); setErrPref(false);
    setDef(""); setErrDef(false);
    setAlts(""); setAltsHidden("");
    setCollection(c.collection !== "—" ? c.collection : "");
    setNote(""); setResUrl(""); setResImg(""); setResGps("");
    setDraftRel({ bt: (c.bt || []).slice(0, 1), nt: [], rt: [],
      htype: Object.fromEntries((c.bt || []).slice(0, 1).map(p => [p, "BTG"])) });
    setDraftTrad({ tr: {}, trAlt: {} });
    setDraftCls([]);
    setMore(false); setConfirmLeave(false);
  }, [c.id]);

  const label = { concept: "Créer le concept", candidat: "Créer le candidat", facette: "Créer la facette" }[kind];
  const chainLabel = kind === "candidat" ? "Créer et créer le candidat suivant" : "Créer et créer le concept suivant";
  const locLabel = draftRel.bt[0] ? "Créé sous « " + LBL(draftRel.bt[0]) + " »" : "Créé à la racine du thésaurus";

  const dirty = !!(pref.trim() || def.trim() || alts.trim() || altsHidden.trim() || collection.trim()
    || note.trim() || resUrl.trim() || resImg.trim() || resGps.trim()
    || draftRel.nt.length || draftRel.rt.length || Object.keys(draftTrad.tr || {}).length || draftCls.length);

  // Reporté à app.jsx : bloquer une navigation qui perdrait ce brouillon.
  useEffect(() => { setDraftDirty(dirty); return () => setDraftDirty(false); }, [dirty]);

  const validate = () => {
    let ok = true;
    if (!pref.trim()) { setErrPref(true); ok = false; }
    if (needDef && !def.trim()) { setErrDef(true); ok = false; }
    return ok;
  };
  const submit = (chain) => {
    if (!validate()) return;
    setDraftDirty(false);
    const data = {
      pref: pref.trim(), def: def.trim(),
      alts: alts.split(",").map(s => s.trim()).filter(Boolean),
      altsHidden: altsHidden.split(",").map(s => s.trim()).filter(Boolean),
      collection: collection.trim(), note: note.trim(),
      resUrl: resUrl.trim(), resImg: resImg.trim(), resGps: resGps.trim(),
      rel: draftRel, trad: draftTrad, cls: draftCls
    };
    onResolve && onResolve("créé", { chain: !!chain, data });
  };
  const requestLeave = () => { if (dirty) setConfirmLeave(true); else onResolve && onResolve("annulé"); };

  return (
    <>
      <div className="cv-head">
        <h1 className="cv-pref">
          {kind === "candidat" && <span className="cv-dot" title="Candidat" />}
          <input className={"cv-draft-input" + (errPref ? " is-err" : "")} autoFocus
                 value={pref} onChange={e => { setPref(e.target.value); if (errPref) setErrPref(false); }}
                 placeholder={{ concept: "Nom du concept…", candidat: "Nom du candidat…", facette: "Nom de la facette…" }[kind]} />
          <span className="cv-lang">{DISPLAY_LANG}</span>
          <span className={"cv-status" + (kind !== "candidat" ? " is-direct" : "")}>brouillon</span>
        </h1>
        {errPref && <div className="df-err">Le nom est obligatoire.</div>}
        {!isFacette && <p className="cv-draft-loc">{locLabel}</p>}
      </div>

      <FormBlock title="Définition">
        <DField label={<>Définition{needDef && <span className="tp-req"> *</span>}</>}>
          <textarea className={"disc-input" + (errDef ? " is-err" : "")} value={def}
            onChange={e => { setDef(e.target.value); if (errDef) setErrDef(false); }}
            placeholder={isFacette ? "Définition de la facette…" : "Définition du concept…"}></textarea>
          {errDef && <div className="df-err">Un concept ne peut pas être créé sans définition.</div>}
        </DField>
        {!isFacette && !draftRel.bt.length && (
          <div className="df-warn">Aucun terme générique — le {kind === "candidat" ? "candidat" : "concept"} sera créé à la racine.</div>
        )}
      </FormBlock>

      <button type="button" className={"df-more-toggle" + (more ? " open" : "")} onClick={() => setMore(m => !m)}>
        <Icon name="chevron-right" />{more ? "Masquer les autres informations" : "Autres informations (facultatif)"}
      </button>

      {more && (
        <>
          <FormBlock title="Libellé">
            <DField label="Formes alternatives">
              <input className="st-input" value={alts} onChange={e => setAlts(e.target.value)} placeholder="synonymes, séparés par des virgules" /></DField>
            {!isFacette && (
              <DField label="Formes cachées">
                <input className="st-input" value={altsHidden} onChange={e => setAltsHidden(e.target.value)} placeholder="fautes, pluriels, graphies (non affichées)" /></DField>
            )}
          </FormBlock>

          <FormBlock title="Collections">
            <DField label="+ Ajouter à une collection">
              <input className="st-input" value={collection} onChange={e => setCollection(e.target.value)} placeholder="Nom de la collection…" /></DField>
          </FormBlock>

          {!isFacette && (
            <FormBlock title="Relations sémantiques">
              <RelationEditor concept={c} value={draftRel} onChange={setDraftRel} />
            </FormBlock>
          )}
          {!isFacette && clsEnabled() && (
            <FormBlock title="Classes de concept">
              <ClassPicker value={draftCls} onChange={setDraftCls} />
            </FormBlock>
          )}
          {!isFacette && (
            <FormBlock title="Traductions">
              <TranslationEditor concept={c} value={draftTrad} onChange={setDraftTrad} />
            </FormBlock>
          )}

          <FormBlock title="Notes">
            <DField label="Note d'application">
              <textarea className="disc-input" value={note} onChange={e => setNote(e.target.value)} placeholder="Note d'application…"></textarea></DField>
          </FormBlock>

          <FormBlock title="Ressources externes">
            <DField label="Ressources externes"><input className="st-input" value={resUrl} onChange={e => setResUrl(e.target.value)} placeholder="URL…" /></DField>
            <DField label="Image (URI)"><input className="st-input" value={resImg} onChange={e => setResImg(e.target.value)} placeholder="https://…" /></DField>
            <DField label="Coordonnées GPS"><input className="st-input" value={resGps} onChange={e => setResGps(e.target.value)} placeholder="48.922548 2.145240" /></DField>
          </FormBlock>
        </>
      )}

      <div className="fsave">
        <button type="button" className="bo-btn ghost" onClick={requestLeave}>Annuler</button>
        {!isFacette && <button type="button" className="bo-btn ghost" onClick={() => submit(true)}>{chainLabel}</button>}
        <button type="button" className="bo-btn primary" onClick={() => submit(false)}>{label}</button>
      </div>

      {confirmLeave && (
        <Modal title="Quitter sans enregistrer ?" onClose={() => setConfirmLeave(false)}
               actions={<>
                 <button className="bo-btn ghost" onClick={() => setConfirmLeave(false)}>Rester sur le brouillon</button>
                 <button className="bo-btn danger" onClick={() => { setConfirmLeave(false); onResolve && onResolve("annulé"); }}>Quitter sans enregistrer</button>
               </>}>
          <p className="mo-warn">Les informations déjà saisies pour ce {kind === "facette" ? "facette" : kind} seront perdues.</p>
        </Modal>
      )}
    </>
  );
}

// Pseudo-QR décoratif (identifiant pérenne).
function QRGlyph() {
  const cells = [];
  let seed = 7;
  const rnd = () => (seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
  for (let y = 0; y < 11; y++) for (let x = 0; x < 11; x++) {
    const finder = (x < 3 && y < 3) || (x > 7 && y < 3) || (x < 3 && y > 7);
    if (finder || rnd() > 0.5) cells.push(<rect key={x + "-" + y} x={x * 5} y={y * 5} width="5" height="5" />);
  }
  return <svg viewBox="0 0 55 55" className="qr-svg">{cells}</svg>;
}

// ═════════════════════════════════════════════════════════════════
//  HIS-1..5 — Journal des modifications. UN seul composant, deux portées :
//  la vue Historique du thésaurus (tous les concepts, groupés par concept) et
//  le bloc Historique de la fiche (un seul concept, groupé par jour).
//  Lecture seule : le journal informe, il ne défait rien (décision du 02/09).
//  ⚠ Les 12 types d'élément sont ceux du redesign ; les « 4 groupes »
//  historisés par l'OpenTheso actuel restent à vérifier avec Miled.
// ═════════════════════════════════════════════════════════════════
const HIST_ELEMENTS = [
  { k: "pref",   label: "Nom" },
  { k: "alt",    label: "Forme alternative" },
  { k: "althid", label: "Forme cachée" },
  { k: "tr",     label: "Traduction" },
  { k: "note",   label: "Note" },
  { k: "rel",    label: "Relation" },
  { k: "btype",  label: "Type de relation" },
  { k: "align",  label: "Alignement" },
  { k: "cls",    label: "Classe de concept" },
  { k: "status", label: "Statut" },
  { k: "nota",   label: "Notation" },
  { k: "coll",   label: "Collection" },
  { k: "move",   label: "Déplacement" }
];
const histEl = (k) => HIST_ELEMENTS.find(e => e.k === k) || { k, label: k };
const HIST_ACTION = { create: "ajouté", update: "modifié", delete: "supprimé" };

// Pas de restauration depuis l'historique (décision 08-09, trop complexe ;
// retirée le 23-09) : l'historique se consulte, il ne s'édite pas.
// scope : "th" (vue du thésaurus, colonne Concept) | "concept" (bloc de fiche)
// Les filtres vivent DANS les colonnes, comme dans la vue Tableau.
const HIST_COLS = [
  { key: "date",    label: "Date",         mono: true, get: h => h.date },
  { key: "concept", label: "Concept",      get: h => h.concept, th: true },
  { key: "el",      label: "Élément",      get: h => histEl(h.el).label },
  { key: "lang",    label: "Langue",       mono: true, get: h => h.lang },
  { key: "action",  label: "Action",       get: h => HIST_ACTION[h.action] },
  { key: "diff",    label: "Modification", get: h => (h.before || "") + " " + (h.after || "") },
  { key: "user",    label: "Utilisateur",  mono: true, get: h => h.user }
];

function HistoryLog({ entries, scope = "th", onOpen }) {
  const [f, setF] = React.useState({ concept: "", el: "", lang: "", action: "", diff: "", user: "", from: "", to: "" });
  const [sort, setSort] = React.useState({ col: "date", dir: -1 });
  const all = entries || [];
  const langs = Array.from(new Set(all.map(h => h.lang).filter(Boolean))).sort();
  const users = Array.from(new Set(all.map(h => h.user))).sort();
  const els = HIST_ELEMENTS.filter(e => all.some(h => h.el === e.k));
  const cols = HIST_COLS.filter(c => scope === "th" || !c.th);
  const set = (k, v) => setF({ ...f, [k]: v });
  const rows = all.filter(h =>
    (!f.el || h.el === f.el) && (!f.lang || h.lang === f.lang) && (!f.user || h.user === f.user) &&
    (!f.action || h.action === f.action) &&
    (!f.concept || norm(LBL(h.concept)).includes(norm(f.concept))) &&
    (!f.diff || norm((h.before || "") + " " + (h.after || "")).includes(norm(f.diff))) &&
    (!f.from || h.date.slice(0, 10) >= f.from) && (!f.to || h.date.slice(0, 10) <= f.to));
  const col = HIST_COLS.find(c => c.key === sort.col) || HIST_COLS[0];
  const sorted = rows.slice().sort((a, b) => sort.dir * String(col.get(a)).localeCompare(String(col.get(b)), "fr"));
  const toggleSort = (k) => setSort(s => s.col === k ? { col: k, dir: -s.dir } : { col: k, dir: 1 });
  const sel = (k, opts, label) => (
    <select className="flat-fi hi-fsel" value={f[k]} aria-label={label}
            onClick={e => e.stopPropagation()} onChange={e => set(k, e.target.value)}>
      <option value="">Tous</option>
      {opts.map(o => <option key={o.v} value={o.v}>{o.l}</option>)}
    </select>
  );
  const txt = (k) => (
    <input className="flat-fi" placeholder="Filtrer…" value={f[k]}
           onClick={e => e.stopPropagation()} onChange={e => set(k, e.target.value)} />
  );

  return (
    <div className="flat-wrap hi-wrap">
      <table className="flat hi-tbl">
        <thead>
          <tr>
            {cols.map(c => (
              <th key={c.key} className={"flat-th hi-th-" + c.key + (sort.col === c.key ? " is-sorted" : "")}
                  onClick={() => !c.nosort && toggleSort(c.key)}>
                <span className="flat-th-l">{c.label}</span>
                {!c.nosort && <span className="flat-th-s">{sort.col === c.key ? (sort.dir > 0 ? "▲" : "▼") : "↕"}</span>}
              </th>
            ))}
          </tr>
          <tr className="flat-filters">
            {cols.map(c => (
              <th key={c.key}>
                {c.key === "date" ? (
                  <div className="hi-fdates">
                    <input type="date" className="flat-fi hi-fdate" value={f.from} aria-label="À partir du"
                           onClick={e => e.stopPropagation()} onChange={e => set("from", e.target.value)} />
                    <input type="date" className="flat-fi hi-fdate" value={f.to} aria-label="Jusqu'au"
                           onClick={e => e.stopPropagation()} onChange={e => set("to", e.target.value)} />
                  </div>
                ) : c.key === "el" ? sel("el", els.map(e => ({ v: e.k, l: e.label })), "Élément")
                  : c.key === "lang" ? sel("lang", langs.map(l => ({ v: l, l })), "Langue")
                  : c.key === "action" ? sel("action", Object.entries(HIST_ACTION).map(([v, l]) => ({ v, l })), "Action")
                  : c.key === "user" ? sel("user", users.map(u => ({ v: u, l: u })), "Utilisateur")
                  : txt(c.key)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sorted.map(h => (
            <tr key={h.id}>
              <td data-label="Date" className="hi-mono hi-td-date">{h.date.slice(0, 10) + "\n" + h.date.slice(11)}</td>
              {scope === "th" && (
                <td data-label="Concept" className="hi-td-cpt">
                  <button className="hi-cpt" onClick={() => onOpen && onOpen(h.concept)}>{LBL(h.concept)}</button>
                </td>
              )}
              <td data-label="Élément" className="hi-td-el">{histEl(h.el).label}</td>
              <td data-label="Langue" className="hi-mono hi-td-lang">{h.lang || "—"}</td>
              <td data-label="Action" className="hi-td-act"><span className={"hi-act hi-" + h.action}>{HIST_ACTION[h.action]}</span></td>
              <td data-label="Modification" className="hi-td-diff">
                {h.before && <span className="hi-before">{h.before}</span>}
                {h.before && h.after && <span className="hi-arrow">→</span>}
                {h.after && <span className="hi-after">{h.after}</span>}
                {!h.before && !h.after && <span className="muted">—</span>}
              </td>
              <td data-label="Utilisateur" className="hi-mono hi-td-user">{h.user}</td>
            </tr>
          ))}
          {sorted.length === 0 && (
            <tr><td className="flat-empty" colSpan={cols.length}>Aucune modification pour ces critères.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

// HIS-2 — vue Historique du thésaurus, dans le switcheur de vues.
function HistoryView({ onOpen }) {
  const all = window.HISTORY || [];
  return (
    <div className="hi-view">
      <h1 className="hi-title">Historique du thésaurus</h1>
      <div className="bo-card hi-card"><HistoryLog entries={all} scope="th" onOpen={onOpen} /></div>
    </div>
  );
}

// ── Sources d'alignement : liste configurable dans les Paramètres ──
const ALIGN_DEFAULT = [
  { key: "wikidata", name: "Wikidata",  kind: "Encyclopédique",     base: "https://www.wikidata.org/wiki/", enabled: true },
  { key: "geonames", name: "GeoNames",  kind: "Géographique",       base: "https://www.geonames.org/", enabled: true },
  { key: "getty",    name: "Getty AAT", kind: "Art & architecture", base: "http://vocab.getty.edu/aat/", enabled: true },
  { key: "idref",    name: "IdRef",     kind: "Autorités (ABES)",   base: "https://www.idref.fr/", enabled: true },
  { key: "bnf",      name: "data.BnF",  kind: "Bibliographique",    base: "https://data.bnf.fr/", enabled: false }
];
const AS_KEY = "ot.alignSources", ST_KEY = "ot.alignStale";
let ASRC = (() => { try { const v = JSON.parse(localStorage.getItem(AS_KEY)); if (Array.isArray(v) && v.length) return v; } catch (e) {} return ALIGN_DEFAULT.slice(); })();
let ASTALE = (() => { const v = parseInt(localStorage.getItem(ST_KEY), 10); return isNaN(v) ? 12 : v; })();
const alignSources = () => ASRC;
function setAlignSources(l) { ASRC = l.slice(); try { localStorage.setItem(AS_KEY, JSON.stringify(ASRC)); } catch (e) {} otSync(); }
const alignStale = () => ASTALE;                    // mois avant « à revoir » (0 = jamais)
function setAlignStale(m) { ASTALE = m; try { localStorage.setItem(ST_KEY, String(m)); } catch (e) {} otSync(); }
const ALIGN_SOURCES = ALIGN_DEFAULT;

// ALI-2 — les cinq propriétés d'alignement de la référence SKOS (W3C). La
// propriété est normative ; ce qui est configurable, c'est celles qu'on propose
// dans les menus et le libellé sous lequel on les présente.
const MATCH_DEFAULT = [
  { k: "exactMatch",   label: "exactMatch",   hint: "le même concept", enabled: true },
  { k: "closeMatch",   label: "closeMatch",   hint: "un concept très proche", enabled: true },
  { k: "broadMatch",   label: "broadMatch",   hint: "le concept distant est plus large", enabled: true },
  { k: "narrowMatch",  label: "narrowMatch",  hint: "le concept distant est plus précis", enabled: true },
  { k: "relatedMatch", label: "relatedMatch", hint: "un concept lié transversalement", enabled: true }
];
const MT_KEY = "ot.matchTypes";
let MTYPES = (() => { try { const v = JSON.parse(localStorage.getItem(MT_KEY)); if (Array.isArray(v) && v.length) return v; } catch (e) {} return MATCH_DEFAULT.map(m => ({ ...m })); })();
const allMatchTypes = () => MTYPES;
const matchTypes = () => MTYPES.filter(m => m.enabled);
const matchMeta = (k) => MTYPES.find(m => m.k === k) || { k, label: k, hint: "" };
function setMatchTypes(l) { MTYPES = l.map(m => ({ ...m })); try { localStorage.setItem(MT_KEY, JSON.stringify(MTYPES)); } catch (e) {} otSync(); }
// Un type désactivé après coup reste visible sur l'alignement qui le porte.
const typeOptions = (cur) => matchTypes().some(m => m.k === cur) ? matchTypes() : matchTypes().concat([matchMeta(cur)]);
const MATCH_TYPES = MATCH_DEFAULT;
const ALIGN_STATE = {
  ok:     { label: "vérifié",     cls: "ok" },
  review: { label: "à revoir",    cls: "rev" },
  broken: { label: "cassé",       cls: "bad" },
  none:   { label: "non vérifié", cls: "non" }
};
const monthsSince = (d) => d ? (Date.now() - new Date(d).getTime()) / (1000 * 60 * 60 * 24 * 30.44) : Infinity;
const isStale = (a) => alignStale() > 0 && monthsSince(a.checked) > alignStale();

// Bloc Alignement : les alignements enregistrés (type modifiable, date, état,
// vérification à la demande) + une recherche inline sous le concept — on trouve,
// on qualifie, on valide, on enchaîne. Aucun score : les web services externes
// ne renvoient rien de comparable (décision du 02/09).
function AlignPanel({ c }) {
  const [rows, setRows] = React.useState(() => (c.align || []).map(a => ({ ...a })));
  const [open, setOpen] = React.useState(false);
  const [term, setTerm] = React.useState("");
  const defaultLang = (window.THESAURUS || {}).lang || "fr";
  const [lang, setLang] = React.useState(defaultLang);
  const [busy, setBusy] = React.useState(false);
  const [hits, setHits] = React.useState(null);
  // {source-filtre-alignement} — une source active à la fois, jamais de mélange.
  const [activeSource, setActiveSource] = React.useState(null);
  const [checking, setChecking] = React.useState(null);
  // Saisie manuelle : l'URI est connue d'avance, ou vient d'un référentiel
  // qui n'est pas interrogeable — on la pose sans passer par une recherche.
  const [man, setMan] = React.useState(null);
  React.useEffect(() => {
    setRows((c.align || []).map(a => ({ ...a })));
    setOpen(false); setHits(null); setActiveSource(null); setTerm(""); setLang(defaultLang); setMan(null);
  }, [c.id]);

  // ALI-4 — formes utilisables comme terme de recherche : le nom, ses variantes,
  // les libellés traduits. On n'interroge qu'avec celle qui est choisie.
  const forms = (l) => {
    const out = l === "fr"
      ? [c.pref, ...(c.alts || [])]
      : [(c.tr || {})[l], ...(((c.trAlt || {})[l]) || [])].filter(Boolean);
    return out.length ? out : [c.pref];
  };
  const activeTerm = term || forms(lang)[0];
  const langs = ["fr", ...Object.keys(c.tr || {})];

  const DESC = {
    wikidata: (t) => "Élément Wikidata « " + t + " » — 42 liens interwiki, instance de « matériau ».",
    getty:    (t) => "AAT : « " + t + " », hiérarchie Materials Facet > Materials > metal.",
    geonames: (t) => "Entité géographique « " + t + " », feature class P (lieu habité).",
    idref:    (t) => "Notice d'autorité IdRef « " + t + " » — rameau matière, 17 documents liés.",
    bnf:      (t) => "Autorité matière BnF « " + t + " », employée dans le catalogue général."
  };
  const search = (t, l) => {
    setBusy(true); setHits(null); setActiveSource(null);
    setTimeout(() => {
      const out = [];
      alignSources().filter(s => s.enabled).forEach((s, si) => {
        const n = (c.idInterne || 1000) + si * 7;
        const id = { wikidata: "Q" + (30000 + n % 90000), geonames: String(2500000 + n % 900000),
          getty: "300" + (10000 + n % 80000), idref: "0" + (26000000 + n % 900000) + "X",
          bnf: "cb" + (11900000 + n % 90000) + "z" }[s.key];
        const mk = (label, sfx) => ({ label, source: s.name, key: s.key, uri: s.base + (sfx ? id.slice(0, -1) + sfx : id),
          desc: (DESC[s.key] || (x => x))(label), lang: l });
        out.push(mk(t));
      });
      setHits(out); setBusy(false); setActiveSource(out[0] ? out[0].key : null);
    }, 550);
  };
  const runSearch = () => { setMan(null); setOpen(true); search(activeTerm, lang); };
  const addManual = () => {
    const uri = (man.uri || "").trim();
    if (!uri) return;
    const today = new Date().toISOString().slice(0, 10);
    const src = alignSources().find(s => uri.startsWith(s.base));
    setRows(rows.concat([{ type: man.type, source: (man.source || "").trim() || (src ? src.name : "Saisie manuelle"),
      uri, label: (man.label || "").trim() || uri.replace(/^https?:\/\//, ""),
      created: today, checked: today, state: "none", fresh: true }]));
    setMan(null);
  };
  const align = (h, type) => {
    const today = new Date().toISOString().slice(0, 10);
    setRows(rows.concat([{ type, source: h.source, uri: h.uri, label: h.label,
      created: today, checked: today, state: "ok", fresh: true }]));
    setHits(hits.filter(x => x.uri !== h.uri));
  };
  const setType = (i, t) => setRows(rows.map((r, j) => j === i ? { ...r, type: t } : r));
  const remove = (i) => setRows(rows.filter((_, j) => j !== i));
  // ALI-7 — vérification à la demande, alignement par alignement : l'URI répond,
  // et le libellé distant est-il toujours celui qu'on a enregistré ?
  const verify = (i) => {
    setChecking(i);
    setTimeout(() => {
      const today = new Date().toISOString().slice(0, 10);
      setRows(rows.map((r, j) => j !== i ? r
        : { ...r, checked: today, state: r.state === "broken" ? "broken" : (r.was ? "review" : "ok") }));
      setChecking(null);
    }, 700);
  };

  return (
    <div className="align-panel">
      {rows.length ? (
        <div className="al-list">
          {rows.map((a, i) => {
            const st = ALIGN_STATE[a.state || "none"];
            const stale = isStale(a);
            return (
              <div className={"al-row" + (a.fresh ? " is-new" : "")} key={i}>
                <div className="al-main">
                  <div className="al-l1">
                    <select className="al-type" value={a.type} onChange={e => setType(i, e.target.value)}
                            title="Propriété d'alignement SKOS">
                      {typeOptions(a.type).map(m => <option key={m.k} value={m.k}>{m.label}</option>)}
                    </select>
                    <span className="al-lbl">{a.label || a.source}</span>
                    <span className="al-src">{a.source}</span>
                  </div>
                  <a className="align-uri" href={a.uri} target="_blank" rel="noreferrer">{a.uri}</a>
                  {a.state === "review" && a.was &&
                    <div className="al-warn">Le libellé distant a changé : « {a.was} » → « {a.label} ».</div>}
                  {a.state === "broken" && <div className="al-warn">L'URI ne répond plus.</div>}
                </div>
                <div className="al-side">
                  <span className={"al-state al-" + st.cls}>{st.label}</span>
                  <span className={"al-date" + (stale ? " is-stale" : "")}
                        title={"Aligné le " + (a.created || "—") + (a.checked ? " · vérifié le " + a.checked : "")}>
                    {a.checked || a.created || "—"}
                  </span>
                  <button className="al-act" onClick={() => verify(i)} disabled={checking === i}
                          title="Vérifier cet alignement">{checking === i ? "…" : "Vérifier"}</button>
                  <button className="note-ed-x" title="Retirer cet alignement" onClick={() => remove(i)}><Icon name="x" className="ico-inline"/></button>
                </div>
              </div>
            );
          })}
        </div>
      ) : <div className="muted">Aucun alignement vers un référentiel externe.</div>}

      {man ? (
        <div className="al-search al-man">
          <div className="al-man-h">
            <span className="al-man-t">Saisir un alignement</span>
            <button className="align-auto-x" onClick={() => setMan(null)}>Fermer <Icon name="x" className="ico-inline"/></button>
          </div>
          <div className="al-man-g">
            <label className="al-man-f al-man-uri"><span>URI</span>
              <input className="st-input st-mono" autoFocus value={man.uri} placeholder="https://…"
                     onChange={e => setMan({ ...man, uri: e.target.value })}
                     onKeyDown={e => { if (e.key === "Enter") addManual(); }} /></label>
            <label className="al-man-f"><span>Libellé distant</span>
              <input className="st-input" value={man.label} placeholder="Terme tel qu'il est écrit à la source"
                     onChange={e => setMan({ ...man, label: e.target.value })}
                     onKeyDown={e => { if (e.key === "Enter") addManual(); }} /></label>
            <label className="al-man-f"><span>Source</span>
              <input className="st-input" value={man.source} list="al-src-list" placeholder="Déduite de l'URI"
                     onChange={e => setMan({ ...man, source: e.target.value })} />
              <datalist id="al-src-list">
                {alignSources().map(s => <option key={s.key} value={s.name} />)}
              </datalist></label>
            <label className="al-man-f"><span>Type</span>
              <select className="al-type" value={man.type} onChange={e => setMan({ ...man, type: e.target.value })}>
                {typeOptions(man.type).map(m => <option key={m.k} value={m.k}>{m.label}</option>)}
              </select></label>
          </div>
          <div className="al-man-run">
            <button className="bo-btn primary sm" onClick={addManual} disabled={!(man.uri || "").trim()}>Aligner</button>
          </div>
        </div>
      ) : !open ? (
        <div className="al-actions">
          <button className="bo-btn ghost sm al-open" onClick={runSearch}>Chercher un alignement</button>
          <button className="bo-btn ghost sm" onClick={() => { setOpen(false); setHits(null);
            setMan({ uri: "", label: "", source: "", type: (matchTypes()[0] || { k: "exactMatch" }).k }); }}>Saisir un alignement</button>
        </div>
      ) : (
        <div className="al-search">
          <div className="al-q">
            <select className="al-qsel" value={activeTerm}
                    onChange={e => { setTerm(e.target.value); search(e.target.value, lang); }}>
              {forms(lang).map(f => <option key={f} value={f}>{f}</option>)}
            </select>
            <select className="re-hsel" value={lang}
                    onChange={e => { const l = e.target.value; setLang(l); setTerm(""); search(forms(l)[0], l); }}>
              {langs.map(l => <option key={l} value={l}>{ENDONYM[l] || l}</option>)}
            </select>
            <button className="align-auto-x" onClick={() => { setOpen(false); setHits(null); }}>Fermer <Icon name="x" className="ico-inline"/></button>
          </div>
          {busy ? <div className="align-res-load">Interrogation des sources…</div> : hits && hits.length ? (
            <>
              <div className="al-src-tabs">
                {[...new Map(hits.map(h => [h.key, h])).values()].map(h => {
                  const n = hits.filter(x => x.key === h.key).length;
                  return (
                    <button key={h.key} type="button" className={"al-src-tab" + (activeSource === h.key ? " is-on" : "")}
                            onClick={() => setActiveSource(h.key)}>
                      {h.source}<span className="al-src-tab-n">{n}</span>
                    </button>
                  );
                })}
              </div>
              <div className="al-hits">
                {hits.filter(h => h.key === activeSource).map((h, i) => <AlignHit key={h.uri + i} h={h} onAlign={align} />)}
              </div>
            </>
          ) : <div className="muted">Aucune correspondance.</div>}
        </div>
      )}
    </div>
  );
}

function AlignHit({ h, onAlign }) {
  const [type, setType] = React.useState((matchTypes()[0] || { k: "exactMatch" }).k);
  return (
    <div className="al-hit">
      <div className="al-main">
        <div className="al-l1">
          <span className="al-lbl">{h.label}</span>
          <span className="al-src">{h.source}</span>
          <span className="al-hl">{h.lang}</span>
        </div>
        <div className="al-desc">{h.desc}</div>
        <a className="align-uri" href={h.uri} target="_blank" rel="noreferrer">{h.uri}</a>
      </div>
      <div className="al-side">
        <select className="al-type" value={type} onChange={e => setType(e.target.value)}
                title={matchMeta(type).hint}>
          {typeOptions(type).map(m => <option key={m.k} value={m.k}>{m.label}</option>)}
        </select>
        <button className="bo-btn primary sm" onClick={() => onAlign(h, type)}>Aligner</button>
      </div>
    </div>
  );
}

function FacetView({ concept, onTerm }) {
  const f = concept.facet;
  const [expanded, setExpanded] = useState(null);
  const [confirm, setConfirm] = useState(false);
  const titleRef = useRef(null);
  const members = f.members || [];
  const dropFacet = () => {
    window.FACETS = (window.FACETS || []).filter(x => x.id !== f.id);
    if (typeof window.__otSync === "function") window.__otSync();
    onTerm && onTerm(f.parent);
  };
  const langs = Object.keys(f.tr || {});
  if (expanded) {
    const close = () => setExpanded(null);
    return (
      <div className="cv cv-exp">
        <div className="cv-exp-bar">
          <button className="cv-exp-back" onClick={close}><Icon name="chevron-left" className="ico-inline"/> {f.label}</button>
          <span className="cv-exp-title">{expanded.title}</span>
        </div>
        <div className="cv-exp-body">{typeof expanded.content === "function" ? expanded.content(close) : expanded.content}</div>
      </div>
    );
  }
  return (
    <ExpandCtx.Provider value={setExpanded}>
      <div className="cv">
        <div className="cv-path"><Path parts={concept.path} /></div>
        <div className="cv-head">
          <StickyTitle anchorRef={titleRef} title={f.label} />
          <h1 className="cv-pref" ref={titleRef}>
            <span className="cv-facet-ico" title="Facette"><Icon name="layers" /></span>
            {f.label}
            <span className="tag tag-col">Facette</span>
          </h1>
        </div>
        <div className="cv-blocks">
          <Block id="f-lib" title="Libellé" editable edit={<>
            <DField label="Libellé"><input className="st-input" defaultValue={f.label} /></DField>
            <DField label="Concept parent"><input className="st-input" defaultValue={f.parent} /></DField>
          </>}>
            <Row label="Libellé">
              <span className="val-strong">{f.label}</span> <span className="val-lang">({DISPLAY_LANG})</span>
            </Row>
            <Row label="Concept parent">
              <button className="link" onClick={() => onTerm && onTerm(f.parent)}>{LBL(f.parent)}</button>
            </Row>
          </Block>

          <Block id="f-mem" title="Membres" editable edit={<>
            {f.members.map((m, i) => <DField key={i} label={LBL(m)}><input className="st-input" defaultValue={m} /></DField>)}
            <DField label="+ Ajouter un membre"><input className="st-input" placeholder="Concept spécifique du parent…" /></DField>
          </>}>
            <Row label={f.members.length + " membre" + (f.members.length > 1 ? "s" : "")}>
              <div className="rel-items">
                {f.members.map((m, i) => <button key={i} className="rel-term" onClick={() => onTerm && onTerm(m)}>{LBL(m)}</button>)}
              </div>
            </Row>
          </Block>

          <Block id="f-tr" title="Traductions" editable edit={<>
            {langs.map(l => <DField key={l} label={LANGNAME[l] || l}><input className="st-input" defaultValue={f.tr[l]} /></DField>)}
            <DField label="+ Ajouter une langue"><input className="st-input" placeholder="Libellé traduit…" /></DField>
          </>}>
            {langs.length
              ? langs.map(l => <Row key={l} label={LANGNAME[l] || l}>{f.tr[l]}</Row>)
              : <div className="muted">Aucune traduction.</div>}
          </Block>

          <Block id="f-notes" title="Notes" editable
            edit={<DField label="Note"><textarea className="disc-input" defaultValue={f.note || ""} placeholder="Note sur la facette…"></textarea></DField>}>
            {f.note ? <div className="cv-def">{f.note}</div> : <div className="muted">Aucune note.</div>}
          </Block>

          <Block id="f-id" title="Identifiants">
            <Row label="Identifiant" mono>{f.code}</Row>
          </Block>

          <div className="cv-danger">
            <button className="bo-btn danger" onClick={() => setConfirm(true)}><Icon name="trash" className="ico-inline"/> Supprimer la facette</button>
          </div>
        </div>
        {confirm && (
          <Modal title="Supprimer la facette ?" onClose={() => setConfirm(false)}
            actions={<>
              <button className="bo-btn ghost" onClick={() => setConfirm(false)}>Annuler</button>
              <button className="bo-btn danger" onClick={dropFacet}><Icon name="trash" className="ico-inline"/> Supprimer la facette</button>
            </>}>
            <p className="mo-warn">
              {members.length > 1
                ? "Les " + members.length + " concepts regroupés par « " + f.label + " » passeront directement sous « " + LBL(f.parent) + " »."
                : "Le concept regroupé par « " + f.label + " » passera directement sous « " + LBL(f.parent) + " »."}
            </p>
            <p className="mo-warn-2">Aucun concept n'est supprimé.</p>
          </Modal>
        )}
      </div>
    </ExpandCtx.Provider>
  );
}

function ConceptView(props) {
  return props.concept && props.concept.isFacet
    ? <FacetView {...props} />
    : <ConceptFiche {...props} />;
}

function ConceptFiche({ concept, query, onBack, onTerm, onResolve }) {
  const c = concept;
  // EDT-2 : état d'édition des relations (le proto n'écrit pas dans data.js).
  const [rel, setRel] = useState({ bt: c.bt || [], nt: c.nt || [], rt: c.rt || [], htype: initHTypes(c) });
  const [trad, setTrad] = useState({ tr: { ...(c.tr || {}) }, trAlt: { ...(c.trAlt || {}) } });
  const [cls, setCls] = useState(c.cls || []);
  const [notes, setNotes] = useState(c.notes || []);
  useEffect(() => {
    setRel({ bt: c.bt || [], nt: c.nt || [], rt: c.rt || [], htype: initHTypes(c) });
    setTrad({ tr: { ...(c.tr || {}) }, trAlt: { ...(c.trAlt || {}) } });
    setCls(c.cls || []);
    setNotes(c.notes || []);
  }, [c.id]);
  const tr = Object.entries(trad.tr || {});
  const copy = (txt) => { try { navigator.clipboard.writeText(txt); } catch (e) {} };

  // {actions-unitaires-fiche} — menu d'actions à droite du titre, hauteur
  // contenue (scroll interne si besoin), ne masque jamais l'arbre à gauche.
  const [actionsOpen, setActionsOpen] = useState(false);
  const actionsRef = useRef(null);
  const titleRef = useRef(null);
  useEffect(() => {
    const onDoc = (e) => { if (actionsRef.current && !actionsRef.current.contains(e.target)) setActionsOpen(false); };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);
  const [proposeOpen, setProposeOpen] = useState(false);
  const [proposeMsg, setProposeMsg] = useState("");
  const [exportOpen, setExportOpen] = useState(false);
  const [deprecateConfirm, setDeprecateConfirm] = useState(false);
  const [flash, setFlash] = useState(null);
  const notify = (msg) => { setFlash(msg); clearTimeout(window.__cvT); window.__cvT = setTimeout(() => setFlash(null), 2400); };
  const sendPropose = () => {
    setProposeOpen(false); setProposeMsg("");
    notify("Modification proposée — en attente de traitement par un gestionnaire");
  };
  const deprecate = () => {
    c.status = "deprecie"; otSync(); setDeprecateConfirm(false);
    notify("« " + LBL(c.pref) + " » marqué obsolète");
  };
  const [expanded, setExpanded] = useState(null);
  useEffect(() => { setExpanded(null); }, [c.id]);

  // ── Ordre des blocs de la fiche, réordonnable par poignée + persistant ──
  const CARD_ORDER_KEY = "ot.conceptCardOrder.v4";
  const CARD_IDS = ["contexte", "collections", "relations", "classes", "traductions", "notes", "ressources", "alignement", "identifiants", "temporel", "histo"];
  const [order, setOrder] = useState(() => {
    try { const s = JSON.parse(localStorage.getItem(CARD_ORDER_KEY)); if (Array.isArray(s) && s.length) return s; } catch (e) {}
    return CARD_IDS;
  });
  useEffect(() => { try { localStorage.setItem(CARD_ORDER_KEY, JSON.stringify(order)); } catch (e) {} }, [order]);
  const [dragId, setDragId] = useState(null);
  const [overId, setOverId] = useState(null);
  const [overPos, setOverPos] = useState(null); // "before" | "after" (gap visé)
  const [handleId, setHandleId] = useState(null);
  // ordre normalisé (tous les ids présents, dans l'ordre enregistré)
  const fullOrder = [...order.filter(x => CARD_IDS.includes(x)), ...CARD_IDS.filter(x => !order.includes(x))];
  const dropAt = (from, to, pos) => {
    if (!from || from === to) return;
    setOrder(() => {
      const a = [...fullOrder];
      const fi = a.indexOf(from); if (fi < 0) return fullOrder;
      a.splice(fi, 1);
      let ti = a.indexOf(to); if (ti < 0) return fullOrder;
      if (pos === "after") ti += 1;
      a.splice(ti, 0, from);
      return a;
    });
  };
  const posFromEvent = (e) => { const r = e.currentTarget.getBoundingClientRect(); return e.clientY < r.top + r.height / 2 ? "before" : "after"; };
  const clearDrag = () => { setDragId(null); setOverId(null); setOverPos(null); setHandleId(null); };
  const dragFor = (bid) => ({
    dragging: dragId === bid,
    dropBefore: overId === bid && overPos === "before" && dragId && dragId !== bid,
    dropAfter: overId === bid && overPos === "after" && dragId && dragId !== bid,
    section: {
      order: fullOrder.indexOf(bid),
      draggable: handleId === bid,
      onDragStart: (e) => { setDragId(bid); e.dataTransfer.effectAllowed = "move"; try { e.dataTransfer.setData("text/plain", bid); } catch (_) {} },
      onDragEnd: clearDrag,
      onDragOver: (e) => { e.preventDefault(); if (dragId === bid) return; const pos = posFromEvent(e); if (overId !== bid) setOverId(bid); if (overPos !== pos) setOverPos(pos); },
      onDrop: (e) => { e.preventDefault(); const from = dragId || (() => { try { return e.dataTransfer.getData("text/plain"); } catch (_) { return null; } })(); dropAt(from, bid, posFromEvent(e)); clearDrag(); }
    },
    handle: { onMouseDown: () => setHandleId(bid), onMouseUp: () => setHandleId(null) }
  });

  if (expanded) {
    const close = () => setExpanded(null);
    return (
      <div className="cv cv-exp">
        <div className="cv-exp-bar">
          <button className="cv-exp-back" onClick={close}><Icon name="chevron-left" className="ico-inline"/> {LBL(c.pref)}</button>
          <span className="cv-exp-title">{expanded.title}</span>
        </div>
        <div className="cv-exp-body">
          {typeof expanded.content === "function" ? expanded.content(close) : expanded.content}
        </div>
      </div>
    );
  }

  return (
    <ExpandCtx.Provider value={setExpanded}>
    <div className="cv">
      <div className="cv-path"><Path parts={c.path} /></div>

      {!c.draft && (
        <div className="cv-head">
          <StickyTitle anchorRef={titleRef} title={LBL(c.pref)} />
          <h1 className="cv-pref" ref={titleRef}>
            {c.status === "candidat" && <span className="cv-dot" title="Concept candidat" />}
            {LBL(c.pref)}
            <button className="copy-btn" title="Copier l'intitulé" onClick={() => copy(LBL(c.pref))}>⧉</button>
            <span className="cv-lang">{DISPLAY_LANG}</span>
            {c.status === "candidat" && <span className="cv-status">candidat</span>}
            {c.type === "collection" && <span className="tag tag-col">Collection</span>}
            <ClassBadges c={{ cls }} max={4} />
            <div className="cv-actions" ref={actionsRef}>
              <button type="button" className="bo-btn ghost sm cv-actions-btn" aria-label="Actions"
                      onClick={() => setActionsOpen(o => !o)}>⋯</button>
              {actionsOpen && (
                <div className="cv-actions-menu">
                  <button type="button" className="cv-actions-mi" onClick={() => { setActionsOpen(false); setProposeOpen(true); }}>
                    Proposer une modification</button>
                  <button type="button" className="cv-actions-mi" onClick={() => { setActionsOpen(false); setExportOpen(true); }}>
                    Exporter ce concept</button>
                  {c.status !== "candidat" && c.status !== "deprecie" && (
                    <button type="button" className="cv-actions-mi is-danger"
                            onClick={() => { setActionsOpen(false); setDeprecateConfirm(true); }}>Marquer comme obsolète</button>
                  )}
                </div>
              )}
            </div>
          </h1>
          <p className="cv-head-uri">
            <span className="is-mono">{c.uri && c.uri !== "—" ? c.uri : c.id}</span>
            <button className="copy-btn" title="Copier l'URI/ARK" onClick={() => copy(c.uri && c.uri !== "—" ? c.uri : c.id)}>⧉</button>
          </p>
        </div>
      )}

      {c.status === "candidat" && !c.draft && <CandidateBanner key={"b" + c.id} c={c} onResolve={onResolve} />}
      {c.status === "candidat" && !c.draft && <VotesBlock key={"v" + c.id} c={c} />}
      {c.status === "candidat" && !c.draft && <DiscussionBlock key={"d" + c.id} c={c} />}

      {c.draft && <DraftForm c={c} onResolve={onResolve} />}
      {!c.draft && (<>
      <div className="cv-blocks">
      {/* Bloc libellé */}
      <Block id="contexte" drag={dragFor("contexte")} title="Libellé" editable edit={<>
        <DField label="Libellé"><LabelField value={LBL(c.pref)} lang={displayLang()} selfId={c.id} onTerm={onTerm} /></DField>
        <DField label="Formes alternatives"><input className="st-input" defaultValue={(c.alts || []).join(", ")} placeholder="synonymes, séparés par des virgules" /></DField>
        <DField label="Formes cachées"><input className="st-input" defaultValue={(c.altsHidden || []).join(", ")} placeholder="fautes, pluriels, graphies (non affichées)" /></DField>
      </>}>
        <Row label="Libellé">
          <span className="val-strong">{LBL(c.pref)}</span> <span className="val-lang">({DISPLAY_LANG})</span>
          <button className="copy-btn" title="Copier" onClick={() => copy(LBL(c.pref))}><Icon name="copy" className="ico-inline"/></button>
        </Row>
        <Row label="Formes alternatives">
          {c.alts && c.alts.length ? c.alts.join(", ") : <span className="muted">—</span>}
        </Row>
        <Row label="Formes cachées">
          {c.altsHidden && c.altsHidden.length
            ? <span className="alts-hidden">{c.altsHidden.join(", ")}</span>
            : <span className="muted">—</span>}
        </Row>
        <Row label="Facettes"><span className="link">{c.facette}</span></Row>
        <Row label="Total de la branche">{c.branche} concept{c.branche > 1 ? "s" : ""}</Row>
        <div className="cv-corpus"><button className="bo-btn primary sm">Afficher les corpus liés{c.corpusCount ? " (" + c.corpusCount + ")" : ""}</button></div>
      </Block>

      {/* Bloc collections */}
      <Block id="collections" drag={dragFor("collections")} title="Collections" editable
        edit={<>
          {(c.collections || []).map((col, i) => (
            <DField key={i} label={col.nom}><input className="st-input" defaultValue={col.id} /></DField>
          ))}
          <DField label="+ Ajouter à une collection"><input className="st-input" placeholder="Nom de la collection…" /></DField>
        </>}>
        {c.collections && c.collections.length ? (
          <div className="coll-list">
            {c.collections.map((col, i) => (
              <div key={i} className="coll-item">
                <span className="coll-name link">{col.nom}</span>
                <span className="coll-id is-mono">id={col.id}</span>
                {col.membres != null && <span className="coll-count">{col.membres} membre{col.membres > 1 ? "s" : ""}</span>}
              </div>
            ))}
          </div>
        ) : <div className="muted">Ce concept n'appartient à aucune collection.</div>}
      </Block>

      {/* Bloc relations */}
      <Block id="relations" drag={dragFor("relations")} title="Relations sémantiques" editable unitary
        edit={<RelationEditor concept={c} value={rel} onChange={setRel} />}
        fullContent={(close) => <RelationGraph concept={{ ...c, ...rel }} onTerm={(t) => { close(); onTerm(t); }} />}>
        <RelRow label="Concept générique" code="TG" kind="bt" items={rel.bt} types={rel.htype} onTerm={onTerm} />
        <RelRow label="Concept spécifique" code="TS" kind="nt" items={rel.nt} types={rel.htype} onTerm={onTerm} />
        <RelRow label="Concept associé" code="TA" kind="rt" items={rel.rt} onTerm={onTerm} />
      </Block>

      {/* CLS-4 — classes du concept (masqué si les classes ne sont pas activées) */}
      {clsEnabled() && (
        <Block id="classes" drag={dragFor("classes")} title="Classes de concept" editable
          onSave={() => { c.cls = cls.slice(); otSync(); }}
          onCancel={() => setCls(c.cls || [])}
          edit={<ClassPicker value={cls} onChange={setCls} />}>
          {cls.length ? <ClassBadges c={{ cls }} max={8} /> : <div className="muted">Aucune classe.</div>}
        </Block>
      )}

      {/* Bloc traductions */}
      <Block id="traductions" drag={dragFor("traductions")} title="Traductions" editable unitary
        edit={<TranslationEditor concept={c} value={trad} onChange={setTrad} onTerm={onTerm} />}>
        {tr.length > 0 ? (
          <div className="tr-list">
            {tr.map(([lang, label]) => {
              const todo = !label || (c.trStatus && c.trStatus[lang] === "todo");
              const alts = (trad.trAlt && trad.trAlt[lang]) || [];
              return (
                <div key={lang} className="tr-block">
                  <div className="tr-item">
                    <span className={"tr-dot " + (todo ? "tr-dot-todo" : "tr-dot-ok")} title={todo ? "Traduction à compléter" : "Traduction validée"} />
                    {FLAG[lang] ? <span className="tr-flag">{FLAG[lang]}</span>
                                : <span className="tr-code">{lang}</span>}
                    <span className="tr-label" lang={lang} dir={dirOf(lang)}>{label}</span>
                    <span className="tr-lang">{LANGNAME[lang] || lang}</span>
                    {todo && <span className="tr-todo">à compléter</span>}
                  </div>
                  {alts.length > 0 && (
                    <div className="tr-alts"><span className="tr-alts-lbl">Formes alternatives</span>
                      <span lang={lang} dir={dirOf(lang)}>{alts.join(", ")}</span></div>
                  )}
                </div>
              );
            })}
          </div>
        ) : <div className="muted">Aucune traduction renseignée.</div>}
      </Block>

      {/* Bloc notes */}
      <Block id="notes" drag={dragFor("notes")} title="Notes" editable
        onSave={() => { c.notes = notes.filter(n => (n.text || "").trim()); otSync(); }}
        onCancel={() => setNotes(c.notes || [])}
        edit={<NotesEditor concept={c} value={notes} onChange={setNotes} />}>
        <NotesRead notes={notes} />
      </Block>

      {/* Bloc ressources externes / image / GPS */}
      <Block id="ressources" drag={dragFor("ressources")} title="Ressources externes" editable
        edit={<>
          <DField label="Ressources externes"><input className="st-input" placeholder="URL…" /></DField>
          <DField label="Image (URI)"><input className="st-input" placeholder="https://…" /></DField>
          <DField label="Coordonnées GPS"><input className="st-input" defaultValue={c.gps || ""} placeholder="48.922548 2.145240" /></DField>
        </>}>
        <Row label="Ressources externes"><span className="muted">—</span></Row>
        <Row label="Image">{c.gps ? <span className="muted">—</span> : <span className="muted">—</span>}</Row>
        <Row label="Coordonnées GPS">
          {c.gps
            ? <span className="is-mono">{c.gps}</span>
            : <span className="muted">Ex. : 48.922548 2.145240</span>}
        </Row>
      </Block>

      {/* Bloc alignement */}
      <Block id="alignement" drag={dragFor("alignement")} title="Alignement">
        <AlignPanel c={c} />
      </Block>

      {/* Bloc identifiants & notation */}
      <Block id="identifiants" drag={dragFor("identifiants")} title="Identifiants & notation">
        <div className="cv-ids">
          <div className="cv-ids-main">
            <Row label="Notation" mono>{c.notation}</Row>
            <Row label="ID interne" mono><span className="link">{c.idInterne}</span></Row>
            <Row label="URI"><a className="link" href="#" onClick={(e) => e.preventDefault()}>{c.uri}</a></Row>
            <Row label="Identifiant permanent" mono>
              {c.permalink}
              <button className="copy-btn" title="Copier" onClick={() => copy(c.permalink)}><Icon name="copy" className="ico-inline"/></button>
            </Row>
          </div>
          <div className="cv-qr" title="QR code de l'identifiant pérenne" aria-label="QR code">
            <QRGlyph />
          </div>
        </div>
        <Row label="Exporter en SKOS">
          <div className="skos-links">
            {CONCEPT_EXPORT_FORMATS.map((f) => (
              <button key={f.key} className="skos-link" onClick={() => exportConcept(c, f.key)}>{f.label}</button>
            ))}
          </div>
        </Row>
      </Block>

      {/* Bloc temporel */}
      <Block id="temporel" drag={dragFor("temporel")} title="Information temporelle">
        <Row label="Créé le" mono>{c.created}</Row>
        <Row label="Dernière modification" mono>{c.modified}</Row>
      </Block>

      {/* HIS-1/3 — historique du concept : même composant que la vue du thésaurus,
          porté à ce seul concept. C'est ici qu'on lit qui a touché quel élément. */}
      <Block id="histo" drag={dragFor("histo")} title="Historique" fullContent={
        <HistoryLog entries={(window.HISTORY || []).filter(h => h.concept === c.pref)} scope="concept" />
      }>
        <HistoryLog entries={(window.HISTORY || []).filter(h => h.concept === c.pref)} scope="concept" />
      </Block>
      </div>

      {/* La discussion n'existe que sur les candidats (retour du 08-09,
          tranché le 23-09) ; un concept validé garde seulement son historique. */}
      {c.status !== "candidat" && c.hist && <HistoryBlock c={c} />}
      </>)}

      {proposeOpen && (
        <Modal title="Proposer une modification" sub={LBL(c.pref)}
               onClose={() => setProposeOpen(false)}
               actions={<>
                 <button className="bo-btn ghost" onClick={() => setProposeOpen(false)}>Annuler</button>
                 <button className="bo-btn primary" disabled={!proposeMsg.trim()} onClick={sendPropose}>Envoyer la proposition</button>
               </>}>
          <div className="st-form">
            <div className="st-row"><div className="st-row-l">Ce qui devrait changer<span className="st-req"> *</span></div>
              <div className="st-row-c"><textarea className="disc-input" value={proposeMsg} autoFocus
                     onChange={e => setProposeMsg(e.target.value)}
                     placeholder="Décrivez la correction à apporter…"></textarea></div></div>
          </div>
        </Modal>
      )}

      {exportOpen && (
        <Modal title="Exporter ce concept" sub={LBL(c.pref)} onClose={() => setExportOpen(false)}>
          <div className="spl">
            {CONCEPT_EXPORT_FORMATS.map(f => (
              <button className="spl-row" key={f.key}
                      onClick={() => { setExportOpen(false); exportConcept(c, f.key); }}>
                <span className="spl-tx"><span className="spl-t">{f.label}</span></span>
                <span className="spl-go"><Icon name="chevron-right" /></span>
              </button>
            ))}
          </div>
        </Modal>
      )}

      {deprecateConfirm && (
        <Modal title="Marquer obsolète ?" onClose={() => setDeprecateConfirm(false)}
               actions={<>
                 <button className="bo-btn ghost" onClick={() => setDeprecateConfirm(false)}>Annuler</button>
                 <button className="bo-btn danger" onClick={deprecate}>Marquer obsolète</button>
               </>}>
          <p className="mo-warn">« {LBL(c.pref)} » restera dans le thésaurus, marqué obsolète : il n'apparaîtra plus dans les listes actives par défaut.</p>
        </Modal>
      )}

      {flash && <div className="app-toast">{flash}</div>}
    </div>
    </ExpandCtx.Provider>
  );
}

// ════════════════════════════════════════════════════════════════════════
//  VUE D'ACCUEIL DU THÉSAURUS — ouverte en cliquant le nom du thésaurus
// ════════════════════════════════════════════════════════════════════════
function ThesaurusHome({ th, onOpen, onSearch, batchEntry = "data" }) {
  const [view, setView] = React.useState(null);   // null | batch | candidats | stats | maintenance | settings | export
  const [batchInit, setBatchInit] = React.useState(null);
  const [mgInit, setMgInit] = React.useState(null);
  const C = th.concepts;
  const candidats = C.filter(c => c.status === "candidat");
  const collections = C.filter(c => c.type === "collection");
  const langues = ["fr", ...new Set(C.flatMap(c => Object.keys(c.tr || {})))];
  const domaines = [...new Set(C.map(c => c.path[0]))];

  if (view === "batch")       return <BatchOps entry={batchEntry} init={batchInit} onBack={() => setView(null)} />;
  if (view === "candidats")   return <CandidatesView th={th} init={mgInit} onBack={() => setView(null)} onOpen={onOpen} />;
  if (view === "stats")       return <StatsView th={th} onBack={() => setView(null)} />;
  if (view === "settings")    return <SettingsView th={th} init={mgInit} onBack={() => setView(null)} />;
  if (view === "export")      return <ExportThesaurusView th={th} onBack={() => setView(null)} />;
  const open = (v, k) => { setMgInit(k || null); setView(v); };

  return (
    <div className="cv th-home">
      <div className="cv-head">
        <h1 className="cv-pref">{th.name}<span className="cv-lang">th17</span></h1>
        <p className="th-sub">{th.domain}</p>
      </div>

      <ThesaurusAbout th={th} onExport={() => setView("export")} />

      <BatchCard entry={batchEntry} onOpen={(k) => { setBatchInit(k); setView("batch"); }} />

      <MgDataCard icon="stats" title="Statistiques"
        desc="Volumétrie, complétude, répartition. Cliquez pour le détail."
        kpis={[[C.length, "Concepts"], [candidats.length, "Candidats"], [domaines.length, "Micro-thésaurus"], [langues.length, "Langues"]]}
        onOpen={() => open("stats")} />

      <MgEntryCard icon="settings" title="Paramètres du thésaurus"
        desc="Métadonnées, langues, préférences, serveurs, corpus liés."
        items={SET_SECTIONS.map(s => ({ key: s.key, label: s.label }))}
        single
        onOpen={(k) => open("settings", k)} />

      <MaintenanceCard />
    </div>
  );
}

// ── Construction de l'arborescence depuis les chemins des concepts ──────────
// Dans un thésaurus, CHAQUE nœud est un concept : les niveaux intermédiaires
// (Matière, Métal…) sont synthétisés en concepts cliquables avec TG/TS.
function buildTree(concepts) {
  const root = { name: "__root", children: new Map() };
  for (const c of concepts) {
    const chain = [...c.path, c.pref];
    let node = root;
    chain.forEach((seg, i) => {
      if (!node.children.has(seg)) node.children.set(seg, { name: seg, children: new Map(), path: chain.slice(0, i) });
      node = node.children.get(seg);
    });
    node.concept = c;
  }
  const countDesc = (n) => [...n.children.values()].reduce((s, k) => s + 1 + countDesc(k), 0);
  let syn = 0;
  const link = (node, ancestors) => {
    if (node.name !== "__root" && !node.concept) {
      const m = concepts.find(c => c.pref === node.name && JSON.stringify(c.path) === JSON.stringify(ancestors));
      if (m) node.concept = m;
      else {
        syn += 1;
        const tag = "g" + String(syn).padStart(3, "0");
        node.concept = {
          id: `ark:/12148/x${tag}`, pref: node.name, alts: [], path: ancestors,
          type: "concept", status: "valide", notation: "—", def: "",
          bt: ancestors.length ? [ancestors[ancestors.length - 1]] : [],
          nt: [...node.children.values()].map(k => k.name), rt: [],
          collection: ancestors[0] || node.name, facette: "—", branche: countDesc(node),
          tr: {}, idInterne: 5000 + syn, uri: `https://ark.frantiq.fr/ark:/26678/x${tag}`,
          permalink: `26678/x${tag}`, created: "2008-02-08", modified: "2023-06-22", synthetic: true
        };
      }
    }
    for (const child of node.children.values()) link(child, [...ancestors, node.name].filter(n => n !== "__root"));
  };
  link(root, []);

  // Les têtes de branche sont des concepts SYNTHÉTISÉS : absentes de
  // THESAURUS.concepts, elles doivent rester résolvables par libellé (relations,
  // membres de facette…) sinon un lien retombe sur une recherche plein texte.
  const index = {};
  (function walk(n) { if (n.concept) index[norm(n.name)] = n.concept; n.children.forEach(walk); })(root);
  window.__treeIndex = index;

  // FAC-2 — Facettes : on retire du parent les spécifiques listés par la
  // facette et on les replace sous un nœud intercalaire (facet:true) : ce n'est
  // pas un concept, mais il s'ouvre dans sa propre fiche.
  const findNode = (n, name) => {
    if (n.name === name && n.concept) return n;
    for (const k of n.children.values()) { const r = findNode(k, name); if (r) return r; }
    return null;
  };
  (window.FACETS || []).forEach(f => {
    const p = findNode(root, f.parent);
    if (!p) return;
    const picked = f.members.map(m => p.children.get(m)).filter(Boolean);
    if (!picked.length) return;
    picked.forEach(k => p.children.delete(k.name));
    p.children.set("§" + f.id, {
      name: f.label, facet: f, path: [...(p.path || []), p.name],
      children: new Map(picked.map(k => [k.name, k]))
    });
  });
  return root;
}

// Filtre candidats avancé (moteur de recherche interne aux candidats) : par
// déposant, par mes dépôts, par plage de dates. N'affecte QUE les candidats.
function candMatch(c, cf) {
  if (!cf) return true;
  if (statusOf(c) !== "candidat") return true;
  const by = (c.cand && c.cand.by) || (c.hist && c.hist.by) || "—";
  const on = (c.cand && c.cand.on) || (c.hist && c.hist.on) || c.created || "—";
  const iso = /^\d{4}-\d{2}-\d{2}$/.test(on);
  if (cf.by && by !== cf.by) return false;
  if (cf.from && (!iso || on < cf.from)) return false;
  if (cf.to && (!iso || on > cf.to)) return false;
  return true;
}

// Visibilité par statut : un Set des statuts visibles + filtre candidats. Un nœud
// est conservé si son concept est visible, ou s'il mène à un descendant visible.
function nodeHasVisible(node, set, cf) {
  if (node.concept && set.has(statusOf(node.concept)) && candMatch(node.concept, cf)) return true;
  for (const k of node.children.values()) if (nodeHasVisible(k, set, cf)) return true;
  return false;
}
function keepTreeNode(node, set, cf) {
  return nodeHasVisible(node, set, cf);
}
function collectConceptIds(node) {
  const ids = [];
  const walk = (n) => { if (n.concept) ids.push(n.concept.id); n.children.forEach(walk); };
  walk(node);
  return ids;
}

function subtreeHasId(node, id) {
  if (node.concept && node.concept.id === id) return true;
  for (const k of node.children.values()) if (subtreeHasId(k, id)) return true;
  return false;
}

function TreeRow({ node, depth, query, highlight, activeId, openSet, toggle, onOpen, statusSet, candFilter, autoOpen, showNotation, showContext, sort, selected, onToggleSel, onCreate, parentCls }) {
  const base = sort === "nota"
    ? (a, b) => String(a.concept?.notation || "~").localeCompare(String(b.concept?.notation || "~"), "fr")
    : (a, b) => a.name.localeCompare(b.name, "fr");
  const sorter = (a, b) => ((b.facet ? 1 : 0) - (a.facet ? 1 : 0)) || base(a, b);
  const kids = [...node.children.values()]
    .filter(k => keepTreeNode(k, statusSet, candFilter))
    .sort(sorter);
  const hasKids = kids.length > 0;
  const key = [...node.path, node.name].join("/");
  const open = autoOpen ? hasKids : openSet.has(key);
  const c = node.concept;
  const isActive = c && activeId === c.id;
  const st = c ? statusOf(c) : null;
  const isCand = st === "candidat";
  const isSel = c && selected && selected.has(c.id);
  const rowRef = useRef(null);

  useEffect(() => {
    if (!isActive || !rowRef.current) return;
    const row = rowRef.current, cont = row.closest(".tree");
    if (!cont) return;
    const rb = row.getBoundingClientRect(), cb = cont.getBoundingClientRect();
    if (rb.top < cb.top + 56 || rb.bottom > cb.bottom - 10) {
      cont.scrollTop += (rb.top - cb.top) - cont.clientHeight / 2 + row.offsetHeight / 2;
    }
  }, [isActive, activeId]);

  const extra = showContext && c;

  // FAC-2 — la facette s'ouvre en fiche : mêmes blocs, contenu propre.
  if (node.facet) {
    const opened = openSet.has(key) || (activeId && subtreeHasId(node, activeId));
    const fid = "§" + node.facet.id;
    return (
      <div className="tn">
        <div className={"tn-row is-facet" + (activeId === fid ? " is-active" : "")}>
          <div className="tn-rowmain" style={{ paddingLeft: 6 + depth * 18 }}>
            <button className={"tn-caret" + (hasKids ? "" : " is-empty")}
                    onClick={() => hasKids && toggle(key)}>{hasKids ? (opened ? "▾" : "▸") : ""}</button>
            <button className="tn-facet-lbl" onClick={() => onOpen(facetConcept(node.facet, node.path || []))}>
              <span className="tn-facet-ico" title="Facette"><Icon name="layers" /></span>
              <span className="tn-facet-txt">{node.name}</span>
            </button>
          </div>
        </div>
        {opened && hasKids && (
          <div className="tn-kids is-facet-kids">
            {kids.map((k) => (
              <TreeRow key={k.name} node={k} depth={depth + 1} query={query} highlight={highlight}
                       activeId={activeId} openSet={openSet} toggle={toggle} onOpen={onOpen}
                       statusSet={statusSet} candFilter={candFilter} autoOpen={autoOpen} showNotation={showNotation} showContext={showContext} sort={sort}
                       selected={selected} onToggleSel={onToggleSel} onCreate={onCreate} parentCls={parentCls} />
            ))}
          </div>
        )}
      </div>
    );
  }
  return (
    <div className="tn">
      <div ref={rowRef} className={"tn-row" + (isActive ? " is-active" : "") + (isCand ? " is-candidate" : "") + (extra ? " tn-tall" : "") + (isSel ? " is-sel" : "")}>
        {c && onToggleSel &&
          <span className={"tn-check" + (isSel ? " on" : "")} title="Sélectionner (avec ses enfants)"
                onClick={(e) => { e.stopPropagation(); onToggleSel(collectConceptIds(node), !isSel); }}>{isSel ? <Icon name="check" /> : ""}</span>}
        <div className="tn-rowmain" style={{ paddingLeft: 6 + depth * 18 }}>
          <button className={"tn-caret" + (hasKids ? "" : " is-empty")}
                  onClick={() => hasKids && toggle(key)}>{hasKids ? (open ? "▾" : "▸") : ""}</button>
          <button className="tn-label" onClick={() => c && onOpen(c)}>
            <span className="tn-textwrap">
              <span className="tn-text">{c
                ? <Highlight text={LBL(node.name)} query={query} on={highlight} />
                : LBL(node.name)}</span>
              {extra && (
                <span className="tn-meta">
                  {c.notation && c.notation !== "\u2014" && <span className="tn-meta-nota">{c.notation}</span>}
                  {st && st !== "valide" && <StatusPill status={st} />}
                  {c.alts && c.alts.length > 0 && <span className="tn-meta-alt">syn.&nbsp;{c.alts.length}</span>}
                  {hasKids && <span className="tn-meta-alt">{kids.length}&nbsp;TS</span>}
                </span>
              )}
            </span>
            {c && <ClassBadges c={c} max={2} compact />}
            {showNotation && !extra && c && c.notation && c.notation !== "\u2014" &&
              <span className="tn-nota">{c.notation}</span>}
            {isCand && !extra && <span className="tn-dot" title="Candidat" />}
          </button>
        </div>
        {c && onCreate &&
          <button className="tn-add" title={"Créer sous « " + LBL(node.name) + " »"}
                  aria-label={"Créer sous " + LBL(node.name)}
                  onClick={(e) => { e.stopPropagation(); onCreate(c); }}>+</button>}
      </div>
      {open && hasKids && (
        <div className="tn-kids">
          {kids.map((k) => (
            <TreeRow key={k.name} node={k} depth={depth + 1} query={query} highlight={highlight}
                     activeId={activeId} openSet={openSet} toggle={toggle} onOpen={onOpen}
                     statusSet={statusSet} candFilter={candFilter} autoOpen={autoOpen} showNotation={showNotation} showContext={showContext} sort={sort}
                     selected={selected} onToggleSel={onToggleSel} onCreate={onCreate} parentCls={c ? (c.cls || []) : parentCls} />
          ))}
        </div>
      )}
    </div>
  );
}

// ── Vue tableau (« vue à plat ») : aplatit l'arbre en liste tabulaire, gérée
//    comme un tableau — colonnes configurables, tri/filtre par colonne, filtres
//    candidats, export de ce qui est affiché. Paramètres de statut partagés. ──
const ME = "c.roussel";
const fDepositor = c => (c.cand && c.cand.by) || (c.hist && c.hist.by) || "—";
const fDepositOn = c => (c.cand && c.cand.on) || (c.hist && c.hist.on) || c.created || "—";
const fTreatBy = c => (c.hist && c.hist.valBy) || "—";
const fTreatOn = c => (c.hist && c.hist.valOn) || "—";
const fElem = c => (c.type === "collection" ? "collection" : "concept");

const FLAT_COLS = [
  { key: "concept",  label: "Concept",    always: true, get: c => LBL(c.pref) },
  { key: "status",   label: "Statut",     pill: true, get: c => (STATUS_META[statusOf(c)] || {}).label || statusOf(c) },
  { key: "type",     label: "Type",       get: c => (c.type === "collection" ? "Collection" : "Concept") },
  { key: "classes",  label: "Classes",    get: c => clsOf(c).map(m => m.label).join(", ") || "—" },
  { key: "notation", label: "Notation",   mono: true, get: c => c.notation },
  { key: "path",     label: "Chemin",     get: c => c.path.join(" › ") || "—" },
  { key: "alts",     label: "Synonymes",  get: c => (c.alts || []).join(", ") || "—" },
  { key: "depositor",label: "Déposé par", get: fDepositor },
  { key: "depositOn",label: "Déposé le",  mono: true, get: fDepositOn },
  { key: "treatBy",  label: "Traité par", get: fTreatBy },
  { key: "treatOn",  label: "Traité le",  mono: true, get: fTreatOn },
  { key: "id",       label: "ID interne", mono: true, get: c => String(c.idInterne) },
  { key: "langs",    label: "Langues",    get: c => String(1 + Object.keys(c.tr || {}).length) }
];
const FLAT_DEFAULT_COLS = ["concept", "status", "type", "classes", "notation", "depositor", "depositOn"];
const FLAT_DEFAULT_CFG = {
  cols: FLAT_DEFAULT_COLS, sortCol: "concept", sortDir: 1, filters: {},
  elemTypes: ["concept", "collection"]
};

// Pipeline commun (filtre + tri) — partagé par la vue et par l'export.
// candFilter = moteur candidats avancé (partagé, cf. StatusChecklist).
function computeFlatRows(concepts, statusSet, cfg, candFilter) {
  cfg = cfg || FLAT_DEFAULT_CFG;
  const visCols = FLAT_COLS.filter(col => (col.always || cfg.cols.includes(col.key)) && (col.key !== "classes" || clsEnabled()));
  let rows = concepts.filter(c => {
    if (!statusSet.has(statusOf(c))) return false;
    if (!cfg.elemTypes.includes(fElem(c))) return false;
    if (!candMatch(c, candFilter)) return false;
    for (const col of visCols) {
      const f = (cfg.filters[col.key] || "").trim();
      if (f && !norm(String(col.get(c))).includes(norm(f))) return false;
    }
    return true;
  });
  const sortCol = FLAT_COLS.find(c => c.key === cfg.sortCol) || FLAT_COLS[0];
  rows = rows.slice().sort((a, b) =>
    String(sortCol.get(a)).localeCompare(String(sortCol.get(b)), "fr", { numeric: true }) * cfg.sortDir);
  return { rows, visCols };
}

// CSV de ce qui est filtré + trié (utilisé par l'export depuis la roue crantée).
function flatToCsv(concepts, statusSet, cfg, candFilter) {
  const { rows, visCols } = computeFlatRows(concepts, statusSet, cfg, candFilter);
  const esc = (v) => '"' + String(v == null ? "" : v).replace(/"/g, '""') + '"';
  const lines = [visCols.map(c => esc(c.label)).join(";")];
  rows.forEach(c => lines.push(visCols.map(col => esc(col.get(c))).join(";")));
  return { csv: "\ufeff" + lines.join("\r\n"), count: rows.length };
}

// STA-1 — briques d'export partagées : un CSV portant en tête le périmètre
// exporté (thésaurus, bloc, période), puis les colonnes et les lignes.
function toCsv(headers, rows, meta) {
  const esc = (v) => '"' + String(v == null ? "" : v).replace(/"/g, '""') + '"';
  const lines = [];
  (meta || []).forEach(m => lines.push(esc(m)));
  if (meta && meta.length) lines.push("");
  lines.push(headers.map(esc).join(";"));
  rows.forEach(r => lines.push(r.map(esc).join(";")));
  return "\ufeff" + lines.join("\r\n");
}
function downloadCsv(filename, csv) {
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
const slugify = (s) => String(s).toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "")
  .replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
function downloadFile(filename, content, mime) {
  const blob = new Blob([content], { type: mime + ";charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
// {export-concept-format} \u2014 un concept, 4 formats SKOS plausibles.
const CONCEPT_EXPORT_FORMATS = [
  { key: "rdf",     label: "RDF/XML",  ext: "rdf",  mime: "application/rdf+xml" },
  { key: "json",    label: "JSON",     ext: "json", mime: "application/json" },
  { key: "jsonld",  label: "JSON-LD",  ext: "jsonld", mime: "application/ld+json" },
  { key: "turtle",  label: "Turtle",   ext: "ttl",  mime: "text/turtle" }
];
function conceptExportText(c, fmt) {
  const uri = c.uri && c.uri !== "\u2014" ? c.uri : "https://ark.frantiq.fr/" + (c.permalink || c.id);
  const esc = (s) => String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/"/g, "&quot;");
  if (fmt === "json") {
    return JSON.stringify({ id: c.id, uri, prefLabel: c.pref, altLabels: c.alts || [],
      broader: c.bt || [], narrower: c.nt || [], related: c.rt || [], definition: c.def || "", notation: c.notation }, null, 2);
  }
  if (fmt === "jsonld") {
    return JSON.stringify({ "@context": "http://www.w3.org/2004/02/skos/core#", "@id": uri, "@type": "skos:Concept",
      "skos:prefLabel": { "@value": c.pref, "@language": "fr" },
      "skos:broader": (c.bt || []).map(l => ({ "skos:prefLabel": l })),
      "skos:definition": c.def ? { "@value": c.def, "@language": "fr" } : undefined }, null, 2);
  }
  if (fmt === "turtle") {
    return '@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n<' + uri + '> a skos:Concept ;\n  skos:prefLabel "' + (c.pref || "").replace(/"/g, '\\"') + '"@fr'
      + (c.bt && c.bt.length ? ' ;\n  skos:broader "' + c.bt[0].replace(/"/g, '\\"') + '"' : "")
      + (c.def ? ' ;\n  skos:definition "' + c.def.replace(/"/g, '\\"') + '"@fr' : "") + ' .\n';
  }
  return '<?xml version="1.0" encoding="UTF-8"?>\n<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:skos="http://www.w3.org/2004/02/skos/core#">\n'
    + '  <skos:Concept rdf:about="' + esc(uri) + '">\n    <skos:prefLabel xml:lang="fr">' + esc(c.pref) + '</skos:prefLabel>\n'
    + (c.def ? '    <skos:definition xml:lang="fr">' + esc(c.def) + '</skos:definition>\n' : "")
    + '  </skos:Concept>\n</rdf:RDF>\n';
}
function exportConcept(c, fmtKey) {
  const f = CONCEPT_EXPORT_FORMATS.find(x => x.key === fmtKey) || CONCEPT_EXPORT_FORMATS[0];
  downloadFile(slugify(c.pref) + "." + f.ext, conceptExportText(c, f.key), f.mime);
}

// ── Panneau d'options du tableau (rendu DANS la roue crantée ⚙, comme les
//    autres vues) : colonnes, types d'éléments, export. (Les filtres candidats
//    avancés vivent dans le sélecteur de statut, cf. StatusChecklist.) ──
function TableOptions({ cfg, onCfg }) {
  cfg = cfg || FLAT_DEFAULT_CFG;
  const patch = (p) => onCfg && onCfg({ ...cfg, ...p });
  const toggleCol = (key) => {
    const has = cfg.cols.includes(key);
    patch({ cols: has ? cfg.cols.filter(k => k !== key) : [...cfg.cols, key] });
  };
  return (
    <div className="topt">
      <div className="topt-h">Colonnes</div>
      <div className="tbl-collist">
        {FLAT_COLS.filter(col => col.key !== "classes" || clsEnabled()).map(col => (
          <label key={col.key} className={"tbl-colrow" + (col.always ? " is-locked" : "")}>
            <input type="checkbox" checked={col.always || cfg.cols.includes(col.key)}
                   disabled={col.always} onChange={() => toggleCol(col.key)} />
            <span className="tbl-colbox" aria-hidden="true">{(col.always || cfg.cols.includes(col.key)) ? <Icon name="check" /> : ""}</span>
            <span>{col.label}</span>
            {col.always && <span className="tbl-lock">figée</span>}
          </label>
        ))}
      </div>
    </div>
  );
}

// ── Vue à plat : tableau des concepts dans le panneau de gauche (comme l'arbre).
//    Tri par clic sur en-tête, filtre texte par colonne, 1re colonne NON figée. ──
function FlatList({ concepts, statusSet, candFilter, cfg, onCfg, activeId, onOpen, selected, onToggleSel }) {
  cfg = cfg || FLAT_DEFAULT_CFG;
  const patch = (p) => onCfg && onCfg({ ...cfg, ...p });
  const { rows, visCols } = computeFlatRows(concepts, statusSet, cfg, candFilter);
  const setFilter = (k, v) => patch({ filters: { ...cfg.filters, [k]: v } });
  const toggleSort = (key) => {
    if (cfg.sortCol === key) patch({ sortDir: -cfg.sortDir });
    else patch({ sortCol: key, sortDir: 1 });
  };
  const clearFilters = () => patch({ filters: {} });
  const allIds = rows.map(r => r.id);
  const allSel = allIds.length > 0 && allIds.every(id => selected && selected.has(id));
  const someSel = allIds.some(id => selected && selected.has(id));
  const toggleAll = () => onToggleSel(allIds, !allSel);
  return (
    <div className="flat-wrap">
      <table className="flat">
        <thead>
          <tr>
            {onToggleSel && <th className="flat-check-h">
              <span className={"tn-check" + (allSel ? " on" : someSel ? " mixed" : "")}
                    title={allSel ? "Tout désélectionner" : "Tout sélectionner"}
                    onClick={toggleAll}>{allSel ? <Icon name="check" /> : someSel ? "–" : ""}</span>
            </th>}
            {visCols.map(col => (
              <th key={col.key} className={"flat-th" + (cfg.sortCol === col.key ? " is-sorted" : "")}
                  onClick={() => toggleSort(col.key)}>
                <span className="flat-th-l">{col.label}</span>
                <span className="flat-th-s">{cfg.sortCol === col.key ? (cfg.sortDir > 0 ? "▲" : "▼") : "↕"}</span>
              </th>
            ))}
          </tr>
          <tr className="flat-filters">
            {onToggleSel && <th className="flat-check-h"></th>}
            {visCols.map(col => (
              <th key={col.key}>
                <input className="flat-fi" placeholder="Filtrer…"
                       value={cfg.filters[col.key] || ""}
                       onChange={e => setFilter(col.key, e.target.value)}
                       onClick={e => e.stopPropagation()} />
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map(c => {
            const st = statusOf(c);
            const isSel = selected && selected.has(c.id);
            return (
              <tr key={c.id} onClick={() => onOpen(c)}
                  className={(activeId === c.id ? "is-active" : "") + (st === "candidat" ? " is-candidate" : "") + (isSel ? " is-sel" : "")}>
                {onToggleSel &&
                  <td className="flat-check-c" onClick={(e) => { e.stopPropagation(); onToggleSel([c.id], !isSel); }}>
                    <span className={"tn-check" + (isSel ? " on" : "")}>{isSel ? <Icon name="check" /> : ""}</span>
                  </td>}
                {visCols.map(col => {
                  if (col.key === "concept") return (
                    <td key={col.key} data-label={col.label} className="flat-name">
                      <span className="flat-text"><Highlight text={LBL(c.pref)} query={cfg.filters.concept || ""} on={true} /></span>
                    </td>
                  );
                  if (col.pill) return <td key={col.key} data-label={col.label}><StatusPill status={st} /></td>;
                  return <td key={col.key} data-label={col.label} className={col.mono ? "flat-mono" : ""}>{col.get(c)}</td>;
                })}
              </tr>
            );
          })}
          {rows.length === 0 && (
            <tr className="flat-empty-row"><td colSpan={visCols.length + (onToggleSel ? 1 : 0)}>
              Aucune ligne. <button className="empty-link" onClick={clearFilters}>Réinitialiser les filtres</button>
            </td></tr>
          )}
        </tbody>
      </table>
      <div className="flat-count">{rows.length} ligne{rows.length > 1 ? "s" : ""}</div>
    </div>
  );
}

function Tree({ root, activeId, activeChain, onOpen, statusSet, candFilter, showNotation = false, showContext = false, sort = "alpha", selected, onToggleSel, onCreate }) {
  const sorter = sort === "nota"
    ? (a, b) => String(a.concept?.notation || "~").localeCompare(String(b.concept?.notation || "~"), "fr")
    : (a, b) => a.name.localeCompare(b.name, "fr");
  const autoOpen = !statusSet.has("valide");
  const tops = [...root.children.values()]
    .filter(k => keepTreeNode(k, statusSet, candFilter))
    .sort(sorter);
  const [openSet, setOpenSet] = useState(() => new Set(tops.map(n => n.name)));
  const chainKey = activeChain ? activeChain.join("/") : "";

  useEffect(() => {
    if (!activeChain) return;
    setOpenSet((prev) => {
      const next = new Set(prev);
      for (let k = 1; k < activeChain.length; k++) next.add(activeChain.slice(0, k).join("/"));
      return next;
    });
  }, [chainKey]);

  const toggle = (key) => setOpenSet((prev) => {
    const n = new Set(prev);
    n.has(key) ? n.delete(key) : n.add(key);
    return n;
  });

  return (
    <div className="tree-body">
      {tops.map((n) => (
        <TreeRow key={n.name} node={n} depth={0} activeId={activeId}
                 openSet={openSet} toggle={toggle} onOpen={onOpen}
                 statusSet={statusSet} candFilter={candFilter} autoOpen={autoOpen} showNotation={showNotation} showContext={showContext} sort={sort}
                 selected={selected} onToggleSel={onToggleSel} onCreate={onCreate} />
      ))}
    </div>
  );
}

// ═════════════════════════════════════════════════════════════════
//  Rail de navigation (« Nav opentheso ») + icônes vectorielles.
// ═════════════════════════════════════════════════════════════════
function LogoMark() {
  return <img src="assets/logo-opentheso.png" alt="" aria-hidden="true" className="logo-mark" />;
}

function LangFilter({ value, options, onChange }) {
  const [open, setOpen] = React.useState(false);
  const ref = React.useRef(null);
  React.useEffect(() => {
    const onDoc = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);
  const toggle = (code) => onChange(value.includes(code) ? value.filter(c => c !== code) : [...value, code]);
  // Pas de drapeau ici : une langue n'est pas un pays (l'anglais, l'espagnol...
  // sont parlés dans plusieurs), donc juste le nom de la langue.
  const label = value.length === 0
    ? "Toutes"
    : value.map(c => ENDONYM[c] || c).join(", ");
  return (
    <div className="tp-langf" ref={ref} onClick={e => e.stopPropagation()}>
      <button className={"tp-fi tp-langf-btn" + (value.length ? " has-val" : "")} onClick={() => setOpen(o => !o)}>
        <span className="tp-langf-lbl">{label}</span>
        <span className="tp-langf-caret">▾</span>
      </button>
      {open && (
        <div className="tp-langf-menu">
          {value.length > 0 &&
            <button className="tp-langf-clear" onClick={() => onChange([])}>Tout décocher</button>}
          {options.map(l => (
            <label key={l} className={"tp-langf-opt" + (value.includes(l) ? " is-on" : "")}>
              <input type="checkbox" checked={value.includes(l)} onChange={() => toggle(l)} />
              <span className="tp-langf-name">{ENDONYM[l] || l}</span>
            </label>
          ))}
        </div>
      )}
    </div>
  );
}

function ColumnsConfig({ cfg, onCfg, labels, onReset }) {
  const [open, setOpen] = React.useState(false);
  const [pos, setPos] = React.useState({ top: 0, left: 0 });
  const ref = React.useRef(null);
  const btnRef = React.useRef(null);
  const [drag, setDrag] = React.useState(null);
  React.useEffect(() => {
    const onDoc = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);
  React.useLayoutEffect(() => {
    if (open && btnRef.current) {
      const r = btnRef.current.getBoundingClientRect();
      setPos({ top: r.bottom + 6, left: r.left });
    }
  }, [open]);
  const move = (i, dir) => {
    const j = i + dir;
    if (j < 0 || j >= cfg.length) return;
    const n = [...cfg];[n[i], n[j]] = [n[j], n[i]]; onCfg(n);
  };
  const toggle = (i) => { const n = cfg.map((c, k) => k === i ? { ...c, on: !c.on } : c); onCfg(n); };
  const onDrop = (i) => {
    if (drag === null || drag === i) return;
    const n = [...cfg]; const [it] = n.splice(drag, 1); n.splice(i, 0, it); onCfg(n); setDrag(null);
  };
  return (
    <span className="tp-cols" ref={ref}>
      <button className={"tp-cfg-btn" + (open ? " is-on" : "")} ref={btnRef} onClick={e => { e.stopPropagation(); setOpen(o => !o); }}
              title="Configurer les colonnes (affichage & ordre)" aria-label="Configurer les colonnes">
        <Icon name="settings" />
      </button>
      {open && (
        <div className="tp-cols-menu" style={{ top: pos.top, left: pos.left }} onClick={e => e.stopPropagation()}>
          <div className="tp-cols-h">Colonnes du tableau<span>glisser pour réordonner</span></div>
          <div className="tp-cols-list">
            {cfg.map((c, i) => (
              <div key={c.key}
                   className={"tp-cols-row" + (drag === i ? " is-drag" : "")}
                   draggable onDragStart={() => setDrag(i)} onDragEnd={() => setDrag(null)}
                   onDragOver={e => e.preventDefault()} onDrop={() => onDrop(i)}>
                <span className="tp-cols-grip" title="Glisser">⋮⋮</span>
                <label className="tp-cols-lbl">
                  <input type="checkbox" checked={c.on} disabled={c.key === "name"} onChange={() => toggle(i)} />
                  <span>{labels[c.key]}</span>
                </label>
                <span className="tp-cols-ord">
                  <button className="tp-cols-mv" disabled={i === 0} onClick={() => move(i, -1)} title="Monter">▲</button>
                  <button className="tp-cols-mv" disabled={i === cfg.length - 1} onClick={() => move(i, 1)} title="Descendre">▼</button>
                </span>
              </div>
            ))}
          </div>
          <button className="tp-cols-reset" onClick={onReset}>Réinitialiser l'ordre par défaut</button>
        </div>
      )}
    </span>
  );
}

// ═════════════════════════════════════════════════════════════════
//  RÔLES — deux niveaux (PRO-6). Application = l'instance OpenTheso,
//  Thésaurus = droits par thésaurus. La table CAPS est PROVISOIRE : elle sera
//  remplacée par la matrice validée avec les métiers. Tout l'affichage
//  conditionnel passe par perm.can(…) — un seul endroit à corriger ensuite.
// ═════════════════════════════════════════════════════════════════
const ROLE_APP = [["super_admin", "Super admin"], ["admin_app", "Admin application"], ["user", "Utilisateur"]];
const ROLE_TH  = [["super_admin", "Super admin"], ["admin", "Admin"], ["manager", "Manager"], ["contributor", "Contributor"]];
const ROLE_NAME = Object.fromEntries([...ROLE_APP, ...ROLE_TH]);
const CAPS = {
  "app.createThesaurus": ["super_admin", "admin_app"],
  "app.deleteThesaurus": ["super_admin"],
  "app.manageUsers":     ["super_admin"],
  "app.manageOrgs":      ["super_admin"],
  "app.assignRoles":     ["super_admin"],
  "app.serverSettings":  ["super_admin"],
  "app.instanceStats":   ["super_admin", "admin_app"],
  "th.proposeCandidate": ["super_admin", "admin", "manager", "contributor"],
  "th.validateCandidate":["super_admin", "admin", "manager"],
  "th.editConcept":      ["super_admin", "admin", "manager"],
  "th.deleteConcept":    ["super_admin", "admin"],
  "th.batch":            ["super_admin", "admin", "manager"],
  "th.move":             ["super_admin", "admin"],
  "th.settings":         ["super_admin", "admin"],
  "th.manageUsers":      ["super_admin", "admin"],
  "th.stats":            ["super_admin", "admin", "manager"]
};
function makePerm(roleApp, roleTh) {
  const can = (k) => {
    const allowed = CAPS[k];
    if (!allowed) return true;
    return allowed.includes(k.slice(0, 4) === "app." ? roleApp : roleTh);
  };
  return { roleApp, roleTh, can, isSuperAdmin: roleApp === "super_admin" };
}
function RoleSelect({ level, value, onChange, compact }) {
  const opts = level === "app" ? ROLE_APP : ROLE_TH;
  return (
    <select className={"role-sel" + (compact ? " is-compact" : "")} value={value}
            onChange={e => onChange && onChange(e.target.value)}>
      {opts.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
    </select>
  );
}

// ACC-3 (intermédiaire) — zone d'administration de l'instance, sur l'écran amont.
// Tout est carte ; chaque carte porte ses entrées de 1er niveau et ouvre en pleine largeur.
const ADMIN_CARDS = [
  { key: "users", cap: "app.manageUsers", icon: "users", title: "Utilisateurs",
    desc: "Comptes de l'instance et rôle au niveau de l'application." },
  { key: "thes", cap: "app.deleteThesaurus", icon: "library", title: "Thésaurus & membres",
    desc: "Thésaurus de l'instance, leurs membres et leurs rôles." },
  { key: "orgs", cap: "app.manageOrgs", icon: "layers", title: "Organisations & projets",
    desc: "Organisations, projets et leurs membres." },
  { key: "server", cap: "app.serverSettings", icon: "sliders", title: "Paramètres serveur",
    desc: "ARK / handle, API, sauvegardes, courriel sortant." },
  { key: "stats", cap: "app.instanceStats", icon: "activity", title: "Statistiques de l'instance",
    desc: "Activité tous thésaurus confondus, comptes actifs." }
];
// PATTERN « page mère → sous-pages » : la page mère ne liste QUE ses sous-pages,
// une par ligne pleine largeur ; le détail vit dans la sous-page.
function SubPageList({ items, onOpen }) {
  return (
    <div className="spl">
      {items.map(c => (
        <button className="spl-row" key={c.key} onClick={() => onOpen(c.key)}>
          <span className="spl-ico"><Icon name={c.icon} /></span>
          <span className="spl-tx">
            <span className="spl-t">{c.title}</span>
            <span className="spl-d">{c.desc}</span>
          </span>
          <span className="spl-go"><Icon name="chevron-right" /></span>
        </button>
      ))}
    </div>
  );
}
function InstanceAdminZone({ perm, onAdmin }) {
  const items = ADMIN_CARDS.filter(c => perm.can(c.cap));
  if (!items.length) return null;
  return <SubPageList items={items} onOpen={onAdmin} />;
}

// Modale partagée — pour les actions de CRÉATION / INVITATION uniquement.
// (L'édition d'un concept, elle, reste en place dans ses blocs : cf. docs/01.)
function Modal({ title, sub, onClose, children, actions, wide }) {
  React.useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);
  return (
    <div className="mo-ov" onMouseDown={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={"mo" + (wide ? " mo--wide" : "")} role="dialog" aria-modal="true" aria-label={title}>
        <div className="mo-h">
          <div><div className="mo-t">{title}</div>{sub && <div className="mo-s">{sub}</div>}</div>
          <button className="mo-x" onClick={onClose} title="Fermer" aria-label="Fermer"><Icon name="x" className="ico-inline"/></button>
        </div>
        <div className="mo-b">{children}</div>
        {actions && <div className="mo-f">{actions}</div>}
      </div>
    </div>
  );
}

// Fil d'Ariane — au-dessus du titre, montre le chemin complet. Chaque segment
// sauf le dernier est cliquable et remonte à ce niveau.
function Breadcrumb({ items }) {
  return (
    <nav className="bc" aria-label="Fil d'Ariane">
      {items.map((it, i) => {
        const last = i === items.length - 1;
        return (
          <React.Fragment key={i}>
            {last
              ? <span className="bc-cur" aria-current="page">{it.label}</span>
              : <button className="bc-lk" onClick={it.onClick}>{it.label}</button>}
            {!last && <span className="bc-sep" aria-hidden="true">›</span>}
          </React.Fragment>
        );
      })}
    </nav>
  );
}

function ThesaurusPicker({ thesauri, session, onOpen, variant = "tabs", perm, onAdmin }) {
  const [q, setQ] = React.useState("");
  const [tab, setTab] = React.useState("all");        // all | member | public  (variant "tabs")
  const [access, setAccess] = React.useState("all");  // filtre chip (variant "badge")
  const [sort, setSort] = React.useState({ col: "name", dir: 1 });
  const [colF, setColF] = React.useState({ lang: [] });          // filtres par colonne (lang = tableau de codes)
  const [addMode, setAddMode] = React.useState(null); // null | "choose" | "create" | "import"
  const [dispMode, setDispMode] = React.useState("flat"); // flat | domain ({domaines-dossiers})
  const [mFilters, setMFilters] = React.useState(false); // modale filtres + tri, mobile uniquement

  // Organisation → Projet → Thésaurus : les noms affichés se résolvent depuis
  // window.PROJECTS / window.ORGANISATIONS, THESAURI ne porte que projectId.
  const projectOf = (t) => (window.PROJECTS || []).find(p => p.id === t.projectId);
  const orgNameOf = (t) => { const p = projectOf(t); return p && (window.ORGANISATIONS || []).find(o => o.id === p.orgId); };
  const projectName = (t) => (projectOf(t) || {}).name || "";
  const orgName = (t) => (orgNameOf(t) || {}).name || "";
  const domainName = (t) => ((window.DOMAINS || []).find(d => d.id === t.domainId) || {}).name || "";

  const COLS = [
    { key: "name",     label: "Nom du thésaurus", filter: true,  grow: true },
    { key: "projects", label: "Projets",          filter: true },
    { key: "access",   label: "Accès",            filter: false },
    { key: "terms",    label: "Termes",           filter: false, num: true },
    { key: "created",  label: "Créé le",          filter: false, mono: true },
    { key: "domain",   label: "Domaine",          filter: true },
    { key: "org",      label: "Organisation",     filter: true },
    { key: "chrono",   label: "Chronologie",      filter: false, mono: true },
    { key: "lang",     label: "Langues",          filter: "select" }
  ];
  const COLMAP = React.useMemo(() => Object.fromEntries(COLS.map(c => [c.key, c])), []);
  const COLLABELS = React.useMemo(() => Object.fromEntries(COLS.map(c => [c.key, c.label])), []);
  const DEFAULT_CFG = COLS.map(c => ({ key: c.key, on: true }));
  const [colCfg, setColCfg] = React.useState(() => {
    try {
      const saved = JSON.parse(localStorage.getItem("ot-tp-cols") || "null");
      if (Array.isArray(saved) && saved.length && saved.every(s => COLS.some(c => c.key === s.key))
          && COLS.every(c => saved.some(s => s.key === c.key))) return saved;
    } catch (e) {}
    return DEFAULT_CFG;
  });
  React.useEffect(() => { localStorage.setItem("ot-tp-cols", JSON.stringify(colCfg)); }, [colCfg]);
  // Colonnes visibles dans l'ordre configuré (access retiré en mode sections).
  const orderedCols = colCfg.filter(c => c.on).map(c => COLMAP[c.key]).filter(Boolean);
  const LANG_OPTS = React.useMemo(() =>
    [...new Set(thesauri.flatMap(t => t.lang))].sort((a, b) => (ENDONYM[a] || a).localeCompare(ENDONYM[b] || b, "fr")), [thesauri]);

  const nQ = norm(q);
  const setCol = (k, v) => setColF(o => ({ ...o, [k]: v }));
  const toggleSort = (k) => setSort(s => s.col === k ? { col: k, dir: -s.dir } : { col: k, dir: 1 });

  const filtered = React.useMemo(() => {
    let rows = thesauri.filter(t => {
      if (nQ && !(norm(t.name).includes(nQ) || norm(domainName(t)).includes(nQ) || norm(orgName(t)).includes(nQ) || norm(projectName(t)).includes(nQ))) return false;
      for (const [k, v] of Object.entries(colF)) {
        if (k === "lang") { if (Array.isArray(v) && v.length && !v.some(code => t.lang.includes(code))) return false; }
        else if (k === "org") { if (v && !norm(orgName(t)).includes(norm(v))) return false; }
        else if (k === "projects") { if (v && !norm(projectName(t)).includes(norm(v))) return false; }
        else if (k === "domain") { if (v && !norm(domainName(t)).includes(norm(v))) return false; }
        else if (v && !norm(String(t[k] || "")).includes(norm(v))) return false;
      }
      return true;
    });
    const s = sort;
    rows = [...rows].sort((a, b) => {
      let av = a[s.col], bv = b[s.col];
      if (s.col === "terms") return (av - bv) * s.dir;
      if (s.col === "org") { av = orgName(a); bv = orgName(b); }
      if (s.col === "projects") { av = projectName(a); bv = projectName(b); }
      if (s.col === "domain") { av = domainName(a); bv = domainName(b); }
      return String(av || "").localeCompare(String(bv || ""), "fr", { numeric: true }) * s.dir;
    });
    return rows;
  }, [thesauri, nQ, colF, sort]);

  const members = filtered.filter(t => t.access === "member");
  const publics = filtered.filter(t => t.access === "public");
  const memberCount = thesauri.filter(t => t.access === "member").length;
  const publicCount = thesauri.filter(t => t.access === "public").length;

  // Lignes réellement affichées compte tenu de l'onglet actif (avant mise en
  // forme) : la base commune du mode liste plate ET du mode par domaine.
  const visibleRows = variant === "tabs"
    ? (tab === "all" ? filtered : filtered.filter(t => t.access === tab))
    : (access === "all" ? filtered : filtered.filter(t => t.access === access));

  // Mode "par domaine" ({domaines-dossiers}) : replié par défaut — on voit
  // d'abord juste les domaines, on explore en dépliant. Pendant une
  // recherche/un filtre actif, tout se déplie automatiquement (sinon un
  // résultat pertinent resterait caché derrière un domaine fermé) ; l'état
  // manuel de repli/dépli n'est pas perdu pour autant, juste ignoré le temps
  // du filtre.
  const [domOpen, setDomOpen] = React.useState(() => new Set());
  const toggleDom = (id) => setDomOpen(s => { const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n; });
  const isFiltering = !!nQ || Object.values(colF).some(v => Array.isArray(v) ? v.length : v);
  const domainGroups = React.useMemo(() => {
    const all = window.DOMAINS || [];
    const childrenOf = (pid) => all.filter(d => (d.parentId || null) === (pid || null))
      .sort((a, b) => a.name.localeCompare(b.name, "fr"));
    const rowsOf = (did) => visibleRows.filter(t => t.domainId === did);
    const totalOf = (did) => rowsOf(did).length + childrenOf(did).reduce((n, c) => n + totalOf(c.id), 0);
    const out = [];
    const walk = (pid, depth) => childrenOf(pid).forEach(d => {
      const total = totalOf(d.id);
      if (total === 0) return; // domaine vide sans descendant utile = masqué, pas de bruit
      const kids = childrenOf(d.id);
      const open = isFiltering || domOpen.has(d.id);
      out.push({ domain: d, depth, rows: rowsOf(d.id), total, open, canOpen: total > 0 });
      if (open) walk(d.id, depth + 1);
    });
    walk(null, 0);
    return out;
  }, [visibleRows, domOpen, isFiltering]);

  if (addMode === "create") return <CreateThesaurusForm onBack={() => setAddMode(null)} onCreate={onOpen} />;
  if (addMode === "import") return <ImportThesaurusForm onBack={() => setAddMode(null)} onCreate={onOpen} />;

  const AccessBadge = ({ a, role }) => a === "member"
    ? <span className="tp-badge tp-badge--member" title={"Vous êtes " + (role || "membre")}><Icon name="key" />{role || "Membre"}</span>
    : <span className="tp-badge tp-badge--public"><Icon name="globe" />Public</span>;

  const cellFor = (t, key) => {
    switch (key) {
      case "name":    return <td data-label="Thésaurus" className="tp-c-name tp-col-name" key={key}><span className="tp-c-name-in">
        <span className="tp-name">{t.name}</span>
      </span></td>;
      case "access":  return <td data-label="Accès" key={key}><AccessBadge a={t.access} role={t.role} /></td>;
      case "terms":   return <td data-label="Termes" className="tp-num" key={key}>{t.terms.toLocaleString("fr")}</td>;
      case "created": return <td data-label="Créé le" className="tp-mono" key={key}>{t.created}</td>;
      case "domain":  return <td data-label="Domaine" key={key}>{domainName(t)}</td>;
      case "projects":return <td data-label="Projets" key={key}>{projectName(t)}</td>;
      case "org":     return <td data-label="Organisation" key={key}>{orgName(t)}</td>;
      case "chrono":  return <td data-label="Chronologie" className="tp-mono" key={key}>{t.chrono}</td>;
      case "lang":    return <td data-label="Langues" className="tp-c-lang" key={key}>{t.lang.map(l => <span key={l} className="tp-lang">{ENDONYM[l] || l}</span>)}</td>;
      default: return <td key={key} />;
    }
  };

  const renderRow = (cols) => (t) => (
    <tr key={t.id} onClick={() => onOpen(t.id)} tabIndex={0}
        onKeyDown={e => { if (e.key === "Enter") onOpen(t.id); }}>
      <td className="tp-c-cfg" />
      {cols.map(c => cellFor(t, c.key))}
    </tr>
  );

  // Ligne de séparation compacte entre groupes de domaine — reste DANS la
  // même table (23-09 : une vraie <table> par domaine
  // répétait tout l'en-tête + la ligne de filtres, beaucoup trop d'espace
  // pour un domaine avec 1 seul thésaurus).
  const renderGroupRow = (cols) => ({ domain, depth, total, open }) => (
    <tr className="tp-group-row" key={"g-" + domain.id} onClick={() => toggleDom(domain.id)}
        tabIndex={0} onKeyDown={e => { if (e.key === "Enter") toggleDom(domain.id); }}>
      <td colSpan={cols.length + 1} style={{ paddingLeft: 12 + depth * 20 }}>
        <span className="tp-group-h">
          <span className="tp-group-caret"><Icon name={open ? "chevron-down" : "chevron-right"} /></span>
          <Icon name="folders" />{domain.name}
          <span className="tp-group-n">{total}</span></span>
      </td>
    </tr>
  );

  // Fonction de rendu, pas composant : un composant défini ici serait recréé à
  // chaque rendu et React remonterait le tableau (filtres : focus perdu à chaque frappe).
  const renderTable = ({ rows, groups, showAccess = true }) => {
    const cols = orderedCols.filter(c => !(c.key === "access" && !showAccess));
    const empty = groups ? groups.every(g => g.rows.length === 0) : rows.length === 0;
    return (
    <table className="tp-table">
      <thead>
        <tr>
          <th className="tp-th tp-th--cfg">
            <ColumnsConfig cfg={colCfg} onCfg={setColCfg} labels={COLLABELS} onReset={() => setColCfg(DEFAULT_CFG)} />
          </th>
          {cols.map(c => (
            <th key={c.key} className={"tp-th" + (c.grow ? " tp-th--grow" : "") + (c.key === "name" ? " tp-th--name" : "") + (sort.col === c.key ? " is-sorted" : "")}
                onClick={() => toggleSort(c.key)}>
              <span className="tp-th-l">{c.label}</span>
              {sort.col === c.key && <span className="tp-th-s">{sort.dir > 0 ? "▲" : "▼"}</span>}
            </th>
          ))}
        </tr>
        <tr className="tp-filters">
          <th className="tp-th--cfg" />
          {cols.map(c => (
            <th key={c.key} className={c.key === "name" ? "tp-fth--name" : ""}>
              {c.filter === "select"
                ? <LangFilter value={colF.lang || []} options={LANG_OPTS} onChange={v => setCol("lang", v)} />
                : c.filter
                ? <input className="tp-fi" placeholder="Filtrer…" value={colF[c.key] || ""}
                         onClick={e => e.stopPropagation()}
                         onChange={e => setCol(c.key, e.target.value)} />
                : null}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {empty
          ? <tr className="tp-empty-row"><td colSpan={cols.length + 1}>Aucun thésaurus ne correspond.</td></tr>
          : groups
          ? groups.flatMap(g => [renderGroupRow(cols)(g), ...(g.open ? g.rows.map(renderRow(cols)) : [])])
          : rows.map(renderRow(cols))}
      </tbody>
    </table>
    );
  };

  return (
    <div className="tp">
      <div className="tp-gov">
        <a className="tp-gov-mark" href="#" onClick={e => e.preventDefault()}
           title="Retour à l'accueil — Ministère de la Culture">
          <img src="assets/ministere-culture.png" alt="Ministère de la Culture — Liberté, Égalité, Fraternité" />
        </a>
        <a className="tp-gov-lk" href="https://www.culture.gouv.fr" target="_blank" rel="noreferrer">culture.gouv.fr<span className="gv-ext" aria-hidden="true"><Icon name="external" /></span></a>
      </div>
      <div className="tp-head">
        <div>
          <h1 className="tp-title">OpenTheso</h1>
          <p className="tp-sub">Sélectionnez un thésaurus pour l'ouvrir, ou créez-en un nouveau.</p>
        </div>
      </div>

      <div className="tp-bar">
        <div className="tp-search">
          <span className="tp-search-ico"><Icon name="search" /></span>
          <input className="tp-search-input" value={q} onChange={e => setQ(e.target.value)}
                 placeholder="Rechercher un thésaurus (nom, domaine, organisation, projet…)" spellCheck={false} />
          {q && <button className="tp-search-x" onClick={() => setQ("")} title="Effacer"><Icon name="x" className="ico-inline"/></button>}
        </div>
        <button className="tp-mfilter" onClick={() => setMFilters(true)} aria-label="Filtres et tri">
          <Icon name="sliders" />Filtres
          {Object.values(colF).some(v => Array.isArray(v) ? v.length : v) &&
            <span className="tp-tab-n">{Object.values(colF).filter(v => Array.isArray(v) ? v.length : v).length}</span>}
        </button>
        <button className="tp-create" onClick={() => setAddMode("choose")}>
          <Icon name="plus" />Ajouter un thésaurus
        </button>
      </div>

      {/* Sous 640px l'en-tête du tableau (filtres par colonne + tri) est masqué :
          mêmes filtres et même tri, regroupés ici ({filtres-accueil-mobile}). */}
      {mFilters && (
        <Modal title="Filtres et tri" onClose={() => setMFilters(false)}
               actions={<>
                 <button type="button" className="bo-btn ghost"
                         onClick={() => { setColF({ lang: [] }); setSort({ col: "name", dir: 1 }); }}>Effacer</button>
                 <button type="button" className="bo-btn primary" onClick={() => setMFilters(false)}>
                   Voir {visibleRows.length} thésaurus</button>
               </>}>
          <div className="tp-mf">
            <div className="tp-mf-row">
              <span className="tp-mf-l">Trier par</span>
              <span className="tp-mf-sort">
                <select className="tp-fi" value={sort.col} aria-label="Colonne de tri"
                        onChange={e => setSort(o => ({ ...o, col: e.target.value }))}>
                  {COLS.map(c => <option key={c.key} value={c.key}>{c.label}</option>)}
                </select>
                <button type="button" className="tp-fi tp-mf-dir" onClick={() => setSort(o => ({ ...o, dir: -o.dir }))}>
                  {sort.dir > 0 ? "Croissant ▲" : "Décroissant ▼"}</button>
              </span>
            </div>
            {COLS.filter(c => c.filter === true).map(c => (
              <label key={c.key} className="tp-mf-row">
                <span className="tp-mf-l">{c.label}</span>
                <input className="tp-fi" placeholder="Filtrer…" value={colF[c.key] || ""}
                       onChange={e => setCol(c.key, e.target.value)} />
              </label>
            ))}
            <div className="tp-mf-row">
              <span className="tp-mf-l">Langues</span>
              <div className="tp-mf-langs">
                {LANG_OPTS.map(l => {
                  const on = (colF.lang || []).includes(l);
                  return (
                    <label key={l} className={"tp-langf-opt" + (on ? " is-on" : "")}>
                      <input type="checkbox" checked={on}
                             onChange={() => setCol("lang", on ? colF.lang.filter(x => x !== l) : [...(colF.lang || []), l])} />
                      <span className="tp-langf-name">{ENDONYM[l] || l}</span>
                    </label>
                  );
                })}
              </div>
            </div>
          </div>
        </Modal>
      )}

      {addMode === "choose" && (
        <Modal title="Ajouter un thésaurus" onClose={() => setAddMode(null)}>
          <div className="spl">
            <button className="spl-row" onClick={() => setAddMode("create")}>
              <span className="spl-ico"><Icon name="plus" /></span>
              <span className="spl-tx">
                <span className="spl-t">Créer</span>
                <span className="spl-d">Nouveau thésaurus vide, à construire depuis zéro.</span>
              </span>
              <span className="spl-go"><Icon name="chevron-right" /></span>
            </button>
            <button className="spl-row" onClick={() => setAddMode("import")}>
              <span className="spl-ico"><Icon name="upload" /></span>
              <span className="spl-tx">
                <span className="spl-t">Importer</span>
                <span className="spl-d">Depuis un fichier CSV ou RDF existant.</span>
              </span>
              <span className="spl-go"><Icon name="chevron-right" /></span>
            </button>
          </div>
        </Modal>
      )}

      {variant === "tabs" && (
        <div className="tp-tabs">
          <button className={"tp-tab" + (tab === "all" ? " is-on" : "")} onClick={() => setTab("all")}>
            Tous<span className="tp-tab-n">{filtered.length}</span></button>
          <button className={"tp-tab" + (tab === "member" ? " is-on" : "")} onClick={() => setTab("member")}>
            <Icon name="key" />Mes thésaurus<span className="tp-tab-n">{memberCount}</span></button>
          <button className={"tp-tab" + (tab === "public" ? " is-on" : "")} onClick={() => setTab("public")}>
            <Icon name="globe" />Publics<span className="tp-tab-n">{publicCount}</span></button>
          <div className="tp-dispmode">
            <button className={"tp-dispmode-b" + (dispMode === "flat" ? " is-on" : "")}
                    onClick={() => setDispMode("flat")} title="Liste plate" aria-label="Liste plate">
              <Icon name="grid" />Liste</button>
            <button className={"tp-dispmode-b" + (dispMode === "domain" ? " is-on" : "")}
                    onClick={() => setDispMode("domain")} title="Regroupé par domaine" aria-label="Regroupé par domaine">
              <Icon name="folders" />Par domaine</button>
          </div>
        </div>
      )}
      {variant === "badge" && (
        <div className="tp-tabs">
          <button className={"tp-chip" + (access === "all" ? " is-on" : "")} onClick={() => setAccess("all")}>Tous</button>
          <button className={"tp-chip" + (access === "member" ? " is-on" : "")} onClick={() => setAccess("member")}>
            <Icon name="key" />Membre</button>
          <button className={"tp-chip" + (access === "public" ? " is-on" : "")} onClick={() => setAccess("public")}>
            <Icon name="globe" />Public</button>
        </div>
      )}

      <div className="tp-body">
        {variant === "sections" ? (
          <>
            <div className="tp-section">
              <div className="tp-section-h"><Icon name="key" /><span>Mes thésaurus</span>
                <span className="tp-section-n">{members.length}</span>
                <span className="tp-section-d">thésaurus où vous avez des droits</span></div>
              <div className="tp-table-wrap">{renderTable({ rows: members, showAccess: false })}</div>
            </div>
            <div className="tp-section">
              <div className="tp-section-h"><Icon name="globe" /><span>Thésaurus publics</span>
                <span className="tp-section-n">{publics.length}</span>
                <span className="tp-section-d">visibles par tous, en lecture</span></div>
              <div className="tp-table-wrap">{renderTable({ rows: publics, showAccess: false })}</div>
            </div>
          </>
        ) : variant === "tabs" && dispMode === "domain" ? (
          <div className="tp-table-wrap">{renderTable({ groups: domainGroups })}</div>
        ) : (
          <div className="tp-table-wrap">
            {renderTable({ rows: visibleRows })}
          </div>
        )}
      </div>
      <InstitutionalFooter />
    </div>
  );
}

// ACC-4 — Bandeau institutionnel. Uniquement sur l'écran amont (liste des thésaurus) :
// dans un thésaurus, il alourdirait chaque écran de travail.
function InstitutionalFooter() {
  const OFF = [["legifrance.gouv.fr", "https://www.legifrance.gouv.fr"], ["info.gouv.fr", "https://www.info.gouv.fr"],
               ["service-public.fr", "https://www.service-public.fr"], ["data.gouv.fr", "https://www.data.gouv.fr"]];
  const MENTIONS = ["Mentions légales", "Données personnelles", "Accessibilité : partiellement conforme"];
  return (
    <footer className="gv">
      <img className="gv-mark" src="assets/ministere-culture.png" alt="Ministère de la Culture" />
      <nav className="gv-mentions" aria-label="Informations légales">
        {MENTIONS.map(m => <a key={m} href="#" onClick={e => e.preventDefault()}>{m}</a>)}
        <span className="gv-lic">Code sous licence CeCILL</span>
      </nav>
      <nav className="gv-links" aria-label="Sites publics">
        {OFF.map(([l, h]) => (
          <a key={l} href={h} target="_blank" rel="noreferrer">{l}<span className="gv-ext" aria-hidden="true"><Icon name="external" /></span></a>
        ))}
      </nav>
    </footer>
  );
}

function CreateThesaurusForm({ onBack, onCreate }) {
  const [name, setName] = React.useState("");
  const [lang, setLang] = React.useState("Français (fr)");
  const [domainId, setDomainId] = React.useState("");
  const [projectId, setProjectId] = React.useState("");
  const [access, setAccess] = React.useState("member");
  const projects = React.useMemo(() =>
    [...(window.PROJECTS || [])].sort((a, b) => a.name.localeCompare(b.name, "fr")), []);
  // Domaines = dossiers imbriqués ({domaines-dossiers}) : liste aplatie en
  // profondeur pour un <select>, indentée par du texte (pas de CSS fiable
  // sur <option> selon les navigateurs). N'importe quel niveau reste
  // choisissable, un domaine peut porter des thésaurus ET des sous-domaines.
  const domains = React.useMemo(() => {
    const all = window.DOMAINS || [];
    const childrenOf = (pid) => all.filter(d => (d.parentId || null) === (pid || null))
      .sort((a, b) => a.name.localeCompare(b.name, "fr"));
    const out = [];
    const walk = (pid, depth) => childrenOf(pid).forEach(d => { out.push({ ...d, depth }); walk(d.id, depth + 1); });
    walk(null, 0);
    return out;
  }, []);
  const valid = name.trim().length > 1 && !!projectId && !!domainId;

  return (
    <div className="tp tp--create">
      <button className="bo-back" onClick={onBack} title="Retour à la sélection"><Icon name="back" /></button>
      <div className="bo-head">
        <div className="bo-head-ico"><Icon name="library" /></div>
        <div><h1 className="bo-title">Créer un thésaurus</h1>
          <p className="bo-head-sub">Métadonnées de base. Vous pourrez tout affiner dans les paramètres une fois créé.</p></div>
      </div>

      <div className="tp-form">
        <div className="tp-form-card">
          <div className="tp-form-row">
            <label className="tp-form-l">Nom du thésaurus <span className="tp-req">*</span></label>
            <input className="tp-form-i" autoFocus value={name} onChange={e => setName(e.target.value)}
                   placeholder="ex. Céramologie médiévale" />
          </div>
          <div className="tp-form-row">
            <label className="tp-form-l">Langue par défaut</label>
            <select className="tp-form-i" value={lang} onChange={e => setLang(e.target.value)}>
              {["Français (fr)", "English (en)", "Español (es)", "Deutsch (de)", "Italiano (it)"].map(o => <option key={o}>{o}</option>)}
            </select>
          </div>
          <div className="tp-form-row">
            <label className="tp-form-l">Domaine <span className="tp-req">*</span></label>
            <select className="tp-form-i" value={domainId} onChange={e => setDomainId(e.target.value)}>
              <option value="" disabled>Choisir un domaine…</option>
              {domains.map(d => <option key={d.id} value={d.id}>{"— ".repeat(d.depth)}{d.name}</option>)}
            </select>
          </div>
          <div className="tp-form-row">
            <label className="tp-form-l">Projet de rattachement <span className="tp-req">*</span></label>
            <select className="tp-form-i" value={projectId} onChange={e => setProjectId(e.target.value)}>
              <option value="" disabled>Choisir un projet…</option>
              {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </div>
          <div className="tp-form-row">
            <label className="tp-form-l">Visibilité</label>
            <div className="tp-form-radios">
              <label className={"tp-form-radio" + (access === "member" ? " is-on" : "")}>
                <input type="radio" name="acc" checked={access === "member"} onChange={() => setAccess("member")} />
                <span><Icon name="key" /><b>Privé</b> — réservé aux membres avec droits</span></label>
              <label className={"tp-form-radio" + (access === "public" ? " is-on" : "")}>
                <input type="radio" name="acc" checked={access === "public"} onChange={() => setAccess("public")} />
                <span><Icon name="globe" /><b>Public</b> — visible par tous en lecture</span></label>
            </div>
          </div>
        </div>

        <div className="tp-form-run">
          <button className="tp-form-cancel" onClick={onBack}>Annuler</button>
          <button className="tp-form-submit" disabled={!valid}
                  onClick={() => onCreate && onCreate("pactols")}>
            <Icon name="check" />Créer le thésaurus</button>
        </div>
      </div>
    </div>
  );
}

// Import CSV/RDF : assistant étape par étape ({import-page-dediee}), même
// habillage que les imports par lot d'un thésaurus existant (batch.jsx) —
// modèle, règles, dépôt, validation avec erreurs, correction, résultat.
const IMPORT_ERRORS = [
  { line: 12,  id: "MAT-0142",  col: "broader",    msg: "Terme générique introuvable (« Alliage cuivreu »)" },
  { line: 47,  id: "— (vide)",  col: "identifier", msg: "Identifiant obligatoire manquant" },
  { line: 88,  id: "MAT-0231",  col: "lang",       msg: "Code langue inconnu « fra » — attendu fr, en, de…" },
  { line: 119, id: "MAT-0309",  col: "prefLabel",  msg: "Libellé en doublon avec un concept existant" }
];
function ImportThesaurusForm({ onBack, onCreate }) {
  const [format, setFormat] = React.useState("csv"); // csv | rdf
  const [projectId, setProjectId] = React.useState("");
  const projects = React.useMemo(() =>
    [...(window.PROJECTS || [])].sort((a, b) => a.name.localeCompare(b.name, "fr")), []);
  const [open, setOpen] = React.useState({});
  const toggle = (k) => setOpen(o => ({ ...o, [k]: !o[k] }));
  const [file, setFile] = React.useState(null);
  const [checked, setChecked] = React.useState(false);
  const [corrected, setCorrected] = React.useState(false);

  const chooseFormat = (f) => { setFormat(f); setFile(null); setChecked(false); setCorrected(false); };
  const pick = () => {
    setFile(format === "csv" ? { name: "thesaurus-import.csv", n: 142, size: "22 Ko" } : { name: "thesaurus-import.rdf", n: 142, size: "58 Ko" });
    setChecked(false); setCorrected(false);
  };
  const clear = () => { setFile(null); setChecked(false); setCorrected(false); };
  const reimport = () => {
    setFile(f => f && ({ ...f, name: f.name.replace(/(\.\w+)$/, "-corrige$1") }));
    setChecked(false); setCorrected(true);
  };

  const errs = corrected ? [] : IMPORT_ERRORS;
  const total = 142, valid = total - errs.length;

  return (
    <div className="tp tp--create">
      <button className="bo-back" onClick={onBack} title="Retour à la sélection"><Icon name="back" /></button>
      <div className="bo-head">
        <div className="bo-head-ico"><Icon name="upload" /></div>
        <div><h1 className="bo-title">Importer un thésaurus</h1>
          <p className="bo-head-sub">Modèle, dépôt, vérification, correction — rien n'est créé avant l'étape 3.</p></div>
      </div>

      <div className="tp-form">
        <div className="tp-form-card">
          <div className="tp-form-row">
            <label className="tp-form-l">Format du fichier</label>
            <div className="tp-form-radios">
              <label className={"tp-form-radio" + (format === "csv" ? " is-on" : "")}>
                <input type="radio" name="impfmt" checked={format === "csv"} onChange={() => chooseFormat("csv")} />
                <span><Icon name="edit" /><b>CSV</b> — tableur, une ligne par concept</span></label>
              <label className={"tp-form-radio" + (format === "rdf" ? " is-on" : "")}>
                <input type="radio" name="impfmt" checked={format === "rdf"} onChange={() => chooseFormat("rdf")} />
                <span><Icon name="braces" /><b>RDF / SKOS</b> — graphe de concepts existant</span></label>
            </div>
          </div>
          <div className="tp-form-row">
            <label className="tp-form-l">Projet de rattachement <span className="tp-req">*</span></label>
            <select className="tp-form-i" value={projectId} onChange={e => setProjectId(e.target.value)}>
              <option value="" disabled>Choisir un projet…</option>
              {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </div>
        </div>

        <div className="bo-p">
          <div className="bo-acc">
            <BoStep n="1" title="Modèle attendu" sub={format === "csv" ? "colonnes & exemple" : "structure attendue"}
              open={open.fmt} onToggle={() => toggle("fmt")}
              action={format === "csv" && (
                <button className="bo-btn ghost sm" type="button" onClick={e => e.stopPropagation()}>
                  <Icon name="download" /> Modèle</button>
              )}>
              {format === "csv" ? (
                <>
                  <table className="bo-fmt">
                    <thead><tr><th>identifier</th><th>prefLabel</th><th>broader<span className="opt"> (opt.)</span></th>
                      <th>definition<span className="opt"> (opt.)</span></th><th>lang</th></tr></thead>
                    <tbody><tr><td>MAT-0142</td><td>Bronze</td><td>MAT-0100</td><td>Alliage de cuivre et d'étain.</td><td>fr</td></tr></tbody>
                  </table>
                  <div className="bo-fmt-note">Une ligne par concept. La 1ʳᵉ ligne (en-têtes) est facultative.</div>
                </>
              ) : (
                <div className="bo-fmt-note">Fichier SKOS (.rdf, .ttl, .n3) : un concept = une ressource skos:Concept,
                  ses relations en skos:broader / skos:narrower / skos:related.</div>
              )}
            </BoStep>

            <BoStep n="2" title="Règles" sub="ce qui bloque, ce qui n'arrive pas"
              open={open.rules} onToggle={() => toggle("rules")}>
              <ul className="bo-rules">
                <li>L'identifiant doit être unique dans le fichier.</li>
                <li>« broader » doit référencer un identifiant déjà présent dans le fichier, ou rester vide pour un terme racine.</li>
                <li className="warn">Rien n'est créé tant que l'import n'est pas confirmé à l'étape 3.</li>
              </ul>
            </BoStep>
          </div>

          <div className="bo-import">
            <div className="bo-import-h">
              <span className="bo-step-n acc">3</span>
              <div><div className="bo-import-t">Déposer le fichier</div>
                <div className="bo-import-s">glissez votre {format === "csv" ? "CSV" : "fichier RDF"} rempli et lancez</div></div>
            </div>
            <div className="bo-card-b">
              {file ? (
                <div className="bo-drop has-file">
                  <div className="bo-p-ico sm"><Icon name="file" /></div>
                  <div className="bo-file"><div className="bo-file-n">{file.name}</div>
                    <div className="bo-file-m">{file.n} concepts · {file.size}</div></div>
                  <button className="bo-file-x" onClick={clear}><Icon name="x" /></button>
                </div>
              ) : (
                <button className="bo-drop" disabled={!projectId} onClick={pick}>
                  <span className="bo-drop-ico"><Icon name="upload" /></span>
                  <span className="bo-drop-t">Glissez le fichier rempli ou <span className="lk">parcourir</span></span>
                  <span className="bo-drop-s">{projectId
                    ? (format === "csv" ? ".csv, .tsv ou .txt" : ".rdf, .ttl ou .n3")
                    : "choisissez d'abord un projet de rattachement"}</span>
                </button>
              )}
              <div className="bo-run">
                <button className="bo-btn primary" disabled={!file || checked} onClick={() => setChecked(true)}>
                  <Icon name="check" /> Valider le fichier{file ? "" : " — déposez un fichier"}</button>
                <span className="bo-hint">{file ? "Le fichier est analysé et prévisualisé avant création." : "Aperçu et validation avant création."}</span>
              </div>

              {file && checked && (
                <div className="bo-valid">
                  <div className="bo-valid-h">
                    <span className="bo-step-n acc sm">✓</span>
                    <div><div className="bo-import-t">Validation du fichier</div>
                      <div className="bo-import-s">{file.name} · aperçu avant création</div></div>
                  </div>
                  <div className="bo-valid-sum">
                    <div className="bo-stat ok"><b>{total}</b><span>concepts lus</span></div>
                    <div className="bo-stat ok"><b>{valid}</b><span>valides</span></div>
                    <div className={"bo-stat " + (errs.length ? "rej" : "ok")}><b>{errs.length}</b><span>en erreur</span></div>
                  </div>
                  {errs.length > 0 ? (<>
                    <div className="bo-valid-msg err">
                      <Icon name="x" /><span>{errs.length} concepts en erreur. Corrigez-les dans votre fichier d'origine puis réimportez-le,
                        ou ne créez le thésaurus qu'avec les {valid} concepts valides.</span>
                    </div>
                    <table className="bo-errtbl">
                      <thead><tr><th>Ligne</th><th>Identifiant</th><th>Colonne</th><th>Problème</th></tr></thead>
                      <tbody>
                        {errs.map(e => (
                          <tr key={e.line}>
                            <td className="bo-err-ln">{e.line}</td>
                            <td className="bo-err-id">{e.id}</td>
                            <td><span className="bo-err-col">{e.col}</span></td>
                            <td className="bo-err-msg">{e.msg}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    <div className="bo-run">
                      <button className="bo-btn primary" onClick={reimport}><Icon name="upload" /> Réimporter le fichier corrigé</button>
                      <button className="bo-btn ghost" onClick={() => onCreate && onCreate("pactols")}>
                        <Icon name="check" /> Créer avec les {valid} concepts valides</button>
                      <button className="bo-btn ghost" onClick={onBack}>Annuler</button>
                    </div>
                  </>) : (<>
                    <div className="bo-valid-msg ok">
                      <Icon name="check" /><span>Aucune erreur détectée — les {total} concepts sont prêts.</span>
                    </div>
                    <div className="bo-run">
                      <button className="bo-btn primary" onClick={() => onCreate && onCreate("pactols")}>
                        <Icon name="check" /> Créer le thésaurus — {total} concepts</button>
                      <button className="bo-btn ghost" onClick={onBack}>Annuler</button>
                    </div>
                  </>)}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function NavRail({ session, uiLang, onUiLang, version = "X.Y.Z", onLogo, onNav }) {
  const [menu, setMenu] = React.useState(null);
  const [copied, setCopied] = React.useState(false);
  // MOB-3 : sur mobile, ce rail (échelle OpenTheso — rare) se replie derrière un
  // hamburger, séparé de la navigation du thésaurus (recherche/vues/arbo, qui
  // reste l'écran par défaut, cf. mPane dans app.jsx). Sans effet sur le bureau.
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const ref = React.useRef(null);
  React.useEffect(() => {
    const onDoc = (e) => { if (ref.current && !ref.current.contains(e.target)) { setMenu(null); setMobileOpen(false); } };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);
  const u = session.user, base = session.apiBase;
  const toggle = (id) => setMenu(m => (m === id ? null : id));
  const copyEndpoint = (txt) => {
    try { navigator.clipboard && navigator.clipboard.writeText(txt); } catch (e) {}
    setCopied(true); setTimeout(() => setCopied(false), 1400);
  };

  const renderBtn = ({ id, icon, flag, label, align, me }) => (
    <div className="rail-item" key={id}>
      <button className={"rail-btn" + (me ? " rail-me" : "") + (menu === id ? " is-on" : "")} title={label}
              aria-label={label} aria-expanded={me ? undefined : menu === id}
              onClick={() => (me ? go("profile") : toggle(id))}>
        {me ? <span className="rail-ava">{u.initials}</span>
            : flag ? <span className="rail-flag">{flag}</span> : <Icon name={icon} />}
        <span className="rail-label">{label}</span>
      </button>
      {!me && menu === id && <div className={"menu-pop menu-pop--" + align}>{renderMenu(id)}</div>}
    </div>
  );

  const go = (section) => { setMenu(null); setMobileOpen(false); onNav && onNav(section); };

  function renderMenu(id) {
    // PRO-1 : l'avatar du rail MÈNE DIRECTEMENT à la page de profil — pas de popover
    // intermédiaire. L'administration d'instance vit sur l'écran amont (ACC-3).
    if (id === "lang") return (
      <>
        <div className="menu-h">{UI("ifaceLang")}</div>
        <div className="lang-opts">
          {UI_LANGS.map(l => (
            <button key={l} className={"lang-opt" + (uiLang === l ? " is-on" : "")}
                    onClick={() => onUiLang(l)}>
              <span className="lang-opt-flag">{FLAG[l]}</span>{ENDONYM[l]}
            </button>
          ))}
        </div>
      </>
    );
    if (id === "api" || id === "graph") {
      const ep = id === "api" ? base + "/openapi/v1" : base + "/graphql";
      return (
        <>
          <div className="menu-h">{UI(id === "api" ? "apiRest" : "apiGraph")}</div>
          <p className="menu-desc">{UI(id === "api" ? "apiRestDesc" : "apiGraphDesc")}</p>
          <div className="api-endpoint">
            <code>{ep}</code>
            <button className="api-copy" title={copied ? UI("copied") : UI("copy")}
                    onClick={() => copyEndpoint(ep)}><Icon name={copied ? "check" : "copy"} /></button>
          </div>
          <button className="menu-foot"><Icon name="external" />
            {UI(id === "api" ? "apiDocs" : "openExplorer")}</button>
        </>
      );
    }
    return null;
  }

  return (
    <nav className={"rail" + (mobileOpen ? " is-mobile-open" : "")} ref={ref}>
      <div className="rail-top">
        <button className="rail-logo" title="Sélection de thésaurus" aria-label="Sélection de thésaurus" onClick={onLogo}><LogoMark /></button>
        <button className="rail-hamburger" title="Menu" aria-label="Menu" aria-expanded={mobileOpen}
                onClick={() => setMobileOpen(o => !o)}><Icon name={mobileOpen ? "x" : "menu"} /></button>
      </div>
      <div className="rail-body">
        <div className="rail-group">
          {renderBtn({ id: "me", label: u.name + " — " + UI("myProfile"), align: "top", me: true })}
          {session.superAdmin && (
            <div className="rail-item">
              <button className="rail-btn" title={"Administration de l'instance"} aria-label="Administration de l'instance"
                      onClick={() => go("admin")}><Icon name="shield" /><span className="rail-label">Administration de l'instance</span></button>
            </div>
          )}
        </div>
        <div className="rail-spacer" />
        <div className="rail-group">
          {renderBtn({ id: "lang", flag: FLAG[uiLang], label: UI("ifaceLang"), align: "bottom" })}
          {renderBtn({ id: "api", icon: "braces", label: UI("apiRest"), align: "bottom" })}
          {renderBtn({ id: "graph", icon: "graph", label: UI("apiGraph"), align: "bottom" })}
        </div>
        <button className="rail-btn rail-out" title={UI("logout")} aria-label={UI("logout")}><Icon name="logout" /><span className="rail-label">{UI("logout")}</span></button>
        <div className="rail-ver" title={"OpenTheso " + version}>{version}</div>
      </div>
    </nav>
  );
}

Object.assign(window, {
  searchConcepts, Autocomplete, ResultsView, ResultsList, ConceptView, ThesaurusHome,
  displayLang,
  RelationEditor, checkRelation, REL_KINDS, REL_LABEL, TranslationEditor, LabelField, checkLabel,
  ThesaurusPicker, InstanceAdminZone, SubPageList, Modal, Breadcrumb, ROLE_APP, ROLE_TH, ROLE_NAME, CAPS, makePerm, RoleSelect,
  Highlight, Path, norm, buildTree, Tree, FlatList, TableOptions, computeFlatRows, flatToCsv, candMatch,
  StatusChecklist, StatusPill, STATUS_META, STATUS_ORDER, allStatuses, statusOf, FLAT_DEFAULT_CFG,
  CreateChooser, CREATE_KINDS,
  CandidateBanner, VotesBlock, DiscussionBlock, ValidationBlock, HistoryBlock, ViewOptions, DraftForm,
  LogoMark, NavRail, InstitutionalFooter,
  LBL, UI, setDisplayLang, setUiLang, termLangs, UI_LANGS, LANGNAME, ENDONYM, FLAG, ALIGN_SOURCES,
  toCsv, downloadCsv, slugify,
  ClassBadges, ClassPicker, clsEnabled, setClsEnabled, clsList, setClsList, clsMeta, clsOf,
  setDraftDirty, isDraftDirty,
  NotesRead, NotesEditor, noteTypes, setNoteTypes, noteType,
  HistoryLog, HistoryView, HIST_ELEMENTS, HIST_COLS, histEl,
  AlignPanel, AlignHit, MATCH_TYPES, MATCH_DEFAULT, allMatchTypes, matchTypes, setMatchTypes, matchMeta, alignSources, setAlignSources, alignStale, setAlignStale, ALIGN_DEFAULT, isStale,
  FacetView, facetConcept,
  REL_HIER, hierMeta, HierSelect, initHTypes
});
