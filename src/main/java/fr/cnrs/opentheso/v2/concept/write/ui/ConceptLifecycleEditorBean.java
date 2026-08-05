package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTopConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApproveConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeprecateConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptLifecycleEditorBean")
@RequiredArgsConstructor
public class ConceptLifecycleEditorBean implements Serializable {

    private final ConceptLifecycleMutationService conceptLifecycleMutationService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ConceptWriteSearchService conceptWriteSearchService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;

    private String preferredLabel;
    private String currentPreferredLabel;
    private String notation;
    private String customConceptId;
    private String source;
    private String selectedGroupId;
    private String selectedNarrowerRelationType = "NT";
    private boolean duplicateLabelWarning;
    private boolean forceDeletePolyhierarchy;
    private boolean addReplacedByRelations;
    private ConceptSearchSuggestion replacedBySearchSelected;
    private List<ConceptWriteCollection> availableCollections = Collections.emptyList();
    private List<ConceptWriteNtRelationType> ntRelationTypes = Collections.emptyList();

    public boolean isWriteActionsAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession);
    }

    public boolean isActiveConceptWriteAvailable() {
        return conceptWritePolicy.canMutateActiveConcept(userSession, isSelectedDeprecated());
    }

    public boolean isRenameAvailable() {
        return conceptWritePolicy.canRenamePreferredLabel(userSession, isSelectedDeprecated());
    }

    public boolean isStatusActionsAvailable() {
        return conceptWritePolicy.canMutateConceptStatus(userSession);
    }

    public List<ConceptSearchSuggestion> autocompleteReplacedByTarget(String query) {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = thesaurusContext.resolveWorkLanguage();
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        return conceptWriteSearchService.autocompleteReplacedByTarget(query, lang, thesaurusId);
    }

    public void prepareDeprecate() {
        refreshCurrentPreferredLabel();
    }

    public void prepareApprove() {
        addReplacedByRelations = false;
        refreshCurrentPreferredLabel();
    }

    public void prepareAddReplacedBy() {
        replacedBySearchSelected = null;
        refreshCurrentPreferredLabel();
    }

    public void prepareDeleteReplacedBy() {
        refreshCurrentPreferredLabel();
    }

    public void submitDeprecate() {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new DeprecateConceptCommand(
                thesaurusContext.resolveThesaurusId(), summary.conceptId(), userId, contributorName());
        handleMutationResult(
                conceptLifecycleMutationService.deprecateConcept(command),
                summary.conceptId(),
                MutationRefreshMode.STRUCTURAL);
        PrimeFaces.current().executeScript("PF('v2DeprecateConceptDlg').hide();");
    }

    public void submitApprove() {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new ApproveConceptCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                thesaurusContext.resolveWorkLanguage(),
                userId,
                contributorName(),
                addReplacedByRelations);
        handleMutationResult(conceptLifecycleMutationService.approveConcept(command), summary.conceptId(),
                MutationRefreshMode.STRUCTURAL);
        PrimeFaces.current().executeScript("PF('v2ApproveConceptDlg').hide();");
    }

    public void submitAddReplacedBy() {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || replacedBySearchSelected == null
                || StringUtils.isBlank(replacedBySearchSelected.conceptId())) {
            MessageUtils.showErrorMessage("Pas de concept sélectionné !");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        handleMutationResult(
                conceptLifecycleMutationService.addReplacedBy(new AddReplacedByCommand(
                        thesaurusContext.resolveThesaurusId(),
                        summary.conceptId(),
                        replacedBySearchSelected.conceptId(),
                        userId,
                        contributorName())),
                summary.conceptId(),
                MutationRefreshMode.PANEL_ONLY);
        PrimeFaces.current().executeScript("PF('v2AddReplacedByDlg').hide();");
    }

    public void submitDeleteReplacedBy(String targetConceptId) {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        handleMutationResult(
                conceptLifecycleMutationService.deleteReplacedBy(new DeleteReplacedByCommand(
                        thesaurusContext.resolveThesaurusId(),
                        summary.conceptId(),
                        targetConceptId,
                        userId,
                        contributorName())),
                summary.conceptId(),
                MutationRefreshMode.PANEL_ONLY);
    }

    public void prepareRename() {
        duplicateLabelWarning = false;
        source = "";
        if (conceptSelectionContext.hasSelection()) {
            currentPreferredLabel = conceptSelectionContext.getSummary().preferredLabel();
            preferredLabel = "";
        } else {
            currentPreferredLabel = "";
            preferredLabel = "";
        }
    }

    public void submitRename() {
        submitRenameInternal(false);
    }

    public void submitRenameForced() {
        submitRenameInternal(true);
    }

    public void prepareAddChild() {
        resetNewConceptForm();
        refreshCurrentPreferredLabel();
        loadCreationFormMetadata();
    }

    public void submitAddChild() {
        submitAddChildInternal(false);
    }

    public void submitAddChildForced() {
        submitAddChildInternal(true);
    }

    public void prepareAddTopConcept() {
        resetNewConceptForm();
        loadCreationFormMetadata();
    }

    private void loadCreationFormMetadata() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = thesaurusContext.resolveWorkLanguage();
        availableCollections = Collections.emptyList();
        availableCollections = conceptWriteMetadataService.listCollections(thesaurusId, lang);
        ntRelationTypes = conceptWriteMetadataService.listNtRelationTypes();
        if (StringUtils.isBlank(selectedGroupId)) {
            selectedGroupId = conceptSelectionContext.getDefaultGroupId();
        }
        if (StringUtils.isBlank(selectedNarrowerRelationType)) {
            selectedNarrowerRelationType = "NT";
        }
    }

    public void submitAddTopConcept() {
        submitAddTopConceptInternal(false);
    }

    public void submitAddTopConceptForced() {
        submitAddTopConceptInternal(true);
    }

    public void prepareDelete() {
        forceDeletePolyhierarchy = false;
    }

    public void submitDelete() {
        if (!conceptWritePolicy.canMutateConcept(userSession) || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        boolean hasNarrowers = conceptSelectionContext.isHasNarrowers();
        var command = new DeleteConceptCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                hasNarrowers,
                forceDeletePolyhierarchy
        );
        MutationResult result = conceptLifecycleMutationService.deleteConcept(command);
        String fallbackConceptId = resolveFallbackConceptAfterDelete();
        handleMutationResult(result, fallbackConceptId, MutationRefreshMode.DELETE);
    }

    public void cancelDuplicate() {
        duplicateLabelWarning = false;
    }

    private void submitRenameInternal(boolean forced) {
        if (!isRenameAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        String lang = thesaurusContext.resolveWorkLanguage();
        var command = new RenamePreferredLabelCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                lang,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                preferredLabel,
                source,
                forced
        );
        MutationResult result = conceptLifecycleMutationService.renamePreferredLabel(command);
        if (result.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            duplicateLabelWarning = true;
            MessageUtils.showWarnMessage(result.message());
            PrimeFaces.current().ajax().update(":v2RenameConceptForm", ":messageIndex");
            return;
        }
        duplicateLabelWarning = false;
        String renamed = StringUtils.trimToEmpty(command.label());
        if (handleMutationResult(result, summary.conceptId(), MutationRefreshMode.RENAME, renamed)) {
            PrimeFaces.current().executeScript("PF('v2RenameConceptDlg').hide();");
        }
    }

    private void submitAddChildInternal(boolean forced) {
        if (!isActiveConceptWriteAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new AddChildConceptCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                thesaurusContext.resolveWorkLanguage(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                preferredLabel,
                notation,
                customConceptId,
                source,
                StringUtils.trimToNull(selectedGroupId),
                StringUtils.defaultIfBlank(selectedNarrowerRelationType, "NT"),
                forced
        );
        MutationResult result = conceptLifecycleMutationService.addChildConcept(command);
        if (result.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            duplicateLabelWarning = true;
            MessageUtils.showWarnMessage(result.message());
            return;
        }
        duplicateLabelWarning = false;
        String targetConceptId = StringUtils.defaultIfBlank(result.createdConceptId(), summary.conceptId());
        if (handleMutationResult(result, targetConceptId, MutationRefreshMode.STRUCTURAL)) {
            PrimeFaces.current().executeScript("PF('v2AddChildConceptDlg').hide();");
        }
    }

    private void submitAddTopConceptInternal(boolean forced) {
        if (!isWriteActionsAvailable()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new AddTopConceptCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                preferredLabel,
                notation,
                customConceptId,
                source,
                StringUtils.trimToNull(selectedGroupId),
                forced
        );
        MutationResult result = conceptLifecycleMutationService.addTopConcept(command);
        if (result.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            duplicateLabelWarning = true;
            MessageUtils.showWarnMessage(result.message());
            return;
        }
        duplicateLabelWarning = false;
        if (handleMutationResult(result, result.createdConceptId(), MutationRefreshMode.STRUCTURAL)) {
            PrimeFaces.current().executeScript("PF('v2AddTopConceptDlg').hide();");
        }
    }

    private enum MutationRefreshMode {
        RENAME,
        STRUCTURAL,
        DELETE,
        PANEL_ONLY
    }

    private boolean handleMutationResult(MutationResult result, String conceptIdToOpen, MutationRefreshMode mode) {
        return handleMutationResult(result, conceptIdToOpen, mode, null);
    }

    private boolean handleMutationResult(
            MutationResult result,
            String conceptIdToOpen,
            MutationRefreshMode mode,
            String renamedLabel
    ) {
        if (result == null) {
            return false;
        }
        switch (result.outcome()) {
            case OK -> {
                switch (mode) {
                    case RENAME -> conceptNavigationSupport.refreshAfterRename(conceptIdToOpen, renamedLabel);
                    case DELETE -> {
                        conceptNavigationSupport.invalidateConceptTree();
                        conceptNavigationSupport.afterConceptDeleted(conceptIdToOpen);
                    }
                    case STRUCTURAL -> {
                        conceptNavigationSupport.invalidateConceptTree();
                        if (StringUtils.isNotBlank(conceptIdToOpen)) {
                            conceptNavigationSupport.openConcept(conceptIdToOpen);
                        } else {
                            conceptNavigationSupport.refreshSelectedConcept();
                        }
                    }
                    case PANEL_ONLY -> conceptNavigationSupport.openConcept(conceptIdToOpen);
                }
                // formLeftTab pour rafraîchir le libellé dans l'arbre (comme legacy EditConcept#updateLabel)
                PrimeFaces.current().ajax().update(
                        ":containerIndex:formRightTab",
                        ":containerIndex:formLeftTab",
                        ":messageIndex");
                MessageUtils.showInformationMessage(result.message());
                return true;
            }
            case VALIDATION_ERROR, FAILURE, FORBIDDEN -> {
                MessageUtils.showErrorMessage(result.message());
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private void resetNewConceptForm() {
        duplicateLabelWarning = false;
        preferredLabel = "";
        notation = "";
        customConceptId = "";
        source = "";
        selectedGroupId = conceptSelectionContext.getDefaultGroupId();
        selectedNarrowerRelationType = "NT";
    }

    private String resolveFallbackConceptAfterDelete() {
        return conceptSelectionContext.getFirstBroaderConceptId();
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(conceptSelectionContext.getSummary().status()));
    }

    private void refreshCurrentPreferredLabel() {
        currentPreferredLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }
}
