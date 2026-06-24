package fr.cnrs.opentheso.v2.admin.model;

public record AdminUserMembership(
        int userId,
        String username,
        int projectId,
        String projectName,
        int roleId,
        String roleName
) {
}
