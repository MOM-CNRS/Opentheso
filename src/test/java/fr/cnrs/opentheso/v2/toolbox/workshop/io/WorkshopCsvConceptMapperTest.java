package fr.cnrs.opentheso.v2.toolbox.workshop.io;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WorkshopCsvConceptMapper}, the small static helper that copies a
 * {@link WorkshopCsvReader.ConceptObject} (produced by {@link WorkshopCsvReader}) into the
 * shared edition model {@link ThesaurusCsvConceptObject}.
 */
class WorkshopCsvConceptMapperTest {

    // ConceptObject is a non-static inner class of WorkshopCsvReader, so it needs an outer instance.
    private final WorkshopCsvReader reader = new WorkshopCsvReader(',');

    @Test
    void toEditionModel_returnsNull_whenSourceIsNull() {
        assertNull(WorkshopCsvConceptMapper.toEditionModel(null));
    }

    @Test
    void toEditionModel_copiesAllScalarAndListFields() {
        WorkshopCsvReader.ConceptObject source = reader.new ConceptObject();
        source.setIdConcept("C1");
        source.setUri("http://example.com/c1");
        source.setLocalId("local1");
        source.setArkId("ark1");
        source.setIdTerm("term1");
        source.setType("skos:concept");
        source.setConceptType("people");
        source.setDeprecated(true);
        source.setNotation("N1");
        source.setLatitude("48.85");
        source.setLongitude("2.35");
        source.setGps("48.85,2.35");
        source.setSuperOrdinate("facetParent");
        source.setCreated("2024-01-01");
        source.setModified("2024-02-02");

        WorkshopCsvReader.Label prefLabel = reader.new Label();
        prefLabel.setLabel("Chat");
        prefLabel.setLang("fr");
        source.getPrefLabels().add(prefLabel);

        WorkshopCsvReader.Label altLabel = reader.new Label();
        altLabel.setLabel("Minou");
        altLabel.setLang("fr");
        source.getAltLabels().add(altLabel);

        WorkshopCsvReader.Label hiddenLabel = reader.new Label();
        hiddenLabel.setLabel("Chah");
        hiddenLabel.setLang("fr");
        source.getHiddenLabels().add(hiddenLabel);

        WorkshopCsvReader.Label note = reader.new Label();
        note.setLabel("une note");
        note.setLang("fr");
        source.getNote().add(note);

        WorkshopCsvReader.Label definition = reader.new Label();
        definition.setLabel("une definition");
        definition.setLang("fr");
        source.getDefinitions().add(definition);

        WorkshopCsvReader.Label scopeNote = reader.new Label();
        scopeNote.setLabel("scope");
        scopeNote.setLang("fr");
        source.getScopeNotes().add(scopeNote);

        WorkshopCsvReader.Label example = reader.new Label();
        example.setLabel("exemple");
        example.setLang("fr");
        source.getExamples().add(example);

        WorkshopCsvReader.Label historyNote = reader.new Label();
        historyNote.setLabel("history");
        historyNote.setLang("fr");
        source.getHistoryNotes().add(historyNote);

        WorkshopCsvReader.Label changeNote = reader.new Label();
        changeNote.setLabel("change");
        changeNote.setLang("fr");
        source.getChangeNotes().add(changeNote);

        WorkshopCsvReader.Label editorialNote = reader.new Label();
        editorialNote.setLabel("editorial");
        editorialNote.setLang("fr");
        source.getEditorialNotes().add(editorialNote);

        source.getBroaders().add("broader1");
        source.getNarrowers().add("narrower1");
        source.getRelateds().add("related1");
        source.getCustomRelations().add(new NodeIdValue("customId", "customType"));

        source.getExactMatchs().add("http://example.com/exact");
        source.getCloseMatchs().add("http://example.com/close");
        source.getBroadMatchs().add("http://example.com/broad");
        source.getNarrowMatchs().add("http://example.com/narrow");
        source.getRelatedMatchs().add("http://example.com/related");

        source.getMembers().add("member1");
        source.getSubGroups().add("subgroup1");
        source.getReplacedBy().add("replacement1");

        NodeImage image = new NodeImage();
        image.setUri("http://example.com/photo.jpg");
        source.getImages().add(image);

        source.getExternalResources().add("http://example.com/resource");
        source.getMemberOfFacets().add("facet1");

        source.getAlignments().add(new NodeIdValue("", "http://example.com/toDelete"));

        ThesaurusCsvConceptObject target = WorkshopCsvConceptMapper.toEditionModel(source);

        assertNotNull(target);
        assertEquals("C1", target.getIdConcept());
        assertEquals("http://example.com/c1", target.getUri());
        assertEquals("local1", target.getLocalId());
        assertEquals("ark1", target.getArkId());
        assertEquals("term1", target.getIdTerm());
        assertEquals("skos:concept", target.getType());
        assertEquals("people", target.getConceptType());
        assertTrue(target.isDeprecated());
        assertEquals("N1", target.getNotation());
        assertEquals("48.85", target.getLatitude());
        assertEquals("2.35", target.getLongitude());
        assertEquals("48.85,2.35", target.getGps());
        assertEquals("facetParent", target.getSuperOrdinate());
        assertEquals("2024-01-01", target.getCreated());
        assertEquals("2024-02-02", target.getModified());

        assertEquals(1, target.getPrefLabels().size());
        assertEquals("Chat", target.getPrefLabels().get(0).getLabel());
        assertEquals("fr", target.getPrefLabels().get(0).getLang());

        assertEquals(1, target.getAltLabels().size());
        assertEquals("Minou", target.getAltLabels().get(0).getLabel());

        assertEquals(1, target.getHiddenLabels().size());
        assertEquals("Chah", target.getHiddenLabels().get(0).getLabel());

        assertEquals(1, target.getNote().size());
        assertEquals("une note", target.getNote().get(0).getLabel());

        assertEquals(1, target.getDefinitions().size());
        assertEquals(1, target.getScopeNotes().size());
        assertEquals(1, target.getExamples().size());
        assertEquals(1, target.getHistoryNotes().size());
        assertEquals(1, target.getChangeNotes().size());
        assertEquals(1, target.getEditorialNotes().size());

        assertEquals(source.getBroaders(), target.getBroaders());
        assertEquals(source.getNarrowers(), target.getNarrowers());
        assertEquals(source.getRelateds(), target.getRelateds());
        assertEquals(1, target.getCustomRelations().size());
        assertEquals("customId", target.getCustomRelations().get(0).getId());

        assertEquals(source.getExactMatchs(), target.getExactMatchs());
        assertEquals(source.getCloseMatchs(), target.getCloseMatchs());
        assertEquals(source.getBroadMatchs(), target.getBroadMatchs());
        assertEquals(source.getNarrowMatchs(), target.getNarrowMatchs());
        assertEquals(source.getRelatedMatchs(), target.getRelatedMatchs());

        assertEquals(source.getMembers(), target.getMembers());
        assertEquals(source.getSubGroups(), target.getSubGroups());
        assertEquals(source.getReplacedBy(), target.getReplacedBy());

        assertEquals(1, target.getImages().size());
        assertEquals("http://example.com/photo.jpg", target.getImages().get(0).getUri());

        assertEquals(source.getExternalResources(), target.getExternalResources());
        assertEquals(source.getMemberOfFacets(), target.getMemberOfFacets());

        assertEquals(1, target.getAlignments().size());
        assertEquals("http://example.com/toDelete", target.getAlignments().get(0).getValue());
    }

    @Test
    void toEditionModel_copiesAreIndependent_fromSourceLists() {
        WorkshopCsvReader.ConceptObject source = reader.new ConceptObject();
        source.setIdConcept("C1");
        source.getBroaders().add("broader1");

        ThesaurusCsvConceptObject target = WorkshopCsvConceptMapper.toEditionModel(source);

        // mutating the source afterwards must not affect the already-mapped target (defensive copy)
        source.getBroaders().add("broader2");

        assertEquals(1, target.getBroaders().size());
        assertEquals("broader1", target.getBroaders().get(0));
    }

    @Test
    void toEditionModel_handlesNullLists_byProducingEmptyLists() {
        WorkshopCsvReader.ConceptObject source = reader.new ConceptObject();
        source.setIdConcept("C1");

        // force some list fields to null, as could happen if the reader never populated them
        source.setImages(null);
        source.setAlignments(null);
        source.setCustomRelations(null);
        source.setBroaders(null);

        ThesaurusCsvConceptObject target = WorkshopCsvConceptMapper.toEditionModel(source);

        assertNotNull(target.getImages());
        assertTrue(target.getImages().isEmpty());
        assertNotNull(target.getAlignments());
        assertTrue(target.getAlignments().isEmpty());
        assertNotNull(target.getCustomRelations());
        assertTrue(target.getCustomRelations().isEmpty());
        assertNotNull(target.getBroaders());
        assertTrue(target.getBroaders().isEmpty());
    }
}
