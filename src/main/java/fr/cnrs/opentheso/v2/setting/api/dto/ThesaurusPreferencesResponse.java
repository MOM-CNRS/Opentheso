package fr.cnrs.opentheso.v2.setting.api.dto;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ThesaurusPreferencesResponse(
        @Schema(description = "Identifiant du thésaurus") String thesaurusId,
        @Schema(description = "Langue source") String sourceLang,
        @Schema(description = "Langues utilisées") List<ThesaurusLanguageResponse> languages,
        @Schema(description = "Nom préféré") String preferredName,
        @Schema(description = "Chemin du site") String cheminSite,
        @Schema(description = "URI d'origine") String originalUri,
        @Schema(description = "Type d'URI d'export") ExportUriType exportUriType,
        boolean autoExpandTree,
        boolean sortByNotation,
        boolean treeCache,
        boolean breadcrumb,
        boolean useConceptTree,
        boolean displayUserName,
        boolean suggestion,
        boolean useCustomRelation,
        boolean showHistoryNote,
        boolean showEditorialNote,
        boolean useDeeplTranslation,
        boolean hasDeeplApiKey,
        boolean webservices,
        boolean kohaLink,
        boolean uppercaseForArk
) {
}
