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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMapperTest {

    @Test
    void toSummary_mapsFromRow() {
        ProjectSummary summary = ProjectMapper.toSummary(new ProjectSummaryRow(3, "Projet A"));

        assertEquals(3, summary.id());
        assertEquals("Projet A", summary.name());
    }

    @Test
    void toSummary_mapsFromEntity() {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(7);
        entity.setLabel("Projet B");

        ProjectSummary summary = ProjectMapper.toSummary(entity);

        assertEquals(7, summary.id());
        assertEquals("Projet B", summary.name());
    }

    @Test
    void toThesaurus_mapsAllFields() {
        ProjectThesaurus thesaurus = ProjectMapper.toThesaurus(
                new ProjectThesaurusRow("TH1", "Thésaurus 1", true)
        );

        assertEquals("TH1", thesaurus.id());
        assertEquals("Thésaurus 1", thesaurus.title());
        assertTrue(thesaurus.privateThesaurus());
    }

    @Test
    void toMember_mapsAllFields() {
        ProjectMember member = ProjectMapper.toMember(
                new ProjectMemberRow(10, "alice", true, 2, "admin")
        );

        assertEquals(10, member.userId());
        assertEquals("alice", member.username());
        assertTrue(member.active());
        assertEquals(2, member.roleId());
        assertEquals("admin", member.roleName());
    }

    @Test
    void toLimitedMember_mapsAllFields() {
        LimitedProjectMember member = ProjectMapper.toLimitedMember(
                new ProjectLimitedMemberRow(11, "bob", false, 4, "contributor", "TH2", "Thésaurus 2")
        );

        assertEquals(11, member.userId());
        assertEquals("bob", member.username());
        assertFalse(member.active());
        assertEquals(4, member.roleId());
        assertEquals("contributor", member.roleName());
        assertEquals("TH2", member.thesaurusId());
        assertEquals("Thésaurus 2", member.thesaurusName());
    }

    @Test
    void toAssignableRole_mapsAllFields() {
        AssignableRole role = ProjectMapper.toAssignableRole(new AssignableRoleRow(3, "manager"));

        assertEquals(3, role.id());
        assertEquals("manager", role.name());
    }
}
