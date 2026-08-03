package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.repositories.SearchRepository;
import fr.cnrs.opentheso.utils.StringUtils;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Autocomplete « Branche en graphe » — même comportement que le legacy
 * {@code SearchService#searchAutoCompletionForRelation(..., includeDeprecated=true)} :
 * termes préférés + synonymes (alt labels), y compris concepts dépréciés.
 */
@Service
@RequiredArgsConstructor
public class GraphConceptSearchService {

    private final SearchRepository searchRepository;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    @Transactional(readOnly = true)
    public List<ConceptSearchSuggestion> searchForRelation(String query, String thesaurusId) {
        if (org.apache.commons.lang3.StringUtils.isAnyBlank(query, thesaurusId)) {
            return Collections.emptyList();
        }

        String lang = thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
        if (org.apache.commons.lang3.StringUtils.isBlank(lang)) {
            return Collections.emptyList();
        }

        String value = StringUtils.unaccentLowerString(StringUtils.convertString(query));
        List<ConceptSearchSuggestion> results = new ArrayList<>();

        for (Object[] row : searchRepository.searchPreferredLabels(value, lang, thesaurusId)) {
            results.add(new ConceptSearchSuggestion(
                    (String) row[0],
                    org.apache.commons.lang3.StringUtils.defaultString((String) row[1]),
                    "",
                    ConceptSearchKind.CONCEPT,
                    false
            ));
        }

        for (Object[] row : searchRepository.searchAltLabelsWithDeprecated(value, lang, thesaurusId)) {
            String combined = org.apache.commons.lang3.StringUtils.defaultString((String) row[1]);
            String[] parts = combined.split(" ->", 2);
            String altLabel = parts.length > 0 ? parts[0].trim() : "";
            String preferredLabel = parts.length > 1 ? parts[1].trim() : "";
            results.add(new ConceptSearchSuggestion(
                    (String) row[0],
                    preferredLabel,
                    altLabel,
                    ConceptSearchKind.ALT_LABEL,
                    false
            ));
        }

        return results;
    }
}
