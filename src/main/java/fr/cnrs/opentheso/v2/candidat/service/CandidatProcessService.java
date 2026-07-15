package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatProcessPersistence;
import fr.cnrs.opentheso.v2.shared.session.ConceptTreeRefreshSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public boolean insertCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatProcessPersistence.insertCandidate(candidate, adminMessage, userId);
    }

    public boolean rejectCandidate(CandidatDto candidate, String adminMessage, int userId) {
        return candidatProcessPersistence.rejectCandidate(candidate, adminMessage, userId);
    }

    public void afterCandidateAccepted(CandidatDto candidate, int userId, String contributorName, Preferences preferences) {
        candidatProcessPersistence.updateConceptDate(candidate.getIdThesaurus(), candidate.getIdConcepte(), userId);
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(contributorName)
                .idConcept(candidate.getIdConcepte())
                .idThesaurus(candidate.getIdThesaurus())
                .build());
        candidatProcessPersistence.generatePersistentIds(preferences, candidate);
        conceptTreeRefreshSupport.refreshConceptTree();
    }

    public void afterCandidateRejected(CandidatDto candidate, int userId, String contributorName) {
        candidatProcessPersistence.updateConceptDate(candidate.getIdThesaurus(), candidate.getIdConcepte(), userId);
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(contributorName)
                .idConcept(candidate.getIdConcepte())
                .idThesaurus(candidate.getIdThesaurus())
                .build());
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

    public boolean sendMail(String mail, String subject, String htmlBody) {
        return candidatProcessPersistence.sendMail(mail, subject, htmlBody);
    }
}
