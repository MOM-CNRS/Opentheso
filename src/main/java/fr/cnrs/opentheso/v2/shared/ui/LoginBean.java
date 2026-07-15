package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.config.AppConfig;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.shared.auth.AuthenticationService;
import fr.cnrs.opentheso.v2.shared.session.SessionAuthenticatedUserSource;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;

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

    private String username;
    private String password;

    public boolean isKeycloakEnabled() {
        return appConfig.isKeycloakEnabled();
    }

    public void login() {
        if (StringUtils.isAnyBlank(username, password)) {
            MessageUtils.showErrorMessage(v2LocaleBean.getMsg("candidat.save.msg9"));
            return;
        }
        var user = authenticationService.authenticate(username, password).orElse(null);
        if (user == null) {
            MessageUtils.showErrorMessage("User or password wrong, please try again");
            return;
        }
        sessionAuthenticatedUserSource.setUserId(user.id());
        password = null;
        consultationShellBean.load();
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                v2LocaleBean.getMsg("connect.welcome"),
                user.username()
        ));
    }

    public void logout() throws IOException {
        String displayName = StringUtils.defaultIfBlank(userSession.getCurrentUsername(), username);
        sessionAuthenticatedUserSource.setUserId(null);
        username = null;
        password = null;
        consultationShellBean.afterLogout();
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                v2LocaleBean.getMsg("connect.goodbye"),
                displayName
        ));
        FacesContext.getCurrentInstance().getExternalContext().redirect(
                FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/v2/thesaurus"
        );
    }
}
