package fr.cnrs.opentheso.v2.project.api.mapper;

import fr.cnrs.opentheso.v2.project.api.dto.ProjectDashboardResponse;
import fr.cnrs.opentheso.v2.project.api.dto.ProjectSummaryResponse;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.LimitedProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectDashboard;
import fr.cnrs.opentheso.v2.project.model.ProjectMember;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.model.ProjectThesaurus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectApiMapperTest {

    @Test
    void toSummaryResponse_mapsFields() {
        ProjectSummaryResponse response = ProjectApiMapper.toSummaryResponse(new ProjectSummary(5, "Alpha"));

        assertEquals(5, response.id());
        assertEquals("Alpha", response.name());
    }

    @Test
    void toSummaryResponses_mapsList() {
        List<ProjectSummaryResponse> responses = ProjectApiMapper.toSummaryResponses(
                List.of(new ProjectSummary(1, "A"), new ProjectSummary(2, "B"))
        );

        assertEquals(2, responses.size());
        assertEquals("A", responses.get(0).name());
        assertEquals("B", responses.get(1).name());
    }

    @Test
    void toDashboardResponse_mapsNestedCollections() {
        ProjectDashboard dashboard = new ProjectDashboard(
                3,
                "Projet X",
                true,
                2,
                List.of(new ProjectThesaurus("TH1", "Thésaurus 1", false)),
                List.of(new ProjectMember(10, "alice", true, 2, "admin")),
                List.of(new LimitedProjectMember(11, "bob", true, 4, "contributor", "TH1", "Thésaurus 1")),
                List.of(new AssignableRole(2, "admin"), new AssignableRole(3, "manager"))
        );

        ProjectDashboardResponse response = ProjectApiMapper.toDashboardResponse(dashboard);

        assertEquals(3, response.projectId());
        assertEquals("Projet X", response.projectName());
        assertTrue(response.projectAdmin());
        assertEquals(2, response.callerRoleId());
        assertEquals(1, response.thesauri().size());
        assertEquals("TH1", response.thesauri().get(0).id());
        assertEquals(1, response.members().size());
        assertEquals("alice", response.members().get(0).username());
        assertEquals(1, response.limitedMembers().size());
        assertEquals("bob", response.limitedMembers().get(0).username());
        assertEquals(2, response.assignableRoles().size());
        assertEquals("admin", response.assignableRoles().get(0).name());
    }
}
