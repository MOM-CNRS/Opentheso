package fr.cnrs.opentheso.v2.proposition.api;

import fr.cnrs.opentheso.v2.proposition.api.dto.ProcessPropositionRequest;
import fr.cnrs.opentheso.v2.proposition.api.dto.SubmitPropositionRequest;
import fr.cnrs.opentheso.v2.proposition.exception.PropositionNotFoundException;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionReadService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionControllerV2Test {

    @Mock
    private PropositionAuthSupport propositionAuthSupport;
    @Mock
    private PropositionReadService propositionReadService;
    @Mock
    private PropositionMutationService propositionMutationService;
    @Mock
    private PropositionDraftService propositionDraftService;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;

    private PropositionControllerV2 controller;

    @BeforeEach
    void setUp() {
        controller = new PropositionControllerV2(
                propositionAuthSupport,
                propositionReadService,
                propositionMutationService,
                propositionDraftService,
                thesaurusPreferenceService
        );
        ReflectionTestUtils.setField(controller, "defaultWorkLanguage", "fr");
        when(propositionAuthSupport.resolveUserId("api-key", null)).thenReturn(4);
    }

    @Test
    void listPropositions_returnsPendingSummaries() {
        when(propositionReadService.listPending("TH1")).thenReturn(List.of(
                new PropositionSummary(12, "TH1", "C1", "Label", "alice", "a@x.fr",
                        "ENVOYER", "20-09-2026 09:00", "fr", "fr")
        ));

        var response = controller.listPropositions("api-key", null, "TH1", "pending");

        assertEquals(1, response.size());
        assertEquals(12, response.get(0).id());
        assertEquals("pending", response.get(0).status());
        verify(propositionAuthSupport).requireBoard(4, "TH1");
    }

    @Test
    void pendingCount_returnsCount() {
        when(propositionReadService.countPending("TH1")).thenReturn(3);

        var response = controller.pendingCount("api-key", null, "TH1");

        assertEquals(3, response.count());
        verify(propositionAuthSupport).requireBoard(4, "TH1");
    }

    @Test
    void getProposition_marksReadByDefault() {
        var detail = detail(12, "ENVOYER");
        var readDetail = detail(12, "LU");
        when(propositionReadService.findDetail(12)).thenReturn(detail, readDetail);

        var response = controller.getProposition("api-key", null, "TH1", 12, true);

        assertEquals("read", response.status());
        verify(propositionMutationService).markRead(12);
        verify(propositionAuthSupport).requireBoard(4, "TH1");
    }

    @Test
    void getProposition_rejectsOtherThesaurus() {
        when(propositionReadService.findDetail(12)).thenReturn(detail(12, "ENVOYER", "OTHER"));

        assertThrows(PropositionNotFoundException.class,
                () -> controller.getProposition("api-key", null, "TH1", 12, false));
        verify(propositionMutationService, never()).markRead(anyInt());
    }

    @Test
    void submitProposition_createsAndReturnsDetail() {
        when(propositionAuthSupport.resolveUsername(4)).thenReturn("alice");
        when(propositionAuthSupport.resolveEmail(4)).thenReturn("alice@example.com");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(propositionMutationService.submitDraft(any(PropositionSubmission.class)))
                .thenReturn(Optional.of(42));
        when(propositionReadService.findDetail(42)).thenReturn(detail(42, "ENVOYER"));

        var request = new SubmitPropositionRequest(
                "C1", "Label", "fr", "Please update", null, null, null, null, null);

        var response = controller.submitProposition("api-key", null, "TH1", request);

        assertEquals(42, response.id());
        ArgumentCaptor<PropositionSubmission> captor = ArgumentCaptor.forClass(PropositionSubmission.class);
        verify(propositionMutationService).submitDraft(captor.capture());
        assertEquals("alice", captor.getValue().authorName());
        assertEquals("alice@example.com", captor.getValue().authorEmail());
        assertEquals(true, captor.getValue().allowMultiplePending());
        verify(propositionAuthSupport).requireSubmit("TH1", "fr");
    }

    @Test
    void submitProposition_conflictWhenDuplicate() {
        when(propositionAuthSupport.resolveUsername(4)).thenReturn("alice");
        when(propositionAuthSupport.resolveEmail(4)).thenReturn("alice@example.com");
        when(propositionMutationService.submitDraft(any(PropositionSubmission.class)))
                .thenReturn(Optional.empty());

        var request = new SubmitPropositionRequest(
                "C1", "Label", "fr", "Please update", null, null, null, null, null);

        assertThrows(ResponseStatusException.class,
                () -> controller.submitProposition("api-key", null, "TH1", request));
    }

    @Test
    void processProposition_approvesWithDraft() {
        when(propositionReadService.findDetail(12)).thenReturn(detail(12, "ENVOYER"), detail(12, "APPROUVER"));
        when(propositionAuthSupport.resolveUsername(4)).thenReturn("manager");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(propositionDraftService.loadDraftChanges(12)).thenReturn(new PropositionDraft());

        var response = controller.processProposition(
                "api-key", null, "TH1", 12, new ProcessPropositionRequest("approve", "ok"));

        assertEquals("approved", response.status());
        verify(propositionAuthSupport).requireDecide(4, "TH1", "a@x.fr");
        verify(propositionMutationService).approve(eq(12), eq("manager"), eq("ok"), eq("Label"), eq("TH1"));
        verify(propositionDraftService, never()).applyAcceptedChanges(any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void processProposition_doesNotApproveWhenApplyFails() {
        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New", "Old", false));
        when(propositionReadService.findDetail(12)).thenReturn(detail(12, "ENVOYER"));
        when(propositionAuthSupport.resolveUsername(4)).thenReturn("manager");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(propositionDraftService.loadDraftChanges(12)).thenReturn(draft);
        when(propositionDraftService.applyAcceptedChanges(any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(List.of("Le libellé existe déjà"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.processProposition(
                "api-key", null, "TH1", 12, new ProcessPropositionRequest("approve", "ok")));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatusCode());
        verify(propositionMutationService, never()).approve(anyInt(), any(), any(), any(), any());
    }

    @Test
    void deleteProposition_removesWhenAllowed() {
        when(propositionReadService.findDetail(12)).thenReturn(detail(12, "ENVOYER"));

        controller.deleteProposition("api-key", null, "TH1", 12);

        verify(propositionAuthSupport).requireDecide(4, "TH1", "a@x.fr");
        verify(propositionMutationService).delete(12);
    }

    private static PropositionDetail detail(int id, String status) {
        return detail(id, status, "TH1");
    }

    private static PropositionDetail detail(int id, String status, String thesaurusId) {
        return new PropositionDetail(
                id, thesaurusId, "C1", "Label", "fr", "fr",
                "alice", "a@x.fr", "comment", status, "20-09-2026 09:00", null, null);
    }
}
