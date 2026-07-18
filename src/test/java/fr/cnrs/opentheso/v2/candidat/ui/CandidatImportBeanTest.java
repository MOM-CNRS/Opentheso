package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatLanguageService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatSkosImportService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatImportBeanTest {

    @Mock private CandidatSkosImportService candidatSkosImportService;
    @Mock private CandidatLanguageService candidatLanguageService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private CandidatBean candidatBean;
    @Mock private FileUploadEvent event;
    @Mock private UploadedFile uploadedFile;
    @Mock private FacesContext facesContext;

    private CandidatImportBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatImportBean(candidatSkosImportService, candidatLanguageService, thesaurusContext, userSession, candidatBean);
        lenient().when(event.getPhaseId()).thenReturn(PhaseId.INVOKE_APPLICATION);
        lenient().when(event.getFile()).thenReturn(uploadedFile);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    @Test
    void init_resetsStateAndLoadsLanguages() {
        when(candidatLanguageService.listAllLanguages()).thenReturn(java.util.List.of());
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");

        bean.init();

        verify(thesaurusContext).syncFromViewParams();
        assertFalse(bean.isLoadDone());
        assertTrue(bean.getUri().isEmpty());
        assertEquals("fr", bean.getSelectedLang());
    }

    @Test
    void loadFileSkos_requeuesEventWhenNotInInvokeApplicationPhase() throws Exception {
        when(event.getPhaseId()).thenReturn(PhaseId.APPLY_REQUEST_VALUES);

        bean.loadFileSkos(event);

        verify(event).setPhaseId(PhaseId.INVOKE_APPLICATION);
        verify(event).queue();
        assertFalse(bean.isLoadDone());
    }

    @Test
    void loadFileSkos_parsesDocumentOnSuccess() throws Exception {
        when(uploadedFile.getInputStream()).thenReturn(new ByteArrayInputStream("<xml/>".getBytes()));
        var document = new SKOSXmlDocument();
        document.setConceptList(new java.util.ArrayList<>(java.util.List.of(new SKOSResource())));
        when(candidatSkosImportService.loadSkosFile(any(), eq(0), any(), any()))
                .thenReturn(new CandidatSkosImportService.SkosLoadResult(document, "http://uri", 1));

        bean.loadFileSkos(event);

        assertTrue(bean.isLoadDone());
        assertEquals(1, bean.getTotal());
        assertEquals("http://uri", bean.getUri());
    }

    @Test
    void loadFileSkos_capturesErrorMessageOnFailure() throws Exception {
        when(uploadedFile.getInputStream()).thenReturn(new ByteArrayInputStream("bad".getBytes()));
        when(candidatSkosImportService.loadSkosFile(any(), anyInt(), any(), any()))
                .thenThrow(new RuntimeException("Fichier invalide"));

        bean.loadFileSkos(event);

        assertTrue(bean.isLoadDone());
        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Fichier invalide"));
    }

    @Test
    void addSkosCandidatToBDD_rejectsWhenNoDocumentLoaded() throws Exception {
        bean.addSkosCandidatToBDD();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Aucun fichier candidat chargé"));
        verify(candidatSkosImportService, never()).importCandidatesForThesaurus(any(), any(), anyInt(), any(), any());
    }

    @Test
    void addSkosCandidatToBDD_rejectsWhenNoThesaurusOrUser() {
        var document = new SKOSXmlDocument();
        document.setConceptList(new java.util.ArrayList<>(java.util.List.of(new SKOSResource())));
        bean.setSkosXmlDocument(document);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("");

        bean.addSkosCandidatToBDD();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Contexte thésaurus ou utilisateur invalide"));
    }

    @Test
    void addSkosCandidatToBDD_importsConceptsAndMarksComplete() throws Exception {
        var document = new SKOSXmlDocument();
        document.setConceptList(new java.util.ArrayList<>(java.util.List.of(new SKOSResource())));
        bean.setSkosXmlDocument(document);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            bean.addSkosCandidatToBDD();
        }

        verify(candidatBean).prepareImportProgress(1);
        verify(candidatSkosImportService).importCandidatesForThesaurus(eq(document), eq("TH1"), eq(7), eq("fr"), any());
        verify(candidatBean).setListCandidatsActivate(true);
        verify(candidatBean).setProgressBarValue(100);
    }

    @Test
    void addSkosCandidatToBDD_showsErrorWhenImportThrows() throws Exception {
        var document = new SKOSXmlDocument();
        document.setConceptList(new java.util.ArrayList<>(java.util.List.of(new SKOSResource())));
        bean.setSkosXmlDocument(document);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        doThrow(new java.io.IOException("boom"))
                .when(candidatSkosImportService).importCandidatesForThesaurus(any(), any(), anyInt(), any(), any());

        bean.addSkosCandidatToBDD();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Erreur pendant l'import des candidats"));
    }
}
