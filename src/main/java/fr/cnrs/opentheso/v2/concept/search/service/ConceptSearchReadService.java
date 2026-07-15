package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptSearchReadService {

    private final ConceptSearchEngine conceptSearchEngine;
    private final ConceptSearchHydrationService conceptSearchHydrationService;
    private final ConceptSearchQueryRepository conceptSearchQueryRepository;

    @Transactional(readOnly = true)
    public List<ConceptSearchSuggestion> autocomplete(
            String query,
            ConceptSearchMode mode,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        return conceptSearchEngine.autocomplete(query, mode, thesaurusId, lang, anonymous);
    }

    @Transactional(readOnly = true)
    public List<String> findConceptIds(
            String query,
            ConceptSearchMode mode,
            String thesaurusId,
            String lang,
            boolean anonymous
    ) {
        return conceptSearchEngine.findConceptIds(query, mode, thesaurusId, lang, anonymous);
    }

    @Transactional(readOnly = true)
    public ConceptSearchResult hydrateResult(String conceptId, String thesaurusId, String lang) {
        return conceptSearchHydrationService.hydrateResult(conceptId, thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public ConceptSearchResult hydrateResultFromLabel(String label, String thesaurusId, String lang) {
        return conceptSearchHydrationService.hydrateResultFromLabel(label, thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public List<String> findDeprecatedConceptIds(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptSearchQueryRepository.findDeprecatedConceptIds(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<String> findPolyhierarchyConceptIds(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptSearchQueryRepository.findPolyhierarchyConceptIds(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<String> findMultiGroupConceptIds(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptSearchQueryRepository.findMultiGroupConceptIds(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<String> findWithoutGroupConceptIds(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptSearchQueryRepository.findWithoutGroupConceptIds(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<String> findDuplicateLabels(String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        return conceptSearchQueryRepository.findDuplicateLabels(thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public List<String> findForbiddenRelationshipConceptIds(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        return conceptSearchQueryRepository.findForbiddenRelationshipConceptIds(thesaurusId);
    }
}
