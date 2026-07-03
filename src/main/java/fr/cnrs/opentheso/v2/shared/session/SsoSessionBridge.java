package fr.cnrs.opentheso.v2.shared.session;

import jakarta.faces.context.FacesContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SsoSessionBridge {

    private final SessionAuthenticatedUserSource sessionAuthenticatedUserSource;

    public void consumePendingSsoLogin() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        Object ssoUserId = sessionMap.remove("ssoUserId");
        if (ssoUserId instanceof Integer userId) {
            sessionAuthenticatedUserSource.setUserId(userId);
        } else if (ssoUserId instanceof Number number) {
            sessionAuthenticatedUserSource.setUserId(number.intValue());
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
