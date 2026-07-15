package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.v2.concept.export.rdf.ConceptSkosExportPersistence;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatExportServiceTest {

    @Mock
    private ConceptSkosExportPersistence conceptSkosExportPersistence;

    private CandidatExportService service;

    @BeforeEach
    void setUp() {
        service = new CandidatExportService(conceptSkosExportPersistence);
    }

    @Test
    void exportPendingCandidates_throwsWhenListEmpty() {
        assertThrows(IllegalStateException.class, () ->
                service.exportPendingCandidates("TH1", List.of(), "skos", null));
    }

    @Test
    void exportPendingCandidates_throwsWhenPreferencesMissing() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");

        when(conceptSkosExportPersistence.findThesaurusPreferences("TH1")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                service.exportPendingCandidates("TH1", List.of(candidat), "skos", null));
    }

    @Test
    void exportPendingCandidates_buildsRdfXmlExport() throws Exception {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        var preferences = new Preferences();
        var scheme = new SKOSResource();
        scheme.setUri("http://example.org/thesaurus/TH1");
        var concept = new SKOSResource();
        concept.setUri("http://example.org/thesaurus/TH1/concept/C1");

        when(conceptSkosExportPersistence.findThesaurusPreferences("TH1")).thenReturn(Optional.of(preferences));
        when(conceptSkosExportPersistence.exportConceptScheme("TH1", preferences)).thenReturn(scheme);
        when(conceptSkosExportPersistence.exportConcept("TH1", "C1", true)).thenReturn(concept);
        when(conceptSkosExportPersistence.serializeSkos(any(), eq(RDFFormat.RDFXML))).thenReturn("<rdf/>".getBytes());

        var progress = new AtomicInteger();
        var result = service.exportPendingCandidates("TH1", List.of(candidat), "skos", progress::set);

        assertNotNull(result.content());
        assertEquals("candidats.rdf", result.filename());
        assertEquals(100, progress.get());
    }
}
