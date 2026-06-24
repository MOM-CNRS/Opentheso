package fr.cnrs.opentheso.v2.project.api;

import fr.cnrs.opentheso.v2.project.api.dto.CreateProjectRequest;
import fr.cnrs.opentheso.v2.project.api.dto.MoveThesaurusRequest;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateProjectLabelRequest;
import fr.cnrs.opentheso.v2.project.model.AssignableRole;
import fr.cnrs.opentheso.v2.project.model.ProjectDashboard;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.project.service.ProjectAdminService;
import fr.cnrs.opentheso.v2.project.service.ProjectManagementService;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectAuthSupport projectAuthSupport;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ProjectAdminService projectAdminService;
    @Mock
    private ProjectManagementService projectManagementService;
    @Mock
    private ProjectMemberService projectMemberService;

    private ProjectController projectController;

    @BeforeEach
    void setUp() {
        projectController = new ProjectController(
                projectAuthSupport,
                userProfileService,
                projectAdminService,
                projectManagementService,
                projectMemberService
        );
        ReflectionTestUtils.setField(projectController, "defaultWorkLanguage", "fr");
    }

    @Test
    void listProjects_delegatesToService() {
        when(projectAuthSupport.resolveUserId("key", null)).thenReturn(3);
        when(projectAdminService.listAccessibleProjects(3))
                .thenReturn(List.of(new ProjectSummary(1, "Projet A")));

        var response = projectController.listProjects("key", null);

        assertEquals(1, response.size());
        assertEquals("Projet A", response.get(0).name());
    }

    @Test
    void createProject_delegatesToService() {
        when(projectAuthSupport.resolveUserId(null, "legacy")).thenReturn(5);
        when(userProfileService.getProfile(5)).thenReturn(
                new UserProfile(5, "admin", "a@b.c", false, true, true, null, true)
        );
        when(projectManagementService.createProject(5, true, "Nouveau"))
                .thenReturn(new ProjectSummary(9, "Nouveau"));

        var response = projectController.createProject(null, "legacy", new CreateProjectRequest("Nouveau"));

        assertEquals(9, response.id());
        verify(projectManagementService).createProject(5, true, "Nouveau");
    }

    @Test
    void getDashboard_delegatesToService() {
        when(projectAuthSupport.resolveUserId("key", null)).thenReturn(3);
        when(projectAdminService.loadDashboard(3, 7, "fr")).thenReturn(
                new ProjectDashboard(7, "Projet", true, 2, List.of(), List.of(), List.of(),
                        List.of(new AssignableRole(2, "admin")))
        );

        var response = projectController.getDashboard("key", null, 7);

        assertEquals(7, response.projectId());
        assertEquals("Projet", response.projectName());
        assertEquals(1, response.assignableRoles().size());
    }

    @Test
    void renameProject_delegatesToService() {
        when(projectAuthSupport.resolveUserId("key", null)).thenReturn(4);
        when(userProfileService.getProfile(4)).thenReturn(
                new UserProfile(4, "admin", "a@b.c", false, false, true, null, true)
        );
        when(projectManagementService.renameProject(4, false, 2, "Renommé"))
                .thenReturn(new ProjectSummary(2, "Renommé"));

        var response = projectController.renameProject(
                "key", null, 2, new UpdateProjectLabelRequest("Renommé")
        );

        assertEquals("Renommé", response.name());
        verify(projectManagementService).renameProject(4, false, 2, "Renommé");
    }

    @Test
    void deleteProject_delegatesToService() {
        when(projectAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(
                new UserProfile(1, "root", "root@example.com", false, true, true, null, true)
        );

        ResponseEntity<Void> response = projectController.deleteProject("key", null, 8);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectManagementService).deleteProject(1, true, 8);
    }

    @Test
    void moveThesaurus_delegatesToService() {
        when(projectAuthSupport.resolveUserId("key", null)).thenReturn(2);
        when(userProfileService.getProfile(2)).thenReturn(
                new UserProfile(2, "admin", "a@b.c", false, false, true, null, true)
        );

        var response = projectController.moveThesaurus(
                "key", null, 3, "TH1", new MoveThesaurusRequest(7)
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).moveThesaurus(2, false, 3, "TH1", 7);
    }
}
