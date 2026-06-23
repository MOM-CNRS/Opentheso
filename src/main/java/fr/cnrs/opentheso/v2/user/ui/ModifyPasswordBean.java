package fr.cnrs.opentheso.v2.user.ui;

import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.service.UserPasswordService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

@Slf4j
@Getter
@Setter
@ViewScoped
@Named("v2ModifyPasswordBean")
public class ModifyPasswordBean implements Serializable {

    private final UserSession userSession;
    private final LanguageBean languageBean;
    private final UserPasswordService userPasswordService;

    private String password;
    private String confirmation;

    public ModifyPasswordBean(
            UserSession userSession,
            LanguageBean languageBean,
            UserPasswordService userPasswordService
    ) {
        this.userSession = userSession;
        this.languageBean = languageBean;
        this.userPasswordService = userPasswordService;
    }

    public void prepareDialog() {
        password = null;
        confirmation = null;
    }

    public void apply() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(languageBean.getMsg("profile.userNotConnected"));
            return;
        }
        try {
            userPasswordService.changePassword(userId, password, confirmation);
            MessageUtils.showInformationMessage(languageBean.getMsg("profile.passwordChangedSuccess"));
            prepareDialog();
            PrimeFaces.current().ajax().update("containerIndex");
            PrimeFaces.current().executeScript("PF('v2ModifyPassword').hide();");
        } catch (InvalidPasswordException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erreur inattendue lors du changement de mot de passe pour l'utilisateur id={}", userId, e);
            MessageUtils.showErrorMessage(languageBean.getMsg("profile.unexpectedError"));
        }
    }
}
