package fr.cnrs.opentheso.v2.shared.auth;

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
class ApiKeyAuthenticationServiceTest {

    @Mock
    private ApiKeyLookupService apiKeyLookupService;

    @InjectMocks
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Test
    void resolveUserId_usesXApiKeyHeader() {
        when(apiKeyLookupService.findUserIdByApiKey("key-v2")).thenReturn(Optional.of(10));

        assertEquals(10, apiKeyAuthenticationService.resolveUserId("key-v2", null));
    }

    @Test
    void resolveUserId_throwsWhenKeyMissing() {
        assertThrows(ApiKeyMissingException.class,
                () -> apiKeyAuthenticationService.resolveUserId(null, null));
    }

    @Test
    void resolveUserId_throwsWhenKeyInvalid() {
        when(apiKeyLookupService.findUserIdByApiKey("bad-key")).thenReturn(Optional.empty());

        assertThrows(ApiKeyInvalidException.class,
                () -> apiKeyAuthenticationService.resolveUserId("bad-key", null));
    }
}
