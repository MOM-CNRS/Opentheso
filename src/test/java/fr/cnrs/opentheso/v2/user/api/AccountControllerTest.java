package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.v2.user.api.dto.ChangePasswordRequest;
import fr.cnrs.opentheso.v2.user.api.dto.UpdateAlertMailRequest;
import fr.cnrs.opentheso.v2.user.api.dto.UpdateEmailRequest;
import fr.cnrs.opentheso.v2.user.api.dto.UpdateUsernameRequest;
import fr.cnrs.opentheso.v2.user.model.ApiKeyGenerationResult;
import fr.cnrs.opentheso.v2.user.model.ProfileWithRoles;
import fr.cnrs.opentheso.v2.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.v2.user.model.ThesaurusRoleOverview;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserApiKeyService;
import fr.cnrs.opentheso.v2.user.service.UserPasswordService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountAuthSupport accountAuthSupport;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserPasswordService userPasswordService;
    @Mock
    private UserApiKeyService userApiKeyService;

    private AccountController accountController;

    private static final UserProfile PROFILE = new UserProfile(
            7, "alice", "alice@example.com", true, false, true, null, true
    );

    @BeforeEach
    void setUp() {
        accountController = new AccountController(
                accountAuthSupport,
                userProfileService,
                userPasswordService,
                userApiKeyService
        );
        when(accountAuthSupport.resolveUserId("api-key", null)).thenReturn(7);
    }

    @Test
    void getProfile_returnsMappedResponse() {
        when(userProfileService.getProfile(7)).thenReturn(PROFILE);

        var response = accountController.getProfile("api-key", null);

        assertEquals(7, response.id());
        assertEquals("alice", response.username());
        assertTrue(response.apiKeySectionVisible());
    }

    @Test
    void updateUsername_delegatesToService() {
        when(userProfileService.updateUsername(7, "bob")).thenReturn(
                new UserProfile(7, "bob", "alice@example.com", true, false, true, null, true)
        );

        var response = accountController.updateUsername("api-key", null, new UpdateUsernameRequest("bob"));

        assertEquals("bob", response.username());
    }

    @Test
    void updateEmail_delegatesToService() {
        when(userProfileService.updateEmail(7, "new@example.com")).thenReturn(
                new UserProfile(7, "alice", "new@example.com", true, false, true, null, true)
        );

        var response = accountController.updateEmail(
                "api-key", null, new UpdateEmailRequest("new@example.com")
        );

        assertEquals("new@example.com", response.email());
    }

    @Test
    void updateAlertMail_delegatesToService() {
        when(userProfileService.updateAlertMail(7, false)).thenReturn(
                new UserProfile(7, "alice", "alice@example.com", false, false, true, null, true)
        );

        var response = accountController.updateAlertMail(
                "api-key", null, new UpdateAlertMailRequest(false)
        );

        assertFalse(response.alertMail());
    }

    @Test
    void changePassword_returnsNoContent() {
        var response = accountController.changePassword(
                "api-key", null, new ChangePasswordRequest("Abcd1234!", "Abcd1234!")
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userPasswordService).changePassword(7, "Abcd1234!", "Abcd1234!");
    }

    @Test
    void getRoles_usesSingleProfileLoad() {
        when(userProfileService.getProfileWithRoles(7)).thenReturn(new ProfileWithRoles(
                PROFILE,
                List.of(new ProjectRoleOverview(3, "Projet A", List.of(
                        new ThesaurusRoleOverview("TH1", "Thésaurus", "admin")
                )))
        ));

        var response = accountController.getRoles("api-key", null);

        assertFalse(response.superAdmin());
        assertEquals(1, response.projectRoles().size());
        verify(userProfileService).getProfileWithRoles(7);
    }

    @Test
    void regenerateApiKey_returnsPlainTextKeyOnce() {
        when(userApiKeyService.regenerateApiKey(7)).thenReturn(
                new ApiKeyGenerationResult("new-plain-key", PROFILE)
        );

        var response = accountController.regenerateApiKey("api-key", null);

        assertEquals("new-plain-key", response.plainTextApiKey());
        assertEquals(7, response.profile().id());
    }
}
