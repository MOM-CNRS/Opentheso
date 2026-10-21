package fr.cnrs.opentheso.user.ui;

import fr.cnrs.opentheso.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.user.exception.PasswordResetException;
import fr.cnrs.opentheso.user.service.AccountPasswordResetService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@ViewScoped
@Named("resetPasswordBean")
public class ResetPasswordBean implements Serializable {

    private final transient AccountPasswordResetService accountPasswordResetService;

    private String token;
    private String newPassword;
    private String confirmPassword;
    private boolean tokenValid;
    private boolean done;
    private String error;

    public ResetPasswordBean(AccountPasswordResetService accountPasswordResetService) {
        this.accountPasswordResetService = accountPasswordResetService;
    }

    public void init() {
        error = null;
        done = false;
        tokenValid = accountPasswordResetService.isTokenValid(token);
    }

    public void resetPassword() {
        error = null;
        try {
            accountPasswordResetService.resetPassword(token, newPassword, confirmPassword);
            done = true;
            tokenValid = false;
            newPassword = null;
            confirmPassword = null;
        } catch (InvalidPasswordException e) {
            error = e.getMessage();
        } catch (PasswordResetException e) {
            tokenValid = false;
            error = e.getMessage();
        }
    }
}
