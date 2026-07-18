package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatImageBeanTest {

    @Mock private CandidatBean candidatBean;
    @Mock private CandidatMutationService candidatMutationService;
    @Mock private ThesaurusContext thesaurusContext;

    private CandidatImageBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatImageBean(candidatBean, candidatMutationService, thesaurusContext);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    private CandidatDto candidat() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");
        return candidat;
    }

    @Test
    void addNewImage_rejectsBlankUri() {
        bean.setUri(" ");

        bean.addNewImage(7);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Aucune URI insérée !"));
        verify(candidatMutationService, never()).addExternalImage(any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void addNewImage_savesImageAndRefreshesList() {
        bean.setUri("http://example.com/img.png");
        bean.setName("Image");
        bean.setCopyright("copy");
        bean.setCreator("creator");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(candidatMutationService.loadExternalImages("TH1", "C1")).thenReturn(List.of());

        bean.addNewImage(7);

        verify(candidatMutationService).addExternalImage("C1", "TH1", "Image", "copy", "http://example.com/img.png", "creator", 7);
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Image ajoutée avec succès"));
        assertNull(bean.getUri());
    }

    @Test
    void deleteImage_delegatesToServiceAndRefreshesList() {
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(candidatMutationService.loadExternalImages("TH1", "C1")).thenReturn(List.of());

        bean.deleteImage("http://example.com/img.png");

        verify(candidatMutationService).deleteExternalImage("TH1", "C1", "http://example.com/img.png");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Image supprimée avec succès"));
    }

    @Test
    void initImageDialog_resetsFields() {
        bean.setUri("u");
        bean.setName("n");
        bean.setCopyright("c");
        bean.setCreator("cr");

        bean.initImageDialog();

        assertNull(bean.getUri());
        assertNull(bean.getName());
        assertNull(bean.getCopyright());
        assertNull(bean.getCreator());
    }
}
