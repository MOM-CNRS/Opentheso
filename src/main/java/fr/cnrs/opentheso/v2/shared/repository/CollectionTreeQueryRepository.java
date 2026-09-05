package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class CollectionTreeQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Collections racines : absentes de {@code relation_group} en tant qu'enfant ({@code relation = 'sub'}).
     * Colonnes : id, label, notation, has_children.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findRootGroups(String thesaurusId, String lang, boolean includePrivate) {
        return em.createNativeQuery("""
            SELECT cg.idgroup,
                   COALESCE((
                       SELECT cgl.lexicalvalue
                       FROM concept_group_label cgl
                       WHERE cgl.idgroup = cg.idgroup
                         AND cgl.idthesaurus = cg.idthesaurus
                         AND cgl.lang = :lang
                       ORDER BY cgl.id
                       LIMIT 1
                   ), cg.idgroup) AS label,
                   COALESCE(cg.notation, '') AS notation,
                   (
                       EXISTS(SELECT 1 FROM relation_group rg
                              WHERE rg.id_thesaurus = cg.idthesaurus
                                AND LOWER(rg.id_group1) = LOWER(cg.idgroup)
                                AND rg.relation = 'sub')
                       OR EXISTS(SELECT 1 FROM concept_group_concept cgc
                                 JOIN concept c
                                   ON c.id_concept = cgc.idconcept
                                  AND c.id_thesaurus = cgc.idthesaurus
                                 WHERE LOWER(cgc.idgroup) = LOWER(cg.idgroup)
                                   AND cgc.idthesaurus = cg.idthesaurus
                                   AND c.status != 'CA')
                   ) AS has_children
            FROM concept_group cg
            WHERE cg.idthesaurus = :thesaurusId
              AND LOWER(cg.idgroup) NOT IN (
                  SELECT LOWER(rg.id_group2)
                  FROM relation_group rg
                  WHERE rg.relation = 'sub'
                    AND rg.id_thesaurus = :thesaurusId
                    AND rg.id_group2 IS NOT NULL
              )
              AND (:includePrivate = true OR cg.private = false)
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.INCLUDE_PRIVATE, includePrivate)
                .getResultList();
    }

    /**
     * Toutes les collections du thésaurus, à plat. Colonnes : id, label, notation.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAllGroups(String thesaurusId, String lang, boolean includePrivate) {
        return em.createNativeQuery("""
            SELECT cg.idgroup,
                   COALESCE((
                       SELECT cgl.lexicalvalue
                       FROM concept_group_label cgl
                       WHERE cgl.idgroup = cg.idgroup
                         AND cgl.idthesaurus = cg.idthesaurus
                         AND cgl.lang = :lang
                       ORDER BY cgl.id
                       LIMIT 1
                   ), cg.idgroup) AS label,
                   COALESCE(cg.notation, '') AS notation
            FROM concept_group cg
            WHERE cg.idthesaurus = :thesaurusId
              AND (:includePrivate = true OR cg.private = false)
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.INCLUDE_PRIVATE, includePrivate)
                .getResultList();
    }

    /**
     * Sous-collections directes. Colonnes : id, label, notation, has_children.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findChildGroups(String parentGroupId, String thesaurusId, String lang, boolean includePrivate) {
        return em.createNativeQuery("""
            SELECT cg.idgroup,
                   COALESCE((
                       SELECT cgl.lexicalvalue
                       FROM concept_group_label cgl
                       WHERE cgl.idgroup = cg.idgroup
                         AND cgl.idthesaurus = cg.idthesaurus
                         AND cgl.lang = :lang
                       ORDER BY cgl.id
                       LIMIT 1
                   ), cg.idgroup) AS label,
                   COALESCE(cg.notation, '') AS notation,
                   (
                       EXISTS(SELECT 1 FROM relation_group rg2
                              WHERE rg2.id_thesaurus = cg.idthesaurus
                                AND LOWER(rg2.id_group1) = LOWER(cg.idgroup)
                                AND rg2.relation = 'sub')
                       OR EXISTS(SELECT 1 FROM concept_group_concept cgc
                                 JOIN concept c
                                   ON c.id_concept = cgc.idconcept
                                  AND c.id_thesaurus = cgc.idthesaurus
                                 WHERE LOWER(cgc.idgroup) = LOWER(cg.idgroup)
                                   AND cgc.idthesaurus = cg.idthesaurus
                                   AND c.status != 'CA')
                   ) AS has_children
            FROM relation_group rg
            JOIN concept_group cg
                ON LOWER(cg.idgroup) = LOWER(rg.id_group2)
                AND cg.idthesaurus = rg.id_thesaurus
            WHERE rg.id_thesaurus = :thesaurusId
              AND LOWER(rg.id_group1) = LOWER(:parentGroupId)
              AND rg.relation = 'sub'
              AND (:includePrivate = true OR cg.private = false)
            """)
                .setParameter("parentGroupId", parentGroupId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.INCLUDE_PRIVATE, includePrivate)
                .getResultList();
    }

    /**
     * Concepts membres directs (pas de hiérarchie NT). Colonnes : id, label, notation, status.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findMemberConcepts(String groupId, String thesaurusId, String lang, int limit) {
        return em.createNativeQuery("""
            SELECT c.id_concept,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   COALESCE(c.notation, '') AS notation,
                   COALESCE(c.status, '') AS status
            FROM concept c
            JOIN concept_group_concept cgc
                ON cgc.idconcept = c.id_concept
                AND cgc.idthesaurus = c.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            WHERE LOWER(cgc.idgroup) = LOWER(:groupId)
              AND c.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
            ORDER BY label
            """)
                .setParameter(NativeQueryParams.GROUP_ID, groupId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }

    /**
     * En-tête de fiche : id, label, notation, ark, handle, type, created, modified.
     */
    @SuppressWarnings("unchecked")
    public Optional<Object[]> findGroupHeader(String groupId, String thesaurusId, String lang, boolean includePrivate) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT cg.idgroup,
                   COALESCE((
                       SELECT cgl.lexicalvalue
                       FROM concept_group_label cgl
                       WHERE cgl.idgroup = cg.idgroup
                         AND cgl.idthesaurus = cg.idthesaurus
                         AND cgl.lang = :lang
                       ORDER BY cgl.id
                       LIMIT 1
                   ), cg.idgroup) AS label,
                   COALESCE(cg.notation, '') AS notation,
                   COALESCE(cg.id_ark, '') AS ark_id,
                   COALESCE(cg.id_handle, '') AS handle_id,
                   COALESCE(cg.idtypecode, '') AS type_code,
                   COALESCE(TO_CHAR(cg.created, 'YYYY-MM-DD'), '') AS created,
                   COALESCE(TO_CHAR(cg.modified, 'YYYY-MM-DD'), '') AS modified
            FROM concept_group cg
            WHERE LOWER(cg.idgroup) = LOWER(:groupId)
              AND cg.idthesaurus = :thesaurusId
              AND (:includePrivate = true OR cg.private = false)
            LIMIT 1
            """)
                .setParameter(NativeQueryParams.GROUP_ID, groupId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.INCLUDE_PRIVATE, includePrivate)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @SuppressWarnings("unchecked")
    public Optional<Object[]> findGroupType(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return Optional.empty();
        }
        List<Object[]> rows = em.createNativeQuery("""
            SELECT COALESCE(label, ''), COALESCE(skoslabel, '')
            FROM concept_group_type
            WHERE code = :typeCode
            LIMIT 1
            """)
                .setParameter(NativeQueryParams.TYPE_CODE, typeCode)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public int countMemberConcepts(String thesaurusId, String groupId) {
        Number count = (Number) em.createNativeQuery("""
            SELECT COUNT(c.id_concept)
            FROM concept c
            JOIN concept_group_concept cgc
                ON c.id_concept = cgc.idconcept
                AND c.id_thesaurus = cgc.idthesaurus
            WHERE c.id_thesaurus = :thesaurusId
              AND LOWER(cgc.idgroup) = LOWER(:groupId)
              AND c.status != 'CA'
            """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.GROUP_ID, groupId)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findGroupTranslations(String groupId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT cgl.lang, cgl.lexicalvalue
            FROM concept_group_label cgl
            WHERE LOWER(cgl.idgroup) = LOWER(:groupId)
              AND cgl.idthesaurus = :thesaurusId
              AND cgl.lang != :lang
            ORDER BY cgl.lang
            """)
                .setParameter(NativeQueryParams.GROUP_ID, groupId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findGroupNotes(String groupId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT n.notetypecode, n.lang, n.lexicalvalue
            FROM note n
            WHERE LOWER(n.identifier) = LOWER(:groupId)
              AND n.id_thesaurus = :thesaurusId
              AND n.lang = :lang
            ORDER BY n.notetypecode
            """)
                .setParameter(NativeQueryParams.GROUP_ID, groupId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }
}
