package fr.cnrs.opentheso.v2.setting.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ThesaurusLanguageResponse(
        @Schema(description = "Identifiant technique") long id,
        @Schema(description = "Code ISO 639-1") String code,
        @Schema(description = "Code drapeau") String codeFlag,
        @Schema(description = "Libellé dans le thésaurus") String labelTheso,
        @Schema(description = "Libellé affiché") String displayLabel
) {
}
