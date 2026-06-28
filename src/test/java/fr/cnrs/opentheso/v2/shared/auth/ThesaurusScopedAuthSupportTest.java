package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.exception.ModuleAccessDeniedException;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusScopedAuthSupportTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;
    @Mock
    private UserCapabilityService userCapabilityService;
    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    @InjectMocks
    private ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    @Test
    void requireThesaurusManager_throwsWhenDenied() {
        when(userProfileService.getProfile(5)).thenReturn(sampleProfile(false));
        when(thesaurusAccessService.canManageThesaurus(5, false, "TH1")).thenReturn(false);

        assertThrows(SettingAccessDeniedException.class,
                () -> thesaurusScopedAuthSupport.requireThesaurusManager(5, "TH1"));
    }

    @Test
    void requireThesaurusContributor_throwsWhenNoRole() {
        when(userCapabilityService.loadSessionUser(5))
                .thenReturn(new SessionUser(5, "u", "u@x", false, false, false, false));
        when(thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(5, "TH1"))
                .thenReturn(Optional.empty());

        assertThrows(ModuleAccessDeniedException.class,
                () -> thesaurusScopedAuthSupport.requireThesaurusContributor(5, "TH1"));
    }

    @Test
    void requireToolboxEditionAccess_throwsForContributorOnly() {
        when(userCapabilityService.loadSessionUser(5))
                .thenReturn(new SessionUser(5, "u", "u@x", false, false, true, false));

        assertThrows(ModuleAccessDeniedException.class,
                () -> thesaurusScopedAuthSupport.requireToolboxEditionAccess(5));
    }

    private static UserProfile sampleProfile(boolean superAdmin) {
        return new UserProfile(1, "alice", "a@b.c", false, superAdmin, false, LocalDate.now(), true);
    }
}
