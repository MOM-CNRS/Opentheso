package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Changement de mot de passe")
public record ChangePasswordRequest(

        @NotBlank
        @Schema(description = "Nouveau mot de passe (8 car. min., majuscule, minuscule, chiffre, caractère spécial)")
        String password,

        @NotBlank
        @Schema(description = "Confirmation du nouveau mot de passe")
        String confirmation
) {
}
