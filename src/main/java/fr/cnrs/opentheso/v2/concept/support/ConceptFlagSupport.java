package fr.cnrs.opentheso.v2.concept.support;

import jakarta.faces.context.FacesContext;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;

public final class ConceptFlagSupport {

    private static final String NO_FLAG = "/resources/img/flag/noflag.png";

    private ConceptFlagSupport() {
    }

    public static String resolveFlagImageUrl(String codeFlag) {
        var facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return defaultFlagPath(codeFlag);
        }
        var externalContext = facesContext.getExternalContext();
        var contextPath = externalContext.getRequestContextPath();
        if (StringUtils.isBlank(codeFlag)) {
            return contextPath + NO_FLAG;
        }
        String resourcePath = "/resources/img/flag/" + codeFlag.toLowerCase() + ".png";
        try (InputStream inputStream = externalContext.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return contextPath + resourcePath;
            }
        } catch (Exception ignored) {
        }
        return contextPath + NO_FLAG;
    }

    private static String defaultFlagPath(String codeFlag) {
        if (StringUtils.isBlank(codeFlag)) {
            return NO_FLAG;
        }
        return "/resources/img/flag/" + codeFlag.toLowerCase() + ".png";
    }
}
