package fr.cnrs.opentheso.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class GraphGlobeQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Concept central : id, label, statut. Vide si le concept n'existe pas ou est un candidat masqué.
     */
    @SuppressWarnings("unchecked")
    public Optional<Object[]> findCenter(
            String thesaurusId,
            String conceptId,
            String lang,
            boolean includeCandidates
    ) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   COALESCE(c.status, '') AS status
            FROM concept c
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            WHERE c.id_thesaurus = :thesaurusId
              AND c.id_concept = :conceptId
              AND (:includeCandidates = true OR UPPER(TRIM(COALESCE(c.status, ''))) <> 'CA')
            LIMIT 1
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter("lang", lang)
                .setParameter("includeCandidates", includeCandidates)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Facette au centre : id, label, statut vide. */
    @SuppressWarnings("unchecked")
    public Optional<Object[]> findFacetCenter(String thesaurusId, String facetId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT ta.id_facet,
                   COALESCE(NULLIF(nl.lexical_value, ''), ta.id_facet) AS label,
                   '' AS status
            FROM thesaurus_array ta
            LEFT JOIN node_label nl
                ON nl.id_facet = ta.id_facet
                AND nl.id_thesaurus = ta.id_thesaurus
                AND nl.lang = :lang
            WHERE ta.id_thesaurus = :thesaurusId
              AND ta.id_facet = :facetId
            LIMIT 1
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.FACET_ID, facetId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * Facettes d'un concept : celles dont il est le parent (FACET) et celles qui le contiennent (FACETOF).
     * Mêmes colonnes que {@link #findNeighborhood}. Le parent peut être stocké par son ARK (ancien import).
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findFacets(String thesaurusId, String conceptId, String lang, int offset, int size) {
        return em.createNativeQuery("""
            WITH rel AS (
                SELECT ta.id_facet AS id, 'FACET' AS role
                FROM thesaurus_array ta
                WHERE ta.id_thesaurus = :thesaurusId
                  AND (
                      ta.id_concept_parent = :conceptId
                      OR ta.id_concept_parent = (
                          SELECT NULLIF(c.id_ark, '')
                          FROM concept c
                          WHERE c.id_concept = :conceptId
                            AND c.id_thesaurus = :thesaurusId
                      )
                  )
                UNION
                SELECT cf.id_facet AS id, 'FACETOF' AS role
                FROM concept_facet cf
                JOIN thesaurus_array ta
                    ON ta.id_facet = cf.id_facet
                    AND ta.id_thesaurus = cf.id_thesaurus
                WHERE cf.id_thesaurus = :thesaurusId
                  AND cf.id_concept = :conceptId
            ),
            visible AS (
                SELECT rel.id, MIN(rel.role) AS role,
                       MIN(COALESCE(NULLIF(nl.lexical_value, ''), rel.id)) AS label
                FROM rel
                LEFT JOIN node_label nl
                    ON nl.id_facet = rel.id
                    AND nl.id_thesaurus = :thesaurusId
                    AND nl.lang = :lang
                GROUP BY rel.id
            ),
            ranked AS (
                SELECT v.*,
                       ROW_NUMBER() OVER (ORDER BY v.role, LOWER(v.label), v.id) AS rn,
                       COUNT(*) OVER () AS total
                FROM visible v
            )
            SELECT id, label, role, '' AS status, 'FA' AS family, total
            FROM ranked
            WHERE rn BETWEEN :rowFrom AND :rowTo
            ORDER BY rn
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter("lang", lang)
                .setParameter("rowFrom", offset + 1)
                .setParameter("rowTo", offset + size)
                .getResultList();
    }

    private static final String CONCEPT_RELATIONS = """
            SELECT hr.id_concept2 AS id,
                   SUBSTRING(hr.role, 1, 2) AS family,
                   MIN(hr.role) AS role
            FROM hierarchical_relationship hr
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :centerId
              AND hr.id_concept2 <> :centerId
              AND (hr.role LIKE 'BT%' OR hr.role LIKE 'NT%' OR hr.role LIKE 'RT%')
            GROUP BY hr.id_concept2, SUBSTRING(hr.role, 1, 2)
            """;

    private static final String FACET_RELATIONS = """
            SELECT COALESCE(byid.id_concept, byark.id_concept) AS id, 'BT' AS family, 'PARENT' AS role
            FROM thesaurus_array ta
            LEFT JOIN concept byid
                ON byid.id_concept = ta.id_concept_parent
                AND byid.id_thesaurus = ta.id_thesaurus
            LEFT JOIN concept byark
                ON byark.id_ark = ta.id_concept_parent
                AND byark.id_thesaurus = ta.id_thesaurus
                AND byark.id_ark <> ''
            WHERE ta.id_thesaurus = :thesaurusId
              AND ta.id_facet = :centerId
            UNION
            SELECT cf.id_concept, 'NT', 'MEMBER'
            FROM concept_facet cf
            WHERE cf.id_thesaurus = :thesaurusId
              AND cf.id_facet = :centerId
            """;

    /**
     * Voisins BT / NT / RT d'un concept, une page par famille.
     * Colonnes : id, label, rôle, statut, famille (BT, NT, RT), total de la famille.
     * Les groupes privés suivent la règle de la fiche : un concept rangé seulement
     * dans des groupes privés est masqué si {@code includePrivateGroups} est faux.
     */
    public List<Object[]> findNeighborhood(
            String thesaurusId,
            String conceptId,
            String lang,
            boolean includeCandidates,
            boolean includePrivateGroups,
            NeighborPage page
    ) {
        return findPage(CONCEPT_RELATIONS, thesaurusId, conceptId, lang,
                includeCandidates, includePrivateGroups, page);
    }

    /** Autour d'une facette : concept parent (famille BT, rôle PARENT) et membres (famille NT, rôle MEMBER). */
    public List<Object[]> findFacetNeighborhood(
            String thesaurusId,
            String facetId,
            String lang,
            boolean includeCandidates,
            boolean includePrivateGroups,
            NeighborPage page
    ) {
        return findPage(FACET_RELATIONS, thesaurusId, facetId, lang,
                includeCandidates, includePrivateGroups, page);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findPage(
            String relations,
            String thesaurusId,
            String centerId,
            String lang,
            boolean includeCandidates,
            boolean includePrivateGroups,
            NeighborPage page
    ) {
        return em.createNativeQuery("WITH rel AS (" + relations + """
            ),
            visible AS (
                SELECT DISTINCT ON (rel.family, rel.id)
                       rel.id, rel.family, rel.role,
                       COALESCE(t.lexical_value, rel.id) AS label,
                       COALESCE(c.status, '') AS status
                FROM rel
                JOIN concept c
                    ON c.id_concept = rel.id
                    AND c.id_thesaurus = :thesaurusId
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                    AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = c.id_thesaurus
                    AND t.lang = :lang
                WHERE (:includeCandidates = true OR UPPER(TRIM(COALESCE(c.status, ''))) <> 'CA')
                  AND (
                      :includePrivateGroups = true
                      OR NOT EXISTS (
                          SELECT 1
                          FROM concept_group_concept cgc
                          JOIN concept_group cg
                              ON cg.idgroup = cgc.idgroup
                              AND cg.idthesaurus = cgc.idthesaurus
                              AND cg.private = true
                          WHERE cgc.idconcept = c.id_concept
                            AND cgc.idthesaurus = c.id_thesaurus
                      )
                      OR EXISTS (
                          SELECT 1
                          FROM concept_group_concept cgc
                          JOIN concept_group cg
                              ON cg.idgroup = cgc.idgroup
                              AND cg.idthesaurus = cgc.idthesaurus
                          WHERE cgc.idconcept = c.id_concept
                            AND cgc.idthesaurus = c.id_thesaurus
                            AND COALESCE(cg.private, false) = false
                      )
                  )
                ORDER BY rel.family, rel.id
            ),
            ranked AS (
                SELECT v.*,
                       ROW_NUMBER() OVER (PARTITION BY v.family ORDER BY LOWER(v.label), v.id) AS rn,
                       COUNT(*) OVER (PARTITION BY v.family) AS total
                FROM visible v
            )
            SELECT id, label, role, status, family, total
            FROM ranked
            WHERE (family = 'BT' AND rn BETWEEN :btFrom AND :btTo)
               OR (family = 'NT' AND rn BETWEEN :ntFrom AND :ntTo)
               OR (family = 'RT' AND rn BETWEEN :rtFrom AND :rtTo)
            ORDER BY family, rn
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("centerId", centerId)
                .setParameter("lang", lang)
                .setParameter("includeCandidates", includeCandidates)
                .setParameter("includePrivateGroups", includePrivateGroups)
                .setParameter("btFrom", page.broaderOffset() + 1)
                .setParameter("btTo", page.broaderOffset() + page.size())
                .setParameter("ntFrom", page.narrowerOffset() + 1)
                .setParameter("ntTo", page.narrowerOffset() + page.size())
                .setParameter("rtFrom", page.relatedOffset() + 1)
                .setParameter("rtTo", page.relatedOffset() + page.size())
                .getResultList();
    }

    /** Page demandée pour chaque famille ; les décalages négatifs sont ramenés à 0. */
    public record NeighborPage(
            int broaderOffset, int narrowerOffset, int relatedOffset, int facetOffset, int size) {

        private static final int MAX_OFFSET = 1_000_000;

        public NeighborPage {
            broaderOffset = clamp(broaderOffset);
            narrowerOffset = clamp(narrowerOffset);
            relatedOffset = clamp(relatedOffset);
            facetOffset = clamp(facetOffset);
        }

        private static int clamp(int offset) {
            return Math.min(MAX_OFFSET, Math.max(0, offset));
        }
    }
}
