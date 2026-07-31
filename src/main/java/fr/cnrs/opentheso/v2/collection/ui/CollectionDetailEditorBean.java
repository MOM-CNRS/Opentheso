package fr.cnrs.opentheso.v2.collection.ui;

import fr.cnrs.opentheso.entites.ConceptGroupType;
import fr.cnrs.opentheso.repositories.ConceptGroupTypeRepository;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.collection.read.CollectionReadService;
import fr.cnrs.opentheso.v2.collection.write.model.command.AddCollectionTranslationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.AddMemberToCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.CreateCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.CreateSubgroupCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.DeleteCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.DeleteCollectionTranslationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.MoveCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RemoveAllMembersFromCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RemoveMemberFromCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RenameCollectionLabelCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionNotationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionTranslationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionTypeCommand;
import fr.cnrs.opentheso.v2.collection.write.service.CollectionMutationService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ViewScoped
@Named("v2CollectionDetailEditorBean")
@RequiredArgsConstructor
public class CollectionDetailEditorBean implements Serializable {

    private final CollectionMutationService collectionMutationService;
    private final CollectionReadService collectionReadService;
    private final ConceptWriteSearchService conceptWriteSearchService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final ConceptGroupTypeRepository conceptGroupTypeRepository;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String label;
    private String notation;
    private String typeCode = "MT";
    private String translationLang;
    private String translationValue;
    private String selectedTranslationLang;
    private ConceptSearchSuggestion selectedConcept;
    private ConceptWriteCollection selectedParentCollection;
    private boolean moveToRoot;
    private boolean applyToBranch;
    private List<ConceptWriteLanguage> availableLanguages = Collections.emptyList();
    private List<ConceptWriteLanguage> availableTranslationLanguages = Collections.emptyList();
    private List<ConceptGroupType> groupTypes = Collections.emptyList();
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
        loadGroupTypes();
        if (thesaurusBrowseBean.getSelectedGroup() == null) {
            return;
        }
        label = thesaurusBrowseBean.getSelectedGroup().label();
        notation = thesaurusBrowseBean.getSelectedGroup().notation();
        typeCode = StringUtils.defaultIfBlank(thesaurusBrowseBean.getSelectedGroup().typeCode(), "MT");
    }

    public void prepareAddTranslation() {
        loadLanguages();
        translationLang = firstAvailableTranslationLang();
        translationValue = "";
    }

    public void prepareEditTranslations() {
        if (thesaurusBrowseBean.getSelectedGroup() == null) {
            translationsToEdit = Collections.emptyList();
            return;
        }
        translationsToEdit = thesaurusBrowseBean.getSelectedGroup().translations().stream()
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

    public void prepareMove() {
        moveToRoot = false;
        selectedParentCollection = null;
    }

    public void prepareCreateRoot() {
        loadGroupTypes();
        loadLanguages();
        label = "";
        notation = "";
        typeCode = "MT";
    }

    public void prepareCreateSubgroup() {
        prepareCreateRoot();
    }

    public List<ConceptSearchSuggestion> autocompleteConcept(String query) {
        return conceptWriteSearchService.autocompleteRelationTarget(
                query,
                thesaurusContext.resolveWorkLanguage(),
                thesaurusContext.resolveThesaurusId(),
                false
        );
    }

    public List<ConceptWriteCollection> autocompleteCollection(String query) {
        return conceptWriteSearchService.autocompleteCollection(
                query,
                thesaurusContext.resolveWorkLanguage(),
                thesaurusContext.resolveThesaurusId()
        );
    }

    public void submitModify() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        MutationResult renameResult = collectionMutationService.renamePreferredLabel(new RenameCollectionLabelCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                thesaurusContext.resolveWorkLanguage(),
                label,
                requireUserId()
        ));
        if (renameResult == null || !renameResult.success()) {
            MessageUtils.showErrorMessage(renameResult != null ? renameResult.message() : "Erreur");
            return;
        }
        if (StringUtils.isNotBlank(typeCode)) {
            MutationResult typeResult = collectionMutationService.updateType(new UpdateCollectionTypeCommand(
                    thesaurusContext.resolveThesaurusId(),
                    group.groupId(),
                    typeCode
            ));
            if (typeResult != null && !typeResult.success()) {
                MessageUtils.showErrorMessage(typeResult.message());
                return;
            }
        }
        if (StringUtils.isNotBlank(notation)) {
            MutationResult notationResult = collectionMutationService.updateNotation(new UpdateCollectionNotationCommand(
                    thesaurusContext.resolveThesaurusId(),
                    group.groupId(),
                    notation
            ));
            if (notationResult != null && !notationResult.success()) {
                MessageUtils.showErrorMessage(notationResult.message());
                return;
            }
        }
        handleMutation(MutationResult.ok("La collection a bien été modifiée"), "v2ModifyCollectionDlg");
    }

    public void submitDelete() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        if (handleMutation(collectionMutationService.deleteCollection(new DeleteCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId()
        )), "v2DeleteCollectionDlg")) {
            thesaurusBrowseBean.invalidateCollectionTree();
            thesaurusBrowseBean.openThesaurusHome();
        }
    }

    public void submitAddTranslation() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.addTranslation(new AddCollectionTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                translationLang,
                translationValue
        )), "v2AddCollectionTranslationDlg");
    }

    public void submitUpdateTranslation(TranslationEditRow translation) {
        if (!canMutateSelectedCollection() || translation == null) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.updateTranslation(new UpdateCollectionTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                translation.getLang(),
                translation.getValue(),
                requireUserId()
        )), null);
        prepareEditTranslations();
    }

    public void submitDeleteTranslation() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.deleteTranslation(new DeleteCollectionTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                selectedTranslationLang
        )), "v2DeleteCollectionTranslationDlg");
    }

    public void submitAddMember() {
        if (!canMutateSelectedCollection() || selectedConcept == null) {
            MessageUtils.showErrorMessage("Sélection invalide !");
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.addMember(new AddMemberToCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                selectedConcept.conceptId(),
                applyToBranch
        )), applyToBranch ? "v2CollectionAddBranchMemberDlg" : "v2AddMemberToCollectionDlg");
    }

    public void submitRemoveBranchMember() {
        if (!canMutateSelectedCollection() || selectedConcept == null) {
            MessageUtils.showErrorMessage("Sélection invalide !");
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.removeMember(new RemoveMemberFromCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                selectedConcept.conceptId(),
                true
        )), "v2CollectionRemoveBranchMemberDlg");
    }

    public void submitRemoveMember(String conceptId) {
        if (!canMutateSelectedCollection() || StringUtils.isBlank(conceptId)) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.removeMember(new RemoveMemberFromCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                conceptId,
                false
        )), null);
    }

    public void submitRemoveAllMembers() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.removeAllMembers(new RemoveAllMembersFromCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId()
        )), "v2RemoveAllMembersFromCollectionDlg");
    }

    public void submitMove() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        handleMutation(collectionMutationService.moveCollection(new MoveCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                selectedParentCollection != null ? selectedParentCollection.id() : null,
                moveToRoot
        )), "v2MoveCollectionDlg");
    }

    public void submitCreateRoot() {
        if (!isManagerActionsAvailable()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        MutationResult result = collectionMutationService.createCollection(new CreateCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                label,
                notation,
                typeCode,
                userId
        ));
        if (handleMutation(result, "v2CreateCollectionDlg") && StringUtils.isNotBlank(result.createdConceptId())) {
            thesaurusBrowseBean.focusGroup(result.createdConceptId());
        }
    }

    public void submitCreateSubgroup() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        MutationResult result = collectionMutationService.createSubgroup(new CreateSubgroupCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId(),
                thesaurusContext.resolveWorkLanguage(),
                label,
                notation,
                typeCode,
                userId
        ));
        if (handleMutation(result, "v2CreateSubgroupDlg") && StringUtils.isNotBlank(result.createdConceptId())) {
            thesaurusBrowseBean.focusGroup(result.createdConceptId());
        }
    }

    private boolean canMutateSelectedCollection() {
        if (!isManagerActionsAvailable() || thesaurusBrowseBean.getSelectedGroup() == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return false;
        }
        if (requireUserId() == null) {
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
        refreshSelectedCollection();
        thesaurusBrowseBean.invalidateCollectionTree();
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :containerIndex:formLeftTab :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(dialogWidget)) {
            PrimeFaces.current().executeScript("PF('" + dialogWidget + "').hide();");
        }
        return true;
    }

    private void refreshSelectedCollection() {
        if (thesaurusBrowseBean.getSelectedGroup() == null) {
            return;
        }
        collectionReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                thesaurusBrowseBean.getSelectedGroup().groupId(),
                thesaurusContext.resolveWorkLanguage()
        ).ifPresent(thesaurusBrowseBean::setSelectedGroup);
    }

    private void loadLanguages() {
        availableLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
        Set<String> usedLangs = thesaurusBrowseBean.getSelectedGroup() != null
                ? thesaurusBrowseBean.getSelectedGroup().translations().stream()
                .map(translation -> translation.lang())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new))
                : new HashSet<>();
        usedLangs.add(thesaurusContext.resolveWorkLanguage());
        availableTranslationLanguages = availableLanguages.stream()
                .filter(lang -> !usedLangs.contains(lang.code()))
                .toList();
    }

    private void loadGroupTypes() {
        groupTypes = conceptGroupTypeRepository.findAll();
    }

    private String firstAvailableTranslationLang() {
        if (availableTranslationLanguages.isEmpty()) {
            return thesaurusContext.resolveWorkLanguage();
        }
        return availableTranslationLanguages.get(0).code();
    }

    private Integer requireUserId() {
        return userSession.getCurrentUserId();
    }
}
