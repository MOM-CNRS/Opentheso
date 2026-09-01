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
import java.util.List;

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
    private List<String> branchConceptIds = Collections.emptyList();

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

    public void prepareGenerateArk() {
        errorMessage = null;
        arkRunState = "";
        refreshContext();
    }

    public void prepareDeleteArk() {
        refreshContext();
    }

    public void prepareGenerateArkForBranch() {
        refreshContext();
        loadBranchConceptIds();
    }

    public void prepareGenerateAllArk() {
        refreshContext();
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

    public void submitGenerateArkForBranch() {
        if (branchConceptIds.isEmpty()) {
            loadBranchConceptIds();
        }
        submitGenerateArkForConcepts(branchConceptIds, isArkGenerationAvailable());
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

    public void submitGenerateAllArk() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isArkBatchGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        List<String> conceptIds = loadAllConceptIds(thesaurusId);
        submitGenerateArkForConcepts(conceptIds, true);
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
        branchConceptIds = branchConceptSupport.collectBranchConceptIds(thesaurusId, conceptId);
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
