package fr.cnrs.opentheso.v2.candidat.policy;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Façade candidat : décisions via {@link RightsService}.
 */
@Component
@RequiredArgsConstructor
public class CandidatAccessPolicy {

    private final RightsService rightsService;

    public boolean canAccessModule(UserSession userSession) {
        return rightsService.can(userSession, Permission.ACCESS_CANDIDAT);
    }

    public boolean canAccessModule(UserSession userSession, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return canAccessModule(userSession);
        }
        return rightsService.can(userSession, Permission.ACCESS_CANDIDAT, AuthTarget.thesaurus(thesaurusId));
    }

    public boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
