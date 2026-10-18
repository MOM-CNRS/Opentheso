package fr.cnrs.opentheso.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectThesaurusResponse(
        @Schema(description = "Identifiant du thésaurus") String id,
        @Schema(description = "Titre du thésaurus") String title,
        @Schema(description = "Thésaurus privé") boolean privateThesaurus
) {
}
