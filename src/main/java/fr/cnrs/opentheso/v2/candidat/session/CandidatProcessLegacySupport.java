package fr.cnrs.opentheso.v2.candidat.session;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;

import java.util.List;

public interface CandidatProcessLegacySupport {

    byte[] exportProcessedCandidatesCsv(List<CandidatDto> candidates);

    boolean insertCandidate(CandidatDto candidate, String adminMessage, int userId);

    boolean rejectCandidate(CandidatDto candidate, String adminMessage, int userId);

    void updateConceptDate(String thesaurusId, String conceptId, int userId);

    void generatePersistentIds(Preferences preferences, CandidatDto candidate);

    boolean isAlertMailEnabled(int userId);

    String resolveUserMail(int userId);

    boolean sendMail(String mail, String subject, String htmlBody);
}
