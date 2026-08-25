package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNoteCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNoteValidationResult;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionsLotNoteServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;

    private ActionsLotNoteService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotNoteService(persistence);
    }

    @Test
    void validate_rejectsUnknownConcept() {
        String csv = """
                localId,skos:definition@fr
                unknown-id,une définition
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotNoteValidationResult result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.linesRead());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("localId", result.errors().get(0).column());
    }

    @Test
    void validate_acceptsKnownConceptAndSplitsValues() {
        String csv = """
                localId,skos:definition@fr,skos:scopeNote@fr
                c1,def1##def2,une note d'application
                """;
        ActionsLotTestIds.existing(persistence, "c1");

        ActionsLotNoteValidationResult result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(3, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("definition", result.validCandidates().get(0).typeCode());
        assertEquals("def1", result.validCandidates().get(0).value());
        assertEquals("definition", result.validCandidates().get(1).typeCode());
        assertEquals("def2", result.validCandidates().get(1).value());
        assertEquals("scopeNote", result.validCandidates().get(2).typeCode());
    }

    @Test
    void validate_rejectsRowWithoutNotes() {
        String csv = """
                localId,skos:definition@fr
                c1,
                """;
        ActionsLotTestIds.existing(persistence, "c1");

        ActionsLotNoteValidationResult result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("skos:note", result.errors().get(0).column());
    }

    @Test
    void applyImport_skipsExistingNotes() {
        ActionsLotNoteCandidate candidate = new ActionsLotNoteCandidate(
                2, "c1", "c1", "definition", "fr", "déjà là"
        );
        when(persistence.isNoteExist("c1", "TH1", "fr", "déjà là", "definition")).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7, false);

        assertTrue(result.success());
        assertEquals(0, result.applied());
        assertEquals(1, result.rejected());
        verify(persistence, never()).addNote(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void applyImport_addsMissingNotes() {
        ActionsLotNoteCandidate candidate = new ActionsLotNoteCandidate(
                2, "c1", "c1", "definition", "fr", "nouvelle"
        );
        when(persistence.isNoteExist("c1", "TH1", "fr", "nouvelle", "definition")).thenReturn(false);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7, false);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).addNote("c1", "fr", "TH1", "nouvelle", "definition", "import", 7);
        verify(persistence, never()).deleteNotes(anyString(), anyString());
    }

    @Test
    void applyImport_clearBeforeDeletesThenAdds() {
        ActionsLotNoteCandidate candidate = new ActionsLotNoteCandidate(
                2, "c1", "c1", "note", "fr", "remplace tout"
        );

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7, true);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).deleteNotes("c1", "TH1");
        verify(persistence).addNote("c1", "fr", "TH1", "remplace tout", "note", "import", 7);
        verify(persistence, never()).isNoteExist(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void applyImport_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyImport(List.of(), "TH1", 1, false);
        assertFalse(result.success());
    }
}
