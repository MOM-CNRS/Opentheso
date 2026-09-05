package fr.cnrs.opentheso.v2.toolbox.actions.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotPanelState;
import fr.cnrs.opentheso.v2.toolbox.actions.service.ActionsLotAlignmentService;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotMessages;

@Getter
@Setter
@ViewScoped
@Named("v2ActionsLotAlignmentBean")
@RequiredArgsConstructor
public class ActionsLotAlignmentBean implements Serializable {

    private final transient ActionsLotAlignmentService alignmentService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotPanelState importPanel = new ActionsLotPanelState();
    private ActionsLotPanelState deletePanel = new ActionsLotPanelState();

    private transient Part importUpload;
    private transient Part deleteUpload;

    private String exportScopeConceptId = "";
    private String exportSource = "";
    private String exportError;
    private boolean exportBusy;

    @PostConstruct
    public void init() {
        prepare();
    }

    public void prepare() {
        importPanel = new ActionsLotPanelState();
        deletePanel = new ActionsLotPanelState();
        exportScopeConceptId = "";
        exportSource = "";
        exportError = null;
        exportBusy = false;
        importUpload = null;
        deleteUpload = null;
    }

    public boolean isAvailable() {
        return ActionsLotUiSupport.isAvailable(toolboxAccessPolicy, userSession, thesaurusContext);
    }

    public String getThesaurusTitle() {
        return ActionsLotUiSupport.thesaurusTitle(thesaurusContext);
    }

    /* ── Import ── */

    public void onImportFileSelected() {
        ActionsLotUiSupport.loadFile(importUpload, importPanel, this::updateImportPanel,
                ActionsLotMessages.FILE_LOADED);
        importUpload = null;
    }

    public void clearImport() {
        importPanel.resetFile();
        importUpload = null;
        updateImportPanel();
        toast(ActionsLotMessages.IMPORT_CANCELLED);
    }

    public void validateImport() {
        if (!guardAccess()) {
            return;
        }
        ActionsLotUiSupport.validateFile(importPanel, this::updateImportPanel,
                () -> alignmentService.validateImport(
                        importPanel.getFileBytes(),
                        importPanel.getChoiceDelimiter(),
                        importPanel.getIdentifierType(),
                        requireThesaurusId()),
                importPanel::applyValidation,
                "ligne(s) prêtes à importer");
    }

    public void applyImport() {
        if (!guardAccess()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Utilisateur invalide");
            return;
        }
        ActionsLotUiSupport.applyFile(importPanel, this::updateImportPanel,
                () -> alignmentService.applyImport(
                        importPanel.getValidCandidates(),
                        requireThesaurusId(),
                        userId),
                ActionsLotMessages.NO_VALID_LINE);
    }

    public void downloadImportTemplate() {
        writeDownload("modele-alignements.csv", alignmentService.importTemplateBytes());
    }

    /* ── Delete ── */

    public void onDeleteFileSelected() {
        ActionsLotUiSupport.loadFile(deleteUpload, deletePanel, this::updateDeletePanel,
                "Fichier chargé — validez-le avant de supprimer");
        deleteUpload = null;
    }

    public void clearDelete() {
        deletePanel.resetFile();
        deleteUpload = null;
        updateDeletePanel();
        toast("Suppression annulée");
    }

    public void validateDelete() {
        if (!guardAccess()) {
            return;
        }
        ActionsLotUiSupport.validateFile(deletePanel, this::updateDeletePanel,
                () -> alignmentService.validateDelete(
                        deletePanel.getFileBytes(),
                        deletePanel.getChoiceDelimiter(),
                        deletePanel.getIdentifierType(),
                        requireThesaurusId()),
                deletePanel::applyValidation,
                "ligne(s) prêtes à supprimer");
    }

    public void applyDelete() {
        if (!guardAccess()) {
            return;
        }
        ActionsLotUiSupport.applyFile(deletePanel, this::updateDeletePanel,
                () -> alignmentService.applyDelete(
                        deletePanel.getValidCandidates(),
                        requireThesaurusId()),
                "Aucune ligne valide à supprimer.");
    }

    public void downloadDeleteTemplate() {
        writeDownload("modele-alignements-suppression.csv", alignmentService.deleteTemplateBytes());
    }

    /* ── Export ── */

    public void exportAlignments() {
        if (!guardAccess()) {
            return;
        }
        exportBusy = true;
        exportError = null;
        try {
            byte[] csv = alignmentService.exportAlignments(
                    requireThesaurusId(),
                    exportSource,
                    exportScopeConceptId
            );
            String safeSource = StringUtils.defaultIfBlank(exportSource, "alignements")
                    .replaceAll("[^a-zA-Z0-9_-]+", "-");
            writeDownload("alignements-" + safeSource + ".csv", csv);
            toast("Export généré (" + csv.length + " octets)");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            exportError = ex.getMessage();
            MessageUtils.showErrorMessage(ex.getMessage());
            toast(ex.getMessage(), true);
        } catch (Exception ex) {
            exportError = "Export impossible : " + ex.getMessage();
            MessageUtils.showErrorMessage(exportError);
            toast(exportError, true);
        } finally {
            exportBusy = false;
        }
    }

    /* ── helpers ── */

    private boolean guardAccess() {
        return ActionsLotUiSupport.guardAccess(toolboxAccessPolicy, userSession, thesaurusContext);
    }

    private String requireThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }


    private void writeDownload(String filename, byte[] content) {
        ActionsLotUiSupport.writeDownload(filename, content);
    }

    private void updateImportPanel() {
        syncPanelClasses("import", importPanel.getCssClasses());
    }

    private void updateDeletePanel() {
        syncPanelClasses("delete", deletePanel.getCssClasses());
    }

    private void syncPanelClasses(String op, String cssClasses) {
        String safe = StringUtils.defaultString(cssClasses)
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        PrimeFaces.current().executeScript(
                "window.boSyncPanel && window.boSyncPanel('alignements','" + op + "','" + safe + "')"
        );
    }

    private void toast(String message) {
        ActionsLotUiSupport.toast(message);
    }

    private void toast(String message, boolean error) {
        ActionsLotUiSupport.toast(message, error);
    }
}
