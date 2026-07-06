package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.legacybridge.LegacyConceptCreationSupport;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTopConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptStructureNativeWriteServiceTest {

    @Mock
    private ConceptCreationWriteRepository conceptCreationWriteRepository;
    @Mock
    private ConceptDeletionWriteRepository conceptDeletionWriteRepository;
    @Mock
    private ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    @Mock
    private ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    @Mock
    private ConceptRenameWriteRepository conceptRenameWriteRepository;
    @Mock
    private ConceptRelationWriteRepository conceptRelationWriteRepository;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    @Mock
    private LegacyConceptCreationSupport legacyConceptCreationSupport;

    @InjectMocks
    private ConceptStructureNativeWriteService service;

    @Test
    void addChildConcept_rejectsBlankLabel() {
        var command = new AddChildConceptCommand(
                "TH1", "C1", "fr", 7, "admin", "  ", "", "", "", "", "NT", false);

        var result = service.addChildConcept(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptCreationWriteRepository, never()).insertConcept(
                anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyInt());
    }

    @Test
    void addTopConcept_createsTopConceptWithPreferredTerm() {
        var command = new AddTopConceptCommand(
                "TH1", "fr", 7, "admin", "Root", "", "", "src", "GRP1", false);
        when(conceptLexicalWriteRepository.existsPrefLabel("Root", "fr", "TH1")).thenReturn(false);
        when(conceptLexicalWriteRepository.existsAltLabel("Root", "fr", "TH1")).thenReturn(false);
        when(conceptCreationWriteRepository.generateConceptId("TH1", "")).thenReturn("C99");

        var result = service.addTopConcept(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("C99", result.createdConceptId());
        verify(conceptCreationWriteRepository).insertConcept(
                eq("C99"), eq("TH1"), eq("D"), eq(""), eq(true), eq(7));
        verify(conceptCreationWriteRepository).linkConceptToGroup("GRP1", "C99", "TH1");
        verify(legacyConceptCreationSupport).assignIdentifiers("TH1", "C99", "fr");
        verify(conceptWritePostMutationRepository).saveCreatorDcTerm("TH1", "C99", "admin");
        verify(conceptRelationWriteRepository, never()).addHierarchicalLink(
                anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void addChildConcept_linksParentAndChild() {
        var command = new AddChildConceptCommand(
                "TH1", "C1", "fr", 7, "admin", "Child", "", "", "src", "", "NTG", false);
        when(conceptLexicalWriteRepository.existsPrefLabel("Child", "fr", "TH1")).thenReturn(false);
        when(conceptLexicalWriteRepository.existsAltLabel("Child", "fr", "TH1")).thenReturn(false);
        when(conceptCreationWriteRepository.generateConceptId("TH1", "")).thenReturn("C2");

        var result = service.addChildConcept(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).addHierarchicalLink("C1", "C2", "TH1", "NTG", 7);
        verify(conceptRelationWriteRepository).addHierarchicalLink("C2", "C1", "TH1", "BTG", 7);
    }

    @Test
    void deleteConcept_rejectsBranchWithPolyhierarchy() {
        var command = new DeleteConceptCommand("TH1", "C1", true, false);
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C1", "TH1")).thenReturn(List.of("C2"));
        when(conceptRelationWriteRepository.listNarrowerChildConceptIds("C2", "TH1")).thenReturn(List.of());
        when(conceptRelationWriteRepository.countBroaderRelations("C1", "TH1")).thenReturn(1);
        when(conceptRelationWriteRepository.countBroaderRelations("C2", "TH1")).thenReturn(2);

        var result = service.deleteConcept(command);

        assertEquals(MutationOutcome.FAILURE, result.outcome());
        verify(conceptDeletionWriteRepository, never()).deleteConcept(anyString(), anyString());
    }

    @Test
    void deleteConcept_deletesSingleConcept() {
        var command = new DeleteConceptCommand("TH1", "C1", false, false);

        var result = service.deleteConcept(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptDeletionWriteRepository).deleteConcept("TH1", "C1");
    }
}
