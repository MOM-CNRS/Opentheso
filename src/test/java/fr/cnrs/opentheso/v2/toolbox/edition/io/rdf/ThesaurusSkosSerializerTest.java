package fr.cnrs.opentheso.v2.toolbox.edition.io.rdf;

import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusSkosSerializerTest {

    @Test
    void serializer_producesNonEmptyRdfXmlModel() throws Exception {
        var document = new SKOSXmlDocument();
        var scheme = new SKOSResource("https://example.com/?idt=TH1", SKOSProperty.CONCEPT_SCHEME);
        scheme.getLabelsList().add(new SKOSLabel("Animaux", "fr", SKOSProperty.PREF_LABEL));
        document.setConceptScheme(scheme);

        var concept = new SKOSResource();
        concept.setUri("https://example.com/?idc=C1&idt=TH1");
        concept.setProperty(SKOSProperty.CONCEPT);
        concept.getLabelsList().add(new SKOSLabel("Chat", "fr", SKOSProperty.PREF_LABEL));
        document.addconcept(concept);

        ThesaurusSkosSerializer serializer = new ThesaurusSkosSerializer(document);
        Model model = serializer.getModel();

        assertFalse(model.isEmpty());

        try (var out = new ByteArrayOutputStream()) {
            Rio.write(model, out, RDFFormat.RDFXML);
            assertTrue(out.size() > 0);
        } finally {
            serializer.closeCache();
        }
    }
}
