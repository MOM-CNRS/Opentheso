package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.shared.exception.ModuleAccessDeniedException;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusScopedAuthSupportTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private RightsService rightsService;

    @InjectMocks
    private ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    @Test
    void requireThesaurusManager_throwsWhenDenied() {
        when(rightsService.can(5, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus("TH1"))).thenReturn(false);

        assertThrows(SettingAccessDeniedException.class,
                () -> thesaurusScopedAuthSupport.requireThesaurusManager(5, "TH1"));
    }

    @Test
    void requireThesaurusContributor_throwsWhenNoRole() {
        when(rightsService.can(5, Permission.CONTRIBUTE_ON_THESAURUS, AuthTarget.thesaurus("TH1"))).thenReturn(false);

        assertThrows(ModuleAccessDeniedException.class,
                () -> thesaurusScopedAuthSupport.requireThesaurusContributor(5, "TH1"));
    }

    @Test
    void requireToolboxEditionAccess_throwsForContributorOnly() {
        when(rightsService.can(5, Permission.TOOLBOX_EDITION)).thenReturn(false);

        assertThrows(ModuleAccessDeniedException.class,
                () -> thesaurusScopedAuthSupport.requireToolboxEditionAccess(5));
    }
}
