package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PreferenceEditor implements Serializable {

    private String sourceLang;
    private Integer identifierType;
    private String cheminSite;
    private String idNaan;
    private String preferredName;
    private String originalUri;
    private String uriType = "uri";
    private ExportUriType exportUriType = ExportUriType.URI;
    private IdentifierServerType identifierServerType = IdentifierServerType.NONE;

    private boolean useHandle;
    private String userHandle;
    private String passHandle;
    private String pathKeyHandle;
    private String pathCertHandle;
    private String urlApiHandle;
    private String prefixIdHandle;
    private String privatePrefixHandle;

    private String uriArk;
    private boolean useArk;
    private String serverArk;
    private String prefixArk;
    private String userArk;
    private String passArk;
    private boolean generateHandle;

    private boolean useArkLocal;
    private String naanArkLocal;
    private String prefixArkLocal;
    private Integer sizeIdArkLocal;
    private boolean uppercaseForArk;

    private boolean useOpenArk;
    private String serverOpenArk;
    private String naanOpenArk;
    private String prefixOpenArk;
    private String apiKeyOpenArk;

    private boolean autoExpandTree;
    private boolean sortByNotation;
    private boolean treeCache;
    private boolean breadcrumb;
    private boolean useConceptTree;
    private boolean displayUserName;
    private boolean suggestion;
    private boolean useCustomRelation;
    private boolean showHistoryNote;
    private boolean showEditorialNote;
    private boolean useHandleWithCertificat;
    private String adminHandle;
    private Integer indexHandle;
    private boolean useDeeplTranslation;
    private String deeplApiKey;
    private boolean webservices;
    private boolean kohaLink;

    private String newPassArk;
    private String newPassHandle;
    private String newDeeplApiKey;
    private String newApiKeyOpenArk;

    private List<ThesaurusLanguage> languages = new ArrayList<>();

    public static PreferenceEditor from(ThesaurusPreferences preferences) {
        PreferenceEditor editor = new PreferenceEditor();
        editor.setSourceLang(preferences.sourceLang());
        editor.setIdentifierType(preferences.identifierType());
        editor.setCheminSite(preferences.cheminSite());
        editor.setIdNaan(preferences.idNaan());
        editor.setPreferredName(preferences.preferredName());
        editor.setOriginalUri(preferences.originalUri());
        editor.setExportUriType(preferences.exportUriType());
        editor.setUriType(toUriType(preferences.exportUriType()));
        editor.setIdentifierServerType(preferences.identifierServerType());
        editor.setUseHandle(preferences.useHandle());
        editor.setUserHandle(preferences.userHandle());
        editor.setPassHandle("");
        editor.setPathKeyHandle(preferences.pathKeyHandle());
        editor.setPathCertHandle(preferences.pathCertHandle());
        editor.setUrlApiHandle(preferences.urlApiHandle());
        editor.setPrefixIdHandle(preferences.prefixIdHandle());
        editor.setPrivatePrefixHandle(preferences.privatePrefixHandle());
        editor.setUriArk(preferences.uriArk());
        editor.setUseArk(preferences.useArk());
        editor.setServerArk(preferences.serverArk());
        editor.setPrefixArk(preferences.prefixArk());
        editor.setUserArk(preferences.userArk());
        editor.setPassArk("");
        editor.setGenerateHandle(preferences.generateHandle());
        editor.setUseArkLocal(preferences.useArkLocal());
        editor.setNaanArkLocal(preferences.naanArkLocal());
        editor.setPrefixArkLocal(preferences.prefixArkLocal());
        editor.setSizeIdArkLocal(preferences.sizeIdArkLocal());
        editor.setUppercaseForArk(preferences.uppercaseForArk());
        editor.setUseOpenArk(preferences.useOpenArk());
        editor.setServerOpenArk(preferences.serverOpenArk());
        editor.setNaanOpenArk(preferences.naanOpenArk());
        editor.setPrefixOpenArk(preferences.prefixOpenArk());
        editor.setApiKeyOpenArk("");
        editor.setAutoExpandTree(preferences.autoExpandTree());
        editor.setSortByNotation(preferences.sortByNotation());
        editor.setTreeCache(preferences.treeCache());
        editor.setBreadcrumb(preferences.breadcrumb());
        editor.setUseConceptTree(preferences.useConceptTree());
        editor.setDisplayUserName(preferences.displayUserName());
        editor.setSuggestion(preferences.suggestion());
        editor.setUseCustomRelation(preferences.useCustomRelation());
        editor.setShowHistoryNote(preferences.showHistoryNote());
        editor.setShowEditorialNote(preferences.showEditorialNote());
        editor.setUseHandleWithCertificat(preferences.useHandleWithCertificat());
        editor.setAdminHandle(preferences.adminHandle());
        editor.setIndexHandle(preferences.indexHandle());
        editor.setUseDeeplTranslation(preferences.useDeeplTranslation());
        editor.setDeeplApiKey("");
        editor.setWebservices(preferences.webservices());
        editor.setKohaLink(preferences.kohaLink());
        editor.setLanguages(new ArrayList<>(preferences.languages()));
        return editor;
    }

    public ThesaurusPreferences toModel(String thesaurusId) {
        ExportUriType resolvedExportUriType = fromUriType(uriType);
        return new ThesaurusPreferences(
                thesaurusId,
                sourceLang,
                identifierType,
                cheminSite,
                idNaan,
                preferredName,
                originalUri,
                resolvedExportUriType,
                identifierServerType,
                useHandle,
                userHandle,
                passHandle,
                pathKeyHandle,
                pathCertHandle,
                urlApiHandle,
                prefixIdHandle,
                privatePrefixHandle,
                uriArk,
                useArk,
                serverArk,
                prefixArk,
                userArk,
                passArk,
                generateHandle,
                autoExpandTree,
                sortByNotation,
                treeCache,
                useArkLocal,
                naanArkLocal,
                prefixArkLocal,
                sizeIdArkLocal,
                breadcrumb,
                useConceptTree,
                displayUserName,
                suggestion,
                useCustomRelation,
                uppercaseForArk,
                showHistoryNote,
                showEditorialNote,
                useHandleWithCertificat,
                adminHandle,
                indexHandle,
                useDeeplTranslation,
                deeplApiKey,
                webservices,
                kohaLink,
                useOpenArk,
                serverOpenArk,
                naanOpenArk,
                prefixOpenArk,
                apiKeyOpenArk,
                languages
        );
    }

    private static String toUriType(ExportUriType exportUriType) {
        if (exportUriType == null) {
            return "uri";
        }
        return switch (exportUriType) {
            case HANDLE -> "handle";
            case ARK -> "ark";
            case DOI -> "doi";
            case URI -> "uri";
        };
    }

    private static ExportUriType fromUriType(String uriTypeValue) {
        if (uriTypeValue == null) {
            return ExportUriType.URI;
        }
        return switch (uriTypeValue.toLowerCase()) {
            case "handle" -> ExportUriType.HANDLE;
            case "ark" -> ExportUriType.ARK;
            case "doi" -> ExportUriType.DOI;
            default -> ExportUriType.URI;
        };
    }
}
