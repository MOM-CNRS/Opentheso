package fr.cnrs.opentheso.v2.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateGraphViewRequest(
        @NotBlank @Schema(description = "Nom de la vue") String name,
        @NotBlank @Schema(description = "Description") String description
) {
}
