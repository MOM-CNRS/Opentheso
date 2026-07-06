package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptRelationNativeWriteServiceTest {

    @Mock
    private ConceptRelationWriteRepository conceptRelationWriteRepository;
    @Mock
    private ConceptCustomRelationWriteRepository conceptCustomRelationWriteRepository;
    @Mock
    private ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    @Mock
    private ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    @Mock
    private ConceptRenameWriteRepository conceptRenameWriteRepository;
    @Mock
    private ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @InjectMocks
    private ConceptRelationNativeWriteService service;

    @Test
    void addBroaderRelation_rejectsSelfRelation() {
        var command = new AddBroaderRelationCommand("TH1", "C1", "C1", 7, "admin");

        var result = service.addBroaderRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptRelationWriteRepository, never()).addBroaderRelation(
                anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void addBroaderRelation_clearsTopConceptFlag() {
        var command = new AddBroaderRelationCommand("TH1", "C1", "C2", 7, "admin");
        when(conceptRelationWriteRepository.hasHierarchicalRelation("C1", "C2", "TH1")).thenReturn(false);
        when(conceptRelationWriteRepository.hasRelatedRelation("C1", "C2", "TH1")).thenReturn(false);
        when(conceptLifecycleWriteRepository.isTopConcept("TH1", "C1")).thenReturn(true);
        when(conceptLifecycleWriteRepository.setTopConcept("TH1", "C1", false)).thenReturn(true);

        var result = service.addBroaderRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).addBroaderRelation("C1", "C2", "TH1", 7);
        verify(conceptLifecycleWriteRepository).setTopConcept("TH1", "C1", false);
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
    }

    @Test
    void deleteBroaderRelation_promotesConceptToTopWhenNoBroaderLeft() {
        var command = new DeleteBroaderRelationCommand("TH1", "C1", "C2", 7, "admin");
        when(conceptRelationWriteRepository.hasBroaderRelation("C1", "TH1")).thenReturn(false);
        when(conceptLifecycleWriteRepository.setTopConcept("TH1", "C1", true)).thenReturn(true);

        var result = service.deleteBroaderRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).deleteBroaderRelation("C1", "C2", "TH1", 7);
        verify(conceptLifecycleWriteRepository).setTopConcept("TH1", "C1", true);
    }

    @Test
    void updateNarrowerRelationType_updatesBothDirections() {
        var command = new UpdateNarrowerRelationTypeCommand("TH1", "C1", "C2", "NTG", 7, "admin");

        var result = service.updateNarrowerRelationType(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).updateRelationRoles(
                "C1", "C2", "TH1", "NTG", "BTG", 7);
    }

    @Test
    void addRelatedRelation_rejectsExistingHierarchicalRelation() {
        var command = new AddRelatedRelationCommand("TH1", "C1", "C2", "fr", 7, "admin", false);
        when(conceptRelationWriteRepository.hasHierarchicalRelation("C1", "C2", "TH1")).thenReturn(true);

        var result = service.addRelatedRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptRelationWriteRepository, never()).addRelatedRelation(
                anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void addRelatedRelation_tagsPreferredLabelWhenRequested() {
        var command = new AddRelatedRelationCommand("TH1", "C1", "C2", "fr", 7, "admin", true);
        when(conceptRelationWriteRepository.hasHierarchicalRelation("C1", "C2", "TH1")).thenReturn(false);
        when(conceptRelationWriteRepository.addRelatedRelation("C1", "C2", "TH1", 7)).thenReturn(true);
        when(conceptLexicalWriteRepository.findPreferredTermId("TH1", "C1")).thenReturn(Optional.of("T1"));
        when(conceptRenameWriteRepository.existsTermInLang("T1", "TH1", "fr")).thenReturn(true);
        when(conceptLexicalWriteRepository.findPreferredLabel("C1", "TH1", "fr")).thenReturn(Optional.of("Alpha"));
        when(conceptLexicalWriteRepository.findPreferredLabel("C2", "TH1", "fr")).thenReturn(Optional.of("Beta"));

        var result = service.addRelatedRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptTranslationWriteRepository).updateTranslation(
                eq("T1"), eq("TH1"), eq("fr"), eq("Alpha (Beta)"), eq(7));
    }

    @Test
    void applyNarrowerRelationToBranch_updatesChildrenRecursively() {
        var command = new ApplyNarrowerRelationToBranchCommand("TH1", "C1", "NTI", 7, "admin");
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C1", "TH1"))
                .thenReturn(List.of("C2"));
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C2", "TH1"))
                .thenReturn(List.of());

        var result = service.applyNarrowerRelationToBranch(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).updateRelationRoles(
                "C1", "C2", "TH1", "NTI", "BTI", 7);
    }

    @Test
    void addCustomRelation_rejectsUnknownConceptType() {
        var command = new AddCustomRelationCommand("TH1", "C1", "C2", 7, "admin");
        when(conceptCustomRelationWriteRepository.findConceptTypeCode("C2", "TH1"))
                .thenReturn(Optional.of("place"));
        when(conceptCustomRelationWriteRepository.findConceptTypeReciprocal("place", "TH1"))
                .thenReturn(Optional.empty());

        var result = service.addCustomRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptRelationWriteRepository, never()).addCustomRelation(
                anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyInt());
    }

    @Test
    void addCustomRelation_insertsReciprocalLinkWhenConfigured() {
        var command = new AddCustomRelationCommand("TH1", "C1", "C2", 7, "admin");
        when(conceptCustomRelationWriteRepository.findConceptTypeCode("C2", "TH1"))
                .thenReturn(Optional.of("place"));
        when(conceptCustomRelationWriteRepository.findConceptTypeReciprocal("place", "TH1"))
                .thenReturn(Optional.of(true));

        var result = service.addCustomRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).addCustomRelation(
                "C1", "C2", "TH1", "place", true, 7);
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
    }

    @Test
    void deleteCustomRelation_deletesSelectedRelation() {
        var command = new DeleteCustomRelationCommand("TH1", "C1", "C2", "place", false, 7, "admin");

        var result = service.deleteCustomRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).deleteCustomRelation(
                "C1", "C2", "TH1", "place", false, 7);
    }
}
