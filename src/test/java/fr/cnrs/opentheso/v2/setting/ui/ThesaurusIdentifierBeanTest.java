package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.component.UIComponent;
import jakarta.faces.event.AjaxBehaviorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusIdentifierBeanTest {

    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private UserSession userSession;
    @Mock
    private RightsService rightsService;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private fr.cnrs.opentheso.v2.setting.service.ThesaurusSettingsPersistService persistService;
    @Mock
    private ThesaurusCorpusBean corpusBean;
    @Mock
    private ThesaurusAlignmentBean alignmentBean;
    @Mock
    private fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean thesaurusViewBean;

    private ThesaurusIdentifierBean bean;
    private ThesaurusPreferenceBean preferenceBean;

    @BeforeEach
    void setUp() {
        ThesaurusContext thesaurusContext = new ThesaurusContext(
                thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        SettingsAccess access = new SettingsAccess(thesaurusContext, userSession, rightsService);
        preferenceBean = new ThesaurusPreferenceBean(
                access, thesaurusContext, thesaurusPreferenceService, persistService, corpusBean, alignmentBean,
                thesaurusViewBean);
        bean = new ThesaurusIdentifierBean(preferenceBean);
        when(thesaurusPreferenceService.loadPreferencesOrNull("th17", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());
        lenient().when(userSession.getCurrentUserId()).thenReturn(2);
        lenient().when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);
    }

    @Test
    void selectIdentifierServer_turnsOffOtherServersWhenOneIsEnabled() {
        PreferenceEditor preference = bean.getPreference();
        preference.setUseHandle(true);
        preference.setUseOpenArk(true);
        preference.setUseArkLocal(true);

        bean.selectIdentifierServer(ajaxEvent("previewUseArkLocal"));

        assertTrue(preference.isUseArkLocal());
        assertFalse(preference.isUseArk());
        assertFalse(preference.isUseHandle());
        assertFalse(preference.isUseOpenArk());
    }

    @Test
    void selectIdentifierServer_activatingHandleTurnsOffArkLocalAndOpenArk() {
        PreferenceEditor preference = bean.getPreference();
        preference.setUseArkLocal(true);
        preference.setUseHandle(true);
        preference.setUseOpenArk(true);

        bean.selectIdentifierServer(ajaxEvent("previewUseHandle"));

        assertTrue(preference.isUseHandle());
        assertFalse(preference.isUseArk());
        assertFalse(preference.isUseArkLocal());
        assertFalse(preference.isUseOpenArk());
    }

    @Test
    void selectIdentifierServer_keepsOthersUnchangedWhenTurningOff() {
        PreferenceEditor preference = bean.getPreference();
        preference.setUseHandle(false);
        preference.setUseArkLocal(false);

        bean.selectIdentifierServer(ajaxEvent("previewUseHandle"));

        assertFalse(preference.isUseHandle());
        assertFalse(preference.isUseArkLocal());
    }

    private static AjaxBehaviorEvent ajaxEvent(String componentId) {
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(component.getId()).thenReturn(componentId);
        AjaxBehaviorEvent event = org.mockito.Mockito.mock(AjaxBehaviorEvent.class);
        when(event.getComponent()).thenReturn(component);
        return event;
    }
}
