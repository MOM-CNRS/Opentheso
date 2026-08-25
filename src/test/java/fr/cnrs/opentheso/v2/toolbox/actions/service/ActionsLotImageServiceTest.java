package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImageCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionsLotImageServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;

    private ActionsLotImageService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotImageService(persistence);
    }

    @Test
    void validate_rejectsUnknownConcept() {
        String csv = """
                localId,foaf:image
                unknown-id,rdf:about=https://example.com/a.jpg
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotImportValidationResult<ActionsLotImageCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.linesRead());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("localId", result.errors().get(0).column());
    }

    @Test
    void validate_acceptsKnownConceptAndSplitsImages() {
        String csv = """
                localId,foaf:image
                c1,rdf:about=https://example.com/a.jpg@@dcterms:title=lait##rdf:about=https://example.com/b.jpg
                """;
        ActionsLotTestIds.existing(persistence, "c1");

        ActionsLotImportValidationResult<ActionsLotImageCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(2, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("https://example.com/a.jpg", result.validCandidates().get(0).uri());
        assertEquals("lait", result.validCandidates().get(0).title());
        assertEquals("https://example.com/b.jpg", result.validCandidates().get(1).uri());
    }

    @Test
    void validate_rejectsInvalidUrl() {
        String csv = """
                localId,foaf:image
                c1,rdf:about=not-a-url
                """;
        ActionsLotTestIds.existing(persistence, "c1");

        ActionsLotImportValidationResult<ActionsLotImageCandidate> result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("foaf:image", result.errors().get(0).column());
    }

    @Test
    void applyImport_skipsExistingUri() {
        ActionsLotImageCandidate candidate = new ActionsLotImageCandidate(
                2, "c1", "c1", "https://example.com/a.jpg", "t", "r", "me"
        );
        when(persistence.findExistingImageKeys(any(), eq("TH1"))).thenReturn(
                Set.of(fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence.imageKey("c1", "https://example.com/a.jpg"))
        );

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7, false);

        assertTrue(result.success());
        assertEquals(0, result.applied());
        assertEquals(1, result.rejected());
        verify(persistence, never()).addExternalImage(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void applyImport_addsMissingImage() {
        ActionsLotImageCandidate candidate = new ActionsLotImageCandidate(
                2, "c1", "c1", "https://example.com/a.jpg", "lait", "Web", "moi"
        );
        when(persistence.findExistingImageKeys(any(), eq("TH1"))).thenReturn(Set.of());

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7, false);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).addExternalImage("c1", "TH1", "lait", "Web", "https://example.com/a.jpg", "moi", 7);
        verify(persistence, never()).deleteImages(anyString(), anyString());
    }

    @Test
    void applyImport_clearBeforeDeletesOnceThenAdds() {
        ActionsLotImageCandidate first = new ActionsLotImageCandidate(
                2, "c1", "c1", "https://example.com/a.jpg", "a", "", ""
        );
        ActionsLotImageCandidate second = new ActionsLotImageCandidate(
                2, "c1", "c1", "https://example.com/b.jpg", "b", "", ""
        );

        ActionsLotApplyResult result = service.applyImport(List.of(first, second), "TH1", 7, true);

        assertTrue(result.success());
        assertEquals(2, result.applied());
        verify(persistence, times(1)).deleteImages("TH1", "c1");
        verify(persistence, never()).findExistingImageKeys(any(), anyString());
    }

    @Test
    void applyImport_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyImport(List.of(), "TH1", 1, false);
        assertFalse(result.success());
    }
}
