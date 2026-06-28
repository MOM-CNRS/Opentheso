package fr.cnrs.opentheso.v2.toolbox.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record EditionThesaurusResponse(
        @Schema(description = "Identifiant du thésaurus") String id,
        @Schema(description = "Titre") String title,
        @Schema(description = "Thésaurus privé") boolean privateThesaurus,
        @Schema(description = "Date de création") LocalDateTime createdAt
) {
}
