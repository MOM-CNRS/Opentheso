package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotCollectionCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionsLotCollectionServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;

    private ActionsLotCollectionService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotCollectionService(persistence);
    }

    @Test
    void validate_rejectsUnknownConcept() {
        String csv = """
                localId,skos:member
                unknown-id,GR1
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotImportValidationResult<ActionsLotCollectionCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.linesRead());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("localId", result.errors().get(0).column());
    }

    @Test
    void validate_rejectsUnknownCollection() {
        String csv = """
                localId,skos:member
                c1,MISSING
                """;
        ActionsLotTestIds.existing(persistence, "c1");
        ActionsLotTestIds.existingGroups(persistence);

        ActionsLotImportValidationResult<ActionsLotCollectionCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("skos:member", result.errors().get(0).column());
    }

    @Test
    void validate_acceptsKnownConceptAndCollection() {
        String csv = """
                localId,skos:member
                c1,GR1
                """;
        ActionsLotTestIds.existing(persistence, "c1");
        ActionsLotTestIds.existingGroups(persistence, "GR1");

        ActionsLotImportValidationResult<ActionsLotCollectionCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("c1", result.validCandidates().get(0).conceptId());
        assertEquals("GR1", result.validCandidates().get(0).groupId());
    }

    @Test
    void applyImport_addsMembership() {
        ActionsLotCollectionCandidate candidate = new ActionsLotCollectionCandidate(2, "c1", "c1", "GR1");
        when(persistence.addConceptGroupConcept("GR1", "c1", "TH1")).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1");

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).addConceptGroupConcept("GR1", "c1", "TH1");
    }

    @Test
    void applyImport_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyImport(List.of(), "TH1");
        assertFalse(result.success());
        verify(persistence, never()).addConceptGroupConcept(anyString(), anyString(), anyString());
    }
}
