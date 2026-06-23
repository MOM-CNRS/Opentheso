package fr.cnrs.opentheso.v2.user.model;

import java.util.List;

public record ProfileWithRoles(
        UserProfile profile,
        List<ProjectRoleOverview> projectRoles
) {
}
