package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvParseResult;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvImportPersistence;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionCsvImportServiceTest {

    @Mock
    private ThesaurusEditionCsvImportPersistence thesaurusEditionCsvImportPersistence;
    @Mock
    private NewThesaurusService newThesaurusService;

    private ThesaurusEditionCsvImportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionCsvImportService(thesaurusEditionCsvImportPersistence, newThesaurusService);
    }

    @Test
    void loadCsvFile_delegatesToPersistence() {
        var concepts = List.<ThesaurusCsvConceptObject>of();
        when(thesaurusEditionCsvImportPersistence.parse(any(), eq(',')))
                .thenReturn(new ThesaurusEditionCsvParseResult(
                        concepts, List.of("fr"), 0, "warn", null
                ));

        var result = service.loadCsvFile("a,b".getBytes(), ',');

        assertTrue(result.success());
        assertEquals("warn", result.warning());
    }

    @Test
    void loadCsvFile_returnsErrorFromPersistence() {
        when(thesaurusEditionCsvImportPersistence.parse(any(), eq(';')))
                .thenReturn(ThesaurusEditionCsvParseResult.error("bad csv"));

        var result = service.loadCsvFile("x".getBytes(), ';');

        assertFalse(result.success());
        assertEquals("bad csv", result.error());
    }

    @Test
    void importNewThesaurus_surfacesPersistenceFailure() {
        when(thesaurusEditionCsvImportPersistence.importNewThesaurus(
                any(), any(), any(), any(), anyInt(), any(), any(), any(), any()
        )).thenReturn(ThesaurusEditionCsvImportResult.error("failed"));

        assertThrows(IllegalStateException.class, () -> service.importNewThesaurus(
                "Animaux", "fr", "yyyy-MM-dd", 7, "user", true, null, List.of(), List.of(), ""
        ));
    }

    @Test
    void importNewThesaurus_returnsCreatedThesaurusId() {
        when(thesaurusEditionCsvImportPersistence.importNewThesaurus(
                any(), any(), any(), any(), anyInt(), any(), any(), any(), any()
        )).thenReturn(new ThesaurusEditionCsvImportResult("TH42", 3, ""));

        var outcome = service.importNewThesaurus(
                "Animaux", "fr", "yyyy-MM-dd", 7, "user", true, 7, List.of(), List.of("fr"), ""
        );

        assertEquals("TH42", outcome.thesaurusId());
        assertEquals(3, outcome.importedConcepts());
    }
}
