package fr.cnrs.opentheso.v2.setting.api.mapper;

import fr.cnrs.opentheso.v2.setting.api.dto.CorpusResponse;
import fr.cnrs.opentheso.v2.setting.api.dto.CreateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.ThesaurusIdentifierSettingsResponse;
import fr.cnrs.opentheso.v2.setting.api.dto.ThesaurusLanguageResponse;
import fr.cnrs.opentheso.v2.setting.api.dto.ThesaurusPreferencesResponse;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateCorpusRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusIdentifierSettingsRequest;
import fr.cnrs.opentheso.v2.setting.api.dto.UpdateThesaurusPreferencesRequest;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class SettingApiMapper {

    private SettingApiMapper() {
    }

    public static ThesaurusPreferencesResponse toPreferencesResponse(ThesaurusPreferences preferences) {
        return new ThesaurusPreferencesResponse(
                preferences.thesaurusId(),
                preferences.sourceLang(),
                toLanguageResponses(preferences.languages()),
                preferences.preferredName(),
                preferences.cheminSite(),
                preferences.originalUri(),
                preferences.exportUriType(),
                preferences.autoExpandTree(),
                preferences.sortByNotation(),
                preferences.treeCache(),
                preferences.breadcrumb(),
                preferences.useConceptTree(),
                preferences.displayUserName(),
                preferences.suggestion(),
                preferences.useCustomRelation(),
                preferences.showHistoryNote(),
                preferences.showEditorialNote(),
                preferences.useDeeplTranslation(),
                StringUtils.isNotBlank(preferences.deeplApiKey()),
                preferences.webservices(),
                preferences.kohaLink(),
                preferences.uppercaseForArk()
        );
    }

    public static ThesaurusIdentifierSettingsResponse toIdentifierResponse(ThesaurusPreferences preferences) {
        return new ThesaurusIdentifierSettingsResponse(
                preferences.thesaurusId(),
                preferences.identifierServerType(),
                preferences.useArk(),
                preferences.useArkLocal(),
                preferences.useHandle(),
                preferences.useOpenArk(),
                preferences.uriArk(),
                preferences.serverArk(),
                preferences.prefixArk(),
                preferences.userArk(),
                StringUtils.isNotBlank(preferences.passArk()),
                preferences.naanArkLocal(),
                preferences.prefixArkLocal(),
                preferences.sizeIdArkLocal(),
                preferences.userHandle(),
                StringUtils.isNotBlank(preferences.passHandle()),
                preferences.pathKeyHandle(),
                preferences.pathCertHandle(),
                preferences.urlApiHandle(),
                preferences.prefixIdHandle(),
                preferences.privatePrefixHandle(),
                preferences.adminHandle(),
                preferences.indexHandle(),
                preferences.useHandleWithCertificat(),
                preferences.generateHandle(),
                preferences.serverOpenArk(),
                preferences.naanOpenArk(),
                preferences.prefixOpenArk(),
                StringUtils.isNotBlank(preferences.apiKeyOpenArk()),
                preferences.idNaan(),
                preferences.identifierType()
        );
    }

    public static ThesaurusPreferences mergePreferencesUpdate(
            ThesaurusPreferences current,
            UpdateThesaurusPreferencesRequest request
    ) {
        return new ThesaurusPreferences(
                current.thesaurusId(),
                coalesce(request.sourceLang(), current.sourceLang()),
                current.identifierType(),
                coalesce(request.cheminSite(), current.cheminSite()),
                current.idNaan(),
                coalesce(request.preferredName(), current.preferredName()),
                coalesce(request.originalUri(), current.originalUri()),
                request.exportUriType() != null ? request.exportUriType() : current.exportUriType(),
                current.identifierServerType(),
                current.useHandle(),
                current.userHandle(),
                current.passHandle(),
                current.pathKeyHandle(),
                current.pathCertHandle(),
                current.urlApiHandle(),
                current.prefixIdHandle(),
                current.privatePrefixHandle(),
                current.uriArk(),
                current.useArk(),
                current.serverArk(),
                current.prefixArk(),
                current.userArk(),
                current.passArk(),
                current.generateHandle(),
                coalesce(request.autoExpandTree(), current.autoExpandTree()),
                coalesce(request.sortByNotation(), current.sortByNotation()),
                coalesce(request.treeCache(), current.treeCache()),
                current.useArkLocal(),
                current.naanArkLocal(),
                current.prefixArkLocal(),
                current.sizeIdArkLocal(),
                coalesce(request.breadcrumb(), current.breadcrumb()),
                coalesce(request.useConceptTree(), current.useConceptTree()),
                coalesce(request.displayUserName(), current.displayUserName()),
                coalesce(request.suggestion(), current.suggestion()),
                coalesce(request.useCustomRelation(), current.useCustomRelation()),
                coalesce(request.uppercaseForArk(), current.uppercaseForArk()),
                coalesce(request.showHistoryNote(), current.showHistoryNote()),
                coalesce(request.showEditorialNote(), current.showEditorialNote()),
                current.useHandleWithCertificat(),
                current.adminHandle(),
                current.indexHandle(),
                coalesce(request.useDeeplTranslation(), current.useDeeplTranslation()),
                StringUtils.isNotBlank(request.deeplApiKey()) ? request.deeplApiKey() : current.deeplApiKey(),
                coalesce(request.webservices(), current.webservices()),
                coalesce(request.kohaLink(), current.kohaLink()),
                current.useOpenArk(),
                current.serverOpenArk(),
                current.naanOpenArk(),
                current.prefixOpenArk(),
                current.apiKeyOpenArk(),
                current.languages()
        );
    }

    public static ThesaurusPreferences mergeIdentifierUpdate(
            ThesaurusPreferences current,
            UpdateThesaurusIdentifierSettingsRequest request
    ) {
        IdentifierServerType serverType = request.identifierServerType() != null
                ? request.identifierServerType()
                : current.identifierServerType();
        return new ThesaurusPreferences(
                current.thesaurusId(),
                current.sourceLang(),
                request.identifierType() != null ? request.identifierType() : current.identifierType(),
                current.cheminSite(),
                coalesce(request.idNaan(), current.idNaan()),
                current.preferredName(),
                current.originalUri(),
                current.exportUriType(),
                serverType,
                serverType == IdentifierServerType.HANDLE,
                coalesce(request.userHandle(), current.userHandle()),
                StringUtils.isNotBlank(request.passHandle()) ? request.passHandle() : current.passHandle(),
                coalesce(request.pathKeyHandle(), current.pathKeyHandle()),
                coalesce(request.pathCertHandle(), current.pathCertHandle()),
                coalesce(request.urlApiHandle(), current.urlApiHandle()),
                coalesce(request.prefixIdHandle(), current.prefixIdHandle()),
                coalesce(request.privatePrefixHandle(), current.privatePrefixHandle()),
                coalesce(request.uriArk(), current.uriArk()),
                serverType == IdentifierServerType.ARK,
                coalesce(request.serverArk(), current.serverArk()),
                coalesce(request.prefixArk(), current.prefixArk()),
                coalesce(request.userArk(), current.userArk()),
                StringUtils.isNotBlank(request.passArk()) ? request.passArk() : current.passArk(),
                coalesce(request.generateHandle(), current.generateHandle()),
                current.autoExpandTree(),
                current.sortByNotation(),
                current.treeCache(),
                serverType == IdentifierServerType.ARK_LOCAL,
                coalesce(request.naanArkLocal(), current.naanArkLocal()),
                coalesce(request.prefixArkLocal(), current.prefixArkLocal()),
                request.sizeIdArkLocal() != null ? request.sizeIdArkLocal() : current.sizeIdArkLocal(),
                current.breadcrumb(),
                current.useConceptTree(),
                current.displayUserName(),
                current.suggestion(),
                current.useCustomRelation(),
                current.uppercaseForArk(),
                current.showHistoryNote(),
                current.showEditorialNote(),
                coalesce(request.useHandleWithCertificat(), current.useHandleWithCertificat()),
                coalesce(request.adminHandle(), current.adminHandle()),
                request.indexHandle() != null ? request.indexHandle() : current.indexHandle(),
                current.useDeeplTranslation(),
                current.deeplApiKey(),
                current.webservices(),
                current.kohaLink(),
                serverType == IdentifierServerType.OPENARK,
                coalesce(request.serverOpenArk(), current.serverOpenArk()),
                coalesce(request.naanOpenArk(), current.naanOpenArk()),
                coalesce(request.prefixOpenArk(), current.prefixOpenArk()),
                current.apiKeyOpenArk(),
                current.languages()
        );
    }

    public static CorpusResponse toCorpusResponse(ThesaurusCorpus corpus) {
        return new CorpusResponse(
                corpus.corpusName(),
                corpus.uriLink(),
                corpus.uriCount(),
                corpus.active(),
                corpus.onlyUriLink(),
                corpus.omekaS(),
                corpus.sort()
        );
    }

    public static ThesaurusCorpus toCorpusModel(CreateCorpusRequest request) {
        return new ThesaurusCorpus(
                request.name(),
                request.uriLink(),
                request.uriCount(),
                request.active(),
                request.onlyUriLink(),
                request.omekaS(),
                null
        );
    }

    public static ThesaurusCorpus toCorpusModel(UpdateCorpusRequest request) {
        return new ThesaurusCorpus(
                request.name(),
                request.uriLink(),
                request.uriCount(),
                request.active(),
                request.onlyUriLink(),
                request.omekaS(),
                null
        );
    }

    private static List<ThesaurusLanguageResponse> toLanguageResponses(List<ThesaurusLanguage> languages) {
        if (languages == null) {
            return List.of();
        }
        return languages.stream()
                .map(lang -> new ThesaurusLanguageResponse(
                        lang.id(),
                        lang.code(),
                        lang.codeFlag(),
                        lang.labelTheso(),
                        lang.displayLabel()
                ))
                .toList();
    }

    private static String coalesce(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private static boolean coalesce(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }
}
