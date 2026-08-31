package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.api.dto.TreeStatusForestNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeStatusForestServiceTest {

    @Test
    void assemble_keepsFullPathWithInactiveParents() {
        List<TreeStatusForestNode> forest = TreeStatusForestService.assemble(
                List.of(new TreeStatusForestService.Seed("c", "Fibule", "", "insere", "", "")),
                List.of(
                        new TreeStatusForestService.Edge("c", "b", "Métal", "valide"),
                        new TreeStatusForestService.Edge("b", "a", "Lieux", "valide")
                ),
                Set.of("insere")
        );

        assertEquals(3, forest.size());
        assertEquals("a", forest.get(0).id());
        assertEquals(0, forest.get(0).depth());
        assertTrue(forest.get(0).inactive());
        assertTrue(forest.get(0).hasChildren());
        assertEquals("b", forest.get(1).id());
        assertEquals(1, forest.get(1).depth());
        assertTrue(forest.get(1).inactive());
        assertEquals("c", forest.get(2).id());
        assertEquals(2, forest.get(2).depth());
        assertFalse(forest.get(2).inactive());
        assertEquals("insere", forest.get(2).status());
    }

    @Test
    void assemble_mergesSharedAncestors() {
        List<TreeStatusForestNode> forest = TreeStatusForestService.assemble(
                List.of(
                        new TreeStatusForestService.Seed("c1", "Alpha", "", "candidat", "", ""),
                        new TreeStatusForestService.Seed("c2", "Beta", "", "candidat", "", "")
                ),
                List.of(
                        new TreeStatusForestService.Edge("c1", "root", "Racine", "valide"),
                        new TreeStatusForestService.Edge("c2", "root", "Racine", "valide")
                ),
                Set.of("candidat")
        );

        assertEquals(3, forest.size());
        assertEquals("root", forest.get(0).id());
        assertEquals("Alpha", forest.get(1).label());
        assertEquals("Beta", forest.get(2).label());
        assertEquals(1, forest.get(1).depth());
        assertEquals(1, forest.get(2).depth());
    }
}
