package fr.cnrs.opentheso.v2.concept.write.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchConceptSupportTest {

    @Mock
    private ConceptRelationWriteRepository conceptRelationWriteRepository;

    @InjectMocks
    private BranchConceptSupport support;

    @Test
    void collectBranchConceptIds_returnsEmptyWhenBlankInput() {
        assertTrue(support.collectBranchConceptIds("TH1", "").isEmpty());
        assertTrue(support.collectBranchConceptIds("", "C1").isEmpty());
    }

    @Test
    void collectBranchConceptIds_collectsRootAndDescendants() {
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C1", "TH1"))
                .thenReturn(List.of("C2"));
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C2", "TH1"))
                .thenReturn(List.of("C3"));
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C3", "TH1"))
                .thenReturn(List.of());

        var ids = support.collectBranchConceptIds("TH1", "C1");

        assertEquals(List.of("C1", "C2", "C3"), ids);
    }

    @Test
    void collectBranchConceptIds_stopsOnCycle() {
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C1", "TH1"))
                .thenReturn(List.of("C2"));
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C2", "TH1"))
                .thenReturn(List.of("C1"));

        var ids = support.collectBranchConceptIds("TH1", "C1");

        assertEquals(List.of("C1", "C2"), ids);
    }
}
