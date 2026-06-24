package fr.cnrs.opentheso.v2.admin.mapper;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.shared.repository.projection.AdminThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.AdminUserRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;

public final class AdminMapper {

    private AdminMapper() {
    }

    public static AdminUserMembership toUserMembership(AdminUserRow row) {
        return new AdminUserMembership(
                row.userId(),
                row.username(),
                row.projectId(),
                row.projectName(),
                row.roleId(),
                row.roleName()
        );
    }

    public static AdminThesaurus toThesaurus(AdminThesaurusRow row) {
        return new AdminThesaurus(
                row.thesaurusId(),
                row.title(),
                row.projectId(),
                row.projectName(),
                row.privateThesaurus(),
                row.createdAt()
        );
    }

    public static ProjectSummary toProjectSummary(ProjectSummaryRow row) {
        return new ProjectSummary(row.id(), row.name());
    }
}
