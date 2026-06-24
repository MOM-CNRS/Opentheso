package fr.cnrs.opentheso.v2.shared.repository.projection;

public record ProjectMemberRow(
        int userId,
        String username,
        boolean active,
        int roleId,
        String roleName
) {
}
