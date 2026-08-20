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

import java.util.List;
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
        when(conceptSkosRdfExportEngine.exportConcepts(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(List.of(new SKOSResource()));
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
        when(conceptSkosRdfExportEngine.exportConcepts(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(List.of(new SKOSResource()));
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
        when(conceptSkosRdfExportEngine.exportConcepts(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(List.of(new SKOSResource()));
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{4});

        var result = service.exportConcept("TH1", "C1", "jsonld");

        assertEquals("TH1_C1.json", result.filename());
        assertEquals("application/ld+json", result.contentType());
    }

    @Test
    void exportConcepts_exportsEachIdAndReportsProgress() throws Exception {
        var preferences = Preferences.builder()
                .cheminSite("https://example.com/")
                .originalUri("https://example.com/theso/")
                .build();
        when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1")).thenReturn(Optional.of(preferences));
        when(conceptSkosRdfExportEngine.exportConcepts(eq("TH1"), eq(List.of("C1", "C2")), any(), eq(false)))
                .thenAnswer(invocation -> {
                    java.util.function.BiConsumer<Integer, Integer> progress = invocation.getArgument(2);
                    progress.accept(1, 2);
                    progress.accept(2, 2);
                    return List.of(new SKOSResource(), new SKOSResource());
                });
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{7, 8});

        var progress = new java.util.ArrayList<String>();
        var result = service.exportConcepts("TH1", List.of("C1", "C2"), "rdf", (done, total) -> progress.add(done + "/" + total));

        assertEquals("TH1_selection.rdf", result.filename());
        assertEquals("application/rdf+xml", result.contentType());
        assertEquals(List.of("1/2", "2/2"), progress);
        ArgumentCaptor<SKOSXmlDocument> documentCaptor = ArgumentCaptor.forClass(SKOSXmlDocument.class);
        verify(conceptSkosRdfExportEngine).serializeSkos(documentCaptor.capture(), eq(RDFFormat.RDFXML));
        assertEquals(2, documentCaptor.getValue().getConceptList().size());
        verify(conceptSkosRdfExportEngine).exportConcepts(eq("TH1"), eq(List.of("C1", "C2")), any(), eq(false));
    }

    @Test
    void contentType_matchesSkosAndCsvFormats() {
        assertEquals("application/rdf+xml", ConceptSkosExportService.contentType("rdf"));
        assertEquals("application/ld+json", ConceptSkosExportService.contentType("jsonld"));
        assertEquals("application/json", ConceptSkosExportService.contentType("json"));
        assertEquals("text/turtle", ConceptSkosExportService.contentType("turtle"));
        assertEquals("text/csv", ConceptSkosExportService.contentType("csv"));
    }

    @Test
    void buildDocument_attachesThesaurusConceptScheme() {
        var preferences = Preferences.builder()
                .cheminSite("https://example.com/")
                .originalUri("https://example.com/theso/")
                .build();
        var scheme = new SKOSResource();
        when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1")).thenReturn(Optional.of(preferences));
        when(conceptSkosRdfExportEngine.exportConceptScheme("TH1", preferences)).thenReturn(scheme);
        when(conceptSkosRdfExportEngine.exportConcepts(eq("TH1"), eq(List.of("C1")), any(), eq(false)))
                .thenReturn(List.of(new SKOSResource()));

        var document = service.buildDocument("TH1", List.of("C1"), null, false);

        assertEquals(scheme, document.getConceptScheme());
        verify(conceptSkosRdfExportEngine).exportConceptScheme("TH1", preferences);
    }
}
