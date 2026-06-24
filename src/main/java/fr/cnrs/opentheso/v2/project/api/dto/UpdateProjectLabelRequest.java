package fr.cnrs.opentheso.v2.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateProjectLabelRequest(
        @NotBlank @Schema(description = "Nouveau libellé du projet") String label
) {
}
