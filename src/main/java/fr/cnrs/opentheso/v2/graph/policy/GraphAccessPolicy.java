package fr.cnrs.opentheso.v2.graph.policy;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;

public final class GraphAccessPolicy {

    private GraphAccessPolicy() {
    }

    public static boolean canAccessModule(UserSession userSession) {
        return userSession.isLoggedIn();
    }
}
