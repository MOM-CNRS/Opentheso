package fr.cnrs.opentheso.v2.proposition.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SubmitPropositionRequest(
        @NotBlank @Schema(description = "Identifiant du concept", example = "12345")
        String conceptId,
        @Schema(description = "Libellé courant du concept")
        String conceptLabel,
        @Schema(description = "Langue de travail", example = "fr")
        String lang,
        @NotBlank @Schema(description = "Commentaire / motivation de la proposition")
        String comment,
        @Schema(description = "Nom d'auteur (sinon profil API)")
        String authorName,
        @Schema(description = "Email d'auteur (sinon profil API)")
        String authorEmail,
        @Schema(description = "Libellé préféré proposé (optionnel)")
        String proposedPreferredLabel,
        @Schema(description = "Libellé préféré actuel (pour détecter le changement)")
        String currentPreferredLabel,
        @Schema(description = "Autoriser plusieurs propositions pending du même auteur")
        Boolean allowMultiplePending
) {
}
