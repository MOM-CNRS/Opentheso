package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptCollectionWritePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptCollectionMutationServiceTest {

    @Mock
    private ConceptCollectionWritePersistence persistence;

    private ConceptCollectionMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptCollectionMutationService(persistence);
    }

    @Test
    void mutations_delegateToPersistence() {
        var add = mock(AddConceptToCollectionCommand.class);
        var remove = mock(RemoveConceptFromCollectionCommand.class);
        when(persistence.addToCollection(add)).thenReturn(MutationResult.ok("add"));
        when(persistence.removeFromCollection(remove)).thenReturn(MutationResult.ok("rm"));

        assertTrue(service.addToCollection(add).success());
        assertTrue(service.removeFromCollection(remove).success());
    }
}
