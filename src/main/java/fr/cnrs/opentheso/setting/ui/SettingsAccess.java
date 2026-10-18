package fr.cnrs.opentheso.setting.ui;

import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Droits et identifiant du thésaurus pour la page Paramètres, mémorisés une fois par vue.
 */
@Named("settingsAccess")
@ViewScoped
@RequiredArgsConstructor
public class SettingsAccess implements Serializable {

    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient RightsService rightsService;

    private Boolean canEdit;
    private Boolean superAdmin;

    public String getThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    public String getTitle() {
        return StringUtils.defaultString(thesaurusContext.getCurrentThesaurusTitle());
    }

    public boolean isLoggedIn() {
        return userSession.isLoggedIn();
    }

    public boolean isCanEdit() {
        if (canEdit != null) {
            return canEdit;
        }
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = getThesaurusId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            canEdit = false;
            return false;
        }
        canEdit = rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
        return canEdit;
    }

    public boolean isSuperAdmin() {
        if (superAdmin != null) {
            return superAdmin;
        }
        superAdmin = userSession.isSuperAdmin();
        return superAdmin;
    }

    public Integer currentUserId() {
        return userSession.getCurrentUserId();
    }
}
