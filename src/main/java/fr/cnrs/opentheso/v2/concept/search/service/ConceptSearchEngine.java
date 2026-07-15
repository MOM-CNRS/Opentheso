package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptSearchEngine {

    private final ConceptSearchQueryRepository conceptSearchQueryRepository;

    public List<ConceptSearchSuggestion> autocomplete(
            String query,
            ConceptSearchMode mode,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        if (StringUtils.isBlank(query) || StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        String normalizedQuery = normalizeQuery(query);
        return switch (mode) {
            case EXACT -> searchExactMatch(normalizedQuery, lang, thesaurusId);
            case START_WITH -> searchStartWith(normalizedQuery, lang, thesaurusId);
            case NOTE -> searchNotes(normalizedQuery, lang, thesaurusId);
            case IDENTIFIER -> searchByAllId(normalizedQuery, lang, thesaurusId, anonymous);
            case FULL_TEXT -> searchFullText(normalizedQuery, lang, thesaurusId, anonymous);
        };
    }

    public List<String> findConceptIds(
            String query,
            ConceptSearchMode mode,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        String normalizedQuery = StringUtils.defaultString(query).trim();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        switch (mode) {
            case IDENTIFIER -> ids.addAll(conceptSearchQueryRepository.searchForIds(normalizedQuery, thesaurusId));
            case NOTE -> ids.addAll(conceptSearchQueryRepository.findConceptIdsFromNotes(
                    thesaurusId, lang, normalizeQuery(normalizedQuery)
            ));
            case EXACT -> autocomplete(normalizedQuery, ConceptSearchMode.EXACT, thesaurusId, lang, anonymous)
                    .forEach(item -> ids.add(item.conceptId()));
            case START_WITH -> autocomplete(normalizedQuery, ConceptSearchMode.START_WITH, thesaurusId, lang, anonymous)
                    .forEach(item -> ids.add(item.conceptId()));
            case FULL_TEXT -> ids.addAll(searchFullTextIds(normalizedQuery, lang, thesaurusId, anonymous));
            default -> {
            }
        }
        return new ArrayList<>(ids);
    }

    private List<ConceptSearchSuggestion> searchStartWith(String value, String lang, String thesaurusId) {
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchStartWithPreferred(value, lang, thesaurusId)) {
            addPreferredResult(results, row, value);
        }
        for (Object[] row : conceptSearchQueryRepository.searchStartWithSynonyms(value, lang, thesaurusId)) {
            addAltResult(results, row, value);
        }
        results.addAll(searchCollections(thesaurusId, value, lang));
        results.addAll(searchFacets(thesaurusId, value, lang));
        return results;
    }

    private List<ConceptSearchSuggestion> searchExactMatch(String value, String lang, String thesaurusId) {
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchExactPreferredTerms(thesaurusId, lang, value)) {
            addPreferredResult(results, row, value);
        }
        for (Object[] row : conceptSearchQueryRepository.searchExactAltTerms(thesaurusId, lang, value)) {
            addAltResult(results, row, value);
        }
        results.addAll(searchCollections(thesaurusId, value, lang));
        results.addAll(searchFacets(thesaurusId, value, lang));
        return results;
    }

    private List<ConceptSearchSuggestion> searchNotes(String value, String lang, String thesaurusId) {
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchNotes(value, lang, thesaurusId)) {
            results.add(conceptSuggestion(
                    stringAt(row, 0),
                    stringAt(row, 1),
                    "",
                    ConceptSearchKind.CONCEPT,
                    false
            ));
        }
        return results;
    }

    private List<ConceptSearchSuggestion> searchFullText(String value, String lang, String thesaurusId, boolean anonymous) {
        boolean langSensitive = StringUtils.isNotBlank(lang);
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchPreferredTermsFullText(value, lang, thesaurusId, langSensitive)) {
            var suggestion = conceptSuggestion(
                    stringAt(row, 0),
                    stringAt(row, 2),
                    "",
                    ConceptSearchKind.CONCEPT,
                    false
            );
            if (value.equalsIgnoreCase(suggestion.preferredLabel())) {
                results.add(0, suggestion);
            } else {
                results.add(suggestion);
            }
        }
        for (Object[] row : conceptSearchQueryRepository.searchAltTermsFullText(value, lang, thesaurusId, langSensitive)) {
            var suggestion = conceptSuggestion(
                    stringAt(row, 0),
                    stringAt(row, 3),
                    stringAt(row, 2),
                    ConceptSearchKind.ALT_LABEL,
                    "DEP".equalsIgnoreCase(stringAt(row, 4))
            );
            if (value.equalsIgnoreCase(suggestion.altLabel())) {
                if (results.isEmpty()) {
                    results.add(0, suggestion);
                } else if (results.get(0).preferredLabel().equalsIgnoreCase(value)) {
                    results.add(1, suggestion);
                } else {
                    results.add(0, suggestion);
                }
            } else {
                results.add(suggestion);
            }
        }
        results.addAll(searchCollections(thesaurusId, value, lang));
        results.addAll(searchFacets(thesaurusId, value, lang));
        return results;
    }

    private List<String> searchFullTextIds(String value, String lang, String thesaurusId, boolean anonymous) {
        String normalized = normalizeQuery(value);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.addAll(conceptSearchQueryRepository.searchPreferredTermsFullTextIds(normalized, lang, thesaurusId, anonymous));
        ids.addAll(conceptSearchQueryRepository.searchAltTermsFullTextIds(normalized, lang, thesaurusId, anonymous));
        return new ArrayList<>(ids);
    }

    private List<ConceptSearchSuggestion> searchByAllId(String identifier, String lang, String thesaurusId, boolean anonymous) {
        if (StringUtils.isBlank(identifier)) {
            return Collections.emptyList();
        }
        List<Object[]> rows = anonymous
                ? conceptSearchQueryRepository.searchConceptByAllIdPrivate(identifier, lang, thesaurusId)
                : conceptSearchQueryRepository.searchConceptByAllIdPublic(identifier, lang, thesaurusId);
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(conceptSuggestion(
                    stringAt(row, 0),
                    stringAt(row, 2),
                    "",
                    ConceptSearchKind.CONCEPT,
                    false
            ));
        }
        conceptSearchQueryRepository.findCollectionById(identifier, lang, thesaurusId).ifPresent(row -> {
            results.add(groupSuggestion(stringAt(row, 0), stringAt(row, 1)));
        });
        conceptSearchQueryRepository.findFacetById(identifier, lang, thesaurusId).ifPresent(row -> {
            results.add(facetSuggestion(stringAt(row, 0), stringAt(row, 1)));
        });
        return results;
    }

    private List<ConceptSearchSuggestion> searchCollections(String thesaurusId, String value, String lang) {
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchCollectionsByPrefix(value, lang, thesaurusId)) {
            results.add(groupSuggestion(stringAt(row, 0), stringAt(row, 1)));
        }
        return results;
    }

    private List<ConceptSearchSuggestion> searchFacets(String thesaurusId, String value, String lang) {
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchFacetsByPrefix(value, lang, thesaurusId)) {
            results.add(facetSuggestion(stringAt(row, 0), stringAt(row, 1)));
        }
        return results;
    }

    private static void addPreferredResult(List<ConceptSearchSuggestion> results, Object[] row, String value) {
        var suggestion = conceptSuggestion(
                stringAt(row, 0),
                stringAt(row, 1),
                "",
                ConceptSearchKind.CONCEPT,
                "DEP".equalsIgnoreCase(stringAt(row, 3))
        );
        if (value.equalsIgnoreCase(suggestion.preferredLabel())) {
            results.add(0, suggestion);
        } else {
            results.add(suggestion);
        }
    }

    private static void addAltResult(List<ConceptSearchSuggestion> results, Object[] row, String value) {
        var suggestion = conceptSuggestion(
                stringAt(row, 0),
                stringAt(row, 3),
                stringAt(row, 2),
                ConceptSearchKind.ALT_LABEL,
                "DEP".equalsIgnoreCase(stringAt(row, 4))
        );
        if (value.equalsIgnoreCase(suggestion.altLabel())) {
            results.add(0, suggestion);
        } else {
            results.add(suggestion);
        }
    }

    private static ConceptSearchSuggestion conceptSuggestion(
            String conceptId,
            String preferredLabel,
            String altLabel,
            ConceptSearchKind kind,
            boolean deprecated
    ) {
        return new ConceptSearchSuggestion(
                conceptId,
                StringUtils.defaultString(preferredLabel),
                StringUtils.defaultString(altLabel),
                kind,
                deprecated
        );
    }

    private static ConceptSearchSuggestion groupSuggestion(String conceptId, String label) {
        return new ConceptSearchSuggestion(
                conceptId + "####isGroup",
                StringUtils.defaultString(label),
                "",
                ConceptSearchKind.GROUP,
                false
        );
    }

    private static ConceptSearchSuggestion facetSuggestion(String conceptId, String label) {
        return new ConceptSearchSuggestion(
                conceptId + "####isFacet",
                StringUtils.defaultString(label),
                "",
                ConceptSearchKind.FACET,
                false
        );
    }

    private static String normalizeQuery(String query) {
        return fr.cnrs.opentheso.utils.StringUtils.convertString(query).trim();
    }

    private static String stringAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }
}
