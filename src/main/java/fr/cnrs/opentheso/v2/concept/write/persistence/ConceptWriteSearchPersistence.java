package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCustomTarget;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteFacet;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConceptWriteSearchPersistence {

    private static final int SEARCH_LIMIT = 40;

    private final ConceptQueryRepository conceptQueryRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<ConceptSearchSuggestion> autocompleteRelationTarget(
            String query,
            String lang,
            String thesaurusId,
            boolean includeDeprecated
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang, query)) {
            return Collections.emptyList();
        }
        if (includeDeprecated) {
            return searchConceptsIncludingDeprecated(thesaurusId, lang, query.trim()).stream()
                    .map(row -> new ConceptSearchSuggestion(
                            ConceptMapper.stringAt(row, 0),
                            ConceptMapper.stringAt(row, 1),
                            "",
                            false
                    ))
                    .toList();
        }
        return conceptQueryRepository.searchByLabel(thesaurusId, lang, query.trim(), SEARCH_LIMIT).stream()
                .map(row -> new ConceptSearchSuggestion(
                        row.conceptId(),
                        row.label(),
                        "",
                        false
                ))
                .toList();
    }

    public List<ConceptSearchSuggestion> autocompleteReplacedByTarget(
            String query,
            String lang,
            String thesaurusId
    ) {
        return autocompleteRelationTarget(query, lang, thesaurusId, false);
    }

    public List<ConceptWriteCollection> autocompleteCollection(
            String query,
            String lang,
            String thesaurusId
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        String cleaned = StringUtils.defaultString(query).trim();
        return conceptGroupLabelRepository.searchGroups(thesaurusId, lang, cleaned).stream()
                .map(row -> new ConceptWriteCollection(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 1)
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ConceptWriteFacet> autocompleteFacet(
            String query,
            String lang,
            String thesaurusId
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        String cleaned = StringUtils.defaultString(query).trim();
        if (cleaned.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT DISTINCT nl.id_facet, nl.lexical_value
                        FROM node_label nl
                        WHERE nl.id_thesaurus = :thesaurusId
                          AND nl.lang = :lang
                          AND f_unaccent(LOWER(nl.lexical_value)) LIKE f_unaccent(LOWER(:query))
                        ORDER BY nl.lexical_value
                        LIMIT :limit
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("query", cleaned + "%")
                .setParameter("limit", SEARCH_LIMIT)
                .getResultList();
        return mapFacetRows(rows);
    }

    @SuppressWarnings("unchecked")
    public List<ConceptWriteFacet> listFacets(String lang, String thesaurusId) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT DISTINCT nl.id_facet, nl.lexical_value
                        FROM node_label nl
                        WHERE nl.id_thesaurus = :thesaurusId
                          AND nl.lang = :lang
                        ORDER BY nl.lexical_value
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return mapFacetRows(rows);
    }

    @SuppressWarnings("unchecked")
    public List<ConceptWriteCustomTarget> autocompleteCustomRelationTarget(
            String query,
            String lang,
            String thesaurusId
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang, query)) {
            return Collections.emptyList();
        }
        String cleaned = query.trim();
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT DISTINCT c.id_concept, t.lexical_value AS label, c.concept_type
                        FROM concept c
                        JOIN preferred_term pt
                            ON pt.id_concept = c.id_concept
                           AND pt.id_thesaurus = c.id_thesaurus
                        JOIN term t
                            ON t.id_term = pt.id_term
                           AND t.id_thesaurus = c.id_thesaurus
                        WHERE c.id_thesaurus = :thesaurusId
                          AND t.lang = :lang
                          AND c.status NOT IN ('CA', 'hidden')
                          AND c.concept_type IS NOT NULL
                          AND c.concept_type != ''
                          AND c.concept_type != 'concept'
                          AND f_unaccent(LOWER(t.lexical_value)) LIKE f_unaccent(LOWER(:query))
                        ORDER BY label
                        LIMIT :limit
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("query", "%" + cleaned + "%")
                .setParameter("limit", SEARCH_LIMIT)
                .getResultList();
        return rows.stream()
                .map(row -> new ConceptWriteCustomTarget(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 1),
                        ConceptMapper.stringAt(row, 2)
                ))
                .toList();
    }

    private static List<ConceptWriteFacet> mapFacetRows(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new ConceptWriteFacet(
                        ConceptMapper.stringAt(row, 0),
                        ConceptMapper.stringAt(row, 1)
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> searchConceptsIncludingDeprecated(String thesaurusId, String lang, String query) {
        return entityManager.createNativeQuery("""
                        SELECT DISTINCT c.id_concept, t.lexical_value AS label
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
                        ORDER BY label
                        LIMIT :limit
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("query", "%" + query + "%")
                .setParameter("limit", SEARCH_LIMIT)
                .getResultList();
    }
}
