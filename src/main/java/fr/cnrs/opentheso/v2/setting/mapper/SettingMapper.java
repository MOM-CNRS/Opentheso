package fr.cnrs.opentheso.v2.setting.mapper;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.shared.persistence.CorpusLinkEntity;
import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import fr.cnrs.opentheso.v2.shared.repository.projection.ThesaurusLanguageRow;

import java.util.List;

public final class SettingMapper {

    private SettingMapper() {
    }

    public static ThesaurusLanguage toLanguage(ThesaurusLanguageRow row) {
        return new ThesaurusLanguage(
                row.id(),
                row.code(),
                row.codeFlag(),
                row.labelTheso(),
                row.displayLabel()
        );
    }

    public static ThesaurusPreferences toPreferences(PreferencesEntity entity, List<ThesaurusLanguage> languages) {
        return new ThesaurusPreferences(
                entity.getIdThesaurus(),
                entity.getSourceLang(),
                entity.getIdentifierType(),
                entity.getCheminSite(),
                entity.getIdNaan(),
                entity.getPreferredName(),
                entity.getOriginalUri(),
                toExportUriType(entity),
                toIdentifierServerType(entity),
                entity.isUseHandle(),
                entity.getUserHandle(),
                entity.getPassHandle(),
                entity.getPathKeyHandle(),
                entity.getPathCertHandle(),
                entity.getUrlApiHandle(),
                entity.getPrefixIdHandle(),
                entity.getPrivatePrefixHandle(),
                entity.getUriArk(),
                entity.isUseArk(),
                entity.getServerArk(),
                entity.getPrefixArk(),
                entity.getUserArk(),
                entity.getPassArk(),
                entity.isGenerateHandle(),
                entity.isAutoExpandTree(),
                entity.isSortByNotation(),
                entity.isTreeCache(),
                entity.isUseArkLocal(),
                entity.getNaanArkLocal(),
                entity.getPrefixArkLocal(),
                entity.getSizeIdArkLocal(),
                entity.isBreadcrumb(),
                entity.isUseConceptTree(),
                entity.isDisplayUserName(),
                entity.isSuggestion(),
                entity.isUseCustomRelation(),
                entity.isUppercaseForArk(),
                entity.isShowHistoryNote(),
                entity.isShowEditorialNote(),
                entity.isUseHandleWithCertificat(),
                entity.getAdminHandle(),
                entity.getIndexHandle(),
                entity.isUseDeeplTranslation(),
                entity.getDeeplApiKey(),
                entity.isWebservices(),
                entity.isKohaLink(),
                entity.isUseOpenArk(),
                entity.getServerOpenArk(),
                entity.getNaanOpenArk(),
                entity.getPrefixOpenArk(),
                entity.getApiKeyOpenArk(),
                languages
        );
    }

    public static void applyPreferences(PreferencesEntity entity, ThesaurusPreferences preferences) {
        entity.setSourceLang(preferences.sourceLang());
        entity.setIdentifierType(preferences.identifierType());
        entity.setCheminSite(preferences.cheminSite());
        entity.setIdNaan(preferences.idNaan());
        entity.setPreferredName(preferences.preferredName());
        entity.setOriginalUri(preferences.originalUri());
        applyExportUriType(entity, preferences.exportUriType());
        applyIdentifierServerType(entity, preferences.identifierServerType());
        entity.setUseHandle(preferences.useHandle());
        entity.setUserHandle(preferences.userHandle());
        entity.setPassHandle(preferences.passHandle());
        entity.setPathKeyHandle(preferences.pathKeyHandle());
        entity.setPathCertHandle(preferences.pathCertHandle());
        entity.setUrlApiHandle(preferences.urlApiHandle());
        entity.setPrefixIdHandle(preferences.prefixIdHandle());
        entity.setPrivatePrefixHandle(preferences.privatePrefixHandle());
        entity.setUriArk(preferences.uriArk());
        entity.setUseArk(preferences.useArk());
        entity.setServerArk(preferences.serverArk());
        entity.setPrefixArk(preferences.prefixArk());
        entity.setUserArk(preferences.userArk());
        entity.setPassArk(preferences.passArk());
        entity.setGenerateHandle(preferences.generateHandle());
        entity.setAutoExpandTree(preferences.autoExpandTree());
        entity.setSortByNotation(preferences.sortByNotation());
        entity.setTreeCache(preferences.treeCache());
        entity.setUseArkLocal(preferences.useArkLocal());
        entity.setNaanArkLocal(preferences.naanArkLocal());
        entity.setPrefixArkLocal(preferences.prefixArkLocal());
        entity.setSizeIdArkLocal(preferences.sizeIdArkLocal());
        entity.setBreadcrumb(preferences.breadcrumb());
        entity.setUseConceptTree(preferences.useConceptTree());
        entity.setDisplayUserName(preferences.displayUserName());
        entity.setSuggestion(preferences.suggestion());
        entity.setUseCustomRelation(preferences.useCustomRelation());
        entity.setUppercaseForArk(preferences.uppercaseForArk());
        entity.setShowHistoryNote(preferences.showHistoryNote());
        entity.setShowEditorialNote(preferences.showEditorialNote());
        entity.setUseHandleWithCertificat(preferences.useHandleWithCertificat());
        entity.setAdminHandle(preferences.adminHandle());
        entity.setIndexHandle(preferences.indexHandle());
        entity.setUseDeeplTranslation(preferences.useDeeplTranslation());
        entity.setDeeplApiKey(preferences.deeplApiKey());
        entity.setWebservices(preferences.webservices());
        entity.setKohaLink(preferences.kohaLink());
        entity.setUseOpenArk(preferences.useOpenArk());
        entity.setServerOpenArk(preferences.serverOpenArk());
        entity.setNaanOpenArk(preferences.naanOpenArk());
        entity.setPrefixOpenArk(preferences.prefixOpenArk());
        entity.setApiKeyOpenArk(preferences.apiKeyOpenArk());
    }

    public static ThesaurusCorpus toCorpus(CorpusLinkEntity entity) {
        return new ThesaurusCorpus(
                entity.getCorpusName(),
                entity.getUriLink(),
                entity.getUriCount(),
                entity.isActive(),
                entity.isOnlyUriLink(),
                entity.isOmekaS(),
                entity.getSort()
        );
    }

    public static CorpusLinkEntity toCorpusEntity(String thesaurusId, ThesaurusCorpus corpus) {
        return CorpusLinkEntity.builder()
                .idThesaurus(thesaurusId)
                .corpusName(corpus.corpusName())
                .uriLink(corpus.uriLink())
                .uriCount(corpus.uriCount())
                .active(corpus.active())
                .onlyUriLink(corpus.onlyUriLink())
                .omekaS(corpus.omekaS())
                .sort(corpus.sort())
                .build();
    }

    public static void applyCorpus(CorpusLinkEntity entity, ThesaurusCorpus corpus) {
        entity.setUriLink(corpus.uriLink());
        entity.setUriCount(corpus.uriCount());
        entity.setActive(corpus.active());
        entity.setOnlyUriLink(corpus.onlyUriLink());
        entity.setOmekaS(corpus.omekaS());
        entity.setSort(corpus.sort());
    }

    public static ExportUriType toExportUriType(PreferencesEntity entity) {
        if (entity.isOriginalUriIsHandle()) {
            return ExportUriType.HANDLE;
        }
        if (entity.isOriginalUriIsArk()) {
            return ExportUriType.ARK;
        }
        if (entity.isOriginalUriIsDoi()) {
            return ExportUriType.DOI;
        }
        return ExportUriType.URI;
    }

    public static void applyExportUriType(PreferencesEntity entity, ExportUriType exportUriType) {
        if (exportUriType == null) {
            return;
        }
        entity.setOriginalUriIsHandle(exportUriType == ExportUriType.HANDLE);
        entity.setOriginalUriIsArk(exportUriType == ExportUriType.ARK);
        entity.setOriginalUriIsDoi(exportUriType == ExportUriType.DOI);
    }

    public static IdentifierServerType toIdentifierServerType(PreferencesEntity entity) {
        if (entity.isUseOpenArk()) {
            return IdentifierServerType.OPENARK;
        }
        if (entity.isUseHandle()) {
            return IdentifierServerType.HANDLE;
        }
        if (entity.isUseArkLocal()) {
            return IdentifierServerType.ARK_LOCAL;
        }
        if (entity.isUseArk()) {
            return IdentifierServerType.ARK;
        }
        return IdentifierServerType.NONE;
    }

    public static void applyIdentifierServerType(PreferencesEntity entity, IdentifierServerType serverType) {
        if (serverType == null) {
            return;
        }
        entity.setUseArk(serverType == IdentifierServerType.ARK);
        entity.setUseArkLocal(serverType == IdentifierServerType.ARK_LOCAL);
        entity.setUseHandle(serverType == IdentifierServerType.HANDLE);
        entity.setUseOpenArk(serverType == IdentifierServerType.OPENARK);
    }
}
