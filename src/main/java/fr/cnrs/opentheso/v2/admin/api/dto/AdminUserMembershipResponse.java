package fr.cnrs.opentheso.v2.admin.api.dto;

public record AdminUserMembershipResponse(
        int userId,
        String username,
        int projectId,
        String projectName,
        int roleId,
        String roleName
) {
}
