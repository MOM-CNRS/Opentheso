package fr.cnrs.opentheso.v2.toolbox.api;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusScopedAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolboxAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireEditionAccess(int userId) {
        thesaurusScopedAuthSupport.requireToolboxEditionAccess(userId);
    }

    public void requireStatisticsAccess(int userId) {
        thesaurusScopedAuthSupport.requireToolboxStatisticsAccess(userId);
    }
}
