package fr.cnrs.opentheso.suggestion.ui;

import fr.cnrs.opentheso.admin.ui.InstanceAdminBean;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.suggestion.model.InstanceFeatures;
import fr.cnrs.opentheso.suggestion.service.FeatureSuggestionMailer;
import fr.cnrs.opentheso.suggestion.service.InstanceFeatureService;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstanceSettingsBeanTest {

    private UserSession userSession;
    private LocaleBean localeBean;
    private InstanceFeatureService service;
    private FeatureSuggestionMailer mailer;
    private InstanceSettingsBean bean;

    @BeforeEach
    void setUp() {
        userSession = mock(UserSession.class);
        localeBean = mock(LocaleBean.class);
        service = mock(InstanceFeatureService.class);
        mailer = mock(FeatureSuggestionMailer.class);
        when(localeBean.getMsg(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(service.current()).thenReturn(new InstanceFeatures(true, false, true, false));
        bean = new InstanceSettingsBean(userSession, localeBean, service, mailer);
    }

    @Test
    void saveIsRefusedWithoutSuperAdmin() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(false);
        bean.setSuggestionEnabled(false);

        bean.save();

        assertTrue(bean.isMessageError());
        assertEquals("admin.prefs.denied", bean.getMessage());
        verify(service, never()).save(any());
    }

    @Test
    void saveWritesTheFourFlags() {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        bean.setSuggestionEnabled(false);
        bean.setLanguageVisible(true);
        bean.setRestApiVisible(false);
        bean.setGraphApiVisible(true);

        bean.save();

        assertFalse(bean.isMessageError());
        assertEquals("admin.prefs.saved", bean.getMessage());
        verify(service).save(new InstanceFeatures(false, true, false, true));
    }

    @Test
    void saveReturnsHomeWithAToastMessage() throws Exception {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        FacesContext facesContext = mock(FacesContext.class);
        ExternalContext externalContext = mock(ExternalContext.class);
        Map<String, Object> session = new HashMap<>();
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getSessionMap()).thenReturn(session);
        when(externalContext.getRequestContextPath()).thenReturn("");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            bean.setSuggestionEnabled(false);
            bean.setLanguageVisible(true);
            bean.setRestApiVisible(true);
            bean.setGraphApiVisible(true);
            bean.save();
        }

        assertEquals("admin.prefs.saved", session.get(InstanceAdminBean.SESSION_PREFS_FLASH));
        verify(externalContext).redirect("/admin/instance");
        verify(facesContext).responseComplete();
    }

    @Test
    void gettersLoadTheStoredFlagsOnce() {
        assertTrue(bean.isSuggestionEnabled());
        assertFalse(bean.isLanguageVisible());
        assertTrue(bean.isRestApiVisible());
        assertFalse(bean.isGraphApiVisible());
        assertFalse(bean.isLanguageVisible());
        verify(service).current();
    }
}
