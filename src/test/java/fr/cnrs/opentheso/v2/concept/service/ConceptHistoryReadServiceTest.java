package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.shared.repository.HistoryQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptHistoryReadServiceTest {

    @Mock
    private HistoryQueryRepository historyQueryRepository;

    private ConceptHistoryReadService service;

    @BeforeEach
    void setUp() {
        service = new ConceptHistoryReadService(historyQueryRepository);
    }

    @Test
    void load_mapsAllHistorySections() {
        when(historyQueryRepository.findTermHistories("T1", "TH1")).thenReturn(List.<Object[]>of(
                new Object[]{"Label", "fr", "CREATE", Timestamp.valueOf("2024-01-01 10:00:00"), "admin"}
        ));
        when(historyQueryRepository.findSynonymHistories("T1", "TH1")).thenReturn(List.of());
        when(historyQueryRepository.findRelationHistories("T1", "TH1")).thenReturn(List.<Object[]>of(
                new Object[]{"C2", "NT1", "ADD", Timestamp.valueOf("2024-01-02 10:00:00"), "editor"}
        ));
        when(historyQueryRepository.findNoteHistories("C1", "T1", "TH1")).thenReturn(List.<Object[]>of(
                new Object[]{"Note", "definition", "fr", "UPDATE", Timestamp.valueOf("2024-01-03 10:00:00"), "admin"}
        ));

        var overview = service.load("TH1", "C1", "T1");

        assertEquals(1, overview.labels().size());
        assertEquals("Label", overview.labels().get(0).value());
        assertEquals("NT1", overview.relations().get(0).role());
        assertEquals("definition", overview.notes().get(0).noteType());
    }

    @Test
    void load_withBlankPreferredTermId_returnsEmptyOverview() {
        var overview = service.load("TH1", "C1", " ");

        assertTrue(overview.isEmpty());
    }
}
