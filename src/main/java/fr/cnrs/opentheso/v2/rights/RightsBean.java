package fr.cnrs.opentheso.v2.rights;

import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Accès JSF au module de droits centralisé.
 * Ex. : {@code #{v2Rights.can('MANAGE_THESAURUS')}}
 */
@SessionScoped
@Named("v2Rights")
@RequiredArgsConstructor
public class RightsBean implements Serializable {

    private final transient RightsService rightsService;
    private final transient UserSession userSession;
    private final transient ThesaurusContext thesaurusContext;

    public boolean can(String permissionName) {
        Permission permission = parse(permissionName);
        if (permission == null) {
            return false;
        }
        return rightsService.can(userSession, permission, currentTarget());
    }

    public boolean canOnThesaurus(String permissionName, String thesaurusId) {
        Permission permission = parse(permissionName);
        if (permission == null) {
            return false;
        }
        return rightsService.can(userSession, permission, AuthTarget.thesaurus(thesaurusId));
    }

    public boolean canOnProject(String permissionName, int projectId) {
        Permission permission = parse(permissionName);
        if (permission == null) {
            return false;
        }
        return rightsService.can(userSession, permission, AuthTarget.project(projectId));
    }

    public void refresh() {
        Integer userId = userSession.getCurrentUserId();
        if (userId != null) {
            rightsService.invalidate(userId);
        }
    }

    private AuthTarget currentTarget() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isNotBlank(thesaurusId)) {
            return AuthTarget.thesaurus(thesaurusId);
        }
        return AuthTarget.none();
    }

    private static Permission parse(String permissionName) {
        if (StringUtils.isBlank(permissionName)) {
            return null;
        }
        try {
            return Permission.valueOf(permissionName.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
