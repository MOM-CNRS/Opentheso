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
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Paths;

@Getter
@Setter
@ViewScoped
@Named("v2ActionsLotAltLabelBean")
@RequiredArgsConstructor
public class ActionsLotAltLabelBean implements Serializable {

    private final ActionsLotAltLabelService altLabelService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotAltLabelPanelState importPanel = new ActionsLotAltLabelPanelState();
    private ActionsLotAltLabelPanelState deletePanel = new ActionsLotAltLabelPanelState();

    private Part importUpload;
    private Part deleteUpload;

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
        return toolboxAccessPolicy.canAccessWorkshop(userSession)
                && toolboxAccessPolicy.hasSelectedThesaurus(thesaurusContext.resolveThesaurusId());
    }

    public String getThesaurusTitle() {
        return StringUtils.defaultIfBlank(thesaurusContext.getCurrentThesaurusTitle(), "thésaurus courant");
    }

    public void onImportFileSelected() {
        try {
            byte[] bytes = readPart(importUpload);
            if (bytes == null) {
                importPanel.setGlobalError("Impossible de lire le fichier.");
                return;
            }
            importPanel.acceptFile(fileNameOf(importUpload), bytes);
            toast("Fichier chargé — validez-le avant d'importer");
        } catch (Exception ex) {
            importPanel.setGlobalError(ex.getMessage());
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Upload impossible"));
        } finally {
            importUpload = null;
            updateImportPanel();
        }
    }

    public void clearImport() {
        importPanel.resetFile();
        importUpload = null;
        updateImportPanel();
        toast("Import annulé");
    }

    public void validateImport() {
        if (!guardAccess()) {
            return;
        }
        if (!importPanel.isHasFile() || importPanel.getFileBytes() == null) {
            importPanel.setGlobalError("Déposez un fichier CSV avant de valider.");
            updateImportPanel();
            return;
        }
        importPanel.setBusy(true);
        try {
            ActionsLotAltLabelValidationResult result = altLabelService.validate(
                    importPanel.getFileBytes(),
                    importPanel.getChoiceDelimiter(),
                    importPanel.getIdentifierType(),
                    requireThesaurusId(),
                    true
            );
            importPanel.applyValidation(result);
            if (!result.success()) {
                MessageUtils.showErrorMessage(result.errorMessage());
                toast(result.errorMessage(), true);
            } else if (result.hasErrors()) {
                toast(result.errorCount() + " ligne(s) en erreur — " + result.validCount() + " valides", true);
            } else {
                toast(result.validCount() + " synonyme(s) prêt(s) à importer");
            }
        } finally {
            importPanel.setBusy(false);
            updateImportPanel();
        }
    }

    public void applyImport() {
        if (!guardAccess()) {
            return;
        }
        if (importPanel.getValidCandidates().isEmpty()) {
            MessageUtils.showErrorMessage("Aucune ligne valide à importer.");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Utilisateur invalide");
            return;
        }
        importPanel.setBusy(true);
        try {
            ActionsLotApplyResult result = altLabelService.applyImport(
                    importPanel.getValidCandidates(),
                    requireThesaurusId(),
                    userId,
                    importPanel.isClearBefore()
            );
            importPanel.applyResult(result);
            if (result.success()) {
                MessageUtils.showInformationMessage(result.message());
                toast(result.message());
            } else {
                MessageUtils.showErrorMessage(result.message());
                toast(result.message(), true);
            }
        } finally {
            importPanel.setBusy(false);
            updateImportPanel();
        }
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
        if (!toolboxAccessPolicy.canAccessWorkshop(userSession)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return false;
        }
        if (StringUtils.isBlank(requireThesaurusId())) {
            MessageUtils.showErrorMessage("Vous devez choisir un thésaurus avant !");
            return false;
        }
        return true;
    }

    private String requireThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    private byte[] readPart(Part part) throws IOException {
        if (part == null || part.getSize() <= 0) {
            return null;
        }
        return part.getInputStream().readAllBytes();
    }

    private String fileNameOf(Part part) {
        if (part == null || StringUtils.isBlank(part.getSubmittedFileName())) {
            return "fichier.csv";
        }
        return Paths.get(part.getSubmittedFileName()).getFileName().toString();
    }

    private void writeDownload(String filename, byte[] content) {
        FacesContext faces = FacesContext.getCurrentInstance();
        ExternalContext ext = faces.getExternalContext();
        ext.responseReset();
        ext.setResponseContentType("text/csv; charset=UTF-8");
        ext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        ext.setResponseContentLength(content.length);
        try (OutputStream out = ext.getResponseOutputStream()) {
            out.write(content);
            out.flush();
        } catch (IOException ex) {
            MessageUtils.showErrorMessage("Téléchargement impossible : " + ex.getMessage());
        }
        faces.responseComplete();
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
        toast(message, false);
    }

    private void toast(String message, boolean error) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        String safe = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        String opts = error ? "{error:true}" : "{}";
        PrimeFaces.current().executeScript("window.toast && window.toast('" + safe + "', " + opts + ")");
    }
}
