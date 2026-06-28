package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ConceptQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Object[]> findConceptGroups(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT cg.id_group, cg.id_thesaurus,
                   COALESCE(cgl.lexical_value, cg.id_group) AS label,
                   cg.notation, cg.display_order
            FROM concept_group cg
            LEFT JOIN concept_group_label cgl
                ON cgl.id_group = cg.id_group AND cgl.id_thesaurus = cg.id_thesaurus AND cgl.lang = :lang
            WHERE cg.id_thesaurus = :thesaurusId
            ORDER BY cg.display_order, label
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findTopConceptsOfGroup(String groupId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.notation, c.status,
                   EXISTS(SELECT 1 FROM hierarchical_relationship hr2
                          WHERE hr2.id_concept2 = c.id_concept AND hr2.id_thesaurus = c.id_thesaurus
                            AND hr2.role LIKE 'NT%') AS has_children
            FROM concept c
            JOIN concept_group_concept cgc ON cgc.id_concept = c.id_concept AND cgc.id_thesaurus = c.id_thesaurus
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE cgc.id_group = :groupId AND c.id_thesaurus = :thesaurusId
              AND NOT EXISTS(SELECT 1 FROM hierarchical_relationship hr
                             WHERE hr.id_concept1 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
                               AND hr.role LIKE 'BT%')
            ORDER BY label
            """)
                .setParameter("groupId", groupId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findTopConceptsWithoutGroup(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.notation, c.status,
                   EXISTS(SELECT 1 FROM hierarchical_relationship hr2
                          WHERE hr2.id_concept2 = c.id_concept AND hr2.id_thesaurus = c.id_thesaurus
                            AND hr2.role LIKE 'NT%') AS has_children
            FROM concept c
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE c.id_thesaurus = :thesaurusId
              AND NOT EXISTS(SELECT 1 FROM hierarchical_relationship hr
                             WHERE hr.id_concept1 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
                               AND hr.role LIKE 'BT%')
            ORDER BY label
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findChildConcepts(String parentId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.notation, c.status,
                   EXISTS(SELECT 1 FROM hierarchical_relationship hr2
                          WHERE hr2.id_concept2 = c.id_concept AND hr2.id_thesaurus = c.id_thesaurus
                            AND hr2.role LIKE 'NT%') AS has_children
            FROM concept c
            JOIN hierarchical_relationship hr ON hr.id_concept1 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE hr.id_concept2 = :parentId AND c.id_thesaurus = :thesaurusId AND hr.role LIKE 'NT%'
            ORDER BY label
            """)
                .setParameter("parentId", parentId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public Optional<Object[]> findConceptHeader(String conceptId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS pref_label,
                   t.lang, c.status, c.ark_id, c.id_type,
                   TO_CHAR(c.created, 'DD/MM/YYYY') AS created,
                   TO_CHAR(c.modified, 'DD/MM/YYYY') AS modified
            FROM concept c
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE c.id_concept = :conceptId AND c.id_thesaurus = :thesaurusId
            LIMIT 1
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Object[]> findConceptLabels(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT t.lang, t.lexical_value, t.hidden_label, true AS preferred
            FROM preferred_term pt
            JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
            WHERE pt.id_concept = :conceptId AND pt.id_thesaurus = :thesaurusId
            UNION ALL
            SELECT t.lang, t.lexical_value, t.hidden_label, false AS preferred
            FROM non_preferred_term npt
            JOIN term t ON t.id_term = npt.id_term AND t.id_thesaurus = npt.id_thesaurus
            WHERE npt.id_concept = :conceptId AND npt.id_thesaurus = :thesaurusId
            ORDER BY preferred DESC, lang
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> findConceptRelations(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT hr.role, c.id_concept,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.ark_id
            FROM hierarchical_relationship hr
            JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE hr.id_concept1 = :conceptId AND hr.id_thesaurus = :thesaurusId
            UNION ALL
            SELECT ar.role, c.id_concept,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.ark_id
            FROM associative_relationship ar
            JOIN concept c ON c.id_concept = ar.id_concept2 AND c.id_thesaurus = ar.id_thesaurus
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE ar.id_concept1 = :conceptId AND ar.id_thesaurus = :thesaurusId
            ORDER BY role, label
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findBreadcrumb(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            WITH RECURSIVE breadcrumb AS (
                SELECT hr.id_concept2 AS ancestor_id,
                       COALESCE(t.lexical_value, hr.id_concept2) AS ancestor_label,
                       1 AS depth
                FROM hierarchical_relationship hr
                LEFT JOIN preferred_term pt ON pt.id_concept = hr.id_concept2 AND pt.id_thesaurus = hr.id_thesaurus
                LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = hr.id_thesaurus AND t.lang = :lang
                WHERE hr.id_concept1 = :conceptId AND hr.id_thesaurus = :thesaurusId AND hr.role LIKE 'BT%'
                UNION ALL
                SELECT hr2.id_concept2,
                       COALESCE(t2.lexical_value, hr2.id_concept2),
                       b.depth + 1
                FROM hierarchical_relationship hr2
                JOIN breadcrumb b ON b.ancestor_id = hr2.id_concept1
                LEFT JOIN preferred_term pt2 ON pt2.id_concept = hr2.id_concept2 AND pt2.id_thesaurus = hr2.id_thesaurus
                LEFT JOIN term t2 ON t2.id_term = pt2.id_term AND t2.id_thesaurus = hr2.id_thesaurus AND t2.lang = :lang
                WHERE hr2.id_thesaurus = :thesaurusId AND hr2.role LIKE 'BT%'
            )
            SELECT DISTINCT ON (ancestor_id) ancestor_id, ancestor_label, depth
            FROM breadcrumb
            ORDER BY ancestor_id, depth DESC
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findConceptNotes(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT n.id_note, n.note_type_code, n.lang, n.lexical_value, n.identifier
            FROM note n
            WHERE n.id_concept = :conceptId AND n.id_thesaurus = :thesaurusId AND n.lang = :lang
            ORDER BY n.note_type_code
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findConceptAlignments(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT a.id_alignement, a.uri, a.internal_id_skos,
                   al.label AS skos_label, as2.name AS source_name, a.status
            FROM alignement a
            LEFT JOIN alignement_label al ON al.code = a.internal_id_skos
            LEFT JOIN alignement_source as2 ON as2.id_source = a.id_source
            WHERE a.id_concept = :conceptId AND a.id_thesaurus = :thesaurusId
            ORDER BY as2.name, a.uri
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> searchByLabel(String thesaurusId, String lang, String query, int limit) {
        return em.createNativeQuery("""
            SELECT DISTINCT c.id_concept, c.id_thesaurus,
                   t.lexical_value AS label,
                   c.notation, c.status, false AS has_children
            FROM concept c
            JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus
            WHERE c.id_thesaurus = :thesaurusId AND t.lang = :lang
              AND f_unaccent(LOWER(t.lexical_value)) LIKE f_unaccent(LOWER(:query))
            ORDER BY label
            LIMIT :limit
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("query", "%" + query + "%")
                .setParameter("limit", limit)
                .getResultList();
    }

    public boolean hasGroups(String thesaurusId) {
        Long count = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM concept_group WHERE id_thesaurus = :thesaurusId")
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult();
        return count != null && count > 0;
    }
}
