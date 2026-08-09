package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatExportBeanTest {

    @Mock private CandidatExportService candidatExportService;
    @Mock private CandidatBean candidatBean;

    private CandidatExportBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        bean = new CandidatExportBean(candidatExportService, candidatBean);
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
    }

    @Test
    void exportPendingCandidates_returnsStreamedContentOnSuccess() throws Exception {
        when(candidatBean.getActiveThesaurusId()).thenReturn("TH1");
        when(candidatBean.getCandidatList()).thenReturn(List.of(new CandidatDto()));
        when(candidatBean.getSelectedExportFormat()).thenReturn("skos");
        when(candidatExportService.exportPendingCandidates(any(), any(), any(), any()))
                .thenReturn(new CandidatExportService.ExportResult("data".getBytes(), "candidats.rdf", "application/rdf+xml"));

        var result = bean.exportPendingCandidates();

        assertNotNull(result);
        assertEquals("candidats.rdf", result.getName());
        verify(candidatBean).resetExportProgress();
        verify(candidatBean).setListCandidatsActivate(true);
    }

    @Test
    void exportPendingCandidates_returnsEmptyFileAndShowsErrorOnFailure() throws Exception {
        when(candidatBean.getActiveThesaurusId()).thenReturn("TH1");
        when(candidatBean.getCandidatList()).thenReturn(List.of());
        when(candidatBean.getSelectedExportFormat()).thenReturn("skos");
        when(candidatExportService.exportPendingCandidates(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Aucun candidat à exporter"));

        var result = bean.exportPendingCandidates();

        assertNotNull(result);
        assertEquals("export-error.txt", result.getName());
        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Aucun candidat à exporter"));
    }
}
