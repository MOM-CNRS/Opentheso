package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.v2.toolbox.workshop.WorkshopBulkImportOperations;
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
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        Integer userId = userSession.getCurrentUserId();
        if (StringUtils.isBlank(thesaurusId) || userId == null) {
            return;
        }
        workshopBulkImportOperations.prepare(
                thesaurusId,
                thesaurusContext.resolveWorkLanguage(),
                userId
        );
    }

    public void actionChoice() {
        workshopBulkImportOperations.actionChoice();
    }

    public void actionChoiceIdentifier() {
        workshopBulkImportOperations.actionChoiceIdentifier();
    }

    public void loadFileAlignmentCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileAlignmentCsv(event);
    }

    public void loadFileAlignmentCsvToDelete(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileAlignmentCsvToDelete(event);
    }

    public void loadFileNoteCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileNoteCsv(event);
    }

    public void loadFileAltlabelCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileAltlabelCsv(event);
    }

    public void loadFileArkCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileArkCsv(event);
    }

    public void loadFileTraductionCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileTraductionCsv(event);
    }

    public void loadFileRelatedCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileRelatedCsv(event);
    }

    public void loadFileIdentifierCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileIdentifierCsv(event);
    }

    public void loadFileImageCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileImageCsv(event);
    }

    public void loadFileNotationCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileNotationCsv(event);
    }

    public void loadFileCollectionCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileCollectionCsv(event);
    }

    public void loadFileCsvForMerge(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileCsvForMerge(event);
    }

    public void loadFileCsvForReplaceValueByNewValue(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileCsvForReplaceValueByNewValue(event);
    }

    public void loadFileCsvDeprecateConcepts(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileCsvDeprecateConcepts(event);
    }

    public void loadFileCsv(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileCsv(event);
    }

    public void loadFileCsvForGetIdFromPrefLabel(FileUploadEvent event) {
        workshopBulkImportOperations.loadFileCsvForGetIdFromPrefLabel(event);
    }

    public void addAlignmentList() {
        workshopBulkImportOperations.addAlignmentList();
    }

    public void deleteAlignmentFromCsv() {
        workshopBulkImportOperations.deleteAlignmentFromCsv();
    }

    public void addNoteList() {
        workshopBulkImportOperations.addNoteList();
    }

    public void addAltLabelList() {
        workshopBulkImportOperations.addAltLabelList();
    }

    public void deleteAltLabelList() {
        workshopBulkImportOperations.deleteAltLabelList();
    }

    public void addArkList() {
        workshopBulkImportOperations.addArkList();
    }

    public void addTraductionList() {
        workshopBulkImportOperations.addTraductionList();
    }

    public void addRelatedList() {
        workshopBulkImportOperations.addRelatedList();
    }

    public void addImageList() {
        workshopBulkImportOperations.addImageList();
    }

    public void addNotationList() {
        workshopBulkImportOperations.addNotationList();
    }

    public void addCollectionListToConcept() {
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
        workshopBulkImportOperations.addListConceptsToTheso(thesaurusId);
    }

    public StreamedContent getAlignmentsOfCurrentTheso() {
        String thesaurusId = requireThesaurusId();
        if (thesaurusId == null) {
            return null;
        }
        return workshopBulkImportOperations.getAlignmentsOfTheso(thesaurusId);
    }

    public StreamedContent getCompareListToCurrentTheso() {
        String thesaurusId = requireThesaurusId();
        if (thesaurusId == null) {
            return null;
        }
        return workshopBulkImportOperations.compareListToTheso(thesaurusId);
    }

    public StreamedContent getArkFromConceptId() {
        return workshopBulkImportOperations.getArkFromConceptId();
    }

    public StreamedContent getConceptIdFromArk() {
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
        workshopBulkImportOperations.prepare(
                thesaurusId,
                thesaurusContext.resolveWorkLanguage(),
                userId
        );
        action.run(thesaurusId, userId);
    }

    @FunctionalInterface
    private interface BulkImportAction {
        void run(String thesaurusId, int userId);
    }
}
