package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionCsvExportServiceTest {

    @Mock
    private ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    @Mock
    private ThesaurusCsvWriter thesaurusCsvWriter;
    @Mock
    private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;

    private ThesaurusEditionCsvExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionCsvExportService(
                thesaurusSkosDocumentBuilder,
                thesaurusCsvWriter,
                toolboxThesaurusPersistence,
                toolboxPreferencePersistence
        );
    }

    @Test
    void exportThesaurus_rejectsBlankThesaurusId() {
        assertThrowsExactly(IllegalStateException.class, () ->
                service.exportThesaurus(" ", "T", ',', List.of("fr"), ThesaurusEditionExportOptions.full()));
    }

    @Test
    void exportThesaurus_buildsCsvStream() throws Exception {
        var lang = NodeLangTheso.builder().code("fr").value("Français").build();
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("fr");
        when(toolboxThesaurusPersistence.loadUsedLanguages("TH1", "fr")).thenReturn(List.of(lang));
        when(thesaurusSkosDocumentBuilder.buildDocument(eq("TH1"), any())).thenReturn(new fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument());
        when(thesaurusCsvWriter.writeCsv(any(), eq(List.of(lang)), eq(','))).thenReturn(new byte[]{1, 2});

        var content = service.exportThesaurus("TH1", "Animaux", ',', List.of("fr"), ThesaurusEditionExportOptions.full());

        assertNotNull(content);
        assertEquals("Animaux_TH1.csv", content.getName());
    }
}
