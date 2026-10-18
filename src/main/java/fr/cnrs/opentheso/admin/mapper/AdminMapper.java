package fr.cnrs.opentheso.admin.mapper;

import fr.cnrs.opentheso.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.admin.model.AdminThesaurusOption;
import fr.cnrs.opentheso.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.project.model.ProjectSummary;
import fr.cnrs.opentheso.shared.repository.projection.AdminThesaurusRow;
import fr.cnrs.opentheso.shared.repository.projection.AdminUserRow;
import fr.cnrs.opentheso.shared.repository.projection.ProjectSummaryRow;
import fr.cnrs.opentheso.shared.repository.projection.ProjectThesaurusRow;

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

    public static AdminThesaurusOption toThesaurusOption(ProjectThesaurusRow row) {
        return new AdminThesaurusOption(row.id(), row.title());
    }
}
