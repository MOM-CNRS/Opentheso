package fr.cnrs.opentheso.v2.toolbox.workshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.event.FileUploadEvent;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopBulkImportOperationsTest {

    @Mock
    private ObjectProvider<WorkshopBulkImportEngine> workshopBulkImportEngineProvider;
    @Mock
    private WorkshopBulkImportEngine engine;
    @Mock
    private FileUploadEvent event;

    private WorkshopBulkImportOperations operations;

    @BeforeEach
    void setUp() {
        when(workshopBulkImportEngineProvider.getObject()).thenReturn(engine);
        operations = new WorkshopBulkImportOperations(workshopBulkImportEngineProvider);
    }

    @Test
    void prepare_initializesEngineWithThesaurusContext() {
        operations.prepare("TH1", "fr", 7);

        verify(engine).prepare("TH1", "fr", 7);
        verify(engine).init();
    }

    @Test
    void init_delegatesToEngine() {
        operations.init();

        verify(engine).init();
    }

    @Test
    void actionChoice_delegatesToEngine() {
        operations.actionChoice();

        verify(engine).actionChoice();
    }

    @Test
    void actionChoiceIdentifier_delegatesToEngine() {
        operations.actionChoiceIdentifier();

        verify(engine).actionChoiceIdentifier();
    }

    @Test
    void loadFileAlignmentCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileAlignmentCsv(event);

        verify(engine).loadFileAlignmentCsv(event);
    }

    @Test
    void loadFileAlignmentCsvToDelete_delegatesToEngineWithSameEvent() {
        operations.loadFileAlignmentCsvToDelete(event);

        verify(engine).loadFileAlignmentCsvToDelete(event);
    }

    @Test
    void loadFileNoteCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileNoteCsv(event);

        verify(engine).loadFileNoteCsv(event);
    }

    @Test
    void loadFileAltlabelCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileAltlabelCsv(event);

        verify(engine).loadFileAltlabelCsv(event);
    }

    @Test
    void loadFileArkCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileArkCsv(event);

        verify(engine).loadFileArkCsv(event);
    }

    @Test
    void loadFileTraductionCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileTraductionCsv(event);

        verify(engine).loadFileTraductionCsv(event);
    }

    @Test
    void loadFileRelatedCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileRelatedCsv(event);

        verify(engine).loadFileRelatedCsv(event);
    }

    @Test
    void loadFileIdentifierCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileIdentifierCsv(event);

        verify(engine).loadFileIdentifierCsv(event);
    }

    @Test
    void loadFileImageCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileImageCsv(event);

        verify(engine).loadFileImageCsv(event);
    }

    @Test
    void loadFileNotationCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileNotationCsv(event);

        verify(engine).loadFileNotationCsv(event);
    }

    @Test
    void loadFileCollectionCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileCollectionCsv(event);

        verify(engine).loadFileCollectionCsv(event);
    }

    @Test
    void loadFileCsvForMerge_delegatesToEngineWithSameEvent() {
        operations.loadFileCsvForMerge(event);

        verify(engine).loadFileCsvForMerge(event);
    }

    @Test
    void loadFileCsvForReplaceValueByNewValue_delegatesToEngineWithSameEvent() {
        operations.loadFileCsvForReplaceValueByNewValue(event);

        verify(engine).loadFileCsvForReplaceValueByNewValue(event);
    }

    @Test
    void loadFileCsvDeprecateConcepts_delegatesToEngineWithSameEvent() {
        operations.loadFileCsvDeprecateConcepts(event);

        verify(engine).loadFileCsvDeprecateConcepts(event);
    }

    @Test
    void loadFileCsv_delegatesToEngineWithSameEvent() {
        operations.loadFileCsv(event);

        verify(engine).loadFileCsv(event);
    }

    @Test
    void loadFileCsvForGetIdFromPrefLabel_delegatesToEngineWithSameEvent() {
        operations.loadFileCsvForGetIdFromPrefLabel(event);

        verify(engine).loadFileCsvForGetIdFromPrefLabel(event);
    }

    @Test
    void addAlignmentList_delegatesToEngine() {
        operations.addAlignmentList();

        verify(engine).addAlignmentList();
    }

    @Test
    void deleteAlignmentFromCsv_delegatesToEngine() {
        operations.deleteAlignmentFromCsv();

        verify(engine).deleteAlignmentFromCsv();
    }

    @Test
    void addNoteList_delegatesToEngine() {
        operations.addNoteList();

        verify(engine).addNoteList();
    }

    @Test
    void addAltLabelList_delegatesToEngine() {
        operations.addAltLabelList();

        verify(engine).addAltLabelList();
    }

    @Test
    void deleteAltLabelList_delegatesToEngine() {
        operations.deleteAltLabelList();

        verify(engine).deleteAltLabelList();
    }

    @Test
    void addArkList_delegatesToEngine() {
        operations.addArkList();

        verify(engine).addArkList();
    }

    @Test
    void addTraductionList_delegatesToEngine() {
        operations.addTraductionList();

        verify(engine).addTraductionList();
    }

    @Test
    void addRelatedList_delegatesToEngine() {
        operations.addRelatedList();

        verify(engine).addRelatedList();
    }

    @Test
    void addImageList_delegatesToEngine() {
        operations.addImageList();

        verify(engine).addImageList();
    }

    @Test
    void addNotationList_delegatesToEngine() {
        operations.addNotationList();

        verify(engine).addNotationList();
    }

    @Test
    void addCollectionListToConcept_delegatesToEngine() {
        operations.addCollectionListToConcept();

        verify(engine).addCollectionListToConcept();
    }

    @Test
    void mergeCsvThesoToBDD_delegatesToEngineWithArguments() {
        operations.mergeCsvThesoToBDD("TH1", 7);

        verify(engine).mergeCsvThesoToBDD("TH1", 7);
    }

    @Test
    void replaceValueByNewValue_delegatesToEngineWithArguments() {
        operations.replaceValueByNewValue("TH1", 7);

        verify(engine).replaceValueByNewValue("TH1", 7);
    }

    @Test
    void deprecateConcepts_delegatesToEngineWithArguments() {
        operations.deprecateConcepts("TH1", 7);

        verify(engine).deprecateConcepts("TH1", 7);
    }

    @Test
    void addListConceptsToTheso_delegatesToEngineWithArgument() {
        operations.addListConceptsToTheso("TH1");

        verify(engine).addListConceptsToTheso("TH1");
    }

    @Test
    void getAlignmentsOfTheso_returnsEngineResult() {
        var streamed = org.mockito.Mockito.mock(org.primefaces.model.StreamedContent.class);
        when(engine.getAlignmentsOfTheso("TH1")).thenReturn(streamed);

        var result = operations.getAlignmentsOfTheso("TH1");

        assertEquals(streamed, result);
    }

    @Test
    void compareListToTheso_returnsEngineResult() {
        var streamed = org.mockito.Mockito.mock(org.primefaces.model.StreamedContent.class);
        when(engine.compareListToTheso("TH1")).thenReturn(streamed);

        var result = operations.compareListToTheso("TH1");

        assertEquals(streamed, result);
    }

    @Test
    void getArkFromConceptId_returnsEngineResult() {
        var streamed = org.mockito.Mockito.mock(org.primefaces.model.StreamedContent.class);
        when(engine.getArkFromConceptId()).thenReturn(streamed);

        var result = operations.getArkFromConceptId();

        assertEquals(streamed, result);
    }

    @Test
    void getConceptIdFromArk_returnsEngineResult() {
        var streamed = org.mockito.Mockito.mock(org.primefaces.model.StreamedContent.class);
        when(engine.getConceptIdFromArk()).thenReturn(streamed);

        var result = operations.getConceptIdFromArk();

        assertEquals(streamed, result);
    }

    @Test
    void selectedIdentifierImportAlign_getterAndSetterDelegateToEngine() {
        when(engine.getSelectedIdentifierImportAlign()).thenReturn("ark");

        operations.setSelectedIdentifierImportAlign("ark");
        String result = operations.getSelectedIdentifierImportAlign();

        verify(engine).setSelectedIdentifierImportAlign("ark");
        assertEquals("ark", result);
    }

    @Test
    void choiceDelimiter_getterAndSetterDelegateToEngine() {
        when(engine.getChoiceDelimiter()).thenReturn(1);

        operations.setChoiceDelimiter(1);
        int result = operations.getChoiceDelimiter();

        verify(engine).setChoiceDelimiter(1);
        assertEquals(1, result);
    }

    @Test
    void loadDone_getterDelegatesToEngine() {
        when(engine.isLoadDone()).thenReturn(true);

        assertTrue(operations.isLoadDone());
    }

    @Test
    void clearBefore_getterAndSetterDelegateToEngine() {
        when(engine.isClearBefore()).thenReturn(true);

        operations.setClearBefore(true);

        verify(engine).setClearBefore(true);
        assertTrue(operations.isClearBefore());
    }
}
