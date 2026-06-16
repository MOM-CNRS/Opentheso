package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Modification de l'adresse email")
public record UpdateEmailRequest(
        @NotBlank
        @Email
        @Schema(description = "Nouvelle adresse email", example = "jdoe@example.org")
        String email
) {
}
