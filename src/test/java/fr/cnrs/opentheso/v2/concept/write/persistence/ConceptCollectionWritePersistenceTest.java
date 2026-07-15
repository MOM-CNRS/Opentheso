package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptCollectionWritePersistenceTest {

    @Mock
    private ConceptGroupConceptRepository conceptGroupConceptRepository;
    @Mock
    private BranchConceptSupport branchConceptSupport;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @InjectMocks
    private ConceptCollectionWritePersistence support;

    @Test
    void addToCollection_rejectsMissingCollection() {
        var command = new AddConceptToCollectionCommand("TH1", "C1", 7, "admin", "", false);

        var result = support.addToCollection(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptGroupConceptRepository, never()).save(any());
    }

    @Test
    void addToCollection_addsBranchAndTouchesConcept() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C1")).thenReturn(List.of("C1", "C2"));
        var command = new AddConceptToCollectionCommand("TH1", "C1", 7, "admin", "g1", true);

        var result = support.addToCollection(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertTrue(result.message().contains("branche"));
        verify(conceptGroupConceptRepository, org.mockito.Mockito.times(2)).save(any(ConceptGroupConcept.class));
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
        verify(conceptWritePostMutationRepository).saveContributorDcTerm("TH1", "C1", "admin");
    }

    @Test
    void removeFromCollection_removesBranchMembers() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C1")).thenReturn(List.of("C1", "C2"));
        var command = new RemoveConceptFromCollectionCommand("TH1", "C1", 7, "admin", "g1", true);

        var result = support.removeFromCollection(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptGroupConceptRepository).deleteByIdGroupAndIdConceptAndIdThesaurus("g1", "C1", "TH1");
        verify(conceptGroupConceptRepository).deleteByIdGroupAndIdConceptAndIdThesaurus("g1", "C2", "TH1");
    }
}
