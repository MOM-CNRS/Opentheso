package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.shared.exception.ModuleAccessDeniedException;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThesaurusScopedAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final UserProfileService userProfileService;
    private final RightsService rightsService;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireThesaurusManager(int userId, String thesaurusId) {
        requireThesaurusSelected(thesaurusId);
        if (!rightsService.can(userId, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus(thesaurusId))) {
            throw new SettingAccessDeniedException();
        }
    }

    public void requireThesaurusContributor(int userId, String thesaurusId) {
        requireThesaurusSelected(thesaurusId);
        if (!rightsService.can(userId, Permission.CONTRIBUTE_ON_THESAURUS, AuthTarget.thesaurus(thesaurusId))) {
            throw new ModuleAccessDeniedException("candidat");
        }
    }

    public void requireToolboxEditionAccess(int userId) {
        if (!rightsService.can(userId, Permission.TOOLBOX_EDITION)) {
            throw new ModuleAccessDeniedException("toolbox");
        }
    }

    public void requireToolboxStatisticsAccess(int userId) {
        if (!rightsService.can(userId, Permission.TOOLBOX_STATISTICS)) {
            throw new ModuleAccessDeniedException("toolbox");
        }
    }

    public void requireAuthenticated(int userId) {
        userProfileService.getProfile(userId);
    }

    private void requireThesaurusSelected(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new ModuleAccessDeniedException("thesaurus");
        }
    }
}
