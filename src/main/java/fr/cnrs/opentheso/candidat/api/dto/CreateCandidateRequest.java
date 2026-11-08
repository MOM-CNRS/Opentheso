package fr.cnrs.opentheso.candidat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateCandidateRequest(
        @NotBlank @Schema(description = "Libellé préféré") String preferredLabel,
        @Schema(description = "Définition") String definition,
        @Schema(description = "Langue (défaut : langue de travail)") String lang,
        @Valid @Schema(description = "Notes typées ajoutées au candidat") List<CandidateNoteRequest> notes
) {
}
