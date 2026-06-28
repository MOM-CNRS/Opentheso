package fr.cnrs.opentheso.v2.candidat.mapper;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatListRow;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
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
                .creationDate(toDate(row.createdAt()))
                .insertionDate(toDate(row.modifiedAt()))
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

    private static Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Timestamp.valueOf(value);
    }
}
