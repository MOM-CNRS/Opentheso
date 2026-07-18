package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptTransferMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatTransferBeanTest {

    @Mock private ConceptTransferMutationService conceptTransferMutationService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private CandidatBean candidatBean;

    private CandidatTransferBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatTransferBean(conceptTransferMutationService, thesaurusContext, userSession, candidatBean);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    @Test
    void initForCandidates_loadsAvailableThesauriWhenUserPresent() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptTransferMutationService.listAdminThesauri(7, false, "TH1", "fr"))
                .thenReturn(List.of(new ConceptWriteThesaurusOption("TH2", "Autre thésaurus")));

        bean.initForCandidates(List.of("C1", "C2"), "TH1");

        assertEquals(List.of("C1", "C2"), bean.getConceptIdsToMove());
        assertEquals("TH1", bean.getSourceThesaurusId());
        assertEquals(1, bean.getAvailableThesauri().size());
    }

    @Test
    void initForCandidates_emptyThesauriWhenNoCurrentUser() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.initForCandidates(List.of("C1"), "TH1");

        assertTrue(bean.getAvailableThesauri().isEmpty());
    }

    @Test
    void moveCandidates_rejectsWhenSelectionIncomplete() {
        bean.setSourceThesaurusId("TH1");
        bean.setTargetThesaurusId(null);
        bean.setConceptIdsToMove(List.of("C1"));

        bean.moveCandidates();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Aucune sélection !"));
        verify(conceptTransferMutationService, never()).moveConceptToThesaurus(any());
    }

    @Test
    void moveCandidates_rejectsWhenNoCurrentUser() {
        bean.setSourceThesaurusId("TH1");
        bean.setTargetThesaurusId("TH2");
        bean.setConceptIdsToMove(List.of("C1"));
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.moveCandidates();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Action non autorisée"));
    }

    @Test
    void moveCandidates_stopsAtFirstFailure() {
        bean.setSourceThesaurusId("TH1");
        bean.setTargetThesaurusId("TH2");
        bean.setConceptIdsToMove(List.of("C1", "C2"));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptTransferMutationService.moveConceptToThesaurus(any(MoveConceptToThesaurusCommand.class)))
                .thenReturn(MutationResult.failure("Le concept est verrouillé"));

        bean.moveCandidates();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Le concept est verrouillé"));
        verify(conceptTransferMutationService, org.mockito.Mockito.times(1)).moveConceptToThesaurus(any());
        verify(candidatBean, never()).initCandidatModule();
    }

    @Test
    void moveCandidates_movesAllConceptsAndRefreshesCandidateList() {
        bean.setSourceThesaurusId("TH1");
        bean.setTargetThesaurusId("TH2");
        bean.setConceptIdsToMove(List.of("C1", "C2"));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptTransferMutationService.moveConceptToThesaurus(any(MoveConceptToThesaurusCommand.class)))
                .thenReturn(MutationResult.ok("Déplacé"));

        bean.moveCandidates();

        verify(conceptTransferMutationService, org.mockito.Mockito.times(2)).moveConceptToThesaurus(any());
        verify(candidatBean).initCandidatModule();
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Le déplacement a réussi"));
    }
}
