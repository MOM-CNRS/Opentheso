package fr.cnrs.opentheso.v2.candidat.policy;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.apache.commons.lang3.StringUtils;

public final class CandidatAccessPolicy {

    private CandidatAccessPolicy() {
    }

    public static boolean canAccessModule(UserSession userSession, CurrentUser currentUser) {
        return userSession.isLoggedIn() && currentUser.isHasRoleAsContributor();
    }

    public static boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
