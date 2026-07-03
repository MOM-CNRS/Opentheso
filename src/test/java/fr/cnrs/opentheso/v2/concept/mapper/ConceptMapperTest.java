package fr.cnrs.opentheso.v2.concept.mapper;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptHeaderRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptResourceStatus;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptUriLabel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptMapperTest {

    @Test
    void toConceptNode_mapsConceptWithChildren() {
        var node = ConceptMapper.toConceptNode(new ConceptTreeRow("C3", "", "Branch", "C", true));

        assertEquals("concept", node.nodeType());
        assertTrue(node.hasChildren());
    }

    @Test
    void toConceptNode_mapsDeprecatedStatus() {
        var node = ConceptMapper.toConceptNode(new ConceptTreeRow("C1", "N1", "Label", "DEP", false));

        assertEquals("deprecated", node.nodeType());
        assertEquals("C1", node.nodeId());
    }

    @Test
    void toConceptNode_mapsActiveDescriptorStatusAsFile() {
        var node = ConceptMapper.toConceptNode(new ConceptTreeRow("C1", "N1", "Label", "D", false));

        assertEquals("file", node.nodeType());
        assertEquals("C1", node.nodeId());
    }

    @Test
    void toConceptNode_mapsDefaultConceptStatusAsFile() {
        var node = ConceptMapper.toConceptNode(new ConceptTreeRow("C1", "N1", "Label", "V", false));

        assertEquals("file", node.nodeType());
    }

    @Test
    void toConceptNode_mapsLeafConceptAsFile() {
        var node = ConceptMapper.toConceptNode(new ConceptTreeRow("C2", "", "Leaf", "C", false));

        assertEquals("file", node.nodeType());
        assertTrue(!node.hasChildren());
    }

    @Test
    void toSummary_mapsHeaderRow() {
        var summary = ConceptMapper.toSummary(new ConceptHeaderRow(
                "C1", "TH1", "Preferred", "fr", "C", "ark:/123", "concept", "N1",
                "01/01/2024", "02/01/2024", "admin"
        ));

        assertEquals("C1", summary.conceptId());
        assertEquals("Preferred", summary.preferredLabel());
        assertEquals("ark:/123", summary.arkId());
        assertEquals("N1", summary.notation());
        assertEquals("admin", summary.creatorName());
    }

    @Test
    void toDetailFromFullConcept_mapsRelationsLabelsAndNotes() {
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        fullConcept.setResourceStatus(ConceptResourceStatus.CONCEPT);
        fullConcept.setConceptType("concept");
        fullConcept.setPrefLabel(new ConceptTermLabel("fr", "Preferred", "T1", 1));

        fullConcept.setBroaders(List.of(
                new ConceptHierarchicalRelation("", "BT1", "Broader", "BT")
        ));
        fullConcept.setNarrowers(List.of(
                new ConceptHierarchicalRelation("", "NT1", "Narrower", "NT")
        ));
        fullConcept.setRelateds(List.of(
                new ConceptHierarchicalRelation("", "RT1", "Related", "RT")
        ));

        fullConcept.setAltLabels(List.of(new ConceptTermLabel("fr", "synonyme", "T2", 2)));
        fullConcept.setHiddenLabels(List.of(new ConceptTermLabel("fr", "hidden syn", "T3", 3)));
        fullConcept.setPrefLabelsTraduction(List.of(
                new ConceptTermLabel(4, "T4", "en", "English label", "")
        ));
        fullConcept.setDefinitions(List.of(new ConceptSnapshotNote(1, "fr", "Une définition", "")));

        fullConcept.setMembres(List.of(new ConceptUriLabel("", "G1", "Collection")));
        fullConcept.setFacets(List.of(new ConceptUriLabel("F1", "F1", "Facette")));
        fullConcept.setExactMatchs(List.of(new ConceptUriLabel("http://example.org", "A1", "")));

        var detail = ConceptMapper.toDetailFromFullConcept(fullConcept, "TH1", "fr", List.of());

        assertEquals(1, detail.broaderTerms().size());
        assertEquals("BT1", detail.broaderTerms().get(0).conceptId());
        assertEquals(1, detail.narrowerTerms().size());
        assertEquals(1, detail.relatedTerms().size());
        assertEquals(List.of("synonyme"), detail.synonyms());
        assertEquals(List.of("hidden syn"), detail.hiddenSynonyms());
        assertEquals(1, detail.translations().size());
        assertEquals("en", detail.translations().get(0).lang());
        assertEquals(1, detail.notes().size());
        assertEquals(1, detail.alignmentGroups().size());
        assertEquals(1, detail.collections().size());
        assertEquals(1, detail.facets().size());
        assertTrue(detail.hasNotesOfType("definition"));
    }
}
