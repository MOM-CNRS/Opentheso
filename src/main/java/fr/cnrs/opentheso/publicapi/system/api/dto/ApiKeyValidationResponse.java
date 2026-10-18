package fr.cnrs.opentheso.publicapi.system.api.dto;

public record ApiKeyValidationResponse(
        boolean valid,
        int userId,
        String username,
        boolean superAdmin
) {
}
