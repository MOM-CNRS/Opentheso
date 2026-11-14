package fr.cnrs.opentheso.toolbox.actions.ui;

import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotImportPanelState;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotMessages;
import fr.cnrs.opentheso.toolbox.actions.model.ActionsLotRelationCandidate;
import fr.cnrs.opentheso.toolbox.actions.service.ActionsLotRelationService;
import fr.cnrs.opentheso.toolbox.policy.ToolboxAccessPolicy;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@ViewScoped
@Named("actionsLotRelationBean")
@RequiredArgsConstructor
public class ActionsLotRelationBean implements Serializable {

    private final transient ActionsLotRelationService relationService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;

    private ActionsLotImportPanelState<ActionsLotRelationCandidate> importPanel = new ActionsLotImportPanelState<>();
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
        ActionsLotUiSupport.toast(ActionsLotMessages.IMPORT_CANCELLED);
    }

    public void validateImport() {
        if (!guardAccess()) {
            return;
        }
        ActionsLotUiSupport.validateFile(importPanel, this::updateImportPanel,
                () -> relationService.validate(
                        importPanel.getFileBytes(),
                        importPanel.getChoiceDelimiter(),
                        importPanel.getIdentifierType(),
                        requireThesaurusId()),
                importPanel::applyValidation,
                "association(s) prête(s) à importer");
    }

    public void applyImport() {
        if (!guardAccess()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        ActionsLotUiSupport.applyFile(importPanel, this::updateImportPanel,
                () -> relationService.applyImport(
                        importPanel.getValidCandidates(),
                        requireThesaurusId(),
                        userId == null ? 0 : userId),
                ActionsLotMessages.NO_VALID_LINE);
    }

    private boolean guardAccess() {
        return ActionsLotUiSupport.guardAccess(toolboxAccessPolicy, userSession, thesaurusContext);
    }

    private String requireThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    private void updateImportPanel() {
        ActionsLotUiSupport.syncPanelClasses("relations", "import", importPanel.getCssClasses());
    }
}
