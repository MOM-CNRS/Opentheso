package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@ViewScoped
@Named("v2WorkshopBean")
@RequiredArgsConstructor
public class WorkshopBean implements Serializable {

    private final UserSession userSession;
    private final ToolboxAccessPolicy toolboxAccessPolicy;
    private final ThesaurusContext thesaurusContext;
    private final WorkshopImportBean workshopImportBean;
    private final ThesaurusAccessService thesaurusAccessService;

    public boolean isScreenAvailable() {
        return toolboxAccessPolicy.canAccessWorkshop(userSession)
                && toolboxAccessPolicy.hasSelectedThesaurus(getThesaurusId());
    }

    public boolean isActionsAvailable() {
        return isScreenAvailable() && isAdminOnCurrentThesaurus();
    }

    public String getThesaurusTitle() {
        return thesaurusContext.getCurrentThesaurusTitle() != null
                ? thesaurusContext.getCurrentThesaurusTitle()
                : getThesaurusId();
    }

    public String getThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        if (!isScreenAvailable()) {
            MessageUtils.showErrorMessage("Vous devez choisir un Thésaurus avant !");
            return;
        }
        prepareBulkActions();
    }

    public void prepareBulkActions() {
        if (!isActionsAvailable()) {
            return;
        }
        workshopImportBean.prepare();
    }

    private boolean isAdminOnCurrentThesaurus() {
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = getThesaurusId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return thesaurusAccessService.canManageThesaurus(
                userId,
                userSession.isSuperAdmin(),
                thesaurusId
        );
    }
}
