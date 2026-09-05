package fr.cnrs.opentheso.v2.candidat.alignment;

import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.v2.candidat.alignment.persistence.CandidatAutoAlignmentPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatAutoAlignmentEngineTest {

    @Mock
    private CandidatAutoAlignmentPersistence persistence;
    @Mock
    private AlignmentAutoExternalSearch externalSearch;

    @InjectMocks
    private CandidatAutoAlignmentEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CandidatAutoAlignmentEngine(persistence, externalSearch);
    }

    @Test
    void prepare_initializesSourcesAndConcept() {
        var source = AlignementSource.builder().id(1).source("Wikidata").source_filter("wikidata_rest").build();
        when(persistence.loadExistingAlignments("C1", "TH1")).thenReturn(List.of());
        when(persistence.loadAlignmentSources("TH1")).thenReturn(List.of(source));
        when(persistence.loadAlignmentTypes()).thenReturn(List.of(Map.entry("1", "skos:exactMatch")));
        when(persistence.loadThesaurusLanguages("TH1")).thenReturn(List.of("fr", "en"));

        engine.prepare("Paris", "C1", "TH1", "fr");

        assertEquals("C1", engine.getIdConceptSelectedForAlignment());
        assertEquals("Paris", engine.getConceptValueForAlignment());
        assertEquals(1, engine.getAlignementSources().size());
        assertTrue(engine.hasAlignmentSources());
    }

    @Test
    void searchAlignments_delegatesToExternalSearch() {
        var source = AlignementSource.builder().id(1).source("Wikidata").source_filter("wikidata_rest").requete("q").build();
        when(persistence.loadExistingAlignments("C1", "TH1")).thenReturn(List.of());
        when(persistence.loadAlignmentSources("TH1")).thenReturn(List.of(source));
        when(persistence.loadAlignmentTypes()).thenReturn(List.of());
        when(persistence.loadThesaurusLanguages("TH1")).thenReturn(List.of("fr"));
        engine.prepare("Paris", "C1", "TH1", "fr");
        engine.setSelectedAlignement("Wikidata");

        var hit = NodeAlignment.builder().uri_target("http://wikidata.org/entity/Q90").concept_target("Paris").build();
        when(externalSearch.search(any(), any())).thenReturn(new AlignmentAutoExternalSearch.SearchOutcome(List.of(hit), null));

        engine.searchAlignments();

        assertEquals(1, engine.getListAlignValues().size());
        verify(externalSearch).search(any(), any());
    }

    @Test
    void actionChoix_idRefAuteurs_setsNameAlignmentAndSplitsLabel() {
        engine.setSelectedAlignement("idRefAuteurs");
        engine.setConceptValueForAlignment("Dupont,Jean");

        engine.actionChoix();

        assertTrue(engine.isNameAlignment());
        assertEquals("Dupont", engine.getNom());
        assertEquals("Jean", engine.getPrenom());
    }

    @Test
    void actionChoix_wikidata_setsAlert() {
        engine.setSelectedAlignement("wikidata");

        engine.actionChoix();

        assertFalse(engine.getAlertWikidata().isBlank());
    }

    @Test
    void cancelManualAlignment_clearsState() {
        engine.setListAlignValues(List.of(NodeAlignment.builder().build()));
        engine.setAlignmentInProgress(true);

        engine.cancelManualAlignment();

        assertFalse(engine.isAlignmentInProgress());
        assertFalse(engine.isViewSelection());
    }
}
