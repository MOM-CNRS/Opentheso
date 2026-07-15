package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWriteSearchPort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyConceptWriteSearchAdapter implements ConceptWriteSearchPort {

    private final SearchService searchService;
    private final GroupService groupService;

    @Override
    public List<ConceptSearchSuggestion> autocompleteRelationTarget(
            String query,
            String lang,
            String thesaurusId,
            boolean includeDeprecated
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        return searchService.searchAutoCompletionForRelation(query, lang, thesaurusId, includeDeprecated).stream()
                .map(this::toSuggestion)
                .toList();
    }

    @Override
    public List<ConceptSearchSuggestion> autocompleteReplacedByTarget(
            String query,
            String lang,
            String thesaurusId
    ) {
        return autocompleteRelationTarget(query, lang, thesaurusId, false);
    }

    @Override
    public List<ConceptWriteCollection> autocompleteCollection(
            String query,
            String lang,
            String thesaurusId
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        return groupService.getAutoCompletionGroup(thesaurusId, lang, query).stream()
                .map(group -> new ConceptWriteCollection(group.getIdGroup(), group.getGroupLexicalValue()))
                .toList();
    }

    private ConceptSearchSuggestion toSuggestion(NodeSearchMini node) {
        return new ConceptSearchSuggestion(
                node.getIdConcept(),
                node.getPrefLabel(),
                node.getAltLabelValue(),
                node.isAltLabel()
        );
    }
}
