package fr.cnrs.opentheso.graph.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateGraphViewRequest(
        @NotBlank @Schema(description = "Nom de la vue") String name,
        @NotBlank @Schema(description = "Description") String description
) {
}
