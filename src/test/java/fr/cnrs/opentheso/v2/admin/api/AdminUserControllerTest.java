package fr.cnrs.opentheso.v2.admin.api;

import fr.cnrs.opentheso.v2.admin.api.dto.CreateAdminUserRequest;
import fr.cnrs.opentheso.v2.admin.api.dto.UpdateAdminApiKeyRequest;
import fr.cnrs.opentheso.v2.admin.api.dto.UpdateAdminUserRequest;
import fr.cnrs.opentheso.v2.admin.model.CreatedAdminUser;
import fr.cnrs.opentheso.v2.admin.service.AdminUserService;
import fr.cnrs.opentheso.v2.user.api.dto.ChangePasswordRequest;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminAuthSupport adminAuthSupport;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private AdminUserService adminUserService;

    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        adminUserController = new AdminUserController(adminAuthSupport, userProfileService, adminUserService);
    }

    @Test
    void createUser_delegatesToService() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        when(adminUserService.createUser(
                true,
                "alice",
                "alice@test.fr",
                false,
                2,
                3,
                false,
                List.of(),
                "Secret1!",
                "Secret1!"
        )).thenReturn(new CreatedAdminUser(10, "alice", "alice@test.fr"));

        var request = new CreateAdminUserRequest(
                "alice",
                "alice@test.fr",
                false,
                2,
                3,
                false,
                List.of(),
                "Secret1!",
                "Secret1!"
        );
        var response = adminUserController.createUser("key", null, request);

        assertEquals(10, response.userId());
        assertEquals("alice", response.username());
    }

    @Test
    void updateUser_delegatesToService() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));

        var response = adminUserController.updateUser(
                "key",
                null,
                5,
                new UpdateAdminUserRequest("bob", "bob@test.fr", true)
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminUserService).updateUser(true, 5, "bob", "bob@test.fr", true);
    }

    @Test
    void updatePassword_delegatesToService() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));

        var response = adminUserController.updatePassword(
                "key",
                null,
                5,
                new ChangePasswordRequest("Secret1!", "Secret1!")
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminUserService).updatePassword(true, 5, "Secret1!", "Secret1!");
    }

    @Test
    void updateApiKeySettings_delegatesToService() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));
        LocalDate expiresAt = LocalDate.of(2026, Month.DECEMBER, 31);

        var response = adminUserController.updateApiKeySettings(
                "key",
                null,
                5,
                new UpdateAdminApiKeyRequest(true, false, expiresAt)
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminUserService).updateApiKeySettings(true, 5, true, false, expiresAt);
    }

    @Test
    void deleteUser_delegatesToService() {
        when(adminAuthSupport.resolveUserId("key", null)).thenReturn(1);
        when(userProfileService.getProfile(1)).thenReturn(superAdminProfile(1));

        var response = adminUserController.deleteUser("key", null, 8);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminUserService).deleteUser(true, 8, 1);
    }

    private static UserProfile superAdminProfile(int id) {
        return new UserProfile(id, "admin", "admin@test.fr", false, true, true, LocalDate.of(2024, Month.JUNE, 15), true);
    }
}
