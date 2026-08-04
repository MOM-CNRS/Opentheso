package fr.cnrs.opentheso.v2.toolbox.export.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvDeprecatedExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvIdExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvStructuredExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionPdfExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionSkosExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionZipExportService;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.StreamedContent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusExportBeanTest {

    @Mock private ThesaurusEditionSkosExportService thesaurusEditionSkosExportService;
    @Mock private ThesaurusEditionCsvExportService thesaurusEditionCsvExportService;
    @Mock private ThesaurusEditionCsvIdExportService thesaurusEditionCsvIdExportService;
    @Mock private ThesaurusEditionCsvStructuredExportService thesaurusEditionCsvStructuredExportService;
    @Mock private ThesaurusEditionPdfExportService thesaurusEditionPdfExportService;
    @Mock private ThesaurusEditionCsvDeprecatedExportService thesaurusEditionCsvDeprecatedExportService;
    @Mock private ThesaurusEditionZipExportService thesaurusEditionZipExportService;
    @Mock private ToolboxExportPersistence toolboxExportPersistence;
    @Mock private ToolboxPreferencePersistence toolboxPreferencePersistence;

    private ThesaurusExportBean bean;

    @BeforeEach
    void setUp() {
        bean = new ThesaurusExportBean(
                thesaurusEditionSkosExportService,
                thesaurusEditionCsvExportService,
                thesaurusEditionCsvIdExportService,
                thesaurusEditionCsvStructuredExportService,
                thesaurusEditionPdfExportService,
                thesaurusEditionCsvDeprecatedExportService,
                thesaurusEditionZipExportService,
                toolboxExportPersistence,
                toolboxPreferencePersistence
        );
    }

    @Test
    void downloadSkos_returnsNullWhenNotPrepared() {
        assertNull(bean.downloadSkos());
    }

    @Test
    void prepare_storesThesaurusIdentifiers() {
        bean.prepare("TH1", "Thésaurus test");

        assertEquals("TH1", bean.getThesaurusId());
        assertEquals("Thésaurus test", bean.getThesaurusTitle());
    }

    @Test
    void downloadSkos_delegatesToExportService() throws Exception {
        StreamedContent content = org.mockito.Mockito.mock(StreamedContent.class);
        bean.prepare("TH1", "Thésaurus test");
        bean.setFormatCode("jsonld");
        when(thesaurusEditionSkosExportService.exportThesaurus(
                "TH1", "Thésaurus test", "jsonld", ThesaurusEditionExportOptions.full()
        )).thenReturn(content);

        assertEquals(content, bean.downloadSkos());
    }

    @Test
    void downloadSkos_showsErrorWhenExportFails() throws Exception {
        bean.prepare("TH1", "Thésaurus test");
        when(thesaurusEditionSkosExportService.exportThesaurus(
                eq("TH1"), eq("Thésaurus test"), eq("rdf"), any(ThesaurusEditionExportOptions.class)
        )).thenThrow(new RuntimeException("boom"));

        try (var messages = mockStatic(MessageUtils.class)) {
            assertNull(bean.downloadSkos());
            messages.verify(() -> MessageUtils.showErrorMessage("Export SKOS impossible"));
        }
    }

    @Test
    void downloadCsv_delegatesToCsvExportService() throws Exception {
        StreamedContent content = org.mockito.Mockito.mock(StreamedContent.class);
        bean.prepare("TH1", "Thésaurus test");
        bean.setCsvDelimiter(";");
        bean.getSelectedLanguageCodes().add("fr");
        when(thesaurusEditionCsvExportService.exportThesaurus(
                eq("TH1"),
                eq("Thésaurus test"),
                eq(';'),
                eq(bean.getSelectedLanguageCodes()),
                any(ThesaurusEditionExportOptions.class)
        )).thenReturn(content);

        assertEquals(content, bean.downloadCsv());
    }

    @Test
    void downloadPdf_delegatesWithImagesAndExportOptions() throws Exception {
        StreamedContent content = org.mockito.Mockito.mock(StreamedContent.class);
        bean.prepare("TH1", "Thésaurus test");
        bean.setPdfLanguage1("fr");
        bean.setPdfLanguage2("en");
        bean.setPdfHierarchical(true);
        bean.setIncludeImages(true);
        bean.setClearHtml(true);
        bean.setFilterByGroup(true);
        bean.setSelectedGroupIds(new java.util.ArrayList<>(List.of("G1")));
        when(thesaurusEditionPdfExportService.exportThesaurus(
                eq("TH1"),
                eq("Thésaurus test"),
                eq("fr"),
                eq("en"),
                eq(true),
                eq(true),
                any(ThesaurusEditionExportOptions.class)
        )).thenReturn(content);

        assertEquals(content, bean.downloadPdf());
    }

    @Test
    void downloadCsvDeprecated_delegatesToDeprecatedExportService() {
        StreamedContent content = org.mockito.Mockito.mock(StreamedContent.class);
        bean.prepare("TH1", "Thésaurus test");
        bean.setDeprecatedLanguage("fr");
        bean.setCsvDelimiter(";");
        when(thesaurusEditionCsvDeprecatedExportService.exportThesaurus("TH1", "Thésaurus test", "fr", ';'))
                .thenReturn(content);

        assertEquals(content, bean.downloadCsvDeprecated());
    }
}
