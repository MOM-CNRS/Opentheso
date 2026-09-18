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
import java.util.ArrayList;
import java.util.Locale;
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
        return initialsFromDisplayName(getCurrentUsername());
    }

    static String initialsFromDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("[.\\s_-]+");
        ArrayList<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                tokens.add(part);
            }
        }
        if (tokens.isEmpty()) {
            return "?";
        }
        if (tokens.size() >= 2) {
            return (firstLetter(tokens.get(0)) + firstLetter(tokens.get(tokens.size() - 1)))
                    .toUpperCase(Locale.ROOT);
        }
        String token = tokens.get(0);
        if (token.length() >= 2) {
            return token.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return token.toUpperCase(Locale.ROOT);
    }

    private static String firstLetter(String value) {
        int cp = value.codePointAt(0);
        return new String(Character.toChars(Character.toUpperCase(cp)));
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
