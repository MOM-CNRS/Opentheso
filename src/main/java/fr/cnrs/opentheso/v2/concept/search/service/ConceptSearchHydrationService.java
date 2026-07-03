package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchResult;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ConceptFullQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConceptSearchHydrationService {

    private final ConceptSearchQueryRepository conceptSearchQueryRepository;
    private final ConceptFullQueryRepository conceptFullQueryRepository;

    @Transactional(readOnly = true)
    public ConceptSearchResult hydrateResult(String conceptId, String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(conceptId, thesaurusId)) {
            return null;
        }
        return buildConceptSearch(conceptId, thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public ConceptSearchResult hydrateResultFromLabel(String label, String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(label, thesaurusId, lang)) {
            return null;
        }
        String normalizedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(label);
        String conceptId = conceptSearchQueryRepository.findConceptIdFromLabel(thesaurusId, normalizedLabel, lang)
                .orElse(null);
        if (StringUtils.isBlank(conceptId)) {
            conceptId = conceptSearchQueryRepository.findConceptIdFromAltLabel(thesaurusId, normalizedLabel, lang)
                    .orElse(null);
        }
        if (StringUtils.isBlank(conceptId)) {
            return null;
        }
        return buildConceptSearch(conceptId, thesaurusId, lang);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchResult> hydrateAll(List<String> conceptIds, String thesaurusId, String lang) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> statusMap = conceptSearchQueryRepository.findStatusByIds(conceptIds, thesaurusId);
        Map<String, String> labelMap = conceptSearchQueryRepository.findPreferredLabelsByIds(conceptIds, thesaurusId, lang);
        Map<String, List<String>> synonymMap = conceptSearchQueryRepository.findSynonymsByIds(conceptIds, thesaurusId, lang);
        Map<String, List<String>> broaderMap = conceptSearchQueryRepository.findBroadersByIds(conceptIds, thesaurusId, lang);
        Map<String, List<String>> relatedMap = conceptSearchQueryRepository.findRelatedsByIds(conceptIds, thesaurusId, lang);
        List<ConceptSearchResult> results = new ArrayList<>(conceptIds.size());
        for (String conceptId : conceptIds) {
            String status = statusMap.get(conceptId);
            if (status == null || "CA".equalsIgnoreCase(status)) {
                continue;
            }
            boolean deprecated = "dep".equalsIgnoreCase(status);
            results.add(new ConceptSearchResult(
                    thesaurusId,
                    conceptId,
                    labelMap.getOrDefault(conceptId, ""),
                    lang,
                    deprecated,
                    synonymMap.getOrDefault(conceptId, Collections.emptyList()),
                    broaderMap.getOrDefault(conceptId, Collections.emptyList()),
                    relatedMap.getOrDefault(conceptId, Collections.emptyList())
            ));
        }
        return results;
    }

    private ConceptSearchResult buildConceptSearch(String conceptId, String thesaurusId, String lang) {
        boolean deprecated = conceptSearchQueryRepository.findConceptStatus(conceptId, thesaurusId)
                .map(status -> "dep".equalsIgnoreCase(status))
                .orElse(false);
        String preferredLabel = conceptFullQueryRepository.findPreferredLabel(conceptId, thesaurusId, lang)
                .map(row -> stringAt(row, 0))
                .orElse("");
        return new ConceptSearchResult(
                thesaurusId,
                conceptId,
                preferredLabel,
                lang,
                deprecated,
                mapSynonyms(conceptId, thesaurusId, lang),
                mapBroaderTerms(conceptId, thesaurusId, lang),
                mapRelatedTerms(conceptId, thesaurusId, lang)
        );
    }

    private List<String> mapSynonyms(String conceptId, String thesaurusId, String lang) {
        List<String> synonyms = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.findSynonymsForSearch(conceptId, thesaurusId, lang)) {
            String value = stringAt(row, 0);
            if (StringUtils.isNotBlank(value)) {
                synonyms.add(value);
            }
        }
        return synonyms;
    }

    private List<String> mapBroaderTerms(String conceptId, String thesaurusId, String lang) {
        List<String> broaders = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.findBroaderRelationsForSearch(conceptId, thesaurusId, lang)) {
            String title = stringAt(row, 2);
            if (StringUtils.isNotBlank(title)) {
                broaders.add(title);
            }
        }
        return broaders;
    }

    private List<String> mapRelatedTerms(String conceptId, String thesaurusId, String lang) {
        List<String> related = new ArrayList<>();
        for (Object[] row : conceptSearchQueryRepository.findRelatedRelationsForSearch(conceptId, thesaurusId, lang)) {
            String title = stringAt(row, 2);
            if (StringUtils.isNotBlank(title)) {
                related.add(title);
            }
        }
        return related;
    }

    private static String stringAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }
}
