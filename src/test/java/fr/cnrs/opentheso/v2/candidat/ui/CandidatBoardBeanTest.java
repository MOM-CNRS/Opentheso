package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatBoardBeanTest {

    @Mock
    private ThesaurusViewBean thesaurusViewBean;
    @Mock
    private CandidatReadService candidatReadService;
    @Mock
    private UserSession userSession;

    private CandidatBoardBean bean;

    @BeforeEach
    void setUp() {
        bean = new CandidatBoardBean(thesaurusViewBean, candidatReadService, userSession);
        when(thesaurusViewBean.getId()).thenReturn("TH1");
        when(thesaurusViewBean.getSelectedLang()).thenReturn("fr");
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
