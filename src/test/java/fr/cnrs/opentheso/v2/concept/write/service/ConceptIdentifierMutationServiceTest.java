package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateArkCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.GenerateHandleCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptIdentifierWritePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptIdentifierMutationServiceTest {

    @Mock
    private ConceptIdentifierWritePersistence persistence;

    private ConceptIdentifierMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptIdentifierMutationService(persistence);
    }

    @Test
    void mutations_delegateToPersistence() {
        var generateArk = mock(GenerateArkCommand.class);
        var deleteArk = mock(DeleteArkCommand.class);
        var generateHandle = mock(GenerateHandleCommand.class);
        var deleteHandle = mock(DeleteHandleCommand.class);
        when(persistence.generateArk(generateArk)).thenReturn(MutationResult.ok("ga"));
        when(persistence.deleteArk(deleteArk)).thenReturn(MutationResult.ok("da"));
        when(persistence.generateHandle(generateHandle)).thenReturn(MutationResult.ok("gh"));
        when(persistence.deleteHandle(deleteHandle)).thenReturn(MutationResult.ok("dh"));

        assertTrue(service.generateArk(generateArk).success());
        assertTrue(service.deleteArk(deleteArk).success());
        assertTrue(service.generateHandle(generateHandle).success());
        assertTrue(service.deleteHandle(deleteHandle).success());
    }
}
