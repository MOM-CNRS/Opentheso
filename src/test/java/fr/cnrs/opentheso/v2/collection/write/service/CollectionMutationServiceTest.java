package fr.cnrs.opentheso.v2.collection.write.service;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptGroupLabel;
import fr.cnrs.opentheso.entites.RelationGroup;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.v2.collection.identifier.CollectionIdentifierAssignmentService;
import fr.cnrs.opentheso.v2.collection.write.model.command.AddMemberToCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.CreateCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.MoveCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RemoveMemberFromCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RenameCollectionLabelCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionNotationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionMutationServiceTest {

    @Mock
    private ConceptGroupRepository conceptGroupRepository;
    @Mock
    private ConceptGroupLabelRepository conceptGroupLabelRepository;
    @Mock
    private ConceptGroupConceptRepository conceptGroupConceptRepository;
    @Mock
    private RelationGroupRepository relationGroupRepository;
    @Mock
    private BranchConceptSupport branchConceptSupport;
    @Mock
    private CollectionIdentifierAssignmentService collectionIdentifierAssignmentService;

    @InjectMocks
    private CollectionMutationService service;

    @Test
    void renamePreferredLabel_rejectsBlankLabel() {
        var command = new RenameCollectionLabelCommand("TH1", "g1", "fr", "  ", 7);

        var result = service.renamePreferredLabel(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptGroupLabelRepository, never()).save(any());
    }

    @Test
    void renamePreferredLabel_updatesExistingLabel() {
        var label = ConceptGroupLabel.builder().idGroup("g1").idThesaurus("TH1").lang("fr").lexicalValue("Old").build();
        when(conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang("TH1", "g1", "fr"))
                .thenReturn(List.of(label));

        var command = new RenameCollectionLabelCommand("TH1", "g1", "fr", "New label", 7);
        var result = service.renamePreferredLabel(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("New label", label.getLexicalValue());
        verify(conceptGroupLabelRepository).save(label);
        verify(conceptGroupRepository).updateModifiedDate("g1", "TH1");
    }

    @Test
    void updateNotation_rejectsDuplicateNotation() {
        var existing = ConceptGroup.builder().idGroup("g2").idThesaurus("TH1").build();
        when(conceptGroupRepository.findByIdGroupAndIdThesaurus("g1", "TH1"))
                .thenReturn(Optional.of(ConceptGroup.builder().idGroup("g1").idThesaurus("TH1").build()));
        when(conceptGroupRepository.findByIdThesaurusAndNotation("TH1", "N1"))
                .thenReturn(List.of(existing));

        var result = service.updateNotation(new UpdateCollectionNotationCommand("TH1", "g1", "N1"));

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        verify(conceptGroupRepository, never()).save(any());
    }

    @Test
    void updateNotation_allowsSameCollectionNotation() {
        var group = ConceptGroup.builder().idGroup("g1").idThesaurus("TH1").notation("").build();
        when(conceptGroupRepository.findByIdGroupAndIdThesaurus("g1", "TH1")).thenReturn(Optional.of(group));
        when(conceptGroupRepository.findByIdThesaurusAndNotation("TH1", "N1")).thenReturn(List.of(group));

        var result = service.updateNotation(new UpdateCollectionNotationCommand("TH1", "g1", "N1"));

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("N1", group.getNotation());
        verify(conceptGroupRepository).save(group);
    }

    @Test
    void updateType_updatesTypeCode() {
        var group = ConceptGroup.builder().idGroup("g1").idThesaurus("TH1").idTypeCode("MT").build();
        when(conceptGroupRepository.findByIdGroupAndIdThesaurus("g1", "TH1")).thenReturn(Optional.of(group));

        var result = service.updateType(new UpdateCollectionTypeCommand("TH1", "g1", "CC"));

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("CC", group.getIdTypeCode());
        verify(conceptGroupRepository).save(group);
    }

    @Test
    void addMember_addsAllBranchConcepts() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C1")).thenReturn(List.of("C1", "C2", "C3"));

        var result = service.addMember(new AddMemberToCollectionCommand("TH1", "g1", "C1", true));

        assertEquals(MutationOutcome.OK, result.outcome());
        assertTrue(result.message().contains("branche"));
        var captor = ArgumentCaptor.forClass(ConceptGroupConcept.class);
        verify(conceptGroupConceptRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals("g1", captor.getAllValues().get(0).getIdGroup());
        assertEquals("C3", captor.getAllValues().get(2).getIdConcept());
    }

    @Test
    void removeMember_removesSingleConcept() {
        var result = service.removeMember(new RemoveMemberFromCollectionCommand("TH1", "g1", "C1", false));

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptGroupConceptRepository).deleteByIdGroupAndIdConceptAndIdThesaurus("g1", "C1", "TH1");
    }

    @Test
    void createCollection_rejectsDuplicateNotationAndAssignsIdentifiers() {
        when(conceptGroupRepository.findByIdThesaurusAndNotation("TH1", "N1"))
                .thenReturn(List.of(ConceptGroup.builder().idGroup("g9").build()));

        var duplicate = service.createCollection(new CreateCollectionCommand(
                "TH1", "fr", "Collection", "N1", "MT", 7));
        assertEquals(MutationOutcome.DUPLICATE_LABEL, duplicate.outcome());

        when(conceptGroupRepository.findByIdThesaurusAndNotation("TH1", "N2")).thenReturn(List.of());
        when(conceptGroupRepository.getNextConceptGroupSequence()).thenReturn(42L);

        var created = service.createCollection(new CreateCollectionCommand(
                "TH1", "fr", "Collection", "N2", "MT", 7));

        assertEquals(MutationOutcome.OK, created.outcome());
        assertEquals("g42", created.createdConceptId());
        verify(collectionIdentifierAssignmentService).assignOnCreation("TH1", "g42", "Collection");
    }

    @Test
    void moveCollection_rejectsMoveToDescendant() {
        when(relationGroupRepository.findByIdThesaurusAndIdGroup2AndRelation("TH1", "g1", "sub"))
                .thenReturn(Optional.of(RelationGroup.builder().idGroup1("g0").idGroup2("g1").build()));
        when(relationGroupRepository.findChildGroupIds("TH1", "g1")).thenReturn(List.of("g2"));
        when(relationGroupRepository.findChildGroupIds("TH1", "g2")).thenReturn(List.of());

        var result = service.moveCollection(new MoveCollectionCommand("TH1", "g1", "g2", false));

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(relationGroupRepository, never()).save(any());
    }

    @Test
    void moveCollection_movesToRoot() {
        when(relationGroupRepository.findByIdThesaurusAndIdGroup2AndRelation("TH1", "g1", "sub"))
                .thenReturn(Optional.of(RelationGroup.builder().idGroup1("g0").idGroup2("g1").build()));

        var result = service.moveCollection(new MoveCollectionCommand("TH1", "g1", null, true));

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(relationGroupRepository).deleteByIdGroup1AndIdGroup2AndIdThesaurus("g0", "g1", "TH1");
    }
}
