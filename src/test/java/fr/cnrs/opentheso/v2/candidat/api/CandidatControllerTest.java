package fr.cnrs.opentheso.v2.candidat.api;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.api.dto.ExportCandidatesRequest;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatControllerTest {

    @Mock
    private CandidatAuthSupport candidatAuthSupport;
    @Mock
    private CandidatReadService candidatReadService;
    @Mock
    private CandidatExportService candidatExportService;

    private CandidatController controller;

    @BeforeEach
    void setUp() {
        controller = new CandidatController(candidatAuthSupport, candidatReadService, candidatExportService);
        ReflectionTestUtils.setField(controller, "defaultWorkLanguage", "fr");
        when(candidatAuthSupport.resolveUserId("api-key", null)).thenReturn(4);
    }

    @Test
    void listCandidates_returnsSummaries() {
        CandidatDto candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setNomPref("Label");
        candidat.setLang("fr");
        candidat.setStatut(String.valueOf(CandidatStatusCode.PENDING));
        candidat.setCreatedBy("alice");
        candidat.setCreationDate(new Date());
        when(candidatReadService.searchByStatus("TH1", "fr", CandidatStatusCode.PENDING, null))
                .thenReturn(List.of(candidat));

        var response = controller.listCandidates("api-key", null, "TH1", "pending", null, null);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
        verify(candidatAuthSupport).requireContributor(4, "TH1");
    }

    @Test
    void exportPendingCandidates_returnsBytes() throws Exception {
        CandidatDto candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.PENDING))
                .thenReturn(List.of(candidat));
        when(candidatExportService.exportPendingCandidates(eq("TH1"), any(), eq("skos"), any()))
                .thenReturn(new CandidatExportService.ExportResult(new byte[]{1, 2}, "candidats.rdf", "application/xml"));

        var response = controller.exportPendingCandidates(
                "api-key", null, "TH1", null, new ExportCandidatesRequest("skos"));

        assertEquals(2, response.getBody().length);
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("candidats.rdf"));
    }
}
