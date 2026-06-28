package fr.cnrs.opentheso.v2.candidat.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ExportCandidatesRequest(
        @NotBlank @Schema(description = "Format d'export (skos, json, turtle, jsonld)", example = "skos") String format
) {
}
