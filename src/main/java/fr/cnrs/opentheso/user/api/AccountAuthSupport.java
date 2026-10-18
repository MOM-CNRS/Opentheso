package fr.cnrs.opentheso.user.api;

import fr.cnrs.opentheso.shared.auth.ApiKeyAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }
}
