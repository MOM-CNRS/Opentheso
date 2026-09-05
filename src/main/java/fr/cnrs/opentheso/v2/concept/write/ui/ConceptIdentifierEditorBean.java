package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptIdentifiers;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptIdentifierMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptIdentifierEditorBean")
@RequiredArgsConstructor
public class ConceptIdentifierEditorBean implements Serializable {

    private static final String ARK_FAILED_KEY = "v2.concept.arkFailed";
    private static final String ARK_FAILED_FALLBACK = "La génération Ark a échoué";


    private final transient ConceptIdentifierMutationService conceptIdentifierMutationService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptNavigationSupport conceptNavigationSupport;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient PreferencesRepository preferencesRepository;
    private final transient BranchConceptSupport branchConceptSupport;
    private final transient ThesaurusBrowseBean thesaurusBrowseBean;
    private final transient ConceptRepository conceptRepository;
    private final transient V2LocaleBean v2LocaleBean;

    private final DialogRunState arkRun = new DialogRunState();

    private String currentConceptId;
    private String currentConceptLabel;
    private String currentArkId;
    private String arkProvider = "";
    private String currentHandleId;
    private String thesaurusLabel;
    private String thesaurusId;
    private List<String> missingArkIds = Collections.emptyList();
    private int missingArkCreated;
    private List<String> branchConceptIds = Collections.emptyList();
    private int branchArkMissing;
    private int branchArkExisting;
    private int branchProcessed;
    private List<String> allConceptIds = Collections.emptyList();
    private int allArkMissing;
    private int allArkExisting;
    private int allProcessed;

    public boolean isIdentifierActionsAvailable() {
        return conceptWritePolicy.canMutateIdentifiers(userSession);
    }

    /**
     * Aligné legacy filAariane :
     * {@code useArk || useArkLocal || useOpenArk} + admin/superAdmin.
     */
    public boolean isArkGenerationAvailable() {
        Preferences preferences = loadPreferences();
        return isIdentifierActionsAvailable()
                && preferences != null
                && (preferences.isUseArk() || preferences.isUseArkLocal() || preferences.isUseOpenArk());
    }

    /** Aligné legacy : missing / all / branche ARK uniquement si useArk ou useArkLocal. */
    public boolean isArkBatchGenerationAvailable() {
        Preferences preferences = loadPreferences();
        return isIdentifierActionsAvailable()
                && preferences != null
                && (preferences.isUseArk() || preferences.isUseArkLocal());
    }

    /**
     * Aligné legacy : « Supprimer l'identifiant Ark » visible seulement si OpenArk est activé
     * (et admin/superAdmin via {@link #isIdentifierActionsAvailable()}).
     */
    public boolean isArkDeletionAvailable() {
        Preferences preferences = loadPreferences();
        return isIdentifierActionsAvailable() && preferences != null && preferences.isUseOpenArk();
    }

    public boolean isHandleGenerationAvailable() {
        Preferences preferences = loadPreferences();
        return isIdentifierActionsAvailable() && preferences != null && preferences.isUseHandle();
    }

    /** Aligné legacy : entrée visible dès que Handle est activé (admin). */
    public boolean isHandleDeletionAvailable() {
        return isHandleGenerationAvailable();
    }

    public boolean isExistingArk() {
        return StringUtils.isNotBlank(currentArkId);
    }

    public boolean isLocalArkProvider() {
        return "local".equals(arkProvider);
    }

    public boolean isMissingArkEmpty() {
        return getMissingArkCount() == 0;
    }

    public int getMissingArkCount() {
        return missingArkIds == null ? 0 : missingArkIds.size();
    }

    public String getArkRunState() {
        return arkRun.getState();
    }

    public void setArkRunState(String state) {
        arkRun.setState(state);
    }

    public String getErrorMessage() {
        return arkRun.getErrorMessage();
    }

    public void setErrorMessage(String errorMessage) {
        arkRun.setErrorMessage(errorMessage);
    }

    public String getFlashMessage() {
        return arkRun.getFlashMessage();
    }

    public void setFlashMessage(String flashMessage) {
        arkRun.setFlashMessage(flashMessage);
    }

    public String getFlashToken() {
        return arkRun.getFlashToken();
    }

    public void setFlashToken(String flashToken) {
        arkRun.setFlashToken(flashToken);
    }

    public boolean isMissingArkSubmitReady() {
        return !isMissingArkEmpty() && !arkRun.isDone();
    }

    public boolean isBranchArkEmpty() {
        return getBranchConceptCount() == 0;
    }

    public int getBranchConceptCount() {
        return branchConceptIds == null ? 0 : branchConceptIds.size();
    }

    public boolean isBranchArkSubmitReady() {
        return !isBranchArkEmpty() && !arkRun.isDone();
    }

    public boolean isAllArkEmpty() {
        return getAllConceptCount() == 0;
    }

    public int getAllConceptCount() {
        return allConceptIds == null ? 0 : allConceptIds.size();
    }

    public boolean isAllArkSubmitReady() {
        return !isAllArkEmpty() && !arkRun.isDone();
    }

    public void prepareGenerateArk() {
        arkRun.reset();
        refreshContext();
    }

    public void prepareGenerateMissingArk() {
        arkRun.reset();
        missingArkCreated = 0;
        refreshContext();
        loadMissingArkIds();
    }

    public void prepareDeleteArk() {
        refreshContext();
    }

    public void prepareGenerateArkForBranch() {
        arkRun.reset();
        branchProcessed = 0;
        refreshContext();
        loadBranchConceptIds();
        countBranchArk();
    }

    public void prepareGenerateAllArk() {
        arkRun.reset();
        allProcessed = 0;
        refreshContext();
        loadAllConceptsForArk();
    }

    public void prepareGenerateHandle() {
        refreshContext();
    }

    public void prepareDeleteHandle() {
        refreshContext();
        currentHandleId = resolveHandleId();
    }

    public void prepareGenerateHandleForBranch() {
        refreshContext();
        loadBranchConceptIds();
    }

    public boolean submitGenerateArk() {
        arkRun.setErrorMessage(null);
        if (!isArkGenerationAvailable() || !conceptSelectionContext.hasSelection()) {
            arkRun.fail(unauthorized());
            return false;
        }
        boolean updating = isExistingArk();
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                List.of(requireConceptId())
        );
        MutationResult result = conceptIdentifierMutationService.generateArk(command);
        if (result == null || !result.success()) {
            arkRun.fail(result != null ? result.message() : msg(ARK_FAILED_KEY, ARK_FAILED_FALLBACK));
            return false;
        }
        currentArkId = resolveArkIdFromStore();
        String arkSuffix = StringUtils.isBlank(currentArkId) ? "" : " : " + currentArkId;
        arkRun.succeed((updating
                ? msg("v2.concept.arkUpdatedFlash", "Identifiant Ark mis à jour")
                : msg("v2.concept.arkCreatedFlash", "Identifiant Ark créé")) + arkSuffix);
        return true;
    }

    public boolean submitGenerateMissingArk() {
        arkRun.setErrorMessage(null);
        if (!isArkBatchGenerationAvailable()) {
            arkRun.fail(unauthorized());
            return false;
        }
        loadMissingArkIds();
        if (missingArkIds.isEmpty()) {
            arkRun.fail(msg("v2.concept.arkNoneMissing", "Aucun concept sans identifiant Ark"));
            return false;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                missingArkIds
        );
        MutationResult result = conceptIdentifierMutationService.generateArk(command);
        if (result == null || !result.success()) {
            arkRun.fail(result != null ? result.message() : msg(ARK_FAILED_KEY, ARK_FAILED_FALLBACK));
            return false;
        }
        missingArkCreated = missingArkIds.size();
        if (result.warning()) {
            arkRun.setErrorMessage(result.message());
        }
        loadMissingArkIds();
        arkRun.complete(missingArkCreated == 1
                ? msg("v2.concept.arkCreatedOne", "1 identifiant Ark créé")
                : msg("v2.concept.arkCreatedMany", "{0} identifiants Ark créés", missingArkCreated));
        return true;
    }

    public void submitDeleteArk() {
        if (!isArkDeletionAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        var command = new DeleteArkCommand(
                thesaurusContext.resolveThesaurusId(),
                List.of(requireConceptId())
        );
        handleMutationResult(conceptIdentifierMutationService.deleteArk(command));
    }

    public boolean submitGenerateArkForBranch() {
        arkRun.setErrorMessage(null);
        if (!isArkBatchGenerationAvailable() || !conceptSelectionContext.hasSelection()) {
            arkRun.fail(unauthorized());
            return false;
        }
        loadBranchConceptIds();
        countBranchArk();
        if (branchConceptIds.isEmpty()) {
            arkRun.fail(msg("v2.concept.arkBranchEmptyFlash", "Aucun concept dans cette branche"));
            return false;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                branchConceptIds
        );
        MutationResult result = conceptIdentifierMutationService.generateArk(command);
        if (result == null || !result.success()) {
            arkRun.fail(result != null ? result.message() : msg(ARK_FAILED_KEY, ARK_FAILED_FALLBACK));
            return false;
        }
        branchProcessed = branchConceptIds.size();
        if (result.warning()) {
            arkRun.setErrorMessage(result.message());
        }
        countBranchArk();
        arkRun.complete(branchProcessed == 1
                ? msg("v2.concept.arkBranchFlashOne", "1 concept de la branche traité")
                : msg("v2.concept.arkBranchFlashMany", "{0} concepts de la branche traités", branchProcessed));
        return true;
    }

    public void submitGenerateArkForConceptsWithoutArk() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isArkBatchGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        List<String> conceptIds = conceptRepository.findAllIdConceptsWithoutArk(thesaurusId);
        if (CollectionUtils.isEmpty(conceptIds)) {
            MessageUtils.showInformationMessage(msg("v2.concept.arkNoneFlash", "Aucun concept sans ARK"));
            return;
        }
        submitGenerateArkForConcepts(conceptIds, true);
    }

    public boolean submitGenerateAllArk() {
        arkRun.setErrorMessage(null);
        if (!isArkBatchGenerationAvailable()) {
            arkRun.fail(unauthorized());
            return false;
        }
        loadAllConceptsForArk();
        if (allConceptIds.isEmpty()) {
            arkRun.fail(msg("v2.concept.arkAllEmptyFlash", "Aucun concept dans ce thésaurus"));
            return false;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                allConceptIds
        );
        MutationResult result = conceptIdentifierMutationService.generateArk(command);
        if (result == null || !result.success()) {
            arkRun.fail(result != null ? result.message() : msg(ARK_FAILED_KEY, ARK_FAILED_FALLBACK));
            return false;
        }
        allProcessed = allConceptIds.size();
        if (result.warning()) {
            arkRun.setErrorMessage(result.message());
        }
        loadAllConceptsForArk();
        arkRun.complete(allProcessed == 1
                ? msg("v2.concept.arkAllFlashOne", "1 concept du thésaurus traité")
                : msg("v2.concept.arkAllFlashMany", "{0} concepts du thésaurus traités", allProcessed));
        return true;
    }

    public void submitGenerateHandle() {
        submitGenerateHandleForConcepts(List.of(requireConceptId()));
    }

    public void submitDeleteHandle() {
        if (!isHandleDeletionAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        currentHandleId = resolveHandleId();
        var command = new DeleteHandleCommand(
                thesaurusContext.resolveThesaurusId(),
                requireConceptId(),
                currentHandleId
        );
        handleMutationResult(conceptIdentifierMutationService.deleteHandle(command));
    }

    public void submitGenerateHandleForBranch() {
        if (branchConceptIds.isEmpty()) {
            loadBranchConceptIds();
        }
        submitGenerateHandleForConcepts(branchConceptIds);
    }

    public void submitGenerateHandleForConceptsWithoutHandle() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isHandleGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        List<String> conceptIds = conceptRepository.findAllIdsWithoutHandle(thesaurusId);
        if (CollectionUtils.isEmpty(conceptIds)) {
            MessageUtils.showInformationMessage(msg("v2.concept.handleNoneFlash", "Aucun concept sans Handle"));
            return;
        }
        submitGenerateHandleForConcepts(conceptIds);
    }

    public void submitGenerateAllHandle() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isHandleGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        submitGenerateHandleForConcepts(loadAllConceptIds(thesaurusId));
    }

    private void submitGenerateArkForConcepts(List<String> conceptIds, boolean allowed) {
        if (!allowed || conceptIds == null || conceptIds.isEmpty()) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                conceptIds
        );
        handleMutationResult(conceptIdentifierMutationService.generateArk(command));
    }

    private void submitGenerateHandleForConcepts(List<String> conceptIds) {
        if (!isHandleGenerationAvailable() || conceptIds == null || conceptIds.isEmpty()) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        var command = new GenerateHandleCommand(thesaurusContext.resolveThesaurusId(), conceptIds);
        handleMutationResult(conceptIdentifierMutationService.generateHandle(command));
    }

    private List<String> loadAllConceptIds(String thesaurusId) {
        List<Concept> concepts = conceptRepository.findAllByIdThesaurusAndStatusNot(thesaurusId, "CA");
        if (CollectionUtils.isEmpty(concepts)) {
            return Collections.emptyList();
        }
        return concepts.stream().map(Concept::getIdConcept).toList();
    }

    private void handleMutationResult(MutationResult result) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : msg("v2.write.failed", "Erreur"));
            return;
        }
        conceptNavigationSupport.refreshSelectedConcept();
        MessageUtils.showInformationMessage(result.message());
    }

    private void refreshContext() {
        currentConceptId = requireConceptId();
        currentConceptLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
        currentArkId = resolveArkId();
        arkProvider = resolveArkProvider();
        currentHandleId = resolveHandleId();
        thesaurusId = StringUtils.defaultString(thesaurusContext.resolveThesaurusId());
        thesaurusLabel = StringUtils.defaultIfBlank(
                thesaurusContext.getCurrentThesaurusTitle(),
                thesaurusId);
    }

    private void loadMissingArkIds() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isArkBatchGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            missingArkIds = Collections.emptyList();
            return;
        }
        List<String> ids = conceptRepository.findAllIdConceptsWithoutArk(thesaurusId);
        missingArkIds = ids == null ? Collections.emptyList() : List.copyOf(ids);
    }

    private String resolveArkId() {
        if (conceptSelectionContext.hasSelection()) {
            String fromSummary = conceptSelectionContext.getSummary().arkId();
            if (StringUtils.isNotBlank(fromSummary)) {
                return fromSummary;
            }
        }
        return resolveArkIdFromStore();
    }

    private String resolveArkIdFromStore() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String conceptId = requireConceptId();
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return "";
        }
        return conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId)
                .map(concept -> StringUtils.defaultString(concept.getIdArk()))
                .orElse("");
    }

    private String resolveArkProvider() {
        Preferences preferences = loadPreferences();
        if (preferences == null) {
            return "";
        }
        if (preferences.isUseOpenArk()) {
            return "openark";
        }
        if (preferences.isUseArkLocal()) {
            return "local";
        }
        if (preferences.isUseArk()) {
            return "ark";
        }
        return "";
    }

    public void finishAfterClose() {
        arkRun.reset();
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

    private void loadBranchConceptIds() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String conceptId = requireConceptId();
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            branchConceptIds = Collections.emptyList();
            return;
        }
        List<String> ids = branchConceptSupport.collectBranchConceptIds(thesaurusId, conceptId);
        branchConceptIds = ids == null ? Collections.emptyList() : List.copyOf(ids);
    }

    private void countBranchArk() {
        branchArkMissing = 0;
        branchArkExisting = 0;
        if (branchConceptIds == null || branchConceptIds.isEmpty()) {
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            branchArkMissing = branchConceptIds.size();
            return;
        }
        List<Object[]> rows = conceptRepository.findArkFromIdConcepts(
                Set.copyOf(branchConceptIds), thesaurusId);
        Set<String> withArk = new HashSet<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row == null || row.length < 2) {
                    continue;
                }
                String id = row[0] != null ? String.valueOf(row[0]) : "";
                String ark = row[1] != null ? String.valueOf(row[1]) : "";
                if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(ark)) {
                    withArk.add(id);
                }
            }
        }
        branchArkExisting = withArk.size();
        branchArkMissing = Math.max(0, branchConceptIds.size() - branchArkExisting);
    }

    private void loadAllConceptsForArk() {
        allArkMissing = 0;
        allArkExisting = 0;
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isArkBatchGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            allConceptIds = Collections.emptyList();
            return;
        }
        List<Concept> concepts = conceptRepository.findAllByIdThesaurusAndStatusNot(thesaurusId, "CA");
        if (CollectionUtils.isEmpty(concepts)) {
            allConceptIds = Collections.emptyList();
            return;
        }
        allConceptIds = concepts.stream().map(Concept::getIdConcept).toList();
        allArkExisting = (int) concepts.stream()
                .filter(concept -> StringUtils.isNotBlank(concept.getIdArk()))
                .count();
        allArkMissing = Math.max(0, allConceptIds.size() - allArkExisting);
    }

    private String resolveHandleId() {
        Preferences preferences = loadPreferences();
        if (preferences == null || !preferences.isUseHandle()) {
            return "";
        }
        ConceptIdentifiers identifiers = thesaurusBrowseBean.getSelectedConcept() != null
                ? thesaurusBrowseBean.getSelectedConcept().identifiers()
                : null;
        return identifiers != null ? StringUtils.defaultString(identifiers.permanentId()) : "";
    }

    /**
     * Lecture directe en BDD (comme legacy {@code roleOnThesaurus.nodePreference}),
     * sans cache V2 — pour que {@code use_openark} soit toujours à jour.
     */
    private Preferences loadPreferences() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        return preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
    }

    private String requireConceptId() {
        return conceptSelectionContext.hasSelection() ? conceptSelectionContext.getConceptId() : "";
    }
}
