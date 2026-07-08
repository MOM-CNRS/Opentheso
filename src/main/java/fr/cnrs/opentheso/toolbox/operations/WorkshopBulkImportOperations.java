package fr.cnrs.opentheso.toolbox.operations;

import fr.cnrs.opentheso.bean.importexport.ImportFileBean;
import fr.cnrs.opentheso.legacybridge.LegacyThesaurusSync;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkshopBulkImportOperations {

    private final ObjectProvider<ImportFileBean> importFileBeanProvider;
    private final LegacyThesaurusSync legacyThesaurusSync;

    public void syncContext(String thesaurusId, String language) {
        if (StringUtils.isNotBlank(thesaurusId)) {
            legacyThesaurusSync.applyThesaurusId(thesaurusId.trim(), language);
        }
    }

    public void init() {
        importFileBean().init();
    }

    public void actionChoice() {
        importFileBean().actionChoice();
    }

    public void actionChoiceIdentifier() {
        importFileBean().actionChoiceIdentifier();
    }

    public void loadFileAlignmentCsv(FileUploadEvent event) {
        importFileBean().loadFileAlignmentCsv(event);
    }

    public void loadFileAlignmentCsvToDelete(FileUploadEvent event) {
        importFileBean().loadFileAlignmentCsvToDelete(event);
    }

    public void loadFileNoteCsv(FileUploadEvent event) {
        importFileBean().loadFileNoteCsv(event);
    }

    public void loadFileAltlabelCsv(FileUploadEvent event) {
        importFileBean().loadFileAltlabelCsv(event);
    }

    public void loadFileArkCsv(FileUploadEvent event) {
        importFileBean().loadFileArkCsv(event);
    }

    public void loadFileTraductionCsv(FileUploadEvent event) {
        importFileBean().loadFileTraductionCsv(event);
    }

    public void loadFileRelatedCsv(FileUploadEvent event) {
        importFileBean().loadFileRelatedCsv(event);
    }

    public void loadFileIdentifierCsv(FileUploadEvent event) {
        importFileBean().loadFileIdentifierCsv(event);
    }

    public void loadFileImageCsv(FileUploadEvent event) {
        importFileBean().loadFileImageCsv(event);
    }

    public void loadFileNotationCsv(FileUploadEvent event) {
        importFileBean().loadFileNotationCsv(event);
    }

    public void loadFileCollectionCsv(FileUploadEvent event) {
        importFileBean().loadFileCollectionCsv(event);
    }

    public void loadFileCsvForMerge(FileUploadEvent event) {
        importFileBean().loadFileCsvForMerge(event);
    }

    public void loadFileCsvForReplaceValueByNewValue(FileUploadEvent event) {
        importFileBean().loadFileCsvForReplaceValueByNewValue(event);
    }

    public void loadFileCsvDeprecateConcepts(FileUploadEvent event) {
        importFileBean().loadFileCsvDeprecateConcepts(event);
    }

    public void loadFileCsv(FileUploadEvent event) {
        importFileBean().loadFileCsv(event);
    }

    public void loadFileCsvForGetIdFromPrefLabel(FileUploadEvent event) {
        importFileBean().loadFileCsvForGetIdFromPrefLabel(event);
    }

    public void addAlignmentList() {
        importFileBean().addAlignmentList();
    }

    public void deleteAlignmentFromCsv() {
        importFileBean().deleteAlignmentFromCsv();
    }

    public void addNoteList() {
        importFileBean().addNoteList();
    }

    public void addAltLabelList() {
        importFileBean().addAltLabelList();
    }

    public void deleteAltLabelList() {
        importFileBean().deleteAltLabelList();
    }

    public void addArkList() {
        importFileBean().addArkList();
    }

    public void addTraductionList() {
        importFileBean().addTraductionList();
    }

    public void addRelatedList() {
        importFileBean().addRelatedList();
    }

    public void addImageList() {
        importFileBean().addImageList();
    }

    public void addNotationList() {
        importFileBean().addNotationList();
    }

    public void addCollectionListToConcept() {
        importFileBean().addCollectionListToConcept();
    }

    public void mergeCsvThesoToBDD(String thesaurusId, int userId) {
        importFileBean().mergeCsvThesoToBDD(thesaurusId, userId);
    }

    public void replaceValueByNewValue(String thesaurusId, int userId) {
        importFileBean().replaceValueByNewValue(thesaurusId, userId);
    }

    public void deprecateConcepts(String thesaurusId, int userId) {
        importFileBean().deprecateConcepts(thesaurusId, userId);
    }

    public void addListConceptsToTheso(String thesaurusId) {
        importFileBean().addListConceptsToTheso(thesaurusId);
    }

    public StreamedContent getAlignmentsOfTheso(String thesaurusId) {
        return importFileBean().getAlignmentsOfTheso(thesaurusId);
    }

    public StreamedContent compareListToTheso(String thesaurusId) {
        return importFileBean().compareListToTheso(thesaurusId);
    }

    public StreamedContent getArkFromConceptId() {
        return importFileBean().getArkFromConceptId();
    }

    public StreamedContent getConceptIdFromArk() {
        return importFileBean().getConceptIdFromArk();
    }

    public String getSelectedIdentifierImportAlign() {
        return importFileBean().getSelectedIdentifierImportAlign();
    }

    public void setSelectedIdentifierImportAlign(String value) {
        importFileBean().setSelectedIdentifierImportAlign(value);
    }

    public int getChoiceDelimiter() {
        return importFileBean().getChoiceDelimiter();
    }

    public void setChoiceDelimiter(int value) {
        importFileBean().setChoiceDelimiter(value);
    }

    public boolean isLoadDone() {
        return importFileBean().isLoadDone();
    }

    public String getUri() {
        return importFileBean().getUri();
    }

    public double getTotal() {
        return importFileBean().getTotal();
    }

    public int getTotalInt() {
        return importFileBean().getTotalInt();
    }

    public boolean isClearBefore() {
        return importFileBean().isClearBefore();
    }

    public void setClearBefore(boolean value) {
        importFileBean().setClearBefore(value);
    }

    public String getSelectedSearchType() {
        return importFileBean().getSelectedSearchType();
    }

    public void setSelectedSearchType(String value) {
        importFileBean().setSelectedSearchType(value);
    }

    public String getFileName() {
        return importFileBean().getFileName();
    }

    public String getSelectedConcept() {
        return importFileBean().getSelectedConcept();
    }

    public void setSelectedConcept(String value) {
        importFileBean().setSelectedConcept(value);
    }

    public String getAlignmentSource() {
        return importFileBean().getAlignmentSource();
    }

    public void setAlignmentSource(String value) {
        importFileBean().setAlignmentSource(value);
    }

    private ImportFileBean importFileBean() {
        return importFileBeanProvider.getObject();
    }
}
