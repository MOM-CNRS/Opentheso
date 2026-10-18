package fr.cnrs.opentheso.proposition.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PendingCountResponse(
        @Schema(description = "Nombre de propositions en attente") int count
) {
}
