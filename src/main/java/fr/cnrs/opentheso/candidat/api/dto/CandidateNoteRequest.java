package fr.cnrs.opentheso.candidat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CandidateNoteRequest(
        @NotBlank
        @Schema(description = "Type : definition, note, scopeNote, historyNote, editorialNote, changeNote, example")
        String type,
        @NotBlank
        @Schema(description = "Texte de la note")
        String value,
        @Schema(description = "Langue (défaut : langue du candidat)")
        String lang,
        @Schema(description = "Source")
        String source
) {
}
