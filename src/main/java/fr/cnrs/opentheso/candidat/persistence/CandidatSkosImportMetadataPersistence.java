package fr.cnrs.opentheso.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import fr.cnrs.opentheso.shared.time.Dates;


@Component
@RequiredArgsConstructor
public class CandidatSkosImportMetadataPersistence {

    private final CandidatStatusRepository candidatStatusRepository;
    private final StatusRepository statusRepository;

    public void saveInitialStatus(String conceptId, String thesaurusId, int userId) {
        candidatStatusRepository.save(CandidatStatus.builder()
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .idUser(userId)
                .date(Dates.nowUtilDate())
                .status(statusRepository.findById(1).orElse(null))
                .build());
    }
}
