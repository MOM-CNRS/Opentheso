package fr.cnrs.opentheso.concept.search.service;

import fr.cnrs.opentheso.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.concept.search.model.ParsedSearchQuery;
import fr.cnrs.opentheso.concept.search.model.SearchClause;
import fr.cnrs.opentheso.concept.search.repository.ConceptSearchQueryRepository;
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

    private static final List<ConceptSearchMode> LEAD_ORDER = List.of(
            ConceptSearchMode.FULL_TEXT,
            ConceptSearchMode.EXACT,
            ConceptSearchMode.START_WITH,
            ConceptSearchMode.PREFERRED,
            ConceptSearchMode.SYNONYM,
            ConceptSearchMode.NOTE,
            ConceptSearchMode.IDENTIFIER,
            ConceptSearchMode.NOTATION
    );

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
        ParsedSearchQuery parsed = SearchQueryParser.parse(query);
        if (parsed.hasOperators()) {
            return autocompleteCombined(parsed, thesaurusId, lang, anonymous);
        }
        String normalizedQuery = normalizeQuery(query);
        ConceptSearchMode active = mode == null ? ConceptSearchMode.FULL_TEXT : mode;
        return autocompleteClause(new SearchClause(active, normalizedQuery, ""), thesaurusId, lang, anonymous);
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
        ParsedSearchQuery parsed = SearchQueryParser.parse(query);
        if (parsed.hasOperators()) {
            return findCombinedIds(parsed, thesaurusId, lang, anonymous);
        }
        String normalizedQuery = StringUtils.defaultString(query).trim();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ConceptSearchMode active = mode == null ? ConceptSearchMode.FULL_TEXT : mode;
        switch (active) {
            case IDENTIFIER -> ids.addAll(conceptSearchQueryRepository.searchForIds(normalizedQuery, thesaurusId));
            case NOTE -> ids.addAll(conceptSearchQueryRepository.findConceptIdsFromNotes(
                    thesaurusId, lang, normalizeQuery(normalizedQuery)
            ));
            case NOTATION -> ids.addAll(conceptSearchQueryRepository.findConceptIdsByNotation(
                    normalizeQuery(normalizedQuery), thesaurusId
            ));
            case PREFERRED -> ids.addAll(conceptSearchQueryRepository.searchPreferredTermsFullTextIds(
                    normalizeQuery(normalizedQuery), lang, thesaurusId, anonymous
            ));
            case SYNONYM -> ids.addAll(conceptSearchQueryRepository.searchAltTermsFullTextIds(
                    normalizeQuery(normalizedQuery), lang, thesaurusId, anonymous
            ));
            case EXACT -> autocomplete(normalizedQuery, ConceptSearchMode.EXACT, thesaurusId, lang, anonymous)
                    .forEach(item -> addConceptId(ids, item));
            case START_WITH -> autocomplete(normalizedQuery, ConceptSearchMode.START_WITH, thesaurusId, lang, anonymous)
                    .forEach(item -> addConceptId(ids, item));
            case FULL_TEXT -> ids.addAll(searchFullTextIds(normalizedQuery, lang, thesaurusId, anonymous));
        }
        return new ArrayList<>(ids);
    }

    private List<ConceptSearchSuggestion> autocompleteCombined(
            ParsedSearchQuery parsed,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        if (parsed.isBlank()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> allowed = intersectIds(parsed.clauses(), thesaurusId, lang, anonymous);
        if (allowed.isEmpty()) {
            return Collections.emptyList();
        }
        SearchClause lead = leadClause(parsed.clauses());
        List<ConceptSearchSuggestion> ranked = autocompleteClause(lead, thesaurusId, lang, anonymous);
        List<ConceptSearchSuggestion> filtered = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ConceptSearchSuggestion suggestion : ranked) {
            if (suggestion.isGroup() || suggestion.isFacet()) {
                continue;
            }
            String id = suggestion.conceptId();
            if (allowed.contains(id) && seen.add(id)) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

    private List<String> findCombinedIds(
            ParsedSearchQuery parsed,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        if (parsed.isBlank()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(intersectIds(parsed.clauses(), thesaurusId, lang, anonymous));
    }

    private LinkedHashSet<String> intersectIds(
            List<SearchClause> clauses,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        LinkedHashSet<String> allowed = null;
        for (SearchClause clause : clauses) {
            List<String> ids = findIdsForClause(clause, thesaurusId, lang, anonymous);
            if (allowed == null) {
                allowed = new LinkedHashSet<>(ids);
            } else {
                allowed.retainAll(ids);
            }
            if (allowed.isEmpty()) {
                return allowed;
            }
        }
        return allowed == null ? new LinkedHashSet<>() : allowed;
    }

    private List<String> findIdsForClause(
            SearchClause clause,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        String value = normalizeQuery(clause.value());
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        switch (clause.mode()) {
            case IDENTIFIER -> ids.addAll(conceptSearchQueryRepository.searchForIds(value, thesaurusId));
            case NOTE -> ids.addAll(conceptSearchQueryRepository.findConceptIdsFromNotes(thesaurusId, lang, value));
            case NOTATION -> ids.addAll(conceptSearchQueryRepository.findConceptIdsByNotation(value, thesaurusId));
            case PREFERRED -> ids.addAll(conceptSearchQueryRepository.searchPreferredTermsFullTextIds(
                    value, lang, thesaurusId, anonymous
            ));
            case SYNONYM -> ids.addAll(conceptSearchQueryRepository.searchAltTermsFullTextIds(
                    value, lang, thesaurusId, anonymous
            ));
            case FULL_TEXT -> ids.addAll(searchFullTextIds(value, lang, thesaurusId, anonymous));
            case EXACT, START_WITH -> autocompleteClause(clause, thesaurusId, lang, anonymous)
                    .forEach(item -> addConceptId(ids, item));
        }
        return new ArrayList<>(ids);
    }

    private List<ConceptSearchSuggestion> autocompleteClause(
            SearchClause clause,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        String value = normalizeQuery(clause.value());
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return switch (clause.mode()) {
            case EXACT -> searchExactMatch(value, lang, thesaurusId);
            case START_WITH -> searchStartWith(value, lang, thesaurusId);
            case NOTE -> searchNotes(value, lang, thesaurusId);
            case IDENTIFIER -> searchByAllId(value, lang, thesaurusId, anonymous);
            case FULL_TEXT -> searchFullText(value, lang, thesaurusId);
            case PREFERRED -> searchPreferred(value, lang, thesaurusId);
            case SYNONYM -> searchSynonyms(value, lang, thesaurusId);
            case NOTATION -> searchNotation(value, lang, thesaurusId);
        };
    }

    private static SearchClause leadClause(List<SearchClause> clauses) {
        for (ConceptSearchMode mode : LEAD_ORDER) {
            for (SearchClause clause : clauses) {
                if (clause.mode() == mode) {
                    return clause;
                }
            }
        }
        return clauses.get(0);
    }

    private static void addConceptId(LinkedHashSet<String> ids, ConceptSearchSuggestion item) {
        if (item == null || item.isGroup() || item.isFacet() || StringUtils.isBlank(item.conceptId())) {
            return;
        }
        ids.add(item.conceptId());
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

    private List<ConceptSearchSuggestion> searchPreferred(String value, String lang, String thesaurusId) {
        boolean langSensitive = StringUtils.isNotBlank(lang);
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchPreferredTermsFullText(value, lang, thesaurusId, langSensitive)) {
            addFullTextPreferred(results, row, value);
        }
        return results;
    }

    private List<ConceptSearchSuggestion> searchSynonyms(String value, String lang, String thesaurusId) {
        boolean langSensitive = StringUtils.isNotBlank(lang);
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchAltTermsFullText(value, lang, thesaurusId, langSensitive)) {
            addFullTextAlt(results, row, value);
        }
        return results;
    }

    private List<ConceptSearchSuggestion> searchNotation(String value, String lang, String thesaurusId) {
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchByNotation(value, lang, thesaurusId)) {
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

    private List<ConceptSearchSuggestion> searchFullText(String value, String lang, String thesaurusId) {
        boolean langSensitive = StringUtils.isNotBlank(lang);
        List<ConceptSearchSuggestion> results = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.searchPreferredTermsFullText(value, lang, thesaurusId, langSensitive)) {
            addFullTextPreferred(results, row, value);
        }
        for (Object[] row : conceptSearchQueryRepository.searchAltTermsFullText(value, lang, thesaurusId, langSensitive)) {
            addFullTextAlt(results, row, value);
        }
        results.addAll(searchCollections(thesaurusId, value, lang));
        results.addAll(searchFacets(thesaurusId, value, lang));
        return results;
    }

    private void addFullTextPreferred(List<ConceptSearchSuggestion> results, Object[] row, String value) {
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

    private void addFullTextAlt(List<ConceptSearchSuggestion> results, Object[] row, String value) {
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
        conceptSearchQueryRepository.findCollectionById(identifier, lang, thesaurusId)
                .ifPresent(row -> results.add(groupSuggestion(stringAt(row, 0), stringAt(row, 1))));
        conceptSearchQueryRepository.findFacetById(identifier, lang, thesaurusId)
                .ifPresent(row -> results.add(facetSuggestion(stringAt(row, 0), stringAt(row, 1))));
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
