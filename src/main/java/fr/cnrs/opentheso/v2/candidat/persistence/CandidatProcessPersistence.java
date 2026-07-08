package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.v2.candidat.export.ProcessedCandidatesCsvWriter;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptArkWriteService;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptHandleWriteService;
import fr.cnrs.opentheso.v2.shared.mail.SystemMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidatProcessPersistence {

    private final CandidatMutationPersistence candidatMutationPersistence;
    private final ConceptRepository conceptRepository;
    private final UserRepository userRepository;
    private final ConceptHandleWriteService conceptHandleWriteService;
    private final ConceptArkWriteService conceptArkWriteService;
    private final SystemMailSender systemMailSender;

    public byte[] exportProcessedCandidatesCsv(List<CandidatDto> candidates) {
        return new ProcessedCandidatesCsvWriter().write(candidates, ';');
    }

    public boolean insertCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatMutationPersistence.insertCandidate(candidate, adminMessage, userId);
    }

    public boolean rejectCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatMutationPersistence.rejectCandidate(candidate, adminMessage, userId);
    }

    public void updateConceptDate(String thesaurusId, String conceptId, int userId) {
        conceptRepository.updateDateOfConcept(thesaurusId, conceptId, userId);
    }

    public void generatePersistentIds(Preferences preferences, CandidatDto candidate) {
        if (preferences == null) {
            return;
        }
        if (preferences.isUseHandle()) {
            if (!conceptHandleWriteService.assignHandleOnCreation(candidate.getIdConcepte(), candidate.getIdThesaurus())) {
                log.error("La création Handle a échoué");
            }
        }
        if (preferences.isUseArk()) {
            var result = conceptArkWriteService.generateArkIds(
                    candidate.getIdThesaurus(),
                    List.of(candidate.getIdConcepte()),
                    candidate.getLang());
            if (CollectionUtils.isEmpty(result)) {
                log.error("La création Ark a échoué");
            }
        }
        if (preferences.isUseArkLocal()) {
            candidatMutationPersistence.generateLocalArkForConcepts(
                    candidate.getIdThesaurus(), List.of(candidate.getIdConcepte()), preferences);
        }
    }

    public boolean isAlertMailEnabled(int userId) {
        return userRepository.findById(userId)
                .map(user -> Boolean.TRUE.equals(user.getAlertMail()))
                .orElse(false);
    }

    public String resolveUserMail(int userId) {
        return userRepository.findById(userId).map(user -> user.getMail()).orElse(null);
    }

    public boolean sendMail(String mail, String subject, String htmlBody) {
        return systemMailSender.sendHtmlMail(mail, subject, htmlBody);
    }
}
