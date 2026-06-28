package fr.cnrs.opentheso.v2.toolbox.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EditionStatisticsResponse(
        @Schema(description = "Nombre de concepts") int conceptCount,
        @Schema(description = "Nombre de candidats") int candidateCount,
        @Schema(description = "Nombre de concepts dépréciés") int deprecatedCount
) {
}
