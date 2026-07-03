package fr.cnrs.opentheso.v2.concept.policy;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import org.apache.commons.lang3.StringUtils;

public final class ConceptUriBuilder {

    private ConceptUriBuilder() {
    }

    public static String buildConceptUri(
            ThesaurusPreferences preferences,
            String applicationBaseUrl,
            String conceptId,
            String thesaurusId,
            String arkId,
            String handleId,
            String doiId
    ) {
        String sitePath = normalizePath(StringUtils.defaultIfBlank(
                preferences != null ? preferences.cheminSite() : "",
                applicationBaseUrl
        ));
        ExportUriType exportType = preferences != null ? preferences.exportUriType() : ExportUriType.URI;
        String originalUri = preferences != null ? StringUtils.defaultString(preferences.originalUri()) : "";

        return switch (exportType) {
            case ARK -> buildArkUri(originalUri, sitePath, conceptId, thesaurusId, arkId);
            case HANDLE -> buildHandleUri(sitePath, conceptId, thesaurusId, handleId);
            case DOI -> buildDoiUri(sitePath, conceptId, thesaurusId, doiId);
            case URI -> buildDefaultUri(originalUri, sitePath, conceptId, thesaurusId);
        };
    }

    public static String resolvePermanentId(
            ThesaurusPreferences preferences,
            String arkId,
            String handleId,
            String doiId
    ) {
        if (preferences == null) {
            return StringUtils.defaultString(arkId);
        }
        return switch (preferences.exportUriType()) {
            case HANDLE -> StringUtils.defaultString(handleId);
            case DOI -> StringUtils.defaultString(doiId);
            default -> StringUtils.defaultString(arkId);
        };
    }

    private static String buildArkUri(
            String originalUri,
            String sitePath,
            String conceptId,
            String thesaurusId,
            String arkId
    ) {
        if (StringUtils.isNotBlank(arkId)) {
            return originalUri + "/" + arkId;
        }
        return sitePath + "?idc=" + conceptId + "&idt=" + thesaurusId;
    }

    private static String buildHandleUri(String sitePath, String conceptId, String thesaurusId, String handleId) {
        if (StringUtils.isNotBlank(handleId)) {
            return "https://hdl.handle.net/" + handleId;
        }
        return sitePath + "?idc=" + conceptId + "&idt=" + thesaurusId;
    }

    private static String buildDoiUri(String sitePath, String conceptId, String thesaurusId, String doiId) {
        if (StringUtils.isNotBlank(doiId)) {
            return "https://doi.org/" + doiId;
        }
        return sitePath + "?idc=" + conceptId + "&idt=" + thesaurusId;
    }

    private static String buildDefaultUri(String originalUri, String sitePath, String conceptId, String thesaurusId) {
        if (StringUtils.isNotBlank(originalUri)) {
            return originalUri + "/?idc=" + conceptId + "&idt=" + thesaurusId;
        }
        return sitePath + "?idc=" + conceptId + "&idt=" + thesaurusId;
    }

    public static String buildGroupUri(
            ThesaurusPreferences preferences,
            String applicationBaseUrl,
            String groupId,
            String thesaurusId,
            String groupArkId,
            String groupHandleId
    ) {
        String sitePath = normalizePath(StringUtils.defaultIfBlank(
                preferences != null ? preferences.cheminSite() : "",
                applicationBaseUrl
        ));
        ExportUriType exportType = preferences != null ? preferences.exportUriType() : ExportUriType.URI;
        String originalUri = preferences != null ? StringUtils.defaultString(preferences.originalUri()) : "";

        if (exportType == ExportUriType.ARK && StringUtils.isNotBlank(groupArkId)) {
            return originalUri + "/" + groupArkId;
        }
        if (exportType == ExportUriType.ARK) {
            return sitePath + "?idg=" + groupId + "&idt=" + thesaurusId;
        }
        if (StringUtils.isNotBlank(groupHandleId)) {
            return "https://hdl.handle.net/" + groupHandleId;
        }
        if (StringUtils.isNotBlank(originalUri)) {
            return originalUri + "/?idg=" + groupId + "&idt=" + thesaurusId;
        }
        return sitePath + "?idc=" + groupId + "&idt=" + thesaurusId;
    }

    private static String normalizePath(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        return path.endsWith("/") ? path : path + "/";
    }
}
