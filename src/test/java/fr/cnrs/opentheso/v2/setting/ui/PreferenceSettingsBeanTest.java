package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import static fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures.samplePreferences;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenceSettingsBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private LanguageBean languageBean;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;

    private PreferenceSettingsBean bean;

    @BeforeEach
    void setUp() {
        bean = new PreferenceSettingsBean(
                userSession,
                thesaurusContext,
                languageBean,
                thesaurusPreferenceService
        );
        lenient().when(languageBean.getIdLangue()).thenReturn("fr");
    }

    @Test
    void load_populatesEditorWhenUserCanManage() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());

        bean.load();

        assertNotNull(bean.getEditor());
        assertTrue(bean.isScreenAvailable());
        assertEquals("https://site/", bean.getEditor().getCheminSite());
        verify(thesaurusContext).syncFromViewParams();
    }

    @Test
    void load_clearsEditorWhenAccessDenied() {
        when(userSession.hasRoleAsAdmin()).thenReturn(false);

        bean.load();

        assertNull(bean.getEditor());
        assertFalse(bean.isScreenAvailable());
        verify(thesaurusPreferenceService, never()).loadPreferences(anyString(), anyString());
    }

    @Test
    void load_deniesWhenUserOrThesaurusMissing() {
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn(null);

        bean.load();

        assertNull(bean.getEditor());
        verify(thesaurusPreferenceService, never()).loadPreferences(anyString(), anyString());
    }

    @Test
    void save_persistsPreferencesWhenAllowed() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());
        bean.load();
        when(thesaurusPreferenceService.savePreferences(
                eq("TH1"), any(ThesaurusPreferences.class),
                nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq("fr")
        )).thenReturn(samplePreferences());

        try (MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces();
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.save();
            verify(thesaurusPreferenceService).savePreferences(
                    eq("TH1"), any(ThesaurusPreferences.class),
                    nullable(String.class), nullable(String.class),
                    nullable(String.class), nullable(String.class), eq("fr")
            );
            messageUtils.verify(() -> MessageUtils.showInformationMessage("Préférences enregistrées avec succès"));
        }
    }

    @Test
    void save_showsErrorWhenServiceFails() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());
        bean.load();
        when(thesaurusPreferenceService.savePreferences(
                eq("TH1"), any(ThesaurusPreferences.class),
                nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq("fr")
        )).thenThrow(new InvalidSettingDataException("erreur"));

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.save();
            messageUtils.verify(() -> MessageUtils.showErrorMessage("erreur"));
        }
    }

    @Test
    void clearNewPasswords_clearsTransientFields() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());
        bean.load();
        bean.getEditor().setNewPassArk("x");
        bean.getEditor().setNewPassHandle("y");

        bean.clearNewPasswords();

        assertNull(bean.getEditor().getNewPassArk());
        assertNull(bean.getEditor().getNewPassHandle());
    }

    @Test
    void updateSelectedServer_switchesToArk() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());
        bean.load();
        bean.getEditor().setUseArk(true);
        when(thesaurusPreferenceService.updateIdentifierServer("TH1", IdentifierServerType.ARK, "fr"))
                .thenReturn(samplePreferences(IdentifierServerType.ARK, true));

        bean.updateSelectedServer("ark");

        assertTrue(bean.getEditor().isUseArk());
    }

    @Test
    void updateSelectedServer_switchesToHandle() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());
        bean.load();
        bean.getEditor().setUseHandle(true);
        when(thesaurusPreferenceService.updateIdentifierServer("TH1", IdentifierServerType.HANDLE, "fr"))
                .thenReturn(samplePreferences(IdentifierServerType.HANDLE, false));

        bean.updateSelectedServer("handle");

        assertTrue(bean.getEditor().isUseHandle());
    }

    @Test
    void updateSelectedServer_ignoresUnknownKey() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());
        bean.load();

        bean.updateSelectedServer("unknown");

        verify(thesaurusPreferenceService, never()).updateIdentifierServer(anyString(), any(), anyString());
    }

    @Test
    void updateSelectedServer_showsErrorWhenServiceFails() {
        grantAccess();
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr")).thenReturn(samplePreferences());
        bean.load();
        bean.getEditor().setUseArkLocal(true);
        when(thesaurusPreferenceService.updateIdentifierServer("TH1", IdentifierServerType.ARK_LOCAL, "fr"))
                .thenThrow(new InvalidSettingDataException("serveur invalide"));

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.updateSelectedServer("arklocal");
            messageUtils.verify(() -> MessageUtils.showErrorMessage("serveur invalide"));
        }
    }

    private void grantAccess() {
        when(userSession.hasRoleAsAdmin()).thenReturn(true);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");
    }

    private MockedStatic<PrimeFaces> mockPrimeFaces() {
        MockedStatic<PrimeFaces> primeFaces = mockStatic(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        PrimeFaces primeFacesInstance = mock(PrimeFaces.class);
        primeFaces.when(PrimeFaces::current).thenReturn(primeFacesInstance);
        when(primeFacesInstance.ajax()).thenReturn(ajax);
        return primeFaces;
    }
}
