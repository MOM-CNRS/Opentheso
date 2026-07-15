package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptBreadcrumbReadServiceTest {

    @Mock
    private ConceptQueryRepository conceptQueryRepository;

    private ConceptBreadcrumbReadService service;

    @BeforeEach
    void setUp() {
        service = new ConceptBreadcrumbReadService(conceptQueryRepository);
    }

    @Test
    void loadBreadcrumbPaths_returnsEmptyWhenInputBlank() {
        assertTrue(service.loadBreadcrumbPaths("", "C1", "fr").isEmpty());
        assertTrue(service.loadBreadcrumbPaths("TH1", " ", "fr").isEmpty());
    }

    @Test
    void loadBreadcrumbPaths_labelsSingleTopConceptPath() {
        // No ancestors — concept is already a root
        when(conceptQueryRepository.findAncestorsWithLabels("C1", "TH1", "fr"))
                .thenReturn(List.of());
        when(conceptQueryRepository.findConceptLabel("C1", "TH1", "fr"))
                .thenReturn("Racine");

        List<List<BreadcrumbStep>> paths = service.loadBreadcrumbPaths("TH1", "C1", "fr");

        assertEquals(1, paths.size());
        assertEquals(1, paths.get(0).size());
        assertEquals("Racine", paths.get(0).get(0).label());
        assertTrue(paths.get(0).get(0).startOfPath());
    }

    @Test
    void loadBreadcrumbPaths_buildsPathThroughBroader() {
        // CTE returns edge: C2 → C1 (depth=1), C1 has no parent so it's the root
        // Object[]: [child_id, parent_id, depth, parent_label]
        List<Object[]> ancestors = new ArrayList<>();
        ancestors.add(new Object[]{"C2", "C1", 1, "Parent"});
        when(conceptQueryRepository.findAncestorsWithLabels("C2", "TH1", "fr"))
                .thenReturn(ancestors);
        when(conceptQueryRepository.findConceptLabel("C2", "TH1", "fr"))
                .thenReturn("Enfant");

        List<List<BreadcrumbStep>> paths = service.loadBreadcrumbPaths("TH1", "C2", "fr");

        assertEquals(1, paths.size());
        assertEquals(2, paths.get(0).size());
        assertEquals("Parent", paths.get(0).get(0).label());
        assertTrue(paths.get(0).get(0).startOfPath());
        assertEquals("Enfant", paths.get(0).get(1).label());
    }

    @Test
    void loadBreadcrumbPaths_polyhierarchy_returnsMultiplePaths() {
        // C3 has two parents: C1 and C2 (polyhierarchy)
        // Object[]: [child_id, parent_id, depth, parent_label]
        List<Object[]> ancestors = new ArrayList<>();
        ancestors.add(new Object[]{"C3", "C1", 1, "Parent1"});
        ancestors.add(new Object[]{"C3", "C2", 1, "Parent2"});
        when(conceptQueryRepository.findAncestorsWithLabels("C3", "TH1", "fr"))
                .thenReturn(ancestors);
        when(conceptQueryRepository.findConceptLabel("C3", "TH1", "fr"))
                .thenReturn("Enfant");

        List<List<BreadcrumbStep>> paths = service.loadBreadcrumbPaths("TH1", "C3", "fr");

        assertEquals(2, paths.size());
        // Each path has 2 steps: root → C3
        assertTrue(paths.stream().allMatch(p -> p.size() == 2));
        assertTrue(paths.stream().anyMatch(p -> "Parent1".equals(p.get(0).label())));
        assertTrue(paths.stream().anyMatch(p -> "Parent2".equals(p.get(0).label())));
    }
}
