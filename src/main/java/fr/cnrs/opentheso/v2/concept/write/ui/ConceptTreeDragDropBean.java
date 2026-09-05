package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReparentConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLifecycleWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLexicalWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptRelationWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptCollectionMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptRelationMutationService;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.UpdateFacetParentCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeKinds;

/**
 * Drag-and-drop + cut/paste de l'arbre concept V2 — équivalent de {@code dragAndDrop} legacy (même thésaurus).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptTreeDragDropBean")
@RequiredArgsConstructor
public class ConceptTreeDragDropBean implements Serializable {

    private static final String ACTION_NOT_ALLOWED = "Action non permise !!!";
    private static final String ERROR_TITLE = "Erreur";

    private final transient ConceptRelationMutationService conceptRelationMutationService;
    private final transient ConceptCollectionMutationService conceptCollectionMutationService;
    private final transient ConceptRelationWriteRepository conceptRelationWriteRepository;
    private final transient ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final transient ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    private final transient ConceptReadService conceptReadService;
    private final transient FacetMutationService facetMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient V2LocaleBean localeBean;
    private final transient ThesaurusBrowseBean thesaurusBrowseBean;
    private final transient ConceptSelectionContext conceptSelectionContext;

    private String dragConceptId;
    private String dragLabel;
    private String dropConceptId;
    private String dropLabel;
    private boolean dropToRoot;
    private boolean groupChangePending;
    private List<BroaderCutRow> broadersToCut = Collections.emptyList();
    private List<GroupToggleRow> groupsToCut = Collections.emptyList();
    private List<GroupToggleRow> groupsToAdd = Collections.emptyList();

    /** Presse-papiers cut/paste (même thésaurus), comme {@code dragAndDrop.copyOn}. */
    private boolean cutOn;
    private String cutConceptId;
    private String cutLabel;
    private String cutThesaurusId;
    private boolean cutWasTopConcept;

    /** Champs du formulaire HTML5 DnD (arbre V2). */
    private String htmlDragId;
    private String htmlDropId;
    private String htmlDragType;
    private String htmlDropType;
    private String htmlParentId;
    private String htmlDropRoot;

    /** Dernier nœud déplacé avec succès, pour révéler l'arbre V2. */
    private String lastMovedId;
    private String lastMovedType;
    private String pendingFacetDetachId;

    /** Toast V2 (évite le bandeau Faces « Information »). */
    private String flashMessage;
    private String flashToken;

    @Getter
    @Setter
    public static class BroaderCutRow implements Serializable {
        private String conceptId;
        private String title;
        private boolean selected = true;

        public BroaderCutRow(String conceptId, String title) {
            this.conceptId = conceptId;
            this.title = title;
        }
    }

    @Getter
    @Setter
    public static class GroupToggleRow implements Serializable {
        private String groupId;
        private String title;
        private boolean selected = true;

        public GroupToggleRow(String groupId, String title) {
            this.groupId = groupId;
            this.title = title;
        }
    }

    public boolean isDragDropEnabled() {
        return conceptWritePolicy.canMutateHierarchicalRelations(userSession, false);
    }

    public boolean isCutActionsAvailable() {
        return isDragDropEnabled();
    }

    public boolean isPasteUnderAvailable() {
        if (!cutOn || !isCutActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            return false;
        }
        if (!StringUtils.equals(cutThesaurusId, thesaurusContext.resolveThesaurusId())) {
            return false;
        }
        return !StringUtils.equalsIgnoreCase(cutConceptId, conceptSelectionContext.getConceptId());
    }

    public boolean isPasteAtRootAvailable() {
        if (!cutOn || !isCutActionsAvailable()) {
            return false;
        }
        if (!StringUtils.equals(cutThesaurusId, thesaurusContext.resolveThesaurusId())) {
            return false;
        }
        return !cutWasTopConcept;
    }

    public boolean isPasteConfirmPending() {
        return StringUtils.isNotBlank(dragConceptId)
                && (broadersToCut.size() >= 2 || groupChangePending);
    }

    public void onStartCut() {
        if (!isCutActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        cutConceptId = conceptSelectionContext.getConceptId();
        cutLabel = conceptSelectionContext.getSummary().preferredLabel();
        cutThesaurusId = thesaurusId;
        cutWasTopConcept = conceptLifecycleWriteRepository.isTopConcept(thesaurusId, cutConceptId);
        cutOn = true;
        flashSuccess(localeMsg("v2.tree.dnd.cut").replace("{0}", StringUtils.defaultString(cutLabel)));
    }

    public void pasteUnderCurrentConcept() {
        if (!isPasteUnderAvailable()) {
            MessageUtils.showWarnMessage(ACTION_NOT_ALLOWED);
            return;
        }
        preparePasteFromClipboard(
                conceptSelectionContext.getConceptId(),
                conceptSelectionContext.getSummary().preferredLabel(),
                false
        );
    }

    public void pasteAtRoot() {
        if (!isPasteAtRootAvailable()) {
            MessageUtils.showWarnMessage(ACTION_NOT_ALLOWED);
            return;
        }
        preparePasteFromClipboard(null, "Root", true);
    }

    public void cancelCut() {
        if (!cutOn) {
            return;
        }
        clearCutClipboard();
        flashSuccess(localeMsg("v2.tree.dnd.cutCancelled"));
    }

    private void preparePasteFromClipboard(String targetConceptId, String targetLabel, boolean toRoot) {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(cutConceptId)) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        lastMovedId = null;
        lastMovedType = null;
        resetDialogState();
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = thesaurusContext.resolveWorkLanguage();

        dragConceptId = cutConceptId;
        dragLabel = resolveLabel(dragConceptId, cutLabel, lang, thesaurusId);
        dropToRoot = toRoot;
        if (toRoot) {
            dropConceptId = null;
            dropLabel = "Root";
        } else {
            dropConceptId = targetConceptId;
            dropLabel = resolveLabel(dropConceptId, targetLabel, lang, thesaurusId);
        }

        broadersToCut = loadBroaderRows(dragConceptId, thesaurusId, lang);
        loadGroupRows(thesaurusId, lang);
        groupChangePending = isDroppedToAnotherGroup();
    }

    public void onHtmlTreeDrop() {
        lastMovedId = null;
        lastMovedType = null;
        handleDrop(
                htmlDragId,
                htmlDropId,
                htmlDragType,
                htmlDropType,
                htmlParentId,
                "1".equals(htmlDropRoot) || "true".equalsIgnoreCase(htmlDropRoot)
        );
    }

    public void handleDrop(
            String dragId,
            String dropId,
            String dragType,
            String dropType,
            String facetParentId,
            boolean toRoot
    ) {
        resetDialogState();
        if (!isDragDropEnabled()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        dragId = StringUtils.trimToNull(dragId);
        dropId = StringUtils.trimToNull(dropId);
        dragType = StringUtils.defaultString(dragType);
        dropType = StringUtils.defaultString(dropType);
        facetParentId = StringUtils.trimToNull(facetParentId);
        if (dragId == null || "root".equals(dragType) || "dummy".equals(dragType)) {
            return;
        }
        if (!toRoot && (dropId == null || dropId.equalsIgnoreCase(dragId))) {
            MessageUtils.showErrorMessage("Relation non permise !");
            return;
        }

        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }

        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = thesaurusContext.resolveWorkLanguage();

        if (isFacetType(dragType)) {
            handleFacetDrag(dragId, dropId, dropType, thesaurusId);
            return;
        }

        if (isFacetType(dropType)) {
            handleDropOntoFacet(dragId, dropId, thesaurusId);
            return;
        }

        pendingFacetDetachId = facetParentId;
        if (!prepareConceptDrop(dragId, dropId, dropType, toRoot, thesaurusId, lang)) {
            return;
        }
        if (broadersToCut.size() < 2 && !groupChangePending) {
            applyReparent(broadersToCut.stream().map(BroaderCutRow::getConceptId).toList(), userId, false);
        }
    }

    private boolean prepareConceptDrop(
            String dragId,
            String dropId,
            String dropType,
            boolean toRoot,
            String thesaurusId,
            String lang
    ) {
        dragConceptId = dragId;
        dragLabel = resolveLabel(dragConceptId, null, lang, thesaurusId);
        broadersToCut = loadBroaderRows(dragConceptId, thesaurusId, lang);

        dropToRoot = toRoot || isRootType(dropType);
        if (dropToRoot) {
            dropConceptId = null;
            dropLabel = "Root";
        } else if (isGroupType(dropType) || isFacetType(dropType)) {
            MessageUtils.showErrorMessage("Relation non permise !");
            resetDialogState();
            return false;
        } else {
            dropConceptId = dropId;
            dropLabel = resolveLabel(dropConceptId, null, lang, thesaurusId);
        }

        loadGroupRows(thesaurusId, lang);
        groupChangePending = isDroppedToAnotherGroup();
        return true;
    }

    public void submitDrop() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(dragConceptId)) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        List<String> toDetach = broadersToCut.stream()
                .filter(BroaderCutRow::isSelected)
                .map(BroaderCutRow::getConceptId)
                .toList();
        applyReparent(toDetach, userId, groupChangePending);
    }

    public void cancelDrop() {
        resetDialogState();
    }

    private void applyReparent(List<String> broaderIdsToDetach, int userId, boolean applyGroupChanges) {
        MutationResult result = conceptRelationMutationService.reparentConcept(new ReparentConceptCommand(
                thesaurusContext.resolveThesaurusId(),
                dragConceptId,
                broaderIdsToDetach,
                dropToRoot ? null : dropConceptId,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        ));
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : ERROR_TITLE);
            resetDialogState();
            return;
        }
        if (StringUtils.isNotBlank(pendingFacetDetachId) && StringUtils.isNotBlank(dragConceptId)) {
            detachFromFacetParentIfNeeded(
                    pendingFacetDetachId,
                    dragConceptId,
                    thesaurusContext.resolveThesaurusId());
        }
        if (applyGroupChanges) {
            applyCollectionChanges(userId);
        }
        String movedId = dragConceptId;
        String message = dropToRoot
                ? localeMsg("v2.tree.dnd.movedRoot").replace("{0}", StringUtils.defaultString(dragLabel))
                : localeMsg("v2.tree.dnd.moved")
                        .replace("{0}", StringUtils.defaultString(dragLabel))
                        .replace("{1}", StringUtils.defaultString(dropLabel));
        boolean wasCutPaste = cutOn && StringUtils.equalsIgnoreCase(cutConceptId, movedId);
        lastMovedId = movedId;
        lastMovedType = "concept";
        resetDialogState();
        if (wasCutPaste) {
            clearCutClipboard();
        }
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusBrowseBean.invalidateCollectionTree();
        thesaurusBrowseBean.openConcept(movedId, true);
        flashSuccess(message);
    }

    private void clearCutClipboard() {
        cutOn = false;
        cutConceptId = null;
        cutLabel = null;
        cutThesaurusId = null;
        cutWasTopConcept = false;
    }

    private void applyCollectionChanges(int userId) {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        for (GroupToggleRow group : groupsToCut) {
            if (group.isSelected()) {
                conceptCollectionMutationService.removeFromCollection(new RemoveConceptFromCollectionCommand(
                        thesaurusId, dragConceptId, userId, contributor, group.getGroupId(), true));
            } else {
                conceptCollectionMutationService.addToCollection(new AddConceptToCollectionCommand(
                        thesaurusId, dragConceptId, userId, contributor, group.getGroupId(), true));
            }
        }
        for (GroupToggleRow group : groupsToAdd) {
            if (group.isSelected()) {
                conceptCollectionMutationService.addToCollection(new AddConceptToCollectionCommand(
                        thesaurusId, dragConceptId, userId, contributor, group.getGroupId(), true));
            } else {
                conceptCollectionMutationService.removeFromCollection(new RemoveConceptFromCollectionCommand(
                        thesaurusId, dragConceptId, userId, contributor, group.getGroupId(), true));
            }
        }
    }

    private void loadGroupRows(String thesaurusId, String lang) {
        List<ConceptRelation> dragGroups = loadConceptCollections(thesaurusId, dragConceptId, lang);
        List<ConceptRelation> dropGroups = dropToRoot || StringUtils.isBlank(dropConceptId)
                ? List.of()
                : loadConceptCollections(thesaurusId, dropConceptId, lang);

        groupsToCut = dragGroups.stream()
                .map(g -> new GroupToggleRow(g.conceptId(), g.getDisplayLabel()))
                .toList();
        groupsToAdd = dropGroups.stream()
                .map(g -> new GroupToggleRow(g.conceptId(), g.getDisplayLabel()))
                .toList();
    }

    private List<ConceptRelation> loadConceptCollections(String thesaurusId, String conceptId, String lang) {
        return conceptReadService.loadDetail(thesaurusId, conceptId, lang)
                .map(detail -> detail.collections() == null ? List.<ConceptRelation>of() : detail.collections())
                .orElse(List.of());
    }

    /** Aligné sur legacy {@code DragAndDrop#isDroppedToAnotherGroup}. */
    private boolean isDroppedToAnotherGroup() {
        if (dropToRoot) {
            return false;
        }
        if (groupsToCut.isEmpty() && groupsToAdd.isEmpty()) {
            return false;
        }
        if (groupsToCut.isEmpty() || groupsToAdd.isEmpty()) {
            return true;
        }
        if (groupsToCut.size() > 1 || groupsToAdd.size() > 1) {
            return true;
        }
        return !groupsToCut.get(0).getGroupId().equalsIgnoreCase(groupsToAdd.get(0).getGroupId());
    }

    private void handleFacetDrag(String facetId, String dropId, String dropType, String thesaurusId) {
        if (StringUtils.isBlank(dropId) || isFacetType(dropType) || isRootType(dropType) || isGroupType(dropType)) {
            MessageUtils.showErrorMessage("Déplacement non permis !");
            return;
        }
        MutationResult result = facetMutationService.updateParent(new UpdateFacetParentCommand(
                thesaurusId, facetId, dropId));
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : ERROR_TITLE);
            return;
        }
        lastMovedId = facetId;
        lastMovedType = ConceptTreeNodeKinds.FACET;
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusBrowseBean.openFacet(facetId);
        flashSuccess(localeMsg("v2.tree.dnd.facetMoved"));
    }

    private void handleDropOntoFacet(String conceptId, String facetId, String thesaurusId) {
        List<String> children = conceptRelationWriteRepository.listNarrowerChildConceptIds(
                conceptId, thesaurusId);
        if (!children.isEmpty()) {
            MessageUtils.showWarnMessage(ACTION_NOT_ALLOWED);
            return;
        }
        MutationResult result = facetMutationService.addMember(new AddFacetMemberCommand(
                thesaurusId, facetId, conceptId, false));
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : ERROR_TITLE);
            return;
        }
        lastMovedId = facetId;
        lastMovedType = ConceptTreeNodeKinds.FACET;
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusBrowseBean.openFacet(facetId);
        flashSuccess(localeMsg("v2.tree.dnd.addedToFacet"));
    }

    private void detachFromFacetParentIfNeeded(String facetParentId, String conceptId, String thesaurusId) {
        if (StringUtils.isBlank(facetParentId) || StringUtils.isBlank(conceptId)) {
            return;
        }
        facetMutationService.removeMember(new RemoveFacetMemberCommand(
                thesaurusId, facetParentId, conceptId, false));
    }

    private static boolean isFacetType(String nodeType) {
        return ConceptTreeNodeKinds.FACET.equalsIgnoreCase(nodeType);
    }

    private static boolean isRootType(String nodeType) {
        return "root".equalsIgnoreCase(nodeType);
    }

    private static boolean isGroupType(String nodeType) {
        return "group".equalsIgnoreCase(nodeType) || "subGroup".equalsIgnoreCase(nodeType);
    }

    private List<BroaderCutRow> loadBroaderRows(String conceptId, String thesaurusId, String lang) {
        List<String> broaderIds = conceptRelationWriteRepository.listBroaderParentConceptIds(conceptId, thesaurusId);
        List<BroaderCutRow> rows = new ArrayList<>();
        for (String broaderId : broaderIds) {
            String title = conceptLexicalWriteRepository.findPreferredLabel(broaderId, thesaurusId, lang)
                    .filter(StringUtils::isNotBlank)
                    .orElse("(" + broaderId + ")");
            rows.add(new BroaderCutRow(broaderId, title));
        }
        return rows;
    }

    private String resolveLabel(String conceptId, String fallback, String lang, String thesaurusId) {
        return conceptLexicalWriteRepository.findPreferredLabel(conceptId, thesaurusId, lang)
                .filter(StringUtils::isNotBlank)
                .orElse(StringUtils.defaultIfBlank(fallback, "(" + conceptId + ")"));
    }

    private void resetDialogState() {
        dragConceptId = null;
        dragLabel = null;
        dropConceptId = null;
        dropLabel = null;
        dropToRoot = false;
        groupChangePending = false;
        broadersToCut = Collections.emptyList();
        groupsToCut = Collections.emptyList();
        groupsToAdd = Collections.emptyList();
        pendingFacetDetachId = null;
    }

    private void flashSuccess(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
    }

    private String localeMsg(String key) {
        return localeBean.getMsg(key);
    }
}
