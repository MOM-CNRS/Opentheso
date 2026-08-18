package fr.cnrs.opentheso.v2.shared.session;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Cycle de vie HTTP session V2 : invalidation et redirections d'accueil unifiées
 * (expiration idle, ViewExpired, logout, clear session).
 */
@Service
public class SessionLifecycleService {

    public static final String HOME_PATH = "/v2";
    public static final String EXPIRE_PATH = "/v2/session/expire";
    public static final String PARAM_SESSION_EXPIRED = "sessionExpired";
    public static final String PARAM_LOGOUT = "logout";

    public String homeUrl(String contextPath) {
        return normalizeContextPath(contextPath) + HOME_PATH;
    }

    public String homeUrlWithSessionExpired(String contextPath) {
        return homeUrl(contextPath) + "?" + PARAM_SESSION_EXPIRED + "=1";
    }

    public String homeUrlWithLogout(String contextPath) {
        return homeUrl(contextPath) + "?" + PARAM_LOGOUT + "=1";
    }

    public String expireUrl(String contextPath) {
        return normalizeContextPath(contextPath) + EXPIRE_PATH;
    }

    public void invalidateQuietly(HttpSession session) {
        if (session == null) {
            return;
        }
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // déjà invalidée
        }
    }

    public void invalidateCurrentFacesSessionQuietly() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        ExternalContext externalContext = facesContext.getExternalContext();
        Object session = externalContext.getSession(false);
        if (session instanceof HttpSession httpSession) {
            invalidateQuietly(httpSession);
        } else if (session != null) {
            try {
                externalContext.invalidateSession();
            } catch (IllegalStateException ignored) {
                // déjà invalidée
            }
        }
    }

    /**
     * Invalide la session puis redirige vers l'accueil V2 avec un marqueur d'expiration.
     * Compatible requêtes AJAX JSF/PrimeFaces ({@link ExternalContext#redirect}).
     */
    public void expireAndRedirectFromFaces() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        ExternalContext externalContext = facesContext.getExternalContext();
        String redirectUrl = homeUrlWithSessionExpired(externalContext.getRequestContextPath());
        invalidateCurrentFacesSessionQuietly();
        externalContext.redirect(redirectUrl);
        facesContext.responseComplete();
    }

    public void logoutAndRedirectFromFaces() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        ExternalContext externalContext = facesContext.getExternalContext();
        String redirectUrl = homeUrlWithLogout(externalContext.getRequestContextPath());
        invalidateCurrentFacesSessionQuietly();
        externalContext.redirect(redirectUrl);
        facesContext.responseComplete();
    }

    public void clearAndRedirectFromFaces() throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        ExternalContext externalContext = facesContext.getExternalContext();
        String redirectUrl = homeUrl(externalContext.getRequestContextPath());
        invalidateCurrentFacesSessionQuietly();
        externalContext.redirect(redirectUrl);
        facesContext.responseComplete();
    }

    public void expireAndRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        invalidateQuietly(request.getSession(false));
        response.sendRedirect(homeUrlWithSessionExpired(request.getContextPath()));
    }

    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
    }
}
