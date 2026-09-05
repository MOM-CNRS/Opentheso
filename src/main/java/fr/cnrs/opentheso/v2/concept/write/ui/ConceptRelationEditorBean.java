package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptRelationMutationService;
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
@Named("v2ConceptRelationEditorBean")
@RequiredArgsConstructor
public class ConceptRelationEditorBean implements Serializable {

    private final transient ConceptRelationMutationService conceptRelationMutationService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptNavigationSupport conceptNavigationSupport;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient ConceptWriteSearchService conceptWriteSearchService;
    private final transient ConceptReadService conceptReadService;
    private final transient ThesaurusBrowseBean thesaurusBrowseBean;

    private ConceptSearchSuggestion searchSelected;
    private String currentConceptLabel;
    private boolean tagPrefLabel;
    private boolean applyToBranch;
    private String selectedRelationRole = "NT";
    private List<ConceptWriteNtRelationType> ntRelationTypes = Collections.emptyList();
    private List<NarrowerRelationEditRow> narrowerEdits = Collections.emptyList();
    private List<ConceptCustomRelationItem> customRelationsToDelete = Collections.emptyList();

    public boolean isCustomRelationActionsAvailable() {
        return conceptWritePolicy.canMutateCustomRelations(userSession, isSelectedDeprecated());
    }

    public boolean isRelationActionsAvailable() {
        return conceptWritePolicy.canMutateHierarchicalRelations(userSession, isSelectedDeprecated());
    }

    public List<ConceptSearchSuggestion> autocompleteRelationTarget(String query) {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = thesaurusContext.resolveWorkLanguage();
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return Collections.emptyList();
        }
        return conceptWriteSearchService.autocompleteRelationTarget(query, lang, thesaurusId, true);
    }

    public void prepareAddBroader() {
        resetSearch();
    }

    public void prepareAddNarrower() {
        resetSearch();
    }

    public void prepareAddRelated() {
        resetSearch();
        tagPrefLabel = false;
    }

    public void prepareDeleteBroader() {
        refreshCurrentConceptLabel();
    }

    public void prepareDeleteNarrower() {
        refreshCurrentConceptLabel();
    }

    public void prepareDeleteRelated() {
        refreshCurrentConceptLabel();
    }

    public void prepareAddCustomRelation() {
        resetSearch();
    }

    public void prepareDeleteCustomRelation() {
        refreshCurrentConceptLabel();
        reloadCustomRelationsToDelete();
    }

    public void prepareChangeNarrowerTypes() {
        refreshCurrentConceptLabel();
        applyToBranch = false;
        selectedRelationRole = "NT";
        ntRelationTypes = conceptRelationMutationService.listNtRelationTypes();
        narrowerEdits = loadNarrowerEdits();
    }

    public void submitAddBroader() {
        submitAddBroaderInternal();
    }

    public void submitAddNarrower() {
        submitAddNarrowerInternal();
    }

    public void submitAddRelated() {
        submitAddRelatedInternal();
    }

    public void submitDeleteBroader(String targetConceptId) {
        submitDeleteBroaderInternal(targetConceptId);
    }

    public void submitDeleteNarrower(String targetConceptId) {
        submitDeleteNarrowerInternal(targetConceptId);
    }

    public void submitDeleteRelated(String targetConceptId) {
        submitDeleteRelatedInternal(targetConceptId);
    }

    public void submitAddCustomRelation() {
        if (!isCustomRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        if (searchSelected == null || StringUtils.isBlank(searchSelected.conceptId())) {
            MessageUtils.showErrorMessage("Aucune relation n'est sélectionnée !");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new AddCustomRelationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                searchSelected.conceptId(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptRelationMutationService.addCustomRelation(command),
                summary.conceptId(),
                "PF('v2AddCustomRelationDlg').hide();",
                RelationRefreshMode.PANEL_ONLY,
                false
        );
    }

    public void submitDeleteCustomRelation(String targetConceptId, String relationCode, boolean reciprocal) {
        if (!isCustomRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new DeleteCustomRelationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                targetConceptId,
                relationCode,
                reciprocal,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptRelationMutationService.deleteCustomRelation(command),
                summary.conceptId(),
                null,
                RelationRefreshMode.PANEL_ONLY,
                false
        );
        reloadCustomRelationsToDelete();
    }

    public void submitUpdateNarrowerType(NarrowerRelationEditRow row) {
        if (!isRelationActionsAvailable() || !conceptSelectionContext.hasSelection() || row == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new UpdateNarrowerRelationTypeCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                row.getConceptId(),
                row.getRole(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptRelationMutationService.updateNarrowerRelationType(command),
                summary.conceptId(),
                null,
                RelationRefreshMode.PANEL_ONLY,
                false
        );
        narrowerEdits = loadNarrowerEdits();
    }

    public void submitApplyNarrowerTypeToBranch() {
        if (!isRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new ApplyNarrowerRelationToBranchCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                selectedRelationRole,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        if (handleMutationResult(
                conceptRelationMutationService.applyNarrowerRelationToBranch(command),
                summary.conceptId(),
                "PF('v2ChangeNarrowerRelationDlg').hide();",
                RelationRefreshMode.STRUCTURAL,
                false
        )) {
            prepareChangeNarrowerTypes();
        }
    }

    private void submitAddBroaderInternal() {
        if (!isRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        if (searchSelected == null || StringUtils.isBlank(searchSelected.conceptId())) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new AddBroaderRelationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                searchSelected.conceptId(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptRelationMutationService.addBroaderRelation(command),
                summary.conceptId(),
                "PF('v2AddBroaderRelationDlg').hide();",
                RelationRefreshMode.STRUCTURAL,
                false
        );
    }

    private void submitAddNarrowerInternal() {
        if (!isRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        if (searchSelected == null || StringUtils.isBlank(searchSelected.conceptId())) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new AddNarrowerRelationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                searchSelected.conceptId(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptRelationMutationService.addNarrowerRelation(command),
                summary.conceptId(),
                "PF('v2AddNarrowerRelationDlg').hide();",
                RelationRefreshMode.STRUCTURAL,
                false
        );
    }

    private void submitAddRelatedInternal() {
        if (!isRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        if (searchSelected == null || StringUtils.isBlank(searchSelected.conceptId())) {
            MessageUtils.showErrorMessage("Aucune relation n'est sélectionnée !");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new AddRelatedRelationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                searchSelected.conceptId(),
                thesaurusContext.resolveWorkLanguage(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                tagPrefLabel
        );
        handleMutationResult(
                conceptRelationMutationService.addRelatedRelation(command),
                summary.conceptId(),
                "PF('v2AddRelatedRelationDlg').hide();",
                tagPrefLabel ? RelationRefreshMode.LABEL_UPDATE : RelationRefreshMode.PANEL_ONLY,
                tagPrefLabel
        );
    }

    private void submitDeleteBroaderInternal(String targetConceptId) {
        submitDeleteHierarchicalInternal(targetConceptId, true);
    }

    private void submitDeleteNarrowerInternal(String targetConceptId) {
        submitDeleteHierarchicalInternal(targetConceptId, false);
    }

    private void submitDeleteRelatedInternal(String targetConceptId) {
        if (!isRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new DeleteRelatedRelationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                targetConceptId,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptRelationMutationService.deleteRelatedRelation(command),
                summary.conceptId(),
                null,
                RelationRefreshMode.PANEL_ONLY,
                false
        );
    }

    private void submitDeleteHierarchicalInternal(String targetConceptId, boolean broader) {
        if (!isRelationActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        MutationResult result = broader
                ? conceptRelationMutationService.deleteBroaderRelation(new DeleteBroaderRelationCommand(
                        thesaurusContext.resolveThesaurusId(), summary.conceptId(), targetConceptId, userId,
                        StringUtils.defaultString(userSession.getCurrentUsername())))
                : conceptRelationMutationService.deleteNarrowerRelation(new DeleteNarrowerRelationCommand(
                        thesaurusContext.resolveThesaurusId(), summary.conceptId(), targetConceptId, userId,
                        StringUtils.defaultString(userSession.getCurrentUsername())));
        handleMutationResult(result, summary.conceptId(), null, RelationRefreshMode.STRUCTURAL, false);
    }

    private boolean handleMutationResult(
            MutationResult result,
            String conceptId,
            String hideDialogScript,
            RelationRefreshMode refreshMode,
            boolean refreshTreeLabelAfterOpen
    ) {
        if (result == null) {
            return false;
        }
        switch (result.outcome()) {
            case OK -> {
                switch (refreshMode) {
                    case STRUCTURAL -> {
                        conceptNavigationSupport.invalidateConceptTree();
                        conceptNavigationSupport.openConcept(conceptId);
                    }
                    case PANEL_ONLY -> conceptNavigationSupport.openConcept(conceptId);
                    case LABEL_UPDATE -> {
                        conceptNavigationSupport.openConcept(conceptId);
                        if (refreshTreeLabelAfterOpen && conceptSelectionContext.hasSelection()) {
                            conceptNavigationSupport.refreshAfterRename(
                                    conceptId,
                                    conceptSelectionContext.getSummary().preferredLabel()
                            );
                        }
                    }
                }
                PrimeFaces.current().ajax().update(
                        ":containerIndex:formRightTab",
                        ":containerIndex:formLeftTab",
                        ":messageIndex");
                MessageUtils.showInformationMessage(result.message());
                if (StringUtils.isNotBlank(hideDialogScript)) {
                    PrimeFaces.current().executeScript(hideDialogScript);
                }
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

    private List<NarrowerRelationEditRow> loadNarrowerEdits() {
        if (!conceptSelectionContext.hasSelection()) {
            return Collections.emptyList();
        }
        var summary = conceptSelectionContext.getSummary();
        return conceptReadService.loadDetail(
                        thesaurusContext.resolveThesaurusId(),
                        summary.conceptId(),
                        thesaurusContext.resolveWorkLanguage())
                .map(detail -> detail.narrowerTerms().stream()
                        .map(this::toEditRow)
                        .toList())
                .orElseGet(Collections::emptyList);
    }

    private NarrowerRelationEditRow toEditRow(ConceptRelation relation) {
        return new NarrowerRelationEditRow(
                relation.conceptId(),
                relation.label(),
                StringUtils.defaultIfBlank(relation.role(), "NT")
        );
    }

    private void resetSearch() {
        refreshCurrentConceptLabel();
        searchSelected = null;
    }

    private void refreshCurrentConceptLabel() {
        if (conceptSelectionContext.hasSelection()) {
            currentConceptLabel = conceptSelectionContext.getSummary().preferredLabel();
        } else {
            currentConceptLabel = "";
        }
    }

    private void reloadCustomRelationsToDelete() {
        if (thesaurusBrowseBean.getSelectedConcept() == null) {
            customRelationsToDelete = Collections.emptyList();
            return;
        }
        List<ConceptCustomRelationItem> relations = thesaurusBrowseBean.getSelectedConcept().getCustomRelations();
        customRelationsToDelete = relations != null ? List.copyOf(relations) : Collections.emptyList();
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(conceptSelectionContext.getSummary().status()));
    }

    private enum RelationRefreshMode {
        STRUCTURAL,
        PANEL_ONLY,
        LABEL_UPDATE
    }
}
