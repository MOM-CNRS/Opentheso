package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ConceptFullQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptSearchHydrationServiceTest {

    @Mock
    private ConceptSearchQueryRepository conceptSearchQueryRepository;
    @Mock
    private ConceptFullQueryRepository conceptFullQueryRepository;

    @InjectMocks
    private ConceptSearchHydrationService service;

    @Test
    void hydrateResult_buildsSearchNodeFromNativeQueries() {
        when(conceptSearchQueryRepository.findConceptStatus("C1", "TH1")).thenReturn(Optional.of("C"));
        when(conceptFullQueryRepository.findPreferredLabel("C1", "TH1", "fr"))
                .thenReturn(Optional.of(new Object[]{"Chat", "T1", 1}));
        when(conceptSearchQueryRepository.findSynonymsForSearch("C1", "TH1", "fr"))
                .thenReturn(Collections.emptyList());
        when(conceptSearchQueryRepository.findBroaderRelationsForSearch("C1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{"P1", "BT", "Parent", "C"}));
        when(conceptSearchQueryRepository.findRelatedRelationsForSearch("C1", "TH1", "fr"))
                .thenReturn(Collections.emptyList());

        var result = service.hydrateResult("C1", "TH1", "fr");

        assertNotNull(result);
        assertEquals("C1", result.getConceptId());
        assertEquals("Chat", result.getPreferredLabel());
        assertEquals(1, result.getBroaderTerms().size());
        assertEquals("Parent", result.getBroaderTerms().get(0));
    }
}
