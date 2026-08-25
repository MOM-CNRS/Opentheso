package fr.cnrs.opentheso.v2.toolbox.actions.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotCompareCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotConceptCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotDeprecateCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportPanelState;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.service.ActionsLotConceptService;
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
@Named("v2ActionsLotConceptBean")
@RequiredArgsConstructor
public class ActionsLotConceptBean implements Serializable {

    private final ActionsLotConceptService conceptService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotImportPanelState<ActionsLotConceptCandidate> addPanel = new ActionsLotImportPanelState<>();
    private ActionsLotImportPanelState<ActionsLotConceptCandidate> mergePanel = new ActionsLotImportPanelState<>();
    private ActionsLotImportPanelState<ActionsLotDeprecateCandidate> deprecatePanel = new ActionsLotImportPanelState<>();
    private ActionsLotImportPanelState<ActionsLotCompareCandidate> comparePanel = new ActionsLotImportPanelState<>();

    private Part addUpload;
    private Part mergeUpload;
    private Part deprecateUpload;
    private Part compareUpload;

    @PostConstruct
    public void init() {
        prepare();
    }

    public void prepare() {
        addPanel = new ActionsLotImportPanelState<>();
        mergePanel = new ActionsLotImportPanelState<>();
        deprecatePanel = new ActionsLotImportPanelState<>();
        comparePanel = new ActionsLotImportPanelState<>();
        addUpload = mergeUpload = deprecateUpload = compareUpload = null;
    }

    public boolean isAvailable() {
        return toolboxAccessPolicy.canAccessWorkshop(userSession)
                && toolboxAccessPolicy.hasSelectedThesaurus(thesaurusContext.resolveThesaurusId());
    }

    public String getThesaurusTitle() {
        return StringUtils.defaultIfBlank(thesaurusContext.getCurrentThesaurusTitle(), "thésaurus courant");
    }

    public void onAddFileSelected() {
        acceptFile(addUpload, addPanel, "concepts", "import", "Fichier chargé — validez-le avant d'importer");
        addUpload = null;
    }

    public void onMergeFileSelected() {
        acceptFile(mergeUpload, mergePanel, "concepts", "replace", "Fichier chargé — validez-le avant d'importer");
        mergeUpload = null;
    }

    public void onDeprecateFileSelected() {
        acceptFile(deprecateUpload, deprecatePanel, "concepts", "deprecate", "Fichier chargé — validez-le avant d'importer");
        deprecateUpload = null;
    }

    public void onCompareFileSelected() {
        acceptFile(compareUpload, comparePanel, "concepts", "compare", "Fichier chargé — validez-le avant de comparer");
        compareUpload = null;
    }

    public void clearAdd() {
        addPanel.resetFile();
        addUpload = null;
        syncPanel("import", addPanel);
        toast("Import annulé");
    }

    public void clearMerge() {
        mergePanel.resetFile();
        mergeUpload = null;
        syncPanel("replace", mergePanel);
        toast("Import annulé");
    }

    public void clearDeprecate() {
        deprecatePanel.resetFile();
        deprecateUpload = null;
        syncPanel("deprecate", deprecatePanel);
        toast("Import annulé");
    }

    public void clearCompare() {
        comparePanel.resetFile();
        compareUpload = null;
        syncPanel("compare", comparePanel);
        toast("Comparaison annulée");
    }

    public void validateAdd() {
        if (!guardFile(addPanel, "import")) {
            return;
        }
        addPanel.setBusy(true);
        try {
            var result = conceptService.validateAdd(
                    addPanel.getFileBytes(),
                    addPanel.getChoiceDelimiter(),
                    addPanel.getIdentifierType(),
                    requireThesaurusId()
            );
            finishValidation(addPanel, "import", result, "concept(s) prêt(s) à importer");
        } finally {
            addPanel.setBusy(false);
            syncPanel("import", addPanel);
        }
    }

    public void applyAdd() {
        Integer userId = requireUserId();
        if (userId == null || addPanel.getValidCandidates().isEmpty()) {
            if (addPanel.getValidCandidates().isEmpty()) {
                MessageUtils.showErrorMessage("Aucune ligne valide à importer.");
            }
            return;
        }
        addPanel.setBusy(true);
        try {
            finishApply(addPanel, "import", conceptService.applyAdd(
                    addPanel.getValidCandidates(),
                    addPanel.getFileBytes(),
                    addPanel.getChoiceDelimiter(),
                    addPanel.getIdentifierType(),
                    requireThesaurusId(),
                    userId
            ));
        } finally {
            addPanel.setBusy(false);
            syncPanel("import", addPanel);
        }
    }

    public void validateMerge() {
        if (!guardFile(mergePanel, "replace")) {
            return;
        }
        mergePanel.setBusy(true);
        try {
            var result = conceptService.validateMerge(
                    mergePanel.getFileBytes(),
                    mergePanel.getChoiceDelimiter(),
                    requireThesaurusId()
            );
            finishValidation(mergePanel, "replace", result, "concept(s) prêt(s) à remplacer");
        } finally {
            mergePanel.setBusy(false);
            syncPanel("replace", mergePanel);
        }
    }

    public void applyMerge() {
        Integer userId = requireUserId();
        if (userId == null || mergePanel.getValidCandidates().isEmpty()) {
            if (mergePanel.getValidCandidates().isEmpty()) {
                MessageUtils.showErrorMessage("Aucune ligne valide à importer.");
            }
            return;
        }
        mergePanel.setBusy(true);
        try {
            finishApply(mergePanel, "replace", conceptService.applyMerge(
                    mergePanel.getValidCandidates(),
                    mergePanel.getFileBytes(),
                    mergePanel.getChoiceDelimiter(),
                    requireThesaurusId(),
                    userId
            ));
        } finally {
            mergePanel.setBusy(false);
            syncPanel("replace", mergePanel);
        }
    }

    public void validateDeprecate() {
        if (!guardFile(deprecatePanel, "deprecate")) {
            return;
        }
        deprecatePanel.setBusy(true);
        try {
            var result = conceptService.validateDeprecate(
                    deprecatePanel.getFileBytes(),
                    deprecatePanel.getChoiceDelimiter(),
                    deprecatePanel.getIdentifierType(),
                    requireThesaurusId()
            );
            finishValidation(deprecatePanel, "deprecate", result, "concept(s) prêt(s) à rendre obsolète(s)");
        } finally {
            deprecatePanel.setBusy(false);
            syncPanel("deprecate", deprecatePanel);
        }
    }

    public void applyDeprecate() {
        Integer userId = requireUserId();
        if (userId == null || deprecatePanel.getValidCandidates().isEmpty()) {
            if (deprecatePanel.getValidCandidates().isEmpty()) {
                MessageUtils.showErrorMessage("Aucune ligne valide à importer.");
            }
            return;
        }
        deprecatePanel.setBusy(true);
        try {
            finishApply(deprecatePanel, "deprecate", conceptService.applyDeprecate(
                    deprecatePanel.getValidCandidates(),
                    requireThesaurusId(),
                    userId
            ));
        } finally {
            deprecatePanel.setBusy(false);
            syncPanel("deprecate", deprecatePanel);
        }
    }

    public void validateCompare() {
        if (!guardFile(comparePanel, "compare")) {
            return;
        }
        comparePanel.setBusy(true);
        try {
            ActionsLotImportValidationResult<ActionsLotCompareCandidate> result = conceptService.validateCompare(
                    comparePanel.getFileBytes(),
                    comparePanel.getChoiceDelimiter()
            );
            finishValidation(comparePanel, "compare", result, "libellé(s) prêt(s) à comparer");
        } finally {
            comparePanel.setBusy(false);
            syncPanel("compare", comparePanel);
        }
    }

    public void downloadCompareResult() {
        if (!guardAccess() || comparePanel.getValidCandidates().isEmpty()) {
            MessageUtils.showErrorMessage("Validez d'abord un fichier.");
            return;
        }
        byte[] csv = conceptService.compareToCsv(
                comparePanel.getValidCandidates(),
                requireThesaurusId(),
                comparePanel.getDetectedLang(),
                comparePanel.getSearchType()
        );
        if (csv == null || csv.length == 0) {
            MessageUtils.showErrorMessage("Comparaison impossible.");
            return;
        }
        writeDownload("comparaison-theso.csv", csv);
    }

    public void downloadAddTemplate() {
        writeDownload("modele-concepts-ajout.csv", conceptService.addTemplateBytes());
    }

    public void downloadMergeTemplate() {
        writeDownload("modele-concepts-remplacer.csv", conceptService.mergeTemplateBytes());
    }

    public void downloadDeprecateTemplate() {
        writeDownload("modele-concepts-obsolete.csv", conceptService.deprecateTemplateBytes());
    }

    public void downloadCompareTemplate() {
        writeDownload("modele-concepts-comparer.csv", conceptService.compareTemplateBytes());
    }

    private <C> void acceptFile(
            Part part,
            ActionsLotImportPanelState<C> panel,
            String obj,
            String op,
            String okMessage
    ) {
        try {
            byte[] bytes = readPart(part);
            if (bytes == null) {
                panel.setGlobalError("Impossible de lire le fichier.");
                return;
            }
            panel.acceptFile(fileNameOf(part), bytes);
            toast(okMessage);
        } catch (Exception ex) {
            panel.setGlobalError(ex.getMessage());
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Upload impossible"));
        } finally {
            syncPanel(op, panel);
        }
    }

    private <C> boolean guardFile(ActionsLotImportPanelState<C> panel, String op) {
        if (!guardAccess()) {
            return false;
        }
        if (!panel.isHasFile() || panel.getFileBytes() == null) {
            panel.setGlobalError("Déposez un fichier CSV avant de valider.");
            syncPanel(op, panel);
            return false;
        }
        return true;
    }

    private <C> void finishValidation(
            ActionsLotImportPanelState<C> panel,
            String op,
            ActionsLotImportValidationResult<C> result,
            String okLabel
    ) {
        panel.applyValidation(result);
        if (!result.success()) {
            MessageUtils.showErrorMessage(result.errorMessage());
            toast(result.errorMessage(), true);
        } else if (result.hasErrors()) {
            toast(result.errorCount() + " ligne(s) en erreur — " + result.validCount() + " valides", true);
        } else {
            toast(result.validCount() + " " + okLabel);
        }
    }

    private <C> void finishApply(ActionsLotImportPanelState<C> panel, String op, ActionsLotApplyResult result) {
        panel.applyResult(result);
        if (result.success()) {
            MessageUtils.showInformationMessage(result.message());
            toast(result.message());
        } else {
            MessageUtils.showErrorMessage(result.message());
            toast(result.message(), true);
        }
    }

    private Integer requireUserId() {
        if (!guardAccess()) {
            return null;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Utilisateur invalide");
        }
        return userId;
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

    private void syncPanel(String op, ActionsLotImportPanelState<?> panel) {
        String safe = StringUtils.defaultString(panel.getCssClasses())
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        PrimeFaces.current().executeScript(
                "window.boSyncPanel && window.boSyncPanel('concepts','" + op + "','" + safe + "')"
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
