package fr.cnrs.opentheso.v2.graph.api;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusScopedAuthSupport;
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
