package fr.cnrs.opentheso.bean.forgetpassword;

import fr.cnrs.opentheso.services.PasswordResetService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.io.Serializable;

@Getter
@Setter
@Named
@ViewScoped
public class ResetPasswordBean implements Serializable {

    @Autowired
    private PasswordResetService passwordResetService;

    private String token;
    private String newPassword;
    private String confirmPassword;

    private boolean tokenValid = false; // pour savoir si le formulaire peut s'afficher
    private boolean redirectAfterReset = false;

    public void redirectToHome() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() + "/index.xhtml"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialisation après récupération du token depuis l'URL
     */
    public void init() {
        try {
            // Vérifie si le token est valide
            passwordResetService.validateToken(token);
            tokenValid = true;
        } catch (IllegalArgumentException e) {
            // Token invalide ou expiré
            tokenValid = false;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Lien de réinitialisation invalide ou expiré", null));
        }
    }

    /**
     * Méthode appelée au clic sur "Valider"
     */
    public void resetPassword() {
        if (!tokenValid) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Impossible de réinitialiser : lien invalide", null));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Les mots de passe ne correspondent pas", null));
            return;
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Mot de passe réinitialisé avec succès !", null));

            redirectAfterReset = true;

        } catch (IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Lien invalide ou déjà utilisé", null));
        }
    }
}
