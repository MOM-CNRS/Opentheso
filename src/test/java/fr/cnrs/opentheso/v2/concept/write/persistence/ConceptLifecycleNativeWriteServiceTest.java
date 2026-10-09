package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddReplacedByCommand;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatLifecyclePersistence;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApproveConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ChangeConceptsStatusCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeprecateConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptLifecycleNativeWriteServiceTest {

    @Mock
    private ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    @Mock
    private ConceptRenameWriteRepository conceptRenameWriteRepository;
    @Mock
    private ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    @Mock
    private ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    @Mock
    private ConceptRelationWriteRepository conceptRelationWriteRepository;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    @Mock
    private CandidatLifecyclePersistence candidatLifecyclePersistence;

    @InjectMocks
    private ConceptLifecycleNativeWriteService service;

    @Test
    void renamePreferredLabel_rejectsBlankLabel() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 7, "admin", "  ", "", false);

        var result = service.renamePreferredLabel(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptLexicalWriteRepository, never()).findPreferredTermId(anyString(), anyString());
    }

    @Test
    void renamePreferredLabel_updatesExistingTranslation() {
        var command = new RenamePreferredLabelCommand("TH1", "C1", "fr", 7, "admin", "New label", "", false);
        when(conceptRenameWriteRepository.findTermByLexicalValue("New label", "fr", "TH1"))
                .thenReturn(Optional.empty());
        when(conceptLexicalWriteRepository.findPreferredTermId("TH1", "C1")).thenReturn(Optional.of("T1"));
        when(conceptRenameWriteRepository.existsTermInLang("T1", "TH1", "fr")).thenReturn(true);

        var result = service.renamePreferredLabel(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptTranslationWriteRepository).updateTranslation(
                "T1", "TH1", "fr", "New label", 7);
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
    }

    @Test
    void deprecateConcept_updatesStatusAndRecordsHistory() {
        var command = new DeprecateConceptCommand("TH1", "C1", 7, "admin");
        var snapshot = new ConceptSnapshot("C1", "TH1", "", "DEP", "", false);
        when(conceptLifecycleWriteRepository.updateConceptStatus("TH1", "C1", "DEP")).thenReturn(true);
        when(conceptLifecycleWriteRepository.loadConceptSnapshot("TH1", "C1")).thenReturn(Optional.of(snapshot));

        var result = service.deprecateConcept(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptLifecycleWriteRepository).insertConceptHistory(snapshot, 7);
        verify(conceptWritePostMutationRepository).saveContributorDcTerm("TH1", "C1", "admin");
    }

    @Test
    void changeConceptsStatus_approvesCandidatesAndSkipsValidated() {
        var command = new ChangeConceptsStatusCommand("TH1", List.of("C1", "C2"), "approve", 7, "admin");
        when(conceptLifecycleWriteRepository.loadConceptSnapshot("TH1", "C1"))
                .thenReturn(Optional.of(new ConceptSnapshot("C1", "TH1", "", "CA", "", false)))
                .thenReturn(Optional.of(new ConceptSnapshot("C1", "TH1", "", "D", "", false)));
        when(conceptLifecycleWriteRepository.loadConceptSnapshot("TH1", "C2"))
                .thenReturn(Optional.of(new ConceptSnapshot("C2", "TH1", "", "D", "", false)));
        when(conceptLifecycleWriteRepository.updateConceptStatus("TH1", "C1", "D")).thenReturn(true);

        var result = service.changeConceptsStatus(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("1 candidat validé", result.message());
        verify(candidatLifecyclePersistence).updateCandidateStatus("TH1", "C1", CandidatStatusCode.ACCEPTED);
        verify(conceptLifecycleWriteRepository, never()).updateConceptStatus("TH1", "C2", "D");
    }

    @Test
    void changeConceptsStatus_deprecatesActiveAndSkipsCandidates() {
        var command = new ChangeConceptsStatusCommand("TH1", List.of("C1", "C2"), "deprecate", 7, "admin");
        when(conceptLifecycleWriteRepository.loadConceptSnapshot("TH1", "C1"))
                .thenReturn(Optional.of(new ConceptSnapshot("C1", "TH1", "", "D", "", false)))
                .thenReturn(Optional.of(new ConceptSnapshot("C1", "TH1", "", "DEP", "", false)));
        when(conceptLifecycleWriteRepository.loadConceptSnapshot("TH1", "C2"))
                .thenReturn(Optional.of(new ConceptSnapshot("C2", "TH1", "", "CA", "", false)));
        when(conceptLifecycleWriteRepository.updateConceptStatus("TH1", "C1", "DEP")).thenReturn(true);

        var result = service.changeConceptsStatus(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("1 concept rendu obsolète", result.message());
        verify(conceptLifecycleWriteRepository, never()).updateConceptStatus("TH1", "C2", "DEP");
    }

    @Test
    void changeConceptsStatus_rejectsWhenNothingApplies() {
        var command = new ChangeConceptsStatusCommand("TH1", List.of("C1"), "approve", 7, "admin");
        when(conceptLifecycleWriteRepository.loadConceptSnapshot("TH1", "C1"))
                .thenReturn(Optional.of(new ConceptSnapshot("C1", "TH1", "", "D", "", false)));

        var result = service.changeConceptsStatus(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        assertEquals("Aucun candidat à valider dans la sélection.", result.message());
    }

    @Test
    void approveConcept_createsRtRelationsBeforeDeletingReplacedByLinks() {
        var command = new ApproveConceptCommand("TH1", "C1", "fr", 7, "admin", true);
        var snapshot = new ConceptSnapshot("C1", "TH1", "", "D", "", false);
        when(conceptLifecycleWriteRepository.listReplacementConceptIds("C1", "TH1"))
                .thenReturn(List.of("C2", "C3"));
        when(conceptLifecycleWriteRepository.updateConceptStatus("TH1", "C1", "D")).thenReturn(true);
        when(conceptLifecycleWriteRepository.loadConceptSnapshot("TH1", "C1")).thenReturn(Optional.of(snapshot));

        var result = service.approveConcept(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptRelationWriteRepository).addRelatedRelation("C1", "C2", "TH1", 7);
        verify(conceptRelationWriteRepository).addRelatedRelation("C1", "C3", "TH1", 7);
        verify(conceptLifecycleWriteRepository).deleteAllReplacedByForConcept("C1", "TH1");
    }

    @Test
    void addReplacedBy_rejectsMissingTarget() {
        var command = new AddReplacedByCommand("TH1", "C1", "  ", 7, "admin");

        var result = service.addReplacedBy(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptLifecycleWriteRepository, never()).insertReplacedBy(
                anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void deleteReplacedBy_deletesLinkAndTouchesConcept() {
        var command = new DeleteReplacedByCommand("TH1", "C1", "C2", 7, "admin");

        var result = service.deleteReplacedBy(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptLifecycleWriteRepository).deleteReplacedBy("C1", "C2", "TH1");
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
    }
}
