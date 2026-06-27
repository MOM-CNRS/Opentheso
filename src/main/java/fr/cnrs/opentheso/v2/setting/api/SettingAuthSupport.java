package fr.cnrs.opentheso.v2.setting.api;

import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.user.api.AccountAuthSupport;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingAuthSupport {

    private final AccountAuthSupport accountAuthSupport;
    private final UserProfileService userProfileService;
    private final ThesaurusAccessService thesaurusAccessService;

    public int resolveUserId(String xApiKey, String legacyApiKey) {
        return accountAuthSupport.resolveUserId(xApiKey, legacyApiKey);
    }

    public void requireThesaurusManager(int userId, String thesaurusId) {
        var profile = userProfileService.getProfile(userId);
        if (!thesaurusAccessService.canManageThesaurus(userId, profile.superAdmin(), thesaurusId)) {
            throw new SettingAccessDeniedException();
        }
    }
}
