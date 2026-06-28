package fr.cnrs.opentheso.v2.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AddGraphExportRequest(
        @NotBlank @Schema(description = "Identifiant du thésaurus") String thesaurusId,
        @Schema(description = "Identifiant du concept racine, optionnel") String conceptId
) {
}
