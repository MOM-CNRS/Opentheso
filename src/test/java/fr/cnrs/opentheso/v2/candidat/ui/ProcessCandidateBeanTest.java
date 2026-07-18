package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessCandidateBeanTest {

    @Mock private CandidatBean candidatBean;
    @Mock private CandidatProcessService candidatProcessService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ThesaurusPreferencesProvider thesaurusPreferencesProvider;

    private ProcessCandidateBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new ProcessCandidateBean(candidatBean, candidatProcessService, thesaurusContext, userSession, thesaurusPreferencesProvider);
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
        candidat.setNomPref("Concept 1");
        candidat.setCreatedById(3);
        return candidat;
    }

    @Test
    void reset_clearsSelectionAndAdminMessage() {
        bean.setAdminMessage("old");

        bean.reset(candidat());
        bean.reset(null);

        assertNull(bean.getSelectedCandidate());
        assertNull(bean.getAdminMessage());
    }

    @Test
    void insertCandidat_rejectsWhenNoSelection() throws Exception {
        bean.insertCandidat();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Pas de candidat sélectionné"));
        verify(candidatProcessService, never()).insertCandidate(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void insertCandidat_showsErrorWhenPersistenceFails() throws Exception {
        bean.reset(candidat());
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatProcessService.insertCandidate(any(), any(), eq(7))).thenReturn(true);

        bean.insertCandidat();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Erreur d'insertion"));
        verify(candidatProcessService, never()).afterCandidateAccepted(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    void insertCandidat_acceptsAndNotifiesAuthorWhenAlertEnabled() throws Exception {
        bean.reset(candidat());
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(candidatProcessService.insertCandidate(any(), any(), eq(7))).thenReturn(false);
        when(candidatProcessService.isAlertMailEnabled(3)).thenReturn(true);
        when(candidatProcessService.resolveUserMail(3)).thenReturn("author@example.com");
        when(candidatProcessService.sendMail(any(), any(), any())).thenReturn(false);
        when(thesaurusPreferencesProvider.findPreferences("TH1")).thenReturn(Optional.of(new Preferences()));

        bean.insertCandidat();

        verify(candidatProcessService).sendMail(eq("author@example.com"), any(), any());
        verify(candidatProcessService).afterCandidateAccepted(any(), eq(7), eq("admin"), any(Preferences.class));
        verify(candidatBean).initCandidatModule();
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Candidat inséré avec succès"));
        assertNull(bean.getSelectedCandidate());
    }

    @Test
    void insertCandidat_skipsMailWhenAlertDisabled() throws Exception {
        bean.reset(candidat());
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatProcessService.insertCandidate(any(), any(), eq(7))).thenReturn(false);
        when(candidatProcessService.isAlertMailEnabled(3)).thenReturn(false);
        when(thesaurusPreferencesProvider.findPreferences("TH1")).thenReturn(Optional.empty());

        bean.insertCandidat();

        verify(candidatProcessService, never()).sendMail(any(), any(), any());
        verify(candidatProcessService, never()).afterCandidateAccepted(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    void rejectCandidat_rejectsWhenNoSelection() throws Exception {
        bean.rejectCandidat();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Pas de candidat sélectionné"));
    }

    @Test
    void rejectCandidat_showsErrorWhenPersistenceFails() throws Exception {
        bean.reset(candidat());
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(candidatProcessService.rejectCandidate(any(), any(), eq(7))).thenReturn(true);

        bean.rejectCandidat();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Erreur d'insertion"));
        verify(candidatProcessService, never()).afterCandidateRejected(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void rejectCandidat_rejectsAndRefreshesList() throws Exception {
        bean.reset(candidat());
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(candidatProcessService.rejectCandidate(any(), any(), eq(7))).thenReturn(false);
        when(candidatProcessService.isAlertMailEnabled(3)).thenReturn(false);

        bean.rejectCandidat();

        verify(candidatProcessService).afterCandidateRejected(any(), eq(7), eq("admin"));
        verify(candidatBean).initCandidatModule();
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Candidat(s) rejeté(s) avec succès"));
    }

    @Test
    void insertListCandidat_rejectsWhenNoCandidatesSelected() throws Exception {
        when(candidatBean.getSelectedCandidates()).thenReturn(List.of());

        bean.insertListCandidat();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Pas de candidat sélectionné"));
        verify(candidatProcessService, never()).insertCandidate(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void insertListCandidat_resolvesPreferencesAndAcceptsAllCandidates() throws Exception {
        var candidate1 = candidat();
        when(candidatBean.getSelectedCandidates()).thenReturn(List.of(candidate1));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(candidatBean.getPreferredLang()).thenReturn("fr");
        var preferences = new Preferences();
        when(thesaurusPreferencesProvider.findPreferences("TH1")).thenReturn(Optional.of(preferences));
        when(candidatProcessService.insertCandidate(any(), any(), eq(7))).thenReturn(false);
        when(candidatProcessService.isAlertMailEnabled(3)).thenReturn(false);

        bean.insertListCandidat();

        verify(candidatProcessService).prepareCandidatesForAccept(any(), eq("TH1"), eq("fr"));
        verify(candidatProcessService).afterCandidateAccepted(candidate1, 7, "admin", preferences);
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Candidats insérés avec succès"));
    }

    @Test
    void insertListCandidat_stopsOnFirstInsertionFailure() throws Exception {
        var candidate1 = candidat();
        when(candidatBean.getSelectedCandidates()).thenReturn(List.of(candidate1));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(candidatBean.getPreferredLang()).thenReturn("fr");
        when(thesaurusPreferencesProvider.findPreferences("TH1")).thenReturn(Optional.empty());
        when(candidatProcessService.insertCandidate(any(), any(), eq(7))).thenReturn(true);

        bean.insertListCandidat();

        verify(candidatProcessService, never()).afterCandidateAccepted(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    void rejectCandidatList_rejectsWhenNoCandidatesSelected() throws Exception {
        when(candidatBean.getSelectedCandidates()).thenReturn(null);

        bean.rejectCandidatList();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Pas de candidat sélectionné"));
    }

    @Test
    void rejectCandidatList_rejectsAllSelectedCandidates() throws Exception {
        var candidate1 = candidat();
        when(candidatBean.getSelectedCandidates()).thenReturn(List.of(candidate1));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(candidatProcessService.rejectCandidate(any(), any(), eq(7))).thenReturn(false);
        when(candidatProcessService.isAlertMailEnabled(3)).thenReturn(false);

        bean.rejectCandidatList();

        verify(candidatProcessService).afterCandidateRejected(candidate1, 7, "admin");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Candidats insérés avec succès"));
    }

    @Test
    void exportProcessedCandidates_returnsNullWhenNoData() {
        when(candidatProcessService.exportProcessedCandidatesCsv(any())).thenReturn(null);

        assertNull(bean.exportProcessedCandidates(List.of()));
    }
}
