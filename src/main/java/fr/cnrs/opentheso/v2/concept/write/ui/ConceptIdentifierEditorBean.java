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

    private final ConceptIdentifierMutationService conceptIdentifierMutationService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final PreferencesRepository preferencesRepository;
    private final BranchConceptSupport branchConceptSupport;
    private final ThesaurusBrowseBean thesaurusBrowseBean;
    private final ConceptRepository conceptRepository;

    private String currentConceptId;
    private String currentConceptLabel;
    private String currentArkId;
    private String arkProvider = "";
    private String currentHandleId;
    private String arkRunState = "";
    private String errorMessage;
    private String flashMessage;
    private String flashToken;
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

    public boolean isMissingArkSubmitReady() {
        return !isMissingArkEmpty() && !"done".equals(arkRunState);
    }

    public boolean isBranchArkEmpty() {
        return getBranchConceptCount() == 0;
    }

    public int getBranchConceptCount() {
        return branchConceptIds == null ? 0 : branchConceptIds.size();
    }

    public boolean isBranchArkSubmitReady() {
        return !isBranchArkEmpty() && !"done".equals(arkRunState);
    }

    public boolean isAllArkEmpty() {
        return getAllConceptCount() == 0;
    }

    public int getAllConceptCount() {
        return allConceptIds == null ? 0 : allConceptIds.size();
    }

    public boolean isAllArkSubmitReady() {
        return !isAllArkEmpty() && !"done".equals(arkRunState);
    }

    public void prepareGenerateArk() {
        errorMessage = null;
        arkRunState = "";
        refreshContext();
    }

    public void prepareGenerateMissingArk() {
        errorMessage = null;
        arkRunState = "";
        missingArkCreated = 0;
        refreshContext();
        loadMissingArkIds();
    }

    public void prepareDeleteArk() {
        refreshContext();
    }

    public void prepareGenerateArkForBranch() {
        errorMessage = null;
        arkRunState = "";
        branchProcessed = 0;
        refreshContext();
        loadBranchConceptIds();
        countBranchArk();
    }

    public void prepareGenerateAllArk() {
        errorMessage = null;
        arkRunState = "";
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
        errorMessage = null;
        if (!isArkGenerationAvailable() || !conceptSelectionContext.hasSelection()) {
            errorMessage = "Action non autorisée";
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
            arkRunState = "error";
            errorMessage = result != null ? result.message() : "La génération Ark a échoué";
            return false;
        }
        currentArkId = resolveArkIdFromStore();
        arkRunState = "done";
        String arkSuffix = StringUtils.isBlank(currentArkId) ? "" : " : " + currentArkId;
        flashSuccess((updating ? "Identifiant Ark mis à jour" : "Identifiant Ark créé") + arkSuffix);
        return true;
    }

    public boolean submitGenerateMissingArk() {
        errorMessage = null;
        if (!isArkBatchGenerationAvailable()) {
            arkRunState = "error";
            errorMessage = "Action non autorisée";
            return false;
        }
        loadMissingArkIds();
        if (missingArkIds.isEmpty()) {
            arkRunState = "error";
            errorMessage = "Aucun concept sans identifiant Ark";
            return false;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                missingArkIds
        );
        MutationResult result = conceptIdentifierMutationService.generateArk(command);
        if (result == null || !result.success()) {
            arkRunState = "error";
            errorMessage = result != null ? result.message() : "La génération Ark a échoué";
            return false;
        }
        missingArkCreated = missingArkIds.size();
        arkRunState = "done";
        if (result.warning()) {
            errorMessage = result.message();
        }
        loadMissingArkIds();
        flashSuccess(missingArkCreated == 1
                ? "1 identifiant Ark créé"
                : missingArkCreated + " identifiants Ark créés");
        return true;
    }

    public void submitDeleteArk() {
        if (!isArkDeletionAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new DeleteArkCommand(
                thesaurusContext.resolveThesaurusId(),
                List.of(requireConceptId())
        );
        handleMutationResult(conceptIdentifierMutationService.deleteArk(command));
    }

    public boolean submitGenerateArkForBranch() {
        errorMessage = null;
        if (!isArkBatchGenerationAvailable() || !conceptSelectionContext.hasSelection()) {
            arkRunState = "error";
            errorMessage = "Action non autorisée";
            return false;
        }
        loadBranchConceptIds();
        countBranchArk();
        if (branchConceptIds.isEmpty()) {
            arkRunState = "error";
            errorMessage = "Aucun concept dans cette branche";
            return false;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                branchConceptIds
        );
        MutationResult result = conceptIdentifierMutationService.generateArk(command);
        if (result == null || !result.success()) {
            arkRunState = "error";
            errorMessage = result != null ? result.message() : "La génération Ark a échoué";
            return false;
        }
        branchProcessed = branchConceptIds.size();
        arkRunState = "done";
        if (result.warning()) {
            errorMessage = result.message();
        }
        countBranchArk();
        flashSuccess(branchProcessed == 1
                ? "1 concept de la branche traité"
                : branchProcessed + " concepts de la branche traités");
        return true;
    }

    public void submitGenerateArkForConceptsWithoutArk() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isArkBatchGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        List<String> conceptIds = conceptRepository.findAllIdConceptsWithoutArk(thesaurusId);
        if (CollectionUtils.isEmpty(conceptIds)) {
            MessageUtils.showInformationMessage("Aucun concept sans ARK");
            return;
        }
        submitGenerateArkForConcepts(conceptIds, true);
    }

    public boolean submitGenerateAllArk() {
        errorMessage = null;
        if (!isArkBatchGenerationAvailable()) {
            arkRunState = "error";
            errorMessage = "Action non autorisée";
            return false;
        }
        loadAllConceptsForArk();
        if (allConceptIds.isEmpty()) {
            arkRunState = "error";
            errorMessage = "Aucun concept dans ce thésaurus";
            return false;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                allConceptIds
        );
        MutationResult result = conceptIdentifierMutationService.generateArk(command);
        if (result == null || !result.success()) {
            arkRunState = "error";
            errorMessage = result != null ? result.message() : "La génération Ark a échoué";
            return false;
        }
        allProcessed = allConceptIds.size();
        arkRunState = "done";
        if (result.warning()) {
            errorMessage = result.message();
        }
        loadAllConceptsForArk();
        flashSuccess(allProcessed == 1
                ? "1 concept du thésaurus traité"
                : allProcessed + " concepts du thésaurus traités");
        return true;
    }

    public void submitGenerateHandle() {
        submitGenerateHandleForConcepts(List.of(requireConceptId()));
    }

    public void submitDeleteHandle() {
        if (!isHandleDeletionAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
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
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        List<String> conceptIds = conceptRepository.findAllIdsWithoutHandle(thesaurusId);
        if (CollectionUtils.isEmpty(conceptIds)) {
            MessageUtils.showInformationMessage("Aucun concept sans Handle");
            return;
        }
        submitGenerateHandleForConcepts(conceptIds);
    }

    public void submitGenerateAllHandle() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isHandleGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        submitGenerateHandleForConcepts(loadAllConceptIds(thesaurusId));
    }

    private void submitGenerateArkForConcepts(List<String> conceptIds, boolean allowed) {
        if (!allowed || conceptIds == null || conceptIds.isEmpty()) {
            MessageUtils.showErrorMessage("Action non autorisée");
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
            MessageUtils.showErrorMessage("Action non autorisée");
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
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
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

    private void flashSuccess(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
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
