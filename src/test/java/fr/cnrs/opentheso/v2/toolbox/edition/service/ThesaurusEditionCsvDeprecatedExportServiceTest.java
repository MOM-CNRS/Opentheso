package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionCsvDeprecatedExportServiceTest {

    @Mock
    private ThesaurusCsvWriter thesaurusCsvWriter;

    private ThesaurusEditionCsvDeprecatedExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionCsvDeprecatedExportService(thesaurusCsvWriter);
    }

    @Test
    void exportThesaurus_buildsStreamedContent() {
        when(thesaurusCsvWriter.writeCsvByDeprecated("TH1", "fr", ',')).thenReturn("a,b".getBytes(StandardCharsets.UTF_8));

        var content = service.exportThesaurus("TH1", "Titre", "fr", ',');

        assertNotNull(content);
    }
}
