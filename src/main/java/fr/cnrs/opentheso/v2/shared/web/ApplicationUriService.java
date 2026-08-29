package fr.cnrs.opentheso.v2.shared.web;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ApplicationUriService {

    public String resolveApplicationBaseUrl() {
        var facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return "";
        }
        var request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        var protocol = request.isSecure() ? "https://" : "http://";
        return protocol + request.getHeader("host") + request.getContextPath();
    }

    public String resolveApplicationRootUrl() {
        String base = resolveApplicationBaseUrl();
        return base.isEmpty() ? base : base + "/";
    }

    public String resolveUrl(String path) {
        String base = resolveApplicationBaseUrl();
        if (base.isEmpty()) {
            return "";
        }
        if (path == null || path.isBlank()) {
            return base;
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    public String resolveSwaggerUrl() {
        return resolveUrl("/swagger-ui/index.html");
    }

    public String resolveOpenApiUrl() {
        return resolveUrl("/openapi/v1");
    }

    public String resolveGraphQlUrl() {
        return resolveUrl("/graphql");
    }

    public String resolveGraphiqlUrl() {
        return resolveUrl("/graphiql.html");
    }
}
