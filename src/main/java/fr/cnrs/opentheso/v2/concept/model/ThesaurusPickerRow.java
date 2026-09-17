package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Ligne de la page de sélection de thésaurus (maquette {@code ThesaurusPicker}).
 */
public record ThesaurusPickerRow(
        String id,
        String name,
        String access,
        String role,
        String roleKey,
        long terms,
        LocalDate created,
        String projects,
        String organization,
        String domain,
        String chronology,
        List<String> languages,
        boolean privateThesaurus
) implements Serializable {

    public boolean isMember() {
        return "member".equalsIgnoreCase(access);
    }

    /** Accesseur EL record ({@code row.member} → {@code member()}). */
    public boolean member() {
        return isMember();
    }

    /** Thésaurus en consultation publique ({@code private = false}), indépendamment du rôle utilisateur. */
    public boolean isPublicAccess() {
        return !privateThesaurus;
    }

    /** Accesseur EL record ({@code row.publicAccess} → {@code publicAccess()}). */
    public boolean publicAccess() {
        return isPublicAccess();
    }

    /** Libellé d'accès = visibilité du thésaurus (EL : {@code row.accessLabel}). */
    public String accessLabel() {
        return privateThesaurus ? "Privé" : "Public";
    }

    public String getAccessLabel() {
        return accessLabel();
    }

    /** Classe CSS du badge de visibilité (EL : {@code row.accessBadgeClass}). */
    public String accessBadgeClass() {
        return privateThesaurus ? "tp-badge--private" : "tp-badge--public";
    }

    public String getAccessBadgeClass() {
        return accessBadgeClass();
    }

    public String accessTitle() {
        if (privateThesaurus) {
            return "Thésaurus privé";
        }
        return "Consultation publique";
    }

    public String getAccessTitle() {
        return accessTitle();
    }

    /** Accesseur EL record ({@code row.createdLabel} → {@code createdLabel()}). */
    public String createdLabel() {
        return created == null ? "" : created.toString();
    }

    /** Accesseur EL record ({@code row.termsLabel} → {@code termsLabel()}). */
    public String termsLabel() {
        return String.format("%,d", terms).replace(',', '\u202f');
    }

    public String getCreatedLabel() {
        return createdLabel();
    }

    public String getTermsLabel() {
        return termsLabel();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAccess() {
        return access;
    }

    public String getRole() {
        return role;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public long getTerms() {
        return terms;
    }

    public LocalDate getCreated() {
        return created;
    }

    public String getProjects() {
        return projects;
    }

    public String getOrganization() {
        return organization;
    }

    public String getDomain() {
        return domain;
    }

    public String getChronology() {
        return chronology;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public boolean isPrivateThesaurus() {
        return privateThesaurus;
    }

    /** Rôle affiché (colonne Rôle / tri). */
    public String roleLabel() {
        return role == null ? "" : role;
    }

    public String getRoleLabel() {
        return roleLabel();
    }

    public boolean hasRole() {
        return role != null && !role.isBlank();
    }

    public boolean getHasRole() {
        return hasRole();
    }

    /** Droit d'édition / suppression depuis la liste (admin, manager, super-admin). */
    public boolean canManage() {
        String key = roleKey == null ? "" : roleKey;
        return switch (key) {
            case "superAdmin", "admin", "manager" -> true;
            default -> false;
        };
    }

    public String roleBadgeClass() {
        String key = roleKey == null ? "" : roleKey;
        return switch (key) {
            case "superAdmin" -> "tp-badge--super";
            case "admin" -> "tp-badge--admin";
            case "manager" -> "tp-badge--manager";
            case "contributor" -> "tp-badge--contributor";
            default -> hasRole() ? "tp-badge--member" : "";
        };
    }

    public String getRoleBadgeClass() {
        return roleBadgeClass();
    }

    public String roleTitle() {
        if (!hasRole()) {
            return "Aucun rôle sur ce thésaurus";
        }
        return "Votre rôle : " + roleLabel();
    }

    public String getRoleTitle() {
        return roleTitle();
    }
}
