package fr.cnrs.opentheso.v2.publicapi.system.service;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyMissingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationPublicServiceTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;
    @Mock
    private UserProfileService userProfileService;

    private ApiKeyValidationPublicService service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyValidationPublicService(apiKeyAuthenticationService, userProfileService);
    }

    @Test
    void validate_returnsUserProfileWhenKeyValid() {
        when(apiKeyAuthenticationService.resolveUserId("x-key", null)).thenReturn(7);
        when(userProfileService.getProfile(7))
                .thenReturn(new UserProfile(7, "admin", "a@b.c", false, true, false, null, true));

        var response = service.validate("x-key", null);

        assertTrue(response.valid());
        assertEquals(7, response.userId());
        assertEquals("admin", response.username());
        assertTrue(response.superAdmin());
    }

    @Test
    void validate_propagatesMissingApiKeyException() {
        when(apiKeyAuthenticationService.resolveUserId(null, null)).thenThrow(new ApiKeyMissingException());

        assertThrows(ApiKeyMissingException.class, () -> service.validate(null, null));
    }
}
