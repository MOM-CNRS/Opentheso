package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;
import fr.cnrs.opentheso.v2.toolbox.model.MaintenanceArkEditor;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusMaintenanceService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@ViewScoped
@Named("v2MaintenanceBean")
public class MaintenanceBean implements Serializable {

    private final UserSession userSession;
    private final ThesaurusContext thesaurusContext;
    private final ThesaurusMaintenanceService thesaurusMaintenanceService;

    private MaintenanceArkEditor arkEditor = new MaintenanceArkEditor();
    private LocalArkSettings localArkSettings = new LocalArkSettings("", "", 0);

    public MaintenanceBean(
            UserSession userSession,
            ThesaurusContext thesaurusContext,
            ThesaurusMaintenanceService thesaurusMaintenanceService
    ) {
        this.userSession = userSession;
        this.thesaurusContext = thesaurusContext;
        this.thesaurusMaintenanceService = thesaurusMaintenanceService;
    }

    public boolean isScreenAvailable() {
        return ToolboxAccessPolicy.canAccessMaintenance(userSession)
                && ToolboxAccessPolicy.hasSelectedThesaurus(thesaurusContext.getCurrentThesaurusId());
    }

    public boolean isSuperAdmin() {
        return userSession.isSuperAdmin();
    }

    public String getThesaurusTitle() {
        return thesaurusContext.getCurrentThesaurusTitle();
    }

    public String getThesaurusId() {
        return thesaurusContext.getCurrentThesaurusId();
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        arkEditor = new MaintenanceArkEditor();
        if (!isScreenAvailable()) {
            localArkSettings = new LocalArkSettings("", "", 0);
            return;
        }
        localArkSettings = thesaurusMaintenanceService.loadLocalArkSettings(thesaurusContext.getCurrentThesaurusId());
    }

    public void correctDisplayTopTerm() {
        if (!isScreenAvailable()) {
            return;
        }
        int count = thesaurusMaintenanceService.correctDisplayTopTerm(thesaurusContext.getCurrentThesaurusId());
        MessageUtils.showInformationMessage("Correction réussie, concepts affectés : " + count);
    }

    public void reorganizeHierarchy() {
        if (!isScreenAvailable()) {
            return;
        }
        try {
            thesaurusMaintenanceService.reorganizeHierarchy(thesaurusContext.getCurrentThesaurusId());
            MessageUtils.showInformationMessage("Correction réussie !!!");
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la réorganisation de la hiérarchie"));
        }
    }

    public void reorganizeConceptsAndCollections() {
        if (!isScreenAvailable()) {
            return;
        }
        if (!isSuperAdmin()) {
            MessageUtils.showWarnMessage("Action réservée aux super-administrateurs");
            return;
        }
        try {
            int cleaned = thesaurusMaintenanceService.reorganizeConceptsAndCollections(
                    thesaurusContext.getCurrentThesaurusId());
            MessageUtils.showInformationMessage(
                    "Correction réussie !!! Liens collection/concept nettoyés : " + cleaned);
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la réorganisation des concepts et collections"));
        }
    }

    public void switchRolesFromTermToConcept() {
        if (!isScreenAvailable()) {
            return;
        }
        try {
            thesaurusMaintenanceService.switchRolesFromTermToConcept(thesaurusContext.getCurrentThesaurusId());
            MessageUtils.showInformationMessage("Correction réussie !!!");
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage("Erreur lors du changement de rôles");
        }
    }

    public void generateArkFromConceptId() {
        if (!isScreenAvailable()) {
            return;
        }
        if (StringUtils.isBlank(arkEditor.getNaan())) {
            MessageUtils.showErrorMessage("Le NAAN est obligatoire");
            return;
        }
        int count = thesaurusMaintenanceService.generateArkFromConceptId(
                thesaurusContext.getCurrentThesaurusId(),
                arkEditor.getPrefix(),
                arkEditor.getNaan(),
                arkEditor.isOverwrite()
        );
        MessageUtils.showInformationMessage("Concepts changés: " + count);
    }

    public void generateLocalArk() {
        if (!isScreenAvailable()) {
            return;
        }
        int count = thesaurusMaintenanceService.generateLocalArk(
                thesaurusContext.getCurrentThesaurusId(),
                arkEditor.isOverwriteLocalArk()
        );
        MessageUtils.showInformationMessage("Concepts changés: " + count);
    }

    public void generateSitemap() {
        if (!isScreenAvailable()) {
            return;
        }
        try {
            thesaurusMaintenanceService.generateSitemap(thesaurusContext.getCurrentThesaurusId());
            MessageUtils.showInformationMessage("Sitemap généré avec succès");
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage("Erreur lors de la génération du sitemap");
        }
    }
}
