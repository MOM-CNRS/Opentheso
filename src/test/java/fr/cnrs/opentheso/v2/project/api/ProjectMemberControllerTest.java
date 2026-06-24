package fr.cnrs.opentheso.v2.project.api;

import fr.cnrs.opentheso.v2.project.api.dto.AddProjectMemberRequest;
import fr.cnrs.opentheso.v2.project.api.dto.CreateProjectMemberRequest;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateLimitedMemberRoleRequest;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateMemberProfileRequest;
import fr.cnrs.opentheso.v2.project.api.dto.UpdateProjectMemberRoleRequest;
import fr.cnrs.opentheso.v2.project.model.CreatedProjectMember;
import fr.cnrs.opentheso.v2.project.model.UserSearchResult;
import fr.cnrs.opentheso.v2.project.service.ProjectMemberService;
import fr.cnrs.opentheso.v2.user.api.dto.ChangePasswordRequest;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberControllerTest {

    @Mock
    private ProjectAuthSupport projectAuthSupport;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ProjectMemberService projectMemberService;

    private ProjectMemberController projectMemberController;

    private static final UserProfile ADMIN_PROFILE = new UserProfile(
            1, "admin", "admin@example.com", false, false, true, null, true
    );

    @BeforeEach
    void setUp() {
        projectMemberController = new ProjectMemberController(
                projectAuthSupport,
                userProfileService,
                projectMemberService
        );
        when(projectAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(ADMIN_PROFILE);
    }

    @Test
    void searchUsers_delegatesToService() {
        when(projectMemberService.searchUsers(1, false, 3, "ali")).thenReturn(
                List.of(new UserSearchResult(10, "alice", "alice@example.com"))
        );

        var response = projectMemberController.searchUsers("key", null, 3, "ali");

        assertEquals(1, response.size());
        assertEquals("alice", response.get(0).username());
    }

    @Test
    void createMember_delegatesToService() {
        when(projectMemberService.createMember(
                1, false, 3, "bob", "bob@example.com", "CNRS", true, 4, false, List.of(),
                "Abcd1234!", "Abcd1234!", "DIRECT"
        )).thenReturn(new CreatedProjectMember(12, "bob", "bob@example.com"));

        var response = projectMemberController.createMember(
                "key", null, 3,
                new CreateProjectMemberRequest(
                        "bob", "bob@example.com", "CNRS", true, 4, false, List.of(),
                        "Abcd1234!", "Abcd1234!", "DIRECT"
                )
        );

        assertEquals(12, response.userId());
        assertEquals("bob", response.username());
    }

    @Test
    void addExistingMember_returnsNoContent() {
        var response = projectMemberController.addExistingMember(
                "key", null, 3, 10, new AddProjectMemberRequest(4)
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).addExistingMember(1, false, 3, 10, 4);
    }

    @Test
    void updateMemberRole_delegatesToService() {
        var response = projectMemberController.updateMemberRole(
                "key", null, 3, 10,
                new UpdateProjectMemberRoleRequest(3, false, List.of())
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).updateMemberRole(1, false, 3, 10, 3, false, List.of());
    }

    @Test
    void updateLimitedMemberRole_delegatesToService() {
        var response = projectMemberController.updateLimitedMemberRole(
                "key", null, 3, 10,
                new UpdateLimitedMemberRoleRequest(4, 3, "TH1", true)
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).updateLimitedMemberRole(1, false, 3, 10, 4, 3, "TH1", true);
    }

    @Test
    void removeMember_delegatesToService() {
        var response = projectMemberController.removeMember("key", null, 3, 10);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).removeMember(1, false, 3, 10);
    }

    @Test
    void removeLimitedRole_delegatesToService() {
        var response = projectMemberController.removeLimitedRole("key", null, 3, 10, "TH1", 4);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).removeLimitedRole(1, false, 3, 10, 4, "TH1");
    }

    @Test
    void updateMemberProfile_delegatesToService() {
        var response = projectMemberController.updateMemberProfile(
                "key", null, 3, 10,
                new UpdateMemberProfileRequest("bob", "bob@example.com", true, "CNRS", true)
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).updateMemberProfile(
                1, false, 3, 10, "bob", "bob@example.com", true, "CNRS", true
        );
    }

    @Test
    void setMemberPassword_delegatesToService() {
        var response = projectMemberController.setMemberPassword(
                "key", null, 3, 10, new ChangePasswordRequest("Abcd1234!", "Abcd1234!")
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectMemberService).setMemberPassword(1, false, 3, 10, "Abcd1234!", "Abcd1234!");
    }
}
