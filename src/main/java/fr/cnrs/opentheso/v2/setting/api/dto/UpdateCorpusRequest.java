package fr.cnrs.opentheso.v2.setting.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateCorpusRequest(
        @NotBlank @Schema(description = "Nouveau nom du corpus") String name,
        @NotBlank @Schema(description = "URI du lien") String uriLink,
        @Schema(description = "URI de comptage") String uriCount,
        boolean active,
        boolean onlyUriLink,
        boolean omekaS
) {
}
