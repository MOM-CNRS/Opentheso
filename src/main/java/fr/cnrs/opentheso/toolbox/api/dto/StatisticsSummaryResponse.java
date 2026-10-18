package fr.cnrs.opentheso.toolbox.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record StatisticsSummaryResponse(
        @Schema(description = "Compteurs de concepts") EditionStatisticsResponse counts,
        @Schema(description = "Dernière modification") Instant lastModification
) {
}
