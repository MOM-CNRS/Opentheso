package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Profil de l'utilisateur connecté")
public record AccountProfileResponse(

        @Schema(description = "Identifiant interne de l'utilisateur", example = "42")
        Integer id,

        @Schema(description = "Pseudo de l'utilisateur", example = "jdoe")
        String username,

        @Schema(description = "Adresse email", example = "jdoe@example.org")
        String email,

        @Schema(description = "Alertes par email activées")
        boolean alertMail,

        @Schema(description = "Indique si l'utilisateur est super-administrateur")
        boolean superAdmin,

        @Schema(description = "La clé API n'a pas de date d'expiration")
        boolean keyNeverExpire,

        @Schema(description = "Date d'expiration de la clé API (null si sans expiration)")
        LocalDate keyExpiresAt,

        @Schema(description = "Une clé API est enregistrée en base")
        boolean hasApiKey,

        @Schema(description = "Section clé API visible (clé existante ou expiration configurée)")
        boolean apiKeySectionVisible,

        @Schema(description = "La clé API est expirée")
        boolean apiKeyExpired,

        @Schema(description = "La clé API peut être régénérée")
        boolean canRegenerateApiKey
) {
}
