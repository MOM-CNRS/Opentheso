package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteFacet;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
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
@Named("v2ConceptFacetMembershipBean")
@RequiredArgsConstructor
public class ConceptFacetMembershipBean implements Serializable {

    private final FacetMutationService facetMutationService;
    private final ConceptWriteSearchService conceptWriteSearchService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentConceptLabel;
    private ConceptWriteFacet selectedFacet;
    private List<ConceptRelation> facetsToRemove = Collections.emptyList();

    public boolean isFacetActionsAvailable() {
        return conceptWritePolicy.canMutateHierarchicalRelations(userSession, false);
    }

    public void prepareAddToFacet() {
        selectedFacet = null;
        refreshCurrentConceptLabel();
    }

    public void prepareRemoveFromFacets() {
        refreshCurrentConceptLabel();
        reloadFacetsToRemove();
    }

    public List<ConceptWriteFacet> autocompleteFacet(String query) {
        return conceptWriteSearchService.autocompleteFacet(
                query,
                thesaurusContext.resolveWorkLanguage(),
                thesaurusContext.resolveThesaurusId()
        );
    }

    public void submitAddToFacet() {
        if (!isFacetActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (selectedFacet == null || StringUtils.isBlank(selectedFacet.id())) {
            MessageUtils.showErrorMessage("Aucune sélection !!");
            return;
        }
        MutationResult result = facetMutationService.addMember(new AddFacetMemberCommand(
                thesaurusContext.resolveThesaurusId(),
                selectedFacet.id(),
                conceptSelectionContext.getConceptId(),
                false
        ));
        if (handleMutationResult(result, "v2AddConceptToFacetDlg")) {
            selectedFacet = null;
        }
    }

    public void submitRemoveFromFacet(ConceptRelation facet) {
        if (!isFacetActionsAvailable() || facet == null || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        MutationResult result = facetMutationService.removeMember(new RemoveFacetMemberCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.conceptId(),
                conceptSelectionContext.getConceptId(),
                false
        ));
        if (handleMutationResult(result, null)) {
            reloadFacetsToRemove();
        }
    }

    private void reloadFacetsToRemove() {
        if (thesaurusBrowseBean.getSelectedConcept() == null) {
            facetsToRemove = Collections.emptyList();
            return;
        }
        List<ConceptRelation> facets = thesaurusBrowseBean.getSelectedConcept().facets();
        facetsToRemove = facets != null ? List.copyOf(facets) : Collections.emptyList();
    }

    private boolean handleMutationResult(MutationResult result, String dialogWidget) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return false;
        }
        conceptNavigationSupport.refreshSelectedConcept();
        thesaurusBrowseBean.invalidateConceptTree();
        reloadFacetsToRemove();
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
}
