package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de la régénération de clé API")
public record ApiKeyRegenerateResponse(

        @Schema(description = "Nouvelle clé API en clair (affichée une seule fois)")
        String plainTextApiKey,

        @Schema(description = "Profil mis à jour après régénération")
        AccountProfileResponse profile
) {
}
