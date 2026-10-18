package fr.cnrs.opentheso.concept.io.rdf.parser;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadRdf4jDocumentTest {

    private static final String WIKI_FILE = "[[File:Atapuerca%20map.jpg|Atapuerca_map]]";

    @Test
    void rdfXml_readsConceptWhenAnIriIsWikiMarkup() throws IOException {
        String rdf = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:skos="http://www.w3.org/2004/02/skos/core#">
                  <skos:Concept rdf:about="http://example.org/c1">
                    <skos:prefLabel xml:lang="fr">Atapuerca</skos:prefLabel>
                    <skos:note>%s</skos:note>
                    <skos:exactMatch rdf:resource="%s"/>
                    <skos:exactMatch rdf:resource="http://example.org/match"/>
                  </skos:Concept>
                </rdf:RDF>
                """.formatted(WIKI_FILE, WIKI_FILE);

        var concept = read(rdf, RDFFormat.RDFXML).getConceptList().get(0);

        assertEquals("Atapuerca", concept.getLabelsList().get(0).getLabel());
        assertTrue(concept.getDocumentationsList().stream().anyMatch(note -> WIKI_FILE.equals(note.getText())));
        assertEquals(1, concept.getMatchList().size());
        assertEquals("http://example.org/match", concept.getMatchList().get(0).getValue());
    }

    @Test
    void turtle_readsConceptWhenAnIriIsWikiMarkup() throws IOException {
        String turtle = """
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                <http://example.org/c1> a skos:Concept ;
                  skos:prefLabel "Atapuerca"@fr ;
                  skos:exactMatch <%s> ;
                  skos:exactMatch <http://example.org/match> .
                """.formatted(WIKI_FILE);

        var concept = read(turtle, RDFFormat.TURTLE).getConceptList().get(0);

        assertEquals("Atapuerca", concept.getLabelsList().get(0).getLabel());
        assertFalse(concept.getMatchList().isEmpty());
        assertTrue(concept.getMatchList().stream().anyMatch(match -> "http://example.org/match".equals(match.getValue())));
        assertTrue(concept.getMatchList().stream().noneMatch(match -> WIKI_FILE.equals(match.getValue())));
    }

    private static SKOSXmlDocument read(String content, RDFFormat format) throws IOException {
        return new ReadRdf4jDocument().readRdfFlux(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                format,
                "fr",
                new StringBuilder());
    }
}
