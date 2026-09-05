package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNotationCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptAttributeWritePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAttributeMutationServiceTest {

    @Mock
    private ConceptAttributeWritePersistence persistence;

    private ConceptAttributeMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptAttributeMutationService(persistence);
    }

    @Test
    void mutations_delegateToPersistence() {
        var notation = mock(UpdateNotationCommand.class);
        var type = mock(UpdateConceptTypeCommand.class);
        when(persistence.updateNotation(notation)).thenReturn(MutationResult.ok("n"));
        when(persistence.updateConceptType(type)).thenReturn(MutationResult.ok("t"));

        assertTrue(service.updateNotation(notation).success());
        assertTrue(service.updateConceptType(type).success());
    }
}
