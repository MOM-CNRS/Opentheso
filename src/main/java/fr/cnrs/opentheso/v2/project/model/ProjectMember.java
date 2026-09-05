package fr.cnrs.opentheso.v2.project.model;

import java.io.Serializable;

public record ProjectMember(
        int userId,
        String username,
        boolean active,
        int roleId,
        String roleName
) implements Serializable {
}
