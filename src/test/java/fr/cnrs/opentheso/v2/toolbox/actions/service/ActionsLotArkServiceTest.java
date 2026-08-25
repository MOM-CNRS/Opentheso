package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotArkCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusMaintenanceService;
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
class ActionsLotArkServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;
    @Mock
    private ThesaurusMaintenanceService thesaurusMaintenanceService;

    private ActionsLotArkService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotArkService(persistence, thesaurusMaintenanceService);
    }

    @Test
    void validate_rejectsUnknownConcept() {
        String csv = """
                localId,arkId
                unknown-id,26678/crtcg26jeN4R9
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotImportValidationResult<ActionsLotArkCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "TH1"
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
                localId,arkId
                152645,26678/crtcg26jeN4R9
                """;
        ActionsLotTestIds.existing(persistence, "152645");

        ActionsLotImportValidationResult<ActionsLotArkCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("26678/crtcg26jeN4R9", result.validCandidates().get(0).arkId());
        assertEquals("152645", result.validCandidates().get(0).conceptId());
    }

    @Test
    void applyImport_skipsWhenArkAlreadyPresent() {
        ActionsLotArkCandidate candidate = new ActionsLotArkCandidate(2, "c1", "c1", "new-ark");
        when(persistence.findConceptsByIds(any(), eq("TH1"))).thenReturn(Map.of("c1", Concept.builder().idArk("old-ark").build()));

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", false);

        assertTrue(result.success());
        assertEquals(0, result.applied());
        assertEquals(1, result.rejected());
        verify(persistence, never()).updateArkIdOfConcept(anyString(), anyString(), anyString());
    }

    @Test
    void applyImport_writesWhenEmpty() {
        ActionsLotArkCandidate candidate = new ActionsLotArkCandidate(2, "c1", "c1", "26678/abc");
        when(persistence.findConceptsByIds(any(), eq("TH1"))).thenReturn(Map.of("c1", Concept.builder().idArk("").build()));
        when(persistence.updateArkIdOfConcept("c1", "TH1", "26678/abc")).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", false);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).updateArkIdOfConcept("c1", "TH1", "26678/abc");
    }

    @Test
    void applyImport_clearBeforeOverwrites() {
        ActionsLotArkCandidate candidate = new ActionsLotArkCandidate(2, "c1", "c1", "new-ark");
        when(persistence.findConceptsByIds(any(), eq("TH1"))).thenReturn(Map.of("c1", Concept.builder().idArk("old-ark").build()));
        when(persistence.updateArkIdOfConcept("c1", "TH1", "new-ark")).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", true);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).updateArkIdOfConcept("c1", "TH1", "new-ark");
    }

    @Test
    void generateFromConceptId_requiresNaan() {
        ActionsLotApplyResult result = service.generateFromConceptId("TH1", "ndp", "  ", false);
        assertFalse(result.success());
        verify(thesaurusMaintenanceService, never()).generateArkFromConceptId(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void generateFromConceptId_delegates() {
        when(thesaurusMaintenanceService.generateArkFromConceptId("TH1", "ndp", "66666", true)).thenReturn(4);

        ActionsLotApplyResult result = service.generateFromConceptId("TH1", "ndp", "66666", true);

        assertTrue(result.success());
        assertEquals(4, result.applied());
    }

    @Test
    void generateLocal_delegates() {
        when(thesaurusMaintenanceService.generateLocalArk("TH1", false)).thenReturn(2);

        ActionsLotApplyResult result = service.generateLocal("TH1", false);

        assertTrue(result.success());
        assertEquals(2, result.applied());
    }

    @Test
    void applyImport_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyImport(List.of(), "TH1", false);
        assertFalse(result.success());
    }
}
