package fr.cnrs.opentheso.v2.admin.model;

import java.io.Serializable;

/**
 * Accès d'un utilisateur à un thésaurus : rôle limité ou héritage projet entier.
 */
public record ThesaurusMember(
        int userId,
        String username,
        boolean active,
        int roleId,
        String roleName,
        boolean projectWide
) implements Serializable {

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isActive() {
        return active;
    }

    public int getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isProjectWide() {
        return projectWide;
    }

    public boolean isLimited() {
        return !projectWide;
    }
}
