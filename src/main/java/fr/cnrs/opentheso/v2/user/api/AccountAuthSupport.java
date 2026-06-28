package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
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
