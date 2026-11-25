package fr.cnrs.opentheso.graph.service;

import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.shared.repository.GraphGlobeQueryRepository;
import fr.cnrs.opentheso.shared.repository.GraphGlobeQueryRepository.NeighborPage;
import fr.cnrs.opentheso.concept.policy.ThesaurusConsultationAccessPolicy;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.shared.session.AuthenticatedUserSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphGlobeConsultationServiceTest {

    @Mock
    private GraphGlobeQueryRepository graphGlobeQueryRepository;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;
    @Mock
    private PreferencesRepository preferencesRepository;
    @Mock
    private ThesaurusConsultationAccessPolicy consultationAccessPolicy;
    @Mock
    private RightsService rightsService;

    private GraphGlobeConsultationService service;

    @BeforeEach
    void setUp() {
        service = new GraphGlobeConsultationService(
                graphGlobeQueryRepository, authenticatedUserSource, preferencesRepository,
                consultationAccessPolicy, rightsService);
        lenient().when(preferencesRepository.findByIdThesaurus(any())).thenReturn(Optional.empty());
    }

    @Test
    void loadNeighborhood_groupsRolesWithTotals() {
        when(consultationAccessPolicy.canConsult("TH1", false)).thenReturn(true);
        center("C1", "Adobe", "D", false);
        when(graphGlobeQueryRepository.findNeighborhood(
                "TH1", "C1", "fr", false, false, new NeighborPage(0, 30, 0, 0, 30)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"P1", "Parent", "BTG", "D", "BT", 1L},
                        new Object[]{"N1", "", "NT", "DEP", "NT", 312L},
                        new Object[]{"R1", "Associé", "RT", "D", "RT", 1L},
                        new Object[]{"", "ignoré", "BT", "D", "BT", 1L}
                ));

        var response = service.loadNeighborhood("TH1", "fr", "C1", false, -5, 30, 0, 0);

        assertTrue(response.found());
        assertEquals("Adobe", response.label());
        assertEquals(1, response.broader().items().size());
        assertEquals("TG", response.broader().items().get(0).role());
        assertEquals("BTG", response.broader().items().get(0).type());
        assertEquals(312, response.narrower().total());
        assertEquals(30, response.narrower().offset());
        assertEquals("N1", response.narrower().items().get(0).label());
        assertEquals("deprecie", response.narrower().items().get(0).status());
        assertEquals("TA", response.related().items().get(0).role());
    }

    @Test
    void loadNeighborhood_withoutNeighborsIsFound() {
        when(consultationAccessPolicy.canConsult("TH1", false)).thenReturn(true);
        center("C1", "Adobe", "D", false);

        var response = service.loadNeighborhood("TH1", "fr", "C1", false, 0, 0, 0, 0);

        assertTrue(response.found());
        assertEquals(0, response.broader().total());
        assertTrue(response.narrower().items().isEmpty());
    }

    @Test
    void loadNeighborhood_loggedInSeesCandidatesAndPrivateGroups() {
        when(consultationAccessPolicy.canConsult("TH1", false)).thenReturn(true);
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        center("C1", "Adobe", "CA", true);

        assertEquals("candidat", service.loadNeighborhood("TH1", "fr", "C1", false, 0, 0, 0, 0).status());
        verify(graphGlobeQueryRepository).findNeighborhood(
                eq("TH1"), eq("C1"), eq("fr"), eq(true), eq(true), any());
    }

    @Test
    void loadNeighborhood_hiddenCenterStopsThere() {
        when(consultationAccessPolicy.canConsult("TH1", false)).thenReturn(true);
        when(graphGlobeQueryRepository.findCenter("TH1", "C1", "fr", false)).thenReturn(Optional.empty());

        assertFalse(service.loadNeighborhood("TH1", "fr", "C1", false, 0, 0, 0, 0).found());
        verify(graphGlobeQueryRepository, never()).findNeighborhood(
                any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void loadNeighborhood_privateThesaurusRefusedToGuestAndStranger() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.empty(), Optional.of(7));
        when(rightsService.roleOnThesaurus(7, "TH1")).thenReturn(Optional.empty());

        assertFalse(service.loadNeighborhood("TH1", "fr", "C1", false, 0, 0, 0, 0).found());
        assertFalse(service.loadNeighborhood("TH1", "fr", "C1", false, 0, 0, 0, 0).found());
        verifyNoInteractions(graphGlobeQueryRepository);
    }

    @Test
    void loadNeighborhood_privateThesaurusOpenToSuperAdmin() {
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(7));
        when(rightsService.can(7, Permission.SUPER_ADMIN)).thenReturn(true);
        center("C1", "Adobe", "D", false);

        assertTrue(service.loadNeighborhood("TH1", "fr", "C1", false, 0, 0, 0, 0).found());
    }

    @Test
    void loadNeighborhood_emptyWhenBlank() {
        assertFalse(service.loadNeighborhood("", "fr", "C1", false, 0, 0, 0, 0).found());
        verifyNoInteractions(graphGlobeQueryRepository);
    }

    @Test
    void loadNeighborhood_listsFacetsOfConcept() {
        when(consultationAccessPolicy.canConsult("TH1", false)).thenReturn(true);
        center("C1", "Adobe", "D", false);
        when(graphGlobeQueryRepository.findFacets("TH1", "C1", "fr", 0, 30))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"F1", "par forme", "FACET", "", "FA", 2L},
                        new Object[]{"F2", "par matière", "FACETOF", "", "FA", 2L}
                ));

        var facets = service.loadNeighborhood("TH1", "fr", "C1", false, 0, 0, 0, 0).facets();

        assertEquals(2, facets.total());
        assertEquals("FA", facets.items().get(0).role());
        assertEquals("FACETOF", facets.items().get(1).type());
    }

    @Test
    void loadNeighborhood_facetCenterHasParentAndMembers() {
        when(consultationAccessPolicy.canConsult("TH1", false)).thenReturn(true);
        when(graphGlobeQueryRepository.findFacetCenter("TH1", "F1", "fr"))
                .thenReturn(Optional.of(new Object[]{"F1", "par forme", ""}));
        when(graphGlobeQueryRepository.findFacetNeighborhood(
                "TH1", "F1", "fr", false, false, new NeighborPage(0, 0, 0, 0, 30)))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"C1", "Adobe", "PARENT", "D", "BT", 1L},
                        new Object[]{"C2", "Brique", "MEMBER", "D", "NT", 1L}
                ));

        var response = service.loadNeighborhood("TH1", "fr", "F1", true, 0, 0, 0, 0);

        assertEquals("facet", response.kind());
        assertEquals("", response.status());
        assertEquals("C1", response.broader().items().get(0).id());
        assertEquals("C2", response.narrower().items().get(0).id());
        assertTrue(response.facets().items().isEmpty());
        verify(graphGlobeQueryRepository, never()).findFacets(any(), any(), any(), anyInt(), anyInt());
    }

    private void center(String id, String label, String status, boolean includeCandidates) {
        when(graphGlobeQueryRepository.findCenter("TH1", id, "fr", includeCandidates))
                .thenReturn(Optional.of(new Object[]{id, label, status}));
    }
}
