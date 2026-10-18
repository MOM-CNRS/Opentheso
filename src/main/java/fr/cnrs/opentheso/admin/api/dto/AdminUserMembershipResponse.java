package fr.cnrs.opentheso.admin.api.dto;

public record AdminUserMembershipResponse(
        int userId,
        String username,
        int projectId,
        String projectName,
        int roleId,
        String roleName
) {
}
