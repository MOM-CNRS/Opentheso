package fr.cnrs.opentheso.v2.candidat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ProcessCandidateRequest(
        @NotBlank @Schema(description = "Action", allowableValues = {"accept", "reject"}, example = "accept")
        String action,
        @Schema(description = "Message administrateur") String message
) {
}
