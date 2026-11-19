package fr.cnrs.opentheso.suggestion.ui;

import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.suggestion.service.FeatureSuggestionMailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeatureSuggestionBeanTest {

    private UserSession userSession;
    private LocaleBean localeBean;
    private InstanceFeatureBean features;
    private FeatureSuggestionMailer mailer;
    private FeatureSuggestionBean bean;

    @BeforeEach
    void setUp() {
        userSession = mock(UserSession.class);
        localeBean = mock(LocaleBean.class);
        features = mock(InstanceFeatureBean.class);
        mailer = mock(FeatureSuggestionMailer.class);
        when(localeBean.getMsg(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(features.isSuggestionEnabled()).thenReturn(true);
        when(mailer.isConfigured()).thenReturn(true);
        when(mailer.dispatch(anyString(), anyString(), anyString())).thenReturn(true);
        bean = new FeatureSuggestionBean(userSession, localeBean, features, mailer);
    }

    @Test
    void disabledFlagRefusesSend() {
        when(features.isSuggestionEnabled()).thenReturn(false);
        fillValid();

        bean.submit();

        assertFalse(bean.isSent());
        assertEquals("suggest.error.disabled", bean.getError());
        verify(mailer, never()).dispatch(anyString(), anyString(), anyString());
    }

    @Test
    void blankDestinationRefusesSend() {
        when(mailer.isConfigured()).thenReturn(false);
        fillValid();

        bean.submit();

        assertFalse(bean.isSent());
        assertEquals("suggest.error.unconfigured", bean.getError());
        verify(mailer, never()).dispatch(anyString(), anyString(), anyString());
    }

    @Test
    void validSuggestionIsQueuedAndEscaped() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUsername()).thenReturn("ada");
        fillValid();
        bean.setMessage("Une note <script> avec un besoin précis.");
        bean.setEmail("Ada@Example.fr");

        bean.submit();

        assertTrue(bean.isSent());
        assertNull(bean.getError());
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> reply = ArgumentCaptor.forClass(String.class);
        verify(mailer).dispatch(subject.capture(), html.capture(), reply.capture());
        assertTrue(subject.getValue().contains("suggest.kind.improvement"));
        assertTrue(subject.getValue().contains("Export plus clair"));
        assertTrue(html.getValue().contains("&lt;script&gt;"));
        assertFalse(html.getValue().contains("<script>"));
        assertEquals("ada@example.fr", reply.getValue());
        assertTrue(html.getValue().contains("ada"));
    }

    @Test
    void honeypotPretendsSuccessWithoutSending() {
        bean.setWebsite("http://spam.test");

        bean.submit();

        assertTrue(bean.isSent());
        verifyNoInteractions(mailer);
    }

    @Test
    void shortMessageIsRejected() {
        fillValid();
        bean.setMessage("trop court");

        bean.submit();

        assertFalse(bean.isSent());
        assertEquals("suggest.error.message", bean.getError());
        verify(mailer, never()).dispatch(anyString(), anyString(), anyString());
    }

    @Test
    void invalidEmailIsRejected() {
        fillValid();
        bean.setEmail("pas-un-mail");

        bean.submit();

        assertEquals("suggest.error.email", bean.getError());
        verify(mailer, never()).dispatch(anyString(), anyString(), anyString());
    }

    @Test
    void preparePrefillsTheSignedInUser() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUsername()).thenReturn("ada");
        when(userSession.getCurrentUserEmail()).thenReturn("ada@example.fr");
        bean.setSubject("déjà saisi");
        bean.setSent(true);

        bean.prepare();

        assertEquals("ada", bean.getName());
        assertEquals("ada@example.fr", bean.getEmail());
        assertEquals("", bean.getSubject());
        assertFalse(bean.isSent());
        assertEquals(FeatureSuggestionBean.KIND_FEATURE, bean.getKind());
    }

    private void fillValid() {
        bean.setKind(FeatureSuggestionBean.KIND_IMPROVEMENT);
        bean.setSubject("Export plus clair");
        bean.setMessage("Je voudrais un export plus clair pour les notes longues.");
        bean.setName("Ada");
        bean.setEmail("ada@example.fr");
    }
}
