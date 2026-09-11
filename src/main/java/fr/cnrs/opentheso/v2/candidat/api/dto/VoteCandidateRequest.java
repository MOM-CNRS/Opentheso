package fr.cnrs.opentheso.v2.candidat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record VoteCandidateRequest(
        @NotBlank @Schema(description = "Direction du vote", allowableValues = {"up", "down"}, example = "up")
        String direction
) {
}
