package fr.cnrs.opentheso.v2.proposition.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PropositionDetailResponse(
        @Schema(description = "Identifiant de la proposition") int id,
        @Schema(description = "Identifiant du thésaurus") String thesaurusId,
        @Schema(description = "Identifiant du concept") String conceptId,
        @Schema(description = "Libellé du concept") String conceptLabel,
        @Schema(description = "Langue") String lang,
        @Schema(description = "Statut (pending / read / approved / refused)") String status,
        @Schema(description = "Auteur") String authorName,
        @Schema(description = "Email de l'auteur") String authorEmail,
        @Schema(description = "Commentaire de la proposition") String comment,
        @Schema(description = "Date de publication") String publishedAt,
        @Schema(description = "Revue par") String reviewedBy,
        @Schema(description = "Commentaire administrateur") String adminComment
) {
}
