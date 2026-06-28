package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.exports.rdf4j.ExportRdf4jHelperNew;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatExportServiceTest {

    @Mock
    private ExportRdf4jHelperNew exportRdf4jHelperNew;

    @Mock
    private PreferenceService preferenceService;

    private CandidatExportService service;

    @BeforeEach
    void setUp() {
        service = new CandidatExportService(exportRdf4jHelperNew, preferenceService);
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

        when(preferenceService.getThesaurusPreferences("TH1")).thenReturn(null);

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

        when(preferenceService.getThesaurusPreferences("TH1")).thenReturn(preferences);
        when(exportRdf4jHelperNew.exportThesoV2("TH1", preferences)).thenReturn(scheme);
        when(exportRdf4jHelperNew.exportConceptV2("TH1", "C1", true)).thenReturn(concept);

        var progress = new AtomicInteger();
        var result = service.exportPendingCandidates("TH1", List.of(candidat), "skos", progress::set);

        assertNotNull(result.content());
        assertEquals("candidats.rdf", result.filename());
        assertEquals(100, progress.get());
    }
}
