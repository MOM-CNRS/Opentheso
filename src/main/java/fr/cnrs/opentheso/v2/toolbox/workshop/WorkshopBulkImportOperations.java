package fr.cnrs.opentheso.v2.toolbox.workshop;

import lombok.RequiredArgsConstructor;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkshopBulkImportOperations {

    private final ObjectProvider<WorkshopBulkImportEngine> workshopBulkImportEngineProvider;

    public void prepare(String thesaurusId, String language, int userId) {
        WorkshopBulkImportEngine engine = engine();
        engine.prepare(thesaurusId, language, userId);
        engine.init();
    }

    public void init() {
        engine().init();
    }

    public void actionChoice() {
        engine().actionChoice();
    }

    public void actionChoiceIdentifier() {
        engine().actionChoiceIdentifier();
    }

    public void loadFileAlignmentCsv(FileUploadEvent event) {
        engine().loadFileAlignmentCsv(event);
    }

    public void loadFileAlignmentCsvToDelete(FileUploadEvent event) {
        engine().loadFileAlignmentCsvToDelete(event);
    }

    public void loadFileNoteCsv(FileUploadEvent event) {
        engine().loadFileNoteCsv(event);
    }

    public void loadFileAltlabelCsv(FileUploadEvent event) {
        engine().loadFileAltlabelCsv(event);
    }

    public void loadFileArkCsv(FileUploadEvent event) {
        engine().loadFileArkCsv(event);
    }

    public void loadFileTraductionCsv(FileUploadEvent event) {
        engine().loadFileTraductionCsv(event);
    }

    public void loadFileRelatedCsv(FileUploadEvent event) {
        engine().loadFileRelatedCsv(event);
    }

    public void loadFileIdentifierCsv(FileUploadEvent event) {
        engine().loadFileIdentifierCsv(event);
    }

    public void loadFileImageCsv(FileUploadEvent event) {
        engine().loadFileImageCsv(event);
    }

    public void loadFileNotationCsv(FileUploadEvent event) {
        engine().loadFileNotationCsv(event);
    }

    public void loadFileCollectionCsv(FileUploadEvent event) {
        engine().loadFileCollectionCsv(event);
    }

    public void loadFileCsvForMerge(FileUploadEvent event) {
        engine().loadFileCsvForMerge(event);
    }

    public void loadFileCsvForReplaceValueByNewValue(FileUploadEvent event) {
        engine().loadFileCsvForReplaceValueByNewValue(event);
    }

    public void loadFileCsvDeprecateConcepts(FileUploadEvent event) {
        engine().loadFileCsvDeprecateConcepts(event);
    }

    public void loadFileCsv(FileUploadEvent event) {
        engine().loadFileCsv(event);
    }

    public void loadFileCsvForGetIdFromPrefLabel(FileUploadEvent event) {
        engine().loadFileCsvForGetIdFromPrefLabel(event);
    }

    public void addAlignmentList() {
        engine().addAlignmentList();
    }

    public void deleteAlignmentFromCsv() {
        engine().deleteAlignmentFromCsv();
    }

    public void addNoteList() {
        engine().addNoteList();
    }

    public void addAltLabelList() {
        engine().addAltLabelList();
    }

    public void deleteAltLabelList() {
        engine().deleteAltLabelList();
    }

    public void addArkList() {
        engine().addArkList();
    }

    public void addTraductionList() {
        engine().addTraductionList();
    }

    public void addRelatedList() {
        engine().addRelatedList();
    }

    public void addImageList() {
        engine().addImageList();
    }

    public void addNotationList() {
        engine().addNotationList();
    }

    public void addCollectionListToConcept() {
        engine().addCollectionListToConcept();
    }

    public void mergeCsvThesoToBDD(String thesaurusId, int userId) {
        engine().mergeCsvThesoToBDD(thesaurusId, userId);
    }

    public void replaceValueByNewValue(String thesaurusId, int userId) {
        engine().replaceValueByNewValue(thesaurusId, userId);
    }

    public void deprecateConcepts(String thesaurusId, int userId) {
        engine().deprecateConcepts(thesaurusId, userId);
    }

    public void addListConceptsToTheso(String thesaurusId) {
        engine().addListConceptsToTheso(thesaurusId);
    }

    public StreamedContent getAlignmentsOfTheso(String thesaurusId) {
        return engine().getAlignmentsOfTheso(thesaurusId);
    }

    public StreamedContent compareListToTheso(String thesaurusId) {
        return engine().compareListToTheso(thesaurusId);
    }

    public StreamedContent getArkFromConceptId() {
        return engine().getArkFromConceptId();
    }

    public StreamedContent getConceptIdFromArk() {
        return engine().getConceptIdFromArk();
    }

    public String getSelectedIdentifierImportAlign() {
        return engine().getSelectedIdentifierImportAlign();
    }

    public void setSelectedIdentifierImportAlign(String value) {
        engine().setSelectedIdentifierImportAlign(value);
    }

    public int getChoiceDelimiter() {
        return engine().getChoiceDelimiter();
    }

    public void setChoiceDelimiter(int value) {
        engine().setChoiceDelimiter(value);
    }

    public boolean isLoadDone() {
        return engine().isLoadDone();
    }

    public String getUri() {
        return engine().getUri();
    }

    public double getTotal() {
        return engine().getTotal();
    }

    public int getTotalInt() {
        return engine().getTotalInt();
    }

    public boolean isClearBefore() {
        return engine().isClearBefore();
    }

    public void setClearBefore(boolean value) {
        engine().setClearBefore(value);
    }

    public String getSelectedSearchType() {
        return engine().getSelectedSearchType();
    }

    public void setSelectedSearchType(String value) {
        engine().setSelectedSearchType(value);
    }

    public String getFileName() {
        return engine().getFileName();
    }

    public String getSelectedConcept() {
        return engine().getSelectedConcept();
    }

    public void setSelectedConcept(String value) {
        engine().setSelectedConcept(value);
    }

    public String getAlignmentSource() {
        return engine().getAlignmentSource();
    }

    public void setAlignmentSource(String value) {
        engine().setAlignmentSource(value);
    }

    private WorkshopBulkImportEngine engine() {
        return workshopBulkImportEngineProvider.getObject();
    }
}
