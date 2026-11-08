package fr.cnrs.opentheso.candidat.api;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.candidat.api.dto.CandidateNoteRequest;
import fr.cnrs.opentheso.candidat.api.dto.CreateCandidateRequest;
import fr.cnrs.opentheso.candidat.api.dto.ExportCandidatesRequest;
import fr.cnrs.opentheso.candidat.exception.CandidateNotFoundException;
import fr.cnrs.opentheso.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatApiControllerTest {

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

    private CandidatApiController controller;

    @BeforeEach
    void setUp() {
        controller = new CandidatApiController(
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
    void createCandidate_rejectsDuplicatePreferredLabel() {
        when(candidatMutationService.preferredLabelExists("TH1", "Amphore")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createCandidate("api-key", null, "TH1",
                        new CreateCandidateRequest("Amphore", null, "fr", null)));

        assertEquals(409, error.getStatusCode().value());
        verify(candidatMutationService, never()).saveNewCandidat(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void createCandidate_rejectsUnknownNoteType() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.createCandidate("api-key", null, "TH1",
                        new CreateCandidateRequest("Amphore", null, "fr",
                                List.of(new CandidateNoteRequest("custom", "texte", "fr", null)))));

        assertEquals(400, error.getStatusCode().value());
        verify(candidatMutationService, never()).saveNewCandidat(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void createCandidate_savesTypedNotes() {
        when(candidatMutationService.preferredLabelExists("TH1", "Amphore")).thenReturn(false);
        when(candidatMutationService.saveNewCandidat(any(), eq("TH1"), eq("fr"), eq(4), any(), eq("fr"), eq("")))
                .thenAnswer(invocation -> {
                    CandidatDto created = invocation.getArgument(0);
                    created.setIdConcepte("C9");
                    return true;
                });
        CandidatDto stored = new CandidatDto();
        stored.setIdConcepte("C9");
        stored.setNomPref("Amphore");
        stored.setLang("fr");
        stored.setStatut(String.valueOf(CandidatStatusCode.PENDING));
        when(candidatReadService.findByConceptId("TH1", "C9", "fr", 4)).thenReturn(Optional.of(stored));

        var response = controller.createCandidate("api-key", null, "TH1",
                new CreateCandidateRequest("Amphore", null, "fr", List.of(
                        new CandidateNoteRequest("scopeNote", "Usage", "en", "BNF"),
                        new CandidateNoteRequest("historyNote", "Historique", null, null))));

        assertEquals("C9", response.conceptId());
        verify(candidatMutationService).addOrUpdateCandidateNote("C9", "en", "TH1", "Usage", "scopeNote", "BNF", 4);
        verify(candidatMutationService).addOrUpdateCandidateNote("C9", "fr", "TH1", "Historique", "historyNote", "", 4);
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
