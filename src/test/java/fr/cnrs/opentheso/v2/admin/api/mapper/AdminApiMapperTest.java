package fr.cnrs.opentheso.v2.admin.api.mapper;

import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminApiMapperTest {

    @Test
    void toUserResponse_mapsAllFields() {
        var membership = new AdminUserMembership(1, "alice", 2, "Projet", 3, "Admin");

        var response = AdminApiMapper.toUserResponse(membership);

        assertEquals(1, response.userId());
        assertEquals("alice", response.username());
        assertEquals(2, response.projectId());
        assertEquals("Projet", response.projectName());
        assertEquals(3, response.roleId());
        assertEquals("Admin", response.roleName());
    }

    @Test
    void toUserResponses_mapsWholeList() {
        var membership1 = new AdminUserMembership(1, "alice", 2, "Projet", 3, "Admin");
        var membership2 = new AdminUserMembership(4, "bob", 5, "Projet 2", 6, "Contributeur");

        var responses = AdminApiMapper.toUserResponses(List.of(membership1, membership2));

        assertEquals(2, responses.size());
        assertEquals("alice", responses.get(0).username());
        assertEquals("bob", responses.get(1).username());
    }

    @Test
    void toProjectResponse_mapsFields() {
        var project = new ProjectSummary(1, "Projet");

        var response = AdminApiMapper.toProjectResponse(project);

        assertEquals(1, response.id());
        assertEquals("Projet", response.name());
    }

    @Test
    void toProjectResponses_mapsWholeList() {
        var project1 = new ProjectSummary(1, "Projet 1");
        var project2 = new ProjectSummary(2, "Projet 2");

        var responses = AdminApiMapper.toProjectResponses(List.of(project1, project2));

        assertEquals(2, responses.size());
        assertEquals("Projet 1", responses.get(0).name());
        assertEquals("Projet 2", responses.get(1).name());
    }

    @Test
    void toThesaurusResponse_mapsAllFields() {
        var created = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0);
        var thesaurus = new AdminThesaurus("TH1", "Test", 2, "Projet", true, created);

        var response = AdminApiMapper.toThesaurusResponse(thesaurus);

        assertEquals("TH1", response.id());
        assertEquals("Test", response.title());
        assertEquals(2, response.projectId());
        assertEquals("Projet", response.projectName());
        assertEquals(true, response.privateThesaurus());
        assertEquals(created, response.createdAt());
    }

    @Test
    void toThesaurusResponses_mapsWholeList() {
        var thesaurus = new AdminThesaurus("TH1", "Test", 2, "Projet", false, LocalDateTime.of(2024, Month.JUNE, 15, 12, 0));

        var responses = AdminApiMapper.toThesaurusResponses(List.of(thesaurus));

        assertEquals(1, responses.size());
        assertEquals("TH1", responses.get(0).id());
    }
}
