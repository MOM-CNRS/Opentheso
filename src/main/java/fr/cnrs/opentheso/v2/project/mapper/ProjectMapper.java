package fr.cnrs.opentheso.v2.project.mapper;

import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.LimitedProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.model.ProjectThesaurus;
import fr.cnrs.opentheso.v2.shared.persistence.ProjectEntity;
import fr.cnrs.opentheso.v2.shared.repository.projection.AssignableRoleRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectLimitedMemberRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectMemberRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectThesaurusRow;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectSummary toSummary(ProjectSummaryRow row) {
        return new ProjectSummary(row.id(), row.name());
    }

    public static ProjectSummary toSummary(ProjectEntity entity) {
        return new ProjectSummary(entity.getId(), entity.getLabel());
    }

    public static ProjectThesaurus toThesaurus(ProjectThesaurusRow row) {
        return new ProjectThesaurus(row.id(), row.title(), row.privateThesaurus());
    }

    public static ProjectMember toMember(ProjectMemberRow row) {
        return new ProjectMember(row.userId(), row.username(), row.active(), row.roleId(), row.roleName());
    }

    public static LimitedProjectMember toLimitedMember(ProjectLimitedMemberRow row) {
        return new LimitedProjectMember(
                row.userId(),
                row.username(),
                row.active(),
                row.roleId(),
                row.roleName(),
                row.thesaurusId(),
                row.thesaurusName()
        );
    }

    public static AssignableRole toAssignableRole(AssignableRoleRow row) {
        return new AssignableRole(row.id(), row.name());
    }
}
