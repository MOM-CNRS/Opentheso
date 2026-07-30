package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.facet.read.FacetReadService;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.CreateFacetCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.DeleteFacetCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.DeleteFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveAllFacetMembersCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RenameFacetLabelCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.UpdateFacetParentCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.UpdateFacetTranslationCommand;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ViewScoped
@Named("v2FacetDetailEditorBean")
@RequiredArgsConstructor
public class FacetDetailEditorBean implements Serializable {

    private final FacetMutationService facetMutationService;
    private final FacetReadService facetReadService;
    private final ConceptWriteSearchService conceptWriteSearchService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String label;
    private String translationLang;
    private String translationValue;
    private String selectedTranslationLang;
    private ConceptSearchSuggestion selectedConcept;
    private ConceptSearchSuggestion selectedParentConcept;
    private boolean applyToBranch;
    private List<ConceptWriteLanguage> availableLanguages = Collections.emptyList();
    private List<ConceptWriteLanguage> availableTranslationLanguages = Collections.emptyList();
    private List<TranslationEditRow> translationsToEdit = Collections.emptyList();

    public static class TranslationEditRow implements Serializable {
        private String lang;
        private String value;

        public TranslationEditRow(String lang, String value) {
            this.lang = lang;
            this.value = value;
        }

        public String getLang() {
            return lang;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public boolean isManagerActionsAvailable() {
        return conceptWritePolicy.canMutateHierarchicalRelations(userSession, false);
    }

    public void prepareModify() {
        if (thesaurusBrowseBean.getSelectedFacet() == null) {
            return;
        }
        label = thesaurusBrowseBean.getSelectedFacet().label();
    }

    public void prepareAddTranslation() {
        loadLanguages();
        translationLang = firstAvailableTranslationLang();
        translationValue = "";
    }

    public void prepareEditTranslations() {
        if (thesaurusBrowseBean.getSelectedFacet() == null) {
            translationsToEdit = Collections.emptyList();
            return;
        }
        translationsToEdit = thesaurusBrowseBean.getSelectedFacet().translations().stream()
                .map(t -> new TranslationEditRow(t.lang(), t.value()))
                .toList();
    }

    public void prepareDeleteTranslation() {
        loadLanguages();
        selectedTranslationLang = thesaurusContext.resolveWorkLanguage();
    }

    public void prepareAddMember() {
        selectedConcept = null;
        applyToBranch = false;
    }

    public void prepareAddBranchMember() {
        selectedConcept = null;
        applyToBranch = true;
    }

    public void prepareRemoveBranchMember() {
        selectedConcept = null;
        applyToBranch = true;
    }

    public void prepareChangeParent() {
        selectedParentConcept = null;
    }

    public void prepareCreate() {
        loadLanguages();
        label = "";
        selectedParentConcept = null;
    }

    public List<ConceptSearchSuggestion> autocompleteConcept(String query) {
        return conceptWriteSearchService.autocompleteRelationTarget(
                query,
                thesaurusContext.resolveWorkLanguage(),
                thesaurusContext.resolveThesaurusId(),
                false
        );
    }

    public void submitModify() {
        if (!canMutateSelectedFacet()) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.renamePreferredLabel(new RenameFacetLabelCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                thesaurusContext.resolveWorkLanguage(),
                label
        )), "v2ModifyFacetDlg");
    }

    public void submitDelete() {
        if (!canMutateSelectedFacet()) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        if (handleMutation(facetMutationService.deleteFacet(new DeleteFacetCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId()
        )), "v2DeleteFacetDlg")) {
            thesaurusBrowseBean.invalidateConceptTree();
            thesaurusBrowseBean.openConcept(facet.parentConceptId());
        }
    }

    public void submitAddTranslation() {
        if (!canMutateSelectedFacet()) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.addTranslation(new AddFacetTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                translationLang,
                translationValue
        )), "v2AddFacetTranslationDlg");
    }

    public void submitUpdateTranslation(TranslationEditRow translation) {
        if (!canMutateSelectedFacet() || translation == null) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.updateTranslation(new UpdateFacetTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                translation.getLang(),
                translation.getValue()
        )), null);
        prepareEditTranslations();
    }

    public void submitDeleteTranslation() {
        if (!canMutateSelectedFacet()) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.deleteTranslation(new DeleteFacetTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                selectedTranslationLang
        )), "v2DeleteFacetTranslationDlg");
    }

    public void submitAddMember() {
        if (!canMutateSelectedFacet() || selectedConcept == null) {
            MessageUtils.showErrorMessage("Sélection invalide !");
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.addMember(new AddFacetMemberCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                selectedConcept.conceptId(),
                applyToBranch
        )), applyToBranch ? "v2FacetAddBranchMemberDlg" : "v2AddMemberToFacetDlg");
    }

    public void submitRemoveBranchMember() {
        if (!canMutateSelectedFacet() || selectedConcept == null) {
            MessageUtils.showErrorMessage("Sélection invalide !");
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.removeMember(new RemoveFacetMemberCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                selectedConcept.conceptId(),
                true
        )), "v2FacetRemoveBranchMemberDlg");
    }

    public void submitRemoveMember(String conceptId) {
        if (!canMutateSelectedFacet() || StringUtils.isBlank(conceptId)) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.removeMember(new RemoveFacetMemberCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                conceptId,
                false
        )), null);
    }

    public void submitRemoveAllMembers() {
        if (!canMutateSelectedFacet()) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.removeAllMembers(new RemoveAllFacetMembersCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId()
        )), "v2RemoveAllMembersFromFacetDlg");
    }

    public void submitChangeParent() {
        if (!canMutateSelectedFacet() || selectedParentConcept == null) {
            MessageUtils.showErrorMessage("Sélection invalide !");
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        handleMutation(facetMutationService.updateParent(new UpdateFacetParentCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                selectedParentConcept.conceptId()
        )), "v2ChangeFacetParentDlg");
    }

    public void submitCreate() {
        if (!isManagerActionsAvailable() || selectedParentConcept == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        MutationResult result = facetMutationService.createFacet(new CreateFacetCommand(
                thesaurusContext.resolveThesaurusId(),
                selectedParentConcept.conceptId(),
                thesaurusContext.resolveWorkLanguage(),
                label
        ));
        if (handleMutation(result, "v2CreateFacetDlg") && StringUtils.isNotBlank(result.createdConceptId())) {
            thesaurusBrowseBean.invalidateConceptTree();
            thesaurusBrowseBean.focusFacet(result.createdConceptId());
        }
    }

    private boolean canMutateSelectedFacet() {
        if (!isManagerActionsAvailable() || thesaurusBrowseBean.getSelectedFacet() == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return false;
        }
        return true;
    }

    private boolean handleMutation(MutationResult result, String dialogWidget) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return false;
        }
        refreshSelectedFacet();
        thesaurusBrowseBean.invalidateConceptTree();
        PrimeFaces.current().ajax().update(":containerIndex:rightTab :containerIndex:formLeftTab :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(dialogWidget)) {
            PrimeFaces.current().executeScript("PF('" + dialogWidget + "').hide();");
        }
        return true;
    }

    private void refreshSelectedFacet() {
        if (thesaurusBrowseBean.getSelectedFacet() == null) {
            return;
        }
        facetReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                thesaurusBrowseBean.getSelectedFacet().facetId(),
                thesaurusContext.resolveWorkLanguage()
        ).ifPresent(thesaurusBrowseBean::setSelectedFacet);
    }

    private void loadLanguages() {
        availableLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
        Set<String> usedLangs = thesaurusBrowseBean.getSelectedFacet() != null
                ? thesaurusBrowseBean.getSelectedFacet().translations().stream()
                .map(translation -> translation.lang())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new))
                : new HashSet<>();
        usedLangs.add(thesaurusContext.resolveWorkLanguage());
        availableTranslationLanguages = availableLanguages.stream()
                .filter(lang -> !usedLangs.contains(lang.code()))
                .toList();
    }

    private String firstAvailableTranslationLang() {
        if (availableTranslationLanguages.isEmpty()) {
            return thesaurusContext.resolveWorkLanguage();
        }
        return availableTranslationLanguages.get(0).code();
    }
}
