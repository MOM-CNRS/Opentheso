package fr.cnrs.opentheso.v2.setting.api.dto;

import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ThesaurusIdentifierSettingsResponse(
        @Schema(description = "Identifiant du thésaurus") String thesaurusId,
        @Schema(description = "Serveur d'identifiants actif") IdentifierServerType identifierServerType,
        boolean useArk,
        boolean useArkLocal,
        boolean useHandle,
        boolean useOpenArk,
        String uriArk,
        String serverArk,
        String prefixArk,
        String userArk,
        boolean hasPassArk,
        String naanArkLocal,
        String prefixArkLocal,
        Integer sizeIdArkLocal,
        String userHandle,
        boolean hasPassHandle,
        String pathKeyHandle,
        String pathCertHandle,
        String urlApiHandle,
        String prefixIdHandle,
        String privatePrefixHandle,
        String adminHandle,
        Integer indexHandle,
        boolean useHandleWithCertificat,
        boolean generateHandle,
        String serverOpenArk,
        String naanOpenArk,
        String prefixOpenArk,
        boolean hasApiKeyOpenArk,
        String idNaan,
        Integer identifierType
) {
}
