package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
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
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.TreeDragDropEvent;
import org.primefaces.model.TreeNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Drag-and-drop + cut/paste de l'arbre concept V2 — équivalent de {@code dragAndDrop} legacy (même thésaurus).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptTreeDragDropBean")
@RequiredArgsConstructor
public class ConceptTreeDragDropBean implements Serializable {

    private final ConceptRelationMutationService conceptRelationMutationService;
    private final ConceptCollectionMutationService conceptCollectionMutationService;
    private final ConceptRelationWriteRepository conceptRelationWriteRepository;
    private final ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    private final ConceptReadService conceptReadService;
    private final FacetMutationService facetMutationService;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ThesaurusBrowseBean thesaurusBrowseBean;
    private final ConceptSelectionContext conceptSelectionContext;

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

    public void onStartCut() {
        if (!isCutActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        cutConceptId = conceptSelectionContext.getConceptId();
        cutLabel = conceptSelectionContext.getSummary().preferredLabel();
        cutThesaurusId = thesaurusId;
        cutWasTopConcept = conceptLifecycleWriteRepository.isTopConcept(thesaurusId, cutConceptId);
        cutOn = true;
        MessageUtils.showInformationMessage(
                "Couper " + StringUtils.defaultString(cutLabel) + " (" + cutConceptId + ")");
    }

    public void pasteUnderCurrentConcept() {
        if (!isPasteUnderAvailable()) {
            MessageUtils.showWarnMessage("Action non permise !!!");
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
            MessageUtils.showWarnMessage("Action non permise !!!");
            return;
        }
        preparePasteFromClipboard(null, "Root", true);
    }

    public void cancelCut() {
        clearCutClipboard();
        MessageUtils.showInformationMessage("Déplacement annulé ");
    }

    private void preparePasteFromClipboard(String targetConceptId, String targetLabel, boolean toRoot) {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(cutConceptId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
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

        if (broadersToCut.size() < 2 && !groupChangePending) {
            applyReparent(broadersToCut.stream().map(BroaderCutRow::getConceptId).toList(), userId, false);
        } else {
            PrimeFaces.current().ajax().update(":containerIndex:v2DragAndDropDlg");
            PrimeFaces.current().executeScript("PF('v2DragAndDropDlg').show();");
        }
    }

    public void onDragDrop(TreeDragDropEvent event) {
        resetDialogState();
        if (!isDragDropEnabled()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            rollbackTree();
            return;
        }
        TreeNode dragNode = event.getDragNode();
        TreeNode dropNode = event.getDropNode();
        if (dragNode == null || dropNode == null
                || !(dragNode.getData() instanceof ConceptTreeNodeData dragData)
                || dragData.isDummy()
                || "root".equals(dragData.nodeType())) {
            rollbackTree();
            return;
        }

        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            rollbackTree();
            return;
        }

        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = thesaurusContext.resolveWorkLanguage();

        if ("facet".equals(dragData.nodeType())) {
            handleFacetDrag(dragData, dropNode, thesaurusId);
            return;
        }

        if (dropNode.getData() instanceof ConceptTreeNodeData dropData
                && "facet".equals(dropData.nodeType())) {
            handleDropOntoFacet(dragData, dropData, thesaurusId);
            return;
        }

        detachFromFacetParentIfNeeded(dragNode, thesaurusId);

        dragConceptId = dragData.nodeId();
        dragLabel = resolveLabel(dragConceptId, dragData.label(), lang, thesaurusId);
        broadersToCut = loadBroaderRows(dragConceptId, thesaurusId, lang);

        ConceptTreeNodeData dropData = dropNode.getData() instanceof ConceptTreeNodeData d ? d : null;
        dropToRoot = dropData == null || "root".equals(dropData.nodeType()) || dropNode.getParent() == null;
        if (dropToRoot) {
            dropConceptId = null;
            dropLabel = "Root";
        } else {
            if (dropData.isGroup() || "facet".equals(dropData.nodeType())) {
                MessageUtils.showErrorMessage("Relation non permise !");
                rollbackTree();
                return;
            }
            dropConceptId = dropData.nodeId();
            dropLabel = resolveLabel(dropConceptId, dropData.label(), lang, thesaurusId);
        }

        loadGroupRows(thesaurusId, lang);
        groupChangePending = isDroppedToAnotherGroup();

        if (broadersToCut.size() < 2 && !groupChangePending) {
            applyReparent(broadersToCut.stream().map(BroaderCutRow::getConceptId).toList(), userId, false);
        } else {
            PrimeFaces.current().ajax().update(":containerIndex:v2DragAndDropDlg");
            PrimeFaces.current().executeScript("PF('v2DragAndDropDlg').show();");
        }
    }

    public void submitDrop() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(dragConceptId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        List<String> toDetach = broadersToCut.stream()
                .filter(BroaderCutRow::isSelected)
                .map(BroaderCutRow::getConceptId)
                .toList();
        applyReparent(toDetach, userId, true);
        PrimeFaces.current().executeScript("PF('v2DragAndDropDlg').hide();");
    }

    public void cancelDrop() {
        rollbackTree();
        resetDialogState();
        PrimeFaces.current().executeScript("PF('v2DragAndDropDlg').hide();");
        MessageUtils.showInformationMessage("Déplacement annulé ");
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
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            rollbackTree();
            resetDialogState();
            return;
        }
        if (applyGroupChanges) {
            applyCollectionChanges(userId);
        }
        String movedId = dragConceptId;
        String message = dropToRoot
                ? dragLabel + " -> Root"
                : dragLabel + " -> " + dropLabel;
        boolean wasCutPaste = cutOn && StringUtils.equalsIgnoreCase(cutConceptId, movedId);
        resetDialogState();
        if (wasCutPaste) {
            clearCutClipboard();
        }
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusBrowseBean.invalidateCollectionTree();
        thesaurusBrowseBean.openConcept(movedId, true);
        PrimeFaces.current().ajax().update(
                ":containerIndex:formLeftTab",
                ":containerIndex:formRightTab",
                ":messageIndex"
        );
        MessageUtils.showInformationMessage(message);
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

    private void handleFacetDrag(ConceptTreeNodeData dragData, TreeNode dropNode, String thesaurusId) {
        if (!(dropNode.getData() instanceof ConceptTreeNodeData dropData)
                || dropData.isDummy()
                || "root".equals(dropData.nodeType())
                || "facet".equals(dropData.nodeType())
                || dropData.isGroup()) {
            MessageUtils.showErrorMessage("Déplacement non permis !");
            rollbackTree();
            return;
        }
        MutationResult result = facetMutationService.updateParent(new UpdateFacetParentCommand(
                thesaurusId, dragData.nodeId(), dropData.nodeId()));
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            rollbackTree();
            return;
        }
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusBrowseBean.openFacet(dragData.nodeId());
        PrimeFaces.current().ajax().update(
                ":containerIndex:formLeftTab",
                ":containerIndex:formRightTab",
                ":messageIndex"
        );
        MessageUtils.showInformationMessage("Facette déplacée avec succès");
    }

    private void handleDropOntoFacet(ConceptTreeNodeData dragData, ConceptTreeNodeData dropData, String thesaurusId) {
        List<String> children = conceptRelationWriteRepository.listNarrowerChildConceptIds(
                dragData.nodeId(), thesaurusId);
        if (!children.isEmpty()) {
            MessageUtils.showWarnMessage("Action non permise !!!");
            rollbackTree();
            return;
        }
        MutationResult result = facetMutationService.addMember(new AddFacetMemberCommand(
                thesaurusId, dropData.nodeId(), dragData.nodeId(), false));
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            rollbackTree();
            return;
        }
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusBrowseBean.openFacet(dropData.nodeId());
        PrimeFaces.current().ajax().update(
                ":containerIndex:formLeftTab",
                ":containerIndex:formRightTab",
                ":messageIndex"
        );
        MessageUtils.showInformationMessage("Concept ajouté à la facette");
    }

    private void detachFromFacetParentIfNeeded(TreeNode dragNode, String thesaurusId) {
        TreeNode parent = dragNode.getParent();
        if (parent == null || !(parent.getData() instanceof ConceptTreeNodeData parentData)) {
            return;
        }
        if (!"facet".equals(parentData.nodeType())) {
            return;
        }
        if (!(dragNode.getData() instanceof ConceptTreeNodeData dragData)) {
            return;
        }
        facetMutationService.removeMember(new RemoveFacetMemberCommand(
                thesaurusId, parentData.nodeId(), dragData.nodeId(), false));
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

    private void rollbackTree() {
        thesaurusBrowseBean.invalidateConceptTree();
        PrimeFaces.current().ajax().update(":containerIndex:formLeftTab", ":messageIndex");
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
    }
}
