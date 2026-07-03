package fr.cnrs.opentheso.v2.concept.support;

import jakarta.faces.context.FacesContext;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;

public final class ConceptFlagSupport {

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
            return contextPath + "/resources/img/flag/noflag.png";
        }
        String resourcePath = "/resources/img/flag/" + codeFlag.toLowerCase() + ".png";
        try (InputStream inputStream = externalContext.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return contextPath + resourcePath;
            }
        } catch (Exception ignored) {
        }
        return contextPath + "/resources/img/flag/noflag.png";
    }

    private static String defaultFlagPath(String codeFlag) {
        if (StringUtils.isBlank(codeFlag)) {
            return "/resources/img/flag/noflag.png";
        }
        return "/resources/img/flag/" + codeFlag.toLowerCase() + ".png";
    }
}
