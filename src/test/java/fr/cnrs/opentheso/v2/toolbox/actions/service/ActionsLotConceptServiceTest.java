package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotCompareCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotConceptCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotDeprecateCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionsLotConceptServiceTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;

    @Mock
    private ThesaurusCsvWriter thesaurusCsvWriter;

    private ActionsLotConceptService service;

    @BeforeEach
    void setUp() {
        service = new ActionsLotConceptService(persistence, thesaurusCsvWriter);
    }

    @Test
    void validateAdd_rejectsExistingIdentifier() {
        String csv = """
                URI,skos:prefLabel@fr
                20,France
                """;
        ActionsLotTestIds.existing(persistence, "20");

        ActionsLotImportValidationResult<ActionsLotConceptCandidate> result = service.validateAdd(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.linesRead());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("URI", result.errors().get(0).column());
    }

    @Test
    void validateAdd_acceptsNewConcept() {
        String csv = """
                URI,skos:prefLabel@fr
                21,Lyon
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotImportValidationResult<ActionsLotConceptCandidate> result = service.validateAdd(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals(0, result.errorCount());
        assertEquals("21", result.validCandidates().get(0).identifier());
    }

    @Test
    void applyAdd_insertsNewConcept() {
        String csv = """
                URI,skos:prefLabel@fr
                21,Lyon
                """;
        when(persistence.addConceptV2(eq("TH1"), any(), eq(7), eq("yyyy-MM-dd"))).thenReturn(true);

        ActionsLotApplyResult result = service.applyAdd(
                List.of(new ActionsLotConceptCandidate(2, "21", "skos:concept")),
                csv.getBytes(StandardCharsets.UTF_8),
                0,
                "identifier",
                "TH1",
                7
        );

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).addConceptV2(eq("TH1"), any(), eq(7), eq("yyyy-MM-dd"));
    }

    @Test
    void validateMerge_rejectsUnknownConcept() {
        String csv = """
                identifier,skos:prefLabel@fr
                missing,astre
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotImportValidationResult<ActionsLotConceptCandidate> result = service.validateMerge(
                csv.getBytes(StandardCharsets.UTF_8), 0, "TH1"
        );

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("identifier", result.errors().get(0).column());
    }

    @Test
    void validateMerge_acceptsKnownConcept() {
        String csv = """
                identifier,skos:prefLabel@fr
                4587,astre
                """;
        ActionsLotTestIds.existing(persistence, "4587");

        ActionsLotImportValidationResult<ActionsLotConceptCandidate> result = service.validateMerge(
                csv.getBytes(StandardCharsets.UTF_8), 0, "TH1"
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals("4587", result.validCandidates().get(0).identifier());
    }

    @Test
    void applyMerge_updatesConcept() {
        String csv = """
                identifier,skos:prefLabel@fr
                4587,astre
                """;
        when(persistence.updateConcept(eq("TH1"), any(), eq(7))).thenReturn(true);
        when(persistence.getUserDisplayName(7)).thenReturn("Ada");

        ActionsLotApplyResult result = service.applyMerge(
                List.of(new ActionsLotConceptCandidate(2, "4587", "skos:concept")),
                csv.getBytes(StandardCharsets.UTF_8),
                0,
                "TH1",
                7
        );

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).updateConcept(eq("TH1"), any(), eq(7));
        verify(persistence).updateDateOfConcept("TH1", "4587", 7);
    }

    @Test
    void validateDeprecate_rejectsUnknownConcept() {
        String csv = """
                deprecated,isReplacedBy,skos:note@fr
                missing,,
                """;
        ActionsLotTestIds.existing(persistence);

        ActionsLotImportValidationResult<ActionsLotDeprecateCandidate> result = service.validateDeprecate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("deprecated", result.errors().get(0).column());
    }

    @Test
    void validateDeprecate_rejectsUnknownReplacement() {
        String csv = """
                deprecated,isReplacedBy,skos:note@fr
                c1,missing,note
                """;
        ActionsLotTestIds.existing(persistence, "c1");

        ActionsLotImportValidationResult<ActionsLotDeprecateCandidate> result = service.validateDeprecate(
                csv.getBytes(StandardCharsets.UTF_8), 0, "identifier", "TH1"
        );

        assertTrue(result.success());
        assertEquals(0, result.validCount());
        assertEquals(1, result.errorCount());
        assertEquals("isReplacedBy", result.errors().get(0).column());
    }

    @Test
    void applyDeprecate_deprecatesAndAddsReplacement() {
        ActionsLotDeprecateCandidate candidate = new ActionsLotDeprecateCandidate(
                2, "c1", "c1", "c2", "c2", "remplacé", "fr"
        );
        when(persistence.deprecateConcept("c1", "TH1", 7)).thenReturn(true);
        when(persistence.isNoteExist("c1", "TH1", "fr", "remplacé", "note")).thenReturn(false);
        when(persistence.getUserDisplayName(7)).thenReturn("Ada");

        ActionsLotApplyResult result = service.applyDeprecate(List.of(candidate), "TH1", 7);

        assertTrue(result.success());
        assertEquals(1, result.applied());
        verify(persistence).deprecateConcept("c1", "TH1", 7);
        verify(persistence).addReplacedBy("c1", "TH1", "c2", 7);
        verify(persistence).addNote("c1", "fr", "TH1", "remplacé", "note", "", 7);
    }

    @Test
    void validateCompare_rejectsMultipleColumns() {
        String csv = """
                skos:prefLabel@fr,other
                Espagne,x
                """;

        ActionsLotImportValidationResult<ActionsLotCompareCandidate> result = service.validateCompare(
                csv.getBytes(StandardCharsets.UTF_8), 0
        );

        assertFalse(result.success());
    }

    @Test
    void validateCompare_capturesLangAndLabels() {
        String csv = """
                skos:prefLabel@fr
                Espagne
                """;

        ActionsLotImportValidationResult<ActionsLotCompareCandidate> result = service.validateCompare(
                csv.getBytes(StandardCharsets.UTF_8), 0
        );

        assertTrue(result.success());
        assertEquals(1, result.validCount());
        assertEquals("fr", result.context());
        assertEquals("Espagne", result.validCandidates().get(0).originalPrefLabel());
    }

    @Test
    void compareToCsv_writesHitsAndEmptyRows() {
        NodeSearchMini hit = NodeSearchMini.builder()
                .idConcept("c1")
                .prefLabel("Espagne")
                .concept(true)
                .build();
        when(persistence.searchExactTermForAutocompletion("Espagne", "fr", "TH1")).thenReturn(List.of(hit));
        when(persistence.findConceptsByIds(any(), eq("TH1"))).thenReturn(Map.of("c1", Concept.builder().idArk("ark:/1").build()));
        when(persistence.searchExactTermForAutocompletion("Inconnu", "fr", "TH1")).thenReturn(List.of());
        when(thesaurusCsvWriter.writeCsvFromNodeCompareTheso(anyList(), eq("fr")))
                .thenReturn("csv".getBytes(StandardCharsets.UTF_8));

        byte[] csv = service.compareToCsv(
                List.of(
                        new ActionsLotCompareCandidate(2, "Espagne"),
                        new ActionsLotCompareCandidate(3, "Inconnu")
                ),
                "TH1",
                "fr",
                "exactWord"
        );

        assertArrayEquals("csv".getBytes(StandardCharsets.UTF_8), csv);
        verify(thesaurusCsvWriter).writeCsvFromNodeCompareTheso(anyList(), eq("fr"));
        verify(persistence, never()).searchExactMatch(anyString(), anyString(), anyString(), eq(false));
        verify(persistence, never()).updateDateOfConcept(anyString(), anyString(), anyInt());
    }

    @Test
    void applyAdd_withoutCandidates_fails() {
        ActionsLotApplyResult result = service.applyAdd(List.of(), new byte[0], 0, "identifier", "TH1", 1);
        assertFalse(result.success());
    }
}
