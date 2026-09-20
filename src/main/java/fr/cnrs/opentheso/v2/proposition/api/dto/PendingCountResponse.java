package fr.cnrs.opentheso.v2.proposition.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PendingCountResponse(
        @Schema(description = "Nombre de propositions en attente") int count
) {
}
