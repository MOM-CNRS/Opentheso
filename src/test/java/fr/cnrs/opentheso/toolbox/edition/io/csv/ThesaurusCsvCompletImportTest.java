package fr.cnrs.opentheso.toolbox.edition.io.csv;

import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.toolbox.edition.model.ThesaurusCsvConceptObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusCsvCompletImportTest {

    @Test
    void exempleCompletKeepsDefinitionsImageSourceAndReplacement() throws Exception {
        List<ThesaurusCsvConceptObject> concepts = read(
                Files.readAllBytes(Path.of("src/main/resources/samples/theso/exemple_complet_facette_image.csv")),
                ';');

        ThesaurusCsvConceptObject lait = concept(concepts, "1027240");
        assertEquals(2, lait.getDefinitions().size());
        assertTrue(lait.getDefinitions().stream().anyMatch(label ->
                "fr".equals(label.getLang()) && label.getLabel().contains("mammifères")));

        assertEquals(1, lait.getImages().size());
        NodeImage image = lait.getImages().get(0);
        assertEquals("Lait", image.getImageName());
        assertEquals("", image.getCopyRight());
        assertTrue(image.getUri().contains("Glass"));

        assertTrue(lait.getDcElements().stream().anyMatch(element ->
                "source".equals(element.getName()) && "http://www.lait.fr".equals(element.getValue())));
        assertTrue(lait.getExternalResources().contains("http://www.lait.fr"));

        ThesaurusCsvConceptObject deprecated = concept(concepts, "38486");
        assertTrue(deprecated.isDeprecated());
        assertEquals(List.of("1027240"), deprecated.getReplacedBy());

        ThesaurusCsvConceptObject facet = concept(concepts, "F144");
        assertEquals("1027239", facet.getSuperOrdinate());
        assertEquals(List.of("1027240"), facet.getMembers());
    }

    @Test
    void queryIdAfterAmpersandResolvesReplacementFacetNoteAndMetadata() throws Exception {
        String csv = """
                URI;rdf:type;identifier;skos:prefLabel@fr;skos:definition@fr;skos:scopeNote@fr;iso-thes:superOrdinate;owl:deprecated;dcterms:isReplacedBy;dcterms:replaces;dcterms:creator@fr;foaf:Image
                http://h/?idt=th1&idc=OLD;skos:Concept;OLD;ancien;texte @@ secret;;;false;http://h/?idt=th1&idc=NEW;;Jean Dupont;rdf:about=http://img.example/a.jpg@@dcterms:rights=Wikidata_sparql@@dcterms:title=amphore · Wikidata_sparql · null@@dcterms:creator=null
                http://h/?idt=th1&idc=NEW;skos:Concept;NEW;nouveau;;;;false;;http://h/?idt=th1&idc=OLD;;
                http://h/?idf=F1&idt=th1;skos-thes:ThesaurusArray;F1;Facette;;note de facette @@ ici;http://h/?idt=th1&idc=NEW;false;;;;
                """;

        List<ThesaurusCsvConceptObject> concepts = read(csv.getBytes(StandardCharsets.UTF_8), ';');

        ThesaurusCsvConceptObject oldConcept = concept(concepts, "OLD");
        assertTrue(oldConcept.isDeprecated());
        assertEquals(List.of("NEW"), oldConcept.getReplacedBy());
        assertEquals("amphore", oldConcept.getImages().get(0).getImageName());
        assertEquals("", oldConcept.getImages().get(0).getCopyRight());
        assertEquals("", oldConcept.getImages().get(0).getCreator());
        assertTrue(oldConcept.getDcElements().stream().anyMatch(element ->
                "creator".equals(element.getName())
                        && "Jean Dupont".equals(element.getValue())
                        && "fr".equals(element.getLanguage())));

        String notes = ThesaurusCsvImportEngine.notesPayload(oldConcept);
        assertTrue(notes.contains("texte @ @ secret@@definition@@fr@@OLD"));
        assertFalse(notes.contains("texte @@ secret"));

        ThesaurusCsvConceptObject replacement = concept(concepts, "NEW");
        assertEquals(List.of("OLD"), replacement.getReplaces());
        assertFalse(replacement.isDeprecated());

        ThesaurusCsvConceptObject facet = concept(concepts, "F1");
        assertEquals("NEW", facet.getSuperOrdinate());
        assertEquals("note de facette @@ ici", facet.getScopeNotes().get(0).getLabel());
        String facetNotes = ThesaurusCsvImportEngine.notesPayload(facet);
        assertTrue(facetNotes.contains("note de facette @ @ ici@@scopeNote@@fr@@F1"));
    }

    @Test
    void notePayloadDropsEmptyTextAndEscapesSeparators() {
        ThesaurusCsvConceptObject concept = new ThesaurusCsvConceptObject();
        concept.setIdConcept("c1");
        ThesaurusCsvConceptLabel pref = new ThesaurusCsvConceptLabel();
        pref.setLabel("Lait");
        pref.setLang("fr");
        concept.getPrefLabels().add(pref);
        ThesaurusCsvConceptLabel definition = new ThesaurusCsvConceptLabel();
        definition.setLabel("a##b@@c");
        definition.setLang("null");
        concept.getDefinitions().add(definition);
        ThesaurusCsvConceptLabel blank = new ThesaurusCsvConceptLabel();
        blank.setLabel("  ");
        blank.setLang("fr");
        concept.getNote().add(blank);

        assertEquals("a# #b@ @c@@definition@@fr@@c1", ThesaurusCsvImportEngine.notesPayload(concept));

        concept.getDcElements().add(new DcElement("description", "x##y", "en", ""));
        assertEquals("description@@x# #y@@en", ThesaurusCsvImportEngine.dcTermsPayload(concept));
        assertEquals("amphore", ThesaurusCsvImportEngine.imageCaptionPart("amphore · Wikidata_sparql · null"));
    }

    private static List<ThesaurusCsvConceptObject> read(byte[] content, char delimiter) throws Exception {
        ThesaurusCsvReader reader = new ThesaurusCsvReader(delimiter);
        assertTrue(reader.setLangs(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)));
        assertTrue(reader.readFile(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8), false));
        return reader.getConceptObjects();
    }

    private static ThesaurusCsvConceptObject concept(List<ThesaurusCsvConceptObject> concepts, String id) {
        return concepts.stream()
                .filter(concept -> id.equals(concept.getIdConcept()))
                .findFirst()
                .orElseThrow();
    }
}
