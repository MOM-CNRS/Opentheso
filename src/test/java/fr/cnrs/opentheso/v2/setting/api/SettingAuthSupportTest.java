package fr.cnrs.opentheso.v2.setting.api;

import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.user.api.AccountAuthSupport;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingAuthSupportTest {

    @Mock
    private AccountAuthSupport accountAuthSupport;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;

    private SettingAuthSupport settingAuthSupport;

    @BeforeEach
    void setUp() {
        settingAuthSupport = new SettingAuthSupport(
                accountAuthSupport,
                userProfileService,
                thesaurusAccessService
        );
    }

    @Test
    void resolveUserId_delegatesToAccountAuthSupport() {
        when(accountAuthSupport.resolveUserId("x-key", "legacy")).thenReturn(9);

        assertEquals(9, settingAuthSupport.resolveUserId("x-key", "legacy"));
        verify(accountAuthSupport).resolveUserId("x-key", "legacy");
    }

    @Test
    void requireThesaurusManager_allowsManager() {
        when(userProfileService.getProfile(5)).thenReturn(sampleProfile(false));
        when(thesaurusAccessService.canManageThesaurus(5, false, "TH1")).thenReturn(true);

        settingAuthSupport.requireThesaurusManager(5, "TH1");
    }

    @Test
    void requireThesaurusManager_throwsWhenDenied() {
        when(userProfileService.getProfile(5)).thenReturn(sampleProfile(false));
        when(thesaurusAccessService.canManageThesaurus(5, false, "TH1")).thenReturn(false);

        assertThrows(SettingAccessDeniedException.class,
                () -> settingAuthSupport.requireThesaurusManager(5, "TH1"));
    }

    @Test
    void requireThesaurusManager_usesSuperAdminFlagFromProfile() {
        when(userProfileService.getProfile(1)).thenReturn(sampleProfile(true));
        when(thesaurusAccessService.canManageThesaurus(1, true, "TH1")).thenReturn(true);

        settingAuthSupport.requireThesaurusManager(1, "TH1");

        verify(thesaurusAccessService).canManageThesaurus(1, true, "TH1");
    }

    private static UserProfile sampleProfile(boolean superAdmin) {
        return new UserProfile(1, "alice", "a@b.c", false, superAdmin, false, LocalDate.now(), true);
    }
}
