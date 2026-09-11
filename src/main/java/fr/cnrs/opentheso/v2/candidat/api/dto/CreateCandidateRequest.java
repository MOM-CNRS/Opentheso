package fr.cnrs.opentheso.v2.candidat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateCandidateRequest(
        @NotBlank @Schema(description = "Libellé préféré") String preferredLabel,
        @Schema(description = "Définition") String definition,
        @Schema(description = "Langue (défaut : langue de travail)") String lang
) {
}
