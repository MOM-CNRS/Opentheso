package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionReadService;
import fr.cnrs.opentheso.v2.proposition.policy.PropositionAccessPolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionBeanTest {

    @Mock private PropositionReadService propositionReadService;
    @Mock private PropositionMutationService propositionMutationService;
    @Mock private PropositionDraftService propositionDraftService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private PropositionAccessPolicy propositionAccessPolicy;
    @Mock private V2LocaleBean localeBean;
    @Mock private ObjectProvider<ThesaurusViewBean> thesaurusViewBeanProvider;
    @Mock private ThesaurusViewBean thesaurusViewBean;

    private PropositionBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    private static final PropositionDetail DETAIL = new PropositionDetail(
            5, "TH1", "C1", "Concept 1", "fr", "fr", "Author", "a@b.fr",
            "comment", "ENVOYER", "01-01-2024", null, null);

    private static final PropositionDetail DETAIL_LU = new PropositionDetail(
            5, "TH1", "C1", "Concept 1", "fr", "fr", "Author", "a@b.fr",
            "comment", "LU", "01-01-2024", null, null);

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        lenient().when(thesaurusViewBeanProvider.getIfAvailable()).thenReturn(thesaurusViewBean);
        bean = new PropositionBean(propositionReadService, propositionMutationService,
                propositionDraftService, thesaurusContext, userSession, propositionAccessPolicy, localeBean,
                thesaurusViewBeanProvider);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    @Test
    void openReview_loadsDraftAndDefaultsAcceptedFlagsFromPresentChanges() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL, DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();

        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New label", "Old label", false));
        draft.setNoteChange(new PropositionFieldChange(
                PropositionFieldCategory.DEFINITION, PropositionFieldAction.ADD, "fr", "Def", null, false));
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(draft);

        bean.openReview(proposition);

        assertTrue(bean.isConsultation());
        assertTrue(bean.isPrefTermeAccepted());
        assertFalse(bean.isVarianteAccepted());
        assertTrue(bean.getNoteChangeEntries().stream()
                .anyMatch(e -> e.getChange().category() == PropositionFieldCategory.DEFINITION && e.isAccepted()));
        assertEquals(2, bean.getReviewFields().size());
        assertEquals("Old label", bean.getReviewFields().get(0).getOldValue());
        assertEquals("New label", bean.getReviewFields().get(0).getNewValue());
        assertFalse(bean.getReviewFields().get(1).isOldPresent());
        assertEquals("Def", bean.getReviewFields().get(1).getNewValue());
        verify(propositionMutationService).markRead(5);
    }

    @Test
    void openReview_doesNothingWhenPropositionNull() {
        bean.openReview(null);

        verify(propositionReadService, never()).findDetail(anyInt());
        assertFalse(bean.isConsultation());
    }

    @Test
    void approveSelected_appliesAcceptedChangesBeforeApproving() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL, DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New label", "Old label", false));
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(draft);
        bean.openReview(proposition);

        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(propositionDraftService.applyAcceptedChanges(any(), eq("TH1"), eq("C1"), eq("fr"), eq(7), eq("admin"), any()))
                .thenReturn(java.util.List.of());

        bean.approveSelected();

        verify(propositionDraftService).applyAcceptedChanges(any(), eq("TH1"), eq("C1"), eq("fr"), eq(7), eq("admin"), any());
        verify(propositionMutationService).approve(eq(5), eq("admin"), any(), eq("Concept 1"), any());
        verify(thesaurusViewBean).reloadSelectedConcept();
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Proposition approuvée"));
        assertFalse(bean.isConsultation());
    }

    @Test
    void approveSelected_appliesOnlyCheckedReviewFields() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL, DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        var draft = new PropositionDraft();
        var kept = new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Keep", null, false);
        var skipped = new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Skip", null, false);
        draft.getSynonymChanges().add(kept);
        draft.getSynonymChanges().add(skipped);
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(draft);
        bean.openReview(proposition);
        bean.getReviewFields().get(1).setAccepted(false);

        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(propositionDraftService.applyAcceptedChanges(any(), eq("TH1"), eq("C1"), eq("fr"), eq(7), eq("admin"), any()))
                .thenReturn(java.util.List.of());

        bean.approveSelected();

        ArgumentCaptor<PropositionDraft> captor = ArgumentCaptor.forClass(PropositionDraft.class);
        verify(propositionDraftService).applyAcceptedChanges(
                captor.capture(), eq("TH1"), eq("C1"), eq("fr"), eq(7), eq("admin"), any());
        assertEquals(1, captor.getValue().getSynonymChanges().size());
        assertEquals("Keep", captor.getValue().getSynonymChanges().get(0).value());
        verify(propositionMutationService).approve(eq(5), eq("admin"), any(), any(), any());
    }

    @Test
    void approveSelected_doesNotApproveWhenApplyFails() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL, DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New label", "Old label", false));
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(draft);
        bean.openReview(proposition);

        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(propositionDraftService.applyAcceptedChanges(any(), eq("TH1"), eq("C1"), eq("fr"), eq(7), eq("admin"), any()))
                .thenReturn(java.util.List.of("Le libellé existe déjà"));

        bean.approveSelected();

        verify(propositionMutationService, never()).approve(anyInt(), any(), any(), any(), any());
        verify(thesaurusViewBean, never()).reloadSelectedConcept();
        assertTrue(bean.isConsultation());
        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Le libellé existe déjà"));
    }

    @Test
    void approveSelected_skipsApplyWhenDraftEmpty() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL, DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());
        bean.openReview(proposition);

        when(userSession.getCurrentUsername()).thenReturn("admin");

        bean.approveSelected();

        verify(propositionDraftService, never()).applyAcceptedChanges(any(), any(), any(), any(), anyInt(), any(), any());
        verify(propositionMutationService).approve(eq(5), eq("admin"), any(), any(), any());
        verify(thesaurusViewBean).reloadSelectedConcept();
    }

    @Test
    void approveSelected_doesNothingWhenNoSelection() {
        bean.approveSelected();

        verify(propositionMutationService, never()).approve(anyInt(), any(), any(), any(), any());
    }

    @Test
    void refresh_loadsPendingAndTotalCounts() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionReadService.countPending("TH1")).thenReturn(3);
        when(propositionReadService.countAll("TH1")).thenReturn(11);
        when(propositionReadService.listPending("TH1")).thenReturn(java.util.List.of());

        bean.refresh();

        assertEquals(3, bean.getPendingCount());
        assertEquals(11, bean.getTotalCount());
        assertFalse(bean.isShowAll());
    }

    @Test
    void refresh_loadsOnlyTheSelectedThesaurusList() {
        var current = new PropositionSummary(1, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionReadService.countPending("TH1")).thenReturn(1);
        when(propositionReadService.countAll("TH1")).thenReturn(1);
        when(propositionReadService.listPending("TH1")).thenReturn(java.util.List.of(current));

        bean.refresh();

        assertEquals(1, bean.getPropositions().size());
        assertEquals("TH1", bean.getPropositions().get(0).thesaurusId());
        assertEquals("proposition/propositions.xhtml?idt=TH1", bean.getBoardHref());
        assertEquals(
                "proposition/review.xhtml?idt=TH1&prop=1",
                bean.consultationHref(current));
    }

    @Test
    void refreshPendingCount_reloadsListWhenThesaurusChanges() {
        var first = new PropositionSummary(1, "TH1", "C1", "Old", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        var second = new PropositionSummary(2, "TH2", "C2", "New", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        stubManagerRights();
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(propositionReadService.countPending("TH1")).thenReturn(1);
        when(propositionReadService.countAll("TH1")).thenReturn(1);
        when(propositionReadService.listPending("TH1")).thenReturn(java.util.List.of(first));
        bean.refresh();

        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH2");
        when(propositionReadService.countPending("TH2")).thenReturn(1);
        when(propositionReadService.countAll("TH2")).thenReturn(4);
        when(propositionReadService.listPending("TH2")).thenReturn(java.util.List.of(second));

        bean.refreshPendingCount();

        assertEquals(1, bean.getPropositions().size());
        assertEquals("TH2", bean.getPropositions().get(0).thesaurusId());
        assertEquals(1, bean.getPendingCount());
        assertEquals(4, bean.getTotalCount());
    }

    @Test
    void refresh_usesThesaurusViewBeanIdWhenContextIsStillEmpty() {
        var current = new PropositionSummary(1, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        stubManagerRights();
        when(thesaurusViewBean.getId()).thenReturn("TH1");
        when(propositionReadService.countPending("TH1")).thenReturn(1);
        when(propositionReadService.countAll("TH1")).thenReturn(3);
        when(propositionReadService.listPending("TH1")).thenReturn(java.util.List.of(current));

        bean.refresh();

        assertEquals(1, bean.getPropositions().size());
        assertEquals(1, bean.getPendingCount());
        assertEquals(3, bean.getTotalCount());
    }

    @Test
    void refreshPendingCount_reloadsEmptySessionListForSameThesaurus() {
        var current = new PropositionSummary(1, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        stubManagerRights();
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(propositionReadService.countPending("TH1")).thenReturn(1);
        when(propositionReadService.countAll("TH1")).thenReturn(1);
        when(propositionReadService.listPending("TH1")).thenReturn(java.util.List.of(current));

        bean.refreshPendingCount();

        assertEquals(1, bean.getPropositions().size());
        assertEquals("TH1", bean.getLoadedThesaurusId());
    }

    @Test
    void openBoard_refreshesListAndBothTabCounts() {
        var current = new PropositionSummary(1, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        stubManagerRights();
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(propositionReadService.countPending("TH1")).thenReturn(2);
        when(propositionReadService.countAll("TH1")).thenReturn(5);
        when(propositionReadService.listPending("TH1")).thenReturn(java.util.List.of(current));

        bean.openBoard();

        assertEquals(1, bean.getPropositions().size());
        assertEquals(2, bean.getPendingCount());
        assertEquals(5, bean.getTotalCount());
    }

    @Test
    void refresh_clearsListWhenNoThesaurusSelected() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn(" ");
        stubManagerRights();

        bean.refresh();

        assertTrue(bean.getPropositions().isEmpty());
        assertEquals(0, bean.getPendingCount());
        assertFalse(bean.isThesaurusSelected());
        assertFalse(bean.isBoardLinkVisible());
    }

    @Test
    void boardLinkVisible_requiresSelectedThesaurusAndBoardAccess() {
        stubManagerRights();
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        assertTrue(bean.isThesaurusSelected());
        assertTrue(bean.isBoardLinkVisible());

        when(thesaurusContext.resolveThesaurusId()).thenReturn(" ");
        assertFalse(bean.isBoardLinkVisible());

        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(propositionAccessPolicy.canAccessBoard(any(UserSession.class), any())).thenReturn(false);
        assertFalse(bean.isBoardLinkVisible());
    }

    @Test
    void openDedicatedReview_loadsPropositionFromRequestedId() {
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());
        bean.setRequestedPropositionId("5");

        bean.openDedicatedReview();

        assertTrue(bean.isConsultation());
        assertTrue(bean.isDedicatedReview());
        assertEquals(5, bean.getSelectedProposition().id());
        verify(propositionMutationService).markRead(5);
    }

    @Test
    void openDedicatedReview_usesPropositionThesaurusWhenContextIsEmpty() {
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL, DETAIL_LU);
        when(thesaurusViewBean.getId()).thenReturn(null);
        when(thesaurusContext.resolveThesaurusId()).thenReturn(" ");
        stubManagerRights();
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());
        bean.setRequestedPropositionId("5");

        bean.openDedicatedReview();

        assertTrue(bean.isConsultation());
        assertEquals("TH1", bean.getSelectedProposition().thesaurusId());
    }

    @Test
    void openDedicatedReview_doesNothingWithoutRequestedId() {
        bean.openDedicatedReview();

        verify(propositionReadService, never()).findDetail(anyInt());
        assertFalse(bean.isConsultation());
    }

    @Test
    void clearConsultation_clearsRequestedPropositionId() {
        bean.setRequestedPropositionId("5");
        bean.setConsultation(true);

        bean.clearConsultation();

        assertFalse(bean.isConsultation());
        assertNull(bean.getRequestedPropositionId());
    }

    @Test
    void sameUser_hidesDecisionButtonsAndShowsAuthorMessage() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "LU", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());
        bean.openReview(proposition);

        when(propositionAccessPolicy.isSameAuthor(userSession, "a@b.fr", "Author")).thenReturn(true);

        assertTrue(bean.isSameUser());
        assertTrue(bean.isShowAuthorCannotDecide());
        assertFalse(bean.isShowDecisionButtons());
        assertFalse(bean.isCanMakeAction());
    }

    @Test
    void approveSelected_doesNothingWhenAuthor() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "LU", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());
        bean.openReview(proposition);

        when(propositionAccessPolicy.isSameAuthor(userSession, "a@b.fr", "Author")).thenReturn(true);
        when(localeBean.getMsg("proposition.alertSameUser")).thenReturn("Vous ne pouvez pas valider votre propre proposition");

        bean.approveSelected();

        verify(propositionMutationService, never()).approve(anyInt(), any(), any(), any(), any());
        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Vous ne pouvez pas valider votre propre proposition"));
        assertTrue(bean.isConsultation());
    }

    @Test
    void addReviewField_appendsAcceptedAddChange() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "LU", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());
        bean.openReview(proposition);

        bean.setNewReviewCategory("SYNONYME");
        bean.setNewReviewLang("en");
        bean.setNewReviewValue("New synonym");
        bean.addReviewField();

        assertEquals(1, bean.getReviewFields().size());
        assertEquals("New synonym", bean.getReviewFields().get(0).getNewValue());
        assertTrue(bean.getReviewFields().get(0).isAccepted());
    }

    @Test
    void removeReviewField_excludesValueFromApproval() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "LU", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        var draft = new PropositionDraft();
        draft.getSynonymChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Keep", null, false));
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(draft);
        bean.openReview(proposition);
        bean.getReviewFields().get(0).setNewValue("Keep edited");

        bean.removeReviewField(bean.getReviewFields().get(0));
        when(userSession.getCurrentUsername()).thenReturn("admin");

        bean.approveSelected();

        verify(propositionDraftService, never()).applyAcceptedChanges(any(), any(), any(), any(), anyInt(), any(), any());
        verify(propositionMutationService).approve(eq(5), eq("admin"), any(), any(), any());
    }

    @Test
    void showButtonDecision_trueWhenPending() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "LU", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL_LU);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        stubManagerRights();
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());

        bean.openReview(proposition);

        assertTrue(bean.isShowButtonDecision());
        assertTrue(bean.isShowDecisionButtons());
        assertFalse(bean.isShowAuthorCannotDecide());
    }

    private void stubManagerRights() {
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(propositionAccessPolicy.canAccessBoard(any(UserSession.class), any())).thenReturn(true);
        lenient().when(propositionAccessPolicy.canReview(any(UserSession.class), any(), any())).thenReturn(true);
        lenient().when(propositionAccessPolicy.canDecide(any(UserSession.class), any(), any(), any())).thenReturn(true);
        lenient().when(propositionAccessPolicy.isSameAuthor(any(), any(), any())).thenReturn(false);
        lenient().when(localeBean.getMsg(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(propositionReadService.listPending(any())).thenReturn(java.util.List.of());
        lenient().when(propositionReadService.listAll(any())).thenReturn(java.util.List.of());
        lenient().when(propositionReadService.countPending(any())).thenReturn(0);
        lenient().when(propositionReadService.countAll(any())).thenReturn(0);
    }
}
