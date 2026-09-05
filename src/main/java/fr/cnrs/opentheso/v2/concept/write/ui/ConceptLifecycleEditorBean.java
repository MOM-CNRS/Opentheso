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
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptLifecycleEditorBean")
@RequiredArgsConstructor
public class ConceptLifecycleEditorBean implements Serializable {

    private final transient ConceptLifecycleMutationService conceptLifecycleMutationService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptNavigationSupport conceptNavigationSupport;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient ConceptWriteSearchService conceptWriteSearchService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;
    private final transient BranchConceptSupport branchConceptSupport;
    private final transient V2LocaleBean v2LocaleBean;

    private final DialogRunState createRun = new DialogRunState();
    private final DialogRunState deleteRun = new DialogRunState();

    private String preferredLabel;
    private String currentPreferredLabel;
    private String notation;
    private String customConceptId;
    private String source;
    private String selectedGroupId;
    private String selectedNarrowerRelationType = "NT";
    private boolean duplicateLabelWarning;
    private int createdCount;
    private String lastCreatedId = "";
    private String lastCreatedLabel = "";
    private List<String> createdLabels = new ArrayList<>();
    private boolean forceDeletePolyhierarchy;
    private boolean hasNarrowersForDelete;
    private String deleteConceptId = "";
    private int deleteBranchCount = 1;
    private int deletedCount;
    private String fallbackConceptId;
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

    public String getCreateErrorMessage() {
        return createRun.getErrorMessage();
    }

    public String getCreateFlashMessage() {
        return createRun.getFlashMessage();
    }

    public String getCreateFlashToken() {
        return createRun.getFlashToken();
    }

    public String getDeleteRunState() {
        return deleteRun.getState();
    }

    public String getDeleteErrorMessage() {
        return deleteRun.getErrorMessage();
    }

    public String getDeleteFlashMessage() {
        return deleteRun.getFlashMessage();
    }

    public String getDeleteFlashToken() {
        return deleteRun.getFlashToken();
    }

    public void submitDeprecate() {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            legacyError(unauthorized());
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new DeprecateConceptCommand(
                thesaurusContext.resolveThesaurusId(), summary.conceptId(), userId, contributorName());
        handleLegacyMutationResult(
                conceptLifecycleMutationService.deprecateConcept(command),
                summary.conceptId(),
                MutationRefreshMode.STRUCTURAL);
        legacyHide("v2DeprecateConceptDlg");
    }

    public void submitApprove() {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            legacyError(unauthorized());
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
        handleLegacyMutationResult(conceptLifecycleMutationService.approveConcept(command), summary.conceptId(),
                MutationRefreshMode.STRUCTURAL);
        legacyHide("v2ApproveConceptDlg");
    }

    public void submitAddReplacedBy() {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            legacyError(unauthorized());
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || replacedBySearchSelected == null
                || StringUtils.isBlank(replacedBySearchSelected.conceptId())) {
            legacyError(msg("v2.write.noSelection", "Pas de concept sélectionné !"));
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        handleLegacyMutationResult(
                conceptLifecycleMutationService.addReplacedBy(new AddReplacedByCommand(
                        thesaurusContext.resolveThesaurusId(),
                        summary.conceptId(),
                        replacedBySearchSelected.conceptId(),
                        userId,
                        contributorName())),
                summary.conceptId(),
                MutationRefreshMode.PANEL_ONLY);
        legacyHide("v2AddReplacedByDlg");
    }

    public void submitDeleteReplacedBy(String targetConceptId) {
        if (!isStatusActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            legacyError(unauthorized());
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        handleLegacyMutationResult(
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

    public boolean isCreateReady() {
        return isActiveConceptWriteAvailable()
                && conceptSelectionContext.hasSelection()
                && StringUtils.isNotBlank(preferredLabel);
    }

    public boolean isCreateDirty() {
        return createdCount > 0;
    }

    public String getWorkLanguage() {
        return StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
    }

    public String formatNtLabel(ConceptWriteNtRelationType type) {
        if (type == null) {
            return "";
        }
        String label = "fr".equalsIgnoreCase(getWorkLanguage())
                ? type.descriptionFr()
                : type.descriptionEn();
        if (StringUtils.isBlank(label)) {
            label = StringUtils.defaultIfBlank(type.descriptionFr(), type.descriptionEn());
        }
        return StringUtils.defaultIfBlank(label, type.relationType());
    }

    public boolean isNtPicked(ConceptWriteNtRelationType type) {
        return type != null
                && StringUtils.defaultIfBlank(selectedNarrowerRelationType, "NT")
                .equalsIgnoreCase(type.relationType());
    }

    public void prepareAddChild() {
        resetNewConceptForm();
        createdCount = 0;
        lastCreatedId = "";
        lastCreatedLabel = "";
        createdLabels = new ArrayList<>();
        createRun.reset();
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

    public boolean isDeleteReady() {
        return isWriteActionsAvailable()
                && conceptSelectionContext.hasSelection()
                && !deleteRun.isDone();
    }

    public boolean isBranchDelete() {
        return hasNarrowersForDelete || deleteBranchCount > 1;
    }

    public void prepareDelete() {
        forceDeletePolyhierarchy = false;
        hasNarrowersForDelete = false;
        deleteConceptId = "";
        deleteBranchCount = 1;
        deletedCount = 0;
        deleteRun.reset();
        fallbackConceptId = resolveFallbackConceptAfterDelete();
        refreshCurrentPreferredLabel();
        if (!conceptSelectionContext.hasSelection()) {
            return;
        }
        deleteConceptId = StringUtils.defaultString(conceptSelectionContext.getConceptId());
        hasNarrowersForDelete = conceptSelectionContext.isHasNarrowers();
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (hasNarrowersForDelete && StringUtils.isNoneBlank(thesaurusId, deleteConceptId)) {
            var ids = branchConceptSupport.collectBranchConceptIds(thesaurusId, deleteConceptId);
            deleteBranchCount = Math.max(ids.size(), 1);
        } else {
            deleteBranchCount = 1;
        }
    }

    public boolean submitDelete() {
        deleteRun.setErrorMessage(null);
        if (!isWriteActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            deleteRun.fail(unauthorized());
            return false;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String conceptId = StringUtils.defaultIfBlank(deleteConceptId, conceptSelectionContext.getConceptId());
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            deleteRun.fail(msg("v2.write.missingParams", "Erreur manque de paramètres"));
            return false;
        }
        boolean hasNarrowers = hasNarrowersForDelete || deleteBranchCount > 1;
        var command = new DeleteConceptCommand(
                thesaurusId,
                conceptId,
                hasNarrowers,
                forceDeletePolyhierarchy
        );
        MutationResult result = conceptLifecycleMutationService.deleteConcept(command);
        if (result == null || !result.success()) {
            deleteRun.fail(result != null
                    ? StringUtils.defaultIfBlank(result.message(), msg("v2.concept.deleteFailed", "La suppression a échoué"))
                    : msg("v2.concept.deleteFailed", "La suppression a échoué"));
            return false;
        }
        deletedCount = Math.max(deleteBranchCount, 1);
        deleteRun.succeed(deletedCount == 1
                ? msg("v2.concept.deleteOne", "1 concept supprimé")
                : msg("v2.concept.deleteMany", "{0} concepts supprimés", deletedCount));
        return true;
    }

    public void finishDeleteAfterClose() {
        String fallback = fallbackConceptId;
        deleteRun.reset();
        conceptNavigationSupport.invalidateConceptTree();
        conceptNavigationSupport.afterConceptDeleted(fallback);
    }

    public void cancelDuplicate() {
        duplicateLabelWarning = false;
        createRun.setErrorMessage(null);
    }

    public void finishCreateAfterClose() {
        createRun.reset();
        duplicateLabelWarning = false;
        createdCount = 0;
        lastCreatedId = "";
        lastCreatedLabel = "";
        createdLabels = new ArrayList<>();
    }

    private void submitRenameInternal(boolean forced) {
        if (!isRenameAvailable() || !conceptSelectionContext.hasSelection()) {
            legacyError(unauthorized());
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            legacyError(unauthorized());
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
            legacyUpdate(":v2RenameConceptForm", ":messageIndex");
            return;
        }
        duplicateLabelWarning = false;
        String renamed = StringUtils.trimToEmpty(command.label());
        if (handleLegacyMutationResult(result, summary.conceptId(), MutationRefreshMode.RENAME, renamed)) {
            legacyHide("v2RenameConceptDlg");
        }
    }

    private void submitAddChildInternal(boolean forced) {
        createRun.setErrorMessage(null);
        createRun.clearFlash();
        if (!isActiveConceptWriteAvailable() || !conceptSelectionContext.hasSelection()) {
            createRun.setErrorMessage(unauthorized());
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            createRun.setErrorMessage(unauthorized());
            return;
        }
        if (StringUtils.isBlank(preferredLabel)) {
            createRun.setErrorMessage(msg("v2.concept.addLabelRequired", "Le libellé est obligatoire"));
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
        if (result == null) {
            createRun.setErrorMessage(msg("v2.concept.addFailed", "La création a échoué"));
            return;
        }
        if (result.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            duplicateLabelWarning = true;
            createRun.setErrorMessage(StringUtils.defaultIfBlank(result.message(),
                    msg("v2.concept.addDup", "Un libellé identique existe déjà")));
            return;
        }
        if (!result.success()) {
            duplicateLabelWarning = false;
            createRun.setErrorMessage(StringUtils.defaultIfBlank(result.message(),
                    msg("v2.concept.addFailed", "La création a échoué")));
            return;
        }
        duplicateLabelWarning = false;
        lastCreatedId = StringUtils.defaultString(result.createdConceptId());
        lastCreatedLabel = preferredLabel.trim();
        createdCount++;
        createdLabels.add(lastCreatedLabel);
        createRun.flash(msg("v2.concept.addCreated", "Concept « {0} » créé", lastCreatedLabel));
        preferredLabel = "";
        notation = "";
        customConceptId = "";
    }

    private void submitAddTopConceptInternal(boolean forced) {
        if (!isWriteActionsAvailable()) {
            legacyError(unauthorized());
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            legacyError(unauthorized());
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
        if (handleLegacyMutationResult(result, result.createdConceptId(), MutationRefreshMode.STRUCTURAL)) {
            legacyHide("v2AddTopConceptDlg");
        }
    }

    private enum MutationRefreshMode {
        RENAME,
        STRUCTURAL,
        DELETE,
        PANEL_ONLY
    }

    private boolean handleLegacyMutationResult(MutationResult result, String conceptIdToOpen, MutationRefreshMode mode) {
        return handleLegacyMutationResult(result, conceptIdToOpen, mode, null);
    }

    private boolean handleLegacyMutationResult(
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
                legacyUpdate(
                        ":containerIndex:formRightTab",
                        ":containerIndex:formLeftTab",
                        ":messageIndex");
                MessageUtils.showInformationMessage(result.message());
                return true;
            }
            case VALIDATION_ERROR, FAILURE, FORBIDDEN -> {
                legacyError(result.message());
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


    private String unauthorized() {
        return WriteUiMessages.unauthorized(v2LocaleBean);
    }

    private String msg(String key, String fallback) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback);
    }

    private String msg(String key, String fallback, Object... args) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback, args);
    }

    /** Dialogues PrimeFaces V1 uniquement — jamais sur le chemin V2 (création / suppression). */
    private void legacyHide(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').hide();");
    }

    private void legacyUpdate(String... clientIds) {
        PrimeFaces.current().ajax().update(clientIds);
    }

    private void legacyError(String message) {
        MessageUtils.showErrorMessage(message);
    }
}
