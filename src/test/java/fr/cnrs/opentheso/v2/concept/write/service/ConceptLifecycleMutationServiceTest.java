package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLifecycleNativeWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptLifecycleMutationServiceTest {

    @Mock
    private ConceptLifecycleNativeWriteService conceptLifecycleNativeWriteService;

    private ConceptLifecycleMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptLifecycleMutationService(
                conceptLifecycleNativeWriteService,
                org.mockito.Mockito.mock(fr.cnrs.opentheso.v2.concept.write.persistence.ConceptStructureNativeWriteService.class)
        );
    }

    @Test
    void renamePreferredLabel_delegatesToPersistence() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 42, "admin", "Label", "", false);
        when(conceptLifecycleNativeWriteService.renamePreferredLabel(command))
                .thenReturn(MutationResult.ok("Le concept a bien été modifié"));

        var result = service.renamePreferredLabel(command);

        assertTrue(result.success());
        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptLifecycleNativeWriteService).renamePreferredLabel(command);
    }
}
