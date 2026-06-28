package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.bean.importexport.ImportFileBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.toolbox.atelier.AtelierThesBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.WorkshopService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;

import java.io.Serializable;

@Getter
@ViewScoped
@Named("v2WorkshopBean")
public class WorkshopBean implements Serializable {

    private final UserSession userSession;
    private final ThesaurusContext thesaurusContext;
    private final SelectedTheso selectedTheso;
    private final ImportFileBean importFileBean;
    private final AtelierThesBean atelierThesBean;
    private final WorkshopService workshopService;

    public WorkshopBean(
            UserSession userSession,
            ThesaurusContext thesaurusContext,
            SelectedTheso selectedTheso,
            ImportFileBean importFileBean,
            AtelierThesBean atelierThesBean,
            WorkshopService workshopService
    ) {
        this.userSession = userSession;
        this.thesaurusContext = thesaurusContext;
        this.selectedTheso = selectedTheso;
        this.importFileBean = importFileBean;
        this.atelierThesBean = atelierThesBean;
        this.workshopService = workshopService;
    }

    public boolean isScreenAvailable() {
        return ToolboxAccessPolicy.canAccessWorkshop(userSession)
                && ToolboxAccessPolicy.hasSelectedThesaurus(getThesaurusId());
    }

    public boolean isActionsAvailable() {
        return isScreenAvailable() && ToolboxAccessPolicy.canManageWorkshopActions(userSession);
    }

    public String getThesaurusTitle() {
        return workshopService.resolveThesaurusTitle(
                thesaurusContext.getCurrentThesaurusTitle(),
                thesaurusContext.getCurrentThesaurusId(),
                selectedTheso.getThesoName(),
                selectedTheso.getCurrentIdTheso()
        );
    }

    public String getThesaurusId() {
        return workshopService.resolveThesaurusId(
                thesaurusContext.getCurrentThesaurusId(),
                selectedTheso.getCurrentIdTheso()
        );
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        if (!isScreenAvailable()) {
            MessageUtils.showErrorMessage("Vous devez choisir un Thésaurus avant !");
            return;
        }
        atelierThesBean.init();
        prepareBulkActions();
    }

    public void prepareBulkActions() {
        if (!isActionsAvailable()) {
            return;
        }
        importFileBean.init();
    }
}
