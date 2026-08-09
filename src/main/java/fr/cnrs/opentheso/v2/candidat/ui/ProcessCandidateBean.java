package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@ViewScoped
@RequiredArgsConstructor
@Named(value = "v2ProcessCandidateBean")
public class ProcessCandidateBean implements Serializable {

    private final CandidatBean candidatBean;
    private final CandidatProcessService candidatProcessService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ThesaurusPreferencesProvider thesaurusPreferencesProvider;

    private CandidatDto selectedCandidate;
    private String adminMessage;

    public void reset(CandidatDto candidatSelected) {
        this.selectedCandidate = candidatSelected;
        adminMessage = null;
    }

    public StreamedContent exportProcessedCandidates(List<CandidatDto> candidatDtos) {
        var datas = candidatProcessService.exportProcessedCandidatesCsv(candidatDtos);
        if (datas == null || datas.length == 0) {
            MessageUtils.showErrorMessage("Aucun candidat à exporter");
            return DefaultStreamedContent.builder()
                    .contentType("text/plain")
                    .name("export-error.txt")
                    .stream(() -> new ByteArrayInputStream(new byte[0]))
                    .build();
        }

        return DefaultStreamedContent.builder()
                .contentType("text/csv")
                .name(resolveThesaurusTitle() + "_candidats.csv")
                .stream(() -> new ByteArrayInputStream(datas))
                .build();
    }

    public void insertCandidat() throws IOException {
        if (selectedCandidate == null) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }

        int userId = requireUserId();
        if (candidatProcessService.insertCandidate(selectedCandidate, adminMessage, userId)) {
            MessageUtils.showErrorMessage("Erreur d'insertion");
            return;
        }

        if (candidatProcessService.isAlertMailEnabled(selectedCandidate.getCreatedById())) {
            String mail = candidatProcessService.resolveUserMail(selectedCandidate.getCreatedById());
            if (mail != null) {
                sendMailCandidateAccepted(mail, selectedCandidate);
            }
        }

        thesaurusPreferencesProvider.findPreferences(selectedCandidate.getIdThesaurus())
                .ifPresent(preferences -> candidatProcessService.afterCandidateAccepted(
                        selectedCandidate,
                        userId,
                        userSession.getCurrentUsername(),
                        preferences
                ));

        reset(null);
        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
        candidatBean.initCandidatModule();
        PrimeFaces.current().ajax().update("containerIndex:tabViewCandidat");
        MessageUtils.showInformationMessage("Candidat inséré avec succès");
    }

    public void rejectCandidat() throws IOException {
        if (selectedCandidate == null) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }

        int userId = requireUserId();
        if (candidatProcessService.rejectCandidate(selectedCandidate, adminMessage, userId)) {
            MessageUtils.showErrorMessage("Erreur d'insertion");
            return;
        }

        if (candidatProcessService.isAlertMailEnabled(selectedCandidate.getCreatedById())) {
            String mail = candidatProcessService.resolveUserMail(selectedCandidate.getCreatedById());
            if (mail != null) {
                sendMailCandidateRejected(mail, selectedCandidate);
            }
        }

        candidatProcessService.afterCandidateRejected(
                selectedCandidate,
                userId,
                userSession.getCurrentUsername()
        );

        MessageUtils.showInformationMessage("Candidat(s) rejeté(s) avec succès");
        reset(null);
        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
        candidatBean.initCandidatModule();
        PrimeFaces.current().ajax().update("containerIndex:tabViewCandidat");
    }

    public void insertListCandidat() throws IOException {
        if (candidatBean.getSelectedCandidates() == null || candidatBean.getSelectedCandidates().isEmpty()) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }

        int idUser = requireUserId();
        Preferences nodePreference = thesaurusPreferencesProvider
                .findPreferences(thesaurusContext.resolveThesaurusId())
                .orElse(null);

        var candidates = new ArrayList<>(candidatBean.getSelectedCandidates());
        candidatProcessService.prepareCandidatesForAccept(
                candidates,
                thesaurusContext.resolveThesaurusId(),
                candidatBean.getPreferredLang()
        );

        CandidatDto failed = candidatProcessService.acceptCandidatesBatch(
                candidates,
                adminMessage,
                idUser,
                userSession.getCurrentUsername(),
                nodePreference
        );
        if (failed != null) {
            MessageUtils.showErrorMessage("Erreur d'insertion pour le candidat : "
                    + failed.getNomPref() + "(" + failed.getIdConcepte() + ")");
            return;
        }

        // Mails après le lot DB (et une seule résolution user par créateur distinct).
        var alertMails = candidatProcessService.resolveAlertMails(candidates);
        for (CandidatDto candidate : candidates) {
            String mail = alertMails.get(candidate.getCreatedById());
            if (mail != null) {
                sendMailCandidateAccepted(mail, candidate);
            }
        }

        MessageUtils.showInformationMessage("Candidats insérés avec succès");
        reset(null);
        candidatBean.initCandidatModule();
        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
    }

    public void rejectCandidatList() throws IOException {
        if (candidatBean.getSelectedCandidates() == null || candidatBean.getSelectedCandidates().isEmpty()) {
            MessageUtils.showErrorMessage("Pas de candidat sélectionné");
            return;
        }

        int userId = requireUserId();
        var candidates = new ArrayList<>(candidatBean.getSelectedCandidates());
        CandidatDto failed = candidatProcessService.rejectCandidatesBatch(
                candidates,
                adminMessage,
                userId,
                userSession.getCurrentUsername()
        );
        if (failed != null) {
            MessageUtils.showErrorMessage("Erreur pour le candidat : "
                    + failed.getNomPref() + "(" + failed.getIdConcepte() + ")");
            return;
        }

        var alertMails = candidatProcessService.resolveAlertMails(candidates);
        for (CandidatDto candidate : candidates) {
            String mail = alertMails.get(candidate.getCreatedById());
            if (mail != null) {
                sendMailCandidateRejected(mail, candidate);
            }
        }

        MessageUtils.showInformationMessage("Candidats insérés avec succès");
        reset(null);
        candidatBean.initCandidatModule();
        candidatBean.getAllCandidatsByThesoAndLangue();
        candidatBean.setIsListCandidatsActivate(true);
    }

    private void sendMailCandidateAccepted(String mail, CandidatDto candidat) {
        if (adminMessage == null) {
            adminMessage = "";
        }
        String thesaurusTitle = resolveThesaurusTitle();
        var subject = "[" + thesaurusTitle + "] Confirmation de l'acceptation de votre candidat (" + candidat.getNomPref() + ")";
        var contentFile = "<html><body>"
                + "Cher(e) " + candidat.getCreatedBy() + ", <br/> "
                + "<p> Votre candidat a été accepté par nos administrateurs, il est désormais intégré au thésaurus "
                + thesaurusTitle + "<br/></p>"
                + "Nous vous remercions de votre contribution à l'enrichissement du thésaurus <b>" + thesaurusTitle + "</b> "
                + "(concept : <a href=\"" + getPath() + "/?idc=" + candidat.getIdConcepte()
                + "&idt=" + candidat.getIdThesaurus() + "\">" + candidat.getNomPref() + "</a>). "
                + "Message de l'administrateur : " + adminMessage
                + "<br/><br/> Cordialement,<br/>"
                + "L'équipe " + thesaurusTitle + ".<br/> <img src=\"" + getPath()
                + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";

        if (candidatProcessService.sendMail(mail, subject, contentFile)) {
            MessageUtils.showErrorMessage("!! votre propostion n'a pas été envoyée !!");
        }
    }

    private boolean sendMailCandidateRejected(String mail, CandidatDto candidat) {
        if (adminMessage == null) {
            adminMessage = "";
        }
        String thesaurusTitle = resolveThesaurusTitle();
        var subject = "[" + thesaurusTitle + "] Refus de votre candidat (" + candidat.getNomPref() + ")";
        var contentFile = "<html><body>"
                + "Cher(e) " + candidat.getCreatedBy() + ", <br/> "
                + "<p> Votre candidat a été refusé par nos administrateurs, il n'a pas été intégré au thésaurus "
                + thesaurusTitle + "<br/></p>"
                + "Message de l'administrateur : " + adminMessage
                + "<br/>L'équipe " + thesaurusTitle + ".<br/> <img src=\"" + getPath()
                + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";

        if (!candidatProcessService.sendMail(mail, subject, contentFile)) {
            MessageUtils.showErrorMessage("!! votre propostion n'a pas été envoyée !!");
            return false;
        }
        return true;
    }

    private String getPath() {
        if (FacesContext.getCurrentInstance() == null) {
            return "";
        }
        var path = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin");
        return path + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    private String resolveThesaurusTitle() {
        String title = thesaurusContext.getCurrentThesaurusTitle();
        return title != null ? title : thesaurusContext.resolveThesaurusId();
    }

    private int requireUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        return userId;
    }
}
