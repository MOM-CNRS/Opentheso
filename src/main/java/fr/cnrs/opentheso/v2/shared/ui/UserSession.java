package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * Point d'accès unique à la session utilisateur pour la v2.
 * Pont temporaire vers {@link CurrentUser} legacy — à remplacer par une session v2 autonome.
 */
@SessionScoped
@Named("v2UserSession")
@RequiredArgsConstructor
public class UserSession implements Serializable {

    private final CurrentUser currentUser;

    public boolean isLoggedIn() {
        return currentUser.getNodeUser() != null;
    }

    public Integer getCurrentUserId() {
        if (currentUser.getNodeUser() == null) {
            return null;
        }
        return currentUser.getNodeUser().getIdUser();
    }

    public String getCurrentUsername() {
        if (currentUser.getNodeUser() == null) {
            return null;
        }
        return currentUser.getNodeUser().getName();
    }

    public void refreshDisplayName(String name) {
        if (currentUser.getNodeUser() != null) {
            currentUser.getNodeUser().setName(name);
        }
    }

    public void refreshEmail(String email) {
        if (currentUser.getNodeUser() != null) {
            currentUser.getNodeUser().setMail(email);
        }
    }

    public void refreshAlertMail(boolean alertMail) {
        if (currentUser.getNodeUser() != null) {
            currentUser.getNodeUser().setAlertMail(alertMail);
        }
    }

    public boolean isSuperAdmin() {
        return currentUser.getNodeUser() != null && currentUser.getNodeUser().isSuperAdmin();
    }

    public boolean canAccessProjectAdminScreen() {
        if (!isLoggedIn()) {
            return false;
        }
        return isSuperAdmin() || currentUser.isHasRoleAsAdmin();
    }

    public boolean canAccessSuperAdminScreen() {
        return isSuperAdmin();
    }
}
