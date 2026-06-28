package fr.cnrs.opentheso.v2.toolbox.policy;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.apache.commons.lang3.StringUtils;

public final class ToolboxAccessPolicy {

    private ToolboxAccessPolicy() {
    }

    public static boolean canAccessEditionScreen(UserSession userSession) {
        if (!userSession.isLoggedIn()) {
            return false;
        }
        return userSession.isSuperAdmin() || userSession.hasRoleAsAdmin();
    }

    public static boolean canCreateOrImportThesaurus(UserSession userSession) {
        if (!userSession.isLoggedIn()) {
            return false;
        }
        return userSession.isSuperAdmin() || userSession.hasRoleAsAdmin();
    }

    public static boolean canManageLanguageFlags(UserSession userSession) {
        return userSession.isLoggedIn() && userSession.isSuperAdmin();
    }

    public static boolean canAccessWorkshop(UserSession userSession) {
        return userSession.isLoggedIn();
    }

    public static boolean canManageWorkshopActions(UserSession userSession) {
        return userSession.isLoggedIn() && userSession.hasRoleAsAdmin();
    }

    public static boolean canAccessMaintenance(UserSession userSession) {
        return userSession.isLoggedIn() && userSession.hasRoleAsAdmin();
    }

    public static boolean canViewStatistics(UserSession userSession) {
        return userSession.isLoggedIn() && userSession.isManager();
    }

    public static boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
