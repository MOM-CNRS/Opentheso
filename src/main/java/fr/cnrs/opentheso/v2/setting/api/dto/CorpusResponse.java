package fr.cnrs.opentheso.v2.setting.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CorpusResponse(
        @Schema(description = "Nom du corpus") String name,
        @Schema(description = "URI du lien") String uriLink,
        @Schema(description = "URI de comptage") String uriCount,
        boolean active,
        boolean onlyUriLink,
        boolean omekaS,
        Integer sort
) {
}
