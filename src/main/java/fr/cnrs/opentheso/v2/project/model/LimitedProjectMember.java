package fr.cnrs.opentheso.v2.project.model;

import java.io.Serializable;

public record LimitedProjectMember(
        int userId,
        String username,
        boolean active,
        int roleId,
        String roleName,
        String thesaurusId,
        String thesaurusName
) implements Serializable {
}
