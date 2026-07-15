package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.StreamedContent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionSkosExportServiceTest {

    @Mock
    private ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;

    private ThesaurusEditionSkosExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionSkosExportService(thesaurusSkosDocumentBuilder);
    }

    @Test
    void exportThesaurus_rejectsBlankThesaurusId() {
        assertThrows(Exception.class, () -> service.exportThesaurus(" ", "Titre", "rdf"));
    }

    @Test
    void exportThesaurus_serializesDocumentInRdfXml() throws Exception {
        var document = sampleDocument();
        when(thesaurusSkosDocumentBuilder.buildDocument(eq("TH1"), any())).thenReturn(document);

        StreamedContent content = service.exportThesaurus("TH1", "Animaux", "rdf");

        assertNotNull(content);
        assertEquals("Animaux_TH1.rdf", content.getName());
        assertNotNull(content.getStream().get());
    }

    @Test
    void exportThesaurus_supportsTurtleFormat() throws Exception {
        var document = sampleDocument();
        when(thesaurusSkosDocumentBuilder.buildDocument(eq("TH1"), any())).thenReturn(document);

        StreamedContent content = service.exportThesaurus("TH1", "Animaux", "turtle");

        assertEquals("Animaux_TH1.ttl", content.getName());
    }

    private SKOSXmlDocument sampleDocument() throws Exception {
        var document = new SKOSXmlDocument();
        var scheme = new SKOSResource("https://example.com/?idt=TH1", SKOSProperty.CONCEPT_SCHEME);
        scheme.getLabelsList().add(new SKOSLabel("Animaux", "fr", SKOSProperty.PREF_LABEL));
        document.setConceptScheme(scheme);

        var concept = new SKOSResource();
        concept.setUri("https://example.com/?idc=C1&idt=TH1");
        concept.setProperty(SKOSProperty.CONCEPT);
        concept.getLabelsList().add(new SKOSLabel("Chat", "fr", SKOSProperty.PREF_LABEL));
        document.addconcept(concept);
        return document;
    }
}
