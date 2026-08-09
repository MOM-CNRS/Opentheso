package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.v2.concept.mapper.ConceptFullAssembler;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptFullReadServiceTest {

    @Mock
    private ConceptFullAssembler conceptFullAssembler;

    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;

    @Mock
    private ApplicationUriService applicationUriService;

    private ConceptFullReadService service;

    @BeforeEach
    void setUp() {
        service = new ConceptFullReadService(
                conceptFullAssembler,
                thesaurusPreferenceService,
                applicationUriService
        );
    }

    @Test
    void loadFullConcept_delegatesToAssemblerWithPaginationProbe() {
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");
        when(conceptFullAssembler.assemble(
                eq("TH1"),
                eq("C1"),
                eq("fr"),
                eq(0),
                eq(ConceptFullReadService.NARROWER_PAGE_SIZE + 1),
                eq(true),
                eq(null),
                eq("http://localhost")
        )).thenReturn(Optional.of(fullConcept));

        Optional<ConceptFullSnapshot> loaded = service.loadFullConcept("TH1", "C1", "fr", 0, true);

        assertTrue(loaded.isPresent());
        assertEquals("C1", loaded.get().getIdentifier());
    }

    @Test
    void loadMoreNarrowers_delegatesToAssembler() {
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");
        when(conceptFullAssembler.assembleNarrowerRelations(
                eq("TH1"),
                eq("C1"),
                eq("fr"),
                eq(41),
                eq(ConceptFullReadService.NARROWER_PAGE_SIZE + 1),
                eq(false),
                eq(null),
                eq("http://localhost")
        )).thenReturn(List.of(new ConceptHierarchicalRelation("", "C2", "Child", "NT")));

        List<ConceptHierarchicalRelation> loaded = service.loadMoreNarrowers("TH1", "C1", "fr", 41, false);

        assertEquals(1, loaded.size());
    }

    @Test
    void hasMoreNarrowers_detectsProbeElement() {
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setNarrowers(new java.util.ArrayList<>());
        for (int i = 0; i <= ConceptFullReadService.NARROWER_PAGE_SIZE; i++) {
            fullConcept.getNarrowers().add(new ConceptHierarchicalRelation("", "C" + i, "Label", "NT"));
        }

        assertTrue(service.hasMoreNarrowers(fullConcept));
    }

    @Test
    void hasMoreFromBatch_falseWhenLastPage() {
        List<ConceptHierarchicalRelation> lastPage = List.of(
                new ConceptHierarchicalRelation("", "C2", "Child", "NT")
        );

        assertFalse(service.hasMoreFromBatch(lastPage));
    }

    @Test
    void hasMoreNarrowers_returnsFalseWhenBelowStep() {
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setNarrowers(List.of(new ConceptHierarchicalRelation("", "C2", "Child", "NT")));

        assertFalse(service.hasMoreNarrowers(fullConcept));
    }
}
