package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptRelationNativeWriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptRelationMutationServiceTest {

    @Mock
    private ConceptRelationNativeWriteService conceptRelationNativeWriteService;
    @Mock
    private ConceptWriteMetadataService conceptWriteMetadataService;

    @InjectMocks
    private ConceptRelationMutationService service;

    @Test
    void addBroaderRelation_delegatesToPersistence() {
        var command = new AddBroaderRelationCommand("TH1", "C1", "C2", 42, "admin");
        when(conceptRelationNativeWriteService.addBroaderRelation(command))
                .thenReturn(MutationResult.ok("Relation ajoutée avec succès"));

        var result = service.addBroaderRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationNativeWriteService).addBroaderRelation(command);
    }

    @Test
    void addRelatedRelation_delegatesToPersistence() {
        var command = new AddRelatedRelationCommand("TH1", "C1", "C2", "fr", 42, "admin", false);
        when(conceptRelationNativeWriteService.addRelatedRelation(command))
                .thenReturn(MutationResult.ok("Relation ajoutée avec succès"));

        service.addRelatedRelation(command);

        verify(conceptRelationNativeWriteService).addRelatedRelation(command);
    }

    @Test
    void deleteNarrowerRelation_delegatesToPersistence() {
        var command = new DeleteNarrowerRelationCommand("TH1", "C1", "C2", 42, "admin");
        when(conceptRelationNativeWriteService.deleteNarrowerRelation(command))
                .thenReturn(MutationResult.ok("Relation supprimée"));

        service.deleteNarrowerRelation(command);

        verify(conceptRelationNativeWriteService).deleteNarrowerRelation(command);
    }

    @Test
    void updateNarrowerRelationType_delegatesToPersistence() {
        var command = new UpdateNarrowerRelationTypeCommand("TH1", "C1", "C2", "NT1", 42, "admin");
        when(conceptRelationNativeWriteService.updateNarrowerRelationType(command))
                .thenReturn(MutationResult.ok("Type mis à jour"));

        service.updateNarrowerRelationType(command);

        verify(conceptRelationNativeWriteService).updateNarrowerRelationType(command);
    }

    @Test
    void applyNarrowerRelationToBranch_delegatesToPersistence() {
        var command = new ApplyNarrowerRelationToBranchCommand("TH1", "C1", "NT1", 42, "admin");
        when(conceptRelationNativeWriteService.applyNarrowerRelationToBranch(command))
                .thenReturn(MutationResult.ok("Branche mise à jour"));

        service.applyNarrowerRelationToBranch(command);

        verify(conceptRelationNativeWriteService).applyNarrowerRelationToBranch(command);
    }
}
