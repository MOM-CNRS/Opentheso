package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptTransferMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
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
@Named("v2ConceptTransferEditorBean")
@RequiredArgsConstructor
public class ConceptTransferEditorBean implements Serializable {

    private final ConceptTransferMutationService conceptTransferMutationService;
    private final ConceptWriteSearchService conceptWriteSearchService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusAccessService thesaurusAccessService;
    private final BranchConceptSupport branchConceptSupport;

    private String targetThesaurusId;
    private ConceptSearchSuggestion parentSearchSelected;
    private List<String> branchConceptIds = Collections.emptyList();
    private List<ConceptWriteThesaurusOption> availableThesauri = Collections.emptyList();

    public boolean isTransferActionsAvailable() {
        return conceptWritePolicy.canTransferConcept(userSession);
    }

    public void prepareMoveToAnotherThesaurus() {
        if (!conceptSelectionContext.hasSelection()) {
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String conceptId = conceptSelectionContext.getConceptId();
        branchConceptIds = branchConceptSupport.collectBranchConceptIds(thesaurusId, conceptId);
        targetThesaurusId = null;
        parentSearchSelected = null;
        loadAvailableThesauri();
    }

    public void onTargetThesaurusChange() {
        parentSearchSelected = null;
    }

    public List<ConceptSearchSuggestion> autocompleteParentConcept(String query) {
        if (StringUtils.isBlank(targetThesaurusId)) {
            return Collections.emptyList();
        }
        return conceptWriteSearchService.autocompleteRelationTarget(
                query,
                thesaurusContext.resolveWorkLanguage(),
                targetThesaurusId,
                true
        );
    }

    public void submitMoveToAnotherThesaurus() {
        if (!isTransferActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(targetThesaurusId) || branchConceptIds.isEmpty()) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }
        String parentConceptId = parentSearchSelected != null ? parentSearchSelected.conceptId() : null;
        var command = new MoveConceptToThesaurusCommand(
                thesaurusContext.resolveThesaurusId(),
                targetThesaurusId,
                conceptSelectionContext.getConceptId(),
                branchConceptIds,
                thesaurusContext.resolveWorkLanguage(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                parentConceptId
        );
        try {
            MutationResult result = conceptTransferMutationService.moveConceptToThesaurus(command);
            if (result == null || !result.success()) {
                MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
                return;
            }
            conceptNavigationSupport.invalidateConceptTree();
            conceptNavigationSupport.openThesaurusHome();
            PrimeFaces.current().ajax().update(":containerIndex:formRightTab :containerIndex:tabTree :messageIndex");
            MessageUtils.showInformationMessage(result.message());
            PrimeFaces.current().executeScript("PF('v2MoveToAnotherThesoDlg').hide();");
        } catch (RuntimeException exception) {
            MessageUtils.showErrorMessage("Le déplacement a échoué !");
        }
    }

    private void loadAvailableThesauri() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            availableThesauri = Collections.emptyList();
            return;
        }
        availableThesauri = conceptTransferMutationService.listAdminThesauri(
                userId,
                userSession.isSuperAdmin(),
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
}
