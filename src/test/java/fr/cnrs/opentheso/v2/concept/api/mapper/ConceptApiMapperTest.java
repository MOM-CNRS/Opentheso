package fr.cnrs.opentheso.v2.concept.api.mapper;

import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptApiMapperTest {

    @Test
    void toTreeNode_mapsAllFields() {
        var node = new ConceptTreeNodeData("C1", "Label", "N1", "concept", true);

        var response = ConceptApiMapper.toTreeNode(node);

        assertEquals("C1", response.nodeId());
        assertEquals("Label", response.label());
        assertEquals("N1", response.notation());
        assertEquals("concept", response.nodeType());
        assertTrue(response.hasChildren());
    }

    @Test
    void toSummary_mapsConceptHeader() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");

        var response = ConceptApiMapper.toSummary(summary);

        assertEquals("C1", response.conceptId());
        assertEquals("Label", response.preferredLabel());
        assertEquals("2025", response.modified());
    }

    @Test
    void toBreadcrumb_mapsDepthAsString() {
        var response = ConceptApiMapper.toBreadcrumb(new BreadcrumbStep("C1", "Root", 2));

        assertEquals("C1", response.conceptId());
        assertEquals("Root", response.label());
        assertEquals("2", response.depth());
    }
}
