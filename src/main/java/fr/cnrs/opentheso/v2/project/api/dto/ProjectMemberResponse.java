package fr.cnrs.opentheso.v2.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectMemberResponse(
        @Schema(description = "Identifiant utilisateur") int userId,
        @Schema(description = "Pseudo") String username,
        @Schema(description = "Compte actif") boolean active,
        @Schema(description = "Identifiant du rôle") int roleId,
        @Schema(description = "Nom du rôle") String roleName
) {
}
