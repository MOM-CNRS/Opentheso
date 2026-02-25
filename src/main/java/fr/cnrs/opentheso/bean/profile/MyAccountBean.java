package fr.cnrs.opentheso.bean.profile;

import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.models.users.NodeUserRoleGroup;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean pour la gestion des actions de compte utilisateur telles que la mise à jour des informations de profil et des clés API.
 * Cette classe est à portée de session et gère les paramètres du compte utilisateur.
 *
 * @author miledrousset
 */
@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "myAccountBean")
public class MyAccountBean implements Serializable {

    private final CurrentUser currentUser;
    private final UserService userService;
    private final ApiKeyService apiKeyService;
    private final PasswordEncoder passwordEncoder;

    private NodeUser nodeUser;
    private String passWord1, passWord2, displayedKey;
    private List<NodeUserRoleGroup> allMyRoleProject;


    public void loadDataPage(){

        log.debug("Chargement des données nécessaire au fonctionnement de l'écran utilisateur");
        nodeUser = userService.getUser(currentUser.getNodeUser().getIdUser());
        displayedKey = StringUtils.isEmpty(nodeUser.getApiKey()) ? null : new String(new char[64]).replace("\0", "*");
        passWord1 = null;
        passWord2 = null;
    }

    public void updateKey() {

        displayedKey = apiKeyService.generateApiKey("ot_", 64);
        nodeUser.setApiKey(displayedKey);

        if (apiKeyService.saveApiKey(MD5Password.getEncodedPassword(displayedKey), nodeUser.getIdUser())) {
            MessageUtils.showInformationMessage("La clé a bien été enregistrée.");
        } else {
            MessageUtils.showErrorMessage("Erreur de sauvegarde de la clé.");
        }

    }

    public void updateUserName() {

        if (StringUtils.isEmpty(nodeUser.getName())) {
            MessageUtils.showErrorMessage("Le pseudo est obligatoire !!!");
            return;
        }

        if (userService.updateUserInformation(currentUser.getNodeUser().getIdUser(), nodeUser.getName(), null, null, null)) {
            MessageUtils.showInformationMessage("Pseudo changé avec succès");
            PrimeFaces.current().ajax().update("containerIndex");
        } else {
            MessageUtils.showErrorMessage("Erreur pendant la modification du pseudo");
        }
    }

    public void updateAlertEmail() {

        if (userService.updateUserInformation(currentUser.getNodeUser().getIdUser(), null, null, null, nodeUser.isAlertMail())) {
            MessageUtils.showInformationMessage("Alerte changée avec succès");
            PrimeFaces.current().ajax().update("containerIndex");
        } else {
            MessageUtils.showErrorMessage("Erreur pendant la modification de l'alerte email");
        }
    }

    public void updateEmail() {

        if (StringUtils.isEmpty(nodeUser.getMail())) {
            MessageUtils.showErrorMessage("Un Email est obligatoire !!!");
            return;
        }

        if (userService.updateUserInformation(currentUser.getNodeUser().getIdUser(), null, null, nodeUser.getMail(), null)) {
            MessageUtils.showInformationMessage("Email changé avec succès");
            PrimeFaces.current().ajax().update("containerIndex");
        } else {
            MessageUtils.showErrorMessage("Erreur pendant la modification du mot de passe");
        }
    }

    public void updatePassword() {

        if (StringUtils.isEmpty(passWord1) || StringUtils.isEmpty(passWord2)) {
            MessageUtils.showErrorMessage("Un mot de passe est obligatoire !!!");
            return;
        }

        if (!passWord1.equals(passWord2)) {
            MessageUtils.showErrorMessage("Mot de passe non identique !!!");
            return;
        }

        // Encode le mot de passe avec BCrypt
        String encodedPassword = passwordEncoder.encode(passWord2);

        boolean result = userService.updateUserInformation(
                currentUser.getNodeUser().getIdUser(),
                null,
                encodedPassword,
                null,
                null
        );

        if (result) {
            MessageUtils.showInformationMessage("Mot de passe changé avec succès");
            PrimeFaces.current().ajax().update("containerIndex");
        } else {
            MessageUtils.showErrorMessage("Erreur pendant la modification du mot de passe");
        }
    }

    public boolean isKeyExpired() {
        if (ObjectUtils.isEmpty(nodeUser.getApiKeyExpireDate())) return false;
        return LocalDate.now().isAfter(nodeUser.getApiKeyExpireDate());
    }
}

