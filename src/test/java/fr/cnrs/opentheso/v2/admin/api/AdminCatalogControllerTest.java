package fr.cnrs.opentheso.v2.admin.api;

import fr.cnrs.opentheso.v2.admin.api.dto.MoveAdminThesaurusRequest;
import fr.cnrs.opentheso.v2.admin.model.AdminThesaurus;
import fr.cnrs.opentheso.v2.admin.model.AdminUserMembership;
import fr.cnrs.opentheso.v2.admin.service.AdminCatalogService;
import fr.cnrs.opentheso.v2.project.model.ProjectSummary;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogControllerTest {

    @Mock
    private AdminAuthSupport adminAuthSupport;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private AdminCatalogService adminCatalogService;

    private AdminCatalogController adminCatalogController;

    @BeforeEach
    void setUp() {
        adminCatalogController = new AdminCatalogController(
                adminAuthSupport,
                userProfileService,
                adminCatalogService
        );
    }

    @Test
    void listUsers_delegatesToServiceWhenNoCriteria() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(adminCatalogService.listAllUsers(true)).thenReturn(
                List.of(new AdminUserMembership(2, "alice", 3, "Projet", 4, "Admin"))
        );

        var response = adminCatalogController.listUsers("key", null, null, null);

        assertEquals(1, response.size());
        assertEquals("alice", response.get(0).username());
    }

    @Test
    void listUsers_delegatesToSearchWhenMailProvided() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(adminCatalogService.searchUsers(true, "alice@example.com", null)).thenReturn(
                List.of(new AdminUserMembership(2, "alice", 3, "Projet", 4, "Admin"))
        );

        var response = adminCatalogController.listUsers("key", null, "alice@example.com", null);

        assertEquals(1, response.size());
        verify(adminCatalogService).searchUsers(true, "alice@example.com", null);
    }

    @Test
    void listUsers_delegatesToSearchWhenUsernameProvided() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(adminCatalogService.searchUsers(true, null, "ali")).thenReturn(
                List.of(new AdminUserMembership(2, "alice", 3, "Projet", 4, "Admin"))
        );

        var response = adminCatalogController.listUsers("key", null, null, "ali");

        assertEquals(1, response.size());
        verify(adminCatalogService).searchUsers(true, null, "ali");
    }

    @Test
    void listProjects_delegatesToService() {
        when(adminAuthSupport.resolveUserId(null, "legacy")).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(adminCatalogService.listAllProjects(true)).thenReturn(
                List.of(new ProjectSummary(5, "Projet A"))
        );

        var response = adminCatalogController.listProjects(null, "legacy");

        assertEquals(1, response.size());
        assertEquals("Projet A", response.get(0).name());
    }

    @Test
    void listThesauri_delegatesToService() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(adminCatalogService.listAllThesauri(true, null)).thenReturn(
                List.of(new AdminThesaurus("th1", "Titre", 2, "Projet", false, LocalDateTime.now()))
        );

        var response = adminCatalogController.listThesauri("key", null);

        assertEquals(1, response.size());
        assertEquals("th1", response.get(0).id());
    }

    @Test
    void moveThesaurus_delegatesToService() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));

        var response = adminCatalogController.moveThesaurus(
                "key",
                null,
                "th1",
                new MoveAdminThesaurusRequest(9)
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminCatalogService).moveThesaurus(true, "th1", 9);
    }

    private static UserProfile superAdminProfile(int id) {
        return new UserProfile(id, "admin", "admin@test.fr", false, true, true, LocalDate.now(), true);
    }
}
