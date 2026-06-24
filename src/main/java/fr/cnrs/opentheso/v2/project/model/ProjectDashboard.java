package fr.cnrs.opentheso.v2.project.model;

import java.util.List;

public record ProjectDashboard(
        int projectId,
        String projectName,
        boolean projectAdmin,
        Integer callerRoleId,
        List<ProjectThesaurus> thesauri,
        List<ProjectMember> members,
        List<LimitedProjectMember> limitedMembers,
        List<AssignableRole> assignableRoles
) {
}
