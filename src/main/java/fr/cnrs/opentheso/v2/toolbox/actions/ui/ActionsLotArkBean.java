package fr.cnrs.opentheso.v2.toolbox.actions.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotApplyResult;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotArkCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotArkGenerateState;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportPanelState;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportValidationResult;
import fr.cnrs.opentheso.v2.toolbox.actions.service.ActionsLotArkService;
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
@Named("v2ActionsLotArkBean")
@RequiredArgsConstructor
public class ActionsLotArkBean implements Serializable {

    private final transient ActionsLotArkService arkService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotImportPanelState<ActionsLotArkCandidate> importPanel = new ActionsLotImportPanelState<>();
    private ActionsLotArkGenerateState generateState = new ActionsLotArkGenerateState();
    private transient Part importUpload;

    @PostConstruct
    public void init() {
        prepare();
    }

    public void prepare() {
        importPanel = new ActionsLotImportPanelState<>();
        importUpload = null;
        generateState = new ActionsLotArkGenerateState();
        generateState.setLocalSettings(arkService.loadLocalArkSettings(requireThesaurusId()));
    }

    public boolean isAvailable() {
        return ActionsLotUiSupport.isAvailable(toolboxAccessPolicy, userSession, thesaurusContext);
    }

    public String getThesaurusTitle() {
        return ActionsLotUiSupport.thesaurusTitle(thesaurusContext);
    }

    public boolean isLocalGenerateReady() {
        return generateState.getLocalSettings() != null
                && StringUtils.isNotBlank(generateState.getLocalSettings().getNaan());
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
        if (!importPanel.isHasFile() || importPanel.getFileBytes() == null) {
            importPanel.setGlobalError("Déposez un fichier CSV avant de valider.");
            updateImportPanel();
            return;
        }
        importPanel.setBusy(true);
        try {
            ActionsLotImportValidationResult<ActionsLotArkCandidate> result = arkService.validate(
                    importPanel.getFileBytes(),
                    importPanel.getChoiceDelimiter(),
                    requireThesaurusId()
            );
            importPanel.applyValidation(result);
            if (!result.success()) {
                MessageUtils.showErrorMessage(result.errorMessage());
                toast(result.errorMessage(), true);
            } else if (result.hasErrors()) {
                toast(result.errorCount() + " ligne(s) en erreur — " + result.validCount() + " valides", true);
            } else {
                toast(result.validCount() + " identifiant(s) ARK prêt(s) à importer");
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
            MessageUtils.showErrorMessage(ActionsLotMessages.NO_VALID_LINE);
            return;
        }
        importPanel.setBusy(true);
        try {
            ActionsLotApplyResult result = arkService.applyImport(
                    importPanel.getValidCandidates(),
                    requireThesaurusId(),
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
        writeDownload("modele-identifiants-ark.csv", arkService.templateBytes());
    }

    public void generateFromConceptId() {
        if (!guardAccess()) {
            return;
        }
        generateState.resetResult();
        generateState.setBusy(true);
        try {
            ActionsLotApplyResult result = arkService.generateFromConceptId(
                    requireThesaurusId(),
                    generateState.getPrefix(),
                    generateState.getNaan(),
                    generateState.isOverwrite()
            );
            generateState.applyResult(result);
            if (result.success()) {
                MessageUtils.showInformationMessage(result.message());
                toast(result.message());
            } else {
                MessageUtils.showErrorMessage(result.message());
                toast(result.message(), true);
            }
        } finally {
            generateState.setBusy(false);
            updateGeneratePanel();
        }
    }

    public void generateLocal() {
        if (!guardAccess()) {
            return;
        }
        generateState.resetResult();
        generateState.setBusy(true);
        try {
            ActionsLotApplyResult result = arkService.generateLocal(
                    requireThesaurusId(),
                    generateState.isOverwriteLocal()
            );
            generateState.applyResult(result);
            if (result.success()) {
                MessageUtils.showInformationMessage(result.message());
                toast(result.message());
            } else {
                MessageUtils.showErrorMessage(result.message());
                toast(result.message(), true);
            }
        } finally {
            generateState.setBusy(false);
            updateGeneratePanel();
        }
    }

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
        String safe = StringUtils.defaultString(importPanel.getCssClasses())
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        PrimeFaces.current().executeScript(
                "window.boSyncPanel && window.boSyncPanel('identifiants','import','" + safe + "')"
        );
    }

    private void updateGeneratePanel() {
        String safe = StringUtils.defaultString(generateState.getCssClasses())
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        PrimeFaces.current().executeScript(
                "window.boSyncPanel && window.boSyncPanel('identifiants','generate','" + safe + "')"
        );
    }

    private void toast(String message) {
        ActionsLotUiSupport.toast(message);
    }

    private void toast(String message, boolean error) {
        ActionsLotUiSupport.toast(message, error);
    }
}
