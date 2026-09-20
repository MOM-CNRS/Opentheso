package fr.cnrs.opentheso.v2.proposition.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PropositionSummaryResponse(
        @Schema(description = "Identifiant de la proposition") int id,
        @Schema(description = "Identifiant du concept") String conceptId,
        @Schema(description = "Libellé du concept") String conceptLabel,
        @Schema(description = "Langue") String lang,
        @Schema(description = "Statut (pending / read / approved / refused)") String status,
        @Schema(description = "Auteur") String authorName,
        @Schema(description = "Email de l'auteur") String authorEmail,
        @Schema(description = "Date de publication") String publishedAt
) {
}
