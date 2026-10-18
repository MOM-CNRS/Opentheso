package fr.cnrs.opentheso.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Modification du pseudo")
public record UpdateUsernameRequest(
        @NotBlank
        @Schema(description = "Nouveau pseudo", example = "jdoe")
        String username
) {
}
