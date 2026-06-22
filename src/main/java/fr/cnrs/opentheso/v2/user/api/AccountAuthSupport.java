package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyMissingException;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountAuthSupport {

    private final ApiKeyService apiKeyService;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        String apiKey = firstNonBlank(xApiKey, legacyApiKey);
        if (apiKey == null) {
            throw new ApiKeyMissingException();
        }
        return apiKeyService.findUserByApiKey(apiKey)
                .map(User::getId)
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
