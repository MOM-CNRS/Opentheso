package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.utils.EmailUtils;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ViewScoped
@RequiredArgsConstructor
@Named("v2DiscussionCandidatBean")
public class CandidatDiscussionBean implements Serializable {

    private final CandidatBean candidatBean;
    private final CandidatMutationService candidatMutationService;
    private final UserSession userSession;
    private final V2LocaleBean localeBean;

    private String email;
    private List<NodeUser> nodeUsers;

    public void clear() {
        if (nodeUsers != null) {
            nodeUsers.clear();
            nodeUsers = null;
        }
        email = null;
    }

    public void getParticipantsInConversation() {
        var candidat = candidatBean.getCandidatSelected();
        nodeUsers = candidat == null
                ? new ArrayList<>()
                : candidatMutationService.loadDiscussionParticipants(candidat.getIdConcepte(), candidat.getIdThesaurus());

        if (CollectionUtils.isEmpty(nodeUsers)) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.send_message.msg8"));
        } else {
            PrimeFaces.current().executeScript("PF('participantsList').show();");
        }
    }

    public void sendMessage() {
        if (candidatBean.getInitialCandidat() == null) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.send_message.msg7"));
            return;
        }

        if (StringUtils.isBlank(candidatBean.getMessage())) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.send_message.msg1"));
            return;
        }

        var candidat = candidatBean.getCandidatSelected();
        candidatMutationService.sendDiscussionMessage(
                candidat.getIdConcepte(), candidat.getIdThesaurus(), candidatBean.getMessage(), requireUserId());
        candidatMutationService.notifyDiscussionParticipants(
                candidat.getIdConcepte(), candidat.getIdThesaurus(), candidat.getNomPref());

        reloadMessage();
        candidatBean.setMessage("");
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.send_message.msg2"));
    }

    public void reloadMessage() {
        var candidat = candidatBean.getCandidatSelected();
        candidat.setMessages(candidatMutationService.loadDiscussionMessages(
                candidat.getIdConcepte(), candidat.getIdThesaurus(), requireUserId()));
    }

    public void sendInvitation() {
        if (StringUtils.isBlank(email)) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.send_message.msg3"));
        } else if (!EmailUtils.isValidEmailAddress(email)) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.send_message.msg4"));
        } else {
            candidatMutationService.sendMailInvitation(email);
            MessageUtils.showInformationMessage(localeBean.getMsg("candidat.send_message.msg5"));
        }
    }

    private int requireUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        return userId;
    }
}
