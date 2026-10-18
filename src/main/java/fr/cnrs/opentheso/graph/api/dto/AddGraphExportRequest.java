package fr.cnrs.opentheso.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AddGraphExportRequest(
        @NotBlank @Schema(description = "Identifiant du thésaurus") String thesaurusId,
        @Schema(description = "Identifiant du concept racine, optionnel") String conceptId
) {
}
