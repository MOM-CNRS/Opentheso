package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotAltLabelCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotAltLabelValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionsLotAltLabelServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;

    private ActionsLotAltLabelService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotAltLabelService(persistence);
    }

    @Test
    void validate_rejectsEmptyFile() {
        ActionsLotAltLabelValidationResult result = service.validate(new byte[0], 0, "identifier", "TH1", true);
        assertFalse(result.success());
    }

    @Test
    void validate_rejectsBlankThesaurus() {
        ActionsLotAltLabelValidationResult result = service.validate(
                "localId,skos:altLabel@fr\nc1,syn\n".getBytes(StandardCharsets.UTF_8),
                0, "identifier", " ", true);
        assertFalse(result.success());
    }

    @Test
    void validate_rejectsUnknownConceptWhenRequired() {
        String csv = """
                localId,skos:altLabel@fr
                unknown-id,synonyme
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotAltLabelValidationResult result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1", true);

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("localId", result.errors().get(0).column());
    }

    @Test
    void validate_ignoresUnknownConceptWhenNotRequired() {
        String csv = """
                localId,skos:altLabel@fr
                unknown-id,synonyme
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotAltLabelValidationResult result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1", false);

        assertTrue(result.success());
        assertEquals(1, result.ignoredCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void validate_acceptsKnownConceptAndSplitsValues() {
        String csv = """
                localId,skos:altLabel@fr
                c1,syn1##syn2
                """;
        ActionsLotTestIds.existing(persistence, "c1");
        when(persistence.findPreferredTermsByConceptIds(any(), eq("TH1")))
                .thenReturn(Map.of("c1", preferredTerm("c1")));

        ActionsLotAltLabelValidationResult result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1", true);

        assertTrue(result.success());
        assertEquals(2, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("syn1", result.validCandidates().get(0).label());
        assertEquals("fr", result.validCandidates().get(0).lang());
        assertEquals("syn2", result.validCandidates().get(1).label());
    }

    @Test
    void validate_rejectsRowWithoutAltLabels() {
        String csv = """
                localId,skos:altLabel@fr
                c1,
                """;
        ActionsLotTestIds.existing(persistence, "c1");
        when(persistence.findPreferredTermsByConceptIds(any(), eq("TH1")))
                .thenReturn(Map.of("c1", preferredTerm("c1")));

        ActionsLotAltLabelValidationResult result = service.validate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1", true);

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("skos:altLabel", result.errors().get(0).column());
    }

    @Test
    void applyImport_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyImport(List.of(), "TH1", 7, false);
        assertFalse(result.success());
    }

    @Test
    void applyImport_addsSynonyms() {
        ActionsLotAltLabelCandidate candidate = new ActionsLotAltLabelCandidate(2, "c1", "c1", "Minou", "fr");
        when(persistence.findPreferredTermsByConceptIds(any(), eq("TH1")))
                .thenReturn(Map.of("c1", preferredTerm("c1")));
        when(persistence.addNonPreferredTerm(any(), eq(7))).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7, false);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence, never()).deleteAllByConceptAndThesaurus(anyString(), anyString());
        verify(persistence).addNonPreferredTerm(any(), eq(7));
    }

    @Test
    void applyImport_clearBeforeDeletesThenAdds() {
        ActionsLotAltLabelCandidate candidate = new ActionsLotAltLabelCandidate(2, "c1", "c1", "Minou", "fr");
        when(persistence.findPreferredTermsByConceptIds(any(), eq("TH1")))
                .thenReturn(Map.of("c1", preferredTerm("c1")));
        when(persistence.addNonPreferredTerm(any(), anyInt())).thenReturn(true);

        ActionsLotApplyResult result = service.applyImport(List.of(candidate), "TH1", 7, true);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).deleteAllByConceptAndThesaurus("c1", "TH1");
    }

    @Test
    void applyDelete_removesSynonyms() {
        ActionsLotAltLabelCandidate candidate = new ActionsLotAltLabelCandidate(2, "c1", "c1", "Minou", "fr");
        when(persistence.findPreferredTermsByConceptIds(any(), eq("TH1")))
                .thenReturn(Map.of("c1", preferredTerm("c1")));

        ActionsLotApplyResult result = service.applyDelete(List.of(candidate), "TH1", 7);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).deleteNonPreferredTerm("T1", "fr", "Minou", "TH1", 7);
    }

    @Test
    void applyDelete_withoutThesaurus_fails() {
        assertFalse(service.applyDelete(List.of(new ActionsLotAltLabelCandidate(2, "c1", "c1", "x", "fr")), " ", 1).success());
    }

    @Test
    void templateBytes_isNotEmpty() {
        assertTrue(service.templateBytes().length > 0);
    }

    private static PreferredTerm preferredTerm(String conceptId) {
        return PreferredTerm.builder()
                .idConcept(conceptId)
                .idThesaurus("TH1")
                .idTerm("T1")
                .build();
    }
}
