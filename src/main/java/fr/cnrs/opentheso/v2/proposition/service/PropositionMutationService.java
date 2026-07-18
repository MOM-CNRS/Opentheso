package fr.cnrs.opentheso.v2.proposition.service;

import fr.cnrs.opentheso.entites.PropositionModification;
import fr.cnrs.opentheso.models.propositions.PropositionStatusEnum;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.v2.shared.mail.SystemMailSender;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropositionMutationService {

    private final PropositionModificationRepository propositionModificationRepository;
    private final PropositionModificationDetailRepository propositionModificationDetailRepository;
    private final SystemMailSender systemMailSender;

    private static final String DATE_PATTERN = "dd-MM-yyyy HH:mm";

    /**
     * Enregistre une nouvelle proposition d'amélioration (commentaire libre).
     *
     * @return true si enregistrée, false si une proposition identique est déjà en cours.
     */
    @Transactional
    public boolean submit(PropositionSubmission submission) {
        return trySave(submission).isPresent();
    }

    /**
     * Enregistre une nouvelle proposition et retourne son identifiant, pour permettre
     * l'enregistrement des changements structurés (libellé/synonymes/traductions/notes) qui
     * l'accompagnent.
     */
    @Transactional
    public Optional<Integer> submitDraft(PropositionSubmission submission) {
        return trySave(submission).map(PropositionModification::getId);
    }

    private Optional<PropositionModification> trySave(PropositionSubmission submission) {
        if (submission == null
                || StringUtils.isAnyBlank(submission.thesaurusId(), submission.conceptId(), submission.lang())
                || StringUtils.isBlank(submission.comment())) {
            return Optional.empty();
        }

        var existing = propositionModificationRepository.findPendingByConcept(
                submission.conceptId(),
                submission.thesaurusId(),
                submission.lang()
        );
        if (existing != null && StringUtils.isNotBlank(submission.authorEmail())
                && submission.authorEmail().equalsIgnoreCase(existing.getEmail())) {
            log.debug("Proposition déjà en cours pour le concept {} ({})", submission.conceptId(), submission.lang());
            return Optional.empty();
        }

        var proposition = PropositionModification.builder()
                .nom(StringUtils.defaultString(submission.authorName()))
                .email(StringUtils.defaultString(submission.authorEmail()))
                .commentaire(submission.comment())
                .idConcept(submission.conceptId())
                .idTheso(submission.thesaurusId())
                .lang(submission.lang())
                .status(PropositionStatusEnum.ENVOYER.name())
                .date(new SimpleDateFormat(DATE_PATTERN).format(new Date()))
                .build();

        var saved = propositionModificationRepository.save(proposition);
        notifyAuthorOfSubmission(submission);
        return Optional.ofNullable(saved);
    }

    @Transactional
    public void markRead(int propositionId) {
        Optional<PropositionModification> found = propositionModificationRepository.findById(propositionId);
        found.ifPresent(proposition -> {
            if (PropositionStatusEnum.ENVOYER.name().equals(proposition.getStatus())) {
                proposition.setStatus(PropositionStatusEnum.LU.name());
                propositionModificationRepository.save(proposition);
            }
        });
    }

    @Transactional
    public void approve(int propositionId, String reviewerName, String adminComment,
                        String conceptLabel, String thesaurusTitle) {
        updateReviewStatus(propositionId, PropositionStatusEnum.APPROUVER, reviewerName, adminComment);
        propositionModificationRepository.findById(propositionId).ifPresent(proposition ->
                notifyAuthorOfDecision(proposition, true, conceptLabel, thesaurusTitle));
    }

    @Transactional
    public void refuse(int propositionId, String reviewerName, String adminComment,
                       String conceptLabel, String thesaurusTitle) {
        updateReviewStatus(propositionId, PropositionStatusEnum.REFUSER, reviewerName, adminComment);
        propositionModificationRepository.findById(propositionId).ifPresent(proposition ->
                notifyAuthorOfDecision(proposition, false, conceptLabel, thesaurusTitle));
    }

    @Transactional
    public void delete(int propositionId) {
        propositionModificationDetailRepository.deleteAllByIdProposition(propositionId);
        propositionModificationRepository.deleteById(propositionId);
    }

    private void updateReviewStatus(int propositionId, PropositionStatusEnum status,
                                    String reviewerName, String adminComment) {
        propositionModificationRepository.findById(propositionId).ifPresent(proposition -> {
            proposition.setStatus(status.name());
            proposition.setApprouvePar(reviewerName);
            proposition.setAdminComment(adminComment);
            proposition.setDate(new SimpleDateFormat(DATE_PATTERN).format(new Date()));
            propositionModificationRepository.save(proposition);
        });
    }

    private void notifyAuthorOfSubmission(PropositionSubmission submission) {
        if (StringUtils.isBlank(submission.authorEmail())) {
            return;
        }
        try {
            String thesaurusName = StringUtils.defaultString(submission.thesaurusTitle(), submission.thesaurusId());
            String subject = "[Opentheso] Confirmation de l'envoi de votre proposition";
            String content = "<html><body>"
                    + "Cher(e) " + StringUtils.defaultString(submission.authorName()) + ",<br/>"
                    + "<p>Votre proposition a bien été reçue par nos administrateurs, "
                    + "elle sera étudiée dans les plus brefs délais.<br/>"
                    + "Vous recevrez un mail dès que votre proposition sera traitée.</p>"
                    + "Merci de votre contribution à l'enrichissement du thésaurus <b>" + thesaurusName + "</b> "
                    + "(concept : " + StringUtils.defaultString(submission.conceptLabel()) + ").<br/><br/>"
                    + "Cordialement,<br/>L'équipe " + thesaurusName + "."
                    + "</body></html>";
            sendMailAsync(submission.authorEmail(), subject, content);
        } catch (Exception ex) {
            log.warn("Échec de l'envoi du mail de confirmation de proposition", ex);
        }
    }

    private void notifyAuthorOfDecision(PropositionModification proposition, boolean approved,
                                        String conceptLabel, String thesaurusTitle) {
        if (StringUtils.isBlank(proposition.getEmail())) {
            return;
        }
        try {
            String thesaurusName = StringUtils.defaultString(thesaurusTitle, proposition.getIdTheso());
            String subject = "[Opentheso] Résultat de votre proposition";
            String decision = approved
                    ? "a été acceptée par les administrateurs."
                    : "a été refusée par les administrateurs.";
            StringBuilder content = new StringBuilder("<html><body>")
                    .append("Cher(e) ").append(StringUtils.defaultString(proposition.getNom())).append(",<br/>")
                    .append("<p>Votre proposition d'enrichissement sur le concept ")
                    .append(StringUtils.defaultString(conceptLabel))
                    .append(" du thésaurus ").append(thesaurusName).append(" ").append(decision).append("<br/>");
            if (StringUtils.isNotBlank(proposition.getAdminComment())) {
                content.append("Message des administrateurs : <br/>")
                        .append(proposition.getAdminComment()).append("<br/>");
            }
            content.append("</p>N'hésitez pas à faire de nouvelles propositions.<br/><br/>")
                    .append("Cordialement,<br/>L'équipe ").append(thesaurusName).append(".")
                    .append("</body></html>");
            sendMailAsync(proposition.getEmail(), subject, content.toString());
        } catch (Exception ex) {
            log.warn("Échec de l'envoi du mail de décision de proposition", ex);
        }
    }

    private void sendMailAsync(String to, String subject, String content) {
        new Thread(() -> {
            try {
                systemMailSender.sendHtmlMail(to, subject, content);
            } catch (Exception ex) {
                log.warn("Envoi de mail de proposition échoué vers {}", to, ex);
            }
        }).start();
    }
}
