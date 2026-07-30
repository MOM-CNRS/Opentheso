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
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock private RightsService rightsService;

    private PropositionBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    private static final PropositionDetail DETAIL = new PropositionDetail(
            5, "TH1", "C1", "Concept 1", "fr", "fr", "Author", "a@b.fr",
            "comment", "ENVOYER", "01-01-2024", null, null);

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new PropositionBean(propositionReadService, propositionMutationService,
                propositionDraftService, thesaurusContext, userSession, rightsService);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    @Test
    void openReview_loadsDraftAndDefaultsAcceptedFlagsFromPresentChanges() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL);

        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New label", "Old label", false));
        draft.setNoteChange(new PropositionFieldChange(
                PropositionFieldCategory.DEFINITION, PropositionFieldAction.ADD, "fr", "Def", null, false));
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(draft);

        bean.openReview(proposition);

        assertTrue(bean.isPrefTermeAccepted());
        assertTrue(bean.isDefinitionAccepted());
        assertFalse(bean.isVarianteAccepted());
        assertFalse(bean.isNoteAccepted());
        verify(propositionMutationService).markRead(5);
    }

    @Test
    void openReview_doesNothingWhenPropositionNull() {
        bean.openReview(null);

        verify(propositionReadService, never()).findDetail(anyInt());
    }

    @Test
    void approveSelected_appliesAcceptedChangesBeforeApproving() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL);
        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New label", "Old label", false));
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(draft);
        bean.openReview(proposition);

        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(propositionDraftService.applyAcceptedChanges(eq(draft), eq("TH1"), eq("C1"), eq("fr"), eq(7), eq("admin"), any()))
                .thenReturn(java.util.List.of());

        bean.approveSelected();

        verify(propositionDraftService).applyAcceptedChanges(eq(draft), eq("TH1"), eq("C1"), eq("fr"), eq(7), eq("admin"), any());
        verify(propositionMutationService).approve(eq(5), eq("admin"), any(), eq("Concept 1"), any());
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Proposition approuvée"));
    }

    @Test
    void approveSelected_skipsApplyWhenDraftEmpty() {
        var proposition = new PropositionSummary(5, "TH1", "C1", "Concept 1", "Author", "a@b.fr", "ENVOYER", "01-01-2024", "fr", "fr");
        when(propositionReadService.findDetail(5)).thenReturn(DETAIL);
        when(propositionDraftService.loadDraftChanges(5)).thenReturn(new PropositionDraft());
        bean.openReview(proposition);

        when(userSession.getCurrentUsername()).thenReturn("admin");

        bean.approveSelected();

        verify(propositionDraftService, never()).applyAcceptedChanges(any(), any(), any(), any(), anyInt(), any(), any());
        verify(propositionMutationService).approve(eq(5), eq("admin"), any(), any(), any());
    }

    @Test
    void approveSelected_doesNothingWhenNoSelection() {
        bean.approveSelected();

        verify(propositionMutationService, never()).approve(anyInt(), any(), any(), any(), any());
    }
}
