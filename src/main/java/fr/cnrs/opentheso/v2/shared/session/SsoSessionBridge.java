package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import jakarta.faces.context.FacesContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SsoSessionBridge {

    private final SessionAuthenticatedUserSource sessionAuthenticatedUserSource;
    private final UserCommandRepository userCommandRepository;

    public void consumePendingSsoLogin() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        Object ssoUserId = sessionMap.remove("ssoUserId");
        Integer userId = null;
        if (ssoUserId instanceof Integer id) {
            userId = id;
        } else if (ssoUserId instanceof Number number) {
            userId = number.intValue();
        }
        if (userId == null) {
            return;
        }
        sessionAuthenticatedUserSource.setUserId(userId);
        try {
            userCommandRepository.updateLastLogin(userId);
        } catch (RuntimeException ex) {
            log.warn("Connexion SSO ok, mais last_login n'a pas pu être mis à jour pour id={}", userId, ex);
        }
    }

    public String consumePendingThesaurusId() {
        return consumeSessionString("ssoIdt");
    }

    public String consumePendingConceptId() {
        return consumeSessionString("ssoIdc");
    }

    private String consumeSessionString(String key) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return null;
        }
        Object value = context.getExternalContext().getSessionMap().remove(key);
        return value == null ? null : StringUtils.trimToNull(value.toString());
    }
}
