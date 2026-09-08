package fr.cnrs.opentheso.v2.candidat.mapper;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatListRow;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;

import java.util.List;

public final class CandidatMapper {

    private CandidatMapper() {
    }

    public static List<CandidatDto> toCandidatDtos(List<CandidatListRow> rows, String thesaurusId, int statusId) {
        return rows.stream()
                .map(row -> toCandidatDto(row, thesaurusId, statusId))
                .toList();
    }

    public static CandidatDto toCandidatDto(CandidatListRow row, String thesaurusId, int statusId) {
        return CandidatDto.builder()
                .idConcepte(row.idConcept())
                .creationDate(V2Dates.toUtilDate(row.createdAt()))
                .insertionDate(V2Dates.toUtilDate(row.modifiedAt()))
                .statut(String.valueOf(statusId))
                .createdById(row.createdById())
                .createdByIdAdmin(row.createdByAdminId() == null ? -1 : row.createdByAdminId())
                .idThesaurus(thesaurusId)
                .adminMessage(row.adminMessage())
                .nomPref(row.preferredLabel())
                .createdBy(row.createdByUsername())
                .createdByAdmin(row.createdByAdminId() != null ? row.createdByAdminUsername() : "Utilisateur inconnu")
                .nbrParticipant(row.messageCount())
                .nbrDemande(row.propositionCount())
                .nbrVote(row.candidateVoteCount())
                .nbrNoteVote(row.noteVoteCount())
                .alignments(List.of())
                .build();
    }
}
