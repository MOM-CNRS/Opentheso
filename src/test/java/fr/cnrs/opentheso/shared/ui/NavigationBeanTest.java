package fr.cnrs.opentheso.shared.ui;

import fr.cnrs.opentheso.config.SessionConfig;
import fr.cnrs.opentheso.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.session.SessionLifecycleService;
import fr.cnrs.opentheso.shared.web.ApplicationUriService;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NavigationBeanTest {

    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private SessionConfig sessionConfig;
    @Mock
    private SessionLifecycleService sessionLifecycleService;
    @Mock
    private ApplicationUriService applicationUriService;
    @Mock
    private UserSession userSession;
    @Mock
    private ConsultationShellBean consultationShellBean;
    @Mock
    private FacesContext facesContext;
    @Mock
    private ExternalContext externalContext;

    private NavigationBean navigationBean;

    @BeforeEach
    void setUp() {
        navigationBean = new NavigationBean(
                thesaurusContext,
                sessionConfig,
                sessionLifecycleService,
                applicationUriService,
                userSession,
                consultationShellBean
        );
    }

    @Test
    void getSessionTimeoutInMilliseconds_delegatesToSessionConfig() {
        when(sessionConfig.getSessionTimeoutInMilliseconds()).thenReturn(60_000);

        assertEquals(60_000, navigationBean.getSessionTimeoutInMilliseconds());
    }

    @Test
    void getSessionExpireUrl_usesLifecycleServiceAndContextPath() {
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/opentheso");
        when(sessionLifecycleService.expireUrl("/opentheso")).thenReturn("/opentheso/session/expire");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            assertEquals("/opentheso/session/expire", navigationBean.getSessionExpireUrl());
        }
    }

    @Test
    void getSessionKeepAliveUrl_usesLifecycleServiceAndContextPath() {
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/opentheso");
        when(sessionLifecycleService.keepAliveUrl("/opentheso")).thenReturn("/opentheso/session/keep-alive");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            assertEquals("/opentheso/session/keep-alive", navigationBean.getSessionKeepAliveUrl());
        }
    }

    @Test
    void swaggerAndOpenApiUrls_delegateToApplicationUriService() {
        when(applicationUriService.resolveSwaggerUrl()).thenReturn("http://localhost/swagger-ui/index.html");
        when(applicationUriService.resolveOpenApiUrl()).thenReturn("http://localhost/openapi/v1");

        assertEquals("http://localhost/swagger-ui/index.html", navigationBean.getSwaggerUrl());
        assertEquals("http://localhost/openapi/v1", navigationBean.getOpenApiUrl());
    }

    @Test
    void graphQlAndGraphiqlUrls_delegateToApplicationUriService() {
        when(applicationUriService.resolveGraphQlUrl()).thenReturn("http://localhost/graphql");
        when(applicationUriService.resolveGraphiqlUrl()).thenReturn("http://localhost/graphiql.html");

        assertEquals("http://localhost/graphql", navigationBean.getGraphQlUrl());
        assertEquals("http://localhost/graphiql.html", navigationBean.getGraphiqlUrl());
    }

    @Test
    void redirectToInstanceAdmin_allowsSuperAdmin() throws Exception {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(true);
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/ot");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            navigationBean.redirectToInstanceAdmin();
        }

        verify(externalContext).redirect("/ot/admin/instance");
        assertEquals("instanceAdminV2", navigationBean.getActivePageName());
    }

    @Test
    void redirectToInstanceAdmin_deniesNonSuperAdmin() throws Exception {
        when(userSession.canAccessSuperAdminScreen()).thenReturn(false);
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/ot");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            navigationBean.redirectToInstanceAdmin();
        }

        verify(externalContext).redirect("/ot/thesauri");
        verify(externalContext, never()).redirect("/ot/admin/instance");
    }

    @Test
    void redirectToPreference_deniesGuest() throws Exception {
        when(userSession.isLoggedIn()).thenReturn(false);
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestContextPath()).thenReturn("/ot");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            navigationBean.redirectToPreference();
        }

        verify(externalContext).redirect("/ot/thesauri");
        verify(externalContext, never()).redirect("/ot/setting/preference.xhtml");
    }
}
