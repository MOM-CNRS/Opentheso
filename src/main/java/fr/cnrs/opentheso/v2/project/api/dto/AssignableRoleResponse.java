package fr.cnrs.opentheso.v2.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssignableRoleResponse(
        @Schema(description = "Identifiant du rôle") int id,
        @Schema(description = "Nom du rôle") String name
) {
}
