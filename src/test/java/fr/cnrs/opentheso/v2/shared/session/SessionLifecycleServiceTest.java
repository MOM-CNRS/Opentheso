package fr.cnrs.opentheso.v2.shared.session;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionLifecycleServiceTest {

    private final SessionLifecycleService service = new SessionLifecycleService();

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private FacesContext facesContext;
    @Mock
    private ExternalContext externalContext;

    @Test
    void homeUrl_handlesRootAndNonRootContext() {
        assertEquals("/v2/thesaurus", service.homeUrl(null));
        assertEquals("/v2/thesaurus", service.homeUrl("/"));
        assertEquals("/opentheso/v2/thesaurus", service.homeUrl("/opentheso"));
        assertEquals("/opentheso/v2/thesaurus", service.homeUrl("/opentheso/"));
        assertEquals("/opentheso/v2/thesaurus?sessionExpired=1", service.homeUrlWithSessionExpired("/opentheso"));
        assertEquals("/opentheso/v2/thesaurus?logout=1", service.homeUrlWithLogout("/opentheso"));
        assertEquals("/opentheso/v2/session/expire", service.expireUrl("/opentheso"));
        assertEquals("/v2/session/expire", service.expireUrl(null));
    }

    @Test
    void expireAndRedirect_invalidatesSessionThenRedirects() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(request.getContextPath()).thenReturn("/opentheso");

        service.expireAndRedirect(request, response);

        verify(session).invalidate();
        verify(response).sendRedirect("/opentheso/v2/thesaurus?sessionExpired=1");
    }

    @Test
    void expireAndRedirect_worksWhenNoExistingSession() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("");

        service.expireAndRedirect(request, response);

        verify(response).sendRedirect("/v2/thesaurus?sessionExpired=1");
    }

    @Test
    void invalidateQuietly_ignoresNullAndAlreadyInvalidSession() {
        service.invalidateQuietly(null);
        doThrow(new IllegalStateException("invalid")).when(session).invalidate();
        service.invalidateQuietly(session);
        verify(session).invalidate();
    }

    @Test
    void logoutAndRedirectFromFaces_invalidatesAndRedirectsWithLogoutFlag() throws Exception {
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/opentheso");
        when(externalContext.getSession(false)).thenReturn(session);

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            service.logoutAndRedirectFromFaces();
        }

        verify(session).invalidate();
        verify(externalContext).redirect("/opentheso/v2/thesaurus?logout=1");
        verify(facesContext).responseComplete();
    }

    @Test
    void expireAndRedirectFromFaces_invalidatesAndRedirectsWithExpiredFlag() throws Exception {
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/");
        when(externalContext.getSession(false)).thenReturn(session);

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            service.expireAndRedirectFromFaces();
        }

        verify(session).invalidate();
        verify(externalContext).redirect("/v2/thesaurus?sessionExpired=1");
        verify(facesContext).responseComplete();
    }

    @Test
    void clearAndRedirectFromFaces_invalidatesAndRedirectsHome() throws Exception {
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/opentheso");
        when(externalContext.getSession(false)).thenReturn(session);

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            service.clearAndRedirectFromFaces();
        }

        verify(session).invalidate();
        verify(externalContext).redirect("/opentheso/v2/thesaurus");
        verify(facesContext).responseComplete();
    }

    @Test
    void facesRedirectMethods_noopWhenFacesContextMissing() throws Exception {
        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(null);

            service.logoutAndRedirectFromFaces();
            service.expireAndRedirectFromFaces();
            service.clearAndRedirectFromFaces();
            service.invalidateCurrentFacesSessionQuietly();
        }

        verify(externalContext, never()).redirect(org.mockito.ArgumentMatchers.anyString());
    }
}
