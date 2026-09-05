package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptMediaWritePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptMediaMutationServiceTest {

    @Mock
    private ConceptMediaWritePersistence persistence;

    private ConceptMediaMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptMediaMutationService(persistence);
    }

    @Test
    void mutations_delegateToPersistence() {
        var gps = mock(ReplaceGpsCoordinatesCommand.class);
        var addImage = mock(AddConceptImageCommand.class);
        var updateImage = mock(UpdateConceptImageCommand.class);
        var deleteImage = mock(DeleteConceptImageCommand.class);
        var addResource = mock(AddExternalResourceCommand.class);
        var updateResource = mock(UpdateExternalResourceCommand.class);
        var deleteResource = mock(DeleteExternalResourceCommand.class);
        when(persistence.replaceGpsCoordinates(gps)).thenReturn(MutationResult.ok("gps"));
        when(persistence.addImage(addImage)).thenReturn(MutationResult.ok("ai"));
        when(persistence.updateImage(updateImage)).thenReturn(MutationResult.ok("ui"));
        when(persistence.deleteImage(deleteImage)).thenReturn(MutationResult.ok("di"));
        when(persistence.addExternalResource(addResource)).thenReturn(MutationResult.ok("ar"));
        when(persistence.updateExternalResource(updateResource)).thenReturn(MutationResult.ok("ur"));
        when(persistence.deleteExternalResource(deleteResource)).thenReturn(MutationResult.ok("dr"));

        assertTrue(service.replaceGpsCoordinates(gps).success());
        assertTrue(service.addImage(addImage).success());
        assertTrue(service.updateImage(updateImage).success());
        assertTrue(service.deleteImage(deleteImage).success());
        assertTrue(service.addExternalResource(addResource).success());
        assertTrue(service.updateExternalResource(updateResource).success());
        assertTrue(service.deleteExternalResource(deleteResource).success());
    }
}
