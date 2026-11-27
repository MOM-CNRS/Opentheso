package fr.cnrs.opentheso.shared.auth;

import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;

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
        when(apiKeyLookupService.lookup("key-v2")).thenReturn(Optional.of(
                new ApiKeyLookupService.ApiKeyMatch(10, "u", ApiKeyState.VALID)));

        assertEquals(10, apiKeyAuthenticationService.resolveUserId("key-v2", null));
    }

    @Test
    void resolveUserId_throwsWhenKeyMissing() {
        assertThrows(ApiKeyMissingException.class,
                () -> apiKeyAuthenticationService.resolveUserId(null, null));
    }

    @Test
    void resolveUserId_throwsWhenKeyInvalid() {
        when(apiKeyLookupService.lookup("bad-key")).thenReturn(Optional.empty());

        assertThrows(ApiKeyInvalidException.class,
                () -> apiKeyAuthenticationService.resolveUserId("bad-key", null));
    }

    @Test
    void resolveUserId_refusesAnExpiredKeyAsExpired() {
        when(apiKeyLookupService.lookup("old-key")).thenReturn(Optional.of(
                new ApiKeyLookupService.ApiKeyMatch(10, "u", ApiKeyState.EXPIRED)));

        ApiKeyInvalidException e = assertThrows(ApiKeyInvalidException.class,
                () -> apiKeyAuthenticationService.resolveUserId("old-key", null));
        assertEquals(ApiKeyState.EXPIRED, e.getState());
    }
}
