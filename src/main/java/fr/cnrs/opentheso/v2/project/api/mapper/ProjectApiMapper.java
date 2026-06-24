package fr.cnrs.opentheso.v2.project.api.mapper;

import fr.cnrs.opentheso.v2.project.api.dto.AssignableRoleResponse;
import fr.cnrs.opentheso.v2.project.api.dto.LimitedProjectMemberResponse;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectDashboardResponse;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectMemberResponse;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectSummaryResponse;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectThesaurusResponse;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.LimitedProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectDashboard;
import fr.cnrs.opentheso.v2.project.model.ProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.model.ProjectThesaurus;

import java.util.List;

public final class ProjectApiMapper {

    private ProjectApiMapper() {
    }

    public static ProjectSummaryResponse toSummaryResponse(ProjectSummary summary) {
        return new ProjectSummaryResponse(summary.id(), summary.name());
    }

    public static List<ProjectSummaryResponse> toSummaryResponses(List<ProjectSummary> summaries) {
        return summaries.stream().map(ProjectApiMapper::toSummaryResponse).toList();
    }

    public static ProjectDashboardResponse toDashboardResponse(ProjectDashboard dashboard) {
        return new ProjectDashboardResponse(
                dashboard.projectId(),
                dashboard.projectName(),
                dashboard.projectAdmin(),
                dashboard.callerRoleId(),
                dashboard.thesauri().stream().map(ProjectApiMapper::toThesaurusResponse).toList(),
                dashboard.members().stream().map(ProjectApiMapper::toMemberResponse).toList(),
                dashboard.limitedMembers().stream().map(ProjectApiMapper::toLimitedMemberResponse).toList(),
                dashboard.assignableRoles().stream().map(ProjectApiMapper::toAssignableRoleResponse).toList()
        );
    }

    private static ProjectThesaurusResponse toThesaurusResponse(ProjectThesaurus thesaurus) {
        return new ProjectThesaurusResponse(
                thesaurus.id(),
                thesaurus.title(),
                thesaurus.privateThesaurus()
        );
    }

    private static ProjectMemberResponse toMemberResponse(ProjectMember member) {
        return new ProjectMemberResponse(
                member.userId(),
                member.username(),
                member.active(),
                member.roleId(),
                member.roleName()
        );
    }

    private static LimitedProjectMemberResponse toLimitedMemberResponse(LimitedProjectMember member) {
        return new LimitedProjectMemberResponse(
                member.userId(),
                member.username(),
                member.active(),
                member.roleId(),
                member.roleName(),
                member.thesaurusId(),
                member.thesaurusName()
        );
    }

    private static AssignableRoleResponse toAssignableRoleResponse(AssignableRole role) {
        return new AssignableRoleResponse(role.id(), role.name());
    }
}
