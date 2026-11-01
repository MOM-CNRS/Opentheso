package fr.cnrs.opentheso.concept.io.rdf.parser;

import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptCompletSkosParseTest {

    @Test
    void conceptCompletKeepsNotesMetadataAndReplaces() throws Exception {
        SKOSXmlDocument document;
        try (InputStream in = getClass().getResourceAsStream("/samples/theso/test_complet_avecArk.rdf")) {
            assertNotNull(in);
            document = new ReadRdf4jDocument().readRdfFlux(in, RDFFormat.RDFXML, "fr", new StringBuilder());
        }

        SKOSResource lyon = concept(document, "1027237");
        assertEquals(2, lyon.getDocumentationsList().size());
        assertTrue(lyon.getDocumentationsList().stream().anyMatch(note ->
                note.getProperty() == SKOSProperty.DEFINITION && "fr".equals(note.getLanguage())));
        assertTrue(dcValues(lyon, "source").contains("https://www.lyon.fr"));
        assertTrue(dcValues(lyon, "description").contains("1027236##1027237"));
        assertTrue(lyon.getsKOSReplaces().stream().anyMatch(replace ->
                replace.getProperty() == SKOSProperty.REPLACES));

        SKOSResource lugdunum = concept(document, "1027238");
        assertEquals(SKOSProperty.DEPRECATED, lugdunum.getStatus());
        assertTrue(lugdunum.getsKOSReplaces().stream().anyMatch(replace ->
                replace.getProperty() == SKOSProperty.IS_REPLACED_BY
                        && replace.getTargetUri().contains("the4lpjkv0cg0dtplxvp9gw-2")));

        assertFalse(document.getFacetList().isEmpty());
        assertEquals("1027237", document.getFoafImage().get(0).getIdentifier());
        assertEquals("Wikidata_rest", document.getFoafImage().get(0).getFoafImage().getCopyRight());
    }

    private static SKOSResource concept(SKOSXmlDocument document, String identifier) {
        return document.getConceptList().stream()
                .filter(concept -> identifier.equals(concept.getIdentifier()))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> dcValues(SKOSResource concept, String name) {
        return concept.getThesaurus().getDcElement().stream()
                .filter(element -> name.equals(element.getName()))
                .map(DcElement::getValue)
                .toList();
    }
}
