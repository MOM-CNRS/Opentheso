package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ConceptTypeService;
import fr.cnrs.opentheso.services.NonPreferredTermService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyConceptWriteSupportTest {

    @Mock
    private TermRepository termRepository;
    @Mock
    private PreferredTermRepository preferredTermRepository;
    @Mock
    private ConceptDcTermRepository conceptDcTermRepository;
    @Mock
    private TermService termService;
    @Mock
    private ConceptService conceptService;
    @Mock
    private ConceptAddService conceptAddService;
    @Mock
    private RelationService relationService;
    @Mock
    private NonPreferredTermService nonPreferredTermService;
    @Mock
    private NoteService noteService;
    @Mock
    private ConceptTypeService conceptTypeService;

    private LegacyConceptWriteSupport support;

    @BeforeEach
    void setUp() {
        support = new LegacyConceptWriteSupport(
                termRepository,
                preferredTermRepository,
                conceptDcTermRepository,
                termService,
                conceptService,
                conceptAddService,
                relationService,
                nonPreferredTermService,
                noteService,
                conceptTypeService
        );
    }

    @Test
    void renamePreferredLabel_returnsValidationErrorWhenBlank() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 42, "admin", "  ", "", false);

        var result = support.renamePreferredLabel(command);

        assertFalse(result.success());
        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
    }

    @Test
    void renamePreferredLabel_returnsDuplicateWhenLabelExistsOnOtherConcept() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 42, "admin", "Chat", "", false);
        var existingTerm = new fr.cnrs.opentheso.entites.Term();
        existingTerm.setId(99);
        existingTerm.setLexicalValue("Chat");
        var currentTerm = new fr.cnrs.opentheso.entites.Term();
        currentTerm.setId(1);
        when(termRepository.findByLexicalValueAndLangAndIdThesaurus("Chat", "fr", "TH1"))
                .thenReturn(Optional.of(existingTerm));
        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idTerm("T1").build()));
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "fr"))
                .thenReturn(Optional.of(currentTerm));

        var result = support.renamePreferredLabel(command);

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        verify(termService, never()).updateTermTraduction(any(), any(), any(), any(), eq(42));
    }

    @Test
    void renamePreferredLabel_allowsUnchangedLabelForSameConcept() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 42, "admin", "Chat", "", false);
        var existingTerm = new fr.cnrs.opentheso.entites.Term();
        existingTerm.setId(1);
        existingTerm.setLexicalValue("Chat");
        when(termRepository.findByLexicalValueAndLangAndIdThesaurus("Chat", "fr", "TH1"))
                .thenReturn(Optional.of(existingTerm));
        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idTerm("T1").build()));
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "fr"))
                .thenReturn(Optional.of(existingTerm));
        when(termService.isTermExistInLangAndThesaurus("T1", "TH1", "fr")).thenReturn(true);

        var result = support.renamePreferredLabel(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(termService).updateTermTraduction("Chat", "T1", "fr", "TH1", 42);
    }

    @Test
    void renamePreferredLabel_updatesExistingTranslation() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 42, "admin", "Nouveau label", "", true);
        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(PreferredTerm.builder().idTerm("T1").build()));
        when(termService.isTermExistInLangAndThesaurus("T1", "TH1", "fr")).thenReturn(true);

        var result = support.renamePreferredLabel(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(termService).updateTermTraduction("Nouveau label", "T1", "fr", "TH1", 42);
        verify(conceptService).updateDateOfConcept("TH1", "C1", 42);
    }

    @Test
    void addBroaderRelation_rejectsSelfLink() {
        var command = new AddBroaderRelationCommand("TH1", "C1", "C1", 42, "admin");

        var result = support.addBroaderRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(relationService, never()).addRelationBT(any(), any(), any(), eq(42));
    }

    @Test
    void addBroaderRelation_addsRelationAndClearsTopConceptFlag() {
        var command = new AddBroaderRelationCommand("TH1", "C1", "C2", 42, "admin");
        when(relationService.isConceptHaveRelationRT("C1", "C2", "TH1")).thenReturn(false);
        when(relationService.isConceptHaveRelationNTorBT("C1", "C2", "TH1")).thenReturn(false);
        when(conceptService.isTopConcept("C1", "TH1")).thenReturn(true);
        when(conceptService.setTopConcept("C1", "TH1", false)).thenReturn(true);

        var result = support.addBroaderRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).addRelationBT("C1", "TH1", "C2", 42);
        verify(conceptService).setTopConcept("C1", "TH1", false);
    }

    @Test
    void deleteNarrowerRelation_promotesOrphanToTopConcept() {
        var command = new DeleteNarrowerRelationCommand("TH1", "C1", "C3", 42, "admin");
        when(relationService.isConceptHaveRelationBT("C3", "TH1")).thenReturn(false);
        when(conceptService.setTopConcept("C3", "TH1", true)).thenReturn(true);

        var result = support.deleteNarrowerRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).deleteRelationNT("C1", "TH1", "C3", 42);
        verify(conceptService).setTopConcept("C3", "TH1", true);
    }

    @Test
    void addNarrowerRelation_rejectsExistingLink() {
        var command = new AddNarrowerRelationCommand("TH1", "C1", "C2", 42, "admin");
        when(relationService.isConceptHaveRelationNTorBT("C1", "C2", "TH1")).thenReturn(true);

        var result = support.addNarrowerRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(relationService, never()).addRelationNT(any(), any(), any(), eq(42));
    }

    @Test
    void deleteBroaderRelation_promotesConceptWithoutBroaderToTopConcept() {
        var command = new DeleteBroaderRelationCommand("TH1", "C1", "C2", 42, "admin");
        when(relationService.isConceptHaveRelationBT("C1", "TH1")).thenReturn(false);
        when(conceptService.setTopConcept("C1", "TH1", true)).thenReturn(true);

        var result = support.deleteBroaderRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).deleteRelationBT("C1", "TH1", "C2", 42);
        verify(conceptService).setTopConcept("C1", "TH1", true);
    }

    @Test
    void updateNarrowerRelationType_updatesRoles() {
        var command = new UpdateNarrowerRelationTypeCommand("TH1", "C1", "C2", "NTG", 42, "admin");
        when(relationService.updateRelationNT("C1", "C2", "TH1", "NTG", "BTG", 42)).thenReturn(true);

        var result = support.updateNarrowerRelationType(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).updateRelationNT("C1", "C2", "TH1", "NTG", "BTG", 42);
    }

    @Test
    void addRelatedRelation_rejectsHierarchicalConflict() {
        var command = new AddRelatedRelationCommand("TH1", "C1", "C2", "fr", 42, "admin", false);
        when(relationService.isConceptHaveRelationNTorBT("C1", "C2", "TH1")).thenReturn(true);

        var result = support.addRelatedRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(relationService, never()).addRelationRT(any(), any(), any(), eq(42));
    }

    @Test
    void addRelatedRelation_addsRelation() {
        var command = new AddRelatedRelationCommand("TH1", "C1", "C2", "fr", 42, "admin", false);
        when(relationService.isConceptHaveRelationNTorBT("C1", "C2", "TH1")).thenReturn(false);
        when(relationService.addRelationRT("C1", "TH1", "C2", 42)).thenReturn(true);

        var result = support.addRelatedRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).addRelationRT("C1", "TH1", "C2", 42);
    }

    @Test
    void applyNarrowerRelationToBranch_updatesChildrenRecursively() {
        var command = new ApplyNarrowerRelationToBranchCommand("TH1", "C1", "NTP", 42, "admin");
        when(conceptService.getListChildrenOfConcept("C1", "TH1")).thenReturn(List.of("C2"));
        when(conceptService.getListChildrenOfConcept("C2", "TH1")).thenReturn(List.of());
        when(relationService.updateRelationNT("C1", "C2", "TH1", "NTP", "BTP", 42)).thenReturn(true);

        var result = support.applyNarrowerRelationToBranch(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).updateRelationNT("C1", "C2", "TH1", "NTP", "BTP", 42);
    }

    @Test
    void deleteRelatedRelation_removesAssociation() {
        var command = new DeleteRelatedRelationCommand("TH1", "C1", "C2", 42, "admin");

        var result = support.deleteRelatedRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).deleteRelationRT("C1", "TH1", "C2", 42);
    }

    @Test
    void addCustomRelation_rejectsUnknownConceptType() {
        var command = new AddCustomRelationCommand("TH1", "C1", "C2", 42, "admin");
        var target = new fr.cnrs.opentheso.entites.Concept();
        target.setIdConcept("C2");
        target.setConceptType("  ");
        when(conceptService.getConcept("C2", "TH1")).thenReturn(target);

        var result = support.addCustomRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(relationService, never()).addCustomRelationship(any(), any(), any(), eq(42), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void addCustomRelation_addsReciprocalRelationFromTargetType() {
        var command = new AddCustomRelationCommand("TH1", "C1", "C2", 42, "admin");
        var target = new fr.cnrs.opentheso.entites.Concept();
        target.setIdConcept("C2");
        target.setConceptType("partOf");
        when(conceptService.getConcept("C2", "TH1")).thenReturn(target);
        when(conceptTypeService.getNodeTypeConcept("partOf", "TH1"))
                .thenReturn(fr.cnrs.opentheso.models.concept.NodeConceptType.builder()
                        .code("partOf").reciprocal(true).build());

        var result = support.addCustomRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).addCustomRelationship("C1", "TH1", "C2", 42, "partOf", true);
    }

    @Test
    void deleteCustomRelation_removesRelation() {
        var command = new DeleteCustomRelationCommand("TH1", "C1", "C2", "partOf", true, 42, "admin");

        var result = support.deleteCustomRelation(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationService).deleteCustomRelationship("C1", "TH1", "C2", 42, "partOf", true);
    }

    @Test
    void deleteCustomRelation_rejectsMissingTarget() {
        var command = new DeleteCustomRelationCommand("TH1", "C1", "  ", "partOf", true, 42, "admin");

        var result = support.deleteCustomRelation(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(relationService, never()).deleteCustomRelationship(any(), any(), any(), eq(42), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
