package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.shared.session.SessionUserService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * Point d'accès unique à la session utilisateur pour la v2.
 * Les capacités sont résolues via {@link SessionUserService} ; {@link CurrentUser}
 * reste synchronisé pour la compatibilité avec le login legacy.
 */
@SessionScoped
@Named("v2UserSession")
@RequiredArgsConstructor
public class UserSession implements Serializable {

    private final CurrentUser currentUser;
    private final SessionUserService sessionUserService;

    private SessionUser cachedSessionUser;

    public boolean isLoggedIn() {
        return currentUser.getNodeUser() != null;
    }

    public Integer getCurrentUserId() {
        return resolveSessionUser().map(SessionUser::userId).orElse(null);
    }

    public String getCurrentUsername() {
        return resolveSessionUser().map(SessionUser::username).orElse(null);
    }

    public void refreshDisplayName(String name) {
        if (currentUser.getNodeUser() != null) {
            currentUser.getNodeUser().setName(name);
            invalidateCache();
        }
    }

    public void refreshEmail(String email) {
        if (currentUser.getNodeUser() != null) {
            currentUser.getNodeUser().setMail(email);
            invalidateCache();
        }
    }

    public void refreshAlertMail(boolean alertMail) {
        if (currentUser.getNodeUser() != null) {
            currentUser.getNodeUser().setAlertMail(alertMail);
        }
    }

    public boolean isSuperAdmin() {
        return resolveSessionUser().map(SessionUser::superAdmin).orElse(false);
    }

    public boolean canAccessProjectAdminScreen() {
        if (!isLoggedIn()) {
            return false;
        }
        return isSuperAdmin() || hasRoleAsAdmin();
    }

    public boolean canAccessSuperAdminScreen() {
        return isSuperAdmin();
    }

    public boolean hasRoleAsAdmin() {
        return resolveSessionUser().map(SessionUser::projectAdmin).orElse(false);
    }

    public boolean isContributor() {
        return resolveSessionUser().map(SessionUser::contributor).orElse(false);
    }

    public boolean isManager() {
        return resolveSessionUser().map(SessionUser::manager).orElse(false);
    }

    private java.util.Optional<SessionUser> resolveSessionUser() {
        if (!isLoggedIn()) {
            cachedSessionUser = null;
            return java.util.Optional.empty();
        }
        int userId = currentUser.getNodeUser().getIdUser();
        if (cachedSessionUser == null || cachedSessionUser.userId() != userId) {
            cachedSessionUser = sessionUserService.load(userId);
        }
        return java.util.Optional.of(cachedSessionUser);
    }

    private void invalidateCache() {
        cachedSessionUser = null;
    }
}
