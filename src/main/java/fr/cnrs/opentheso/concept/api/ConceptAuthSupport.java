package fr.cnrs.opentheso.concept.api;

import fr.cnrs.opentheso.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.shared.auth.ThesaurusScopedAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConceptAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireThesaurusContributor(int userId, String thesaurusId) {
        thesaurusScopedAuthSupport.requireThesaurusContributor(userId, thesaurusId);
    }
}
