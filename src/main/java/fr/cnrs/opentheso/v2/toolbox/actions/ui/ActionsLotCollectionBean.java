package fr.cnrs.opentheso.v2.toolbox.actions.ui;

import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotCollectionCandidate;
import fr.cnrs.opentheso.v2.toolbox.actions.model.ActionsLotImportPanelState;
import fr.cnrs.opentheso.v2.toolbox.actions.service.ActionsLotCollectionService;
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
@Named("v2ActionsLotCollectionBean")
@RequiredArgsConstructor
public class ActionsLotCollectionBean implements Serializable {

    private final transient ActionsLotCollectionService collectionService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotImportPanelState<ActionsLotCollectionCandidate> importPanel = new ActionsLotImportPanelState<>();
    private transient Part importUpload;

    @PostConstruct
    public void init() {
        prepare();
    }

    public void prepare() {
        importPanel = new ActionsLotImportPanelState<>();
        importUpload = null;
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
                () -> collectionService.validate(
                        importPanel.getFileBytes(),
                        importPanel.getChoiceDelimiter(),
                        importPanel.getIdentifierType(),
                        requireThesaurusId()),
                importPanel::applyValidation,
                "rattachement(s) prêt(s) à importer");
    }

    public void applyImport() {
        if (!guardAccess()) {
            return;
        }
        ActionsLotUiSupport.applyFile(importPanel, this::updateImportPanel,
                () -> collectionService.applyImport(
                        importPanel.getValidCandidates(),
                        requireThesaurusId()),
                ActionsLotMessages.NO_VALID_LINE);
    }

    public void downloadImportTemplate() {
        writeDownload("modele-collections.csv", collectionService.templateBytes());
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
                "window.boSyncPanel && window.boSyncPanel('collections','import','" + safe + "')"
        );
    }

    private void toast(String message) {
        ActionsLotUiSupport.toast(message);
    }
}
