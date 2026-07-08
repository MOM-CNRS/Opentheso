package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.toolbox.operations.WorkshopBulkImportOperations;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;

import java.io.Serializable;

@SessionScoped
@Named("v2WorkshopImportBean")
@RequiredArgsConstructor
public class WorkshopImportBean implements Serializable {

    private final WorkshopBulkImportOperations workshopBulkImportOperations;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;

    public void prepare() {
        workshopBulkImportOperations.syncContext(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
        workshopBulkImportOperations.init();
    }

    public void actionChoice() {
        syncContext();
        workshopBulkImportOperations.actionChoice();
    }

    public void actionChoiceIdentifier() {
        syncContext();
        workshopBulkImportOperations.actionChoiceIdentifier();
    }

    public void loadFileAlignmentCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileAlignmentCsv(event);
    }

    public void loadFileAlignmentCsvToDelete(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileAlignmentCsvToDelete(event);
    }

    public void loadFileNoteCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileNoteCsv(event);
    }

    public void loadFileAltlabelCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileAltlabelCsv(event);
    }

    public void loadFileArkCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileArkCsv(event);
    }

    public void loadFileTraductionCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileTraductionCsv(event);
    }

    public void loadFileRelatedCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileRelatedCsv(event);
    }

    public void loadFileIdentifierCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileIdentifierCsv(event);
    }

    public void loadFileImageCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileImageCsv(event);
    }

    public void loadFileNotationCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileNotationCsv(event);
    }

    public void loadFileCollectionCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileCollectionCsv(event);
    }

    public void loadFileCsvForMerge(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileCsvForMerge(event);
    }

    public void loadFileCsvForReplaceValueByNewValue(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileCsvForReplaceValueByNewValue(event);
    }

    public void loadFileCsvDeprecateConcepts(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileCsvDeprecateConcepts(event);
    }

    public void loadFileCsv(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileCsv(event);
    }

    public void loadFileCsvForGetIdFromPrefLabel(FileUploadEvent event) {
        syncContext();
        workshopBulkImportOperations.loadFileCsvForGetIdFromPrefLabel(event);
    }

    public void addAlignmentList() {
        syncContext();
        workshopBulkImportOperations.addAlignmentList();
    }

    public void deleteAlignmentFromCsv() {
        syncContext();
        workshopBulkImportOperations.deleteAlignmentFromCsv();
    }

    public void addNoteList() {
        syncContext();
        workshopBulkImportOperations.addNoteList();
    }

    public void addAltLabelList() {
        syncContext();
        workshopBulkImportOperations.addAltLabelList();
    }

    public void deleteAltLabelList() {
        syncContext();
        workshopBulkImportOperations.deleteAltLabelList();
    }

    public void addArkList() {
        syncContext();
        workshopBulkImportOperations.addArkList();
    }

    public void addTraductionList() {
        syncContext();
        workshopBulkImportOperations.addTraductionList();
    }

    public void addRelatedList() {
        syncContext();
        workshopBulkImportOperations.addRelatedList();
    }

    public void addImageList() {
        syncContext();
        workshopBulkImportOperations.addImageList();
    }

    public void addNotationList() {
        syncContext();
        workshopBulkImportOperations.addNotationList();
    }

    public void addCollectionListToConcept() {
        syncContext();
        workshopBulkImportOperations.addCollectionListToConcept();
    }

    public void mergeCsvThesoToBDD() {
        executeWithUser((thesaurusId, userId) ->
                workshopBulkImportOperations.mergeCsvThesoToBDD(thesaurusId, userId));
    }

    public void replaceValueByNewValue() {
        executeWithUser((thesaurusId, userId) ->
                workshopBulkImportOperations.replaceValueByNewValue(thesaurusId, userId));
    }

    public void deprecateConcepts() {
        executeWithUser((thesaurusId, userId) ->
                workshopBulkImportOperations.deprecateConcepts(thesaurusId, userId));
    }

    public void addListConceptsToTheso() {
        String thesaurusId = requireThesaurusId();
        if (thesaurusId == null) {
            return;
        }
        syncContext();
        workshopBulkImportOperations.addListConceptsToTheso(thesaurusId);
    }

    public StreamedContent getAlignmentsOfCurrentTheso() {
        String thesaurusId = requireThesaurusId();
        if (thesaurusId == null) {
            return null;
        }
        syncContext();
        return workshopBulkImportOperations.getAlignmentsOfTheso(thesaurusId);
    }

    public StreamedContent getCompareListToCurrentTheso() {
        String thesaurusId = requireThesaurusId();
        if (thesaurusId == null) {
            return null;
        }
        syncContext();
        return workshopBulkImportOperations.compareListToTheso(thesaurusId);
    }

    public StreamedContent getArkFromConceptId() {
        syncContext();
        return workshopBulkImportOperations.getArkFromConceptId();
    }

    public StreamedContent getConceptIdFromArk() {
        syncContext();
        return workshopBulkImportOperations.getConceptIdFromArk();
    }

    public void init() {
        prepare();
    }

    public String getSelectedIdentifierImportAlign() {
        return workshopBulkImportOperations.getSelectedIdentifierImportAlign();
    }

    public void setSelectedIdentifierImportAlign(String value) {
        workshopBulkImportOperations.setSelectedIdentifierImportAlign(value);
    }

    public int getChoiceDelimiter() {
        return workshopBulkImportOperations.getChoiceDelimiter();
    }

    public void setChoiceDelimiter(int value) {
        workshopBulkImportOperations.setChoiceDelimiter(value);
    }

    public boolean isLoadDone() {
        return workshopBulkImportOperations.isLoadDone();
    }

    public String getUri() {
        return workshopBulkImportOperations.getUri();
    }

    public double getTotal() {
        return workshopBulkImportOperations.getTotal();
    }

    public int getTotalInt() {
        return workshopBulkImportOperations.getTotalInt();
    }

    public boolean isClearBefore() {
        return workshopBulkImportOperations.isClearBefore();
    }

    public void setClearBefore(boolean value) {
        workshopBulkImportOperations.setClearBefore(value);
    }

    public String getSelectedSearchType() {
        return workshopBulkImportOperations.getSelectedSearchType();
    }

    public void setSelectedSearchType(String value) {
        workshopBulkImportOperations.setSelectedSearchType(value);
    }

    public String getFileName() {
        return workshopBulkImportOperations.getFileName();
    }

    public String getSelectedConcept() {
        return workshopBulkImportOperations.getSelectedConcept();
    }

    public void setSelectedConcept(String value) {
        workshopBulkImportOperations.setSelectedConcept(value);
    }

    public String getAlignmentSource() {
        return workshopBulkImportOperations.getAlignmentSource();
    }

    public void setAlignmentSource(String value) {
        workshopBulkImportOperations.setAlignmentSource(value);
    }

    private void syncContext() {
        workshopBulkImportOperations.syncContext(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    private String requireThesaurusId() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            MessageUtils.showErrorMessage("Vous devez choisir un Thésaurus avant !");
            return null;
        }
        return thesaurusId;
    }

    private void executeWithUser(BulkImportAction action) {
        String thesaurusId = requireThesaurusId();
        Integer userId = userSession.getCurrentUserId();
        if (thesaurusId == null || userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        syncContext();
        action.run(thesaurusId, userId);
    }

    @FunctionalInterface
    private interface BulkImportAction {
        void run(String thesaurusId, int userId);
    }
}
