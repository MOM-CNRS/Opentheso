package fr.cnrs.opentheso.v2.setting.api.dto;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateThesaurusPreferencesRequest(
        @Schema(description = "Langue source") String sourceLang,
        @Schema(description = "Nom préféré") String preferredName,
        @Schema(description = "Chemin du site") String cheminSite,
        @Schema(description = "URI d'origine") String originalUri,
        @Schema(description = "Type d'URI d'export") ExportUriType exportUriType,
        Boolean autoExpandTree,
        Boolean sortByNotation,
        Boolean treeCache,
        Boolean breadcrumb,
        Boolean useConceptTree,
        Boolean displayUserName,
        Boolean suggestion,
        Boolean useCustomRelation,
        Boolean showHistoryNote,
        Boolean showEditorialNote,
        Boolean useDeeplTranslation,
        @Schema(description = "Nouvelle clé API Deepl (laisser vide pour conserver)") String deeplApiKey,
        Boolean webservices,
        Boolean kohaLink,
        Boolean uppercaseForArk
) {
}
