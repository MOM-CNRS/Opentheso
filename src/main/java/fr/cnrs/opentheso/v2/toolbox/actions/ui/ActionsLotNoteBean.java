package fr.cnrs.opentheso.v2.toolbox.actions.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNotePanelState;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotNoteValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.service.ActionsLotNoteService;
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
@Named("v2ActionsLotNoteBean")
@RequiredArgsConstructor
public class ActionsLotNoteBean implements Serializable {

    private final ActionsLotNoteService noteService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotNotePanelState importPanel = new ActionsLotNotePanelState();
    private Part importUpload;

    @PostConstruct
    public void init() {
        prepare();
    }

    public void prepare() {
        importPanel = new ActionsLotNotePanelState();
        importUpload = null;
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
            ActionsLotNoteValidationResult result = noteService.validate(
                    importPanel.getFileBytes(),
                    importPanel.getChoiceDelimiter(),
                    importPanel.getIdentifierType(),
                    requireThesaurusId()
            );
            importPanel.applyValidation(result);
            if (!result.success()) {
                MessageUtils.showErrorMessage(result.errorMessage());
                toast(result.errorMessage(), true);
            } else if (result.hasErrors()) {
                toast(result.errorCount() + " ligne(s) en erreur — " + result.validCount() + " valides", true);
            } else {
                toast(result.validCount() + " note(s) prête(s) à importer");
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
            ActionsLotApplyResult result = noteService.applyImport(
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
        writeDownload("modele-notes.csv", noteService.templateBytes());
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
        String safe = StringUtils.defaultString(importPanel.getCssClasses())
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        PrimeFaces.current().executeScript(
                "window.boSyncPanel && window.boSyncPanel('notes','import','" + safe + "')"
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
