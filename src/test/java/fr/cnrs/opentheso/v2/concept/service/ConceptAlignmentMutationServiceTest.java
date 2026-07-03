package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentType;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.ConceptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAlignmentMutationServiceTest {

    @Mock
    private AlignmentService alignmentService;
    @Mock
    private ConceptService conceptService;

    private ConceptAlignmentMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptAlignmentMutationService(alignmentService, conceptService);
    }

    @Test
    void listAlignmentTypes_delegatesToAlignmentService() {
        var types = List.of(NodeAlignmentType.builder().id(1).label("exactMatch").labelSkos("exactMatch").isocode("").build());
        when(alignmentService.searchAllAlignementTypes()).thenReturn(types);

        assertEquals(types, service.listAlignmentTypes());
    }

    @Test
    void addManualAlignment_updatesConceptDateWhenSaved() {
        when(alignmentService.addNewAlignment(42, "", "source", "http://example.org", 3, "C1", "TH1", 0))
                .thenReturn(true);

        assertTrue(service.addManualAlignment(42, "source", "http://example.org", 3, "C1", "TH1"));

        verify(conceptService).updateDateOfConcept("TH1", "C1", 42);
    }

    @Test
    void addManualAlignment_doesNotUpdateConceptDateWhenSaveFails() {
        when(alignmentService.addNewAlignment(42, "", "source", "http://example.org", 3, "C1", "TH1", 0))
                .thenReturn(false);

        assertFalse(service.addManualAlignment(42, "source", "http://example.org", 3, "C1", "TH1"));

        verify(conceptService, never()).updateDateOfConcept(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void deleteAlignment_delegatesToAlignmentService() {
        when(alignmentService.deleteAlignment(7, "TH1")).thenReturn(true);

        assertTrue(service.deleteAlignment(7, "TH1"));
    }

    @Test
    void updateAlignment_delegatesToAlignmentService() {
        var element = AlignementElement.builder()
                .idAlignment(7)
                .alignement_id_type(2)
                .targetUri("http://example.org")
                .build();

        service.updateAlignment(element, "C1", "TH1");

        verify(alignmentService).updateAlignement(element, "C1", "TH1");
    }
}
