package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptHistoryEntry;
import fr.cnrs.opentheso.v2.concept.model.ConceptHistoryOverview;
import fr.cnrs.opentheso.v2.concept.service.ConceptHistoryReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptHistoryBeanTest {

    @Mock
    private ConceptHistoryReadService conceptHistoryReadService;

    private ConceptHistoryBean bean;

    @BeforeEach
    void setUp() {
        bean = new ConceptHistoryBean(conceptHistoryReadService);
    }

    @Test
    void load_withValidIds_delegatesToHistoryReadService() {
        var overview = new ConceptHistoryOverview(
                List.of(new ConceptHistoryEntry("Label", "fr", "CREATE", new Date(), "admin", null, null)),
                List.of(),
                List.of(),
                List.of()
        );
        when(conceptHistoryReadService.load("TH1", "C1", "T1")).thenReturn(overview);

        bean.load("TH1", "C1", "T1");

        assertNotNull(bean.getOverview());
        assertEquals(1, bean.getOverview().labels().size());
        verify(conceptHistoryReadService).load("TH1", "C1", "T1");
    }

    @Test
    void load_withBlankPreferredTermId_returnsEmptyOverview() {
        bean.load("TH1", "C1", " ");

        assertNotNull(bean.getOverview());
        assertTrue(bean.getOverview().isEmpty());
    }

    @Test
    void reset_clearsOverview() {
        var overview = new ConceptHistoryOverview(List.of(), List.of(), List.of(), List.of());
        when(conceptHistoryReadService.load("TH1", "C1", "T1")).thenReturn(overview);
        bean.load("TH1", "C1", "T1");

        bean.reset();

        assertNull(bean.getOverview());
    }
}
