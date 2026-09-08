package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;


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
                .date(V2Dates.nowUtilDate())
                .status(statusRepository.findById(1).orElse(null))
                .build());
    }
}
