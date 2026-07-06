package fr.cnrs.opentheso.v2.concept.write.session;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;

import java.util.List;

public interface ConceptWriteSearchPort {

    List<ConceptSearchSuggestion> autocompleteRelationTarget(
            String query,
            String lang,
            String thesaurusId,
            boolean includeDeprecated
    );

    List<ConceptSearchSuggestion> autocompleteReplacedByTarget(
            String query,
            String lang,
            String thesaurusId
    );

    List<ConceptWriteCollection> autocompleteCollection(
            String query,
            String lang,
            String thesaurusId
    );
}
