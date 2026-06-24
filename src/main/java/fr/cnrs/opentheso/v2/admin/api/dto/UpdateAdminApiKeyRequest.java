package fr.cnrs.opentheso.v2.admin.api.dto;

import java.time.LocalDate;

public record UpdateAdminApiKeyRequest(
        boolean hasApiKey,
        boolean keyNeverExpire,
        LocalDate keyExpiresAt
) {
}
