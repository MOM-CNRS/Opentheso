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

    private void stubMinimalConceptQueries() {
        Object[] core = new Object[]{
                "C1", "C", "ark1", "", "", "N1",
                Date.valueOf("2024-01-01"), Date.valueOf("2024-02-01"), "subject"
        };
        when(conceptFullQueryRepository.findConceptCore("C1", "TH1")).thenReturn(Optional.of(core));
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
        when(conceptFullQueryRepository.findNarrowerRelations(
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
}
