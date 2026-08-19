package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.concept.model.ConceptFacetNodeRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptHeaderRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional(readOnly = true)
public class ConceptQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Object[]> findConceptGroups(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT cg.idgroup, cg.idthesaurus,
                   COALESCE(cgl.lexicalvalue, cg.idgroup) AS label,
                   cg.notation,
                   (
                       EXISTS(SELECT 1 FROM relation_group rg
                              WHERE rg.id_thesaurus = cg.idthesaurus
                                AND LOWER(rg.id_group1) = LOWER(cg.idgroup)
                                AND rg.relation = 'sub')
                       OR EXISTS(SELECT 1 FROM concept_group_concept cgc
                                 WHERE cgc.idgroup = cg.idgroup
                                   AND cgc.idthesaurus = cg.idthesaurus)
                   ) AS has_children
            FROM concept_group cg
            LEFT JOIN concept_group_label cgl
                ON cgl.idgroup = cg.idgroup
                AND cgl.idthesaurus = cg.idthesaurus
                AND cgl.lang = :lang
            WHERE cg.idthesaurus = :thesaurusId
              AND cg.idgroup NOT IN (
                  SELECT rg.id_group2
                  FROM relation_group rg
                  WHERE rg.relation = 'sub'
                    AND rg.id_thesaurus = :thesaurusId
              )
            ORDER BY cg.numerotation, label
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    /**
     * Toutes les collections du thésaurus (pour listes de sélection), un libellé par id —
     * équivalent legacy {@code GroupService#getListConceptGroup}.
     * Évite les doublons dus à des labels multiples sur le même groupe/langue.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAllConceptGroupsForSelection(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT cg.idgroup,
                   COALESCE(
                       (
                           SELECT cgl.lexicalvalue
                           FROM concept_group_label cgl
                           WHERE cgl.idgroup = cg.idgroup
                             AND cgl.idthesaurus = cg.idthesaurus
                             AND cgl.lang = :lang
                           ORDER BY cgl.id
                           LIMIT 1
                       ),
                       cg.idgroup
                   ) AS label
            FROM concept_group cg
            WHERE cg.idthesaurus = :thesaurusId
            ORDER BY label
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findChildConceptGroups(String parentGroupId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT cg.idgroup, cg.idthesaurus,
                   COALESCE(cgl.lexicalvalue, cg.idgroup) AS label,
                   cg.notation,
                   (
                       EXISTS(SELECT 1 FROM relation_group rg
                              WHERE rg.id_thesaurus = cg.idthesaurus
                                AND LOWER(rg.id_group1) = LOWER(cg.idgroup)
                                AND rg.relation = 'sub')
                       OR EXISTS(SELECT 1 FROM concept_group_concept cgc
                                 WHERE cgc.idgroup = cg.idgroup
                                   AND cgc.idthesaurus = cg.idthesaurus)
                   ) AS has_children
            FROM relation_group rg
            JOIN concept_group cg
                ON LOWER(cg.idgroup) = LOWER(rg.id_group2)
                AND cg.idthesaurus = rg.id_thesaurus
            LEFT JOIN concept_group_label cgl
                ON cgl.idgroup = cg.idgroup
                AND cgl.idthesaurus = cg.idthesaurus
                AND cgl.lang = :lang
            WHERE rg.id_thesaurus = :thesaurusId
              AND LOWER(rg.id_group1) = LOWER(:parentGroupId)
              AND rg.relation = 'sub'
            ORDER BY cg.numerotation, label
            """)
                .setParameter("parentGroupId", parentGroupId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<ConceptTreeRow> findTopConceptsOfGroup(String groupId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.notation, c.status,
                   EXISTS(SELECT 1 FROM hierarchical_relationship hr2
                          WHERE hr2.id_concept1 = c.id_concept
                            AND hr2.id_thesaurus = c.id_thesaurus
                            AND hr2.role LIKE 'NT%') AS has_children
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
            WHERE cgc.idgroup = :groupId
              AND c.id_thesaurus = :thesaurusId
              AND NOT EXISTS(SELECT 1 FROM hierarchical_relationship hr
                             WHERE hr.id_concept1 = c.id_concept
                               AND hr.id_thesaurus = c.id_thesaurus
                               AND hr.role LIKE 'BT%')
              AND c.status != 'CA'
            ORDER BY label
            LIMIT 2000
            """)
                .setParameter("groupId", groupId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return toConceptTreeRows6(rows);
    }

    public List<ConceptTreeRow> findChildConcepts(String parentId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.notation, c.status,
                   EXISTS(SELECT 1 FROM hierarchical_relationship hr2
                          WHERE hr2.id_concept1 = c.id_concept
                            AND hr2.id_thesaurus = c.id_thesaurus
                            AND hr2.role LIKE 'NT%') AS has_children
            FROM concept c
            JOIN hierarchical_relationship hr
                ON hr.id_concept2 = c.id_concept
                AND hr.id_thesaurus = c.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            WHERE hr.id_concept1 = :parentId
              AND c.id_thesaurus = :thesaurusId
              AND hr.role LIKE 'NT%'
              AND c.status != 'CA'
            ORDER BY label
            LIMIT 2000
            """)
                .setParameter("parentId", parentId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return toConceptTreeRows6(rows);
    }

    public Optional<ConceptHeaderRow> findConceptHeader(String conceptId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS pref_label,
                   t.lang, c.status, c.id_ark, c.concept_type,
                   COALESCE(c.notation, '') AS notation,
                   TO_CHAR(c.created, 'DD/MM/YYYY') AS created,
                   TO_CHAR(c.modified, 'DD/MM/YYYY') AS modified,
                   COALESCE(uc.username, '') AS creator_name
            FROM concept c
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            LEFT JOIN users uc
                ON uc.id_user = c.creator
                AND c.creator > 0
            WHERE c.id_concept = :conceptId
              AND c.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
            LIMIT 1
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] r = rows.get(0);
        return Optional.of(new ConceptHeaderRow(
                str(r[0]), str(r[1]), str(r[2]), str(r[3]), str(r[4]),
                str(r[5]), str(r[6]), str(r[7]), str(r[8]), str(r[9]), str(r[10])
        ));
    }

    public List<Object[]> findBreadcrumb(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            WITH RECURSIVE breadcrumb AS (
                SELECT hr.id_concept2 AS ancestor_id,
                       COALESCE(t.lexical_value, hr.id_concept2) AS ancestor_label,
                       1 AS depth
                FROM hierarchical_relationship hr
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = hr.id_concept2
                    AND pt.id_thesaurus = hr.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = hr.id_thesaurus
                    AND t.lang = :lang
                WHERE hr.id_concept1 = :conceptId
                  AND hr.id_thesaurus = :thesaurusId
                  AND hr.role LIKE 'BT%'
                UNION ALL
                SELECT hr2.id_concept2,
                       COALESCE(t2.lexical_value, hr2.id_concept2),
                       b.depth + 1
                FROM hierarchical_relationship hr2
                JOIN breadcrumb b ON b.ancestor_id = hr2.id_concept1
                LEFT JOIN preferred_term pt2
                    ON pt2.id_concept = hr2.id_concept2
                    AND pt2.id_thesaurus = hr2.id_thesaurus
                LEFT JOIN term t2
                    ON t2.id_term = pt2.id_term
                    AND t2.id_thesaurus = hr2.id_thesaurus
                    AND t2.lang = :lang
                WHERE hr2.id_thesaurus = :thesaurusId
                  AND hr2.role LIKE 'BT%'
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
            SELECT n.id, n.notetypecode, n.lang, n.lexicalvalue, n.identifier
            FROM note n
            WHERE n.identifier = :conceptId
              AND n.id_thesaurus = :thesaurusId
              AND n.lang = :lang
            ORDER BY n.notetypecode
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findConceptsOfGroup(String groupId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_thesaurus,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.notation, c.status,
                   false AS has_children
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
            WHERE cgc.idgroup = :groupId
              AND c.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
            ORDER BY label
            LIMIT 4001
            """)
                .setParameter("groupId", groupId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<ConceptTreeRow> searchByLabel(String thesaurusId, String lang, String query, int limit) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT DISTINCT c.id_concept, c.id_thesaurus,
                   t.lexical_value AS label,
                   c.notation, c.status, false AS has_children
            FROM concept c
            JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
            WHERE c.id_thesaurus = :thesaurusId
              AND t.lang = :lang
              AND f_unaccent(LOWER(t.lexical_value)) LIKE f_unaccent(LOWER(:query))
              AND c.status != 'CA'
            ORDER BY label
            LIMIT :limit
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("query", "%" + query + "%")
                .setParameter("limit", limit)
                .getResultList();
        return toConceptTreeRows6(rows);
    }

    public boolean hasGroups(String thesaurusId) {
        Long count = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM concept_group WHERE idthesaurus = :thesaurusId")
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult();
        return count != null && count > 0;
    }

    public Optional<String> findPreferredTermId(String conceptId, String thesaurusId, String lang) {
        List<?> rows = em.createNativeQuery("""
            SELECT pt.id_term
            FROM preferred_term pt
            JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
            WHERE pt.id_concept = :conceptId
              AND pt.id_thesaurus = :thesaurusId
              AND t.lang = :lang
            LIMIT 1
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(rows.get(0)));
    }

    public List<Object[]> searchIndexPreferredPrefix(String thesaurusId, String lang, String value) {
        return searchIndexTerms(thesaurusId, lang, value, false, false);
    }

    public List<Object[]> searchIndexPreferredPermuted(String thesaurusId, String lang, String value) {
        return searchIndexTerms(thesaurusId, lang, value, true, false);
    }

    public List<Object[]> searchIndexSynonymsPrefix(String thesaurusId, String lang, String value) {
        return searchIndexTerms(thesaurusId, lang, value, false, true);
    }

    public List<Object[]> searchIndexSynonymsPermuted(String thesaurusId, String lang, String value) {
        return searchIndexTerms(thesaurusId, lang, value, true, true);
    }

    private List<Object[]> searchIndexTerms(
            String thesaurusId,
            String lang,
            String value,
            boolean permuted,
            boolean synonyms
    ) {
        String pattern = permuted ? "% " + value + "%" : value + "%";
        if (synonyms) {
            return em.createNativeQuery("""
                SELECT DISTINCT pt.id_concept, npt.lexical_value
                FROM non_preferred_term npt
                JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                WHERE npt.lang = :lang
                  AND npt.id_thesaurus = :thesaurusId
                  AND f_unaccent(LOWER(npt.lexical_value)) LIKE f_unaccent(LOWER(:pattern))
                  AND c.status != 'CA'
                ORDER BY npt.lexical_value ASC
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("lang", lang)
                    .setParameter("pattern", pattern)
                    .getResultList();
        }
        return em.createNativeQuery("""
            SELECT DISTINCT c.id_concept, t.lexical_value
            FROM concept c
            JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus
            WHERE t.lang = :lang
              AND c.id_thesaurus = :thesaurusId
              AND f_unaccent(LOWER(t.lexical_value)) LIKE f_unaccent(LOWER(:pattern))
              AND c.status != 'CA'
            ORDER BY t.lexical_value ASC
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("pattern", pattern)
                .getResultList();
    }

    public List<ConceptTreeRow> findFacetMembers(String facetId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.status,
                   (
                       EXISTS(
                           SELECT 1
                           FROM hierarchical_relationship hr
                           JOIN concept child
                               ON child.id_concept = hr.id_concept2
                               AND child.id_thesaurus = hr.id_thesaurus
                           WHERE hr.id_concept1 = c.id_concept
                             AND hr.id_thesaurus = c.id_thesaurus
                             AND hr.role LIKE 'NT%'
                             AND child.status != 'CA'
                       )
                       OR EXISTS(
                           SELECT 1
                           FROM thesaurus_array ta
                           WHERE ta.id_concept_parent = c.id_concept
                             AND ta.id_thesaurus = c.id_thesaurus
                       )
                   ) AS has_children
            FROM concept_facet cf
            JOIN concept c ON c.id_concept = cf.id_concept AND c.id_thesaurus = cf.id_thesaurus
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE cf.id_facet = :facetId
              AND cf.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
            ORDER BY label
            """)
                .setParameter("facetId", facetId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        // columns: [0]=id_concept, [1]=label, [2]=status, [3]=has_children
        return rows.stream()
                .map(r -> new ConceptTreeRow(
                        str(r[0]), "", str(r[1]), str(r[2]),
                        "true".equalsIgnoreCase(str(r[3])) || "t".equalsIgnoreCase(str(r[3]))
                ))
                .toList();
    }

    public int countDescendantConcepts(String thesaurusId, String conceptId) {
        Number count = (Number) em.createNativeQuery("""
            WITH RECURSIVE descendants AS (
                SELECT hr.id_concept2 AS concept_id
                FROM hierarchical_relationship hr
                JOIN concept c
                    ON c.id_concept = hr.id_concept2
                    AND c.id_thesaurus = hr.id_thesaurus
                WHERE hr.id_concept1 = :conceptId
                  AND hr.id_thesaurus = :thesaurusId
                  AND hr.role LIKE 'NT%'
                  """ + excludeRejectedCandidates("c") + """
                UNION
                SELECT hr2.id_concept2
                FROM hierarchical_relationship hr2
                JOIN descendants d ON d.concept_id = hr2.id_concept1
                JOIN concept c2
                    ON c2.id_concept = hr2.id_concept2
                    AND c2.id_thesaurus = hr2.id_thesaurus
                WHERE hr2.id_thesaurus = :thesaurusId
                  AND hr2.role LIKE 'NT%'
                  """ + excludeRejectedCandidates("c2") + """
            )
            SELECT COUNT(*) FROM descendants
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    public int countConceptsInGroup(String thesaurusId, String groupId) {
        Number count = (Number) em.createNativeQuery("""
            SELECT COUNT(c.id_concept)
            FROM concept c
            JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept AND c.id_thesaurus = cgc.idthesaurus
            WHERE c.id_thesaurus = :thesaurusId
              AND LOWER(cgc.idgroup) = LOWER(:groupId)
              AND c.status != 'CA'
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("groupId", groupId)
                .getSingleResult();
        return count == null ? 0 : count.intValue();
    }

    public Optional<Object[]> findGroupHeader(String groupId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT cg.idgroup,
                   COALESCE(cgl.lexicalvalue, cg.idgroup) AS label,
                   COALESCE(cg.notation, '') AS notation,
                   COALESCE(cg.id_ark, '') AS ark_id,
                   COALESCE(cg.id_handle, '') AS handle_id,
                   COALESCE(cg.idtypecode, '') AS type_code
            FROM concept_group cg
            LEFT JOIN concept_group_label cgl
                ON cgl.idgroup = cg.idgroup
                AND cgl.idthesaurus = cg.idthesaurus
                AND cgl.lang = :lang
            WHERE LOWER(cg.idgroup) = LOWER(:groupId)
              AND cg.idthesaurus = :thesaurusId
            LIMIT 1
            """)
                .setParameter("groupId", groupId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Object[]> findGroupTranslations(String groupId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT cgl.lang, cgl.lexicalvalue
            FROM concept_group_label cgl
            WHERE LOWER(cgl.idgroup) = LOWER(:groupId)
              AND cgl.idthesaurus = :thesaurusId
              AND cgl.lang != :lang
            ORDER BY cgl.lang
            """)
                .setParameter("groupId", groupId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

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
                .setParameter("typeCode", typeCode)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<Object[]> findFacetHeader(String facetId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT ta.id_facet,
                   ta.id_concept_parent,
                   COALESCE(nl.lexical_value, '') AS label
            FROM thesaurus_array ta
            LEFT JOIN node_label nl
                ON nl.id_facet = ta.id_facet
                AND nl.id_thesaurus = ta.id_thesaurus
                AND nl.lang = :lang
            WHERE ta.id_facet = :facetId
              AND ta.id_thesaurus = :thesaurusId
            LIMIT 1
            """)
                .setParameter("facetId", facetId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Object[]> findFacetTranslations(String facetId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT nl.lang, nl.lexical_value
            FROM node_label nl
            WHERE nl.id_facet = :facetId
              AND nl.id_thesaurus = :thesaurusId
              AND nl.lang != :lang
            ORDER BY nl.lang
            """)
                .setParameter("facetId", facetId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findNotesByIdentifier(String identifier, String thesaurusId, String lang) {
        return findConceptNotes(identifier, thesaurusId, lang);
    }

    public int countBranchConcepts(String thesaurusId, String conceptId) {
        Number result = (Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM opentheso_get_narrowers_ignorefacet(:thesaurusId, :conceptId)
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getSingleResult();
        return result != null ? result.intValue() : 0;
    }

    public List<String> findBroaderConceptIds(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT id_concept2
            FROM hierarchical_relationship
            WHERE id_thesaurus = :thesaurusId
              AND id_concept1 = :conceptId
              AND role LIKE 'BT%'
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    /**
     * Remonte toute la hiérarchie d'un concept en une seule requête CTE récursive.
     * Retourne les arêtes (child → parent) et les labels, triés du plus profond au plus haut.
     * Remplace la boucle récursive Java qui émettait 1 requête par niveau.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAncestorsWithLabels(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            WITH RECURSIVE ancestors AS (
                SELECT hr.id_concept1 AS child_id,
                       hr.id_concept2 AS parent_id,
                       1              AS depth
                FROM hierarchical_relationship hr
                WHERE hr.id_concept1   = :conceptId
                  AND hr.id_thesaurus  = :thesaurusId
                  AND hr.role LIKE 'BT%'

                UNION ALL

                SELECT hr.id_concept1,
                       hr.id_concept2,
                       a.depth + 1
                FROM hierarchical_relationship hr
                JOIN ancestors a ON hr.id_concept1 = a.parent_id
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.role LIKE 'BT%'
                  AND a.depth < 30
            )
            SELECT DISTINCT
                a.child_id,
                a.parent_id,
                a.depth,
                COALESCE(t.lexical_value, a.parent_id) AS parent_label
            FROM ancestors a
            LEFT JOIN preferred_term pt
                ON pt.id_concept   = a.parent_id
               AND pt.id_thesaurus = :thesaurusId
            LEFT JOIN term t
                ON t.id_term       = pt.id_term
               AND t.id_thesaurus  = :thesaurusId
               AND t.lang          = :lang
            ORDER BY a.depth DESC
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    /**
     * Récupère le label d'un concept unique (pour compléter le nœud feuille du breadcrumb).
     */
    public String findConceptLabel(String conceptId, String thesaurusId, String lang) {
        return findConceptHeader(conceptId, thesaurusId, lang)
                .map(row -> org.apache.commons.lang3.StringUtils.defaultIfBlank(row.prefLabel(), "(" + conceptId + ")"))
                .orElse("(" + conceptId + ")");
    }

    public List<ConceptTreeRow> findTopConceptsForTree(String thesaurusId, String lang, boolean includePrivateGroups) {
        // has_children is resolved in one bulk query (avoids correlated EXISTS per top concept).
        List<Object[]> rows;
        if (includePrivateGroups) {
            rows = em.createNativeQuery("""
                SELECT c.id_concept,
                       COALESCE(c.notation, '') AS notation,
                       COALESCE(t.lexical_value, c.id_concept) AS label,
                       c.status,
                       false AS has_children
                FROM concept c
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                    AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = c.id_thesaurus
                    AND t.lang = :lang
                WHERE c.top_concept = true
                  AND c.status != 'CA'
                  AND c.id_thesaurus = :thesaurusId
                ORDER BY c.notation, label
                LIMIT 2000
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("lang", lang)
                    .getResultList();
        } else {
            rows = em.createNativeQuery("""
                SELECT c.id_concept,
                       COALESCE(c.notation, '') AS notation,
                       COALESCE(t.lexical_value, c.id_concept) AS label,
                       c.status,
                       false AS has_children
                FROM concept c
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                    AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = c.id_thesaurus
                    AND t.lang = :lang
                LEFT JOIN concept_group_concept cgc
                    ON c.id_concept = cgc.idconcept
                    AND c.id_thesaurus = cgc.idthesaurus
                LEFT JOIN concept_group cg
                    ON cgc.idgroup = cg.idgroup
                WHERE c.top_concept = true
                  AND c.status != 'CA'
                  AND c.id_thesaurus = :thesaurusId
                GROUP BY c.id_concept, c.notation, t.lexical_value, c.status
                HAVING BOOL_OR(cg.private IS NULL OR cg.private = false)
                ORDER BY c.notation, label
                LIMIT 2000
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("lang", lang)
                    .getResultList();
        }
        return withBulkHasChildren(thesaurusId, toConceptTreeRows(rows), false);
    }

    public List<ConceptTreeRow> findFacetMembersForTree(String thesaurusId, String facetId, String lang) {
        return findFacetMembers(facetId, thesaurusId, lang);
    }

    public List<ConceptTreeRow> findNarrowersForTree(
            String thesaurusId,
            String parentId,
            String lang,
            boolean includePrivateGroups
    ) {
        String facetExclusion = """
            AND NOT EXISTS (
                SELECT 1
                FROM concept_facet cf
                JOIN thesaurus_array ta
                    ON cf.id_facet = ta.id_facet
                    AND cf.id_thesaurus = ta.id_thesaurus
                WHERE cf.id_concept = c.id_concept
                  AND cf.id_thesaurus = c.id_thesaurus
                  AND ta.id_concept_parent = :parentId
                  AND ta.id_thesaurus = :thesaurusId
            )
            """;
        List<Object[]> rows;
        if (includePrivateGroups) {
            rows = em.createNativeQuery("""
                SELECT c.id_concept,
                       COALESCE(c.notation, '') AS notation,
                       COALESCE(t.lexical_value, c.id_concept) AS label,
                       c.status,
                       """ + hasChildrenExpression() + """
                FROM hierarchical_relationship hr
                JOIN concept c
                    ON c.id_concept = hr.id_concept2
                    AND c.id_thesaurus = hr.id_thesaurus
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                    AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = c.id_thesaurus
                    AND t.lang = :lang
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 = :parentId
                  AND hr.role LIKE 'NT%'
                  AND c.status != 'CA'
                """ + facetExclusion + """
                ORDER BY c.notation, label
                LIMIT 2000
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("parentId", parentId)
                    .setParameter("lang", lang)
                    .getResultList();
        } else {
            rows = em.createNativeQuery("""
                SELECT c.id_concept,
                       COALESCE(c.notation, '') AS notation,
                       COALESCE(t.lexical_value, c.id_concept) AS label,
                       c.status,
                       """ + hasChildrenExpression() + """
                FROM hierarchical_relationship hr
                JOIN concept c
                    ON c.id_concept = hr.id_concept2
                    AND c.id_thesaurus = hr.id_thesaurus
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                    AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = c.id_thesaurus
                    AND t.lang = :lang
                LEFT JOIN concept_group_concept cgc
                    ON c.id_concept = cgc.idconcept
                    AND c.id_thesaurus = cgc.idthesaurus
                LEFT JOIN concept_group cg
                    ON cgc.idgroup = cg.idgroup
                    AND cgc.idthesaurus = cg.idthesaurus
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 = :parentId
                  AND hr.role LIKE 'NT%'
                  AND c.status != 'CA'
                """ + facetExclusion + """
                GROUP BY c.id_concept, c.notation, t.lexical_value, c.status
                HAVING BOOL_OR(cg.private IS NULL OR cg.private = false)
                ORDER BY c.notation, label
                LIMIT 2000
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("parentId", parentId)
                    .setParameter("lang", lang)
                    .getResultList();
        }
        return toConceptTreeRows(rows);
    }

    public List<ConceptTreeRow> findTreeRootConcepts(String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept,
                   COALESCE(c.notation, '') AS notation,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.status,
                   false AS has_children
            FROM concept c
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            WHERE c.id_thesaurus = :thesaurusId
              AND (
                  c.top_concept = true
                  OR NOT EXISTS (
                      SELECT 1
                      FROM hierarchical_relationship hr
                      WHERE hr.id_concept1 = c.id_concept
                        AND hr.id_thesaurus = c.id_thesaurus
                        AND hr.role LIKE 'BT%'
                  )
              )
            """ + excludeRejectedCandidates("c") + """
            ORDER BY c.notation, label
            LIMIT 4000
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return withBulkHasChildren(thesaurusId, toConceptTreeRows(rows), true);
    }

    public List<ConceptTreeRow> findTreeChildConcepts(String thesaurusId, String parentId, String lang) {
        String facetExclusion = """
            AND NOT EXISTS (
                SELECT 1
                FROM concept_facet cf
                JOIN thesaurus_array ta
                    ON cf.id_facet = ta.id_facet
                    AND cf.id_thesaurus = ta.id_thesaurus
                WHERE cf.id_concept = c.id_concept
                  AND cf.id_thesaurus = c.id_thesaurus
                  AND ta.id_concept_parent = :parentId
                  AND ta.id_thesaurus = :thesaurusId
            )
            """;
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept,
                   COALESCE(c.notation, '') AS notation,
                   COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.status,
                   """ + hasChildrenExpression(true) + """
            FROM hierarchical_relationship hr
            JOIN concept c
                ON c.id_concept = hr.id_concept2
                AND c.id_thesaurus = hr.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :parentId
              AND hr.role LIKE 'NT%'
            """ + facetExclusion + excludeRejectedCandidates("c") + """
            ORDER BY c.notation, label
            LIMIT 4000
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("parentId", parentId)
                .setParameter("lang", lang)
                .getResultList();
        return toConceptTreeRows(rows);
    }

    public List<Object[]> findCandidateMeta(String thesaurusId, List<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        return em.createNativeQuery("""
            SELECT cs.id_concept,
                   COALESCE(u.username, '') AS created_by,
                   TO_CHAR(cs.date, 'YYYY-MM-DD') AS created_on
            FROM candidat_status cs
            LEFT JOIN users u ON u.id_user = cs.id_user
            WHERE cs.id_thesaurus = :thesaurusId
              AND cs.id_concept IN (:conceptIds)
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptIds", conceptIds)
                .getResultList();
    }

    public List<ConceptFacetNodeRow> findFacetsOfConceptForTree(String thesaurusId, String conceptId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT ta.id_facet,
                   COALESCE(nl.lexical_value, ta.id_facet) AS label,
                   EXISTS(
                       SELECT 1
                       FROM concept_facet cf
                       WHERE cf.id_facet = ta.id_facet
                         AND cf.id_thesaurus = ta.id_thesaurus
                   ) AS has_members
            FROM thesaurus_array ta
            LEFT JOIN node_label nl
                ON nl.id_facet = ta.id_facet
                AND nl.id_thesaurus = ta.id_thesaurus
                AND nl.lang = :lang
            WHERE ta.id_thesaurus = :thesaurusId
              AND (
                  ta.id_concept_parent = :conceptId
                  OR ta.id_concept_parent = (
                      SELECT c.id_ark
                      FROM concept c
                      WHERE c.id_concept = :conceptId
                        AND c.id_thesaurus = :thesaurusId
                  )
              )
            ORDER BY label
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .setParameter("lang", lang)
                .getResultList();
        // columns: [0]=id_facet, [1]=label, [2]=has_members
        return rows.stream()
                .map(r -> new ConceptFacetNodeRow(
                        str(r[0]), str(r[1]),
                        "true".equalsIgnoreCase(str(r[2])) || "t".equalsIgnoreCase(str(r[2]))
                ))
                .toList();
    }

    public List<Object[]> findCustomRelations(String conceptId, String thesaurusId, String lang, String interfaceLang) {
        return em.createNativeQuery("""
            SELECT hr.id_concept2,
                   COALESCE(t.lexical_value, hr.id_concept2) AS target_label,
                   hr.role,
                   COALESCE(
                       CASE WHEN LOWER(:interfaceLang) = 'fr' THEN ct.label_fr ELSE ct.label_en END,
                       hr.role
                   ) AS relation_label,
                   COALESCE(ct.reciprocal, false) AS reciprocal
            FROM hierarchical_relationship hr
            JOIN concept c
                ON c.id_concept = hr.id_concept2
                AND c.id_thesaurus = hr.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            LEFT JOIN LATERAL (
                SELECT ct2.label_fr, ct2.label_en, ct2.reciprocal
                FROM concept_type ct2
                WHERE ct2.code = hr.role
                  AND ct2.id_theso IN (:thesaurusId, 'all')
                ORDER BY CASE WHEN ct2.id_theso = :thesaurusId THEN 0 ELSE 1 END
                LIMIT 1
            ) ct ON true
            WHERE hr.id_concept1 = :conceptId
              AND hr.id_thesaurus = :thesaurusId
              AND hr.role NOT IN ('BT', 'BTG', 'BTP', 'BTI', 'NT', 'NTG', 'NTP', 'NTI', 'RT')
              AND c.status != 'CA'
            ORDER BY relation_label, target_label
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("interfaceLang", interfaceLang)
                .getResultList();
    }

    public Optional<Object[]> findConceptType(String code, String thesaurusId) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        List<Object[]> rows = em.createNativeQuery("""
            SELECT COALESCE(label_fr, ''), COALESCE(label_en, ''), COALESCE(reciprocal, false)
            FROM concept_type
            WHERE code = :code
              AND id_theso IN (:thesaurusId, 'all')
            ORDER BY CASE WHEN id_theso = :thesaurusId THEN 0 ELSE 1 END
            LIMIT 1
            """)
                .setParameter("code", code)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    // columns: [0]=id_concept, [1]=id_thesaurus (ignored), [2]=label, [3]=notation, [4]=status, [5]=has_children
    private static List<ConceptTreeRow> toConceptTreeRows6(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new ConceptTreeRow(
                        str(r[0]),
                        str(r[3]),
                        str(r[2]),
                        str(r[4]),
                        "true".equalsIgnoreCase(str(r[5])) || "t".equalsIgnoreCase(str(r[5]))
                ))
                .toList();
    }

    // columns: [0]=id_concept, [1]=notation, [2]=label, [3]=status, [4]=has_children
    private static List<ConceptTreeRow> toConceptTreeRows(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new ConceptTreeRow(
                        str(r[0]),
                        str(r[1]),
                        str(r[2]),
                        str(r[3]),
                        "true".equalsIgnoreCase(str(r[4])) || "t".equalsIgnoreCase(str(r[4]))
                ))
                .toList();
    }

    private List<ConceptTreeRow> withBulkHasChildren(String thesaurusId, List<ConceptTreeRow> rows) {
        return withBulkHasChildren(thesaurusId, rows, false);
    }

    private List<ConceptTreeRow> withBulkHasChildren(
            String thesaurusId,
            List<ConceptTreeRow> rows,
            boolean includeAllStatuses
    ) {
        if (rows.isEmpty()) {
            return rows;
        }
        List<String> parentIds = rows.stream()
                .map(ConceptTreeRow::conceptId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (parentIds.isEmpty()) {
            return rows;
        }
        Set<String> withChildren = findParentIdsHavingChildren(thesaurusId, parentIds, includeAllStatuses);
        if (withChildren.isEmpty()) {
            return rows;
        }
        return rows.stream()
                .map(row -> withChildren.contains(row.conceptId())
                        ? new ConceptTreeRow(row.conceptId(), row.notation(), row.label(), row.status(), true)
                        : row)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Set<String> findParentIdsHavingChildren(
            String thesaurusId,
            List<String> parentIds,
            boolean includeAllStatuses
    ) {
        String statusFilter = includeAllStatuses
                ? excludeRejectedCandidates("child")
                : "AND child.status != 'CA'";
        List<String> found = em.createNativeQuery("""
                SELECT DISTINCT parent_id
                FROM (
                    SELECT hr.id_concept1 AS parent_id
                    FROM hierarchical_relationship hr
                    JOIN concept child
                        ON child.id_concept = hr.id_concept2
                        AND child.id_thesaurus = hr.id_thesaurus
                    WHERE hr.id_thesaurus = :thesaurusId
                      AND hr.role LIKE 'NT%'
                      AND hr.id_concept1 IN (:parentIds)
                      """ + statusFilter + """
                    UNION
                    SELECT ta.id_concept_parent AS parent_id
                    FROM thesaurus_array ta
                    WHERE ta.id_thesaurus = :thesaurusId
                      AND ta.id_concept_parent IN (:parentIds)
                ) parents
                """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("parentIds", parentIds)
                .getResultList();
        Set<String> result = new HashSet<>(found.size());
        for (Object value : found) {
            if (value != null) {
                result.add(value.toString());
            }
        }
        return result;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private static String hasChildrenExpression() {
        return hasChildrenExpression(false);
    }

    private static String excludeRejectedCandidates(String conceptAlias) {
        return """
            AND NOT EXISTS (
                SELECT 1
                FROM candidat_status cs
                WHERE cs.id_concept = %1$s.id_concept
                  AND cs.id_thesaurus = %1$s.id_thesaurus
                  AND cs.id_status = %2$d
            )
            """.formatted(conceptAlias, CandidatStatusCode.REJECTED);
    }

    private static String hasChildrenExpression(boolean includeAllStatuses) {
        String statusFilter = includeAllStatuses
                ? excludeRejectedCandidates("child")
                : "AND child.status != 'CA'";
        return """
            (
                EXISTS(
                    SELECT 1
                    FROM hierarchical_relationship hr
                    JOIN concept child
                        ON child.id_concept = hr.id_concept2
                        AND child.id_thesaurus = hr.id_thesaurus
                    WHERE hr.id_concept1 = c.id_concept
                      AND hr.id_thesaurus = :thesaurusId
                      AND hr.role LIKE 'NT%'
                      """ + statusFilter + """
                )
                OR EXISTS(
                    SELECT 1
                    FROM thesaurus_array ta
                    WHERE ta.id_concept_parent = c.id_concept
                      AND ta.id_thesaurus = :thesaurusId
                )
            ) AS has_children
            """;
    }
}

