package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvDeprecatedExportSupport;
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
class ThesaurusEditionCsvDeprecatedExportServiceTest {

    @Mock
    private ThesaurusEditionCsvDeprecatedExportSupport thesaurusEditionCsvDeprecatedExportSupport;

    private ThesaurusEditionCsvDeprecatedExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionCsvDeprecatedExportService(thesaurusEditionCsvDeprecatedExportSupport);
    }

    @Test
    void exportThesaurus_rejectsBlankThesaurusId() {
        assertThrowsExactly(IllegalStateException.class, () ->
                service.exportThesaurus(" ", "T", "fr", ';'));
    }

    @Test
    void exportThesaurus_buildsCsvStream() {
        when(thesaurusEditionCsvDeprecatedExportSupport.writeCsvByDeprecated("TH1", "fr", ';'))
                .thenReturn(new byte[]{1, 2});

        var content = service.exportThesaurus("TH1", "Animaux", "fr", ';');

        assertNotNull(content);
        assertEquals("Animaux_TH1.csv", content.getName());
    }
}
