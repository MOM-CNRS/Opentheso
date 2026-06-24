package fr.cnrs.opentheso.v2.shared.repository.projection;

public record AdminUserRow(
        int userId,
        String username,
        int projectId,
        String projectName,
        int roleId,
        String roleName
) {
}
