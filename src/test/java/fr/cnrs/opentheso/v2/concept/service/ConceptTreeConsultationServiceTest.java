package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFacetNodeRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTreeConsultationServiceTest {

    @Mock
    private ConceptQueryRepository conceptQueryRepository;

    private ConceptTreeConsultationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptTreeConsultationService(conceptQueryRepository);
    }

    @Test
    void loadTopConcepts_mapsRepositoryRows() {
        when(conceptQueryRepository.findTopConceptsForTree("TH1", "fr", false)).thenReturn(
                List.of(new ConceptTreeRow("C1", "N1", "Top", "C", true))
        );

        var nodes = service.loadTopConcepts("TH1", "fr", false, false);

        assertEquals(1, nodes.size());
        assertEquals("C1", nodes.get(0).nodeId());
        assertEquals("Top", nodes.get(0).label());
        assertEquals("concept", nodes.get(0).nodeType());
    }

    @Test
    void loadConceptTreeChildren_loadsNarrowersAndFacets() {
        when(conceptQueryRepository.findNarrowersForTree("TH1", "C1", "fr", false)).thenReturn(
                List.of(new ConceptTreeRow("C2", "N2", "Child", "C", false))
        );
        when(conceptQueryRepository.findFacetsOfConceptForTree("TH1", "C1", "fr")).thenReturn(
                List.of(new ConceptFacetNodeRow("F1", "Facette", true))
        );

        var nodes = service.loadConceptTreeChildren("TH1", "C1", "concept", "fr", false, false);

        assertEquals(2, nodes.size());
        assertEquals("C2", nodes.get(0).nodeId());
        assertEquals("F1", nodes.get(1).nodeId());
        assertEquals("facet", nodes.get(1).nodeType());
    }

    @Test
    void loadConceptTreeChildren_facetType_loadsMembers() {
        when(conceptQueryRepository.findFacetMembersForTree("TH1", "F1", "fr")).thenReturn(
                List.of(new ConceptTreeRow("C3", "", "Member", "C", false))
        );

        var nodes = service.loadConceptTreeChildren("TH1", "F1", "facet", "fr", false, false);

        assertEquals(1, nodes.size());
        assertEquals("C3", nodes.get(0).nodeId());
        assertEquals("file", nodes.get(0).nodeType());
    }

    @Test
    void loadConceptTreeChildren_returnsEmptyWhenParentMissing() {
        assertTrue(service.loadConceptTreeChildren("TH1", "", "concept", "fr", false, false).isEmpty());
    }

    @Test
    void loadTopConcepts_sortsAlphabeticallyWhenPreferenceIsOff() {
        when(conceptQueryRepository.findTopConceptsForTree("TH1", "fr", false)).thenReturn(List.of(
                new ConceptTreeRow("C1", "02", "Zèbre", "C", false),
                new ConceptTreeRow("C2", "01", "Abeille", "C", false)
        ));

        var nodes = service.loadTopConcepts("TH1", "fr", false, false);

        assertEquals("Abeille", nodes.get(0).label());
        assertEquals("Zèbre", nodes.get(1).label());
    }

    @Test
    void loadTopConcepts_sortsByNotationWhenPreferenceIsOn() {
        when(conceptQueryRepository.findTopConceptsForTree("TH1", "fr", false)).thenReturn(List.of(
                new ConceptTreeRow("C1", "02", "Abeille", "C", false),
                new ConceptTreeRow("C2", "01", "Zèbre", "C", false)
        ));

        var nodes = service.loadTopConcepts("TH1", "fr", true, false);

        assertEquals("Zèbre", nodes.get(0).label());
        assertEquals("01", nodes.get(0).notation());
        assertEquals("Abeille", nodes.get(1).label());
    }

    @Test
    void loadTopConcepts_sortsNotationsLexicographicallyLikeLegacySql() {
        when(conceptQueryRepository.findTopConceptsForTree("TH1", "fr", false)).thenReturn(List.of(
                new ConceptTreeRow("C1", "2", "Deux", "C", false),
                new ConceptTreeRow("C2", "10", "Dix", "C", false)
        ));

        var nodes = service.loadTopConcepts("TH1", "fr", true, false);

        assertEquals("Dix", nodes.get(0).label());
        assertEquals("Deux", nodes.get(1).label());
    }

    @Test
    void loadConceptTreeChildren_keepsFacetsLastWhenSortingByNotation() {
        when(conceptQueryRepository.findNarrowersForTree("TH1", "C1", "fr", false)).thenReturn(
                List.of(new ConceptTreeRow("C2", "02", "Child", "C", false))
        );
        when(conceptQueryRepository.findFacetsOfConceptForTree("TH1", "C1", "fr")).thenReturn(
                List.of(new ConceptFacetNodeRow("F1", "Facette", true))
        );

        var nodes = service.loadConceptTreeChildren("TH1", "C1", "concept", "fr", true, false);

        assertEquals(2, nodes.size());
        assertEquals("C2", nodes.get(0).nodeId());
        assertEquals("F1", nodes.get(1).nodeId());
    }
}
