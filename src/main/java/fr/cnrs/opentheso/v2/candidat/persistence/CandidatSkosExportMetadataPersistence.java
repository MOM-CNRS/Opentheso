package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.models.skosapi.SKOSDiscussion;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSStatus;
import fr.cnrs.opentheso.models.skosapi.SKOSVote;
import fr.cnrs.opentheso.repositories.CandidatMessageRepository;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

@Component
@RequiredArgsConstructor
public class CandidatSkosExportMetadataPersistence {

    private final CandidatStatusRepository candidatStatusRepository;
    private final CandidatMessageRepository candidatMessageRepository;
    private final CandidatVoteRepository candidatVoteRepository;
    private final NoteRepository noteRepository;

    public void enrich(SKOSResource resource, String conceptId, String thesaurusId) {
        candidatStatusRepository.findByIdConcept(conceptId)
                .ifPresent(status -> resource.setSkosStatus(toSkosStatus(status)));

        var messages = candidatMessageRepository.findMessagesByConceptAndThesaurus(conceptId, thesaurusId);
        if (CollectionUtils.isNotEmpty(messages)) {
            for (var message : messages) {
                var discussion = new SKOSDiscussion();
                discussion.setMsg(message.getValue());
                discussion.setIdUser(message.getIdUser());
                discussion.setDate(message.getDate());
                resource.addMessage(discussion);
            }
        }

        var votes = candidatVoteRepository.findAllByIdConceptAndIdThesaurus(conceptId, thesaurusId);
        if (CollectionUtils.isNotEmpty(votes)) {
            for (var vote : votes) {
                var skosVote = new SKOSVote();
                skosVote.setIdNote(vote.getIdNote());
                skosVote.setIdUser(vote.getIdUser());
                skosVote.setIdThesaurus(vote.getIdThesaurus());
                skosVote.setIdConcept(vote.getIdConcept());
                skosVote.setTypeVote(vote.getTypeVote());
                if (StringUtils.isNotEmpty(vote.getIdNote()) && !"null".equalsIgnoreCase(vote.getIdNote())) {
                    noteRepository.findById(Integer.parseInt(vote.getIdNote())).ifPresent(note -> {
                        String htmlTagsRegEx = "<[^>]*>";
                        skosVote.setValueNote(note.getLexicalValue().replaceAll(htmlTagsRegEx, ""));
                    });
                }
                resource.addVote(skosVote);
            }
        }
    }

    private SKOSStatus toSkosStatus(CandidatStatus nodeStatus) {
        var skosStatus = new SKOSStatus();
        skosStatus.setDate(new SimpleDateFormat("yyyy-MM-dd").format(nodeStatus.getDate()));
        skosStatus.setIdConcept(nodeStatus.getIdConcept());
        skosStatus.setIdStatus(String.valueOf(nodeStatus.getStatus().getIdStatus()));
        skosStatus.setMessage(nodeStatus.getMessage());
        skosStatus.setIdThesaurus(nodeStatus.getIdThesaurus());
        skosStatus.setIdUser(String.valueOf(nodeStatus.getIdUser()));
        return skosStatus;
    }
}
