package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.exception.ModuleAccessDeniedException;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThesaurusScopedAuthSupport {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final UserProfileService userProfileService;
    private final ThesaurusAccessService thesaurusAccessService;
    private final UserCapabilityService userCapabilityService;
    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return apiKeyAuthenticationService.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireThesaurusManager(int userId, String thesaurusId) {
        requireThesaurusSelected(thesaurusId);
        var profile = userProfileService.getProfile(userId);
        if (!thesaurusAccessService.canManageThesaurus(userId, profile.superAdmin(), thesaurusId)) {
            throw new SettingAccessDeniedException();
        }
    }

    public void requireThesaurusContributor(int userId, String thesaurusId) {
        requireThesaurusSelected(thesaurusId);
        var capabilities = userCapabilityService.loadSessionUser(userId);
        if (!hasRoleOnThesaurus(userId, capabilities.superAdmin(), thesaurusId, ProjectAccessPolicy.ROLE_CONTRIBUTOR)) {
            throw new ModuleAccessDeniedException("candidat");
        }
    }

    public void requireToolboxEditionAccess(int userId) {
        var capabilities = userCapabilityService.loadSessionUser(userId);
        if (!capabilities.projectAdmin() && !capabilities.superAdmin()) {
            throw new ModuleAccessDeniedException("toolbox");
        }
    }

    public void requireToolboxStatisticsAccess(int userId) {
        var capabilities = userCapabilityService.loadSessionUser(userId);
        if (!capabilities.manager() && !capabilities.superAdmin()) {
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

    private boolean hasRoleOnThesaurus(int userId, boolean superAdmin, String thesaurusId, int maxRoleId) {
        if (superAdmin) {
            return true;
        }
        return thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(userId, thesaurusId)
                .map(roleId -> roleId <= maxRoleId)
                .orElse(false);
    }
}
