package fr.cnrs.opentheso.v2.shared.repository.projection;

import java.time.LocalDateTime;

public record CandidatListRow(
        String idConcept,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        int createdById,
        Integer createdByAdminId,
        String adminMessage,
        String preferredLabel,
        String createdByUsername,
        String createdByAdminUsername,
        int messageCount,
        int propositionCount,
        int candidateVoteCount,
        int noteVoteCount
) {
}
