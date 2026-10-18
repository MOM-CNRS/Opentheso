package fr.cnrs.opentheso.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectSummaryResponse(
        @Schema(description = "Identifiant du projet") int id,
        @Schema(description = "Libellé du projet") String name
) {
}
