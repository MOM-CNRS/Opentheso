package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.model.ThesaurusBarSearchHit;
import fr.cnrs.opentheso.v2.concept.search.model.ThesaurusBarSearchResponse;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.stats.service.StatEventService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThesaurusBarSearchService {

    public static final int SUGGEST_LIMIT = 12;
    public static final int DEFAULT_PAGE = 24;
    public static final int MAX_PAGE = 80;

    private final ConceptSearchService conceptSearchService;
    private final ConceptSearchHydrationService conceptSearchHydrationService;
    private final AuthenticatedUserSource authenticatedUserSource;
    private final StatEventService statEventService;

    @Transactional(readOnly = true)
    public ThesaurusBarSearchResponse suggest(String thesaurusId, String lang, String query, ConceptSearchMode mode) {
        ThesaurusBarSearchResponse response = page(thesaurusId, lang, query, mode, 0, SUGGEST_LIMIT);
        if (response.total() == 0 && StringUtils.length(response.query()) >= 3) {
            statEventService.logSearchNoResult(response.query(), 0, thesaurusId, null, resolveLang(lang));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ThesaurusBarSearchResponse search(
            String thesaurusId,
            String lang,
            String query,
            ConceptSearchMode mode,
            int offset,
            int limit
    ) {
        ThesaurusBarSearchResponse response = page(thesaurusId, lang, query, mode, offset, limit);
        if (offset == 0 && StringUtils.isNotBlank(response.query())) {
            if (response.total() == 0) {
                statEventService.logSearchNoResult(
                        response.query(), 0, thesaurusId, null, resolveLang(lang));
            } else {
                statEventService.logSearchApplied(
                        response.query(), response.total(), thesaurusId, null, resolveLang(lang));
            }
        }
        return response;
    }

    public void logSuggestionSelected(String thesaurusId, String lang, String query, String selectedLabel) {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(query) || StringUtils.isBlank(selectedLabel)) {
            return;
        }
        statEventService.logSearchResultSelected(
                query.trim(), selectedLabel.trim(), thesaurusId, null, resolveLang(lang));
    }

    @Transactional(readOnly = true)
    public ThesaurusBarSearchResponse programmed(String thesaurusId, String lang, String kind, int offset, int limit) {
        if (!authenticatedUserSource.isLoggedIn() || StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(kind)) {
            return ThesaurusBarSearchResponse.empty("", "PROGRAMMED");
        }
        String resolved = resolveLang(lang);
        List<ConceptSearchResult> results = switch (kind.trim().toLowerCase(Locale.ROOT)) {
            case "deprecated" -> conceptSearchService.searchDeprecated(thesaurusId, resolved);
            case "polyhierarchy" -> conceptSearchService.searchPolyhierarchy(thesaurusId, resolved);
            case "multi-groups" -> conceptSearchService.searchMultiGroups(thesaurusId, resolved);
            case "without-groups" -> conceptSearchService.searchWithoutGroups(thesaurusId, resolved);
            case "duplicates" -> conceptSearchService.searchDuplicates(thesaurusId, resolved);
            case "forbidden" -> conceptSearchService.searchForbiddenRelationships(thesaurusId, resolved);
            default -> List.of();
        };
        int from = Math.max(0, offset);
        int size = clamp(limit, DEFAULT_PAGE);
        int to = Math.min(results.size(), from + size);
        List<ThesaurusBarSearchHit> hits = new ArrayList<>();
        if (from < results.size()) {
            for (ConceptSearchResult result : results.subList(from, to)) {
                hits.add(fromResult(result));
            }
        }
        return new ThesaurusBarSearchResponse("", "PROGRAMMED", kind.trim().toLowerCase(Locale.ROOT), results.size(), hits);
    }

    private ThesaurusBarSearchResponse page(
            String thesaurusId,
            String lang,
            String query,
            ConceptSearchMode mode,
            int offset,
            int limit
    ) {
        ConceptSearchMode active = mode == null ? ConceptSearchMode.FULL_TEXT : mode;
        String trimmed = StringUtils.trimToEmpty(query);
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(trimmed)) {
            return ThesaurusBarSearchResponse.empty(trimmed, active.name());
        }
        String resolved = resolveLang(lang);
        List<ConceptSearchSuggestion> all = conceptSearchService.autocomplete(
                trimmed,
                active,
                thesaurusId,
                resolved,
                !authenticatedUserSource.isLoggedIn()
        );
        int from = Math.max(0, offset);
        int size = clamp(limit, DEFAULT_PAGE);
        int to = Math.min(all.size(), from + size);
        List<ConceptSearchSuggestion> window = from >= all.size() ? List.of() : all.subList(from, to);
        Map<String, ConceptSearchResult> hydrated = hydrate(window, thesaurusId, resolved);
        List<ThesaurusBarSearchHit> hits = new ArrayList<>(window.size());
        for (ConceptSearchSuggestion suggestion : window) {
            hits.add(fromSuggestion(suggestion, hydrated.get(suggestion.conceptId()), active));
        }
        return new ThesaurusBarSearchResponse(trimmed, active.name(), "", all.size(), hits);
    }

    private Map<String, ConceptSearchResult> hydrate(
            List<ConceptSearchSuggestion> suggestions,
            String thesaurusId,
            String lang
    ) {
        if (StringUtils.isBlank(lang) || suggestions.isEmpty()) {
            return Map.of();
        }
        List<String> ids = suggestions.stream()
                .filter(item -> item.isConcept() || item.isAltLabelMatch())
                .map(ConceptSearchSuggestion::conceptId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, ConceptSearchResult> byId = new LinkedHashMap<>();
        for (ConceptSearchResult result : conceptSearchHydrationService.hydrateAll(ids, thesaurusId, lang)) {
            if (result != null && StringUtils.isNotBlank(result.conceptId())) {
                byId.putIfAbsent(result.conceptId(), result);
            }
        }
        return byId;
    }

    private static ThesaurusBarSearchHit fromSuggestion(
            ConceptSearchSuggestion suggestion,
            ConceptSearchResult hydrated,
            ConceptSearchMode mode
    ) {
        String label = StringUtils.defaultIfBlank(
                suggestion.preferredLabel(),
                hydrated == null ? "" : hydrated.preferredLabel()
        );
        String via = "";
        if (mode == ConceptSearchMode.NOTE) {
            via = "note";
        } else if (suggestion.isAltLabelMatch()) {
            via = StringUtils.defaultString(suggestion.altLabel());
        }
        boolean deprecated = suggestion.deprecated() || (hydrated != null && hydrated.deprecated());
        String path = hydrated == null ? "" : joinPath(hydrated.broaderTerms());
        return new ThesaurusBarSearchHit(suggestion.conceptId(), label, kindOf(suggestion), via, deprecated, path);
    }

    private static ThesaurusBarSearchHit fromResult(ConceptSearchResult result) {
        return new ThesaurusBarSearchHit(
                result.conceptId(),
                StringUtils.defaultString(result.preferredLabel()),
                "concept",
                "",
                result.deprecated(),
                joinPath(result.broaderTerms())
        );
    }

    private static String kindOf(ConceptSearchSuggestion suggestion) {
        if (suggestion.isGroup()) {
            return "group";
        }
        if (suggestion.isFacet()) {
            return "facet";
        }
        if (suggestion.isAltLabelMatch()) {
            return "alt";
        }
        return "concept";
    }

    private static String joinPath(List<String> broaderTerms) {
        if (broaderTerms == null || broaderTerms.isEmpty()) {
            return "";
        }
        return String.join(" · ", broaderTerms);
    }

    private static String resolveLang(String lang) {
        if (StringUtils.isBlank(lang) || "all".equalsIgnoreCase(lang)) {
            return null;
        }
        return lang;
    }

    private static int clamp(int limit, int fallback) {
        int size = limit > 0 ? limit : fallback;
        return Math.min(size, MAX_PAGE);
    }
}
