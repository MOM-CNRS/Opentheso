package fr.cnrs.opentheso.bean.menu.users;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.bean.profile.MyProjectBean;
import fr.cnrs.opentheso.bean.profile.SuperAdminBean;
import fr.cnrs.opentheso.utils.MessageUtils;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.security.crypto.password.PasswordEncoder;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "modifyUserBean")
public class ModifyUserBean implements Serializable {

    private final MyProjectBean myProjectBean;
    private final SuperAdminBean superAdminBean;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    private User nodeUser;
    private String passWord1, passWord2;
    private boolean hasKey;
    private LocalDate apiKeyExpireDate;
    
    /**
     * Permet de selectionner l'utilisateur dans la liste avec toutes les informations nécessaires pour sa modification
     */
    public void selectUser(int idUser) {
        nodeUser = userService.getUserById(idUser);
        setUserStringId(""+idUser);
        passWord1 = null;
        passWord2 = null;
    }

    public void setUserStringId(String idUser){
        try {
            nodeUser = userService.getUserById(Integer.parseInt(idUser));
            hasKey = nodeUser.getKeyExpiresAt() != null;
            apiKeyExpireDate = nodeUser.getKeyExpiresAt();
        } catch (NumberFormatException e) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné");
        }
    }

    public void deleteUser() {

        if(nodeUser.getId() == -1) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné");
            return;
        }

        userService.deleteUserById(nodeUser.getId());
        MessageUtils.showInformationMessage("L'utilisateur a bien été supprimé");
        superAdminBean.init();
    }

    public void updateUser(){
        
        if(ObjectUtils.isEmpty(nodeUser)) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné");
            return;              
        }

        userService.saveUser(nodeUser);
        MessageUtils.showInformationMessage("Utilisateur changé avec succès");

        selectUser(nodeUser.getId());
        myProjectBean.setLists();

        PrimeFaces.current().ajax().update("containerIndex");
    }      

    public void updateUser2(){
        
        if(ObjectUtils.isEmpty(nodeUser)) {
            MessageUtils.showErrorMessage("Aucun utilisateur sélectionné");
            return;              
        }

        nodeUser.setUsername(nodeUser.getUsername().trim());

        userService.saveUser(nodeUser);
        MessageUtils.showInformationMessage("Utilisateur changé avec succès");
        superAdminBean.init();
    }

    public boolean updatePassword() {

        if (StringUtils.isEmpty(passWord1)) {
            MessageUtils.showErrorMessage("Un mot de passe est obligatoire");
            return false;
        }

        if (StringUtils.isEmpty(passWord2)) {
            MessageUtils.showErrorMessage("Un mot de passe est obligatoire");
            return false;
        }

        if (!passWord1.equals(passWord2)) {
            MessageUtils.showErrorMessage("Mot de passe non identique");
            return false;
        }

        if (!passWord1.matches(".*[A-Z].*") ||
                !passWord1.matches(".*[a-z].*") ||
                !passWord1.matches(".*\\d.*") ||
                !passWord1.matches(".*[^A-Za-z\\d].*")) {
            MessageUtils.showErrorMessage("Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial");
            return false;
        }

        // On encode le mot de passe avec BCrypt
        String encodedPassword = passwordEncoder.encode(passWord2);
        nodeUser.setPassword(encodedPassword);

        // Sauvegarde
        userService.saveUser(nodeUser);

        MessageUtils.showInformationMessage("Mot de passe changé avec succès");

        // Rafraîchir la sélection ou l'affichage si nécessaire
        selectUser(nodeUser.getId());
        return true;
    }

    public void updateApiKey() {

        var keyNeverExpireValue = nodeUser.getKeyNeverExpire();
        LocalDate apiKeyExpireDateValue = null;

        if (hasKey) {
            if (keyNeverExpireValue) {
                keyNeverExpireValue = true;
                apiKeyExpireDate = null;
            } else {
                keyNeverExpireValue = false;
                apiKeyExpireDateValue = apiKeyExpireDate;
            }
        } else {
            keyNeverExpireValue = false;
            apiKeyExpireDate = null;
        }

        nodeUser.setKeyNeverExpire(keyNeverExpireValue);
        nodeUser.setKeyExpiresAt(apiKeyExpireDateValue);
        userService.saveUser(nodeUser);
        MessageUtils.showInformationMessage("Clé mise à jour avec succès");

    }
}
