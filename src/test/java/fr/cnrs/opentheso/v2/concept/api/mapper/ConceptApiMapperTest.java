package fr.cnrs.opentheso.v2.concept.api.mapper;

import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void toRelation_mapsFields() {
        var response = ConceptApiMapper.toRelation(new ConceptRelation("C1", "Label", "ark123"));

        assertEquals("C1", response.conceptId());
        assertEquals("Label", response.label());
        assertEquals("ark123", response.arkId());
    }

    @Test
    void toLabel_mapsHiddenAndPreferredIndependently() {
        var response = ConceptApiMapper.toLabel(new ConceptLabel("en", "Value", false, true));

        assertEquals("en", response.lang());
        assertEquals("Value", response.value());
        assertTrue(response.hidden());
        assertFalse(response.preferred());
    }

    @Test
    void toNote_mapsFields() {
        var response = ConceptApiMapper.toNote(new ConceptNote("note1", "definition", "fr", "Une note"));

        assertEquals("note1", response.id());
        assertEquals("definition", response.typeCode());
        assertEquals("fr", response.lang());
        assertEquals("Une note", response.value());
    }

    @Test
    void toDetail_mapsAllSubCollections() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        var breadcrumb = List.of(new BreadcrumbStep("C0", "Root", 1));
        var broader = List.of(new ConceptRelation("B1", "Broader", "arkB"));
        var narrower = List.of(new ConceptRelation("N1", "Narrower", "arkN"));
        var related = List.of(new ConceptRelation("R1", "Related", "arkR"));
        var translations = List.of(new ConceptLabel("en", "Value", true, false));
        var notes = List.of(new ConceptNote("note1", "definition", "fr", "Une note"));
        var collections = List.of(new ConceptRelation("Col1", "Collection", "arkCol"));
        var facets = List.of(new ConceptRelation("Fac1", "Facet", "arkFac"));
        var replacedBy = List.of(new ConceptRelation("Rep1", "ReplacedBy", "arkRep"));
        var replaces = List.of(new ConceptRelation("Rep2", "Replaces", "arkRep2"));

        var detail = new ConceptDetail(
                summary, breadcrumb, broader, narrower, related,
                List.of("syn1"), List.of("hidden1"), translations, notes,
                List.of(), collections, facets, replacedBy, replaces);

        var response = ConceptApiMapper.toDetail(detail);

        assertEquals("C1", response.summary().conceptId());
        assertEquals("B1", response.broaderTerms().get(0).conceptId());
        assertEquals("N1", response.narrowerTerms().get(0).conceptId());
        assertEquals("R1", response.relatedTerms().get(0).conceptId());
        assertEquals(List.of("syn1"), response.synonyms());
        assertEquals(List.of("hidden1"), response.hiddenSynonyms());
        assertEquals(1, response.translations().size());
        assertEquals(1, response.notes().size());
        assertEquals("Col1", response.collections().get(0).conceptId());
        assertEquals("Fac1", response.facets().get(0).conceptId());
        assertEquals("Rep1", response.replacedBy().get(0).conceptId());
        assertEquals("Rep2", response.replaces().get(0).conceptId());
        assertEquals("", response.preferredTermId());
    }
}
