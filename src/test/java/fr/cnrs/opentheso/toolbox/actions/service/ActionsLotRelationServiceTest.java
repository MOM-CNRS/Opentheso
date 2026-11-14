package fr.cnrs.opentheso.toolbox.actions.service;

import fr.cnrs.opentheso.concept.write.persistence.ConceptRelationWriteRepository;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotRelationCandidate;
import fr.cnrs.opentheso.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionsLotRelationServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;

    @Mock
    private ConceptRelationWriteRepository relations;

    private ActionsLotRelationService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotRelationService(persistence, relations);
    }

    @Test
    void validate_acceptsAPairOfKnownConcepts() {
        String csv = """
                localId,localId cible
                c1,c2
                """;
        ActionsLotTestIds.existing(persistence, "c1", "c2");
        when(relations.hasRelatedRelation(anyString(), anyString(), anyString())).thenReturn(false);

        ActionsLotImportValidationResult<ActionsLotRelationCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("c2", result.validCandidates().get(0).targetConceptId());
    }

    @Test
    void validate_rejectsAnUnknownTargetAndAnExistingRelation() {
        String csv = """
                localId,localId cible
                c1,missing
                c1,c2
                """;
        ActionsLotTestIds.existing(persistence, "c1", "c2");
        when(relations.hasRelatedRelation(eq("c1"), eq("c2"), eq("TH1"))).thenReturn(true);

        ActionsLotImportValidationResult<ActionsLotRelationCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(2, result.errorCount());
        assertEquals("localId cible", result.errors().get(0).column());
        assertEquals("Relation déjà existante", result.errors().get(1).message());
    }

    @Test
    void validate_rejectsAFileWithoutTheExpectedColumns() {
        String csv = """
                foo,bar
                a,b
                """;

        ActionsLotImportValidationResult<ActionsLotRelationCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertFalse(result.success());
        assertEquals(ActionsLotRelationService.EXPECTED_COLUMNS, result.errorMessage());
    }

    @Test
    void applyImport_writesTheRelatedPair() {
        ActionsLotRelationCandidate candidate = new ActionsLotRelationCandidate(2, "c1", "c2", "c1", "c2");
        when(relations.addRelatedRelation("c1", "c2", "TH1", 7)).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(relations).addRelatedRelation("c1", "c2", "TH1", 7);
    }
}
