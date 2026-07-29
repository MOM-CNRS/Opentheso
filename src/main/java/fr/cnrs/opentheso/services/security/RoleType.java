package fr.cnrs.opentheso.services.security;

import java.util.Arrays;

/**
 * Représente les niveaux de rôle applicables sur un projet/groupe ou un thésaurus.
 * Correspond aux lignes de la table "roles" en base :
 *   1 = superAdmin, 2 = admin, 3 = manager, 4 = contributor
 *
 * Le "level" sert uniquement à la comparaison hiérarchique en Java
 * (plus le chiffre est petit, plus le rôle a de pouvoir).
 * SUPER_ADMIN n'est jamais assigné via user_role_group / user_role_only_on :
 * il découle uniquement du booléen users.issuperadmin.
 */
public enum RoleType {

    SUPER_ADMIN(1, 0),
    ADMIN(2, 10),
    MANAGER(3, 20),
    CONTRIBUTOR(4, 30);

    private final int idRole;
    private final int level;

    RoleType(int idRole, int level) {
        this.idRole = idRole;
        this.level = level;
    }

    /**
     * Vrai si ce rôle a au moins autant de pouvoir que "other".
     * Exemple : ADMIN.isAtLeast(MANAGER) -> true
     *           CONTRIBUTOR.isAtLeast(MANAGER) -> false
     */
    public boolean isAtLeast(RoleType other) {
        return this.level <= other.level;
    }

    /**
     * Retrouve le RoleType correspondant à un id de la table "roles".
     * Lève une exception si l'id est inconnu (fail-fast plutôt qu'un droit silencieusement absent).
     */
    public static RoleType fromId(int idRole) {
        return Arrays.stream(values())
                .filter(r -> r.idRole == idRole)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Rôle inconnu id=" + idRole));
    }

    public int getIdRole() {
        return idRole;
    }

    public int getLevel() {
        return level;
    }
}