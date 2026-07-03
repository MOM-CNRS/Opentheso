package fr.cnrs.opentheso.v2.toolbox.export.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.toolbox.export.service.ThesaurusSkosExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.StreamedContent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusExportBeanTest {

    @Mock
    private ThesaurusSkosExportService thesaurusSkosExportService;

    private ThesaurusExportBean bean;

    @BeforeEach
    void setUp() {
        bean = new ThesaurusExportBean(thesaurusSkosExportService);
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
        when(thesaurusSkosExportService.exportThesaurus("TH1", "Thésaurus test", "jsonld"))
                .thenReturn(content);

        assertEquals(content, bean.downloadSkos());
    }

    @Test
    void downloadSkos_showsErrorWhenExportFails() throws Exception {
        bean.prepare("TH1", "Thésaurus test");
        when(thesaurusSkosExportService.exportThesaurus("TH1", "Thésaurus test", "rdf"))
                .thenThrow(new RuntimeException("boom"));

        try (var messages = mockStatic(MessageUtils.class)) {
            assertNull(bean.downloadSkos());
            messages.verify(() -> MessageUtils.showErrorMessage("Export SKOS impossible"));
        }
    }
}
