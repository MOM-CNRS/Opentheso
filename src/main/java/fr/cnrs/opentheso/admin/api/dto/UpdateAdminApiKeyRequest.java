package fr.cnrs.opentheso.admin.api.dto;

import java.time.LocalDate;

public record UpdateAdminApiKeyRequest(
        boolean hasApiKey,
        boolean keyNeverExpire,
        LocalDate keyExpiresAt
) {
}
