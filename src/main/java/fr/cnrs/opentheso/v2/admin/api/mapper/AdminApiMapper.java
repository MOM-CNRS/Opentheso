package fr.cnrs.opentheso.v2.admin.api.mapper;

import fr.cnrs.opentheso.v2.admin.api.dto.AdminThesaurusResponse;
import fr.cnrs.opentheso.v2.admin.api.dto.AdminUserMembershipResponse;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectSummaryResponse;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;

import java.util.List;

public final class AdminApiMapper {

    private AdminApiMapper() {
    }

    public static AdminUserMembershipResponse toUserResponse(AdminUserMembership membership) {
        return new AdminUserMembershipResponse(
                membership.userId(),
                membership.username(),
                membership.projectId(),
                membership.projectName(),
                membership.roleId(),
                membership.roleName()
        );
    }

    public static List<AdminUserMembershipResponse> toUserResponses(List<AdminUserMembership> memberships) {
        return memberships.stream().map(AdminApiMapper::toUserResponse).toList();
    }

    public static ProjectSummaryResponse toProjectResponse(ProjectSummary project) {
        return new ProjectSummaryResponse(project.id(), project.name());
    }

    public static List<ProjectSummaryResponse> toProjectResponses(List<ProjectSummary> projects) {
        return projects.stream().map(AdminApiMapper::toProjectResponse).toList();
    }

    public static AdminThesaurusResponse toThesaurusResponse(AdminThesaurus thesaurus) {
        return new AdminThesaurusResponse(
                thesaurus.id(),
                thesaurus.title(),
                thesaurus.projectId(),
                thesaurus.projectName(),
                thesaurus.privateThesaurus(),
                thesaurus.createdAt()
        );
    }

    public static List<AdminThesaurusResponse> toThesaurusResponses(List<AdminThesaurus> thesauri) {
        return thesauri.stream().map(AdminApiMapper::toThesaurusResponse).toList();
    }
}
