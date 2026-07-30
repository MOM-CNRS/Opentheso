package fr.cnrs.opentheso.v2.toolbox.policy;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Façade toolbox : décisions via {@link RightsService}.
 */
@Component
@RequiredArgsConstructor
public class ToolboxAccessPolicy {

    private final RightsService rightsService;

    public boolean canAccessEditionScreen(UserSession userSession) {
        return rightsService.can(userSession, Permission.TOOLBOX_EDITION);
    }

    public boolean canCreateOrImportThesaurus(UserSession userSession) {
        return rightsService.can(userSession, Permission.TOOLBOX_EDITION);
    }

    public boolean canManageLanguageFlags(UserSession userSession) {
        return rightsService.can(userSession, Permission.TOOLBOX_FLAGS);
    }

    public boolean canAccessWorkshop(UserSession userSession) {
        return rightsService.can(userSession, Permission.ACCESS_WORKSHOP);
    }

    public boolean canAccessMaintenance(UserSession userSession) {
        return rightsService.can(userSession, Permission.TOOLBOX_MAINTENANCE);
    }

    public boolean canViewStatistics(UserSession userSession) {
        return rightsService.can(userSession, Permission.TOOLBOX_STATISTICS);
    }

    public boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
