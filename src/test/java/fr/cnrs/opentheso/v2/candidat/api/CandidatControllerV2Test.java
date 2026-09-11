package fr.cnrs.opentheso.v2.candidat.api;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.api.dto.ExportCandidatesRequest;
import fr.cnrs.opentheso.v2.candidat.exception.CandidateNotFoundException;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatControllerV2Test {

    @Mock
    private CandidatAuthSupport candidatAuthSupport;
    @Mock
    private CandidatReadService candidatReadService;
    @Mock
    private CandidatMutationService candidatMutationService;
    @Mock
    private CandidatProcessService candidatProcessService;
    @Mock
    private CandidatExportService candidatExportService;
    @Mock
    private ThesaurusPreferencesProvider thesaurusPreferencesProvider;
    @Mock
    private UserProfileService userProfileService;

    private CandidatControllerV2 controller;

    @BeforeEach
    void setUp() {
        controller = new CandidatControllerV2(
                candidatAuthSupport,
                candidatReadService,
                candidatMutationService,
                candidatProcessService,
                candidatExportService,
                thesaurusPreferencesProvider,
                userProfileService
        );
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
        candidat.setCreationDate(Date.from(java.time.Instant.parse("2024-06-15T12:00:00Z")));
        when(candidatReadService.searchByStatus("TH1", "fr", CandidatStatusCode.PENDING, null))
                .thenReturn(List.of(candidat));

        var response = controller.listCandidates("api-key", null, "TH1", "pending", null, null);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
        verify(candidatAuthSupport).requireContributor(4, "TH1");
    }

    @Test
    void getCandidate_returnsMatchingCandidate() {
        CandidatDto candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setNomPref("Label");
        candidat.setLang("fr");
        candidat.setStatut(String.valueOf(CandidatStatusCode.PENDING));
        when(candidatReadService.findByConceptId("TH1", "C1", "fr", 4))
                .thenReturn(Optional.of(candidat));

        var response = controller.getCandidate("api-key", null, "TH1", "C1", null);

        assertEquals("C1", response.conceptId());
        verify(candidatAuthSupport).requireContributor(4, "TH1");
    }

    @Test
    void getCandidate_throwsWhenMissing() {
        when(candidatReadService.findByConceptId("TH1", "missing", "fr", 4))
                .thenReturn(Optional.empty());

        assertThrows(CandidateNotFoundException.class,
                () -> controller.getCandidate("api-key", null, "TH1", "missing", null));
    }

    @Test
    void exportPendingCandidates_returnsAttachment() throws Exception {
        CandidatDto candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.PENDING))
                .thenReturn(List.of(candidat));
        when(candidatExportService.exportPendingCandidates(eq("TH1"), any(), eq("rdf"), any()))
                .thenReturn(new CandidatExportService.ExportResult(
                        new byte[]{1, 2, 3}, "candidats.rdf", "application/rdf+xml"));

        var response = controller.exportPendingCandidates(
                "api-key", null, "TH1", null, new ExportCandidatesRequest("rdf"));

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("candidats.rdf"));
        assertEquals(3, response.getBody().length);
    }
}
