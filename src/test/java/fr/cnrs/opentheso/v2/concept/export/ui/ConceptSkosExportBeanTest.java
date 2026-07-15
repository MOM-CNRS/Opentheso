package fr.cnrs.opentheso.v2.concept.export.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.export.service.ConceptSkosExportService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSkosExportBeanTest {

    @Mock
    private ConceptSkosExportService conceptSkosExportService;

    private ConceptSkosExportBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptSkosExportBean(conceptSkosExportService);
    }

    @Test
    void downloadConcept_returnsNullWhenNoSelection() {
        assertNull(bean.downloadConcept("skos"));
    }

    @Test
    void prepare_storesConceptIdentifiers() {
        bean.prepare("TH1", "C1");

        assertEquals("TH1", bean.getThesaurusId());
        assertEquals("C1", bean.getConceptId());
    }

    @Test
    void downloadConcept_buildsStreamedContent() throws Exception {
        bean.prepare("TH1", "C1");
        when(conceptSkosExportService.exportConcept("TH1", "C1", "skos"))
                .thenReturn(new ExportResult(new byte[]{1, 2}, "TH1_C1.rdf", "application/rdf+xml"));

        var content = bean.downloadConcept("skos");

        assertNotNull(content);
        assertEquals("TH1_C1.rdf", content.getName());
    }

    @Test
    void downloadConcept_showsErrorWhenExportFails() throws Exception {
        bean.prepare("TH1", "C1");
        when(conceptSkosExportService.exportConcept("TH1", "C1", "skos"))
                .thenThrow(new IllegalStateException("missing prefs"));

        try (var messages = mockStatic(MessageUtils.class)) {
            assertNull(bean.downloadConcept("skos"));
            messages.verify(() -> MessageUtils.showErrorMessage("missing prefs"));
        }
    }

    @Test
    void downloadConcept_showsGenericErrorOnIOException() throws Exception {
        bean.prepare("TH1", "C1");
        when(conceptSkosExportService.exportConcept("TH1", "C1", "skos"))
                .thenThrow(new IOException("io"));

        try (var messages = mockStatic(MessageUtils.class)) {
            assertNull(bean.downloadConcept("skos"));
            messages.verify(() -> MessageUtils.showErrorMessage("Export SKOS impossible"));
        }
    }
}
