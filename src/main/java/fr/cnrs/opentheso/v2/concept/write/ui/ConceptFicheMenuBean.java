package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.facet.ui.FacetDetailEditorBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Actions du menu contextuel à côté du fil d'Ariane (consultation V2).
 * Délègue aux beans d'écriture existants puis recharge la fiche / l'arbre V2.
 */
@Getter
@ViewScoped
@Named("v2ConceptFicheMenuBean")
@RequiredArgsConstructor
public class ConceptFicheMenuBean implements Serializable {

    private final ThesaurusViewBean thesaurusViewBean;
    private final ThesaurusBrowseBean thesaurusBrowseBean;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptLifecycleEditorBean lifecycleEditorBean;
    private final ConceptTreeDragDropBean treeDragDropBean;
    private final ConceptMaintenanceEditorBean maintenanceEditorBean;
    private final ConceptIdentifierEditorBean identifierEditorBean;
    private final FacetDetailEditorBean facetDetailEditorBean;
    private final ConceptAttributeEditorBean attributeEditorBean;
    private final ConceptTypeManagerBean typeManagerBean;
    private final ConceptTransferEditorBean transferEditorBean;
    private final ConceptCopyBetweenThesaurusBean copyBetweenThesaurusBean;
    private final ConceptListCsvImportBean listCsvImportBean;

    private String dialogToReopen;

    public void prepareAddChild() {
        lifecycleEditorBean.prepareAddChild();
        openDialog("cvDlgAddNt");
    }

    public void submitAddChild() {
        lifecycleEditorBean.submitAddChild();
        if (lifecycleEditorBean.isDuplicateLabelWarning()) {
            openDialog("cvDlgAddNt");
            return;
        }
        closeDialogs();
        refreshConsult();
    }

    public void submitAddChildForced() {
        lifecycleEditorBean.submitAddChildForced();
        if (lifecycleEditorBean.isDuplicateLabelWarning()) {
            openDialog("cvDlgAddNt");
            return;
        }
        closeDialogs();
        refreshConsult();
    }

    public void prepareCreateFacet() {
        facetDetailEditorBean.prepareCreateUnderCurrentConcept();
        if (facetDetailEditorBean.getSelectedParentConcept() == null && conceptSelectionContext.hasSelection()) {
            var summary = conceptSelectionContext.getSummary();
            String conceptId = summary.conceptId();
            String preferredLabel = StringUtils.defaultString(summary.preferredLabel());
            facetDetailEditorBean.setSelectedParentConcept(
                    new fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion(
                            conceptId, preferredLabel, "", false));
            facetDetailEditorBean.setParentConceptLabel(
                    StringUtils.isNotBlank(preferredLabel) ? preferredLabel : "(" + conceptId + ")");
        }
        openDialog("cvDlgAddFacet");
    }

    public void submitCreateFacet() {
        facetDetailEditorBean.submitCreate();
        closeDialogs();
        var facet = thesaurusBrowseBean.getSelectedFacet();
        thesaurusViewBean.reloadTree();
        if (facet != null && StringUtils.isNotBlank(facet.facetId())) {
            thesaurusViewBean.openTreeNode(facet.facetId(), "facet");
        } else {
            refreshConsult();
        }
    }

    public void prepareEditConceptType() {
        attributeEditorBean.prepareEditConceptType();
        openDialog("cvDlgEditType");
    }

    public void submitEditConceptType() {
        attributeEditorBean.submitUpdateConceptType();
        closeDialogs();
        refreshConsult();
    }

    public void prepareManageConceptTypes() {
        typeManagerBean.prepareManage();
        openDialog("cvDlgManageTypes");
    }

    public void addConceptType() {
        typeManagerBean.addNewConceptType();
        openDialog("cvDlgManageTypes");
    }

    public void deleteConceptType(fr.cnrs.opentheso.models.concept.NodeConceptType type) {
        typeManagerBean.prepareDelete(type);
        typeManagerBean.deleteCustomRelationship();
        openDialog("cvDlgManageTypes");
    }

    public void cutConcept() {
        treeDragDropBean.onStartCut();
        closeDialogs();
    }

    public void cancelCut() {
        treeDragDropBean.cancelCut();
        closeDialogs();
    }

    public void prepareCancelCut() {
        if (!treeDragDropBean.isCutOn()) {
            closeDialogs();
            return;
        }
        openDialog("cvDlgCancelCut");
    }

    public void pasteUnder() {
        treeDragDropBean.pasteUnderCurrentConcept();
        if (StringUtils.isNotBlank(treeDragDropBean.getDragConceptId())) {
            openDialog("cvDlgPaste");
            return;
        }
        closeDialogs();
    }

    public void pasteAtRoot() {
        treeDragDropBean.pasteAtRoot();
        if (StringUtils.isNotBlank(treeDragDropBean.getDragConceptId())) {
            openDialog("cvDlgPaste");
            return;
        }
        closeDialogs();
    }

    public void dropFromTree() {
        treeDragDropBean.onHtmlTreeDrop();
        if (treeDragDropBean.isPasteConfirmPending()) {
            String dragId = treeDragDropBean.getDragConceptId();
            if (StringUtils.isNotBlank(dragId)) {
                thesaurusViewBean.openTreeNode(dragId, "concept");
            }
            openDialog("cvDlgPaste");
            return;
        }
        closeDialogs();
        refreshAfterTreeMove();
    }

    public void submitPaste() {
        treeDragDropBean.submitDrop();
        closeDialogs();
        refreshAfterTreeMove();
    }

    public void cancelPaste() {
        treeDragDropBean.cancelDrop();
        closeDialogs();
    }

    public void prepareDelete() {
        lifecycleEditorBean.prepareDelete();
        openDialog("cvDlgDelete");
    }

    public void submitDelete() {
        lifecycleEditorBean.submitDelete();
        closeDialogs();
        refreshConsult();
    }

    public void prepareDeprecate() {
        lifecycleEditorBean.prepareDeprecate();
        openDialog("cvDlgDeprecate");
    }

    public void submitDeprecate() {
        lifecycleEditorBean.submitDeprecate();
        closeDialogs();
        refreshConsult();
    }

    public void prepareApprove() {
        lifecycleEditorBean.prepareApprove();
        openDialog("cvDlgApprove");
    }

    public void submitApprove() {
        lifecycleEditorBean.submitApprove();
        closeDialogs();
        refreshConsult();
    }

    public void prepareCopyToAnotherThesaurus() {
        copyBetweenThesaurusBean.prepareCopyToAnotherThesaurus();
        openDialog("cvDlgCopyTheso");
    }

    public void onCopyTargetThesaurusChange() {
        copyBetweenThesaurusBean.onTargetThesaurusChange();
    }

    public void submitCopyToAnotherThesaurus() {
        boolean ok = copyBetweenThesaurusBean.submitCopyToAnotherThesaurus();
        if (ok) {
            closeDialogs();
            refreshConsult();
            return;
        }
        openDialog("cvDlgCopyTheso");
    }

    public void prepareMoveToAnotherThesaurus() {
        transferEditorBean.prepareMoveToAnotherThesaurus();
        openDialog("cvDlgMoveTheso");
    }

    public void onMoveTargetThesaurusChange() {
        transferEditorBean.onTargetThesaurusChange();
    }

    public void submitMoveToAnotherThesaurus() {
        boolean ok = transferEditorBean.submitMoveToAnotherThesaurus();
        if (ok) {
            closeDialogs();
            conceptSelectionContext.clear();
            thesaurusViewBean.refreshFromSelectionContext();
            thesaurusViewBean.reloadTree();
            return;
        }
        openDialog("cvDlgMoveTheso");
    }

    public void prepareGenerateArk() {
        identifierEditorBean.prepareGenerateArk();
        openDialog("cvDlgArkGen");
    }

    public void submitGenerateArk() {
        identifierEditorBean.submitGenerateArk();
        closeDialogs();
        refreshConsult();
    }

    public void prepareDeleteArk() {
        identifierEditorBean.prepareDeleteArk();
        openDialog("cvDlgArkDel");
    }

    public void submitDeleteArk() {
        identifierEditorBean.submitDeleteArk();
        closeDialogs();
        refreshConsult();
    }

    public void generateMissingArk() {
        identifierEditorBean.submitGenerateArkForConceptsWithoutArk();
        closeDialogs();
        refreshConsult();
    }

    public void prepareGenerateArkBranch() {
        identifierEditorBean.prepareGenerateArkForBranch();
        openDialog("cvDlgArkBranch");
    }

    public void submitGenerateArkBranch() {
        identifierEditorBean.submitGenerateArkForBranch();
        closeDialogs();
        refreshConsult();
    }

    public void prepareGenerateAllArk() {
        identifierEditorBean.prepareGenerateAllArk();
        openDialog("cvDlgArkAll");
    }

    public void submitGenerateAllArk() {
        identifierEditorBean.submitGenerateAllArk();
        closeDialogs();
        refreshConsult();
    }

    public void prepareGenerateHandle() {
        identifierEditorBean.prepareGenerateHandle();
        openDialog("cvDlgHandleGen");
    }

    public void submitGenerateHandle() {
        identifierEditorBean.submitGenerateHandle();
        closeDialogs();
        refreshConsult();
    }

    public void prepareDeleteHandle() {
        identifierEditorBean.prepareDeleteHandle();
        openDialog("cvDlgHandleDel");
    }

    public void submitDeleteHandle() {
        identifierEditorBean.submitDeleteHandle();
        closeDialogs();
        refreshConsult();
    }

    public void generateMissingHandle() {
        identifierEditorBean.submitGenerateHandleForConceptsWithoutHandle();
        closeDialogs();
        refreshConsult();
    }

    public void prepareGenerateHandleBranch() {
        identifierEditorBean.prepareGenerateHandleForBranch();
        openDialog("cvDlgHandleBranch");
    }

    public void submitGenerateHandleBranch() {
        identifierEditorBean.submitGenerateHandleForBranch();
        closeDialogs();
        refreshConsult();
    }

    public void generateAllHandle() {
        identifierEditorBean.submitGenerateAllHandle();
        closeDialogs();
        refreshConsult();
    }

    public void prepareImportCsv() {
        listCsvImportBean.prepareImport();
        openDialog("cvDlgImportCsv");
    }

    public void loadImportCsv() {
        listCsvImportBean.actionChoice();
        listCsvImportBean.loadFromUpload();
        openDialog("cvDlgImportCsv");
    }

    public void submitImportCsv() {
        listCsvImportBean.importUnderCurrentConcept();
        closeDialogs();
        refreshConsult();
    }

    public void repairLoopedRelationships() {
        maintenanceEditorBean.repairLoopedRelationships();
        closeDialogs();
        refreshConsult();
    }

    public void dismissDialog() {
        closeDialogs();
    }

    private void refreshConsult() {
        thesaurusViewBean.refreshFromSelectionContext();
    }

    private void refreshAfterTreeMove() {
        String movedId = treeDragDropBean.getLastMovedId();
        String movedType = StringUtils.defaultIfBlank(treeDragDropBean.getLastMovedType(), "concept");
        if (StringUtils.isBlank(movedId)) {
            refreshConsult();
            return;
        }
        thesaurusViewBean.reloadTree();
        thesaurusViewBean.setRevealId(movedId);
        thesaurusViewBean.revealInTree();
        thesaurusViewBean.openTreeNode(movedId, movedType);
    }

    private void openDialog(String id) {
        dialogToReopen = id;
    }

    private void closeDialogs() {
        dialogToReopen = null;
    }
}
