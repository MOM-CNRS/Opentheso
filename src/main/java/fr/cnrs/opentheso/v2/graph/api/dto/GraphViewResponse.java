package fr.cnrs.opentheso.v2.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GraphViewResponse(
        @Schema(description = "Identifiant de la vue") int id,
        @Schema(description = "Nom de la vue") String name,
        @Schema(description = "Description") String description,
        @Schema(description = "Entrées exportées") List<GraphExportResponse> exports
) {
}
