package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.time.RelativeTimeFormat;
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
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Getter
@Setter
@ViewScoped
@Named("v2MaintenanceBean")
public class MaintenanceBean implements Serializable {

    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ThesaurusMaintenanceService thesaurusMaintenanceService;
    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient MaintenanceLastRunStore lastRunStore;

    private MaintenanceArkEditor arkEditor = new MaintenanceArkEditor();
    private LocalArkSettings localArkSettings = new LocalArkSettings("", "", 0);
    private boolean localArkSettingsLoaded;
    private boolean lastOk = true;

    public MaintenanceBean(
            UserSession userSession,
            ToolboxAccessPolicy toolboxAccessPolicy,
            ThesaurusContext thesaurusContext,
            ThesaurusMaintenanceService thesaurusMaintenanceService,
            ThesaurusViewBean thesaurusViewBean,
            MaintenanceLastRunStore lastRunStore
    ) {
        this.userSession = userSession;
        this.toolboxAccessPolicy = toolboxAccessPolicy;
        this.thesaurusContext = thesaurusContext;
        this.thesaurusMaintenanceService = thesaurusMaintenanceService;
        this.thesaurusViewBean = thesaurusViewBean;
        this.lastRunStore = lastRunStore;
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
        localArkSettingsLoaded = false;
        ensureLocalArkSettings();
    }

    public LocalArkSettings getLocalArkSettings() {
        ensureLocalArkSettings();
        return localArkSettings;
    }

    private void ensureLocalArkSettings() {
        if (localArkSettingsLoaded) {
            return;
        }
        localArkSettingsLoaded = true;
        if (!isScreenAvailable()) {
            localArkSettings = new LocalArkSettings("", "", 0);
            return;
        }
        localArkSettings = thesaurusMaintenanceService.loadLocalArkSettings(thesaurusContext.getCurrentThesaurusId());
    }

    public String getTopTermLastRunLabel() {
        return formatLastRun(MaintenanceLastRunStore.TOP_TERM);
    }

    public String getRestructureLastRunLabel() {
        return formatLastRun(MaintenanceLastRunStore.RESTRUCTURE);
    }

    public String getCollectionsLastRunLabel() {
        return formatLastRun(MaintenanceLastRunStore.COLLECTIONS);
    }

    public String getRolesLastRunLabel() {
        return formatLastRun(MaintenanceLastRunStore.ROLES);
    }

    public String getArkLastRunLabel() {
        return formatLastRun(MaintenanceLastRunStore.ARK);
    }

    public String getSitemapLastRunLabel() {
        return formatLastRun(MaintenanceLastRunStore.SITEMAP);
    }

    public void correctDisplayTopTerm() {
        if (!ensureAvailable()) {
            return;
        }
        try {
            int count = thesaurusMaintenanceService.correctDisplayTopTerm(thesaurusContext.getCurrentThesaurusId());
            markRun(MaintenanceLastRunStore.TOP_TERM);
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
            markRun(MaintenanceLastRunStore.RESTRUCTURE);
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
            markRun(MaintenanceLastRunStore.COLLECTIONS);
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
            markRun(MaintenanceLastRunStore.ROLES);
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
            markRun(MaintenanceLastRunStore.ARK);
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
            markRun(MaintenanceLastRunStore.ARK);
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
            String thesaurusId = thesaurusContext.getCurrentThesaurusId();
            String xml = thesaurusMaintenanceService.buildSitemapXml(thesaurusId);
            if (StringUtils.isBlank(xml)) {
                fail("Le sitemap n'a pas pu être généré.");
                return;
            }
            lastRunStore.putPendingSitemap(thesaurusId + ".xml", xml.getBytes(StandardCharsets.UTF_8));
            markRun(MaintenanceLastRunStore.SITEMAP);
            succeed("Sitemap généré. Téléchargement…", false);
            executeScript("window.downloadMaintSitemap && window.downloadMaintSitemap()");
        } catch (RuntimeException e) {
            fail(StringUtils.defaultIfBlank(e.getMessage(),
                    "Erreur lors de la génération du sitemap"));
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

    private void markRun(String action) {
        lastRunStore.mark(thesaurusContext.getCurrentThesaurusId(), action);
    }

    private String formatLastRun(String action) {
        Instant instant = lastRunStore.get(thesaurusContext.getCurrentThesaurusId(), action);
        return RelativeTimeFormat.lastRun(instant);
    }

    private void toast(String message) {
        toast(message, false);
    }

    private void toast(String message, boolean error) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        String safe = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        String opts = error ? "{error:true}" : "{}";
        executeScript("window.toast && window.toast('" + safe + "', " + opts + ")");
    }

    private void executeScript(String script) {
        try {
            PrimeFaces.current().executeScript(script);
        } catch (Exception ignored) {
            // Hors contexte JSF (tests unitaires).
        }
    }
}
