package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.TraductionDto;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatTranslationBeanTest {

    @Mock private CandidatBean candidatBean;
    @Mock private CandidatMutationService candidatMutationService;
    @Mock private UserSession userSession;
    @Mock private V2LocaleBean localeBean;

    private CandidatTranslationBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatTranslationBean(candidatBean, candidatMutationService, userSession, localeBean);
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
        candidat.setIdTerm("T1");
        candidat.setLang("fr");
        candidat.setUserId(7);
        candidat.setTraductions(List.of(TraductionDto.builder().langue("en").traduction("Value").build()));
        return candidat;
    }

    @Test
    void init_populatesFieldsFromTraductionDto() {
        bean.init(TraductionDto.builder().langue("en").traduction("Value").build());

        assertEquals("en", bean.getLangage());
        assertEquals("Value", bean.getTraduction());
        assertEquals("en", bean.getLangageOld());
        assertEquals("Value", bean.getTraductionOld());
    }

    @Test
    void init_warnsWhenNoLanguageAvailable() {
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(candidatBean.getLanguagesOfTheso()).thenReturn(List.of(
                NodeLangTheso.builder().code("fr").build(),
                NodeLangTheso.builder().code("en").build()));

        bean.init();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Le candidat est traduit dans toutes les langues du thésaurus"));
    }

    @Test
    void init_populatesFilteredLanguagesWhenAvailable() {
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(candidatBean.getLanguagesOfTheso()).thenReturn(List.of(
                NodeLangTheso.builder().code("fr").build(),
                NodeLangTheso.builder().code("en").build(),
                NodeLangTheso.builder().code("de").build()));

        bean.init();

        assertEquals(1, bean.getNodeLanguesFiltered().size());
        assertEquals("de", bean.getNodeLanguesFiltered().get(0).getCode());
    }

    @Test
    void deleteTraduction_delegatesToService() {
        bean.setLangage("en");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(candidatMutationService.loadCandidateTranslations("C1", "TH1", "fr")).thenReturn(List.of());
        when(localeBean.getMsg("candidat.traduction.msg2")).thenReturn("Traduction supprimée");

        bean.deleteTraduction();

        verify(candidatMutationService).deleteCandidateTranslation("TH1", "T1", "en");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Traduction supprimée"));
    }

    @Test
    void updateTraduction_delegatesToService() {
        bean.setLangage("en");
        bean.setTraduction("New value");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(candidatMutationService.loadCandidateTranslations("C1", "TH1", "fr")).thenReturn(List.of());
        when(localeBean.getMsg("candidat.traduction.msg3")).thenReturn("Traduction modifiée");

        bean.updateTraduction();

        verify(candidatMutationService).updateTermLabel("New value", "TH1", "en", "T1");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Traduction modifiée"));
    }

    @Test
    void addTraductionCandidat_rejectsDuplicateLabel() {
        bean.setNewTraduction("Value");
        bean.setNewLangage("en");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(candidatMutationService.isLabelExistIgnoreCase("Value", "TH1", "en")).thenReturn(true);

        bean.addTraductionCandidat();

        verify(candidatMutationService, never()).addCandidateTranslation(any(Term.class), anyInt());
    }

    @Test
    void addTraductionCandidat_addsTranslationWhenLabelIsNew() {
        bean.setNewTraduction("Value");
        bean.setNewLangage("en");
        when(candidatBean.getCandidatSelected()).thenReturn(candidat());
        when(candidatMutationService.isLabelExistIgnoreCase("Value", "TH1", "en")).thenReturn(false);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatMutationService.loadCandidateTranslations("C1", "TH1", "fr")).thenReturn(List.of());
        when(localeBean.getMsg("candidat.traduction.msg1")).thenReturn("Traduction ajoutée");

        bean.addTraductionCandidat();

        verify(candidatMutationService).addCandidateTranslation(any(Term.class), eq(7));
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Traduction ajoutée"));
    }
}
