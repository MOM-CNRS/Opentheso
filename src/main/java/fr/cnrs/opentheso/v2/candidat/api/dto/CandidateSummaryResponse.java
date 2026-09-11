package fr.cnrs.opentheso.v2.candidat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record CandidateSummaryResponse(
        @Schema(description = "Identifiant du concept") String conceptId,
        @Schema(description = "Libellé préféré") String preferredLabel,
        @Schema(description = "Langue") String lang,
        @Schema(description = "Statut (pending / accepted / rejected ou code numérique)") String status,
        @Schema(description = "Créé par") String createdBy,
        @Schema(description = "Date de création") Instant creationDate,
        @Schema(description = "Votes pour") int votesUp,
        @Schema(description = "Votes contre") int votesDown,
        @Schema(description = "Message administrateur") String adminMessage
) {
}
