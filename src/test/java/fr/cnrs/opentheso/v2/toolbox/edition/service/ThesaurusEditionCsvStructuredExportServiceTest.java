package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvStructuredExportPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionCsvStructuredExportServiceTest {

    @Mock
    private ThesaurusEditionCsvStructuredExportPersistence persistence;
    @Mock
    private ThesaurusCsvWriter csvWriter;

    private ThesaurusEditionCsvStructuredExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionCsvStructuredExportService(persistence, csvWriter);
    }

    @Test
    void exportThesaurus_rejectsBlankIds() {
        assertThrowsExactly(IllegalStateException.class, () -> service.exportThesaurus(" ", "T", "fr"));
        assertThrowsExactly(IllegalStateException.class, () -> service.exportThesaurus("TH1", "T", " "));
    }

    @Test
    void exportThesaurus_buildsCsvStream() {
        var matrix = new String[][]{{"a", "b"}};
        when(persistence.buildStructuredMatrix("TH1", "fr")).thenReturn(matrix);
        when(csvWriter.importTreeCsv(matrix, ';')).thenReturn(new byte[]{1, 2, 3});

        var content = service.exportThesaurus("TH1", "Animaux", "fr");

        assertNotNull(content);
        assertEquals("Animaux_TH1.csv", content.getName());
    }
}
