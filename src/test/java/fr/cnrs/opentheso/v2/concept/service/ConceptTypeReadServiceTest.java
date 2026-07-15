package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTypeReadServiceTest {

    @Mock
    private ConceptQueryRepository conceptQueryRepository;

    private ConceptTypeReadService service;

    @BeforeEach
    void setUp() {
        service = new ConceptTypeReadService(conceptQueryRepository);
    }

    @Test
    void resolveLabel_returnsFrenchLabel() {
        when(conceptQueryRepository.findConceptType("place", "TH1"))
                .thenReturn(Optional.of(new Object[]{"Lieu", "Place", true}));

        assertEquals("Lieu", service.resolveLabel("place", "TH1", "fr"));
    }

    @Test
    void resolveLabel_returnsEnglishLabel() {
        when(conceptQueryRepository.findConceptType("place", "TH1"))
                .thenReturn(Optional.of(new Object[]{"Lieu", "Place", true}));

        assertEquals("Place", service.resolveLabel("place", "TH1", "en"));
    }

    @Test
    void resolveLabel_returnsEmptyWhenMissing() {
        when(conceptQueryRepository.findConceptType("place", "TH1")).thenReturn(Optional.empty());

        assertTrue(service.resolveLabel("place", "TH1", "fr").isEmpty());
    }
}
