package fr.cnrs.opentheso.v2.user.model;

import java.time.LocalDate;
import java.io.Serializable;

/**
 * Profil utilisateur exposé par la couche v2 (sans mot de passe ni clé API en clair).
 */
public record UserProfile(
        Integer id,
        String username,
        String email,
        boolean alertMail,
        boolean superAdmin,
        boolean keyNeverExpire,
        LocalDate keyExpiresAt,
        boolean hasApiKey
) implements Serializable {
}
