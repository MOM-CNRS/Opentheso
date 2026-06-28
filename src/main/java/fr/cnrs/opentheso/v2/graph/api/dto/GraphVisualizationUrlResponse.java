package fr.cnrs.opentheso.v2.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GraphVisualizationUrlResponse(
        @Schema(description = "URL du visualiseur force-directed") String url
) {
}
