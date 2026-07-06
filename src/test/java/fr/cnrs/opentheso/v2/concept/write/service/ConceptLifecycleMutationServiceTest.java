package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWritePort;
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
    private ConceptWritePort conceptWriteLegacySupport;

    private ConceptLifecycleMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptLifecycleMutationService(conceptWriteLegacySupport);
    }

    @Test
    void renamePreferredLabel_delegatesToLegacySupport() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 42, "admin", "Label", "", false);
        when(conceptWriteLegacySupport.renamePreferredLabel(command))
                .thenReturn(MutationResult.ok("Le concept a bien été modifié"));

        var result = service.renamePreferredLabel(command);

        assertTrue(result.success());
        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptWriteLegacySupport).renamePreferredLabel(command);
    }
}
