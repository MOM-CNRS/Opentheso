package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfExportType;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionPdfExportServiceTest {

    @Mock
    private ThesaurusSkosDocumentBuilder documentBuilder;
    @Mock
    private ThesaurusPdfWriter pdfWriter;

    private ThesaurusEditionPdfExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionPdfExportService(documentBuilder, pdfWriter);
    }

    @Test
    void exportThesaurus_rejectsBlankThesaurusId() {
        assertThrowsExactly(IllegalStateException.class, () ->
                service.exportThesaurus(" ", "T", "fr", "en", false, false, ThesaurusEditionExportOptions.full()));
    }

    @Test
    void exportThesaurus_rejectsBlankLanguage() {
        assertThrowsExactly(IllegalStateException.class, () ->
                service.exportThesaurus("TH1", "T", " ", "en", false, false, ThesaurusEditionExportOptions.full()));
    }

    @Test
    void exportThesaurus_buildsPdfStream() throws Exception {
        var document = new SKOSXmlDocument();
        when(documentBuilder.buildDocument(eq("TH1"), any())).thenReturn(document);
        when(pdfWriter.createPdfFile(document, "fr", "en", ThesaurusPdfExportType.HIERARCHIQUE, true))
                .thenReturn(new byte[]{1, 2, 3});

        var content = service.exportThesaurus("TH1", "Animaux", "fr", "en", true, true, null);

        assertNotNull(content);
        assertEquals("Animaux_TH1.pdf", content.getName());
    }

    @Test
    void exportThesaurus_rejectsEmptyPdf() throws Exception {
        when(documentBuilder.buildDocument(eq("TH1"), any())).thenReturn(new SKOSXmlDocument());
        when(pdfWriter.createPdfFile(any(), eq("fr"), eq(""), eq(ThesaurusPdfExportType.ALPHABETIQUE), eq(false)))
                .thenReturn(new byte[0]);

        assertThrowsExactly(IllegalStateException.class, () ->
                service.exportThesaurus("TH1", "T", "fr", null, false, false, ThesaurusEditionExportOptions.full()));
    }
}
