package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptSearchService {

    private final ConceptSearchReadService conceptSearchReadService;
    private final ConceptSearchHydrationService conceptSearchHydrationService;

    @Transactional(readOnly = true)
    public List<ConceptSearchSuggestion> autocomplete(
            String query,
            ConceptSearchMode mode,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        return conceptSearchReadService.autocomplete(query, mode, thesaurusId, resolveLang(lang), anonymous);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> search(
            String query,
            ConceptSearchMode mode,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        String resolvedLang = resolveLang(lang);
        List<String> ids = conceptSearchReadService.findConceptIds(query, mode, thesaurusId, resolvedLang, anonymous);
        return List.copyOf(conceptSearchHydrationService.hydrateAll(ids, thesaurusId, resolvedLang));
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> searchDeprecated(String thesaurusId, String lang) {
        return hydrateIds(conceptSearchReadService.findDeprecatedConceptIds(thesaurusId), thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> searchPolyhierarchy(String thesaurusId, String lang) {
        return hydrateIds(conceptSearchReadService.findPolyhierarchyConceptIds(thesaurusId), thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> searchMultiGroups(String thesaurusId, String lang) {
        return hydrateIds(conceptSearchReadService.findMultiGroupConceptIds(thesaurusId), thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> searchWithoutGroups(String thesaurusId, String lang) {
        return hydrateIds(conceptSearchReadService.findWithoutGroupConceptIds(thesaurusId), thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> searchDuplicates(String thesaurusId, String lang) {
        // Pas de resolveLang("all") → null : les doublons exigent une langue concrète (legacy currentLang).
        if (StringUtils.isAnyBlank(thesaurusId, lang) || "all".equalsIgnoreCase(lang)) {
            return Collections.emptyList();
        }
        List<ConceptSearchResult> results = new ArrayList<>();
        for (String label : conceptSearchReadService.findDuplicateLabels(thesaurusId, lang)) {
            var mapped = conceptSearchReadService.hydrateResultFromLabel(label, thesaurusId, lang);
            if (mapped != null) {
                results.add(mapped);
            }
        }
        results.sort(Comparator.naturalOrder());
        return List.copyOf(results);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> searchForbiddenRelationships(String thesaurusId, String lang) {
        return hydrateIds(conceptSearchReadService.findForbiddenRelationshipConceptIds(thesaurusId), thesaurusId, lang);
    }

    private List<ConceptSearchResult> hydrateIds(List<String> conceptIds, String thesaurusId, String lang) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return Collections.emptyList();
        }
        String resolvedLang = resolveLang(lang);
        List<ConceptSearchResult> results = new ArrayList<>(
                conceptSearchHydrationService.hydrateAll(conceptIds, thesaurusId, resolvedLang));
        results.sort(Comparator.naturalOrder());
        return List.copyOf(results);
    }

    private String resolveLang(String lang) {
        return "all".equalsIgnoreCase(lang) ? null : lang;
    }
}
