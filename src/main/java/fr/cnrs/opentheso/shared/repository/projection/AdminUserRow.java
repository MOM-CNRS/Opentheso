package fr.cnrs.opentheso.shared.repository.projection;

public record AdminUserRow(
        int userId,
        String username,
        int projectId,
        String projectName,
        int roleId,
        String roleName
) {
}
