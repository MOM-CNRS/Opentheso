package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyMissingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuthSupportTest {

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private AccountAuthSupport accountAuthSupport;

    @Test
    void resolveUserId_usesXApiKeyHeader() {
        User user = new User();
        user.setId(10);
        when(apiKeyService.findUserByApiKey("key-v2")).thenReturn(Optional.of(user));

        assertEquals(10, accountAuthSupport.resolveUserId("key-v2", null));
    }

    @Test
    void resolveUserId_fallsBackToLegacyApiKeyHeader() {
        User user = new User();
        user.setId(11);
        when(apiKeyService.findUserByApiKey("key-v1")).thenReturn(Optional.of(user));

        assertEquals(11, accountAuthSupport.resolveUserId(null, "key-v1"));
        assertEquals(11, accountAuthSupport.resolveUserId("  ", "key-v1"));
    }

    @Test
    void resolveUserId_prefersXApiKeyOverLegacyHeader() {
        User user = new User();
        user.setId(12);
        when(apiKeyService.findUserByApiKey("primary")).thenReturn(Optional.of(user));

        assertEquals(12, accountAuthSupport.resolveUserId("primary", "legacy"));
    }

    @Test
    void resolveUserId_throwsWhenKeyMissing() {
        assertThrows(ApiKeyMissingException.class,
                () -> accountAuthSupport.resolveUserId(null, null));
        assertThrows(ApiKeyMissingException.class,
                () -> accountAuthSupport.resolveUserId("  ", "  "));
    }

    @Test
    void resolveUserId_throwsWhenKeyInvalid() {
        when(apiKeyService.findUserByApiKey("bad-key")).thenReturn(Optional.empty());

        assertThrows(ApiKeyInvalidException.class,
                () -> accountAuthSupport.resolveUserId("bad-key", null));
    }
}
