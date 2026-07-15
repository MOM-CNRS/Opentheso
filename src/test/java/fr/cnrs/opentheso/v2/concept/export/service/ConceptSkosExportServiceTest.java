package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSkosExportServiceTest {

    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;

    private ConceptSkosExportService service;

    @BeforeEach
    void setUp() {
        service = new ConceptSkosExportService(conceptSkosRdfExportEngine);
    }

    @Test
    void exportConcept_buildsSkosDocumentWithReadService() throws Exception {
        var preferences = Preferences.builder()
                .cheminSite("https://example.com/")
                .originalUri("https://example.com/theso/")
                .build();
        when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1")).thenReturn(Optional.of(preferences));
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C1")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{1, 2, 3});

        var result = service.exportConcept("TH1", "C1", "skos");

        verify(conceptSkosRdfExportEngine).prepareExport(preferences);
        ArgumentCaptor<SKOSXmlDocument> documentCaptor = ArgumentCaptor.forClass(SKOSXmlDocument.class);
        verify(conceptSkosRdfExportEngine).serializeSkos(documentCaptor.capture(), eq(RDFFormat.RDFXML));
        assertEquals(1, documentCaptor.getValue().getConceptList().size());
        assertEquals("TH1_C1.rdf", result.filename());
        assertArrayEquals(new byte[]{1, 2, 3}, result.content());
    }

    @Test
    void exportConcept_rejectsMissingPreferences() {
        when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.exportConcept("TH1", "C1", "skos"));
    }

    @Test
    void exportConcept_rejectsMissingConceptId() {
        assertThrows(IllegalStateException.class, () -> service.exportConcept("TH1", " ", "skos"));
    }

    @Test
    void exportConcept_supportsTurtleFormat() throws Exception {
        var preferences = Preferences.builder()
                .cheminSite("https://example.com/")
                .originalUri("https://example.com/theso/")
                .build();
        when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1")).thenReturn(Optional.of(preferences));
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C1")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{9});

        var result = service.exportConcept("TH1", "C1", "turtle");

        assertEquals("TH1_C1.ttl", result.filename());
        verify(conceptSkosRdfExportEngine).serializeSkos(any(SKOSXmlDocument.class), eq(RDFFormat.TURTLE));
    }

    @Test
    void exportConcept_rejectsMissingOriginalUri() {
        when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1"))
                .thenReturn(Optional.of(Preferences.builder().cheminSite("https://example.com/").build()));

        assertThrows(IllegalStateException.class, () -> service.exportConcept("TH1", "C1", "skos"));
    }

    @Test
    void exportConcept_supportsJsonLdFormat() throws Exception {
        var preferences = Preferences.builder()
                .cheminSite("https://example.com/")
                .originalUri("https://example.com/theso/")
                .build();
        when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1")).thenReturn(Optional.of(preferences));
        when(conceptSkosRdfExportEngine.exportConcept("TH1", "C1")).thenReturn(new SKOSResource());
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{4});

        var result = service.exportConcept("TH1", "C1", "jsonld");

        assertEquals("TH1_C1.json", result.filename());
    }
}
