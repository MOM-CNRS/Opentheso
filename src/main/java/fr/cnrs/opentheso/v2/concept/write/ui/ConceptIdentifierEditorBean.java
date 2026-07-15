package fr.cnrs.opentheso.v2.concept.write.ui;

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
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
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
@Named("v2ConceptIdentifierEditorBean")
@RequiredArgsConstructor
public class ConceptIdentifierEditorBean implements Serializable {

    private final ConceptIdentifierMutationService conceptIdentifierMutationService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ThesaurusAccessService thesaurusAccessService;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final BranchConceptSupport branchConceptSupport;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentConceptLabel;
    private String currentHandleId;
    private List<String> branchConceptIds = Collections.emptyList();

    public boolean isIdentifierActionsAvailable() {
        return ConceptWritePolicy.canMutateIdentifiers(userSession, canManageCurrentThesaurus());
    }

    public boolean isArkGenerationAvailable() {
        ThesaurusPreferences preferences = loadPreferences();
        return isIdentifierActionsAvailable()
                && preferences != null
                && (preferences.useArk() || preferences.useArkLocal() || preferences.useOpenArk());
    }

    public boolean isArkDeletionAvailable() {
        ThesaurusPreferences preferences = loadPreferences();
        return isIdentifierActionsAvailable() && preferences != null && preferences.useOpenArk();
    }

    public boolean isHandleGenerationAvailable() {
        ThesaurusPreferences preferences = loadPreferences();
        return isIdentifierActionsAvailable() && preferences != null && preferences.useHandle();
    }

    public boolean isHandleDeletionAvailable() {
        return isHandleGenerationAvailable() && StringUtils.isNotBlank(currentHandleId);
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
        submitGenerateArkForConcepts(List.of(requireConceptId()));
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
        submitGenerateArkForConcepts(branchConceptIds);
    }

    public void submitGenerateHandle() {
        submitGenerateHandleForConcepts(List.of(requireConceptId()));
    }

    public void submitDeleteHandle() {
        if (!isHandleDeletionAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
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
        submitGenerateHandleForConcepts(branchConceptIds);
    }

    private void submitGenerateArkForConcepts(List<String> conceptIds) {
        if (!isArkGenerationAvailable() || conceptIds.isEmpty()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new GenerateArkCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                conceptIds
        );
        handleMutationResult(conceptIdentifierMutationService.generateArk(command), null);
    }

    private void submitGenerateHandleForConcepts(List<String> conceptIds) {
        if (!isHandleGenerationAvailable() || conceptIds.isEmpty()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new GenerateHandleCommand(thesaurusContext.resolveThesaurusId(), conceptIds);
        handleMutationResult(conceptIdentifierMutationService.generateHandle(command), null);
    }

    private void handleMutationResult(MutationResult result, String dialogWidget) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return;
        }
        conceptNavigationSupport.refreshSelectedConcept();
        PrimeFaces.current().ajax().update(":containerIndex:conceptSummaryPanel :messageIndex");
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
        ThesaurusPreferences preferences = loadPreferences();
        if (preferences == null || !preferences.useHandle()) {
            return "";
        }
        ConceptIdentifiers identifiers = thesaurusBrowseBean.getSelectedConcept() != null
                ? thesaurusBrowseBean.getSelectedConcept().identifiers()
                : null;
        return identifiers != null ? StringUtils.defaultString(identifiers.permanentId()) : "";
    }

    private ThesaurusPreferences loadPreferences() {
        return thesaurusPreferenceService.loadPreferencesOrNull(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    private boolean canManageCurrentThesaurus() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        return thesaurusAccessService.canManageThesaurus(
                userId,
                userSession.isSuperAdmin(),
                thesaurusContext.resolveThesaurusId()
        );
    }

    private String requireConceptId() {
        return conceptSelectionContext.hasSelection() ? conceptSelectionContext.getConceptId() : "";
    }
}
