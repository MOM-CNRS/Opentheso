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
}
