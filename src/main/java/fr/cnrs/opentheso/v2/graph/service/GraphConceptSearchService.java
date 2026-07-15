package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphConceptSearchService {

    private final ConceptReadService conceptReadService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    @Transactional(readOnly = true)
    public List<ConceptSearchSuggestion> searchForRelation(String query, String thesaurusId) {
        if (StringUtils.isAnyBlank(query, thesaurusId)) {
            return Collections.emptyList();
        }
        String lang = thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
        return conceptReadService.searchByLabel(thesaurusId, lang, query, 25).stream()
                .filter(node -> !node.isGroup())
                .map(node -> new ConceptSearchSuggestion(
                        node.nodeId(),
                        StringUtils.defaultString(node.label()),
                        "",
                        ConceptSearchKind.CONCEPT,
                        false
                ))
                .toList();
    }
}
