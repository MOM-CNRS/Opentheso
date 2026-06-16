package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Rôles de l'utilisateur sur un projet")
public record ProjectRoleApiDto(

        @Schema(description = "Identifiant du projet", example = "3")
        int projectId,

        @Schema(description = "Nom du projet", example = "Projet Huma-Num")
        String projectName,

        @Schema(description = "Rôles par thésaurus du projet")
        List<ThesaurusRoleApiDto> thesaurusRoles
) {
}
