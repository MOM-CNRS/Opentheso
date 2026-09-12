package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.config.SessionConfig;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
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
class V2NavigationBeanTest {

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

    private V2NavigationBean navigationBean;

    @BeforeEach
    void setUp() {
        navigationBean = new V2NavigationBean(
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
        when(sessionLifecycleService.expireUrl("/opentheso")).thenReturn("/opentheso/v2/session/expire");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            assertEquals("/opentheso/v2/session/expire", navigationBean.getSessionExpireUrl());
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

        verify(externalContext).redirect("/ot/v2/admin/instance");
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

        verify(externalContext).redirect("/ot/v2/thesauri");
        verify(externalContext, never()).redirect("/ot/v2/admin/instance");
    }
}
