package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class ConceptTableQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Lignes du tableau plat : id, label, notation, status, type code, type label,
     * statut candidat, auteur, date.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findTableConceptRows(
            String thesaurusId,
            String lang,
            boolean includeCandidates,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.id_concept,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   COALESCE(c.notation, '') AS notation,
                   COALESCE(c.status, '') AS status,
                   COALESCE(NULLIF(c.concept_type, ''), 'concept') AS concept_type,
                   COALESCE(ctype.type_label, '') AS type_label,
                   COALESCE(cs.id_status, 0) AS candidat_status,
                   COALESCE(u.username, '') AS cand_by,
                   COALESCE(TO_CHAR(cs.date, 'YYYY-MM-DD'), '') AS cand_on
            FROM concept c
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            LEFT JOIN LATERAL (
                SELECT cs2.id_status, cs2.date, cs2.id_user
                FROM candidat_status cs2
                WHERE cs2.id_concept = c.id_concept
                  AND cs2.id_thesaurus = c.id_thesaurus
                ORDER BY cs2.date DESC NULLS LAST
                LIMIT 1
            ) cs ON true
            LEFT JOIN users u ON u.id_user = cs.id_user
            LEFT JOIN LATERAL (
                SELECT COALESCE(
                    CASE
                        WHEN :lang LIKE 'en%' THEN NULLIF(ct2.label_en, '')
                        ELSE NULLIF(ct2.label_fr, '')
                    END,
                    NULLIF(ct2.label_fr, ''),
                    NULLIF(ct2.label_en, ''),
                    ct2.code
                ) AS type_label
                FROM concept_type ct2
                WHERE ct2.code = COALESCE(NULLIF(c.concept_type, ''), 'concept')
                  AND ct2.id_theso IN (:thesaurusId, 'all')
                ORDER BY CASE WHEN ct2.id_theso = :thesaurusId THEN 0 ELSE 1 END
                LIMIT 1
            ) ctype ON true
            WHERE c.id_thesaurus = :thesaurusId
            """);
        if (!includeCandidates) {
            sql.append(" AND c.status != 'CA' ");
        }
        sql.append(" ORDER BY label LIMIT ").append(Math.max(1, limit));
        return em.createNativeQuery(sql.toString())
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    /**
     * Arêtes BT du thésaurus : enfant, parent, libellé parent.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findBroaderEdges(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT hr.id_concept1 AS child_id,
                   hr.id_concept2 AS parent_id,
                   COALESCE(t.lexical_value, hr.id_concept2) AS parent_label
            FROM hierarchical_relationship hr
            LEFT JOIN preferred_term pt
                ON pt.id_concept = hr.id_concept2
                AND pt.id_thesaurus = hr.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = hr.id_thesaurus
                AND t.lang = :lang
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.role LIKE 'BT%'
            ORDER BY hr.id_concept1, hr.id_concept2
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }
}
