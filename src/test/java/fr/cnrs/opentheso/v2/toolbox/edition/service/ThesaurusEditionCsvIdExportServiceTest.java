package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionCsvIdExportServiceTest {

    @Mock
    private ThesaurusCsvWriter csvWriter;

    private ThesaurusEditionCsvIdExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionCsvIdExportService(csvWriter);
    }

    @Test
    void exportThesaurus_rejectsBlankThesaurusId() {
        assertThrowsExactly(IllegalStateException.class, () ->
                service.exportThesaurus(" ", "T", "fr", ',', false, List.of()));
    }

    @Test
    void exportThesaurus_buildsCsvStream() {
        when(csvWriter.writeCsvById("TH1", "fr", null, ',')).thenReturn(new byte[]{1, 2});

        var content = service.exportThesaurus("TH1", "Animaux", "fr", ',', false, List.of("G1"));

        assertNotNull(content);
        assertEquals("Animaux_TH1.csv", content.getName());
    }

    @Test
    void exportThesaurus_filtersByGroupWhenRequested() {
        when(csvWriter.writeCsvById("TH1", "fr", List.of("G1"), ';')).thenReturn(new byte[]{9});

        var content = service.exportThesaurus("TH1", null, "fr", ';', true, List.of("G1"));

        assertEquals("TH1_TH1.csv", content.getName());
    }
}
