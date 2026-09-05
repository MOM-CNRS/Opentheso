package fr.cnrs.opentheso.v2.concept.mapper;

import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptResourceStatus;
import fr.cnrs.opentheso.v2.shared.repository.ConceptFullQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptFullAssemblerTest {

    @Mock
    private ConceptFullQueryRepository conceptFullQueryRepository;

    @Mock
    private ExternalResourcesRepository externalResourcesRepository;

    @InjectMocks
    private ConceptFullAssembler assembler;

    @Test
    void assemble_buildsConceptFromNativeQueries() {
        stubMinimalConceptQueries();
        when(externalResourcesRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Collections.emptyList());

        Optional<ConceptFullSnapshot> loaded = assembler.assemble(
                "TH1", "C1", "fr", 0, 41, true, null, "http://localhost/"
        );

        assertTrue(loaded.isPresent());
        ConceptFullSnapshot concept = loaded.get();
        assertEquals("C1", concept.getIdentifier());
        assertEquals(ConceptResourceStatus.CONCEPT, concept.getResourceStatus());
        assertEquals("Libellé", concept.getPrefLabel().getLabel());
        assertEquals(null, concept.getCreatorName());
    }

    @Test
    void assemble_mapsExternalResourcesFromRepository() {
        stubMinimalConceptQueries();
        when(externalResourcesRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(List.of(ExternalResource.builder()
                        .idConcept("C1")
                        .idThesaurus("TH1")
                        .externalUri("https://example.org/doc")
                        .description("Doc")
                        .build()));

        Optional<ConceptFullSnapshot> loaded = assembler.assemble(
                "TH1", "C1", "fr", 0, 41, true, null, "http://localhost/"
        );

        assertTrue(loaded.isPresent());
        assertEquals(1, loaded.get().getExternalResources().size());
        assertEquals("https://example.org/doc", loaded.get().getExternalResources().get(0).uri());
        assertEquals("Doc", loaded.get().getExternalResources().get(0).description());
    }

    @Test
    void assemble_blankIdentifiers_returnsEmpty() {
        assertTrue(assembler.assemble("", "C1", "fr", 0, 10, true, null, "http://localhost/").isEmpty());
        assertTrue(assembler.assemble("TH1", "", "fr", 0, 10, true, null, "http://localhost/", true).isEmpty());
    }

    @Test
    void assemble_mapsNotesAlignmentsGpsImagesFacetsAndRelations() {
        stubMinimalConceptQueries();
        when(conceptFullQueryRepository.findCreator("C1", "TH1")).thenReturn(Optional.of("alice"));
        when(conceptFullQueryRepository.findContributors("C1", "TH1")).thenReturn(List.of("bob"));
        when(conceptFullQueryRepository.findAltLabels("C1", "TH1", "fr", false))
                .thenReturn(rows(new Object[]{"Alt", "T2", "3"}));
        when(conceptFullQueryRepository.findAltLabels("C1", "TH1", "fr", true))
                .thenReturn(rows(new Object[]{"Hidden", "T3", "4"}));
        when(conceptFullQueryRepository.findPrefLabelTranslations("C1", "TH1", "fr"))
                .thenReturn(rows(new Object[]{"T1", "Label EN", "en", "9", "gb"}));
        when(conceptFullQueryRepository.findAltLabelTranslations("C1", "TH1", "fr", false))
                .thenReturn(rows(new Object[]{"T2", "Alt EN", "en", "8"}));
        when(conceptFullQueryRepository.findAltLabelTranslations("C1", "TH1", "fr", true))
                .thenReturn(rows(new Object[]{"T3", "Hid EN", "en", "7"}));
        when(conceptFullQueryRepository.findAllConceptNotes("C1", "TH1")).thenReturn(rows(
                new Object[]{"1", "note", "une note", "fr", "src"},
                new Object[]{"2", "definition", "def", "fr", "src"},
                new Object[]{"3", "example", "ex", "fr", "src"},
                new Object[]{"4", "editorialNote", "ed", "fr", "src"},
                new Object[]{"5", "changeNote", "ch", "fr", "src"},
                new Object[]{"6", "scopeNote", "sc", "fr", "src"},
                new Object[]{"7", "historyNote", "hi", "fr", "src"},
                new Object[]{"8", "other", "x", "fr", "src"}
        ));
        when(conceptFullQueryRepository.findBroaderRelations("C1", "TH1", "fr", true))
                .thenReturn(rows(new Object[]{"Parent", "P1", "BT", "arkP", "", ""}));
        when(conceptFullQueryRepository.findRelatedRelations("C1", "TH1", "fr"))
                .thenReturn(rows(new Object[]{"Cousin", "R1", "RT", "", "", ""}));
        when(conceptFullQueryRepository.findNarrowerRelations(
                eq("C1"), eq("TH1"), eq("fr"), anyBoolean(), anyInt(), anyInt()
        )).thenReturn(rows(new Object[]{"Enfant", "N1", "NT", "arkN", "", ""}));
        when(conceptFullQueryRepository.findAlignments("C1", "TH1")).thenReturn(rows(
                new Object[]{"Q1", "http://ex/1", "1", "exact"},
                new Object[]{"Q2", "http://ex/2", "2", "close"},
                new Object[]{"Q3", "http://ex/3", "3", "broad"},
                new Object[]{"Q4", "http://ex/4", "4", "related"},
                new Object[]{"Q5", "http://ex/5", "5", "narrow"},
                new Object[]{"Q6", "http://ex/6", "9", "other"}
        ));
        when(conceptFullQueryRepository.findGpsPoints("C1", "TH1"))
                .thenReturn(rows(new Object[]{48.8, 2.3, "1"}));
        when(conceptFullQueryRepository.findGroupMemberships("C1", "TH1", "fr"))
                .thenReturn(rows(new Object[]{"G1", "arkG", "", "", "Groupe"}));
        when(conceptFullQueryRepository.findImages("C1", "TH1"))
                .thenReturn(rows(new Object[]{"11", "null", "copy", "http://img", "alice"}));
        when(conceptFullQueryRepository.findReplaces("C1", "TH1", "fr"))
                .thenReturn(rows(new Object[]{"OLD", "arkO", "", "", "Ancien"}));
        when(conceptFullQueryRepository.findReplacedBy("C1", "TH1", "fr"))
                .thenReturn(rows(new Object[]{"NEW", "arkN", "", "", "Nouveau"}));
        when(conceptFullQueryRepository.findFacets("C1", "TH1", "fr"))
                .thenReturn(rows(new Object[]{"F1", "Facet A"}, new Object[]{"", "ignored"}));
        when(externalResourcesRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Collections.emptyList());

        Optional<ConceptFullSnapshot> loaded = assembler.assemble(
                "TH1", "C1", "fr", 0, 41, true, null, "http://localhost/"
        );

        assertTrue(loaded.isPresent());
        ConceptFullSnapshot concept = loaded.get();
        assertEquals("alice", concept.getCreatorName());
        assertEquals(1, concept.getAltLabels().size());
        assertEquals(1, concept.getHiddenLabels().size());
        assertEquals(1, concept.getDefinitions().size());
        assertEquals(1, concept.getExactMatchs().size());
        assertEquals(1, concept.getCloseMatchs().size());
        assertEquals(1, concept.getGps().size());
        assertEquals(1, concept.getImages().size());
        assertEquals("", concept.getImages().get(0).imageName());
        assertEquals(1, concept.getFacets().size());
        assertEquals("N1", concept.getNarrowers().get(0).conceptId());
    }

    @Test
    void assembleNarrowerRelations_returnsEmptyWhenLimitInvalid() {
        assertTrue(assembler.assembleNarrowerRelations("TH1", "C1", "fr", 0, 0, true, null, "http://x").isEmpty());
    }

    @Test
    void assembleNarrowerRelations_mapsRows() {
        when(conceptFullQueryRepository.findNarrowerRelations(
                eq("C1"), eq("TH1"), eq("fr"), anyBoolean(), anyInt(), anyInt()
        )).thenReturn(rows(new Object[]{"Enfant", "N1", "NT", "", "", ""}));

        var relations = assembler.assembleNarrowerRelations("TH1", "C1", "fr", 0, 10, true, null, "http://x");

        assertEquals(1, relations.size());
        assertEquals("N1", relations.get(0).conceptId());
    }

    private void stubMinimalConceptQueries() {
        Object[] core = new Object[]{
                "C1", "C", "ark1", "", "", "N1",
                Date.valueOf("2024-01-01"), Date.valueOf("2024-02-01"), "subject"
        };
        when(conceptFullQueryRepository.findConceptCore(eq("C1"), eq("TH1"), anyBoolean()))
                .thenReturn(Optional.of(core));
        when(conceptFullQueryRepository.findPreferredLabel("C1", "TH1", "fr"))
                .thenReturn(Optional.of(new Object[]{"Libellé", "T1", 12}));
        when(conceptFullQueryRepository.findAltLabels(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findPrefLabelTranslations(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findAltLabelTranslations(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findAllConceptNotes(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findBroaderRelations(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findRelatedRelations(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        org.mockito.Mockito.lenient().when(conceptFullQueryRepository.findNarrowerRelations(
                eq("C1"), eq("TH1"), eq("fr"), anyBoolean(), anyInt(), anyInt()
        )).thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findAlignments(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findGpsPoints(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findGroupMemberships(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findImages(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findReplaces(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findReplacedBy(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(conceptFullQueryRepository.findFacets(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
    }

    private static List<Object[]> rows(Object[]... values) {
        return Arrays.asList(values);
    }
}
