package fr.cnrs.opentheso.collection.ui;

import fr.cnrs.opentheso.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.entites.ConceptGroupType;
import fr.cnrs.opentheso.repositories.ConceptGroupTypeRepository;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.collection.read.CollectionReadService;
import fr.cnrs.opentheso.collection.write.model.command.AddCollectionTranslationCommand;
import fr.cnrs.opentheso.collection.write.model.command.AddMemberToCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.CreateCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.CreateSubgroupCommand;
import fr.cnrs.opentheso.collection.write.model.command.DeleteCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.DeleteCollectionTranslationCommand;
import fr.cnrs.opentheso.collection.write.model.command.MoveCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.RemoveAllMembersFromCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.RemoveMemberFromCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.RenameCollectionLabelCommand;
import fr.cnrs.opentheso.collection.write.model.command.UpdateCollectionNotationCommand;
import fr.cnrs.opentheso.collection.write.model.command.UpdateCollectionTranslationCommand;
import fr.cnrs.opentheso.collection.write.model.command.UpdateCollectionTypeCommand;
import fr.cnrs.opentheso.collection.write.service.CollectionMutationService;
import fr.cnrs.opentheso.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.concept.model.ConceptNote;
import fr.cnrs.opentheso.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.concept.write.model.ConceptWriteCollection;
import fr.cnrs.opentheso.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.concept.write.model.MutationResult;
import fr.cnrs.opentheso.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.concept.write.service.ConceptWriteSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ViewScoped
@Named("collectionDetailEditorBean")
@RequiredArgsConstructor
public class CollectionDetailEditorBean implements Serializable {

    private static final ObjectMapper FICHE_JSON = new ObjectMapper();
    private static final Set<String> NOTE_TYPES = Set.of(
            "definition", "scopeNote", "example", "historyNote", "editorialNote", "changeNote", "note");
    private static final String FICHE_UPDATED = "La collection a bien été modifiée";

    private final transient CollectionMutationService collectionMutationService;
    private final transient CollectionReadService collectionReadService;
    private final transient ConceptWriteSearchService conceptWriteSearchService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;
    private final transient ConceptGroupTypeRepository conceptGroupTypeRepository;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient ThesaurusBrowseBean thesaurusBrowseBean;
    private final transient ConceptNoteMutationService conceptNoteMutationService;

    private String label;
    private String bulkLabel = "";
    private String bulkCollectionId = "";
    private String bulkConceptIds = "";
    private String bulkMessage = "";
    private boolean bulkOk;
    private String createMessage = "";
    private boolean createOk;
    private String createdGroupId = "";
    private String parentCollectionId = "";
    private String ficheGroupId = "";
    private String ficheMode = "";
    private String fichePayload = "";
    private String ficheMessage = "";
    private boolean ficheOk;
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
    private transient List<ConceptGroupType> groupTypes = Collections.emptyList();
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

    public java.util.List<fr.cnrs.opentheso.concept.write.model.ConceptWriteNoteType> getFicheNoteTypes() {
        return conceptWriteMetadataService.listNoteTypes();
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
        prepareAddBranchMember();
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

    public void submitFiche() {
        ficheOk = false;
        ficheMessage = "";
        if (!isManagerActionsAvailable()) {
            ficheMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            ficheMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        String thesaurusId = StringUtils.trimToEmpty(thesaurusContext.resolveThesaurusId());
        String groupId = StringUtils.trimToEmpty(ficheGroupId);
        if (StringUtils.isAnyBlank(thesaurusId, groupId)) {
            ficheMessage = "Collection introuvable !";
            return;
        }
        MutationResult result = switch (StringUtils.trimToEmpty(ficheMode)) {
            case "label" -> collectionMutationService.renamePreferredLabel(new RenameCollectionLabelCommand(
                    thesaurusId,
                    groupId,
                    thesaurusContext.resolveWorkLanguage(),
                    StringUtils.trimToEmpty(fichePayload),
                    userId));
            case "notation" -> collectionMutationService.updateNotation(new UpdateCollectionNotationCommand(
                    thesaurusId, groupId, StringUtils.trimToEmpty(fichePayload)));
            case "translations" -> saveFicheTranslations(thesaurusId, groupId, userId);
            case "notes" -> saveFicheNotes(thesaurusId, groupId, userId);
            case "members" -> saveFicheMembers(thesaurusId, groupId);
            default -> MutationResult.validationError("Modification inconnue.");
        };
        if (result == null) {
            ficheMessage = "Erreur";
            return;
        }
        ficheOk = result.success();
        ficheMessage = StringUtils.defaultString(result.message());
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
        handleMutation(MutationResult.ok("La collection a bien été modifiée"), "modifyCollectionDlg");
    }

    public void submitDelete() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        var group = thesaurusBrowseBean.getSelectedGroup();
        if (handleMutation(collectionMutationService.deleteCollection(new DeleteCollectionCommand(
                thesaurusContext.resolveThesaurusId(),
                group.groupId()
        )), "deleteCollectionDlg")) {
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
        )), "addCollectionTranslationDlg");
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
        )), "deleteCollectionTranslationDlg");
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
        )), applyToBranch ? "collectionAddBranchMemberDlg" : "addMemberToCollectionDlg");
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
        )), "collectionRemoveBranchMemberDlg");
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
        )), "removeAllMembersFromCollectionDlg");
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
        )), "moveCollectionDlg");
    }

    public void submitCreateFromSelection() {
        bulkOk = false;
        bulkMessage = "";
        if (!isManagerActionsAvailable()) {
            bulkMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            bulkMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        String name = StringUtils.trimToEmpty(bulkLabel);
        if (StringUtils.isBlank(name)) {
            bulkMessage = "Le nom de la collection est obligatoire.";
            return;
        }
        List<String> conceptIds = parseBulkConceptIds(bulkConceptIds);
        if (conceptIds.isEmpty()) {
            bulkMessage = "Aucun concept à rattacher.";
            return;
        }
        MutationResult result = collectionMutationService.createCollectionWithMembers(
                new CreateCollectionCommand(
                        thesaurusContext.resolveThesaurusId(),
                        thesaurusContext.resolveWorkLanguage(),
                        name,
                        "",
                        "MT",
                        userId
                ),
                conceptIds
        );
        if (result == null || !result.success()) {
            bulkMessage = result != null ? result.message() : "Erreur";
            return;
        }
        thesaurusBrowseBean.invalidateCollectionTree();
        bulkOk = true;
        bulkMessage = result.message();
        bulkLabel = "";
        bulkConceptIds = "";
    }

    public void submitAddSelectionToCollection() {
        bulkOk = false;
        bulkMessage = "";
        if (!isManagerActionsAvailable()) {
            bulkMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        String collectionId = StringUtils.trimToEmpty(bulkCollectionId);
        if (StringUtils.isBlank(collectionId)) {
            bulkMessage = "Choisissez une collection.";
            return;
        }
        List<String> conceptIds = parseBulkConceptIds(bulkConceptIds);
        if (conceptIds.isEmpty()) {
            bulkMessage = "Aucun concept à rattacher.";
            return;
        }
        MutationResult result = collectionMutationService.addConceptsToCollection(
                thesaurusContext.resolveThesaurusId(),
                collectionId,
                conceptIds
        );
        if (result == null || !result.success()) {
            bulkMessage = result != null ? result.message() : "Erreur";
            return;
        }
        thesaurusBrowseBean.invalidateCollectionTree();
        bulkOk = true;
        bulkMessage = result.message();
        bulkCollectionId = "";
        bulkConceptIds = "";
    }

    public void submitCreateFromView() {
        createOk = false;
        createMessage = "";
        createdGroupId = "";
        if (!isManagerActionsAvailable()) {
            createMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            createMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        if (StringUtils.isBlank(label)) {
            createMessage = "Le nom de la collection est obligatoire.";
            return;
        }
        if (StringUtils.isBlank(typeCode)) {
            typeCode = "MT";
        }
        String parentId = StringUtils.trimToEmpty(parentCollectionId);
        MutationResult result = StringUtils.isBlank(parentId)
                ? collectionMutationService.createCollection(new CreateCollectionCommand(
                        thesaurusContext.resolveThesaurusId(),
                        thesaurusContext.resolveWorkLanguage(),
                        label.trim(),
                        StringUtils.trimToEmpty(notation),
                        typeCode,
                        userId
                ))
                : collectionMutationService.createSubgroup(new CreateSubgroupCommand(
                        thesaurusContext.resolveThesaurusId(),
                        parentId,
                        thesaurusContext.resolveWorkLanguage(),
                        label.trim(),
                        StringUtils.trimToEmpty(notation),
                        typeCode,
                        userId
                ));
        if (result == null || !result.success()) {
            createMessage = result != null ? result.message() : "Erreur";
            return;
        }
        thesaurusBrowseBean.invalidateCollectionTree();
        createOk = true;
        createMessage = StringUtils.defaultIfBlank(result.message(), "Collection créée");
        createdGroupId = StringUtils.trimToEmpty(result.createdConceptId());
        if (StringUtils.isNotBlank(createdGroupId)) {
            thesaurusBrowseBean.focusGroup(createdGroupId);
        }
        parentCollectionId = "";
    }

    public void submitCreateRoot() {
        if (!isManagerActionsAvailable()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
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
        if (handleMutation(result, "createCollectionDlg") && StringUtils.isNotBlank(result.createdConceptId())) {
            thesaurusBrowseBean.focusGroup(result.createdConceptId());
        }
    }

    public void submitCreateSubgroup() {
        if (!canMutateSelectedCollection()) {
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
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
        if (handleMutation(result, "createSubgroupDlg") && StringUtils.isNotBlank(result.createdConceptId())) {
            thesaurusBrowseBean.focusGroup(result.createdConceptId());
        }
    }

    private boolean canMutateSelectedCollection() {
        if (!isManagerActionsAvailable() || thesaurusBrowseBean.getSelectedGroup() == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return false;
        }
        if (requireUserId() == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
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
                .map(GroupTranslationItem::lang)
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

    private MutationResult saveFicheTranslations(String thesaurusId, String groupId, int userId) {
        List<Map<String, String>> rows = readFicheRows();
        if (rows == null) {
            return MutationResult.validationError("Modification impossible.");
        }
        String workLang = StringUtils.defaultString(thesaurusContext.resolveWorkLanguage()).toLowerCase(Locale.ROOT);
        var detail = collectionReadService.loadDetail(thesaurusId, groupId, thesaurusContext.resolveWorkLanguage());
        if (detail.isEmpty()) {
            return MutationResult.validationError("Collection introuvable !");
        }
        Map<String, String> wanted = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String lang = StringUtils.trimToEmpty(row.get("lang")).toLowerCase(Locale.ROOT);
            String value = StringUtils.trimToEmpty(row.get("value"));
            if (StringUtils.isBlank(lang) && StringUtils.isBlank(value)) {
                continue;
            }
            if (StringUtils.isBlank(lang)) {
                return MutationResult.validationError("La langue est obligatoire !");
            }
            if (lang.equals(workLang) || !lang.matches("[a-z]{2,3}(-[a-z0-9]{2,8})?")) {
                return MutationResult.validationError("Langue inconnue.");
            }
            if (StringUtils.isNotBlank(value)) {
                wanted.put(lang, value);
            }
        }
        Set<String> existing = new LinkedHashSet<>();
        if (detail.get().translations() != null) {
            for (GroupTranslationItem item : detail.get().translations()) {
                if (item != null && StringUtils.isNotBlank(item.lang())) {
                    existing.add(item.lang().toLowerCase(Locale.ROOT));
                }
            }
        }
        for (String lang : existing) {
            if (wanted.containsKey(lang)) {
                continue;
            }
            MutationResult deleted = collectionMutationService.deleteTranslation(
                    new DeleteCollectionTranslationCommand(thesaurusId, groupId, lang));
            if (deleted == null || !deleted.success()) {
                return deleted == null ? MutationResult.failure("Erreur") : deleted;
            }
        }
        MutationResult last = MutationResult.ok(FICHE_UPDATED);
        for (Map.Entry<String, String> entry : wanted.entrySet()) {
            MutationResult step = existing.contains(entry.getKey())
                    ? collectionMutationService.updateTranslation(new UpdateCollectionTranslationCommand(
                    thesaurusId, groupId, entry.getKey(), entry.getValue(), userId))
                    : collectionMutationService.addTranslation(new AddCollectionTranslationCommand(
                    thesaurusId, groupId, entry.getKey(), entry.getValue()));
            if (step == null || !step.success()) {
                return step == null ? MutationResult.failure("Erreur") : step;
            }
            last = step;
        }
        return last;
    }

    private MutationResult saveFicheMembers(String thesaurusId, String groupId) {
        LinkedHashSet<String> wanted = new LinkedHashSet<>();
        for (String line : StringUtils.defaultString(fichePayload).split("\\R")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            int tab = line.indexOf('\t');
            String conceptId = (tab < 0 ? line : line.substring(0, tab)).trim();
            if (!conceptId.isEmpty()) {
                wanted.add(conceptId);
            }
        }
        var detail = collectionReadService.loadDetail(
                thesaurusId, groupId, thesaurusContext.resolveWorkLanguage());
        if (detail.isEmpty()) {
            return MutationResult.validationError("Collection introuvable !");
        }
        LinkedHashSet<String> existing = new LinkedHashSet<>();
        List<FacetMemberItem> members = detail.get().members();
        if (members != null) {
            for (FacetMemberItem member : members) {
                if (member != null && StringUtils.isNotBlank(member.conceptId())) {
                    existing.add(member.conceptId().trim());
                }
            }
        }
        MutationResult last = MutationResult.ok(FICHE_UPDATED);
        for (String conceptId : existing) {
            if (wanted.contains(conceptId)) {
                continue;
            }
            MutationResult deleted = collectionMutationService.removeMember(new RemoveMemberFromCollectionCommand(
                    thesaurusId, groupId, conceptId, false));
            if (deleted == null || !deleted.success()) {
                return deleted == null ? MutationResult.failure("Erreur") : deleted;
            }
            last = deleted;
        }
        for (String conceptId : wanted) {
            if (existing.contains(conceptId)) {
                continue;
            }
            MutationResult added = collectionMutationService.addMember(new AddMemberToCollectionCommand(
                    thesaurusId, groupId, conceptId, false));
            if (added == null || !added.success()) {
                return added == null ? MutationResult.failure("Erreur") : added;
            }
            last = added;
        }
        return last;
    }

    private MutationResult saveFicheNotes(String thesaurusId, String groupId, int userId) {
        List<FicheNoteLine> rows = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String line : StringUtils.defaultString(fichePayload).split("\\R")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String[] parts = line.split("\\t", 4);
            String type = parts.length > 0 ? StringUtils.trimToEmpty(parts[0]) : "";
            String lang = parts.length > 1 ? StringUtils.trimToEmpty(parts[1]).toLowerCase(Locale.ROOT) : "";
            String value = parts.length > 2 ? decodeNotePart(parts[2]) : "";
            String source = parts.length > 3 ? decodeNotePart(parts[3]) : "";
            if (type.isEmpty() && lang.isEmpty() && value.isEmpty()) {
                continue;
            }
            if (!knownNoteType(type)) {
                return MutationResult.validationError("Type de note inconnu.");
            }
            if (lang.isEmpty() || !lang.matches("[a-z]{2,3}(-[a-z0-9]{2,8})?")) {
                return MutationResult.validationError("Langue inconnue.");
            }
            if (value.isEmpty()) {
                continue;
            }
            if (!seen.add(type + "\0" + lang)) {
                return MutationResult.validationError("Chaque type ne peut apparaître qu'une fois par langue.");
            }
            rows.add(new FicheNoteLine(type, lang, value, source));
        }
        if (collectionReadService.loadDetail(
                thesaurusId, groupId, thesaurusContext.resolveWorkLanguage()).isEmpty()) {
            return MutationResult.validationError("Collection introuvable !");
        }
        List<ConceptNote> existing = collectionReadService.loadNotes(thesaurusId, groupId);
        for (ConceptNote note : existing) {
            if (note == null || seen.contains(noteKey(note.typeCode(), note.lang()))) {
                continue;
            }
            int noteId;
            try {
                noteId = Integer.parseInt(StringUtils.trimToEmpty(note.id()));
            } catch (NumberFormatException ex) {
                return MutationResult.validationError("Note introuvable !");
            }
            MutationResult deleted = conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                    thesaurusId, groupId, noteId, note.lang(), note.typeCode(), userId, contributorName()));
            if (deleted == null || !deleted.success()) {
                return deleted == null ? MutationResult.failure("Erreur") : deleted;
            }
        }
        MutationResult last = MutationResult.ok(FICHE_UPDATED);
        for (FicheNoteLine row : rows) {
            ConceptNote previous = existing.stream()
                    .filter(note -> note != null && noteKey(note.typeCode(), note.lang()).equals(noteKey(row.type(), row.lang())))
                    .findFirst()
                    .orElse(null);
            if (previous != null
                    && StringUtils.trimToEmpty(previous.value()).equals(row.value())
                    && StringUtils.trimToEmpty(previous.source()).equals(row.source())) {
                continue;
            }
            MutationResult step = conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                    thesaurusId, groupId, row.lang(), row.type(), row.value(), row.source(),
                    userId, contributorName()));
            if (step == null || !step.success()) {
                return step == null ? MutationResult.failure("Erreur") : step;
            }
            last = step;
        }
        return last;
    }

    private boolean knownNoteType(String type) {
        if (NOTE_TYPES.contains(type)) {
            return true;
        }
        var types = conceptWriteMetadataService.listNoteTypes();
        return types != null && types.stream()
                .anyMatch(item -> item != null && type.equals(item.code()));
    }

    private static String noteKey(String type, String lang) {
        return StringUtils.trimToEmpty(type) + "\0" + StringUtils.trimToEmpty(lang).toLowerCase(Locale.ROOT);
    }

    private static String decodeNotePart(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ex) {
            return raw.trim();
        }
    }

    private record FicheNoteLine(String type, String lang, String value, String source) {
    }

    private List<Map<String, String>> readFicheRows() {
        if (StringUtils.isBlank(fichePayload)) {
            return List.of();
        }
        String trimmed = fichePayload.trim();
        if (trimmed.startsWith("[")) {
            try {
                return FICHE_JSON.readValue(fichePayload, new TypeReference<List<Map<String, String>>>() {});
            } catch (JsonProcessingException ex) {
                return null;
            }
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : fichePayload.split("\\R")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String[] parts = line.split("\\t", 3);
            Map<String, String> row = new LinkedHashMap<>();
            row.put("lang", parts.length > 0 ? parts[0] : "");
            row.put("value", parts.length > 1 ? parts[1] : "");
            rows.add(row);
        }
        return rows;
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }

    private Integer requireUserId() {
        return userSession.getCurrentUserId();
    }

    private static List<String> parseBulkConceptIds(String raw) {
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String part : raw.split("[\\n\\r\\t,;]+")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }
}
