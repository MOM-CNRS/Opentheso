package fr.cnrs.opentheso.toolbox.policy;

import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.shared.ui.UserSession;
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

    /**
     * Maintenance du thésaurus courant : utilisateur connecté, super-administrateur
     * ou administrateur du projet auquel ce thésaurus est rattaché.
     */
    public boolean canAccessMaintenance(UserSession userSession, String thesaurusId) {
        return rightsService.isAdminOfThesaurusProject(userSession, thesaurusId);
    }

    public boolean canViewStatistics(UserSession userSession) {
        return rightsService.can(userSession, Permission.TOOLBOX_STATISTICS);
    }

    public boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
