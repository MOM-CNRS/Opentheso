package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatDiscussionBeanTest {

    @Mock private CandidatBean candidatBean;
    @Mock private CandidatMutationService candidatMutationService;
    @Mock private UserSession userSession;
    @Mock private V2LocaleBean localeBean;

    private CandidatDiscussionBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatDiscussionBean(candidatBean, candidatMutationService, userSession, localeBean);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    private CandidatDto candidat() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");
        candidat.setNomPref("Concept 1");
        return candidat;
    }

    @Test
    void getParticipantsInConversation_warnsWhenNoCandidateSelected() {
        when(candidatBean.getCandidatSelected()).thenReturn(null);
        when(localeBean.getMsg("candidat.send_message.msg8")).thenReturn("Aucun participant");

        bean.getParticipantsInConversation();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Aucun participant"));
        verify(candidatMutationService, never()).loadDiscussionParticipants(any(), any());
    }

    @Test
    void getParticipantsInConversation_loadsParticipants() {
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        var participant = NodeUser.builder().idUser(7).name("alice").build();
        when(candidatMutationService.loadDiscussionParticipants("C1", "TH1")).thenReturn(List.of(participant));

        bean.getParticipantsInConversation();

        assertTrue(bean.getNodeUsers().contains(participant));
    }

    @Test
    void sendMessage_warnsWhenNoInitialCandidateLoaded() {
        when(candidatBean.getInitialCandidat()).thenReturn(null);
        when(localeBean.getMsg("candidat.send_message.msg7")).thenReturn("Pas de candidat");

        bean.sendMessage();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Pas de candidat"));
        verify(candidatMutationService, never()).sendDiscussionMessage(any(),
                any(), any(), anyInt());
    }

    @Test
    void sendMessage_warnsWhenMessageBlank() {
        when(candidatBean.getInitialCandidat()).thenReturn(candidat());
        when(candidatBean.getMessage()).thenReturn(" ");
        when(localeBean.getMsg("candidat.send_message.msg1")).thenReturn("Message vide");

        bean.sendMessage();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Message vide"));
    }

    @Test
    void sendMessage_sendsMessageNotifiesAndReloads() {
        when(candidatBean.getInitialCandidat()).thenReturn(candidat());
        when(candidatBean.getMessage()).thenReturn("Hello");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatMutationService.loadDiscussionMessages("C1", "TH1", 7)).thenReturn(List.of());
        when(localeBean.getMsg("candidat.send_message.msg2")).thenReturn("Message envoyé");

        bean.sendMessage();

        verify(candidatMutationService).sendDiscussionMessage("C1", "TH1", "Hello", 7);
        verify(candidatMutationService).notifyDiscussionParticipants("C1", "TH1", "Concept 1");
        verify(candidatBean).setMessage("");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Message envoyé"));
    }

    @Test
    void reloadMessage_updatesCandidateMessages() {
        var candidat = candidat();
        when(candidatBean.getCandidatSelected()).thenReturn(candidat);
        when(userSession.getCurrentUserId()).thenReturn(7);
        var message = MessageDto.builder().idUser(7).msg("Hi").build();
        when(candidatMutationService.loadDiscussionMessages("C1", "TH1", 7)).thenReturn(List.of(message));

        bean.reloadMessage();

        assertTrue(candidat.getMessages().contains(message));
    }

    @Test
    void sendInvitation_warnsWhenEmailBlank() {
        bean.setEmail(" ");
        when(localeBean.getMsg("candidat.send_message.msg3")).thenReturn("Email requis");

        bean.sendInvitation();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Email requis"));
        verify(candidatMutationService, never()).sendMailInvitation(any());
    }

    @Test
    void sendInvitation_warnsWhenEmailInvalid() {
        bean.setEmail("not-an-email");
        when(localeBean.getMsg("candidat.send_message.msg4")).thenReturn("Email invalide");

        bean.sendInvitation();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Email invalide"));
        verify(candidatMutationService, never()).sendMailInvitation(any());
    }

    @Test
    void sendInvitation_sendsMailForValidEmail() {
        bean.setEmail("test@example.com");
        when(localeBean.getMsg("candidat.send_message.msg5")).thenReturn("Invitation envoyée");

        bean.sendInvitation();

        verify(candidatMutationService).sendMailInvitation("test@example.com");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Invitation envoyée"));
    }

    @Test
    void clear_resetsStateWhenNodeUsersPresent() {
        bean.setNodeUsers(new java.util.ArrayList<>(List.of(NodeUser.builder().idUser(1).build())));
        bean.setEmail("test@example.com");

        bean.clear();

        assertNull(bean.getNodeUsers());
        assertNull(bean.getEmail());
    }
}
