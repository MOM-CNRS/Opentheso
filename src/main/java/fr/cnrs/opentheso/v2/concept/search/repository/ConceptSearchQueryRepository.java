package fr.cnrs.opentheso.v2.concept.search.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ConceptSearchQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Object[]> searchStartWithPreferred(String value, String lang, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT id_concept, lexical_value, id_term, status
            FROM (
                SELECT pt.id_concept, t.lexical_value, t.id_term, c.status,
                       unaccent(lower(t.lexical_value)) AS sort_norm
                FROM preferred_term pt
                JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                JOIN concept c ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                WHERE c.status != 'CA'
                  AND pt.id_thesaurus = :thesaurusId
                  AND (:lang IS NULL OR t.lang = :lang)
                  AND unaccent(lower(t.lexical_value)) ~* ('(^|[^[:alpha:]])' || unaccent(lower(:value)))
            ) x
            ORDER BY x.sort_norm
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> searchStartWithSynonyms(String value, String lang, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT id_concept, id_term, npt, pt, status
            FROM (
                SELECT pt.id_concept, t.id_term, npt.lexical_value AS npt, t.lexical_value AS pt, c.status,
                       unaccent(lower(npt.lexical_value)) AS sort_norm
                FROM non_preferred_term npt
                JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus AND t.lang = npt.lang
                JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                WHERE c.status != 'CA'
                  AND npt.id_thesaurus = :thesaurusId
                  AND (:lang IS NULL OR npt.lang = :lang)
                  AND unaccent(lower(npt.lexical_value)) ~* ('(^|[^[:alpha:]])' || unaccent(lower(:value)))
            ) x
            ORDER BY x.sort_norm
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> searchExactPreferredTerms(String thesaurusId, String lang, String value) {
        return em.createNativeQuery("""
            SELECT pt.id_concept, t.lexical_value, t.id_term, c.status
            FROM term t
            JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN concept c ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            WHERE t.id_thesaurus = :thesaurusId
              AND (:lang IS NULL OR t.lang = :lang)
              AND c.status != 'CA'
              AND (' ' || lower(
                      unaccent(
                          translate(
                              t.lexical_value,
                              '[]()~-''"`.,;:!?',
                              '                    '
                          )
                      )
                  ) || ' ') LIKE '% ' || lower(unaccent(:value)) || ' %'
            ORDER BY unaccent(lower(t.lexical_value))
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("value", value)
                .getResultList();
    }

    public List<Object[]> searchExactAltTerms(String thesaurusId, String lang, String value) {
        return em.createNativeQuery("""
            SELECT pt.id_concept, t.id_term, npt.lexical_value AS alt_label, t.lexical_value AS pref_label, c.status
            FROM non_preferred_term npt
            JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
            JOIN term t ON t.id_term = pt.id_term AND t.lang = npt.lang AND t.id_thesaurus = pt.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE npt.id_thesaurus = :thesaurusId
              AND (' ' || lower(
                      unaccent(
                          translate(
                              npt.lexical_value,
                              '[]()~-''"`.,;:!?',
                              '                    '
                          )
                      )
                  ) || ' ') LIKE '% ' || lower(unaccent(:value)) || ' %'
              AND npt.lang = :lang
              AND c.status != 'CA'
            ORDER BY unaccent(lower(npt.lexical_value))
            LIMIT 50
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("value", value)
                .getResultList();
    }

    public List<Object[]> searchNotes(String value, String lang, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT c.id_concept, t.lexical_value, t.id_term, c.status
            FROM concept c
            JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN note n ON c.id_concept = n.identifier AND c.id_thesaurus = n.id_thesaurus AND n.lang = t.lang
            WHERE n.lang = :lang
              AND c.id_thesaurus = :thesaurusId
              AND (
                  unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT(:value, '%')))
                  OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('% ', :value, '%')))
                  OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%-', :value, '%')))
                  OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%_', :value, '%')))
                  OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%;', :value, '%')))
                  OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%:', :value, '%')))
                  OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%''', :value, '%')))
                  OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%"', :value, '%')))
              )
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> searchPreferredTermsFullText(
            String value,
            String lang,
            String thesaurusId,
            boolean langSensitive
    ) {
        return em.createNativeQuery("""
            SELECT pt.id_concept, t.id_term, t.lexical_value, c.status
            FROM term t
            JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE c.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
              AND t.lexical_value_norm ILIKE '%' || unaccent(lower(:value)) || '%'
              AND (:langSensitive = false OR t.lang = :lang)
            ORDER BY similarity(t.lexical_value_norm, unaccent(lower(:value))) DESC
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("langSensitive", langSensitive)
                .getResultList();
    }

    public List<Object[]> searchAltTermsFullText(
            String value,
            String lang,
            String thesaurusId,
            boolean langSensitive
    ) {
        return em.createNativeQuery("""
            SELECT pt.id_concept, t.id_term, npt.lexical_value, t.lexical_value, c.status
            FROM non_preferred_term npt
            JOIN term t ON t.id_term = npt.id_term AND t.lang = npt.lang AND t.id_thesaurus = npt.id_thesaurus
            JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE c.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
              AND npt.lexical_value_norm ILIKE '%' || unaccent(lower(:value)) || '%'
              AND (:langSensitive = false OR npt.lang = :lang)
            ORDER BY similarity(npt.lexical_value_norm, unaccent(lower(:value))) DESC
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("langSensitive", langSensitive)
                .getResultList();
    }

    public List<String> searchPreferredTermsFullTextIds(
            String value,
            String lang,
            String thesaurusId,
            boolean anonymous
    ) {
        return em.createNativeQuery("""
            SELECT grouped.id_concept
            FROM (
                SELECT pt.id_concept,
                       MAX(similarity(unaccent(lower(t.lexical_value)), unaccent(lower(:value)))) AS score
                FROM term t
                JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept
                LEFT JOIN concept_group cg ON cgc.idgroup = cg.idgroup
                WHERE c.id_thesaurus = :thesaurusId
                  AND (:lang IS NULL OR t.lang = :lang)
                  AND similarity(unaccent(lower(t.lexical_value)), unaccent(lower(:value))) > 0.2
                  AND c.status != 'CA'
                  AND (:anonymous = false OR cg.private IS NULL OR cg.private = false)
                GROUP BY pt.id_concept
            ) grouped
            ORDER BY grouped.score DESC
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("anonymous", anonymous)
                .getResultList();
    }

    public List<String> searchAltTermsFullTextIds(
            String value,
            String lang,
            String thesaurusId,
            boolean anonymous
    ) {
        return em.createNativeQuery("""
            SELECT grouped.id_concept
            FROM (
                SELECT pt.id_concept,
                       MAX(similarity(unaccent(lower(npt.lexical_value)), unaccent(lower(:value)))) AS score
                FROM non_preferred_term npt
                JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus AND npt.lang = t.lang
                JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept
                LEFT JOIN concept_group cg ON cgc.idgroup = cg.idgroup
                WHERE c.id_thesaurus = :thesaurusId
                  AND (:lang IS NULL OR t.lang = :lang)
                  AND similarity(unaccent(lower(npt.lexical_value)), unaccent(lower(:value))) > 0.2
                  AND c.status != 'CA'
                  AND (:anonymous = false OR cg.private IS NULL OR cg.private = false)
                GROUP BY pt.id_concept
            ) grouped
            ORDER BY grouped.score DESC
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("anonymous", anonymous)
                .getResultList();
    }

    public List<Object[]> searchCollectionsByPrefix(String value, String lang, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT idgroup, lexicalvalue
            FROM concept_group_label
            WHERE idthesaurus = :thesaurusId
              AND lang = :lang
              AND (
                unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT(:value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('% ', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%-', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%(', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%\\_', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%''', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%ʿ', :value, '%')))
              )
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> searchFacetsByPrefix(String value, String lang, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT DISTINCT id_facet, lexical_value
            FROM node_label
            WHERE id_thesaurus = :thesaurusId
              AND lang = :lang
              AND (
                unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT(:value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('% ', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%-', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%(', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%\\_', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%''', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%ʿ', :value, '%')))
              )
            LIMIT 50
            """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> searchConceptByAllIdPublic(String identifier, String lang, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT pt.id_concept, t.id_term, t.lexical_value, c.status
            FROM term t
            JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE t.lang = :lang
              AND c.id_thesaurus = :thesaurusId
              AND (c.id_concept = :identifier OR c.id_ark = :identifier OR c.id_handle = :identifier OR c.notation = :identifier)
              AND c.status != 'CA'
            """)
                .setParameter("identifier", identifier)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> searchConceptByAllIdPrivate(String identifier, String lang, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT pt.id_concept, t.id_term, t.lexical_value, c.status
            FROM term t
            JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept
            LEFT JOIN concept_group cg ON cgc.idgroup = cg.idgroup AND cgc.idthesaurus = cg.idthesaurus
            WHERE t.lang = :lang
              AND c.id_thesaurus = :thesaurusId
              AND (c.id_concept = :identifier OR c.id_ark = :identifier OR c.id_handle = :identifier OR c.notation = :identifier)
              AND c.status != 'CA'
            GROUP BY pt.id_concept, t.id_term, t.lexical_value, c.status, cg.private
            HAVING BOOL_OR(cg.private IS NULL OR cg.private = false)
            """)
                .setParameter("identifier", identifier)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public Optional<Object[]> findCollectionById(String identifier, String lang, String thesaurusId) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT cgl.idgroup, cgl.lexicalvalue
            FROM concept_group_label cgl
            WHERE cgl.idthesaurus = :thesaurusId
              AND cgl.lang = :lang
              AND LOWER(cgl.idgroup) = LOWER(:identifier)
            LIMIT 1
            """)
                .setParameter("identifier", identifier)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<Object[]> findFacetById(String identifier, String lang, String thesaurusId) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT nl.id_facet, nl.lexical_value
            FROM node_label nl
            WHERE nl.id_thesaurus = :thesaurusId
              AND nl.lang = :lang
              AND nl.id_facet = :identifier
            LIMIT 1
            """)
                .setParameter("identifier", identifier)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<String> searchForIds(String value, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT c.id_concept
            FROM concept c
            WHERE c.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
              AND lower(:value) IN (LOWER(c.id_concept), LOWER(c.id_ark), LOWER(c.id_handle), LOWER(c.notation))
            """)
                .setParameter("value", value)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<String> findConceptIdsFromNotes(String thesaurusId, String lang, String rawValue) {
        if (StringUtils.isBlank(rawValue)) {
            return Collections.emptyList();
        }
        String[] words = rawValue.trim().split("\\s+");
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT pt.id_concept
            FROM note n
            JOIN preferred_term pt ON pt.id_concept = n.identifier AND pt.id_thesaurus = n.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE n.id_thesaurus = :thesaurusId
              AND n.lang = :lang
              AND c.status != 'CA'
            """);
        List<String> params = new ArrayList<>();
        for (int i = 0; i < words.length && i < 4; i++) {
            sql.append(" AND f_unaccent(lower(n.lexicalvalue)) LIKE :word").append(i);
            params.add("%" + words[i] + "%");
        }
        sql.append(" LIMIT 50");
        var query = em.createNativeQuery(sql.toString())
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang);
        for (int i = 0; i < params.size(); i++) {
            query.setParameter("word" + i, params.get(i));
        }
        return query.getResultList();
    }

    public List<String> findDeprecatedConceptIds(String thesaurusId) {
        return em.createNativeQuery("""
            SELECT id_concept
            FROM concept
            WHERE LOWER(status) = LOWER('dep')
              AND id_thesaurus = :thesaurusId
            LIMIT 200
            """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<String> findPolyhierarchyConceptIds(String thesaurusId) {
        return em.createNativeQuery("""
            SELECT id_concept1
            FROM hierarchical_relationship
            WHERE role = 'BT'
              AND id_thesaurus = :thesaurusId
            GROUP BY id_concept1
            HAVING COUNT(id_concept1) > 1
            LIMIT 200
            """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<String> findMultiGroupConceptIds(String thesaurusId) {
        return em.createNativeQuery("""
            SELECT cgc.idconcept
            FROM concept_group_concept cgc
            WHERE cgc.idthesaurus = :thesaurusId
            GROUP BY cgc.idconcept
            HAVING COUNT(cgc.idconcept) > 1
            """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<String> findWithoutGroupConceptIds(String thesaurusId) {
        return em.createNativeQuery("""
            SELECT c.id_concept
            FROM concept c
            WHERE c.id_thesaurus = :thesaurusId
              AND c.status != 'CA'
              AND c.id_concept NOT IN (
                  SELECT idconcept FROM concept_group_concept WHERE idthesaurus = :thesaurusId
              )
            LIMIT 200
            """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<String> findDuplicateLabels(String thesaurusId, String lang) {
        List<String> duplicates = new ArrayList<>();
        duplicates.addAll(findDuplicatePreferredTerms(thesaurusId, lang));
        duplicates.addAll(findPreferredAndAltLabelDuplicates(thesaurusId, lang));
        duplicates.addAll(findDuplicateAltLabels(thesaurusId, lang));
        return duplicates.stream().distinct().toList();
    }

    public List<String> findForbiddenRelationshipConceptIds(String thesaurusId) {
        return em.createNativeQuery("""
            SELECT DISTINCT hr_nt_bt.id_concept1
            FROM hierarchical_relationship hr_nt_bt
            JOIN hierarchical_relationship hr_rt
              ON hr_rt.id_thesaurus = hr_nt_bt.id_thesaurus
             AND hr_rt.id_concept1 = hr_nt_bt.id_concept1
             AND hr_rt.id_concept2 = hr_nt_bt.id_concept2
             AND hr_rt.role = 'RT'
            WHERE hr_nt_bt.id_thesaurus = :thesaurusId
              AND hr_nt_bt.role IN ('NT', 'BT')
            LIMIT 200
            """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public Optional<String> findConceptIdFromLabel(String thesaurusId, String label, String lang) {
        List<String> rows = em.createNativeQuery("""
            SELECT c.id_concept
            FROM concept c
            JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            WHERE t.id_thesaurus = :thesaurusId
              AND t.lang = :lang
              AND LOWER(t.lexical_value) = LOWER(:label)
              AND c.status NOT IN ('DEP', 'dep', 'CA')
            LIMIT 1
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("label", label)
                .setParameter("lang", lang)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<String> findConceptIdFromAltLabel(String thesaurusId, String label, String lang) {
        List<String> rows = em.createNativeQuery("""
            SELECT c.id_concept
            FROM concept c
            JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            JOIN non_preferred_term npt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
            WHERE npt.id_thesaurus = :thesaurusId
              AND npt.lang = :lang
              AND LOWER(npt.lexical_value) = LOWER(:label)
              AND LOWER(c.status) != LOWER('dep')
            LIMIT 1
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("label", label)
                .setParameter("lang", lang)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<String> findConceptStatus(String conceptId, String thesaurusId) {
        List<String> rows = em.createNativeQuery("""
            SELECT c.status
            FROM concept c
            WHERE c.id_concept = :conceptId
              AND c.id_thesaurus = :thesaurusId
            LIMIT 1
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Object[]> findBroaderRelationsForSearch(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT hr.id_concept2, hr.role, COALESCE(t.lexical_value, hr.id_concept2), c.status
            FROM hierarchical_relationship hr
            JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND hr.role LIKE 'BT%'
              AND c.status != 'CA'
            ORDER BY t.lexical_value
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findRelatedRelationsForSearch(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT hr.id_concept2, hr.role, COALESCE(t.lexical_value, hr.id_concept2), c.status
            FROM hierarchical_relationship hr
            JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
            LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND hr.role LIKE 'RT%'
              AND c.status != 'CA'
            ORDER BY t.lexical_value
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findSynonymsForSearch(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT npt.lexical_value, npt.hiden
            FROM non_preferred_term npt
            JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
            WHERE pt.id_concept = :conceptId
              AND npt.id_thesaurus = :thesaurusId
              AND npt.lang = :lang
              AND npt.hiden = false
            ORDER BY npt.lexical_value
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public Map<String, String> findStatusByIds(List<String> conceptIds, String thesaurusId) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return Map.of();
        }
        return chunkedMapQuery(conceptIds, chunk -> {
            List<Object[]> rows = em.createNativeQuery("""
                SELECT c.id_concept, c.status
                FROM concept c
                WHERE c.id_thesaurus = :thesaurusId
                  AND c.id_concept IN (:conceptIds)
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("conceptIds", chunk)
                    .getResultList();
            Map<String, String> result = new LinkedHashMap<>();
            for (Object[] row : rows) {
                if (row[0] != null) result.put(row[0].toString(), row[1] != null ? row[1].toString() : null);
            }
            return result;
        });
    }

    public Map<String, String> findPreferredLabelsByIds(List<String> conceptIds, String thesaurusId, String lang) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return Map.of();
        }
        return chunkedMapQuery(conceptIds, chunk -> {
            List<Object[]> rows = em.createNativeQuery("""
                SELECT pt.id_concept, t.lexical_value
                FROM preferred_term pt
                JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                WHERE pt.id_thesaurus = :thesaurusId
                  AND pt.id_concept IN (:conceptIds)
                  AND t.lang = :lang
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("conceptIds", chunk)
                    .setParameter("lang", lang)
                    .getResultList();
            Map<String, String> result = new LinkedHashMap<>();
            for (Object[] row : rows) {
                if (row[0] != null) result.put(row[0].toString(), row[1] != null ? row[1].toString() : "");
            }
            return result;
        });
    }

    public Map<String, List<String>> findSynonymsByIds(List<String> conceptIds, String thesaurusId, String lang) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return Map.of();
        }
        return chunkedMapQuery(conceptIds, chunk -> {
            List<Object[]> rows = em.createNativeQuery("""
                SELECT pt.id_concept, npt.lexical_value
                FROM non_preferred_term npt
                JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                WHERE npt.id_thesaurus = :thesaurusId
                  AND npt.lang = :lang
                  AND npt.hiden = false
                  AND pt.id_concept IN (:conceptIds)
                ORDER BY npt.lexical_value
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("conceptIds", chunk)
                    .setParameter("lang", lang)
                    .getResultList();
            return groupStringValues(rows);
        });
    }

    public Map<String, List<String>> findBroadersByIds(List<String> conceptIds, String thesaurusId, String lang) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return Map.of();
        }
        return chunkedMapQuery(conceptIds, chunk -> {
            List<Object[]> rows = em.createNativeQuery("""
                SELECT hr.id_concept1, COALESCE(t.lexical_value, hr.id_concept2)
                FROM hierarchical_relationship hr
                JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
                LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 IN (:conceptIds)
                  AND hr.role LIKE 'BT%'
                  AND c.status != 'CA'
                ORDER BY t.lexical_value
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("conceptIds", chunk)
                    .setParameter("lang", lang)
                    .getResultList();
            return groupStringValues(rows);
        });
    }

    public Map<String, List<String>> findRelatedsByIds(List<String> conceptIds, String thesaurusId, String lang) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return Map.of();
        }
        return chunkedMapQuery(conceptIds, chunk -> {
            List<Object[]> rows = em.createNativeQuery("""
                SELECT hr.id_concept1, COALESCE(t.lexical_value, hr.id_concept2)
                FROM hierarchical_relationship hr
                JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
                LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 IN (:conceptIds)
                  AND hr.role LIKE 'RT%'
                  AND c.status != 'CA'
                ORDER BY t.lexical_value
                """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("conceptIds", chunk)
                    .setParameter("lang", lang)
                    .getResultList();
            return groupStringValues(rows);
        });
    }



    private List<String> findDuplicatePreferredTerms(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT lower(lexical_value)
            FROM term
            WHERE id_thesaurus = :thesaurusId AND lang = :lang
            GROUP BY lower(lexical_value)
            HAVING COUNT(*) > 1
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    private List<String> findPreferredAndAltLabelDuplicates(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT lower(t.lexical_value)
            FROM term t, non_preferred_term npt
            WHERE t.id_thesaurus = npt.id_thesaurus
              AND t.lang = npt.lang
              AND lower(t.lexical_value) = lower(npt.lexical_value)
              AND t.id_thesaurus = :thesaurusId
              AND t.lang = :lang
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Splits ids into chunks of at most CHUNK_SIZE to avoid exceeding PostgreSQL's
     * IN-list limit (~32k parameters) on large result sets.
     * Merges the Maps returned by the per-chunk query supplier.
     */
    private static final int CHUNK_SIZE = 500;

    private <V> Map<String, V> chunkedMapQuery(
            List<String> ids,
            java.util.function.Function<List<String>, Map<String, V>> queryFn
    ) {
        if (ids.size() <= CHUNK_SIZE) {
            return queryFn.apply(ids);
        }
        Map<String, V> merged = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i += CHUNK_SIZE) {
            List<String> chunk = ids.subList(i, Math.min(i + CHUNK_SIZE, ids.size()));
            merged.putAll(queryFn.apply(chunk));
        }
        return merged;
    }

    private Map<String, List<String>> groupStringValues(List<Object[]> rows) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                map.computeIfAbsent(row[0].toString(), k -> new ArrayList<>())
                   .add(row[1] != null ? row[1].toString() : "");
            }
        }
        return map;
    }

    private List<String> findDuplicateAltLabels(String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT lower(lexical_value)
            FROM non_preferred_term
            WHERE id_thesaurus = :thesaurusId AND lang = :lang
            GROUP BY lower(lexical_value)
            HAVING COUNT(*) > 1
            """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }
}
