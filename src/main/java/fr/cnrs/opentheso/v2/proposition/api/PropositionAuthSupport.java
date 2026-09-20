package fr.cnrs.opentheso.v2.proposition.api;

import fr.cnrs.opentheso.v2.proposition.policy.PropositionAccessPolicy;
import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.shared.exception.ModuleAccessDeniedException;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropositionAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final PropositionAccessPolicy propositionAccessPolicy;
    private final UserProfileService userProfileService;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireSubmit(String thesaurusId, String workLang) {
        requireThesaurusSelected(thesaurusId);
        if (!propositionAccessPolicy.canSubmit(thesaurusId, workLang)) {
            throw new ModuleAccessDeniedException("proposition");
        }
    }

    public void requireBoard(int userId, String thesaurusId) {
        requireThesaurusSelected(thesaurusId);
        if (!propositionAccessPolicy.canAccessBoard(userId, thesaurusId)) {
            throw new ModuleAccessDeniedException("proposition");
        }
    }

    public void requireDecide(int userId, String thesaurusId, String authorEmail) {
        requireThesaurusSelected(thesaurusId);
        String userEmail = resolveEmail(userId);
        if (!propositionAccessPolicy.canDecide(userId, thesaurusId, userEmail, authorEmail)) {
            throw new ModuleAccessDeniedException("proposition");
        }
    }

    public String resolveUsername(int userId) {
        try {
            return userProfileService.getProfile(userId).username();
        } catch (Exception ex) {
            return "api-user-" + userId;
        }
    }

    public String resolveEmail(int userId) {
        try {
            return userProfileService.getProfile(userId).email();
        } catch (Exception ex) {
            return null;
        }
    }

    private static void requireThesaurusSelected(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new ModuleAccessDeniedException("thesaurus");
        }
    }
}
