package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Vue d'ensemble des rôles de l'utilisateur")
public record AccountRolesResponse(

        @Schema(description = "Indique si l'utilisateur est super-administrateur (liste vide dans ce cas)")
        boolean superAdmin,

        @Schema(description = "Rôles par projet et thésaurus")
        List<ProjectRoleApiDto> projectRoles
) {
}
