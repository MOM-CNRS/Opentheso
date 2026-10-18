package fr.cnrs.opentheso.shared.repository.projection;

public record ProjectMemberRow(
        int userId,
        String username,
        boolean active,
        int roleId,
        String roleName
) {
}
