package fr.cnrs.opentheso.v2.candidat.api;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusScopedAuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CandidatAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireContributor(int userId, String thesaurusId) {
        thesaurusScopedAuthSupport.requireThesaurusContributor(userId, thesaurusId);
    }
}
