package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.shared.session.SessionUserService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Optional;

/**
 * Point d'accès unique à la session utilisateur pour la v2.
 * Les droits sont lus via {@link SessionUserService} / {@link RightsService} (cache Caffeine + TTL).
 */
@SessionScoped
@Named("v2UserSession")
@RequiredArgsConstructor
public class UserSession implements Serializable {

    private final transient AuthenticatedUserSource authenticatedUserSource;
    private final transient SessionUserService sessionUserService;
    private final transient RightsService rightsService;

    public boolean isLoggedIn() {
        return authenticatedUserSource.isLoggedIn();
    }

    public Integer getCurrentUserId() {
        return resolveSessionUser().map(SessionUser::userId).orElse(null);
    }

    public String getCurrentUsername() {
        return resolveSessionUser().map(SessionUser::username).orElse(null);
    }

    public String getCurrentUserEmail() {
        return resolveSessionUser().map(SessionUser::email).orElse(null);
    }

    public void refreshDisplayName(String name) {
        authenticatedUserSource.refreshDisplayName(name);
        invalidateCache();
    }

    public void refreshEmail(String email) {
        authenticatedUserSource.refreshEmail(email);
        invalidateCache();
    }

    public void refreshAlertMail(boolean alertMail) {
        authenticatedUserSource.refreshAlertMail(alertMail);
    }

    public boolean isSuperAdmin() {
        return resolveSessionUser().map(SessionUser::superAdmin).orElse(false);
    }

    public boolean canAccessProjectAdminScreen() {
        return rightsService.can(this, Permission.MANAGE_PROJECT);
    }

    public boolean canAccessSuperAdminScreen() {
        return rightsService.can(this, Permission.SUPER_ADMIN);
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

    public String getCurrentUserInitials() {
        String name = getCurrentUsername();
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        String token = parts[0];
        return token.length() >= 2 ? token.substring(0, 2).toUpperCase() : token.toUpperCase();
    }

    public String getCurrentRoleLabel() {
        if (isSuperAdmin()) {
            return "Super-admin";
        }
        if (isManager()) {
            return "Gestionnaire";
        }
        if (isContributor()) {
            return "Contributeur";
        }
        return "Lecteur";
    }

    /**
     * Force le rechargement des droits (profil / rôles) au prochain accès.
     */
    public void invalidateRightsCache() {
        invalidateCache();
    }

    private Optional<SessionUser> resolveSessionUser() {
        if (!isLoggedIn()) {
            return Optional.empty();
        }
        int userId = authenticatedUserSource.getUserId().orElseThrow();
        return Optional.of(sessionUserService.load(userId));
    }

    private void invalidateCache() {
        authenticatedUserSource.getUserId().ifPresent(sessionUserService::invalidate);
    }
}
