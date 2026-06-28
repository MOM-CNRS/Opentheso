package fr.cnrs.opentheso.v2.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GraphExportResponse(
        @Schema(description = "Identifiant du thésaurus") String thesaurusId,
        @Schema(description = "Identifiant du concept racine, null pour le thésaurus entier") String conceptId
) {
}
