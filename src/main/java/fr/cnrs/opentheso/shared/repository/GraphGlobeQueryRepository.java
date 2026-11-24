package fr.cnrs.opentheso.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
     * Points du globe : id, label, statut. Requête légère pour charger tout le thésaurus.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findGlobeConcepts(
            String thesaurusId,
            String lang,
            boolean includeCandidates,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
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
            """);
        if (!includeCandidates) {
            sql.append(" AND c.status != 'CA' ");
        }
        sql.append(" ORDER BY label ");
        Query query = em.createNativeQuery(sql.toString())
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang);
        query.setMaxResults(Math.max(1, limit));
        return query.getResultList();
    }

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

    /**
     * Voisins BT / NT / RT d'un concept, une page par famille.
     * Colonnes : id, label, rôle, statut, famille (BT, NT, RT), total de la famille.
     * Les groupes privés suivent la règle de la fiche : un concept rangé seulement
     * dans des groupes privés est masqué si {@code includePrivateGroups} est faux.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findNeighborhood(
            String thesaurusId,
            String conceptId,
            String lang,
            boolean includeCandidates,
            boolean includePrivateGroups,
            NeighborPage page
    ) {
        return em.createNativeQuery("""
            WITH rel AS (
                SELECT hr.id_concept2 AS id,
                       SUBSTRING(hr.role, 1, 2) AS family,
                       MIN(hr.role) AS role
                FROM hierarchical_relationship hr
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 = :conceptId
                  AND hr.id_concept2 <> :conceptId
                  AND (hr.role LIKE 'BT%' OR hr.role LIKE 'NT%' OR hr.role LIKE 'RT%')
                GROUP BY hr.id_concept2, SUBSTRING(hr.role, 1, 2)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
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
    public record NeighborPage(int broaderOffset, int narrowerOffset, int relatedOffset, int size) {

        private static final int MAX_OFFSET = 1_000_000;

        public NeighborPage {
            broaderOffset = clamp(broaderOffset);
            narrowerOffset = clamp(narrowerOffset);
            relatedOffset = clamp(relatedOffset);
        }

        private static int clamp(int offset) {
            return Math.min(MAX_OFFSET, Math.max(0, offset));
        }
    }
}
