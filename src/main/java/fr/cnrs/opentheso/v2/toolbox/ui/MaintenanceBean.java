package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;
import fr.cnrs.opentheso.v2.toolbox.model.MaintenanceArkEditor;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusMaintenanceService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@ViewScoped
@Named("v2MaintenanceBean")
public class MaintenanceBean implements Serializable {

    private final UserSession userSession;
    private final ToolboxAccessPolicy toolboxAccessPolicy;
    private final ThesaurusContext thesaurusContext;
    private final ThesaurusMaintenanceService thesaurusMaintenanceService;
    private final ThesaurusViewBean thesaurusViewBean;

    private MaintenanceArkEditor arkEditor = new MaintenanceArkEditor();
    private LocalArkSettings localArkSettings = new LocalArkSettings("", "", 0);
    private boolean lastOk = true;
    private Instant topTermLastRun;
    private Instant restructureLastRun;
    private Instant collectionsLastRun;
    private Instant rolesLastRun;
    private Instant arkLastRun;

    public MaintenanceBean(
            UserSession userSession,
            ToolboxAccessPolicy toolboxAccessPolicy,
            ThesaurusContext thesaurusContext,
            ThesaurusMaintenanceService thesaurusMaintenanceService,
            ThesaurusViewBean thesaurusViewBean
    ) {
        this.userSession = userSession;
        this.toolboxAccessPolicy = toolboxAccessPolicy;
        this.thesaurusContext = thesaurusContext;
        this.thesaurusMaintenanceService = thesaurusMaintenanceService;
        this.thesaurusViewBean = thesaurusViewBean;
    }

    @PostConstruct
    public void init() {
        load();
    }

    public boolean isScreenAvailable() {
        return toolboxAccessPolicy.canAccessMaintenance(userSession)
                && toolboxAccessPolicy.hasSelectedThesaurus(thesaurusContext.getCurrentThesaurusId());
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

    public String getTopTermLastRunLabel() {
        return formatLastRun(topTermLastRun);
    }

    public String getRestructureLastRunLabel() {
        return formatLastRun(restructureLastRun);
    }

    public String getCollectionsLastRunLabel() {
        return formatLastRun(collectionsLastRun);
    }

    public String getRolesLastRunLabel() {
        return formatLastRun(rolesLastRun);
    }

    public String getArkLastRunLabel() {
        return formatLastRun(arkLastRun);
    }

    public void correctDisplayTopTerm() {
        if (!ensureAvailable()) {
            return;
        }
        try {
            int count = thesaurusMaintenanceService.correctDisplayTopTerm(thesaurusContext.getCurrentThesaurusId());
            topTermLastRun = Instant.now();
            succeed("Correction réussie, concepts affectés : " + count, true);
        } catch (RuntimeException e) {
            fail(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la correction des faux top termes"));
        }
    }

    public void reorganizeHierarchy() {
        if (!ensureAvailable()) {
            return;
        }
        try {
            thesaurusMaintenanceService.reorganizeHierarchy(thesaurusContext.getCurrentThesaurusId());
            restructureLastRun = Instant.now();
            succeed("Correction réussie !!!", true);
        } catch (RuntimeException e) {
            fail(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la réorganisation de la hiérarchie"));
        }
    }

    public void reorganizeConceptsAndCollections() {
        if (!ensureAvailable()) {
            return;
        }
        if (!isSuperAdmin()) {
            lastOk = false;
            String message = "Action réservée aux super-administrateurs";
            MessageUtils.showWarnMessage(message);
            toast(message, true);
            return;
        }
        try {
            int cleaned = thesaurusMaintenanceService.reorganizeConceptsAndCollections(
                    thesaurusContext.getCurrentThesaurusId());
            collectionsLastRun = Instant.now();
            succeed("Correction réussie !!! Liens collection/concept nettoyés : " + cleaned, true);
        } catch (RuntimeException e) {
            fail(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la réorganisation des concepts et collections"));
        }
    }

    public void switchRolesFromTermToConcept() {
        if (!ensureAvailable()) {
            return;
        }
        try {
            thesaurusMaintenanceService.switchRolesFromTermToConcept(thesaurusContext.getCurrentThesaurusId());
            rolesLastRun = Instant.now();
            succeed("Correction réussie !!!", false);
        } catch (RuntimeException e) {
            fail(StringUtils.defaultIfBlank(e.getMessage(), "Erreur lors du changement de rôles"));
        }
    }

    public void generateArkFromConceptId() {
        if (!ensureAvailable()) {
            return;
        }
        if (StringUtils.isBlank(arkEditor.getNaan())) {
            fail("Le NAAN est obligatoire");
            return;
        }
        try {
            int count = thesaurusMaintenanceService.generateArkFromConceptId(
                    thesaurusContext.getCurrentThesaurusId(),
                    arkEditor.getPrefix(),
                    arkEditor.getNaan(),
                    arkEditor.isOverwrite()
            );
            arkLastRun = Instant.now();
            succeed("Concepts changés: " + count, false);
        } catch (RuntimeException e) {
            fail(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la génération des identifiants ARK"));
        }
    }

    public void generateLocalArk() {
        if (!ensureAvailable()) {
            return;
        }
        try {
            int count = thesaurusMaintenanceService.generateLocalArk(
                    thesaurusContext.getCurrentThesaurusId(),
                    arkEditor.isOverwriteLocalArk()
            );
            arkLastRun = Instant.now();
            succeed("Concepts changés: " + count, false);
        } catch (RuntimeException e) {
            fail(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la génération des identifiants ARK locaux"));
        }
    }

    public void generateSitemap() {
        if (!ensureAvailable()) {
            return;
        }
        try {
            thesaurusMaintenanceService.generateSitemap(thesaurusContext.getCurrentThesaurusId());
            MessageUtils.showInformationMessage("Sitemap généré avec succès");
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage("Erreur lors de la génération du sitemap");
        }
    }

    private boolean ensureAvailable() {
        if (isScreenAvailable()) {
            return true;
        }
        lastOk = false;
        toast("Action réservée aux administrateurs du projet.", true);
        return false;
    }

    private void succeed(String message, boolean reloadTree) {
        lastOk = true;
        if (reloadTree) {
            thesaurusViewBean.reloadTree();
        }
        MessageUtils.showInformationMessage(message);
        toast(message);
    }

    private void fail(String message) {
        lastOk = false;
        MessageUtils.showErrorMessage(message);
        toast(message, true);
    }

    private String formatLastRun(Instant instant) {
        return instant == null ? "jamais" : "à l'instant";
    }

    private void toast(String message) {
        toast(message, false);
    }

    private void toast(String message, boolean error) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        try {
            String safe = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
            String opts = error ? "{error:true}" : "{}";
            PrimeFaces.current().executeScript("window.toast && window.toast('" + safe + "', " + opts + ")");
        } catch (Exception ignored) {
            // Hors contexte JSF (tests unitaires).
        }
    }
}
