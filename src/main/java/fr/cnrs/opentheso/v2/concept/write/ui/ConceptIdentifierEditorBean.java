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
import org.primefaces.PrimeFaces;

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

    private String currentConceptLabel;
    private String currentHandleId;
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

    public void prepareGenerateArk() {
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

    public void submitGenerateArk() {
        submitGenerateArkForConcepts(List.of(requireConceptId()), isArkGenerationAvailable(), "v2GenerateArkDlg");
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
        handleMutationResult(conceptIdentifierMutationService.deleteArk(command), "v2DeleteArkDlg");
    }

    public void submitGenerateArkForBranch() {
        if (branchConceptIds.isEmpty()) {
            loadBranchConceptIds();
        }
        submitGenerateArkForConcepts(branchConceptIds, isArkGenerationAvailable(), "v2GenerateArkBranchDlg");
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
        submitGenerateArkForConcepts(conceptIds, true, null);
    }

    public void submitGenerateAllArk() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isArkBatchGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        List<String> conceptIds = loadAllConceptIds(thesaurusId);
        submitGenerateArkForConcepts(conceptIds, true, "v2GenerateAllArkDlg");
    }

    public void submitGenerateHandle() {
        submitGenerateHandleForConcepts(List.of(requireConceptId()), "v2GenerateHandleDlg");
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
        handleMutationResult(conceptIdentifierMutationService.deleteHandle(command), "v2DeleteHandleDlg");
    }

    public void submitGenerateHandleForBranch() {
        if (branchConceptIds.isEmpty()) {
            loadBranchConceptIds();
        }
        submitGenerateHandleForConcepts(branchConceptIds, "v2GenerateHandleBranchDlg");
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
        submitGenerateHandleForConcepts(conceptIds, null);
    }

    public void submitGenerateAllHandle() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (!isHandleGenerationAvailable() || StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        submitGenerateHandleForConcepts(loadAllConceptIds(thesaurusId), null);
    }

    private void submitGenerateArkForConcepts(List<String> conceptIds, boolean allowed, String dialogWidget) {
        if (!allowed || conceptIds == null || conceptIds.isEmpty()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                conceptIds
        );
        handleMutationResult(conceptIdentifierMutationService.generateArk(command), dialogWidget);
    }

    private void submitGenerateHandleForConcepts(List<String> conceptIds, String dialogWidget) {
        if (!isHandleGenerationAvailable() || conceptIds == null || conceptIds.isEmpty()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new GenerateHandleCommand(thesaurusContext.resolveThesaurusId(), conceptIds);
        handleMutationResult(conceptIdentifierMutationService.generateHandle(command), dialogWidget);
    }

    private List<String> loadAllConceptIds(String thesaurusId) {
        List<Concept> concepts = conceptRepository.findAllByIdThesaurusAndStatusNot(thesaurusId, "CA");
        if (CollectionUtils.isEmpty(concepts)) {
            return Collections.emptyList();
        }
        return concepts.stream().map(Concept::getIdConcept).toList();
    }

    private void handleMutationResult(MutationResult result, String dialogWidget) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return;
        }
        conceptNavigationSupport.refreshSelectedConcept();
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(dialogWidget)) {
            PrimeFaces.current().executeScript("PF('" + dialogWidget + "').hide();");
        }
    }

    private void refreshContext() {
        currentConceptLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
        currentHandleId = resolveHandleId();
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
