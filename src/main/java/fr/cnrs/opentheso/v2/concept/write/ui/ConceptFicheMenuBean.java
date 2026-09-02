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
        openDialog("cvDlgAddNt");
    }

    public void submitAddChildForced() {
        lifecycleEditorBean.submitAddChildForced();
        openDialog("cvDlgAddNt");
    }

    public void cancelAddChildDuplicate() {
        lifecycleEditorBean.cancelDuplicate();
        openDialog("cvDlgAddNt");
    }

    public void afterAddChild() {
        boolean created = lifecycleEditorBean.isCreateDirty();
        String lastId = lifecycleEditorBean.getLastCreatedId();
        lifecycleEditorBean.finishCreateAfterClose();
        closeDialogs();
        if (!created) {
            return;
        }
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusViewBean.reloadTree();
        if (StringUtils.isNotBlank(lastId)) {
            thesaurusViewBean.setRevealId(lastId);
            thesaurusViewBean.revealInTree();
        }
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
            facetDetailEditorBean.setParentConceptId(conceptId);
            facetDetailEditorBean.setParentConceptLabel(
                    StringUtils.isNotBlank(preferredLabel) ? preferredLabel : "(" + conceptId + ")");
            facetDetailEditorBean.setComposing(true);
        }
        closeDialogs();
    }

    public void submitCreateFacet() {
        facetDetailEditorBean.submitCreate();
        if (!"done".equals(facetDetailEditorBean.getCreateRunState())) {
            return;
        }
        String facetId = facetDetailEditorBean.getCreatedFacetId();
        thesaurusBrowseBean.invalidateConceptTree();
        thesaurusViewBean.reloadTree();
        if (StringUtils.isNotBlank(facetId)) {
            thesaurusViewBean.setRevealId(facetId);
            thesaurusViewBean.revealInTree();
        }
        refreshConsult();
    }

    public void cancelCreateFacet() {
        facetDetailEditorBean.cancelCreate();
        closeDialogs();
    }

    public void prepareEditConceptType() {
        attributeEditorBean.prepareEditConceptType();
        openDialog("cvDlgEditType");
    }

    public void selectConceptType(String code) {
        attributeEditorBean.selectConceptType(code);
        openDialog("cvDlgEditType");
    }

    public void submitEditConceptType() {
        attributeEditorBean.submitUpdateConceptType();
        openDialog("cvDlgEditType");
    }

    public void afterEditConceptType() {
        boolean refresh = attributeEditorBean.isTypeDone();
        attributeEditorBean.finishTypeAfterClose();
        closeDialogs();
        if (refresh) {
            refreshConsult();
        }
    }

    public void prepareManageConceptTypes() {
        typeManagerBean.prepareManage();
        openDialog("cvDlgManageTypes");
    }

    public void applyConceptType(fr.cnrs.opentheso.models.concept.NodeConceptType type) {
        typeManagerBean.applyChange(type);
        openDialog("cvDlgManageTypes");
    }

    public void addConceptType() {
        typeManagerBean.addNewConceptType();
        openDialog("cvDlgManageTypes");
    }

    public void prepareDeleteConceptType(fr.cnrs.opentheso.models.concept.NodeConceptType type) {
        typeManagerBean.prepareDelete(type);
        openDialog("cvDlgManageTypes");
    }

    public void confirmDeleteConceptType() {
        typeManagerBean.deleteCustomRelationship();
        openDialog("cvDlgManageTypes");
    }

    public void cancelDeleteConceptType() {
        typeManagerBean.cancelDelete();
        openDialog("cvDlgManageTypes");
    }

    public void afterManageConceptTypes() {
        boolean dirty = typeManagerBean.isDirty();
        typeManagerBean.finishAfterClose();
        closeDialogs();
        if (dirty) {
            refreshConsult();
        }
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
        openDialog("cvDlgDelete");
    }

    public void afterDelete() {
        lifecycleEditorBean.finishDeleteAfterClose();
        closeDialogs();
        thesaurusViewBean.reloadTree();
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
        openDialog("cvDlgArkGen");
    }

    public void afterGenerateArk() {
        identifierEditorBean.setFlashMessage(null);
        identifierEditorBean.setFlashToken(null);
        identifierEditorBean.setArkRunState("");
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

    public void prepareGenerateMissingArk() {
        identifierEditorBean.prepareGenerateMissingArk();
        openDialog("cvDlgArkMissing");
    }

    public void submitGenerateMissingArk() {
        identifierEditorBean.submitGenerateMissingArk();
        openDialog("cvDlgArkMissing");
    }

    public void prepareGenerateArkBranch() {
        identifierEditorBean.prepareGenerateArkForBranch();
        openDialog("cvDlgArkBranch");
    }

    public void submitGenerateArkBranch() {
        identifierEditorBean.submitGenerateArkForBranch();
        openDialog("cvDlgArkBranch");
    }

    public void prepareGenerateAllArk() {
        identifierEditorBean.prepareGenerateAllArk();
        openDialog("cvDlgArkAll");
    }

    public void submitGenerateAllArk() {
        identifierEditorBean.submitGenerateAllArk();
        openDialog("cvDlgArkAll");
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
        listCsvImportBean.loadFromUpload();
        openDialog("cvDlgImportCsv");
    }

    public void submitImportCsv() {
        listCsvImportBean.importUnderCurrentConcept();
        openDialog("cvDlgImportCsv");
    }

    public void afterImportCsv() {
        listCsvImportBean.setFlashMessage(null);
        listCsvImportBean.setFlashToken(null);
        listCsvImportBean.setRunState("");
        closeDialogs();
        thesaurusViewBean.reloadTree();
        refreshConsult();
    }

    public void prepareRepairLoopedRelationships() {
        maintenanceEditorBean.prepareRepairLoopedRelationships();
        openDialog("cvDlgRepairLoop");
    }

    public void submitRepairLoopedRelationships() {
        maintenanceEditorBean.submitRepairLoopedRelationships();
        openDialog("cvDlgRepairLoop");
    }

    public void afterRepairLoopedRelationships() {
        maintenanceEditorBean.setFlashMessage(null);
        maintenanceEditorBean.setFlashToken(null);
        maintenanceEditorBean.setRunState("");
        closeDialogs();
        thesaurusViewBean.reloadTree();
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
