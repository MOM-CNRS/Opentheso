package fr.cnrs.opentheso.v2.shared.session;

import jakarta.faces.context.FacesContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return Optional.empty();
        }
        Object value = context.getExternalContext().getSessionMap().get(SESSION_USER_ID_KEY);
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
        if (context == null) {
            return;
        }
        if (userId == null) {
            context.getExternalContext().getSessionMap().remove(SESSION_USER_ID_KEY);
        } else {
            context.getExternalContext().getSessionMap().put(SESSION_USER_ID_KEY, userId);
        }
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
