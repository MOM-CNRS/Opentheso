package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatVote;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CandidatVotePersistence {

    private final CandidatVoteRepository candidatVoteRepository;

    public boolean hasVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) {
        return CollectionUtils.isNotEmpty(candidatVoteRepository.findAllByIdConceptAndIdThesaurusAndIdUserAndIdNoteAndTypeVote(
                conceptId, thesaurusId, userId, noteId, type.getLabel()));
    }

    public void removeVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) {
        candidatVoteRepository.deleteAllByIdUserAndIdConceptAndIdThesaurusAndTypeVoteAndIdNote(
                userId, conceptId, thesaurusId, type.getLabel(), noteId);
    }

    public void addVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) {
        candidatVoteRepository.save(CandidatVote.builder()
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .idUser(userId)
                .idNote(noteId)
                .typeVote(type.getLabel())
                .build());
    }
}
