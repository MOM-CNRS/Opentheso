package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
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
    private final ConceptLifecycleMutationService conceptLifecycleMutationService;
    private final ConceptNoteMutationService conceptNoteMutationService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String label;
    private String definition;
    private String parentConceptLabel;
    private String translationLang;
    private String translationValue;
    private String selectedTranslationLang;
    private ConceptSearchSuggestion selectedConcept;
    private ConceptSearchSuggestion selectedParentConcept;
    private boolean applyToBranch;
    private List<ConceptWriteLanguage> availableLanguages = Collections.emptyList();
    private List<ConceptWriteLanguage> availableTranslationLanguages = Collections.emptyList();
    private List<TranslationEditRow> translationsToEdit = Collections.emptyList();

    /** Création d'un concept membre sous la facette (équivalent legacy addNTFacette). */
    private String childPreferredLabel;
    private String childNotation;
    private String childCustomConceptId;
    private String childSource;
    private String childSelectedGroupId;
    private String childNarrowerRelationType = "NT";
    private boolean childDuplicateLabelWarning;
    private List<ConceptWriteCollection> availableCollections = Collections.emptyList();
    private List<ConceptWriteNtRelationType> ntRelationTypes = Collections.emptyList();

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

    /**
     * Prépare la création d'un nouveau concept rattaché au parent de la facette puis ajouté comme membre.
     */
    public void prepareAddChildUnderFacet() {
        if (thesaurusBrowseBean.getSelectedFacet() == null) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        parentConceptLabel = StringUtils.isNotBlank(facet.parentConceptLabel())
                ? facet.parentConceptLabel()
                : "(" + facet.parentConceptId() + ")";
        childPreferredLabel = "";
        childNotation = "";
        childCustomConceptId = "";
        childSource = "";
        childNarrowerRelationType = "NT";
        childDuplicateLabelWarning = false;
        availableCollections = conceptWriteMetadataService.listCollections(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
        ntRelationTypes = conceptWriteMetadataService.listNtRelationTypes();
        childSelectedGroupId = "";
    }

    public void submitAddChildUnderFacet() {
        submitAddChildUnderFacetInternal(false);
    }

    public void submitAddChildUnderFacetForced() {
        submitAddChildUnderFacetInternal(true);
    }

    public void cancelChildDuplicate() {
        childDuplicateLabelWarning = false;
    }

    private void submitAddChildUnderFacetInternal(boolean forced) {
        if (!canMutateSelectedFacet()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (StringUtils.isBlank(childPreferredLabel)) {
            MessageUtils.showWarnMessage("le label est obligatoire !");
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        var command = new AddChildConceptCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.parentConceptId(),
                thesaurusContext.resolveWorkLanguage(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                childPreferredLabel,
                childNotation,
                childCustomConceptId,
                childSource,
                StringUtils.trimToNull(childSelectedGroupId),
                StringUtils.defaultIfBlank(childNarrowerRelationType, "NT"),
                forced
        );
        MutationResult createResult = conceptLifecycleMutationService.addChildConcept(command);
        if (createResult.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            childDuplicateLabelWarning = true;
            MessageUtils.showWarnMessage(createResult.message());
            return;
        }
        childDuplicateLabelWarning = false;
        if (!createResult.success() || StringUtils.isBlank(createResult.createdConceptId())) {
            MessageUtils.showErrorMessage(createResult.message() != null ? createResult.message() : "Erreur");
            return;
        }
        MutationResult memberResult = facetMutationService.addMember(new AddFacetMemberCommand(
                thesaurusContext.resolveThesaurusId(),
                facet.facetId(),
                createResult.createdConceptId(),
                false
        ));
        if (handleMutation(memberResult, "v2AddChildUnderFacetDlg")) {
            childPreferredLabel = "";
            childNotation = "";
            childCustomConceptId = "";
            childSource = "";
        }
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
        parentConceptLabel = "";
        if (thesaurusBrowseBean.getSelectedFacet() == null) {
            return;
        }
        var facet = thesaurusBrowseBean.getSelectedFacet();
        parentConceptLabel = StringUtils.isNotBlank(facet.parentConceptLabel())
                ? facet.parentConceptLabel()
                : "(" + facet.parentConceptId() + ")";
    }

    public void prepareCreate() {
        loadLanguages();
        label = "";
        definition = "";
        parentConceptLabel = "";
        selectedParentConcept = null;
    }

    /**
     * Création d'une facette sous le concept courant (menu contextuel fiche concept, comme legacy).
     */
    public void prepareCreateUnderCurrentConcept() {
        label = "";
        definition = "";
        parentConceptLabel = "";
        selectedParentConcept = null;
        var concept = thesaurusBrowseBean.getSelectedConcept();
        if (concept == null || concept.summary() == null || StringUtils.isBlank(concept.summary().conceptId())) {
            return;
        }
        String conceptId = concept.summary().conceptId();
        String preferredLabel = StringUtils.defaultString(concept.summary().preferredLabel());
        parentConceptLabel = StringUtils.isNotBlank(preferredLabel) ? preferredLabel : "(" + conceptId + ")";
        selectedParentConcept = new ConceptSearchSuggestion(conceptId, preferredLabel, "", false);
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
        if (!isManagerActionsAvailable() || selectedParentConcept == null
                || StringUtils.isBlank(selectedParentConcept.conceptId())) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (StringUtils.isBlank(label)) {
            MessageUtils.showErrorMessage("Le libellé est obligatoire !");
            return;
        }
        MutationResult result = facetMutationService.createFacet(new CreateFacetCommand(
                thesaurusContext.resolveThesaurusId(),
                selectedParentConcept.conceptId(),
                thesaurusContext.resolveWorkLanguage(),
                label
        ));
        String dialogWidget = StringUtils.isNotBlank(parentConceptLabel)
                ? "v2CreateFacetUnderConceptDlg"
                : "v2CreateFacetDlg";
        if (handleMutation(result, dialogWidget) && StringUtils.isNotBlank(result.createdConceptId())) {
            persistOptionalDefinition(result.createdConceptId());
            thesaurusBrowseBean.invalidateConceptTree();
            thesaurusBrowseBean.focusFacet(result.createdConceptId());
            label = "";
            definition = "";
            parentConceptLabel = "";
            selectedParentConcept = null;
        }
    }

    private void persistOptionalDefinition(String facetId) {
        if (StringUtils.isBlank(definition) || StringUtils.isBlank(facetId)) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                facetId,
                thesaurusContext.resolveWorkLanguage(),
                "definition",
                definition,
                null,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        ));
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
