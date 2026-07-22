package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.config.AppConfig;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.shared.auth.AuthenticatedUser;
import fr.cnrs.opentheso.v2.shared.auth.AuthenticationService;
import fr.cnrs.opentheso.v2.shared.session.SessionAuthenticatedUserSource;
import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginBeanTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private SessionAuthenticatedUserSource sessionAuthenticatedUserSource;
    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private ConsultationShellBean consultationShellBean;
    @Mock
    private AppConfig appConfig;
    @Mock
    private SessionLifecycleService sessionLifecycleService;
    @Mock
    private FacesContext facesContext;

    private LoginBean loginBean;

    @BeforeEach
    void setUp() {
        loginBean = new LoginBean(
                authenticationService,
                sessionAuthenticatedUserSource,
                userSession,
                v2LocaleBean,
                consultationShellBean,
                appConfig,
                sessionLifecycleService
        );
    }

    @Test
    void logout_clearsCredentialsAndDelegatesToLifecycle() throws Exception {
        loginBean.setUsername("alice");
        loginBean.setPassword("secret");

        loginBean.logout();

        assertNull(loginBean.getUsername());
        assertNull(loginBean.getPassword());
        verify(sessionAuthenticatedUserSource).setUserId(null);
        verify(sessionLifecycleService).logoutAndRedirectFromFaces();
    }

    @Test
    void login_setsUserIdOnSuccess() {
        loginBean.setUsername("alice");
        loginBean.setPassword("secret");
        when(authenticationService.authenticate("alice", "secret"))
                .thenReturn(Optional.of(new AuthenticatedUser(7, "alice")));
        when(v2LocaleBean.getMsg("connect.welcome")).thenReturn("Bienvenue");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            loginBean.login();
        }

        verify(sessionAuthenticatedUserSource).setUserId(7);
        verify(consultationShellBean).load();
        assertNull(loginBean.getPassword());
    }

    @Test
    void login_showsErrorWhenCredentialsBlank() {
        loginBean.setUsername(" ");
        loginBean.setPassword(null);
        when(v2LocaleBean.getMsg("candidat.save.msg9")).thenReturn("required");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            loginBean.login();
            messages.verify(() -> MessageUtils.showErrorMessage("required"));
        }

        verify(authenticationService, never()).authenticate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void isKeycloakEnabled_delegatesToAppConfig() {
        when(appConfig.isKeycloakEnabled()).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertTrue(loginBean.isKeycloakEnabled());
    }
}
