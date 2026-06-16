package fr.cnrs.opentheso.v2.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Activation ou désactivation des alertes par email")
public record UpdateAlertMailRequest(
        @Schema(description = "Recevoir les alertes par email")
        boolean alertMail
) {
}
