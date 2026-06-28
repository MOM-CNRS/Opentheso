package fr.cnrs.opentheso.v2.setting.api;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusScopedAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettingAuthSupportTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;
    @Mock
    private ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    private SettingAuthSupport settingAuthSupport;

    @BeforeEach
    void setUp() {
        settingAuthSupport = new SettingAuthSupport(apiKeyAuthenticationService, thesaurusScopedAuthSupport);
    }

    @Test
    void resolveUserId_delegatesToSharedService() {
        org.mockito.Mockito.when(apiKeyAuthenticationService.resolveUserId("x-key", "legacy")).thenReturn(9);

        assertEquals(9, settingAuthSupport.resolveUserId("x-key", "legacy"));
        verify(apiKeyAuthenticationService).resolveUserId("x-key", "legacy");
    }

    @Test
    void requireThesaurusManager_delegatesToSharedSupport() {
        settingAuthSupport.requireThesaurusManager(5, "TH1");

        verify(thesaurusScopedAuthSupport).requireThesaurusManager(5, "TH1");
    }
}
