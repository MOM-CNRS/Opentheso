package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.group.NodeGroupLabel;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public final class ThesaurusSkosUriSupport {

    private ThesaurusSkosUriSupport() {
    }

    public static String resolveBaseUrl(Preferences preferences) {
        if (preferences == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(preferences.getCheminSite())) {
            return preferences.getCheminSite();
        }
        var facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            var externalContext = facesContext.getExternalContext();
            var request = (HttpServletRequest) externalContext.getRequest();
            return externalContext.getRequestScheme()
                    + "://"
                    + externalContext.getRequestServerName()
                    + ":"
                    + request.getLocalPort()
                    + externalContext.getApplicationContextPath();
        }
        return StringUtils.defaultString(preferences.getOriginalUri());
    }

    public static String uriForThesaurus(String thesaurusId, String originalUri, Preferences preferences) {
        if (preferences.isOriginalUriIsArk()) {
            return preferences.getOriginalUri() + "/" + preferences.getIdNaan() + "/" + thesaurusId;
        }
        if (preferences.isOriginalUriIsHandle()) {
            return "https://hdl.handle.net/" + thesaurusId;
        }
        if (StringUtils.isNotEmpty(originalUri)) {
            return originalUri + "/?idt=" + thesaurusId;
        }
        return originalUri + "/?idt=" + thesaurusId;
    }

    public static String uriFromId(String id, Preferences preferences, String baseUrl) {
        var uriBase = StringUtils.removeEnd(baseUrl, "/");
        if (preferences.isOriginalUriIsArk()) {
            if (StringUtils.isNotEmpty(preferences.getOriginalUri())) {
                return preferences.getOriginalUri() + "/" + preferences.getIdNaan() + "/" + id;
            }
            return uriBase + "/?idt=" + id;
        }
        return uriBase + "/?idt=" + id;
    }

    public static String uriForFacet(String facetId, String thesaurusId, String originalUri) {
        return originalUri + "/?idf=" + facetId + "&idt=" + thesaurusId;
    }

    public static String buildConceptUri(NodeUri nodeUri, String thesaurusId, String conceptId,
                                         String originalUri, Preferences preferences) {
        if (preferences.isOriginalUriIsArk() && StringUtils.isNotEmpty(nodeUri.getIdArk())) {
            return originalUri + '/' + nodeUri.getIdArk();
        }
        if (preferences.isOriginalUriIsHandle() && StringUtils.isNotEmpty(nodeUri.getIdHandle())) {
            return "https://hdl.handle.net/" + nodeUri.getIdHandle();
        }
        if (preferences.isOriginalUriIsDoi() && StringUtils.isNotEmpty(nodeUri.getIdDoi())) {
            return "https://doi.org/" + nodeUri.getIdDoi();
        }
        return originalUri + "/?idc=" + conceptId + "&idt=" + thesaurusId;
    }

    public static String uriFromGroup(NodeGroupLabel group, Preferences preferences, String baseUrl) {
        if (group == null || group.getIdGroup() == null) {
            return "";
        }
        var uriBase = StringUtils.removeEnd(baseUrl, "/");
        if (preferences.isOriginalUriIsArk()) {
            if (StringUtils.isNotBlank(group.getIdArk())) {
                return preferences.getUriArk() + group.getIdArk();
            }
            return uriBase + "/?idg=" + group.getIdGroup() + "&idt=" + group.getIdThesaurus();
        }
        if (preferences.isOriginalUriIsHandle() && StringUtils.isNotBlank(group.getIdHandle())) {
            return "https://hdl.handle.net/" + group.getIdHandle();
        }
        if (preferences.isOriginalUriIsDoi() && StringUtils.isNotBlank(group.getIdDoi())) {
            return "https://doi.org/" + group.getIdDoi();
        }
        if (StringUtils.isNotEmpty(preferences.getOriginalUri())) {
            return preferences.getOriginalUri() + "/?idg=" + group.getIdGroup() + "&idt=" + group.getIdThesaurus();
        }
        return uriBase + "/?idg=" + group.getIdGroup() + "&idt=" + group.getIdThesaurus();
    }

    public static String uriGroupFromNodeUri(NodeUri nodeUri, String thesaurusId, Preferences preferences, String baseUrl) {
        if (nodeUri == null) {
            return "";
        }
        var uriBase = StringUtils.removeEnd(baseUrl, "/");
        if (preferences.isOriginalUriIsArk()) {
            if (StringUtils.isNotEmpty(nodeUri.getIdArk())) {
                return preferences.getOriginalUri() + "/" + nodeUri.getIdArk();
            }
            return uriBase + "/?idg=" + nodeUri.getIdConcept() + "&idt=" + thesaurusId;
        }
        if (StringUtils.isNotBlank(nodeUri.getIdHandle())) {
            return "https://hdl.handle.net/" + nodeUri.getIdHandle();
        }
        if (StringUtils.isNotEmpty(preferences.getOriginalUri())) {
            return preferences.getOriginalUri() + "/?idg=" + nodeUri.getIdConcept() + "&idt=" + thesaurusId;
        }
        return uriBase + "/?idg=" + nodeUri.getIdConcept() + "&idt=" + thesaurusId;
    }

    public static String uriFromNodeUri(NodeUri nodeUri, String thesaurusId, String conceptId,
                                          String originalUri, Preferences preferences, String baseUrl) {
        if (preferences.isOriginalUriIsArk()) {
            if (StringUtils.isNotEmpty(nodeUri.getIdArk())) {
                return preferences.getOriginalUri() + "/" + nodeUri.getIdArk();
            }
            var uriBase = StringUtils.removeEnd(baseUrl, "/");
            return uriBase + "/?idc=" + conceptId + "&idt=" + thesaurusId;
        }
        if (preferences.isOriginalUriIsHandle() && StringUtils.isNotBlank(nodeUri.getIdHandle())) {
            return "https://hdl.handle.net/" + nodeUri.getIdHandle();
        }
        if (preferences.isOriginalUriIsDoi() && StringUtils.isNotBlank(nodeUri.getIdDoi())) {
            return "https://doi.org/" + nodeUri.getIdDoi();
        }
        if (StringUtils.isNotEmpty(originalUri)) {
            return originalUri + "/?idc=" + conceptId + "&idt=" + thesaurusId;
        }
        var uriBase = StringUtils.removeEnd(baseUrl, "/");
        return uriBase + "/?idc=" + conceptId + "&idt=" + thesaurusId;
    }
}
