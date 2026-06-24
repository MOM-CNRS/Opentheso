package fr.cnrs.opentheso.v2.project.model;

public record ProjectMember(
        int userId,
        String username,
        boolean active,
        int roleId,
        String roleName
) {
}
