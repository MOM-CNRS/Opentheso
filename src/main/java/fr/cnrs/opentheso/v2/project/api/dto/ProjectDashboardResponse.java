package fr.cnrs.opentheso.v2.project.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProjectDashboardResponse(
        @Schema(description = "Identifiant du projet") int projectId,
        @Schema(description = "Libellé du projet") String projectName,
        @Schema(description = "L'utilisateur est admin sur ce projet") boolean projectAdmin,
        @Schema(description = "Rôle de l'utilisateur connecté sur le projet") Integer callerRoleId,
        @Schema(description = "Thésaurus du projet") List<ProjectThesaurusResponse> thesauri,
        @Schema(description = "Utilisateurs avec rôle projet") List<ProjectMemberResponse> members,
        @Schema(description = "Utilisateurs avec rôle limité par thésaurus") List<LimitedProjectMemberResponse> limitedMembers,
        @Schema(description = "Rôles assignables par l'utilisateur connecté") List<AssignableRoleResponse> assignableRoles
) {
}
