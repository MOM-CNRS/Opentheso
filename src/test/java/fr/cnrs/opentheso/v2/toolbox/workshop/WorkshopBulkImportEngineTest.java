package fr.cnrs.opentheso.v2.toolbox.workshop;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;
import jakarta.faces.event.PhaseId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ces tests pilotent {@link WorkshopBulkImportEngine} exclusivement via son API publique
 * (comme le ferait {@link WorkshopBulkImportOperations}) car les listes internes parsées
 * (nodeAlignmentImports, conceptObjects...) n'ont pas de setter public — seule la phase
 * "load" (via un {@link FileUploadEvent} simulé) permet de les peupler, exactement comme
 * en production. {@link MessageUtils} et {@link PrimeFaces} sont mockés en statique car
 * le moteur les appelle directement (growl, exécution de script JS) hors de tout contexte
 * JSF réel.
 */
@ExtendWith(MockitoExtension.class)
class WorkshopBulkImportEngineTest {

    @Mock
    private WorkshopBulkImportPersistence persistence;
    @Mock
    private ThesaurusCsvWriter thesaurusCsvWriter;
    @Mock
    private FileUploadEvent event;
    @Mock
    private UploadedFile uploadedFile;

    private WorkshopBulkImportEngine engine;
    private MockedStatic<PrimeFaces> primeFacesStatic;
    private MockedStatic<MessageUtils> messageUtilsStatic;

    @BeforeEach
    void setUp() {
        PrimeFaces primeFacesInstance = mock(PrimeFaces.class);
        primeFacesStatic = mockStatic(PrimeFaces.class);
        primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFacesInstance);
        messageUtilsStatic = mockStatic(MessageUtils.class);

        engine = new WorkshopBulkImportEngine(persistence, thesaurusCsvWriter);
        // WorkshopBulkImportOperations.prepare() en production enchaîne toujours prepare() puis init() :
        // init() est ce qui positionne selectedIdentifierImportAlign="identifier" (sinon addAlignmentList/
        // addNoteList ne peuvent résoudre aucun idConcept et sautent silencieusement toutes les lignes).
        engine.prepare("TH1", "fr", 7);
        engine.init();
        lenient().when(event.getPhaseId()).thenReturn(PhaseId.INVOKE_APPLICATION);
        lenient().when(event.getFile()).thenReturn(uploadedFile);
    }

    @AfterEach
    void tearDown() {
        primeFacesStatic.close();
        messageUtilsStatic.close();
    }

    private void stubUploadedContent(String csv) throws Exception {
        // Le moteur ouvre deux flux distincts sur le même fichier (un pour l'en-tête/langues, un pour les données) :
        // on doit donc pouvoir appeler getInputStream() plusieurs fois avec un contenu frais à chaque fois.
        when(uploadedFile.getInputStream()).thenAnswer(invocation ->
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    // ---- prepare / init / actionChoice ----

    @Test
    void init_resetsStateToDefaults() {
        engine.init();

        assertFalse(engine.isLoadDone());
        assertEquals("identifier", engine.getSelectedIdentifierImportAlign());
        assertEquals(0, engine.getChoiceDelimiter());
    }

    @Test
    void actionChoice_semicolonDelimiter_parsesSemicolonSeparatedFile() throws Exception {
        engine.setChoiceDelimiter(1);
        engine.actionChoice();
        stubUploadedContent("localId;skos:exactMatch\nC1;http://example.com/c1\n");

        engine.loadFileAlignmentCsv(event);

        assertTrue(engine.isLoadDone());
        assertEquals(1, engine.getTotalInt());
    }

    @Test
    void actionChoice_commaDelimiter_failsOnSemicolonSeparatedFile() throws Exception {
        engine.setChoiceDelimiter(0);
        engine.actionChoice();
        stubUploadedContent("localId;skos:exactMatch\nC1;http://example.com/c1\n");

        engine.loadFileAlignmentCsv(event);

        assertFalse(engine.isLoadDone());
    }

    // ---- loadFileAlignmentCsv ----

    @Test
    void loadFileAlignmentCsv_requeuesEvent_whenNotInInvokeApplicationPhase() {
        when(event.getPhaseId()).thenReturn(PhaseId.APPLY_REQUEST_VALUES);

        engine.loadFileAlignmentCsv(event);

        verify(event).setPhaseId(PhaseId.INVOKE_APPLICATION);
        verify(event).queue();
        assertFalse(engine.isLoadDone());
    }

    @Test
    void loadFileAlignmentCsv_validCsv_marksLoadDoneWithCorrectTotal() throws Exception {
        stubUploadedContent("localId,skos:exactMatch\nC1,http://example.com/c1\n");

        engine.loadFileAlignmentCsv(event);

        assertTrue(engine.isLoadDone());
        assertEquals(1, engine.getTotalInt());
    }

    @Test
    void loadFileAlignmentCsv_invalidHeader_doesNotMarkLoadDone() throws Exception {
        stubUploadedContent("not,a,valid,alignment,header\nfoo,bar,baz,qux,quux\n");

        engine.loadFileAlignmentCsv(event);

        assertFalse(engine.isLoadDone());
    }

    // ---- addAlignmentList ----

    @Test
    void addAlignmentList_noThesaurusSelected_doesNotCallPersistence() {
        WorkshopBulkImportEngine noThesaurusEngine = new WorkshopBulkImportEngine(persistence, thesaurusCsvWriter);
        noThesaurusEngine.prepare("", "fr", 7);
        noThesaurusEngine.init();

        noThesaurusEngine.addAlignmentList();

        verify(persistence, never()).addNewAlignment(any());
    }

    @Test
    void addAlignmentList_noDataLoaded_doesNotCallPersistence() {
        engine.addAlignmentList();

        verify(persistence, never()).addNewAlignment(any());
    }

    @Test
    void addAlignmentList_validImport_addsAlignmentForExistingConcept() throws Exception {
        stubUploadedContent("localId,skos:exactMatch\nC1,http://example.com/c1\n");
        engine.loadFileAlignmentCsv(event);
        assertTrue(engine.isLoadDone());
        when(persistence.isIdExiste("C1", "TH1")).thenReturn(true);
        when(persistence.addNewAlignment(any())).thenReturn(true);

        engine.addAlignmentList();

        ArgumentCaptor<NodeAlignment> captor = ArgumentCaptor.forClass(NodeAlignment.class);
        verify(persistence).addNewAlignment(captor.capture());
        NodeAlignment saved = captor.getValue();
        assertEquals("C1", saved.getInternal_id_concept());
        assertEquals("TH1", saved.getInternal_id_thesaurus());
        assertEquals("http://example.com/c1", saved.getUri_target());
        assertEquals(7, saved.getId_author());
    }

    @Test
    void addAlignmentList_conceptDoesNotExist_skipsAlignment() throws Exception {
        stubUploadedContent("localId,skos:exactMatch\nC1,http://example.com/c1\n");
        engine.loadFileAlignmentCsv(event);
        assertTrue(engine.isLoadDone());
        when(persistence.isIdExiste("C1", "TH1")).thenReturn(false);

        engine.addAlignmentList();

        verify(persistence, never()).addNewAlignment(any());
    }

    // ---- loadFileNoteCsv / addNoteList ----

    @Test
    void loadFileNoteCsv_validCsv_marksLoadDoneWithParsedConcept() throws Exception {
        stubUploadedContent("localid,skos:note@fr,skos:definition@fr\nC1,Une note,Une definition\n");

        engine.loadFileNoteCsv(event);

        assertTrue(engine.isLoadDone());
        assertEquals(1, engine.getTotalInt());
    }

    @Test
    void addNoteList_validImport_addsDefinitionForExistingConcept() throws Exception {
        stubUploadedContent("localid,skos:note@fr,skos:definition@fr\nC1,Une note,Une definition\n");
        engine.loadFileNoteCsv(event);
        assertTrue(engine.isLoadDone());
        when(persistence.isIdExiste("C1", "TH1")).thenReturn(true);
        when(persistence.isNoteExist(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        engine.addNoteList();

        verify(persistence).addNote("C1", "fr", "TH1", "Une definition", "definition", "", -1);
        verify(persistence).addNote("C1", "fr", "TH1", "Une note", "note", "", -1);
    }

    @Test
    void addNoteList_noThesaurusSelected_doesNotCallPersistence() {
        WorkshopBulkImportEngine noThesaurusEngine = new WorkshopBulkImportEngine(persistence, thesaurusCsvWriter);
        noThesaurusEngine.prepare(null, "fr", 7);
        noThesaurusEngine.init();

        noThesaurusEngine.addNoteList();

        verify(persistence, never()).addNote(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    // ---- deprecateConcepts / replaceValueByNewValue guard clauses ----

    @Test
    void deprecateConcepts_noDataLoaded_doesNotThrowAndSkipsPersistence() {
        engine.deprecateConcepts("TH1", 7);

        verify(persistence, never()).deprecateConcept(anyString(), anyString(), anyInt());
    }

    @Test
    void replaceValueByNewValue_blankThesaurus_doesNotThrow() {
        engine.replaceValueByNewValue("", 7);

        verify(persistence, never()).isIdExiste(anyString(), anyString());
    }

    @Test
    void loadCsvVariants_markLoadDone() throws Exception {
        stubUploadedContent("localid,skos:notation\nC1,N-001\n");
        engine.loadFileNotationCsv(event);
        assertTrue(engine.isLoadDone());

        stubUploadedContent("localid,skos:related\nC1,C2\n");
        engine.loadFileRelatedCsv(event);
        assertTrue(engine.isLoadDone());

        stubUploadedContent("localId,Uri\nC1,http://example.com/x\n");
        engine.loadFileAlignmentCsvToDelete(event);
        assertTrue(engine.isLoadDone());

        stubUploadedContent("identifier\nC1\n");
        engine.loadFileIdentifierCsv(event);
        assertTrue(engine.isLoadDone());

        stubUploadedContent("localid,skos:prefLabel@fr\nC1,Chat\n");
        engine.loadFileTraductionCsv(event);
        assertTrue(engine.isLoadDone());

        stubUploadedContent("localid,skos:altLabel@fr\nC1,Minou\n");
        engine.loadFileAltlabelCsv(event);
        assertTrue(engine.isLoadDone());

        stubUploadedContent("deprecated,isReplacedBy\nOLD,NEW\n");
        engine.loadFileCsvDeprecateConcepts(event);
        assertTrue(engine.isLoadDone());
    }

    @Test
    void addLists_afterLoad_callPersistenceWhenConceptExists() throws Exception {
        when(persistence.isIdExiste(anyString(), anyString())).thenReturn(true);
        when(persistence.updateNotation(anyString(), anyString(), anyString())).thenReturn(true);
        engine.setClearBefore(true);

        stubUploadedContent("localid,skos:notation\nC1,N-001\n");
        engine.loadFileNotationCsv(event);
        engine.addNotationList();
        verify(persistence).updateNotation("C1", "TH1", "N-001");
    }
}
