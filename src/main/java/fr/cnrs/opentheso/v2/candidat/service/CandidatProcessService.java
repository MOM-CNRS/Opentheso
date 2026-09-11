package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.v2.candidat.model.CandidateProcessOutcome;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatProcessPersistence;
import fr.cnrs.opentheso.v2.shared.session.ConceptTreeRefreshSupport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CandidatProcessService {

    private final CandidatProcessPersistence candidatProcessPersistence;
    private final CandidatReadService candidatReadService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ConceptTreeRefreshSupport conceptTreeRefreshSupport;

    public byte[] exportProcessedCandidatesCsv(List<CandidatDto> candidates) {
        return candidatProcessPersistence.exportProcessedCandidatesCsv(candidates);
    }

    public CandidateProcessOutcome insertCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatProcessPersistence.insertCandidate(candidate, adminMessage, userId);
    }

    public CandidateProcessOutcome rejectCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatProcessPersistence.rejectCandidate(candidate, adminMessage, userId);
    }

    public void afterCandidateAccepted(CandidatDto candidate, int userId, String contributorName, Preferences preferences) {
        applyContributorMetadata(candidate, userId, contributorName);
        candidatProcessPersistence.generatePersistentIds(preferences, candidate);
        conceptTreeRefreshSupport.refreshConceptTree();
    }

    public void afterCandidateRejected(CandidatDto candidate, int userId, String contributorName) {
        applyContributorMetadata(candidate, userId, contributorName);
    }

    /**
     * Accepte un lot de candidats en factorisant les opérations coûteuses :
     * ARK en un seul appel, rafraîchissement de l'arbre une seule fois.
     *
     * @return le premier candidat en échec, ou {@code null} si tout a réussi
     */
    @Transactional
    public CandidatDto acceptCandidatesBatch(
            List<CandidatDto> candidates,
            String adminMessage,
            int userId,
            String contributorName,
            Preferences preferences
    ) {
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }
        List<CandidatDto> accepted = new ArrayList<>(candidates.size());
        for (CandidatDto candidate : candidates) {
            if (candidatProcessPersistence.insertCandidate(candidate, adminMessage, userId).isFailure()) {
                return candidate;
            }
            applyContributorMetadata(candidate, userId, contributorName);
            accepted.add(candidate);
        }
        if (!accepted.isEmpty()) {
            candidatProcessPersistence.generatePersistentIds(preferences, accepted);
            conceptTreeRefreshSupport.refreshConceptTree();
        }
        return null;
    }

    /**
     * Rejette un lot de candidats.
     *
     * @return le premier candidat en échec, ou {@code null} si tout a réussi
     */
    @Transactional
    public CandidatDto rejectCandidatesBatch(
            List<CandidatDto> candidates,
            String adminMessage,
            int userId,
            String contributorName
    ) {
        if (CollectionUtils.isEmpty(candidates)) {
            return null;
        }
        for (CandidatDto candidate : candidates) {
            if (candidatProcessPersistence.rejectCandidate(candidate, adminMessage, userId).isFailure()) {
                return candidate;
            }
            applyContributorMetadata(candidate, userId, contributorName);
        }
        return null;
    }

    public void prepareCandidatesForAccept(List<CandidatDto> candidates, String thesaurusId, String lang) {
        candidatReadService.prepareCandidatesForAccept(candidates, thesaurusId, lang);
    }

    public boolean isAlertMailEnabled(int userId) {
        return candidatProcessPersistence.isAlertMailEnabled(userId);
    }

    public String resolveUserMail(int userId) {
        return candidatProcessPersistence.resolveUserMail(userId);
    }

    /**
     * Résout les mails à alerter pour un lot (une lecture user par id distinct).
     */
    public Map<Integer, String> resolveAlertMails(List<CandidatDto> candidates) {
        Map<Integer, String> mailsByUserId = new HashMap<>();
        if (CollectionUtils.isEmpty(candidates)) {
            return mailsByUserId;
        }
        for (CandidatDto candidate : candidates) {
            int creatorId = candidate.getCreatedById();
            if (mailsByUserId.containsKey(creatorId)) {
                continue;
            }
            if (isAlertMailEnabled(creatorId)) {
                mailsByUserId.put(creatorId, resolveUserMail(creatorId));
            } else {
                mailsByUserId.put(creatorId, null);
            }
        }
        return mailsByUserId;
    }

    public boolean sendMail(String mail, String subject, String htmlBody) {
        return candidatProcessPersistence.sendMail(mail, subject, htmlBody);
    }

    /**
     * @return {@code true} si le mail a été envoyé avec succès
     */
    public boolean sendAcceptedMail(
            String mail,
            CandidatDto candidat,
            String adminMessage,
            String thesaurusTitle,
            String appBaseUrl
    ) {
        if (StringUtils.isBlank(mail) || candidat == null) {
            return false;
        }
        String message = adminMessage == null ? "" : adminMessage;
        String title = thesaurusTitle == null ? "" : thesaurusTitle;
        String baseUrl = appBaseUrl == null ? "" : appBaseUrl;
        var subject = "[" + title + "] Confirmation de l'acceptation de votre candidat (" + candidat.getNomPref() + ")";
        var contentFile = "<html><body>"
                + "Cher(e) " + candidat.getCreatedBy() + ", <br/> "
                + "<p> Votre candidat a été accepté par nos administrateurs, il est désormais intégré au thésaurus "
                + title + "<br/></p>"
                + "Nous vous remercions de votre contribution à l'enrichissement du thésaurus <b>" + title + "</b> "
                + "(concept : <a href=\"" + baseUrl + "/?idc=" + candidat.getIdConcepte()
                + "&idt=" + candidat.getIdThesaurus() + "\">" + candidat.getNomPref() + "</a>). "
                + "Message de l'administrateur : " + message
                + "<br/><br/> Cordialement,<br/>"
                + "L'équipe " + title + ".<br/> <img src=\"" + baseUrl
                + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";
        return sendMail(mail, subject, contentFile);
    }

    /**
     * @return {@code true} si le mail a été envoyé avec succès
     */
    public boolean sendRejectedMail(
            String mail,
            CandidatDto candidat,
            String adminMessage,
            String thesaurusTitle,
            String appBaseUrl
    ) {
        if (StringUtils.isBlank(mail) || candidat == null) {
            return false;
        }
        String message = adminMessage == null ? "" : adminMessage;
        String title = thesaurusTitle == null ? "" : thesaurusTitle;
        String baseUrl = appBaseUrl == null ? "" : appBaseUrl;
        var subject = "[" + title + "] Refus de votre candidat (" + candidat.getNomPref() + ")";
        var contentFile = "<html><body>"
                + "Cher(e) " + candidat.getCreatedBy() + ", <br/> "
                + "<p> Votre candidat a été refusé par nos administrateurs, il n'a pas été intégré au thésaurus "
                + title + "<br/></p>"
                + "Message de l'administrateur : " + message
                + "<br/>L'équipe " + title + ".<br/> <img src=\"" + baseUrl
                + "/resources/img/icon_opentheso2.png\" height=\"106\"></body></html>";
        return sendMail(mail, subject, contentFile);
    }

    private void applyContributorMetadata(CandidatDto candidate, int userId, String contributorName) {
        candidatProcessPersistence.updateConceptDate(candidate.getIdThesaurus(), candidate.getIdConcepte(), userId);
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(contributorName)
                .idConcept(candidate.getIdConcepte())
                .idThesaurus(candidate.getIdThesaurus())
                .build());
    }
}
