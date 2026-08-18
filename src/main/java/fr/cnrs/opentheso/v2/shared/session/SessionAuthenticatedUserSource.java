package fr.cnrs.opentheso.v2.shared.session;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
@Primary
public class SessionAuthenticatedUserSource implements AuthenticatedUserSource {

    public static final String SESSION_USER_ID_KEY = "v2.authenticatedUserId";

    @Override
    public boolean isLoggedIn() {
        return getUserId().isPresent();
    }

    @Override
    public Optional<Integer> getUserId() {
        Object value = sessionAttribute(SESSION_USER_ID_KEY);
        if (value instanceof Integer userId) {
            return Optional.of(userId);
        }
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }
        return Optional.empty();
    }

    public void setUserId(Integer userId) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            if (userId == null) {
                context.getExternalContext().getSessionMap().remove(SESSION_USER_ID_KEY);
            } else {
                context.getExternalContext().getSessionMap().put(SESSION_USER_ID_KEY, userId);
            }
            return;
        }
        HttpSession session = currentHttpSession(true);
        if (session == null) {
            return;
        }
        if (userId == null) {
            session.removeAttribute(SESSION_USER_ID_KEY);
        } else {
            session.setAttribute(SESSION_USER_ID_KEY, userId);
        }
    }

    private static Object sessionAttribute(String key) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            return context.getExternalContext().getSessionMap().get(key);
        }
        HttpSession session = currentHttpSession(false);
        return session == null ? null : session.getAttribute(key);
    }

    private static HttpSession currentHttpSession(boolean create) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(create);
    }

    @Override
    public void refreshDisplayName(String name) {
        // Le profil est relu depuis la base via UserSession après invalidation du cache.
    }

    @Override
    public void refreshEmail(String email) {
        // Le profil est relu depuis la base via UserSession après invalidation du cache.
    }

    @Override
    public void refreshAlertMail(boolean alertMail) {
        // Le profil est relu depuis la base via UserSession après invalidation du cache.
    }
}
