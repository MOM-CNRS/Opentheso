package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.user.service.AccountPasswordResetService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@RequestScoped
@Named("v2ForgotPasswordBean")
@RequiredArgsConstructor
public class ForgotPasswordBean implements Serializable {

    private final AccountPasswordResetService accountPasswordResetService;

    private String sendTo;
    private String message;

    public void sendMail() {
        if (StringUtils.isBlank(sendTo)) {
            MessageUtils.showErrorMessage("Veuillez saisir une adresse mail");
            return;
        }
        accountPasswordResetService.requestPasswordReset(sendTo.trim(), false);
        message = "Si un compte existe pour cette adresse, un email de réinitialisation a été envoyé.";
        MessageUtils.showWarnMessage(message);
    }
}
