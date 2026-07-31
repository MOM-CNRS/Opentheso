package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptCollectionMutationService;
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
@Named("v2ConceptCollectionEditorBean")
@RequiredArgsConstructor
public class ConceptCollectionEditorBean implements Serializable {

    private final ConceptCollectionMutationService conceptCollectionMutationService;
    private final ConceptWriteSearchService conceptWriteSearchService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentConceptLabel;
    private ConceptWriteCollection selectedCollection;
    private boolean applyToBranch;
    private List<ConceptRelation> collectionsToRemove = Collections.emptyList();

    public boolean isCollectionActionsAvailable() {
        return conceptWritePolicy.canMutateConceptAttributes(userSession, isSelectedDeprecated());
    }

    public boolean isHasCollections() {
        if (thesaurusBrowseBean.getSelectedConcept() == null) {
            return false;
        }
        List<ConceptRelation> collections = thesaurusBrowseBean.getSelectedConcept().collections();
        return collections != null && !collections.isEmpty();
    }

    public void prepareAddToCollection() {
        resetCollectionForm();
        refreshCurrentConceptLabel();
    }

    public void prepareAddBranchToCollection() {
        resetCollectionForm();
        applyToBranch = true;
        refreshCurrentConceptLabel();
    }

    public void prepareRemoveFromCollection() {
        refreshCurrentConceptLabel();
        applyToBranch = false;
        reloadCollectionsToRemove();
    }

    public void prepareRemoveBranchFromCollection() {
        refreshCurrentConceptLabel();
        applyToBranch = true;
        reloadCollectionsToRemove();
    }

    public List<ConceptWriteCollection> autocompleteCollection(String query) {
        return conceptWriteSearchService.autocompleteCollection(
                query,
                thesaurusContext.resolveWorkLanguage(),
                thesaurusContext.resolveThesaurusId()
        );
    }

    public void submitAddToCollection() {
        Integer userId = requireUserId();
        if (userId == null || !isCollectionActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (selectedCollection == null || StringUtils.isBlank(selectedCollection.id())) {
            MessageUtils.showErrorMessage("Aucune sélection !!");
            return;
        }
        var command = new AddConceptToCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                selectedCollection.id(),
                applyToBranch
        );
        if (handleMutationResult(conceptCollectionMutationService.addToCollection(command),
                applyToBranch ? "v2AddBranchToCollectionDlg" : "v2AddToCollectionDlg")) {
            resetCollectionForm();
        }
    }

    public void submitRemoveFromCollection(ConceptRelation collection) {
        Integer userId = requireUserId();
        if (userId == null || collection == null || !isCollectionActionsAvailable()
                || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new RemoveConceptFromCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                collection.conceptId(),
                applyToBranch
        );
        if (handleMutationResult(conceptCollectionMutationService.removeFromCollection(command), null)) {
            reloadCollectionsToRemove();
        }
    }

    private void reloadCollectionsToRemove() {
        if (thesaurusBrowseBean.getSelectedConcept() == null) {
            collectionsToRemove = Collections.emptyList();
            return;
        }
        List<ConceptRelation> collections = thesaurusBrowseBean.getSelectedConcept().collections();
        collectionsToRemove = collections != null ? List.copyOf(collections) : Collections.emptyList();
    }

    private void resetCollectionForm() {
        selectedCollection = null;
        applyToBranch = false;
    }

    private boolean handleMutationResult(MutationResult result, String dialogWidget) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return false;
        }
        conceptNavigationSupport.refreshSelectedConcept();
        thesaurusBrowseBean.invalidateCollectionTree();
        reloadCollectionsToRemove();
        PrimeFaces.current().ajax().update(
                ":containerIndex:formRightTab",
                ":containerIndex:formLeftTab",
                ":messageIndex"
        );
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(dialogWidget)) {
            PrimeFaces.current().executeScript("PF('" + dialogWidget + "').hide();");
        }
        return true;
    }

    private void refreshCurrentConceptLabel() {
        currentConceptLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
    }

    private Integer requireUserId() {
        return userSession.getCurrentUserId();
    }

    private String contributorName() {
        return org.apache.commons.lang3.StringUtils.defaultString(userSession.getCurrentUsername());
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(org.apache.commons.lang3.StringUtils.trimToEmpty(
                conceptSelectionContext.getSummary().status()));
    }
}
