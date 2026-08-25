package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotValidationResult;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;



@ExtendWith(MockitoExtension.class)
class ActionsLotAlignmentServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;
    @Mock
    private ThesaurusCsvWriter thesaurusCsvWriter;

    private ActionsLotAlignmentService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotAlignmentService(persistence, thesaurusCsvWriter);
    }

    @Test
    void validateImport_rejectsUnknownConcept() {
        String csv = """
                localId,source
                unknown-id,https://www.wikidata.org/wiki/Q1##1
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotValidationResult result = service.validateImport(
                csv.getBytes(StandardCharsets.UTF_8),
                0,
                "identifier",
                "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.linesRead());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("localId", result.errors().get(0).column());
    }

    @Test
    void validateImport_acceptsKnownConcept() {
        String csv = """
                localId,source
                c1,https://www.wikidata.org/wiki/Q1##2
                """;
        ActionsLotTestIds.existing(persistence, "c1");

        ActionsLotValidationResult result = service.validateImport(
                csv.getBytes(StandardCharsets.UTF_8),
                0,
                "identifier",
                "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("c1", result.validCandidates().get(0).conceptId());
        assertEquals(2, result.validCandidates().get(0).alignmentTypeId());
    }

    @Test
    void validateDelete_ignoresMissingConcept() {
        String csv = """
                localId,URI
                missing,https://example.org/a
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotValidationResult result = service.validateDelete(
                csv.getBytes(StandardCharsets.UTF_8),
                0,
                "identifier",
                "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.ignoredCount());
        assertEquals(0, result.validCount());
        assertFalse(result.hasErrors());
    }

    @Test
    void applyImport_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyImport(java.util.List.of(), "TH1", 1);
        assertFalse(result.success());
    }
}
