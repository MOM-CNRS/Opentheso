package fr.cnrs.opentheso.graph.api;

import fr.cnrs.opentheso.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.shared.auth.ThesaurusScopedAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GraphAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireAuthenticated(int userId) {
        thesaurusScopedAuthSupport.requireAuthenticated(userId);
    }
}
