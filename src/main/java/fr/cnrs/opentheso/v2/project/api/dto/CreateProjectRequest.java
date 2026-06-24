package fr.cnrs.opentheso.v2.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
        @NotBlank @Schema(description = "Libellé du nouveau projet") String label
) {
}
