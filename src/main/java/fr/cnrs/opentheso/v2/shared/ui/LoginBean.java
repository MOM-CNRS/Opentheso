package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.config.AppConfig;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.shared.auth.AuthenticationService;
import fr.cnrs.opentheso.v2.shared.session.SessionAuthenticatedUserSource;
import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;

@Slf4j
@Getter
@Setter
@SessionScoped
@Named("v2LoginBean")
@RequiredArgsConstructor
public class LoginBean implements Serializable {

    private final AuthenticationService authenticationService;
    private final SessionAuthenticatedUserSource sessionAuthenticatedUserSource;
    private final UserSession userSession;
    private final V2LocaleBean v2LocaleBean;
    private final ConsultationShellBean consultationShellBean;
    private final AppConfig appConfig;
    private final SessionLifecycleService sessionLifecycleService;

    private String username;
    private String password;
    private String loginError;
    private boolean usernameInvalid;
    private boolean passwordInvalid;

    public boolean isKeycloakEnabled() {
        return appConfig.isKeycloakEnabled();
    }

    public boolean hasLoginError() {
        return StringUtils.isNotBlank(loginError);
    }

    public void login() {
        clearLoginError();
        boolean missingUser = StringUtils.isBlank(username);
        boolean missingPass = StringUtils.isBlank(password);
        if (missingUser && missingPass) {
            failLogin("connect.error.required", true, true);
            return;
        }
        if (missingUser) {
            failLogin("connect.error.username", true, false);
            return;
        }
        if (missingPass) {
            failLogin("connect.error.password", false, true);
            return;
        }
        try {
            var user = authenticationService.authenticate(username.trim(), password).orElse(null);
            if (user == null) {
                password = null;
                failLogin("connect.error.credentials", true, true);
                return;
            }
            sessionAuthenticatedUserSource.setUserId(user.id());
            password = null;
            try {
                consultationShellBean.load();
            } catch (RuntimeException ex) {
                log.warn("Session chargée après connexion, mais le shell n'a pas pu se rafraîchir", ex);
            }
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    v2LocaleBean.getMsg("connect.welcome"),
                    user.username()
            ));
            reloadCurrentView();
        } catch (RuntimeException ex) {
            log.error("Échec de l'authentification", ex);
            failLogin("connect.error.unavailable", false, false);
        }
    }

    public void logout() throws IOException {
        username = null;
        password = null;
        clearLoginError();
        sessionAuthenticatedUserSource.setUserId(null);
        sessionLifecycleService.logoutAndRedirectFromFaces();
    }

    /** Déconnexion depuis le clone v2-preview : reste sur /v2-preview. */
    public void logoutToPreview() throws IOException {
        username = null;
        password = null;
        clearLoginError();
        sessionAuthenticatedUserSource.setUserId(null);
        var facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        var externalContext = facesContext.getExternalContext();
        String ctx = externalContext.getRequestContextPath();
        if (ctx == null || ctx.isBlank() || "/".equals(ctx)) {
            ctx = "";
        }
        sessionLifecycleService.invalidateCurrentFacesSessionQuietly();
        externalContext.redirect(ctx + "/v2-preview");
        facesContext.responseComplete();
    }

    private void reloadCurrentView() {
        var facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        var externalContext = facesContext.getExternalContext();
        if (externalContext == null) {
            return;
        }
        String ctx = externalContext.getRequestContextPath();
        if (ctx == null || ctx.isBlank() || "/".equals(ctx)) {
            ctx = "";
        }
        String path = externalContext.getRequestServletPath();
        if (path == null || path.isBlank()) {
            path = "/v2-preview";
        }
        String query = null;
        if (externalContext.getRequest() instanceof HttpServletRequest httpRequest) {
            query = httpRequest.getQueryString();
        }
        String target = ctx + path + (query == null || query.isBlank() ? "" : "?" + query);
        try {
            externalContext.redirect(target);
            facesContext.responseComplete();
        } catch (IOException ex) {
            log.warn("Connexion OK, mais la page n'a pas pu être rechargée", ex);
        }
    }

    private void failLogin(String messageKey, boolean invalidUser, boolean invalidPass) {
        usernameInvalid = invalidUser;
        passwordInvalid = invalidPass;
        loginError = v2LocaleBean.getMsg(messageKey);
        MessageUtils.showErrorMessage(loginError);
    }

    private void clearLoginError() {
        loginError = null;
        usernameInvalid = false;
        passwordInvalid = false;
    }
}
