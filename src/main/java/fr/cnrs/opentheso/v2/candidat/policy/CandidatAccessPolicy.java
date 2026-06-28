package fr.cnrs.opentheso.v2.candidat.policy;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.apache.commons.lang3.StringUtils;

public final class CandidatAccessPolicy {

    private CandidatAccessPolicy() {
    }

    public static boolean canAccessModule(UserSession userSession) {
        return userSession.isLoggedIn() && userSession.isContributor();
    }

    public static boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
