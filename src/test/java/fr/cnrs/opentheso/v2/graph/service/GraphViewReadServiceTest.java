package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.v2.shared.repository.GraphViewQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.GraphViewListRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphViewReadServiceTest {

    @Mock
    private GraphViewQueryRepository graphViewQueryRepository;

    private GraphViewReadService service;

    @BeforeEach
    void setUp() {
        service = new GraphViewReadService(graphViewQueryRepository);
    }

    @Test
    void loadViewsForUser_mapsRepositoryRows() {
        when(graphViewQueryRepository.findViewsByUserId(5)).thenReturn(List.of(
                new GraphViewListRow(1, "Alpha", "desc", "[]"),
                new GraphViewListRow(2, "Beta", "desc", "[]")
        ));

        var result = service.loadViewsForUser(5);

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).getName());
    }

    @Test
    void loadView_parsesExportsFromJson() {
        when(graphViewQueryRepository.findViewById(3)).thenReturn(Optional.of(
                new GraphViewListRow(3, "Vue", "desc",
                        "[{\"thesaurusId\":\"TH1\",\"conceptId\":null},{\"thesaurusId\":\"TH2\",\"conceptId\":\"C1\"}]")
        ));

        var view = service.loadView(3);

        assertEquals(2, view.getExports().size());
        assertEquals("TH1", view.getExports().get(0).thesaurusId());
        assertEquals("C1", view.getExports().get(1).conceptId());
    }

    @Test
    void loadView_returnsNullWhenNotFound() {
        when(graphViewQueryRepository.findViewById(99)).thenReturn(Optional.empty());

        assertNull(service.loadView(99));
    }

    @Test
    void loadView_acceptsStringId() {
        when(graphViewQueryRepository.findViewById(3)).thenReturn(Optional.of(
                new GraphViewListRow(3, "Vue", "desc", "[]")
        ));

        var view = service.loadView("3");

        assertEquals("Vue", view.getName());
    }

    @Test
    void reloadViewsForUser_returnsCopyOfViews() {
        when(graphViewQueryRepository.findViewsByUserId(5)).thenReturn(List.of(
                new GraphViewListRow(1, "Alpha", "desc", "[]")
        ));

        var result = service.reloadViewsForUser(5);

        assertEquals(1, result.size());
        assertEquals("Alpha", result.get(0).getName());
    }

    @Test
    void requireViewForUser_throwsWhenNotOwned() {
        when(graphViewQueryRepository.isViewOwnedByUser(9, 5)).thenReturn(false);

        assertThrows(fr.cnrs.opentheso.v2.graph.exception.GraphViewNotFoundException.class,
                () -> service.requireViewForUser(9, 5));
    }
}
