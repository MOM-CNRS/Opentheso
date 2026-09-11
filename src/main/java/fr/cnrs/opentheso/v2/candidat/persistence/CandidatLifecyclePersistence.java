package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.model.CandidateProcessOutcome;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CandidatLifecyclePersistence {

    private final CandidatStatusRepository candidatStatusRepository;
    private final StatusRepository statusRepository;
    private final ConceptRepository conceptRepository;

    /**
     * Met à jour le statut candidat. Même résolution que accept/rejet ({@code findByIdConcept})
     * en secours si le couple concept/thésaurus ne matche pas (données historiques).
     */
    @Transactional
    public boolean updateCandidateStatus(String thesaurusId, String conceptId, int status) {
        if (StringUtils.isBlank(conceptId)) {
            return false;
        }
        Optional<CandidatStatus> candidatStatus = StringUtils.isNotBlank(thesaurusId)
                ? candidatStatusRepository.findAllByIdConceptAndIdThesaurus(conceptId, thesaurusId)
                : Optional.empty();
        if (candidatStatus.isEmpty()) {
            candidatStatus = candidatStatusRepository.findByIdConcept(conceptId);
        }
        if (candidatStatus.isEmpty()) {
            return false;
        }
        var newStatus = statusRepository.findById(status);
        if (newStatus.isEmpty()) {
            return false;
        }
        CandidatStatus entity = candidatStatus.get();
        entity.setStatus(newStatus.get());
        if (status == CandidatStatusCode.PENDING) {
            entity.setMessage(null);
            entity.setIdUserAdmin(null);
        }
        if (StringUtils.isNotBlank(thesaurusId) && StringUtils.isBlank(entity.getIdThesaurus())) {
            entity.setIdThesaurus(thesaurusId);
        }
        candidatStatusRepository.save(entity);
        candidatStatusRepository.flush();
        return true;
    }

    public CandidateProcessOutcome insertCandidate(CandidatDto candidatDto, String adminMessage, int userId) {
        var candidatStatus = candidatStatusRepository.findByIdConcept(candidatDto.getIdConcepte());
        if (candidatStatus.isEmpty()) {
            return CandidateProcessOutcome.fail();
        }
        // Statut "accepté" (id=2) : cache JPA de 1er niveau dans un lot transactionnel.
        candidatStatus.get().setStatus(statusRepository.findById(2).orElse(null));
        candidatStatus.get().setMessage(adminMessage);
        candidatStatus.get().setIdUserAdmin(userId);
        candidatStatusRepository.save(candidatStatus.get());
        conceptRepository.setStatus("D", candidatDto.getIdConcepte(), candidatDto.getIdThesaurus());
        conceptRepository.setTopConceptTag(CollectionUtils.isEmpty(candidatDto.getTermesGenerique()),
                candidatDto.getIdConcepte(), candidatDto.getIdThesaurus());
        return CandidateProcessOutcome.ok();
    }

    public CandidateProcessOutcome rejectCandidate(CandidatDto candidatDto, String adminMessage, int userId) {
        var candidatStatus = candidatStatusRepository.findByIdConcept(candidatDto.getIdConcepte());
        if (candidatStatus.isEmpty()) {
            return CandidateProcessOutcome.fail();
        }
        // Statut "rejeté" (id=3) : cache JPA de 1er niveau dans un lot transactionnel.
        candidatStatus.get().setStatus(statusRepository.findById(3).orElse(null));
        candidatStatus.get().setMessage(adminMessage);
        candidatStatus.get().setIdUserAdmin(userId);
        candidatStatusRepository.save(candidatStatus.get());
        return CandidateProcessOutcome.ok();
    }
}
