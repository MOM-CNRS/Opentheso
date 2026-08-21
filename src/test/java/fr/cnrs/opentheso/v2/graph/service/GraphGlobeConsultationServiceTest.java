package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.v2.graph.model.GraphGlobeNode;
import fr.cnrs.opentheso.v2.shared.repository.GraphGlobeQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphGlobeConsultationServiceTest {

    @Mock
    private GraphGlobeQueryRepository graphGlobeQueryRepository;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;

    private GraphGlobeConsultationService service;

    @BeforeEach
    void setUp() {
        service = new GraphGlobeConsultationService(graphGlobeQueryRepository, authenticatedUserSource);
    }

    @Test
    void loadGlobe_mapsStatusAndLabel() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(graphGlobeQueryRepository.findGlobeConcepts("TH1", "fr", false, 15001))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"C1", "Adobe", "C"},
                        new Object[]{"C2", "", "DEP"}
                ));

        var response = service.loadGlobe("TH1", "fr");

        assertFalse(response.truncated());
        assertEquals(2, response.nodes().size());
        GraphGlobeNode first = response.nodes().get(0);
        assertEquals("C1", first.id());
        assertEquals("Adobe", first.label());
        assertEquals("valide", first.status());
        assertEquals("C2", response.nodes().get(1).id());
        assertEquals("C2", response.nodes().get(1).label());
        assertEquals("deprecie", response.nodes().get(1).status());
        verify(graphGlobeQueryRepository).findGlobeConcepts(eq("TH1"), eq("fr"), eq(false), anyInt());
    }

    @Test
    void loadGlobe_includesCandidatesWhenLoggedIn() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(graphGlobeQueryRepository.findGlobeConcepts("TH1", "fr", true, 15001))
                .thenReturn(List.<Object[]>of(new Object[]{"CA1", "Aiguille", "CA"}));

        assertEquals("candidat", service.loadGlobe("TH1", "fr").nodes().get(0).status());
        verify(graphGlobeQueryRepository).findGlobeConcepts(eq("TH1"), eq("fr"), eq(true), anyInt());
    }

    @Test
    void loadGlobe_truncatesAfterCap() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < 15001; i++) {
            rows.add(new Object[]{"C" + i, "L" + i, "C"});
        }
        when(graphGlobeQueryRepository.findGlobeConcepts(eq("TH1"), eq("fr"), eq(false), anyInt()))
                .thenReturn(rows);

        var response = service.loadGlobe("TH1", "fr");

        assertTrue(response.truncated());
        assertEquals(15000, response.nodes().size());
    }

    @Test
    void loadNeighborhood_groupsRolesAndCaps() {
        when(graphGlobeQueryRepository.findNeighborhood("TH1", "C1", "fr", 120))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"P1", "Parent", "BT"},
                        new Object[]{"N1", "Enfant", "NT"},
                        new Object[]{"R1", "Associé", "RT"},
                        new Object[]{"", "skip", "BT"}
                ));

        var response = service.loadNeighborhood("TH1", "fr", "C1");

        assertEquals("C1", response.id());
        assertEquals(1, response.broader().size());
        assertEquals("TG", response.broader().get(0).role());
        assertEquals("P1", response.broader().get(0).id());
        assertEquals("TS", response.narrower().get(0).role());
        assertEquals("TA", response.related().get(0).role());
    }

    @Test
    void loadNeighborhood_emptyWhenBlank() {
        var response = service.loadNeighborhood("", "fr", "C1");
        assertTrue(response.broader().isEmpty());
        assertTrue(response.narrower().isEmpty());
        assertTrue(response.related().isEmpty());
    }
}
