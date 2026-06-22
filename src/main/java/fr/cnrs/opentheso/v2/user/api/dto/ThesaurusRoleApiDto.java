package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rôle de l'utilisateur sur un thésaurus")
public record ThesaurusRoleApiDto(
        @Schema(description = "Identifiant du thésaurus", example = "th1")
        String thesaurusId,
        @Schema(description = "Libellé du thésaurus", example = "Archéologie")
        String thesaurusName,
        @Schema(description = "Libellé du rôle", example = "Gestionnaire")
        String roleName
) {
}
