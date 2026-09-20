package fr.cnrs.opentheso.v2.proposition.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ProcessPropositionRequest(
        @NotBlank @Schema(description = "Action", allowableValues = {"approve", "refuse"}, example = "approve")
        String action,
        @Schema(description = "Message administrateur")
        String message
) {
}
