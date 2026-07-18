package fr.cnrs.opentheso.v2.publicapi.system.api.dto;

public record ApiKeyValidationResponse(
        boolean valid,
        int userId,
        String username,
        boolean superAdmin
) {
}
