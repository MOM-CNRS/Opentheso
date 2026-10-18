// ──────────────────────────────────────────────────────────────────────────
// Données de démo — thésaurus PACTOLS (archéologie / patrimoine), réseau de
// relations sémantiques RICHE (TG / TS / TA). Recherche TOUJOURS dans ce thésaurus.
// ──────────────────────────────────────────────────────────────────────────
(function () {
  const C = [
    // ───────── Matière / métal ─────────
    {
      id: "ark:/12148/c0m0t1", pref: "Métal", type: "concept", status: "valide",
      notation: "MAT-0100", alts: [], path: ["Matière"],
      def: "Matériau caractérisé par son éclat, sa conductivité et sa malléabilité, extrait de minerais.",
      bt: ["Matière"],
      nt: ["Cuivre", "Étain", "Plomb", "Alliage à base de cuivre", "Fer", "Or", "Argent"],
      rt: ["Métallurgie", "Minerai", "Alliage"]
    },
    {
      id: "ark:/12148/c1a0c1", pref: "Alliage à base de cuivre", type: "concept", status: "valide",
      notation: "MAT-0120", alts: ["Alliage cuivreux"], path: ["Matière", "Métal"],
      def: "Famille d'alliages dont le composant majoritaire est le cuivre.",
      bt: ["Métal"],
      nt: ["Bronze", "Laiton", "Billon", "Potin"],
      rt: ["Cuivre", "Étain", "Zinc", "Métallurgie"]
    },
    {
      id: "ark:/12148/c8h4n2", pref: "Bronze", type: "concept", status: "valide",
      notation: "MAT-0142", alts: ["Airain"], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Alliage de cuivre et d'étain, parfois additionné de plomb, employé pour le mobilier, l'armement et la statuaire dès la Protohistoire.",
      bt: ["Alliage à base de cuivre"],
      nt: ["Bronze doré", "Bronze à la cire perdue", "Bronze au plomb", "Bronze argentifère"],
      rt: ["Cuivre", "Étain", "Plomb", "Métallurgie", "Patine", "Laiton", "Orichalque",
           "Statuette en bronze", "Vase en bronze", "Miroir en bronze", "Fibule",
           "Monnaie de bronze", "Hache à douille", "Âge du bronze", "Dépôt de bronzes"]
    },
    {
      id: "ark:/12148/c8w4q1", pref: "Bronze doré", type: "concept", status: "candidat",
      notation: "MAT-0148", alts: ["Vermeil de bronze"], path: ["Matière", "Métal", "Alliage à base de cuivre", "Bronze"],
      def: "Bronze recouvert d'une dorure à la feuille ou à l'amalgame.",
      bt: ["Bronze"], nt: [], rt: ["Dorure", "Mercure", "Statuette en bronze", "Bronze"]
    },
    {
      id: "ark:/12148/c8w4q2", pref: "Bronze au plomb", type: "concept", status: "valide",
      notation: "MAT-0149", alts: ["Bronze plombifère"], path: ["Matière", "Métal", "Alliage à base de cuivre", "Bronze"],
      def: "Bronze additionné de plomb pour faciliter la coulée.",
      bt: ["Bronze"], nt: [], rt: ["Plomb", "Bronze", "Fonderie"]
    },
    {
      id: "ark:/12148/c8w4q3", pref: "Bronze argentifère", type: "concept", status: "valide",
      notation: "MAT-0150", alts: [], path: ["Matière", "Métal", "Alliage à base de cuivre", "Bronze"],
      def: "Bronze contenant une proportion notable d'argent.",
      bt: ["Bronze"], nt: [], rt: ["Argent", "Bronze", "Monnaie de bronze"]
    },
    {
      id: "ark:/12148/c1l2a3", pref: "Laiton", type: "concept", status: "valide",
      notation: "MAT-0144", alts: ["Cuivre jaune"], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Alliage de cuivre et de zinc, souvent confondu avec le bronze dans les sources anciennes.",
      bt: ["Alliage à base de cuivre"], nt: [], rt: ["Cuivre", "Zinc", "Bronze", "Orichalque"]
    },
    {
      id: "ark:/12148/c1b3i5", pref: "Billon", type: "concept", status: "valide",
      notation: "MAT-0146", alts: [], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Alliage pauvre en argent mêlé de cuivre, utilisé en numismatique.",
      bt: ["Alliage à base de cuivre"], nt: [], rt: ["Argent", "Cuivre", "Monnaie de bronze"]
    },
    {
      id: "ark:/12148/c1p7t9", pref: "Potin", type: "concept", status: "valide",
      notation: "MAT-0147", alts: [], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Alliage cuivreux très chargé en étain et plomb, coulé pour les monnaies gauloises.",
      bt: ["Alliage à base de cuivre"], nt: [], rt: ["Étain", "Plomb", "Monnaie de bronze"]
    },
    {
      id: "ark:/12148/c3d8n1", pref: "Cuivre", type: "concept", status: "valide",
      notation: "MAT-0130", alts: [], path: ["Matière", "Métal"],
      def: "Métal natif rouge, composant principal du bronze et du laiton.",
      bt: ["Métal"], nt: [], rt: ["Bronze", "Laiton", "Étain", "Minerai", "Métallurgie", "Alliage à base de cuivre"],
      matchAlt: "principal du bronze"
    },
    {
      id: "ark:/12148/c4r6v7", pref: "Étain", type: "concept", status: "valide",
      notation: "MAT-0131", alts: ["Cassitérite (minerai)"], path: ["Matière", "Métal"],
      def: "Métal blanc dont l'addition au cuivre produit le bronze.",
      bt: ["Métal"], nt: [], rt: ["Bronze", "Cuivre", "Plomb", "Métallurgie"], matchAlt: "produit le bronze"
    },
    {
      id: "ark:/12148/c4r6v8", pref: "Plomb", type: "concept", status: "valide",
      notation: "MAT-0132", alts: [], path: ["Matière", "Métal"],
      def: "Métal lourd et fusible, ajouté au bronze pour en abaisser le point de fusion.",
      bt: ["Métal"], nt: [], rt: ["Bronze", "Bronze au plomb", "Étain", "Galène"], matchAlt: "ajouté au bronze"
    },
    {
      id: "ark:/12148/c4r6v9", pref: "Argent", type: "concept", status: "valide",
      notation: "MAT-0133", alts: [], path: ["Matière", "Métal"],
      def: "Métal blanc précieux, parfois allié au bronze.",
      bt: ["Métal"], nt: [], rt: ["Bronze argentifère", "Billon", "Monnaie de bronze"]
    },
    {
      id: "ark:/12148/c4r6w0", pref: "Fer", type: "concept", status: "valide",
      notation: "MAT-0134", alts: [], path: ["Matière", "Métal"],
      def: "Métal qui supplante le bronze pour l'armement et l'outillage à l'âge du fer.",
      bt: ["Métal"], nt: [], rt: ["Âge du fer", "Forge", "Fibule"], matchAlt: "supplante le bronze"
    },
    {
      id: "ark:/12148/c4r6w1", pref: "Or", type: "concept", status: "valide",
      notation: "MAT-0135", alts: [], path: ["Matière", "Métal"],
      def: "Métal précieux employé pour la dorure du bronze.",
      bt: ["Métal"], nt: [], rt: ["Bronze doré", "Dorure"], matchAlt: "dorure du bronze"
    },
    {
      id: "ark:/12148/c1o4r7", pref: "Orichalque", type: "concept", status: "candidat",
      notation: "MAT-0145", alts: ["Aurichalque"], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Alliage cuivreux doré des sources antiques, proche du laiton.",
      bt: ["Alliage à base de cuivre"], nt: [], rt: ["Laiton", "Bronze", "Monnaie de bronze"]
    },

    // ───────── Période ─────────
    {
      id: "ark:/12148/c2k9p1", pref: "Âge du bronze", type: "concept", status: "valide",
      notation: "PER-0031", alts: ["Bronze (période)"], path: ["Période", "Protohistoire"],
      def: "Période protohistorique caractérisée par la généralisation de la métallurgie du bronze, env. 2200–800 av. J.-C. en Europe occidentale.",
      bt: ["Protohistoire"],
      nt: ["Bronze ancien", "Bronze moyen", "Bronze final"],
      rt: ["Âge du fer", "Néolithique", "Métallurgie", "Bronze", "Dépôt de bronzes"]
    },
    {
      id: "ark:/12148/c5d3w8", pref: "Bronze ancien", type: "concept", status: "valide",
      notation: "PER-0032", alts: [], path: ["Période", "Protohistoire", "Âge du bronze"],
      def: "Première phase de l'âge du bronze (env. 2200–1600 av. J.-C.).",
      bt: ["Âge du bronze"], nt: [], rt: ["Bronze moyen", "Néolithique"]
    },
    {
      id: "ark:/12148/c5d3w9", pref: "Bronze moyen", type: "concept", status: "valide",
      notation: "PER-0033", alts: [], path: ["Période", "Protohistoire", "Âge du bronze"],
      def: "Phase médiane de l'âge du bronze (env. 1600–1400 av. J.-C.).",
      bt: ["Âge du bronze"], nt: [], rt: ["Bronze ancien", "Bronze final"]
    },
    {
      id: "ark:/12148/c5d4a0", pref: "Bronze final", type: "concept", status: "valide",
      notation: "PER-0034", alts: ["Bronze récent"], path: ["Période", "Protohistoire", "Âge du bronze"],
      def: "Dernière phase de l'âge du bronze (env. 1400–800 av. J.-C.), marquée par les dépôts et l'essor des champs d'urnes.",
      bt: ["Âge du bronze"], nt: [], rt: ["Âge du fer", "Dépôt de bronzes", "Hache à douille"]
    },
    {
      id: "ark:/12148/c2a1f3", pref: "Âge du fer", type: "concept", status: "valide",
      notation: "PER-0040", alts: [], path: ["Période", "Protohistoire"],
      def: "Période protohistorique succédant à l'âge du bronze, marquée par la diffusion du fer.",
      bt: ["Protohistoire"], nt: ["Fibule de La Tène"], rt: ["Âge du bronze", "Fer", "Bronze final"]
    },

    // ───────── Objets ─────────
    {
      id: "ark:/12148/c9f2m4", pref: "Statuette en bronze", type: "concept", status: "valide",
      notation: "OBJ-1188", alts: ["Bronze figuré", "Petit bronze"], path: ["Objet", "Mobilier", "Statuaire"],
      def: "Figurine de petite dimension coulée en alliage cuivreux, votive, domestique ou décorative.",
      bt: ["Statuaire"], nt: [], rt: ["Bronze", "Bronze à la cire perdue", "Bronze doré", "Ex-voto", "Patine"]
    },
    {
      id: "ark:/12148/c1a7v3", pref: "Vase en bronze", type: "concept", status: "valide",
      notation: "OBJ-0455", alts: ["Vaisselle de bronze"], path: ["Objet", "Mobilier", "Vaisselle"],
      def: "Récipient en alliage cuivreux destiné au service ou au stockage des liquides.",
      bt: ["Vaisselle"], nt: [], rt: ["Bronze", "Situle", "Œnochoé", "Patine"]
    },
    {
      id: "ark:/12148/c4t6b2", pref: "Miroir en bronze", type: "concept", status: "valide",
      notation: "OBJ-0712", alts: [], path: ["Objet", "Mobilier", "Objet de toilette"],
      def: "Disque de bronze poli sur une face, employé comme miroir dès l'Antiquité.",
      bt: ["Objet de toilette"], nt: [], rt: ["Bronze", "Étrurie", "Patine"]
    },
    {
      id: "ark:/12148/c7n8k5", pref: "Fibule", type: "concept", status: "valide",
      notation: "OBJ-0203", alts: ["Agrafe à vêtement"], path: ["Objet", "Parure", "Élément vestimentaire"],
      def: "Épingle articulée servant à fixer un vêtement, souvent en bronze, parfois en fer ou en argent.",
      bt: ["Élément vestimentaire"], nt: ["Fibule de La Tène"], rt: ["Bronze", "Épingle", "Parure", "Fer"],
      matchAlt: "souvent en bronze"
    },
    {
      id: "ark:/12148/c7n8k6", pref: "Fibule de La Tène", type: "concept", status: "valide",
      notation: "OBJ-0205", alts: [], path: ["Objet", "Parure", "Élément vestimentaire", "Fibule"],
      def: "Type de fibule caractéristique du second âge du fer.",
      bt: ["Fibule"], nt: [], rt: ["Âge du fer", "Bronze"]
    },
    {
      id: "ark:/12148/c3p5r9", pref: "Monnaie de bronze", type: "concept", status: "valide",
      notation: "OBJ-0890", alts: ["Bronze monétaire", "Petit bronze (numismatique)"], path: ["Objet", "Numismatique", "Monnaie"],
      def: "Pièce de monnaie frappée ou coulée en alliage cuivreux.",
      bt: ["Monnaie"], nt: [], rt: ["As", "Sesterce", "Bronze", "Billon", "Potin", "Orichalque"]
    },
    {
      id: "ark:/12148/c6m1d7", pref: "Hache à douille", type: "concept", status: "valide",
      notation: "OBJ-0331", alts: [], path: ["Objet", "Outil", "Hache"],
      def: "Hache de bronze à douille de fixation, caractéristique du Bronze final atlantique.",
      bt: ["Hache"], nt: [], rt: ["Bronze final", "Dépôt de bronzes", "Bronze"], matchAlt: "en bronze à douille"
    },
    {
      id: "ark:/12148/c7h1m9", pref: "Situle", type: "concept", status: "valide",
      notation: "OBJ-0461", alts: [], path: ["Objet", "Mobilier", "Vaisselle"],
      def: "Seau métallique, souvent en bronze, à usage rituel ou domestique.",
      bt: ["Vaisselle"], nt: [], rt: ["Vase en bronze", "Bronze"], matchAlt: "souvent en bronze"
    },
    {
      id: "ark:/12148/c2v5p8", pref: "Épingle", type: "concept", status: "valide",
      notation: "OBJ-0210", alts: [], path: ["Objet", "Parure", "Élément vestimentaire"],
      def: "Tige métallique pointue, fréquemment en bronze, servant d'attache ou d'ornement.",
      bt: ["Élément vestimentaire"], nt: [], rt: ["Fibule", "Bronze"], matchAlt: "fréquemment en bronze"
    },

    // ───────── Structures / dépôts ─────────
    {
      id: "ark:/12148/c1k4z2", pref: "Dépôt de bronzes", type: "collection", status: "valide",
      notation: "COL-0044", alts: ["Cachette de fondeur"], path: ["Structure", "Dépôt"],
      def: "Ensemble d'objets en bronze volontairement enfouis, à caractère votif, funéraire ou utilitaire.",
      bt: ["Dépôt"], nt: [], rt: ["Bronze final", "Hache à douille", "Bronze", "Âge du bronze"]
    },

    // ───────── Techniques ─────────
    {
      id: "ark:/12148/c9b2x4", pref: "Métallurgie", type: "concept", status: "valide",
      notation: "TEC-0061", alts: ["Travail des métaux"], path: ["Technique", "Production des matériaux"],
      def: "Ensemble des techniques d'extraction et de transformation des métaux, dont le bronze.",
      bt: ["Production des matériaux"], nt: ["Fonderie", "Forge"],
      rt: ["Bronze", "Cuivre", "Étain", "Minerai", "Bronze à la cire perdue"], matchAlt: "alliages comme le bronze"
    },
    {
      id: "ark:/12148/c9b2x5", pref: "Fonderie", type: "concept", status: "valide",
      notation: "TEC-0062", alts: [], path: ["Technique", "Production des matériaux", "Métallurgie"],
      def: "Branche de la métallurgie consacrée à la fonte et au moulage des métaux.",
      bt: ["Métallurgie"], nt: ["Bronze à la cire perdue"], rt: ["Bronze", "Bronze au plomb", "Moule"]
    },
    {
      id: "ark:/12148/c9b2x6", pref: "Forge", type: "concept", status: "valide",
      notation: "TEC-0063", alts: [], path: ["Technique", "Production des matériaux", "Métallurgie"],
      def: "Travail des métaux par déformation à chaud.",
      bt: ["Métallurgie"], nt: [], rt: ["Fer", "Métallurgie"]
    },
    {
      id: "ark:/12148/c2y9h6", pref: "Bronze à la cire perdue", type: "concept", status: "valide",
      notation: "TEC-0077", alts: ["Fonte à la cire perdue"], path: ["Technique", "Production des matériaux", "Métallurgie", "Fonderie"],
      def: "Procédé de fonte d'un objet en bronze à partir d'un modèle en cire détruit lors de la coulée.",
      bt: ["Fonderie"], nt: [], rt: ["Bronze", "Statuette en bronze", "Cire", "Moule"]
    },

    // ───────── État de conservation ─────────
    {
      id: "ark:/12148/c5g3t8", pref: "Patine", type: "concept", status: "valide",
      notation: "ALT-0019", alts: ["Vert-de-gris"], path: ["État de conservation", "Altération"],
      def: "Couche d'altération de surface des métaux cuivreux, notamment du bronze, allant du brun au vert-de-gris.",
      bt: ["Altération"], nt: [], rt: ["Bronze", "Corrosion", "Statuette en bronze"], matchAlt: "des bronzes anciens"
    }
  ];

  // ── Enrichissement : traductions, collection/facette, identifiants, dates ──
  const TRAD = {
    "Bronze": { de: "Bronze", en: "Bronze", es: "Bronce", it: "Bronzo", nl: "Brons" },
    "Âge du bronze": { de: "Bronzezeit", en: "Bronze Age", es: "Edad del Bronce", it: "Età del bronzo" },
    "Âge du fer": { de: "Eisenzeit", en: "Iron Age", es: "Edad del Hierro", it: "Età del ferro" },
    "Cuivre": { de: "Kupfer", en: "Copper", es: "Cobre", it: "Rame" },
    "Étain": { de: "Zinn", en: "Tin", es: "Estaño", it: "Stagno" },
    "Plomb": { de: "Blei", en: "Lead", es: "Plomo", it: "Piombo" },
    "Argent": { de: "Silber", en: "Silver", es: "Plata", it: "Argento" },
    "Or": { de: "Gold", en: "Gold", es: "Oro", it: "Oro" },
    "Fer": { de: "Eisen", en: "Iron", es: "Hierro", it: "Ferro" },
    "Laiton": { de: "Messing", en: "Brass", es: "Latón", it: "Ottone" },
    "Métal": { de: "Metall", en: "Metal", es: "Metal", it: "Metallo" },
    "Statuette en bronze": { en: "Bronze statuette", de: "Bronzestatuette", it: "Statuetta in bronzo" },
    "Fibule": { en: "Fibula", de: "Fibel", es: "Fíbula", it: "Fibula" },
    "Métallurgie": { en: "Metallurgy", de: "Metallurgie", es: "Metalurgia", it: "Metallurgia" }
  };
  const FACETTE = {
    "Matière": "matériaux", "Période": "chronologie", "Objet": "objets archéologiques",
    "Technique": "techniques", "Structure": "structures", "État de conservation": "états"
  };
  // Alignements externes (par concept), corpus liés, coordonnées GPS — démo.
  const ALIGN = {
    "Bronze": [
      { type: "exactMatch", source: "Wikidata", uri: "https://www.wikidata.org/wiki/Q34095" },
      { type: "closeMatch", source: "Getty AAT", uri: "http://vocab.getty.edu/aat/300010957" }
    ],
    "Laiton": [{ type: "exactMatch", source: "Wikidata", uri: "https://www.wikidata.org/wiki/Q39782" }],
    "Fibule": [{ type: "closeMatch", source: "Getty AAT", uri: "http://vocab.getty.edu/aat/300043760" }],
    "Tjarou": [
      { type: "exactMatch", source: "Wikidata", uri: "https://www.wikidata.org/wiki/Q826420" },
      { type: "exactMatch", source: "Wikimedia maps", uri: "https://geohack.toolforge.org/geohack.php?params=30.857_N_32.350_E" }
    ]
  };
  const CORPUS_LINKS = { "Bronze": 128, "Fibule": 64, "Statuette en bronze": 22, "Laiton": 9 };
  const GPS = { "Tjarou": "30.857222 32.350556" };

  // ── Variantes cachées : libellés de recherche non affichés (fautes, pluriels, graphies) ──
  const ALTS_HIDDEN = {
    "Bronze": ["bronzes", "airin", "bronze antique"],
    "Alliage à base de cuivre": ["alliages cuivreux", "alliage cuivreu"],
    "Laiton": ["laitons", "leton", "cuivre-jaune"],
    "Statuette en bronze": ["statuettes en bronze", "statuete en bronze"],
    "Fibule": ["fibules", "fibulae"],
    "Métallurgie": ["métalurgie", "metalurgie"]
  };
  // ── Collections / groupes : un concept peut appartenir à plusieurs collections ──
  const COLLECTIONS = {
    "Bronze": [
      { id: "e3453", nom: "Mobilier métallique", membres: 148 },
      { id: "65d65", nom: "Alliages antiques",   membres: 22 },
      { id: "3453",  nom: "Statuaire",           membres: 61 }
    ],
    "Laiton": [
      { id: "65d65", nom: "Alliages antiques",   membres: 22 }
    ],
    "Statuette en bronze": [
      { id: "3453",  nom: "Statuaire",           membres: 61 },
      { id: "6565",  nom: "Objets votifs",       membres: 37 }
    ]
  };
  // ── Traductions : formes alternatives par langue + statut de complétude ("ok" | "todo") ──
  const TRAD_ALT = {
    "Bronze": { en: ["bronze alloy"], de: ["Bronzelegierung"], it: ["lega di bronzo"] },
    "Laiton": { en: ["yellow copper"], de: ["Gelbkupfer"] },
    "Âge du bronze": { de: ["Bronzezeitalter"] }
  };
  const TRAD_STATUS = {
    "Bronze": { es: "todo" },
    "Laiton": { it: "todo", nl: "todo" }
  };
  // ── Notes multilingues : note d'application + définition (autres langues que le fr) ──
  const NOTE = {
    "Bronze": {
      fr: "Employer ce concept pour l'alliage cuivre-étain. Pour un objet, préférer un concept spécifique (statuette, vase…).",
      en: "Use this concept for the copper-tin alloy. For an object, prefer a specific concept (statuette, vessel…)."
    },
    "Laiton": {
      fr: "Ne pas confondre avec le bronze : le laiton contient du zinc, non de l'étain."
    }
  };
  const DEF_TR = {
    "Bronze": {
      en: "Alloy of copper and tin, sometimes with added lead, used for furnishings, weaponry and statuary from Protohistory onwards.",
      de: "Legierung aus Kupfer und Zinn, gelegentlich mit Blei, seit der Vorgeschichte für Gerät, Waffen und Statuen verwendet."
    }
  };

  // ── Candidats orphelins (sans terme générique) → 1er niveau de l'arbre ──
  C.push(
    { id: "ark:/12148/ctj0u1", pref: "Tjarou", type: "concept", status: "candidat",
      notation: "—", alts: ["Tjaru", "Silé"], path: [],
      def: "Forteresse égyptienne située sur l'actuel Tell Heboua, à la frontière orientale de l'Égypte ; appelée Σελη (Selê) par les Grecs.",
      bt: [], nt: [], rt: ["Basse Égypte"] },
    { id: "ark:/12148/csb0b2", pref: "Sigillée claire B", type: "concept", status: "candidat",
      notation: "—", alts: [], path: [],
      def: "Production céramique tardive (Hayes), à distinguer de la sigillée claire A.",
      bt: [], nt: [], rt: [] }
  );

  // ── Concepts d'autres statuts (inséré / rejeté / déprécié) pour illustrer la
  //    gestion d'affichage par statut. « inséré » = candidat accepté (vie du
  //    thésaurus), distinct des « normaux » configurés à la création. ──
  C.push(
    { id: "ark:/12148/cins01", pref: "Fibule ansée", type: "concept", status: "insere",
      notation: "OBJ-0206", alts: ["Fibule à arc"], path: ["Objet", "Parure", "Élément vestimentaire", "Fibule"],
      def: "Fibule dont l'arc dessine une anse, insérée récemment après proposition et vote.",
      bt: ["Fibule"], nt: [], rt: ["Bronze"],
      hist: { by: "m.lefevre", on: "2026-04-02", valBy: "c.roussel", valOn: "2026-04-09" } },
    { id: "ark:/12148/cins02", pref: "Vase campaniforme", type: "concept", status: "insere",
      notation: "OBJ-0456", alts: [], path: ["Objet", "Mobilier", "Vaisselle"],
      def: "Gobelet en forme de cloche renversée, inséré au thésaurus après validation.",
      bt: ["Vaisselle"], nt: [], rt: [],
      hist: { by: "c.roussel", on: "2026-05-30", valBy: "blandine.nouvel", valOn: "2026-06-04" } },
    { id: "ark:/12148/crej01", pref: "Bronze blanc", type: "concept", status: "rejete",
      notation: "—", alts: [], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Terme ambigu proposé puis rejeté : recouvre plusieurs alliages déjà décrits.",
      bt: ["Alliage à base de cuivre"], nt: [], rt: [],
      hist: { by: "j.bonnet", on: "2026-03-11", valBy: "c.roussel", valOn: "2026-03-15" } },
    { id: "ark:/12148/crej02", pref: "Applique zoomorphe", type: "concept", status: "rejete",
      notation: "—", alts: [], path: ["Objet", "Parure"],
      def: "Doublon d'un concept existant ; proposition rejetée.",
      bt: ["Parure"], nt: [], rt: [],
      hist: { by: "a.costa", on: "2026-02-20", valBy: "c.roussel", valOn: "2026-02-24" } },
    { id: "ark:/12148/cdep01", pref: "Cuivre jaune (ancien terme)", type: "concept", status: "deprecie",
      notation: "MAT-0144b", alts: [], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Ancien libellé du laiton, déprécié au profit de « Laiton ».",
      bt: ["Alliage à base de cuivre"], nt: [], rt: ["Laiton"],
      hist: { by: "a.costa", on: "2012-01-10", valBy: "c.roussel", valOn: "2021-09-01" } },
    { id: "ark:/12148/cdep02", pref: "Bronze antique", type: "concept", status: "deprecie",
      notation: "MAT-0142b", alts: [], path: ["Matière", "Métal", "Alliage à base de cuivre", "Bronze"],
      def: "Terme jugé trop général, déprécié au profit de « Bronze ».",
      bt: ["Bronze"], nt: [], rt: ["Bronze"],
      hist: { by: "m.lefevre", on: "2010-05-05", valBy: "c.roussel", valOn: "2022-06-14" } },
    { id: "ark:/12148/ccan09", pref: "Alliage argent-cuivre", type: "concept", status: "candidat",
      notation: "—", alts: ["Argent cuivreux"], path: ["Matière", "Métal", "Alliage à base de cuivre"],
      def: "Alliage binaire argent-cuivre, proposé comme spécifique de l'alliage cuivreux.",
      bt: ["Alliage à base de cuivre"], nt: [], rt: ["Argent", "Cuivre"] }
  );

  // ── Génération procédurale : grossit le jeu de démo (~10×) ──
  (function () {
    let gi = 0;
    const mk = (pref, path, status) => {
      gi++;
      C.push({
        id: "ark:/12148/g" + String(gi).padStart(4, "0"), pref, type: "concept",
        status: status || "valide", notation: "GEN-" + (1000 + gi), alts: [],
        path, bt: path.length ? [path[path.length - 1]] : [], nt: [], rt: [],
        def: "Concept de démonstration « " + pref + " » (branche " + (path[0] || "—") + ")."
      });
    };
    const ADJ = ["à décor géométrique", "de tradition indigène", "d'importation", "de production locale",
      "à vernis rouge", "miniature", "monumental", "fragmentaire", "de type provincial", "tardif"];
    const FAM = {
      "Matière": {
        "Céramique": ["Terre cuite", "Sigillée", "Sigillée claire A", "Céramique commune", "Céramique fine", "Amphore", "Dolium", "Lampe à huile", "Vernis noir", "Campanienne"],
        "Pierre": ["Calcaire", "Marbre", "Granite", "Basalte", "Silex", "Grès", "Obsidienne", "Tuf", "Albâtre"],
        "Verre": ["Verre soufflé", "Verre moulé", "Pâte de verre", "Millefiori"],
        "Matière organique": ["Os", "Ivoire", "Bois", "Cuir", "Textile", "Ambre", "Corne"]
      },
      "Objet": {
        "Armement": ["Épée", "Poignard", "Lance", "Javelot", "Bouclier", "Casque", "Cuirasse", "Pointe de flèche", "Umbo"],
        "Parure": ["Bague", "Bracelet", "Collier", "Pendentif", "Boucle d'oreille", "Torque", "Perle", "Applique"],
        "Vaisselle": ["Coupe", "Cruche", "Assiette", "Plat", "Gobelet", "Patère", "Œnochoé", "Cratère"],
        "Outil": ["Couteau", "Hachette", "Ciseau", "Aiguille", "Alêne", "Faucille", "Meule", "Enclume"],
        "Monnaie": ["As", "Sesterce", "Denier", "Aureus", "Drachme", "Statère", "Antoninien"]
      },
      "Période": {
        "Préhistoire": ["Paléolithique", "Mésolithique", "Néolithique", "Paléolithique supérieur", "Magdalénien", "Gravettien"],
        "Antiquité": ["Époque romaine", "Haut-Empire", "Bas-Empire", "Antiquité tardive", "Époque hellénistique", "Époque classique"],
        "Moyen Âge": ["Haut Moyen Âge", "Époque carolingienne", "Période mérovingienne", "Moyen Âge central"]
      },
      "Technique": {
        "Décor": ["Estampage", "Incision", "Champlevé", "Émail", "Dorure", "Gravure", "Repoussé", "Filigrane"],
        "Construction": ["Opus reticulatum", "Opus incertum", "Opus spicatum", "Mortier de chaux", "Pisé", "Adobe"]
      },
      "Structure": {
        "Habitat": ["Domus", "Villa", "Insula", "Cabane", "Ferme"],
        "Funéraire": ["Tombe", "Tumulus", "Nécropole", "Sarcophage", "Urne", "Mausolée", "Hypogée"],
        "Édifice public": ["Thermes", "Forum", "Amphithéâtre", "Théâtre", "Basilique", "Aqueduc", "Temple"]
      },
      "État de conservation": {
        "Altération": ["Corrosion", "Concrétion", "Fragmentation", "Lacune", "Déformation", "Oxydation"]
      }
    };
    Object.entries(FAM).forEach(([top, subs]) => {
      Object.entries(subs).forEach(([sub, leaves]) => {
        mk(sub, [top]);
        leaves.forEach((leaf, li) => {
          mk(leaf, [top, sub], gi % 13 === 0 ? "candidat" : "valide");
          for (let k = 0; k < 2; k++) {
            mk(leaf + " " + ADJ[(li + k) % ADJ.length], [top, sub, leaf], gi % 17 === 0 ? "candidat" : "valide");
          }
        });
      });
    });
  })();

  // ── Données de gouvernance des candidats (proposant, votes, discussion) ──
  const CAND = {
    "Bronze doré": {
      by: "a.costa", on: "2026-05-21", votes: { up: 3, down: 1 }, mine: null,
      participants: ["a.costa", "c.roussel", "m.lefevre"],
      discussion: [
        { u: "a.costa", d: "2026-05-21 09:12", t: "Terme fréquent dans les inventaires de mobilier métallique ; il mérite un concept distinct du bronze." },
        { u: "c.roussel", d: "2026-05-22 14:03", t: "D'accord pour un concept distinct, à rattacher sous « Bronze ». Penser à l'alignement Wikidata." }
      ]
    },
    "Orichalque": {
      by: "m.lefevre", on: "2026-05-18", votes: { up: 2, down: 0 }, mine: "up",
      participants: ["m.lefevre", "c.roussel"],
      discussion: [
        { u: "m.lefevre", d: "2026-05-18 16:40", t: "Attesté dans les sources antiques (laiton doré). Proposé comme spécifique de l'alliage cuivreux." }
      ]
    },
    "Tjarou": {
      by: "anais.mauriceau", on: "2026-09-29", votes: { up: 1, down: 0 }, mine: null,
      participants: ["anais.mauriceau", "c.roussel"],
      discussion: [
        { u: "anais.mauriceau", d: "2026-09-29 12:00", t: "Pour les Grecs le site s'appelle Σελη (Selê) et pour les Romains Silé." },
        { u: "anais.mauriceau", d: "2026-09-29 12:04", t: "Autres formes : Tjaru, Zaru, Tharo (de) ; Sile, Taru (es)." }
      ]
    },
    "Sigillée claire B": {
      by: "j.bonnet", on: "2026-05-12", votes: { up: 0, down: 0 }, mine: null,
      participants: ["j.bonnet"],
      discussion: []
    },
    "Alliage argent-cuivre": {
      by: "c.roussel", on: "2026-06-30", votes: { up: 1, down: 0 }, mine: "up",
      participants: ["c.roussel"],
      discussion: [
        { u: "c.roussel", d: "2026-06-30 10:15", t: "Attesté en numismatique ; à rattacher sous « Alliage à base de cuivre »." }
      ]
    }
  };

  // ── Historique de candidature des concepts validés (tout concept fut candidat) ──
  // Conserve votes + discussion + qui/quand a validé, même après insertion.
  const HIST = {
    "Bronze": {
      by: "a.costa", on: "2018-03-11", valBy: "c.roussel", valOn: "2018-03-18",
      votes: { up: 5, down: 0 }, participants: ["a.costa", "c.roussel", "j.bonnet"],
      discussion: [
        { u: "a.costa", d: "2018-03-11 10:22", t: "Concept fondamental pour le mobilier métallique, à créer sous « Alliage à base de cuivre »." },
        { u: "j.bonnet", d: "2018-03-14 09:05", t: "Penser à l'alignement Getty AAT et à la traduction multilingue avant validation." },
        { u: "c.roussel", d: "2018-03-18 11:40", t: "Validé. Traductions de/en/es/it en place, alignement Wikidata ajouté." }
      ]
    },
    "Laiton": {
      by: "m.lefevre", on: "2019-06-02", valBy: "c.roussel", valOn: "2019-06-09",
      votes: { up: 3, down: 1 }, participants: ["m.lefevre", "c.roussel"],
      discussion: [
        { u: "m.lefevre", d: "2019-06-02 15:10", t: "Souvent confondu avec le bronze dans les sources anciennes — préciser la note d'application." },
        { u: "c.roussel", d: "2019-06-09 08:55", t: "Note ajoutée, validé." }
      ]
    },
    "Statuette en bronze": {
      by: "anais.mauriceau", on: "2020-11-20", valBy: "blandine.nouvel", valOn: "2020-11-27",
      votes: { up: 2, down: 0 }, participants: ["anais.mauriceau", "blandine.nouvel"],
      discussion: [
        { u: "anais.mauriceau", d: "2020-11-20 14:30", t: "Distinguer du « vase en bronze » : usage votif/domestique. OK pour rattacher sous Statuaire." }
      ]
    }
  };

  C.forEach((c, i) => {
    const ark = c.id.split("/").pop();
    c.collection = c.path[0];
    c.facette = FACETTE[c.path[0]] || "—";
    c.altsHidden = ALTS_HIDDEN[c.pref] || [];
    c.collections = COLLECTIONS[c.pref] || (c.collection && c.collection !== "—"
      ? [{ id: "col-" + c.path[0].toLowerCase().replace(/\s+/g, "-"), nom: c.collection, membres: null }] : []);
    c.trAlt = TRAD_ALT[c.pref] || {};
    c.trStatus = TRAD_STATUS[c.pref] || {};
    c.note = NOTE[c.pref] || {};
    c.defTr = DEF_TR[c.pref] || {};
    c.branche = C.filter(o => o !== c && o.path.includes(c.pref)).length;
    c.tr = TRAD[c.pref] || {};
    c.idInterne = 6200 + i * 17;
    c.uri = `https://ark.frantiq.fr/ark:/26678/${ark}`;
    c.permalink = `26678/${ark}`;
    c.created = c.created || ["2007-02-08", "2009-05-14", "2011-09-30", "2008-11-22"][i % 4];
    c.modified = c.modified || ["2023-06-22", "2024-01-18", "2022-10-05", "2023-12-11"][i % 4];
    c.align = ALIGN[c.pref] || [];
    c.corpusCount = CORPUS_LINKS[c.pref] || 0;
    c.gps = GPS[c.pref] || "";
    if (c.status === "candidat") {
      c.cand = CAND[c.pref] || { by: "—", on: "—", votes: { up: 0, down: 0 }, mine: null, participants: [], discussion: [] };
    } else if (HIST[c.pref]) {
      c.hist = HIST[c.pref];
    }
  });

  // ── Annuaire des contributeurs (proposants / validateurs des candidats) ──
  const PEOPLE = {
    "c.roussel":       { name: "Camille Roussel",   initials: "CR", role: "Gestionnaire" },
    "blandine.nouvel": { name: "Blandine Nouvel",   initials: "BN", role: "Gestionnaire" },
    "a.costa":         { name: "Antonio Costa",     initials: "AC", role: "Contributeur" },
    "m.lefevre":       { name: "Marc Lefèvre",      initials: "ML", role: "Contributeur" },
    "j.bonnet":        { name: "Julie Bonnet",      initials: "JB", role: "Contributeur" },
    "anais.mauriceau": { name: "Anaïs Mauriceau",   initials: "AM", role: "Contributeur" },
    "l.petit":         { name: "Lucie Petit",       initials: "LP", role: "Contributeur" },
    "s.garnier":       { name: "Samuel Garnier",    initials: "SG", role: "Contributeur" }
  };

  // ── Journal de gouvernance des candidats ────────────────────────────────
  // Source unique pour les statistiques candidats. Chaque enregistrement = un
  // terme passé (ou en cours) dans le cycle de gestion :
  //   proposition (by, on)  →  discussion & vote (up/down, part = nb participants)
  //   →  décision (outcome : "attente" | "insere" | "rejete" ; valBy, valOn)
  // Couvre ~14 mois glissants (réf. juillet 2026). Cohérent avec les concepts
  // candidat / insere / rejete du thésaurus, enrichi pour donner de la matière.
  const candLog = [
    // ─── En attente (en cours de consensus) ───
    { term: "Céramique à vernis noir", by: "a.costa",         on: "2026-06-15", outcome: "attente", up: 4, down: 0, part: 4 },
    { term: "Fibule à charnière",      by: "l.petit",         on: "2026-07-02", outcome: "attente", up: 2, down: 1, part: 3 },
    { term: "Alliage argent-cuivre",   by: "c.roussel",       on: "2026-06-30", outcome: "attente", up: 1, down: 0, part: 1 },
    { term: "Tjarou",                  by: "anais.mauriceau", on: "2026-06-29", outcome: "attente", up: 1, down: 0, part: 2 },
    { term: "Bronze doré",             by: "a.costa",         on: "2026-05-21", outcome: "attente", up: 3, down: 1, part: 3 },
    { term: "Orichalque",              by: "m.lefevre",       on: "2026-05-18", outcome: "attente", up: 2, down: 0, part: 2 },
    { term: "Sigillée claire B",       by: "j.bonnet",        on: "2026-05-12", outcome: "attente", up: 0, down: 0, part: 1 },
    { term: "Umbo de bouclier",        by: "s.garnier",       on: "2026-03-28", outcome: "attente", up: 1, down: 2, part: 3 },
    // ─── Insérés (candidats acceptés) ───
    { term: "Patère",           by: "a.costa",         on: "2026-05-02", outcome: "insere", valBy: "c.roussel",       valOn: "2026-05-11", up: 4, down: 0, part: 4 },
    { term: "Balsamaire",       by: "s.garnier",       on: "2026-04-06", outcome: "insere", valBy: "blandine.nouvel", valOn: "2026-04-14", up: 3, down: 0, part: 3 },
    { term: "Vase campaniforme",by: "c.roussel",       on: "2026-05-30", outcome: "insere", valBy: "blandine.nouvel", valOn: "2026-06-04", up: 3, down: 0, part: 3 },
    { term: "Fibule ansée",     by: "m.lefevre",       on: "2026-04-02", outcome: "insere", valBy: "c.roussel",       valOn: "2026-04-09", up: 5, down: 1, part: 5 },
    { term: "Épée à antennes",  by: "m.lefevre",       on: "2026-03-19", outcome: "insere", valBy: "c.roussel",       valOn: "2026-03-27", up: 4, down: 0, part: 4 },
    { term: "Rasoir en bronze", by: "j.bonnet",        on: "2026-02-14", outcome: "insere", valBy: "c.roussel",       valOn: "2026-02-25", up: 3, down: 1, part: 4 },
    { term: "Strigile",         by: "a.costa",         on: "2026-01-22", outcome: "insere", valBy: "c.roussel",       valOn: "2026-01-30", up: 4, down: 0, part: 4 },
    { term: "Simpulum",         by: "l.petit",         on: "2026-01-08", outcome: "insere", valBy: "blandine.nouvel", valOn: "2026-01-16", up: 2, down: 0, part: 2 },
    { term: "Cnémide",          by: "anais.mauriceau", on: "2025-11-20", outcome: "insere", valBy: "c.roussel",       valOn: "2025-12-01", up: 3, down: 0, part: 3 },
    { term: "Phalère",          by: "a.costa",         on: "2025-10-11", outcome: "insere", valBy: "c.roussel",       valOn: "2025-10-19", up: 5, down: 0, part: 5 },
    { term: "Applique de char", by: "m.lefevre",       on: "2025-09-03", outcome: "insere", valBy: "blandine.nouvel", valOn: "2025-09-15", up: 4, down: 1, part: 5 },
    { term: "Œnochoé",          by: "j.bonnet",        on: "2025-07-14", outcome: "insere", valBy: "c.roussel",       valOn: "2025-07-22", up: 3, down: 0, part: 3 },
    { term: "Torque",           by: "a.costa",         on: "2025-06-10", outcome: "insere", valBy: "c.roussel",       valOn: "2025-06-20", up: 6, down: 0, part: 6 },
    // ─── Rejetés (propositions refusées) ───
    { term: "Objet indéterminé",   by: "l.petit",   on: "2026-04-18", outcome: "rejete", valBy: "c.roussel",       valOn: "2026-04-22", up: 0, down: 4, part: 4 },
    { term: "Bronze blanc",        by: "j.bonnet",  on: "2026-03-11", outcome: "rejete", valBy: "c.roussel",       valOn: "2026-03-15", up: 1, down: 3, part: 4 },
    { term: "Applique zoomorphe",  by: "a.costa",   on: "2026-02-20", outcome: "rejete", valBy: "c.roussel",       valOn: "2026-02-24", up: 0, down: 2, part: 3 },
    { term: "Métal blanc",         by: "s.garnier", on: "2025-12-15", outcome: "rejete", valBy: "blandine.nouvel", valOn: "2025-12-20", up: 1, down: 3, part: 4 },
    { term: "Cuivre rouge",        by: "m.lefevre", on: "2025-08-05", outcome: "rejete", valBy: "c.roussel",       valOn: "2025-08-12", up: 0, down: 3, part: 3 }
  ];

  window.THESAURUS = {
    name: "PACTOLS",
    fullName: "Peuples et cultures, Anthroponymes, Chronologie relative, Toponymes, Œuvres, Lieux, Sujets",
    lang: "fr",
    domain: "Archéologie & sciences de l'Antiquité",
    concepts: C,
    people: PEOPLE,
    candLog
  };

  // ── Session de démo : utilisateur connecté + thésaurus auxquels il a accès ──
  window.SESSION = {
    user: { name: "Camille Roussel", email: "c.roussel@mom.fr", initials: "CR", role: "Gestionnaire" },
    superAdmin: true,
    apiBase: "https://opentheso.huma-num.fr",
    projects: [
      { id: "pactols", name: "PACTOLS", role: "Gestionnaire", concepts: C.length, active: true, color: "#1f7a5c" },
      { id: "geoethno", name: "GeoEthno", role: "Contributeur", concepts: 4120, active: false, color: "#2f5fd0" },
      { id: "thesw", name: "Thésaurus-W", role: "Lecteur", concepts: 1860, active: false, color: "#9a3b2e" },
      { id: "garnier", name: "Garnier — Matériaux", role: "Contributeur", concepts: 980, active: false, color: "#5b4bb8" }
    ]
  };
})();
