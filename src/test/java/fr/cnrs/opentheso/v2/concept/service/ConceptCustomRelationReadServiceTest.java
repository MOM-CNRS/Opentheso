package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptCustomRelationReadServiceTest {

    @Mock
    private ConceptQueryRepository conceptQueryRepository;

    private ConceptCustomRelationReadService service;

    @BeforeEach
    void setUp() {
        service = new ConceptCustomRelationReadService(conceptQueryRepository);
    }

    @Test
    void loadCustomRelations_mapsRepositoryRows() {
        when(conceptQueryRepository.findCustomRelations("C1", "TH1", "fr", "fr")).thenReturn(
                List.<Object[]>of(new Object[]{"C2", "Target", "REL", "Relation", true})
        );

        var relations = service.loadCustomRelations("TH1", "C1", "fr");

        assertEquals(1, relations.size());
        assertEquals("C2", relations.get(0).targetConceptId());
        assertEquals("Relation", relations.get(0).relationLabel());
        assertTrue(relations.get(0).reciprocal());
    }

    @Test
    void loadCustomRelations_returnsEmptyWhenInputBlank() {
        assertTrue(service.loadCustomRelations("", "C1", "fr").isEmpty());
    }
}
