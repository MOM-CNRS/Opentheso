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
@Named("v2ActionsLotConceptBean")
@RequiredArgsConstructor
public class ActionsLotConceptBean implements Serializable {

    private final transient ActionsLotConceptService conceptService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotImportPanelState<ActionsLotConceptCandidate> addPanel = new ActionsLotImportPanelState<>();
    private ActionsLotImportPanelState<ActionsLotConceptCandidate> mergePanel = new ActionsLotImportPanelState<>();
    private ActionsLotImportPanelState<ActionsLotDeprecateCandidate> deprecatePanel = new ActionsLotImportPanelState<>();
    private ActionsLotImportPanelState<ActionsLotCompareCandidate> comparePanel = new ActionsLotImportPanelState<>();

    private transient Part addUpload;
    private transient Part mergeUpload;
    private transient Part deprecateUpload;
    private transient Part compareUpload;

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
        return ActionsLotUiSupport.isAvailable(toolboxAccessPolicy, userSession, thesaurusContext);
    }

    public String getThesaurusTitle() {
        return ActionsLotUiSupport.thesaurusTitle(thesaurusContext);
    }

    public void onAddFileSelected() {
        acceptFile(addUpload, addPanel, ActionsLotMessages.KIND_CONCEPTS, ActionsLotMessages.ACTION_IMPORT, ActionsLotMessages.FILE_LOADED);
        addUpload = null;
    }

    public void onMergeFileSelected() {
        acceptFile(mergeUpload, mergePanel, ActionsLotMessages.KIND_CONCEPTS, ActionsLotMessages.ACTION_REPLACE, ActionsLotMessages.FILE_LOADED);
        mergeUpload = null;
    }

    public void onDeprecateFileSelected() {
        acceptFile(deprecateUpload, deprecatePanel, ActionsLotMessages.KIND_CONCEPTS, ActionsLotMessages.ACTION_DEPRECATE, ActionsLotMessages.FILE_LOADED);
        deprecateUpload = null;
    }

    public void onCompareFileSelected() {
        acceptFile(compareUpload, comparePanel, ActionsLotMessages.KIND_CONCEPTS, ActionsLotMessages.ACTION_COMPARE, "Fichier chargé — validez-le avant de comparer");
        compareUpload = null;
    }

    public void clearAdd() {
        addPanel.resetFile();
        addUpload = null;
        syncPanel(ActionsLotMessages.ACTION_IMPORT, addPanel);
        toast(ActionsLotMessages.IMPORT_CANCELLED);
    }

    public void clearMerge() {
        mergePanel.resetFile();
        mergeUpload = null;
        syncPanel(ActionsLotMessages.ACTION_REPLACE, mergePanel);
        toast(ActionsLotMessages.IMPORT_CANCELLED);
    }

    public void clearDeprecate() {
        deprecatePanel.resetFile();
        deprecateUpload = null;
        syncPanel(ActionsLotMessages.ACTION_DEPRECATE, deprecatePanel);
        toast(ActionsLotMessages.IMPORT_CANCELLED);
    }

    public void clearCompare() {
        comparePanel.resetFile();
        compareUpload = null;
        syncPanel(ActionsLotMessages.ACTION_COMPARE, comparePanel);
        toast("Comparaison annulée");
    }

    public void validateAdd() {
        if (!guardFile(addPanel, ActionsLotMessages.ACTION_IMPORT)) {
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
            finishValidation(addPanel, ActionsLotMessages.ACTION_IMPORT, result, "concept(s) prêt(s) à importer");
        } finally {
            addPanel.setBusy(false);
            syncPanel(ActionsLotMessages.ACTION_IMPORT, addPanel);
        }
    }

    public void applyAdd() {
        Integer userId = requireUserId();
        if (userId == null || addPanel.getValidCandidates().isEmpty()) {
            if (addPanel.getValidCandidates().isEmpty()) {
                MessageUtils.showErrorMessage(ActionsLotMessages.NO_VALID_LINE);
            }
            return;
        }
        addPanel.setBusy(true);
        try {
            finishApply(addPanel, ActionsLotMessages.ACTION_IMPORT, conceptService.applyAdd(
                    addPanel.getValidCandidates(),
                    addPanel.getFileBytes(),
                    addPanel.getChoiceDelimiter(),
                    addPanel.getIdentifierType(),
                    requireThesaurusId(),
                    userId
            ));
        } finally {
            addPanel.setBusy(false);
            syncPanel(ActionsLotMessages.ACTION_IMPORT, addPanel);
        }
    }

    public void validateMerge() {
        if (!guardFile(mergePanel, ActionsLotMessages.ACTION_REPLACE)) {
            return;
        }
        mergePanel.setBusy(true);
        try {
            var result = conceptService.validateMerge(
                    mergePanel.getFileBytes(),
                    mergePanel.getChoiceDelimiter(),
                    requireThesaurusId()
            );
            finishValidation(mergePanel, ActionsLotMessages.ACTION_REPLACE, result, "concept(s) prêt(s) à remplacer");
        } finally {
            mergePanel.setBusy(false);
            syncPanel(ActionsLotMessages.ACTION_REPLACE, mergePanel);
        }
    }

    public void applyMerge() {
        Integer userId = requireUserId();
        if (userId == null || mergePanel.getValidCandidates().isEmpty()) {
            if (mergePanel.getValidCandidates().isEmpty()) {
                MessageUtils.showErrorMessage(ActionsLotMessages.NO_VALID_LINE);
            }
            return;
        }
        mergePanel.setBusy(true);
        try {
            finishApply(mergePanel, ActionsLotMessages.ACTION_REPLACE, conceptService.applyMerge(
                    mergePanel.getValidCandidates(),
                    mergePanel.getFileBytes(),
                    mergePanel.getChoiceDelimiter(),
                    requireThesaurusId(),
                    userId
            ));
        } finally {
            mergePanel.setBusy(false);
            syncPanel(ActionsLotMessages.ACTION_REPLACE, mergePanel);
        }
    }

    public void validateDeprecate() {
        if (!guardFile(deprecatePanel, ActionsLotMessages.ACTION_DEPRECATE)) {
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
            finishValidation(deprecatePanel, ActionsLotMessages.ACTION_DEPRECATE, result, "concept(s) prêt(s) à rendre obsolète(s)");
        } finally {
            deprecatePanel.setBusy(false);
            syncPanel(ActionsLotMessages.ACTION_DEPRECATE, deprecatePanel);
        }
    }

    public void applyDeprecate() {
        Integer userId = requireUserId();
        if (userId == null || deprecatePanel.getValidCandidates().isEmpty()) {
            if (deprecatePanel.getValidCandidates().isEmpty()) {
                MessageUtils.showErrorMessage(ActionsLotMessages.NO_VALID_LINE);
            }
            return;
        }
        deprecatePanel.setBusy(true);
        try {
            finishApply(deprecatePanel, ActionsLotMessages.ACTION_DEPRECATE, conceptService.applyDeprecate(
                    deprecatePanel.getValidCandidates(),
                    requireThesaurusId(),
                    userId
            ));
        } finally {
            deprecatePanel.setBusy(false);
            syncPanel(ActionsLotMessages.ACTION_DEPRECATE, deprecatePanel);
        }
    }

    public void validateCompare() {
        if (!guardFile(comparePanel, ActionsLotMessages.ACTION_COMPARE)) {
            return;
        }
        comparePanel.setBusy(true);
        try {
            ActionsLotImportValidationResult<ActionsLotCompareCandidate> result = conceptService.validateCompare(
                    comparePanel.getFileBytes(),
                    comparePanel.getChoiceDelimiter()
            );
            finishValidation(comparePanel, ActionsLotMessages.ACTION_COMPARE, result, "libellé(s) prêt(s) à comparer");
        } finally {
            comparePanel.setBusy(false);
            syncPanel(ActionsLotMessages.ACTION_COMPARE, comparePanel);
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

    private <C extends java.io.Serializable> void acceptFile(
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

    private <C extends java.io.Serializable> boolean guardFile(ActionsLotImportPanelState<C> panel, String op) {
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

    private <C extends java.io.Serializable> void finishValidation(
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

    private <C extends java.io.Serializable> void finishApply(ActionsLotImportPanelState<C> panel, String op, ActionsLotApplyResult result) {
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

    private void syncPanel(String op, ActionsLotImportPanelState<?> panel) {
        String safe = StringUtils.defaultString(panel.getCssClasses())
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        PrimeFaces.current().executeScript(
                "window.boSyncPanel && window.boSyncPanel('concepts','" + op + "','" + safe + "')"
        );
    }

    private void toast(String message) {
        ActionsLotUiSupport.toast(message);
    }

    private void toast(String message, boolean error) {
        ActionsLotUiSupport.toast(message, error);
    }
}
