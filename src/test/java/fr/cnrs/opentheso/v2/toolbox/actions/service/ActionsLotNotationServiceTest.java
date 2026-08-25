package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNotationCandidate;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionsLotNotationServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;

    private ActionsLotNotationService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotNotationService(persistence);
    }

    @Test
    void validate_rejectsUnknownConcept() {
        String csv = """
                localId,skos:notation
                unknown-id,CBL1
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotImportValidationResult<ActionsLotNotationCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.linesRead());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("localId", result.errors().get(0).column());
    }

    @Test
    void validate_acceptsKnownConcept() {
        String csv = """
                localId,skos:notation
                c1,CBL1
                """;
        ActionsLotTestIds.existing(persistence, "c1");

        ActionsLotImportValidationResult<ActionsLotNotationCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("CBL1", result.validCandidates().get(0).notation());
    }

    @Test
    void applyImport_skipsWhenNotationAlreadyPresent() {
        ActionsLotNotationCandidate candidate = new ActionsLotNotationCandidate(2, "c1", "c1", "NEW");
        when(persistence.findConceptsByIds(any(), eq("TH1"))).thenReturn(Map.of("c1", Concept.builder().notation("OLD").build()));

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", false);

        assertTrue(result.success());
        assertEquals(0, result.applied());
        assertEquals(1, result.rejected());
        verify(persistence, never()).updateNotation(anyString(), anyString(), anyString());
    }

    @Test
    void applyImport_writesWhenEmpty() {
        ActionsLotNotationCandidate candidate = new ActionsLotNotationCandidate(2, "c1", "c1", "CBL1");
        when(persistence.findConceptsByIds(any(), eq("TH1"))).thenReturn(Map.of("c1", Concept.builder().notation("").build()));
        when(persistence.updateNotation("c1", "TH1", "CBL1")).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", false);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).updateNotation("c1", "TH1", "CBL1");
    }

    @Test
    void applyImport_clearBeforeOverwrites() {
        ActionsLotNotationCandidate candidate = new ActionsLotNotationCandidate(2, "c1", "c1", "NEW");
        when(persistence.findConceptsByIds(any(), eq("TH1"))).thenReturn(Map.of("c1", Concept.builder().notation("OLD").build()));
        when(persistence.updateNotation("c1", "TH1", "NEW")).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", true);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).updateNotation("c1", "TH1", "NEW");
    }

    @Test
    void applyImport_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyImport(List.of(), "TH1", false);
        assertFalse(result.success());
    }
}
