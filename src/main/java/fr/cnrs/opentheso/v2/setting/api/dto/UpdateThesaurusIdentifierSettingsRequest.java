package fr.cnrs.opentheso.v2.setting.api.dto;

import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateThesaurusIdentifierSettingsRequest(
        @Schema(description = "Serveur d'identifiants actif") IdentifierServerType identifierServerType,
        String uriArk,
        String serverArk,
        String prefixArk,
        String userArk,
        @Schema(description = "Nouveau mot de passe Ark (laisser vide pour conserver)") String passArk,
        String naanArkLocal,
        String prefixArkLocal,
        Integer sizeIdArkLocal,
        String userHandle,
        @Schema(description = "Nouveau mot de passe Handle (laisser vide pour conserver)") String passHandle,
        String pathKeyHandle,
        String pathCertHandle,
        String urlApiHandle,
        String prefixIdHandle,
        String privatePrefixHandle,
        String adminHandle,
        Integer indexHandle,
        Boolean useHandleWithCertificat,
        Boolean generateHandle,
        String serverOpenArk,
        String naanOpenArk,
        String prefixOpenArk,
        @Schema(description = "Nouvelle clé API OpenArk (laisser vide pour conserver)") String apiKeyOpenArk,
        String idNaan,
        Integer identifierType
) {
}
