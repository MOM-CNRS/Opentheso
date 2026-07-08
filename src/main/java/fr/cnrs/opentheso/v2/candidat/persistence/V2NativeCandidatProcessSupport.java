package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.session.CandidatProcessLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeCandidatProcessSupport implements CandidatProcessLegacySupport {

    private final CandidatProcessPersistence candidatProcessPersistence;

    @Override
    public byte[] exportProcessedCandidatesCsv(List<CandidatDto> candidates) {
        return candidatProcessPersistence.exportProcessedCandidatesCsv(candidates);
    }

    @Override
    public boolean insertCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatProcessPersistence.insertCandidate(candidate, adminMessage, userId);
    }

    @Override
    public boolean rejectCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatProcessPersistence.rejectCandidate(candidate, adminMessage, userId);
    }

    @Override
    public void updateConceptDate(String thesaurusId, String conceptId, int userId) {
        candidatProcessPersistence.updateConceptDate(thesaurusId, conceptId, userId);
    }

    @Override
    public void generatePersistentIds(Preferences preferences, CandidatDto candidate) {
        candidatProcessPersistence.generatePersistentIds(preferences, candidate);
    }

    @Override
    public boolean isAlertMailEnabled(int userId) {
        return candidatProcessPersistence.isAlertMailEnabled(userId);
    }

    @Override
    public String resolveUserMail(int userId) {
        return candidatProcessPersistence.resolveUserMail(userId);
    }

    @Override
    public boolean sendMail(String mail, String subject, String htmlBody) {
        return candidatProcessPersistence.sendMail(mail, subject, htmlBody);
    }
}
