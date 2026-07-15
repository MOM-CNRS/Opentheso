package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.services.ArkService;
import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.HandleConceptService;
import fr.cnrs.opentheso.services.MailService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.services.exports.csv.CsvWriteHelper;
import fr.cnrs.opentheso.v2.candidat.session.CandidatProcessLegacySupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyCandidatProcessSupport implements CandidatProcessLegacySupport {

    private final CandidatService candidatService;
    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final HandleConceptService handleConceptService;
    private final ArkService arkService;
    private final UserService userService;
    private final MailService mailService;
    private final CsvWriteHelper csvWriteHelper;

    @Override
    public byte[] exportProcessedCandidatesCsv(List<CandidatDto> candidates) {
        return csvWriteHelper.writeProcessedCandidates(candidates, ';');
    }

    @Override
    public boolean insertCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatService.insertCandidate(candidate, adminMessage, userId);
    }

    @Override
    public boolean rejectCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatService.rejectCandidate(candidate, adminMessage, userId);
    }

    @Override
    public void updateConceptDate(String thesaurusId, String conceptId, int userId) {
        conceptService.updateDateOfConcept(thesaurusId, conceptId, userId);
    }

    @Override
    public void generatePersistentIds(Preferences preferences, CandidatDto candidate) {
        if (preferences == null) {
            return;
        }
        if (preferences.isUseHandle()) {
            if (!handleConceptService.generateIdHandle(candidate.getIdConcepte(), candidate.getIdThesaurus())) {
                log.error("La création Handle a échoué");
            }
        }
        if (preferences.isUseArk()) {
            var result = conceptAddService.generateArkId(
                    candidate.getIdThesaurus(),
                    List.of(candidate.getIdConcepte()),
                    candidate.getLang(),
                    null
            );
            if (CollectionUtils.isEmpty(result)) {
                log.error("La création Ark a échoué");
            }
        }
        if (preferences.isUseArkLocal()) {
            List<String> conceptIds = new ArrayList<>();
            conceptIds.add(candidate.getIdConcepte());
            if (!arkService.generateArkIdLocal(candidate.getIdThesaurus(), conceptIds)) {
                log.error("La création du Ark local a échoué");
            }
        }
    }

    @Override
    public boolean isAlertMailEnabled(int userId) {
        var user = userService.getUser(userId);
        return user != null && user.isAlertMail();
    }

    @Override
    public String resolveUserMail(int userId) {
        var user = userService.getUser(userId);
        return user != null ? user.getMail() : null;
    }

    @Override
    public boolean sendMail(String mail, String subject, String htmlBody) {
        return mailService.sendMail(mail, subject, htmlBody);
    }
}
