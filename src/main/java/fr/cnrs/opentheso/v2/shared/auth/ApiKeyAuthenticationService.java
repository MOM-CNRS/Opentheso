package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyMissingException;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiKeyAuthenticationService {

    private final ApiKeyLookupService apiKeyLookupService;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        String apiKey = firstNonBlank(xApiKey, legacyApiKey);
        if (apiKey == null) {
            throw new ApiKeyMissingException();
        }
        return apiKeyLookupService.findUserIdByApiKey(apiKey)
                .orElseThrow(() -> new ApiKeyInvalidException(ApiKeyState.INVALID));
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }
}
