package fr.cnrs.opentheso.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.candidat.policy.CandidatAccessPolicy;
import fr.cnrs.opentheso.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.config.JsfPrettyUrlFilter;
import fr.cnrs.opentheso.concept.model.ConceptTreeNodeKinds;
import fr.cnrs.opentheso.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.session.ConceptTreeRefreshSupport;
import fr.cnrs.opentheso.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.shared.time.Dates;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Gouvernance de la fiche candidat (banner, votes, discussion, traitement)
 * alignée sur la maquette consultation.
 */
@Named("candidateFicheBean")
@ViewScoped
@RequiredArgsConstructor
public class CandidateFicheBean implements Serializable {

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ThesaurusContext thesaurusContext;
    private final transient CandidatReadService candidatReadService;
    private final transient CandidatMutationService candidatMutationService;
    private final transient CandidatProcessService candidatProcessService;
    private final transient CandidatExportService candidatExportService;
    private final transient ThesaurusPreferencesProvider thesaurusPreferencesProvider;
    private final transient ConceptTreeRefreshSupport conceptTreeRefreshSupport;
    private final transient CandidatAccessPolicy candidatAccessPolicy;
    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;

    private String loadedConceptId;
    private int loadedRevision = -1;
    private CandidatDto candidat;
    @Getter
    @Setter
    private String message = "";
    @Getter
    @Setter
    private String rejectReason = "";
    @Getter
    private boolean rejectMode;
    @Getter
    @Setter
    private String acceptMessage = "";
    @Getter
    private boolean acceptMode;
    @Getter
    private Boolean canProcess;
    @Getter
    private boolean actionsOpen;
    @Getter
    private String flashMessage = "";
    @Getter
    private String flashToken = "";

    public boolean isActive() {
        return thesaurusViewBean.isCandidateSelected() || thesaurusViewBean.isRejectedSelected();
    }

    public boolean isPending() {
        ensureLoaded();
        if (candidat != null) {
            return String.valueOf(CandidatStatusCode.PENDING).equals(candidat.getStatut());
        }
        return thesaurusViewBean.isCandidateSelected() && !thesaurusViewBean.isRejectedSelected();
    }

    public boolean isRejected() {
        ensureLoaded();
        if (candidat != null) {
            return String.valueOf(CandidatStatusCode.REJECTED).equals(candidat.getStatut());
        }
        return thesaurusViewBean.isRejectedSelected();
    }

    public boolean isCanProcess() {
        ensureLoaded();
        if (!isPending()) {
            return false;
        }
        if (canProcess != null) {
            return canProcess;
        }
        canProcess = candidatAccessPolicy.canProcess(userSession, thesaurusViewBean.getId());
        return canProcess;
    }

    public boolean isCanValidate() {
        ensureLoaded();
        if (!isCanProcess() || candidat == null) {
            return false;
        }
        return candidatProcessService.hasDefinition(candidat.getIdThesaurus(), candidat.getIdConcepte());
    }

    public boolean isCanReactivate() {
        ensureLoaded();
        return isRejected()
                && candidatAccessPolicy.canReactivate(userSession, thesaurusViewBean.getId());
    }

    public boolean isCanDelete() {
        ensureLoaded();
        return isActive()
                && candidatAccessPolicy.canDelete(userSession, thesaurusViewBean.getId());
    }

    public boolean isCanExport() {
        ensureLoaded();
        return isActive()
                && candidatAccessPolicy.canExport(userSession, thesaurusViewBean.getId());
    }

    public boolean isCanVote() {
        return isPending()
                && candidatAccessPolicy.canVote(userSession, thesaurusViewBean.getId());
    }

    public boolean isCanDiscuss() {
        return isActive()
                && candidatAccessPolicy.canDiscuss(userSession, thesaurusViewBean.getId());
    }

    public boolean isLoggedIn() {
        return userSession != null && userSession.isLoggedIn();
    }

    public String getProposedBy() {
        ensureLoaded();
        if (candidat != null && StringUtils.isNotBlank(candidat.getCreatedBy())) {
            return candidat.getCreatedBy();
        }
        return StringUtils.defaultIfBlank(thesaurusViewBean.getCandidateBy(), "—");
    }

    public String getProposedOn() {
        ensureLoaded();
        if (candidat != null && candidat.getCreationDate() != null) {
            return formatDate(Dates.toInstant(candidat.getCreationDate()));
        }
        return StringUtils.defaultIfBlank(thesaurusViewBean.getCandidateOn(), "—");
    }

    public String getAdminMessage() {
        ensureLoaded();
        if (candidat == null) {
            return "";
        }
        return StringUtils.defaultString(candidat.getAdminMessage());
    }

    public int getVotesUp() {
        ensureLoaded();
        return candidat == null ? 0 : Math.max(0, candidat.getNbrVote());
    }

    public int getVotesDown() {
        ensureLoaded();
        return candidat == null ? 0 : Math.max(0, candidat.getNbrDownVote());
    }

    public int getVotesTotal() {
        return getVotesUp() + getVotesDown();
    }

    public int getVotesUpPercent() {
        int total = getVotesTotal();
        if (total <= 0) {
            return 50;
        }
        return (int) Math.round(100.0 * getVotesUp() / total);
    }

    public int getVotesDownPercent() {
        return 100 - getVotesUpPercent();
    }

    public boolean isVotedUp() {
        ensureLoaded();
        return candidat != null && candidat.isVoted();
    }

    public boolean isVotedDown() {
        ensureLoaded();
        return candidat != null && candidat.isDownVoted();
    }

    public int getMessageCount() {
        ensureLoaded();
        if (candidat == null || candidat.getMessages() == null) {
            return 0;
        }
        return candidat.getMessages().size();
    }

    public List<MessageDto> getMessages() {
        ensureLoaded();
        if (candidat == null || candidat.getMessages() == null) {
            return Collections.emptyList();
        }
        return candidat.getMessages();
    }

    public int getParticipantCount() {
        ensureLoaded();
        return candidat == null ? 0 : Math.max(0, candidat.getNbrParticipant());
    }

    public void toggleActions() {
        actionsOpen = !actionsOpen;
    }

    public void toggleAcceptMode() {
        if (!isCanValidate()) {
            return;
        }
        acceptMode = !acceptMode;
        if (acceptMode) {
            rejectMode = false;
            rejectReason = "";
        } else {
            acceptMessage = "";
        }
    }

    public void cancelAccept() {
        acceptMode = false;
        acceptMessage = "";
    }

    public void toggleRejectMode() {
        rejectMode = !rejectMode;
        if (rejectMode) {
            acceptMode = false;
            acceptMessage = "";
        }
        if (!rejectMode) {
            rejectReason = "";
        }
    }

    public void cancelReject() {
        rejectMode = false;
        rejectReason = "";
    }

    public void toggleVoteUp() {
        if (!isCanVote()) {
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            return;
        }
        int userId = requireUserId();
        // Un vote pour annule un éventuel vote contre.
        if (candidat.isDownVoted()) {
            candidatMutationService.removeVote(
                    candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CONTRE);
            candidat.setDownVoted(false);
            candidat.setNbrDownVote(Math.max(0, candidat.getNbrDownVote() - 1));
        }
        if (candidatMutationService.hasVote(
                candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CANDIDAT)) {
            candidatMutationService.removeVote(
                    candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CANDIDAT);
            candidat.setVoted(false);
            candidat.setNbrVote(Math.max(0, candidat.getNbrVote() - 1));
            publishFlash(localeBean.getMsg("candidat.voteRemoved"));
        } else {
            candidatMutationService.addVote(
                    candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CANDIDAT);
            candidat.setVoted(true);
            candidat.setNbrVote(candidat.getNbrVote() + 1);
            publishFlash(localeBean.getMsg("candidat.voteSaved"));
        }
    }

    public void toggleVoteDown() {
        if (!isCanVote()) {
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            return;
        }
        int userId = requireUserId();
        // Un vote contre annule un éventuel vote pour.
        if (candidat.isVoted()) {
            candidatMutationService.removeVote(
                    candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CANDIDAT);
            candidat.setVoted(false);
            candidat.setNbrVote(Math.max(0, candidat.getNbrVote() - 1));
        }
        if (candidatMutationService.hasVote(
                candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CONTRE)) {
            candidatMutationService.removeVote(
                    candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CONTRE);
            candidat.setDownVoted(false);
            candidat.setNbrDownVote(Math.max(0, candidat.getNbrDownVote() - 1));
            publishFlash(localeBean.getMsg("candidat.voteRemoved"));
        } else {
            candidatMutationService.addVote(
                    candidat.getIdThesaurus(), candidat.getIdConcepte(), userId, null, VoteType.CONTRE);
            candidat.setDownVoted(true);
            candidat.setNbrDownVote(candidat.getNbrDownVote() + 1);
            publishFlash(localeBean.getMsg("candidat.voteSaved"));
        }
    }

    public void sendMessage() {
        if (!isCanDiscuss()) {
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            return;
        }
        if (StringUtils.isBlank(message)) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.send_message.msg1"));
            return;
        }
        int userId = requireUserId();
        candidatMutationService.sendDiscussionMessage(
                candidat.getIdConcepte(), candidat.getIdThesaurus(), message.trim(), userId);
        candidatMutationService.notifyDiscussionParticipants(
                candidat.getIdConcepte(), candidat.getIdThesaurus(), candidat.getNomPref());
        candidat.setMessages(candidatMutationService.loadDiscussionMessages(
                candidat.getIdConcepte(), candidat.getIdThesaurus(), userId));
        candidat.setNbrParticipant(Math.max(candidat.getNbrParticipant(), getMessageCount()));
        message = "";
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.send_message.msg2"));
    }

    public void insertCandidate() {
        if (!isCanProcess() || !isPending()) {
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            return;
        }
        int userId = requireUserId();
        if (!candidatProcessService.hasDefinition(candidat.getIdThesaurus(), candidat.getIdConcepte())) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.validate.needDefinition"));
            return;
        }
        if (candidatProcessService.insertCandidate(candidat, acceptMessage, userId).isFailure()) {
            MessageUtils.showErrorMessage(localeBean.getMsg("candidat.insertError"));
            return;
        }
        notifyCreatorAccepted(candidat, acceptMessage);
        int uid = userId;
        String username = userSession.getCurrentUsername();
        CandidatDto accepted = candidat;
        thesaurusPreferencesProvider.findPreferences(candidat.getIdThesaurus())
                .ifPresent(preferences -> candidatProcessService.afterCandidateAccepted(
                        accepted, uid, username, preferences));
        String conceptId = candidat.getIdConcepte();
        String thesaurusId = StringUtils.defaultIfBlank(candidat.getIdThesaurus(), thesaurusViewBean.getId());
        clear();
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.insertOk"));
        redirectToAcceptedConcept(thesaurusId, conceptId);
    }

    public void confirmReject() {
        if (!isCanProcess() || !isPending()) {
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            return;
        }
        int userId = requireUserId();
        if (candidatProcessService.rejectCandidate(candidat, rejectReason, userId).isFailure()) {
            MessageUtils.showErrorMessage(localeBean.getMsg("candidat.rejectError"));
            return;
        }
        notifyCreatorRejected(candidat, rejectReason);
        candidatProcessService.afterCandidateRejected(
                candidat, userId, userSession.getCurrentUsername());
        String conceptId = candidat.getIdConcepte();
        clear();
        thesaurusViewBean.markTreeNodeRejected(conceptId);
        thesaurusViewBean.openTreeNode(conceptId, ConceptTreeNodeKinds.REJETE);
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.rejectOk"));
    }

    public void reactivateCandidate() {
        if (!isCanReactivate()) {
            publishFlash(localeBean.getMsg("candidat.reactivateError"));
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            publishFlash(localeBean.getMsg("candidat.reactivateError"));
            return;
        }
        String conceptId = candidat.getIdConcepte();
        String thesaurusId = StringUtils.defaultIfBlank(candidat.getIdThesaurus(), thesaurusViewBean.getId());
        if (!candidatMutationService.updateCandidateStatus(
                thesaurusId, conceptId, CandidatStatusCode.PENDING)) {
            publishFlash(localeBean.getMsg("candidat.reactivateError"));
            return;
        }
        clear();
        conceptTreeRefreshSupport.refreshConceptTree();
        thesaurusViewBean.reloadTree();
        thesaurusViewBean.openTreeNode(conceptId, ConceptTreeNodeKinds.CANDIDAT);
        thesaurusViewBean.markSelectedCandidatePending();
        ensureLoaded();
        if (candidat != null) {
            candidat.setStatut(String.valueOf(CandidatStatusCode.PENDING));
            candidat.setAdminMessage(null);
        }
        canProcess = null;
        publishFlash(localeBean.getMsg("candidat.reactivateOk"));
    }

    public void deleteCandidate() {
        if (!isCanDelete()) {
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            return;
        }
        String conceptId = candidat.getIdConcepte();
        String thesaurusId = candidat.getIdThesaurus();
        if (!candidatMutationService.deleteConcept(conceptId, thesaurusId)) {
            MessageUtils.showErrorMessage(localeBean.getMsg("candidat.deleteError"));
            return;
        }
        clear();
        actionsOpen = false;
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.deleteOk"));
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces != null) {
            try {
                String ctx = faces.getExternalContext().getRequestContextPath();
                faces.getExternalContext().redirect(ctx + "/candidat/candidats.xhtml");
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    public void exportSkos() {
        if (!isCanExport()) {
            return;
        }
        ensureLoaded();
        if (candidat == null) {
            return;
        }
        try {
            var result = candidatExportService.exportPendingCandidates(
                    candidat.getIdThesaurus(),
                    List.of(candidat),
                    "rdf",
                    ignored -> { });
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null) {
                return;
            }
            HttpServletResponse response = (HttpServletResponse) faces.getExternalContext().getResponse();
            response.reset();
            response.setContentType(result.contentType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + result.filename() + "\"");
            try (OutputStream out = response.getOutputStream()) {
                out.write(result.content());
                out.flush();
            }
            faces.responseComplete();
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(
                    ex.getMessage() != null ? ex.getMessage() : localeBean.getMsg("candidat.exportError"));
        }
    }

    private void notifyCreatorAccepted(CandidatDto candidate, String adminMessage) {
        if (candidate == null || !candidatProcessService.isAlertMailEnabled(candidate.getCreatedById())) {
            return;
        }
        String mail = candidatProcessService.resolveUserMail(candidate.getCreatedById());
        if (StringUtils.isBlank(mail)) {
            return;
        }
        candidatProcessService.sendAcceptedMailLater(
                mail, candidate, adminMessage, resolveThesaurusTitle(), resolveAppBaseUrl());
    }

    private void notifyCreatorRejected(CandidatDto candidate, String adminMessage) {
        if (candidate == null || !candidatProcessService.isAlertMailEnabled(candidate.getCreatedById())) {
            return;
        }
        String mail = candidatProcessService.resolveUserMail(candidate.getCreatedById());
        if (StringUtils.isBlank(mail)) {
            return;
        }
        candidatProcessService.sendRejectedMailLater(
                mail, candidate, adminMessage, resolveThesaurusTitle(), resolveAppBaseUrl());
    }

    private String resolveThesaurusTitle() {
        String title = thesaurusContext.getCurrentThesaurusTitle();
        return title != null ? title : thesaurusViewBean.getId();
    }

    private String resolveAppBaseUrl() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            return "";
        }
        var ext = faces.getExternalContext();
        String origin = ext.getRequestHeaderMap().get("origin");
        if (origin == null) {
            origin = "";
        }
        return origin + ext.getRequestContextPath();
    }

    private void ensureLoaded() {
        int revision = thesaurusViewBean.getConceptRevision();
        if (revision != loadedRevision) {
            loadedRevision = revision;
            candidat = null;
            loadedConceptId = null;
            canProcess = null;
        }
        if (!isActive()) {
            clear();
            return;
        }
        String conceptId = thesaurusViewBean.getSelectedId();
        String thesaurusId = thesaurusViewBean.getId();
        if (StringUtils.isBlank(conceptId) || StringUtils.isBlank(thesaurusId)) {
            clear();
            return;
        }
        if (candidat != null && conceptId.equals(loadedConceptId)) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        String lang = StringUtils.defaultIfBlank(
                thesaurusViewBean.getSelectedLang(),
                thesaurusContext.resolveWorkLanguage());
        candidat = candidatReadService.findByConceptId(thesaurusId, conceptId, lang, userId).orElse(null);
        loadedConceptId = conceptId;
        rejectMode = false;
        rejectReason = "";
        acceptMode = false;
        acceptMessage = "";
        message = "";
        canProcess = null;
        actionsOpen = false;
    }

    private void clear() {
        candidat = null;
        loadedConceptId = null;
        rejectMode = false;
        rejectReason = "";
        acceptMode = false;
        acceptMessage = "";
        message = "";
        canProcess = null;
        actionsOpen = false;
    }

    /**
     * Recharge la consultation du concept accepté. La réponse ajax partielle
     * laisse la fiche candidat à l'écran alors que l'enregistrement a réussi.
     */
    private void redirectToAcceptedConcept(String thesaurusId, String conceptId) {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null || StringUtils.isBlank(conceptId)) {
            return;
        }
        try {
            faces.getExternalContext().getFlash().setKeepMessages(true);
            String ctx = faces.getExternalContext().getRequestContextPath();
            String path = JsfPrettyUrlFilter.conceptBrowserPath(thesaurusId, conceptId);
            if (StringUtils.isBlank(path)) {
                path = "/thesaurus/consultation.xhtml?id=" + conceptId;
            }
            faces.getExternalContext().redirect(ctx + path);
        } catch (Exception ignored) {
            thesaurusViewBean.markTreeNodeInserted(conceptId);
            thesaurusViewBean.openTreeNode(conceptId, ConceptTreeNodeKinds.CONCEPT);
            thesaurusViewBean.markSelectedAsConcept();
        }
    }

    private void publishFlash(String msg) {
        this.flashMessage = StringUtils.defaultString(msg);
        this.flashToken = String.valueOf(System.currentTimeMillis());
    }

    private int requireUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        return userId;
    }

    private String formatDate(Instant instant) {
        if (instant == null) {
            return "—";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(instant);
    }
}
