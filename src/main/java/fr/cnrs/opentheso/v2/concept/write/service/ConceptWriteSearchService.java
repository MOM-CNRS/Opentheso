package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCustomTarget;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteFacet;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptWriteSearchPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptWriteSearchService {

    private final ConceptWriteSearchPersistence conceptWriteSearchPersistence;

    @Transactional(readOnly = true)
    public List<ConceptSearchSuggestion> autocompleteRelationTarget(
            String query,
            String lang,
            String thesaurusId,
            boolean includeDeprecated
    ) {
        return conceptWriteSearchPersistence.autocompleteRelationTarget(query, lang, thesaurusId, includeDeprecated);
    }

    @Transactional(readOnly = true)
    public List<ConceptSearchSuggestion> autocompleteReplacedByTarget(
            String query,
            String lang,
            String thesaurusId
    ) {
        return conceptWriteSearchPersistence.autocompleteReplacedByTarget(query, lang, thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteCollection> autocompleteCollection(
            String query,
            String lang,
            String thesaurusId
    ) {
        return conceptWriteSearchPersistence.autocompleteCollection(query, lang, thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteFacet> autocompleteFacet(
            String query,
            String lang,
            String thesaurusId
    ) {
        return conceptWriteSearchPersistence.autocompleteFacet(query, lang, thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteFacet> listFacets(String lang, String thesaurusId) {
        return conceptWriteSearchPersistence.listFacets(lang, thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<ConceptWriteCustomTarget> autocompleteCustomRelationTarget(
            String query,
            String lang,
            String thesaurusId
    ) {
        return conceptWriteSearchPersistence.autocompleteCustomRelationTarget(query, lang, thesaurusId);
    }
}
