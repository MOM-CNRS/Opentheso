package fr.cnrs.opentheso.bean.forgetpassword;

import fr.cnrs.opentheso.services.PasswordResetService;
import fr.cnrs.opentheso.services.MailService;
import fr.cnrs.opentheso.utils.MessageUtils;
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
@RequiredArgsConstructor
@Named(value = "forgotPasswordBean")
public class ForgotPasswordBean implements Serializable {

    private final PasswordResetService passwordResetService;
    private final MailService mailBean;

    private String sendTo;   // email saisi par l'utilisateur
    private String message;  // message à afficher sur la page JSF

    public void sendMail() {

        if (StringUtils.isEmpty(sendTo)) {
            MessageUtils.showErrorMessage("Veuillez saisir une adresse mail");
            return;
        }

        // Génère et envoie le token si l'utilisateur existe
        passwordResetService.requestPasswordReset(sendTo);

        // Message générique pour éviter l’énumération des emails
        message = "Si un compte existe pour cette adresse, un email de réinitialisation a été envoyé.";
        MessageUtils.showWarnMessage(message);
    }
}
