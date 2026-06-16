package fr.cnrs.opentheso.v2.user.model;

import java.util.List;

public record ProjectRoleOverview(
        int projectId,
        String projectName,
        List<ThesaurusRoleOverview> thesaurusRoles
) {
}
