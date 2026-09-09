package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

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

    /** Thésaurus en consultation publique ({@code private = false}), indépendamment du rôle utilisateur. */
    public boolean isPublicAccess() {
        return !privateThesaurus;
    }

    /** Libellé d'accès pour l'affichage (EL : {@code row.accessLabel}). */
    public String accessLabel() {
        if (isMember()) {
            return (role == null || role.isBlank()) ? "Membre" : role;
        }
        return privateThesaurus ? "Privé" : "Public";
    }

    public String getAccessLabel() {
        return accessLabel();
    }

    /** Classe CSS du badge (EL : {@code row.accessBadgeClass}). */
    public String accessBadgeClass() {
        if (isMember()) {
            String key = roleKey == null ? "" : roleKey;
            return switch (key) {
                case "superAdmin" -> "tp-badge--super";
                case "admin" -> "tp-badge--admin";
                case "manager" -> "tp-badge--manager";
                case "contributor" -> "tp-badge--contributor";
                default -> "tp-badge--member";
            };
        }
        return privateThesaurus ? "tp-badge--private" : "tp-badge--public";
    }

    public String getAccessBadgeClass() {
        return accessBadgeClass();
    }

    public String accessTitle() {
        if (isMember()) {
            return "Vous êtes " + accessLabel().toLowerCase(Locale.ROOT);
        }
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
}
