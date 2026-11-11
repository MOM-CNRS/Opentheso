package fr.cnrs.opentheso.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.candidat.policy.CandidatAccessPolicy;
import fr.cnrs.opentheso.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.candidat.service.CandidatSkosImportService;
import fr.cnrs.opentheso.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatBoardBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private CandidatReadService candidatReadService;
    @Mock
    private CandidatProcessService candidatProcessService;
    @Mock
    private CandidatExportService candidatExportService;
    @Mock
    private CandidatSkosImportService candidatSkosImportService;
    @Mock
    private CandidatAccessPolicy candidatAccessPolicy;
    @Mock
    private ThesaurusPreferencesProvider thesaurusPreferencesProvider;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private UserSession userSession;
    @Mock
    private RightsService rightsService;
    @Mock
    private LocaleBean localeBean;

    private CandidatBoardBean bean;

    @BeforeEach
    void setUp() {
        bean = new CandidatBoardBean(
                thesaurusViewBean,
                candidatReadService,
                candidatProcessService,
                candidatExportService,
                candidatSkosImportService,
                candidatAccessPolicy,
                thesaurusPreferencesProvider,
                thesaurusContext,
                userSession,
                rightsService,
                localeBean
        );
        lenient().when(thesaurusViewBean.getId()).thenReturn("TH1");
        lenient().when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
    }

    @Test
    void load_mapsPendingAcceptedAndRejected() {
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), eq(CandidatStatusCode.PENDING), isNull()))
                .thenReturn(List.of(candidat("c1", "Fibule", "Ada", 2, 4)));
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), eq(CandidatStatusCode.ACCEPTED), isNull()))
                .thenReturn(List.of(candidat("c2", "Patère", "Luc", 7, 3)));
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), eq(CandidatStatusCode.REJECTED), isNull()))
                .thenReturn(List.of());
        when(candidatReadService.countByStatus("TH1", CandidatStatusCode.PENDING)).thenReturn(8);
        when(candidatReadService.countByStatus("TH1", CandidatStatusCode.ACCEPTED)).thenReturn(13);
        when(candidatReadService.countByStatus("TH1", CandidatStatusCode.REJECTED)).thenReturn(0);

        bean.load();

        assertEquals(1, bean.getPending().size());
        assertEquals("Fibule", bean.getPending().get(0).title());
        assertEquals("Ada · 2026-06-15", bean.getPending().get(0).meta());
        assertEquals("+2 −0", bean.getPending().get(0).votes());
        assertEquals("candidat", bean.getPending().get(0).openType());
        assertEquals("concept", bean.getAccepted().get(0).openType());
        assertTrue(bean.getRejected().isEmpty());
        assertEquals(8, bean.getPendingCount());
        assertEquals(13, bean.getAcceptedCount());
        assertEquals(0, bean.getRejectedCount());
        assertTrue(bean.isCapped("attente"));
        assertFalse(bean.isCapped("rejete"));
        verify(thesaurusViewBean).getId();
    }

    @Test
    void boardLinkVisible_whenTheUserAdministersTheThesaurusProject() {
        when(rightsService.isAdminOfThesaurusProject(userSession, "TH1")).thenReturn(true);

        assertTrue(bean.isBoardLinkVisible());

        when(rightsService.isAdminOfThesaurusProject(userSession, "TH1")).thenReturn(false);
        assertFalse(bean.isBoardLinkVisible());
    }

    @Test
    void refreshBadgeCount_countsPendingCandidatesOnly() {
        when(rightsService.isAdminOfThesaurusProject(userSession, "TH1")).thenReturn(true);
        when(candidatReadService.countByStatus("TH1", CandidatStatusCode.PENDING)).thenReturn(3);

        bean.refreshBadgeCount();

        assertEquals(3, bean.getBadgeCount());
        verify(candidatReadService, never()).searchByStatus(any(), any(), anyInt(), any());
    }

    @Test
    void exportEnabled_followsTheVisibleList() {
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), eq(CandidatStatusCode.PENDING), isNull()))
                .thenReturn(List.of());
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), eq(CandidatStatusCode.ACCEPTED), isNull()))
                .thenReturn(List.of(candidat("c2", "Patère", "Luc", 7, 3)));
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), eq(CandidatStatusCode.REJECTED), isNull()))
                .thenReturn(List.of());
        when(candidatReadService.countByStatus(eq("TH1"), anyInt())).thenReturn(0);

        bean.load();

        assertFalse(bean.isExportEnabled());
        bean.setActiveTab("insere");
        assertTrue(bean.isExportEnabled());
        bean.setActiveTab("rejete");
        assertFalse(bean.isExportEnabled());
    }

    @Test
    void exportCandidates_exportsTheVisibleListAsCsv() {
        when(candidatAccessPolicy.canExport(userSession, "TH1")).thenReturn(true);
        CandidatDto inserted = candidat("c2", "Patère", "Luc", 7, 3);
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), eq(CandidatStatusCode.ACCEPTED), isNull()))
                .thenReturn(List.of(inserted));
        when(candidatProcessService.exportProcessedCandidatesCsv(any())).thenReturn("Id;Candidat\n".getBytes());
        bean.setActiveTab("insere");

        bean.exportCandidates();

        verify(candidatProcessService).exportProcessedCandidatesCsv(List.of(inserted));
    }

    @Test
    void reopenSelected_putsRejectedCandidatesBackInProgress() {
        when(candidatAccessPolicy.canProcess(eq(userSession), eq("TH1"))).thenReturn(true);
        CandidatDto rejected = candidat("c3", "Fibule", "Ada", 0, 4);
        when(candidatReadService.loadByStatus("TH1", "fr", CandidatStatusCode.REJECTED))
                .thenReturn(List.of(rejected));
        when(candidatProcessService.reactivateCandidatesBatch(List.of(rejected))).thenReturn(null);

        bean.toggleSelect("c3");
        bean.reopenSelected();

        verify(candidatProcessService).reactivateCandidatesBatch(List.of(rejected));
        assertEquals("attente", bean.getActiveTab());
        assertFalse(bean.isHasSelection());
    }

    @Test
    void hasSelection_turnsOnOnlyAfterACandidateIsChecked() {
        assertFalse(bean.isHasSelection());

        bean.toggleSelect("c1");

        assertTrue(bean.isHasSelection());
        assertTrue(bean.isSelected("c1"));

        bean.toggleSelect("c1");

        assertFalse(bean.isHasSelection());
        assertFalse(bean.isSelected("c1"));
    }

    @Test
    void toggleSelected_checksThePostedCandidate() {
        bean.setSelectId("c1");
        bean.toggleSelected();

        assertTrue(bean.isSelected("c1"));
        assertTrue(bean.isHasSelection());

        bean.setSelectId("c1");
        bean.toggleSelected();

        assertFalse(bean.isSelected("c1"));
        assertFalse(bean.isHasSelection());
    }

    @Test
    void toggleMine_keepsOnlyCurrentUserRows() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(4);
        when(candidatReadService.searchByStatus(eq("TH1"), eq("fr"), anyInt(), any()))
                .thenReturn(List.of(
                        candidat("c1", "Mien", "moi", 1, 4),
                        candidat("c2", "Autre", "lui", 0, 9)
                ));

        bean.setMineOnly(true);
        bean.toggleMine();

        assertEquals(1, bean.getPending().size());
        assertEquals("Mien", bean.getPending().get(0).title());
        assertEquals(1, bean.getPendingCount());
    }

    private static CandidatDto candidat(String id, String label, String author, int votes, int userId) {
        var dto = new CandidatDto();
        dto.setIdConcepte(id);
        dto.setNomPref(label);
        dto.setCreatedBy(author);
        dto.setCreatedById(userId);
        dto.setNbrVote(votes);
        dto.setCreationDate(Date.valueOf(LocalDate.of(2026, Month.JUNE, 15)));
        return dto;
    }
}
