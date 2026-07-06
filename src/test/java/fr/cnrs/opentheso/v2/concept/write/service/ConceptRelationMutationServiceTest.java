package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWritePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptRelationMutationServiceTest {

    @Mock
    private ConceptWritePort conceptWritePort;
    @Mock
    private ConceptWriteMetadataService conceptWriteMetadataService;

    @InjectMocks
    private ConceptRelationMutationService service;

    @Test
    void addBroaderRelation_delegatesToLegacySupport() {
        var command = new AddBroaderRelationCommand("TH1", "C1", "C2", 42, "admin");
        when(conceptWritePort.addBroaderRelation(command))
                .thenReturn(MutationResult.ok("Relation ajoutée avec succès"));

        var result = service.addBroaderRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptWritePort).addBroaderRelation(command);
    }

    @Test
    void deleteNarrowerRelation_delegatesToLegacySupport() {
        var command = new DeleteNarrowerRelationCommand("TH1", "C1", "C3", 42, "admin");
        when(conceptWritePort.deleteNarrowerRelation(command))
                .thenReturn(MutationResult.ok("Relation supprimée avec succès"));

        var result = service.deleteNarrowerRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptWritePort).deleteNarrowerRelation(command);
    }

    @Test
    void updateNarrowerRelationType_delegatesToLegacySupport() {
        var command = new UpdateNarrowerRelationTypeCommand("TH1", "C1", "C2", "NTG", 42, "admin");
        when(conceptWritePort.updateNarrowerRelationType(command))
                .thenReturn(MutationResult.ok("Relation modifiée avec succès"));

        var result = service.updateNarrowerRelationType(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptWritePort).updateNarrowerRelationType(command);
    }

    @Test
    void addRelatedRelation_delegatesToLegacySupport() {
        var command = new AddRelatedRelationCommand("TH1", "C1", "C2", "fr", 42, "admin", false);
        when(conceptWritePort.addRelatedRelation(command))
                .thenReturn(MutationResult.ok("Relation ajoutée avec succès"));

        var result = service.addRelatedRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptWritePort).addRelatedRelation(command);
    }

    @Test
    void listNtRelationTypes_delegatesToMetadataService() {
        var expected = List.of(new fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNtRelationType("NT", "NT", "NT"));
        when(conceptWriteMetadataService.listNtRelationTypes()).thenReturn(expected);

        var result = service.listNtRelationTypes();

        assertEquals(1, result.size());
        verify(conceptWriteMetadataService).listNtRelationTypes();
    }
}
