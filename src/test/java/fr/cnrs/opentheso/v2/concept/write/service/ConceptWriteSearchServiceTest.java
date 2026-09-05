package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCustomTarget;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteFacet;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptWriteSearchPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptWriteSearchServiceTest {

    @Mock
    private ConceptWriteSearchPersistence persistence;

    private ConceptWriteSearchService service;

    @BeforeEach
    void setUp() {
        service = new ConceptWriteSearchService(persistence);
    }

    @Test
    void autocompleteMethods_delegateToPersistence() {
        var suggestion = new ConceptSearchSuggestion("C1", "Chat", null, false);
        when(persistence.autocompleteRelationTarget("ch", "fr", "TH1", true)).thenReturn(List.of(suggestion));
        when(persistence.autocompleteReplacedByTarget("ch", "fr", "TH1")).thenReturn(List.of(suggestion));
        when(persistence.autocompleteCollection("col", "fr", "TH1"))
                .thenReturn(List.of(new ConceptWriteCollection("G1", "Groupe")));
        when(persistence.autocompleteFacet("fa", "fr", "TH1"))
                .thenReturn(List.of(new ConceptWriteFacet("F1", "Facette")));
        when(persistence.listFacets("fr", "TH1"))
                .thenReturn(List.of(new ConceptWriteFacet("F1", "Facette")));
        when(persistence.autocompleteCustomRelationTarget("x", "fr", "TH1"))
                .thenReturn(List.of(new ConceptWriteCustomTarget("C2", "Cible", "related")));

        assertEquals("C1", service.autocompleteRelationTarget("ch", "fr", "TH1", true).get(0).conceptId());
        assertEquals("C1", service.autocompleteReplacedByTarget("ch", "fr", "TH1").get(0).conceptId());
        assertEquals("G1", service.autocompleteCollection("col", "fr", "TH1").get(0).id());
        assertEquals("F1", service.autocompleteFacet("fa", "fr", "TH1").get(0).id());
        assertEquals("F1", service.listFacets("fr", "TH1").get(0).id());
        assertEquals("C2", service.autocompleteCustomRelationTarget("x", "fr", "TH1").get(0).id());
    }
}
