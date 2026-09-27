package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.policy.CandidatAccessPolicy;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateDraftBeanCreateTest {

    @Mock private CandidatMutationService candidatMutationService;
    @Mock private CandidatAccessPolicy candidatAccessPolicy;
    @Mock private ThesaurusViewBean thesaurusViewBean;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private ThesaurusPreferencesProvider thesaurusPreferencesProvider;
    @Mock private UserSession userSession;
    @Mock private V2LocaleBean localeBean;
    @Mock private CandidatBoardBean candidatBoardBean;
    @Mock private FacesContext facesContext;
    @Mock private ExternalContext externalContext;

    private CandidateDraftBean bean() {
        return new CandidateDraftBean(
                candidatMutationService,
                candidatAccessPolicy,
                thesaurusViewBean,
                thesaurusContext,
                thesaurusPreferencesProvider,
                userSession,
                localeBean,
                candidatBoardBean
        );
    }

    private void stubSuccessfulSave() {
        when(thesaurusViewBean.getId()).thenReturn("TH1");
        when(candidatAccessPolicy.canCreate(userSession, "TH1")).thenReturn(true);
        when(thesaurusPreferencesProvider.findPreferences("TH1")).thenReturn(Optional.of(new Preferences()));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("alice");
        when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(localeBean.getMsg("v2.candidat.draft.created")).thenReturn("créé");
        when(candidatMutationService.saveNewCandidat(any(CandidatDto.class), eq("TH1"), eq("fr"), eq(7),
                eq("alice"), eq("fr"), anyString())).thenAnswer(invocation -> {
            CandidatDto candidat = invocation.getArgument(0);
            candidat.setIdConcepte("C99");
            candidat.setIdTerm("T99");
            return true;
        });
    }

    @Test
    void createAndContinue_persistsThenResetsFormForNextDraft() {
        CandidateDraftBean bean = bean();
        bean.setTitle("Forêt");
        bean.setDefinition("Couvert arboré");
        bean.setBroaderTerm("Nature");
        bean.setAlternatives("Bois");
        stubSuccessfulSave();

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.createAndContinue();
        }

        assertTrue(bean.isCreated());
        assertTrue(bean.isChainNext());
        assertEquals("C99", bean.getCreatedConceptId());
        assertEquals("", bean.getTitle());
        assertEquals("", bean.getDefinition());
        assertEquals("", bean.getAlternatives());
        assertEquals("Nature", bean.getBroaderTerm());
        assertEquals("create", bean.getCreateMode());
        verify(candidatBoardBean).load("TH1");
    }

    @Test
    void create_withChainMode_samePersistThenReadyForNextDraft() {
        CandidateDraftBean bean = bean();
        bean.setTitle("Rivière");
        bean.setCreateMode("chain");
        stubSuccessfulSave();

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.create();
        }

        assertTrue(bean.isCreated());
        assertTrue(bean.isChainNext());
        assertEquals("", bean.getTitle());
    }

    @Test
    void create_withoutChain_persistsAndRedirectsToCandidate() throws Exception {
        CandidateDraftBean bean = bean();
        bean.setTitle("Montagne");
        bean.setCreateMode("create");
        stubSuccessfulSave();
        when(externalContext.getRequestContextPath()).thenReturn("/ot");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            when(facesContext.getExternalContext()).thenReturn(externalContext);

            bean.create();
        }

        assertFalse(bean.isCreated());
        assertFalse(bean.isChainNext());
        assertEquals("", bean.getTitle());
        verify(externalContext).redirect("/ot/v2/thesaurus/consultation.xhtml?id=C99&type=candidat&from=candidats");
    }
}
