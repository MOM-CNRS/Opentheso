package fr.cnrs.opentheso.v2.publicapi.thesaurus.api;

import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusFlatEntryResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusLanguagesResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusLastUpdateResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusTopConceptResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.service.ThesaurusPublicReadService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusPublicControllerTest {

    @Mock
    private ThesaurusPublicReadService thesaurusPublicReadService;

    private ThesaurusPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ThesaurusPublicController(thesaurusPublicReadService);
    }

    @Test
    void exportThesaurus_returnsFileResponse() throws Exception {
        when(thesaurusPublicReadService.exportThesaurus("TH1", "skos"))
                .thenReturn(new ExportResult(new byte[]{1, 2, 3}, "TH1.rdf", "application/xml"));

        var response = controller.exportThesaurus("TH1", "skos");

        assertEquals(3, response.getBody().length);
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("TH1.rdf"));
    }

    @Test
    void flatList_returnsServiceResult() {
        var entries = List.of(new ThesaurusFlatEntryResponse("C1", "Label"));
        when(thesaurusPublicReadService.flatList("TH1", "fr")).thenReturn(entries);

        var response = controller.flatList("TH1", "fr");

        assertEquals(entries, response);
    }

    @Test
    void topConcepts_returnsServiceResult() {
        var entries = List.of(new ThesaurusTopConceptResponse("C1", "ark1", "hdl1", List.of()));
        when(thesaurusPublicReadService.topConcepts("TH1")).thenReturn(entries);

        var response = controller.topConcepts("TH1");

        assertEquals(entries, response);
    }

    @Test
    void lastUpdate_returnsServiceResult() {
        var expected = new ThesaurusLastUpdateResponse(Instant.EPOCH);
        when(thesaurusPublicReadService.lastUpdate("TH1")).thenReturn(expected);

        var response = controller.lastUpdate("TH1");

        assertEquals(expected, response);
    }

    @Test
    void listLang_returnsServiceResult() {
        var expected = new ThesaurusLanguagesResponse(List.of("fr", "en"));
        when(thesaurusPublicReadService.usedLanguages("TH1")).thenReturn(expected);

        var response = controller.listLang("TH1");

        assertEquals(expected, response);
    }
}
