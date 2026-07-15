-- ============================================================
-- Migration : Index de performance manquants
-- Tous les index utilisent CONCURRENTLY pour éviter tout lock de table
-- Applicable en production sans interruption de service
-- ============================================================

-- 1. Parcours inverse de la hiérarchie (breadcrumb BT → parent)
--    Requête : WHERE id_concept1 = ? AND id_thesaurus = ? AND role = 'BT'
--    Sans cet index : seq scan sur toute la table pour trouver les parents
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hierarchical_rel_parent
    ON public.hierarchical_relationship (id_concept2, id_thesaurus, role);

-- 2. Filtrage par thésaurus + statut
--    Requête : WHERE id_thesaurus = ? AND status != 'CA'
--    Utilisé dans toutes les requêtes de consultation et de recherche
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_concept_thesaurus_status
    ON public.concept (id_thesaurus, status);

-- 3. Jointure concept ↔ terme préféré (très fréquente)
--    Requête : WHERE id_concept = ? AND id_thesaurus = ?
--    Actuellement seul id_term est indexé, pas la lookup par concept
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_preferred_term_concept
    ON public.preferred_term (id_concept, id_thesaurus);

-- 4. Synonymes et termes non-préférés par thésaurus et langue
--    Requête batch : WHERE id_thesaurus = ? AND lang = ? AND id_term IN (...)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_non_preferred_term_thesaurus_lang
    ON public.non_preferred_term (id_thesaurus, lang);

-- 5. Membres d'un groupe de concepts
--    Requête : WHERE idthesaurus = ? AND idconcept IN (...)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_concept_group_concept_lookup
    ON public.concept_group_concept (idthesaurus, idconcept);

-- 6. Terme par thésaurus et langue (batch preferred labels)
--    Requête : WHERE id_thesaurus = ? AND lang = ? AND id_term IN (...)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_term_thesaurus_lang
    ON public.term (id_thesaurus, lang);

-- ============================================================
-- Contraintes d'intégrité (sans impact sur les performances,
-- mais évitent la corruption silencieuse des données)
-- ============================================================

-- 7. Une note appartient soit à un concept, soit à un terme, jamais les deux
ALTER TABLE public.note
    ADD CONSTRAINT IF NOT EXISTS chk_note_single_owner
    CHECK (
        (id_concept IS NOT NULL AND id_term IS NULL) OR
        (id_concept IS NULL AND id_term IS NOT NULL)
    );

-- 8. Valeurs de statut autorisées sur concept
--    'CA' = candidat (exclu de la consultation publique)
--    'D'  = descripteur actif (DEFAULT)
--    'DEP' = déprécié
--    Empêche l'insertion de statuts inconnus (ex: faute de frappe 'dep' vs 'DEP')
ALTER TABLE public.concept
    ADD CONSTRAINT IF NOT EXISTS chk_concept_status_values
    CHECK (status IN ('CA', 'D', 'DEP'));
