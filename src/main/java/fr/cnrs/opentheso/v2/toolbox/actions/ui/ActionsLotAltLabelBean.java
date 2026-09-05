package fr.cnrs.opentheso.v2.toolbox.actions.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotAltLabelPanelState;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotAltLabelValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.service.ActionsLotAltLabelService;
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

import java.io.IOException;
import java.io.Serializable;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotMessages;

@Getter
@Setter
@ViewScoped
@Named("v2ActionsLotAltLabelBean")
@RequiredArgsConstructor
public class ActionsLotAltLabelBean implements Serializable {

    private final transient ActionsLotAltLabelService altLabelService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotAltLabelPanelState importPanel = new ActionsLotAltLabelPanelState();
    private ActionsLotAltLabelPanelState deletePanel = new ActionsLotAltLabelPanelState();

    private transient Part importUpload;
    private transient Part deleteUpload;

    @PostConstruct
    public void init() {
        prepare();
    }

    public void prepare() {
        importPanel = new ActionsLotAltLabelPanelState();
        deletePanel = new ActionsLotAltLabelPanelState();
        importUpload = null;
        deleteUpload = null;
    }

    public boolean isAvailable() {
        return ActionsLotUiSupport.isAvailable(toolboxAccessPolicy, userSession, thesaurusContext);
    }

    public String getThesaurusTitle() {
        return ActionsLotUiSupport.thesaurusTitle(thesaurusContext);
    }

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
                () -> altLabelService.validate(
                        importPanel.getFileBytes(),
                        importPanel.getChoiceDelimiter(),
                        importPanel.getIdentifierType(),
                        requireThesaurusId(),
                        true),
                importPanel::applyValidation,
                "synonyme(s) prêt(s) à importer");
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
                () -> altLabelService.applyImport(
                        importPanel.getValidCandidates(),
                        requireThesaurusId(),
                        userId,
                        importPanel.isClearBefore()),
                ActionsLotMessages.NO_VALID_LINE);
    }

    public void downloadImportTemplate() {
        writeDownload("modele-formes-alternatives.csv", altLabelService.templateBytes());
    }

    public void onDeleteFileSelected() {
        try {
            byte[] bytes = readPart(deleteUpload);
            if (bytes == null) {
                deletePanel.setGlobalError("Impossible de lire le fichier.");
                return;
            }
            deletePanel.acceptFile(fileNameOf(deleteUpload), bytes);
            toast("Fichier chargé — validez-le avant de supprimer");
        } catch (Exception ex) {
            deletePanel.setGlobalError(ex.getMessage());
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Upload impossible"));
        } finally {
            deleteUpload = null;
            updateDeletePanel();
        }
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
        if (!deletePanel.isHasFile() || deletePanel.getFileBytes() == null) {
            deletePanel.setGlobalError("Déposez un fichier CSV avant de valider.");
            updateDeletePanel();
            return;
        }
        deletePanel.setBusy(true);
        try {
            ActionsLotAltLabelValidationResult result = altLabelService.validate(
                    deletePanel.getFileBytes(),
                    deletePanel.getChoiceDelimiter(),
                    deletePanel.getIdentifierType(),
                    requireThesaurusId(),
                    false
            );
            deletePanel.applyValidation(result);
            if (!result.success()) {
                MessageUtils.showErrorMessage(result.errorMessage());
                toast(result.errorMessage(), true);
            } else if (result.hasErrors()) {
                toast(result.errorCount() + " ligne(s) en erreur — " + result.validCount() + " valides", true);
            } else {
                String msg = result.validCount() + " synonyme(s) prêt(s) à supprimer";
                if (result.ignoredCount() > 0) {
                    msg += " (" + result.ignoredCount() + " ignorée(s))";
                }
                toast(msg);
            }
        } finally {
            deletePanel.setBusy(false);
            updateDeletePanel();
        }
    }

    public void applyDelete() {
        if (!guardAccess()) {
            return;
        }
        if (deletePanel.getValidCandidates().isEmpty()) {
            MessageUtils.showErrorMessage("Aucune ligne valide à supprimer.");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Utilisateur invalide");
            return;
        }
        deletePanel.setBusy(true);
        try {
            ActionsLotApplyResult result = altLabelService.applyDelete(
                    deletePanel.getValidCandidates(),
                    requireThesaurusId(),
                    userId
            );
            deletePanel.applyResult(result);
            if (result.success()) {
                MessageUtils.showInformationMessage(result.message());
                toast(result.message());
            } else {
                MessageUtils.showErrorMessage(result.message());
                toast(result.message(), true);
            }
        } finally {
            deletePanel.setBusy(false);
            updateDeletePanel();
        }
    }

    public void downloadDeleteTemplate() {
        writeDownload("modele-formes-alternatives-suppression.csv", altLabelService.templateBytes());
    }

    private boolean guardAccess() {
        return ActionsLotUiSupport.guardAccess(toolboxAccessPolicy, userSession, thesaurusContext);
    }

    private String requireThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    private byte[] readPart(Part part) throws IOException {
        return ActionsLotUiSupport.readPart(part);
    }

    private String fileNameOf(Part part) {
        return ActionsLotUiSupport.fileNameOf(part);
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
                "window.boSyncPanel && window.boSyncPanel('synonymes','" + op + "','" + safe + "')"
        );
    }

    private void toast(String message) {
        ActionsLotUiSupport.toast(message);
    }

    private void toast(String message, boolean error) {
        ActionsLotUiSupport.toast(message, error);
    }
}
