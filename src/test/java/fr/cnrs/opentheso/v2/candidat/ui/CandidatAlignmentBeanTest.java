package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatAlignmentBeanTest {

    @Mock private ConceptAlignmentMutationService conceptAlignmentMutationService;
    @Mock private CandidatMutationService candidatMutationService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;

    private CandidatAlignmentBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new CandidatAlignmentBean(conceptAlignmentMutationService, candidatMutationService, thesaurusContext, userSession);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    @Test
    void loadAlignmentTypes_populatesFromService() {
        when(conceptAlignmentMutationService.listAlignmentTypes())
                .thenReturn(List.of(new ConceptWriteAlignmentType(1, "exact", "exactMatch")));

        bean.loadAlignmentTypes();

        assertEquals(1, bean.getAlignmentTypes().size());
    }

    @Test
    void reset_selectsFirstAlignmentTypeWhenAvailable() {
        when(conceptAlignmentMutationService.listAlignmentTypes())
                .thenReturn(List.of(new ConceptWriteAlignmentType(3, "exact", "exactMatch")));

        bean.reset();

        assertEquals(3, bean.getManualAlignmentType());
        assertEquals("", bean.getManualAlignmentUri());
    }

    @Test
    void reset_defaultsToMinusOneWhenNoTypesAvailable() {
        when(conceptAlignmentMutationService.listAlignmentTypes()).thenReturn(List.of());

        bean.reset();

        assertEquals(-1, bean.getManualAlignmentType());
    }

    @Test
    void addManualAlignment_rejectsNullCandidate() {
        bean.addManualAlignment(null);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Action non autorisée"));
        verify(conceptAlignmentMutationService, never()).addManualAlignment(any());
    }

    @Test
    void addManualAlignment_rejectsBlankUri() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        bean.setManualAlignmentUri(" ");

        bean.addManualAlignment(candidat);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Veuillez saisir une valeur !"));
    }

    @Test
    void addManualAlignment_rejectsInvalidUrl() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        bean.setManualAlignmentUri("not-a-url");

        bean.addManualAlignment(candidat);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("L'URL n'est pas valide !"));
    }

    @Test
    void addManualAlignment_rejectsWhenNoCurrentUser() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        bean.setManualAlignmentUri("http://example.com/c1");
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.addManualAlignment(candidat);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Action non autorisée"));
    }

    @Test
    void addManualAlignment_showsErrorMessageWhenServiceFails() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        bean.setManualAlignmentUri("http://example.com/c1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(conceptAlignmentMutationService.addManualAlignment(any(AddManualAlignmentCommand.class)))
                .thenReturn(MutationResult.failure("Erreur serveur"));

        bean.addManualAlignment(candidat);

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Erreur serveur"));
        verify(candidatMutationService, never()).loadAlignments(any(), any());
    }

    @Test
    void addManualAlignment_addsAlignmentAndRefreshesCandidate() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        bean.setManualAlignmentUri("http://example.com/c1");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(conceptAlignmentMutationService.addManualAlignment(any(AddManualAlignmentCommand.class)))
                .thenReturn(MutationResult.ok("Alignement ajouté"));
        when(candidatMutationService.loadAlignments("C1", "TH1")).thenReturn(List.of());

        bean.addManualAlignment(candidat);

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Alignement ajouté"));
        verify(candidatMutationService).loadAlignments("C1", "TH1");
    }
}
