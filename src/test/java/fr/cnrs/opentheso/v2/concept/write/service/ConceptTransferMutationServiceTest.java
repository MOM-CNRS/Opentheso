package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptsToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptTransferWritePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTransferMutationServiceTest {

    @Mock
    private ConceptTransferWritePersistence persistence;
    @Mock
    private BranchConceptSupport branchConceptSupport;

    private ConceptTransferMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptTransferMutationService(persistence, branchConceptSupport);
    }

    @Test
    void moveConceptToThesaurus_delegatesToPersistence() {
        var command = mock(MoveConceptToThesaurusCommand.class);
        when(persistence.moveConceptToThesaurus(command)).thenReturn(MutationResult.ok("moved"));

        assertTrue(service.moveConceptToThesaurus(command).success());
    }

    @Test
    void listAdminThesauri_delegatesToPersistence() {
        var options = List.of(new ConceptWriteThesaurusOption("TH2", "Autre"));
        when(persistence.listAdminThesauri(7, true, "TH1", "fr")).thenReturn(options);

        assertEquals(options, service.listAdminThesauri(7, true, "TH1", "fr"));
    }

    @Test
    void moveConceptsToThesaurus_rejectsEmptySelection() {
        var result = service.moveConceptsToThesaurus(new MoveConceptsToThesaurusCommand(
                "TH1", "TH2", List.of("  "), "fr", 7, "admin", null));

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(persistence, times(0)).moveConceptToThesaurus(any());
    }

    @Test
    void moveConceptsToThesaurus_skipsDescendantsOfSelectedHeads() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "P")).thenReturn(List.of("P", "C"));
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C")).thenReturn(List.of("C"));
        when(persistence.moveConceptToThesaurus(any())).thenReturn(MutationResult.ok("ok"));

        var result = service.moveConceptsToThesaurus(new MoveConceptsToThesaurusCommand(
                "TH1", "TH2", List.of("P", "C"), "fr", 7, "admin", null));

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("1 concept déplacé", result.message());
        ArgumentCaptor<MoveConceptToThesaurusCommand> captor =
                ArgumentCaptor.forClass(MoveConceptToThesaurusCommand.class);
        verify(persistence, times(1)).moveConceptToThesaurus(captor.capture());
        assertEquals("P", captor.getValue().headConceptId());
        assertEquals(List.of("P", "C"), captor.getValue().branchConceptIds());
    }

    @Test
    void moveConceptsToThesaurus_movesIndependentHeads() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C1")).thenReturn(List.of("C1"));
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C2")).thenReturn(List.of("C2"));
        when(persistence.moveConceptToThesaurus(any())).thenReturn(MutationResult.ok("ok"));

        var result = service.moveConceptsToThesaurus(new MoveConceptsToThesaurusCommand(
                "TH1", "TH2", List.of("C1", "C2"), "fr", 7, "admin", "PARENT"));

        assertEquals("2 concepts déplacés", result.message());
        verify(persistence, times(2)).moveConceptToThesaurus(any());
    }
}
