package fr.cnrs.opentheso.v2.toolbox.policy;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public final class ToolboxAccessPolicy {

    private ToolboxAccessPolicy() {
    }

    public static boolean canAccessEditionScreen(UserSession userSession, CurrentUser currentUser) {
        if (!userSession.isLoggedIn()) {
            return false;
        }
        if (userSession.isSuperAdmin() || userSession.hasRoleAsAdmin()) {
            return true;
        }
        return CollectionUtils.isNotEmpty(currentUser.getAllAuthorizedProjectAsAdmin());
    }

    public static boolean canCreateOrImportThesaurus(UserSession userSession, CurrentUser currentUser) {
        if (!userSession.isLoggedIn()) {
            return false;
        }
        if (userSession.isSuperAdmin()) {
            return true;
        }
        return CollectionUtils.isNotEmpty(currentUser.getAllAuthorizedProjectAsAdmin());
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

    public static boolean canViewStatistics(UserSession userSession, CurrentUser currentUser) {
        return userSession.isLoggedIn() && currentUser.isHasRoleAsManager();
    }

    public static boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
