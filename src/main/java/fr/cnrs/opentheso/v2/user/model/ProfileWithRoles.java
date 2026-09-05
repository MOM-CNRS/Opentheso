package fr.cnrs.opentheso.v2.user.model;

import java.util.List;
import java.io.Serializable;

public record ProfileWithRoles(
        UserProfile profile,
        List<ProjectRoleOverview> projectRoles
) implements Serializable {
}
