package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.entites.Status;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import fr.cnrs.opentheso.v2.candidat.model.CandidateProcessOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatLifecyclePersistenceTest {

    @Mock private CandidatStatusRepository candidatStatusRepository;
    @Mock private StatusRepository statusRepository;
    @Mock private ConceptRepository conceptRepository;

    private CandidatLifecyclePersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new CandidatLifecyclePersistence(
                candidatStatusRepository, statusRepository, conceptRepository);
    }

    @Test
    void insertCandidate_returnsOkAndUpdatesStatusWhenCandidateFound() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");
        var candidatStatus = new CandidatStatus();
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.of(candidatStatus));
        when(statusRepository.findById(2)).thenReturn(Optional.of(new Status()));

        CandidateProcessOutcome outcome = persistence.insertCandidate(candidat, "Bravo", 7);

        assertTrue(outcome.success());
        assertFalse(outcome.isFailure());
        assertEquals("Bravo", candidatStatus.getMessage());
        assertEquals(7, candidatStatus.getIdUserAdmin());
        verify(candidatStatusRepository).save(candidatStatus);
        verify(conceptRepository).setStatus("D", "C1", "TH1");
    }

    @Test
    void insertCandidate_returnsFailWhenCandidateNotFound() {
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.empty());

        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");

        assertTrue(persistence.insertCandidate(candidat, "msg", 7).isFailure());
        verify(conceptRepository, never()).setStatus(any(), any(), any());
    }

    @Test
    void rejectCandidate_returnsOkWhenUpdated() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");
        var candidatStatus = new CandidatStatus();
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.of(candidatStatus));
        when(statusRepository.findById(3)).thenReturn(Optional.of(new Status()));

        assertTrue(persistence.rejectCandidate(candidat, "Nope", 7).success());
        assertEquals("Nope", candidatStatus.getMessage());
        verify(candidatStatusRepository).save(candidatStatus);
    }

    @Test
    void updateCandidateStatus_updatesWhenFoundByConceptAndThesaurus() {
        var candidatStatus = new CandidatStatus();
        candidatStatus.setMessage("motif");
        candidatStatus.setIdUserAdmin(9);
        when(candidatStatusRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Optional.of(candidatStatus));
        when(statusRepository.findById(1)).thenReturn(Optional.of(new Status()));

        assertTrue(persistence.updateCandidateStatus("TH1", "C1", 1));
        assertEquals(null, candidatStatus.getMessage());
        assertEquals(null, candidatStatus.getIdUserAdmin());
        verify(candidatStatusRepository).save(candidatStatus);
        verify(candidatStatusRepository, never()).findByIdConcept(any());
    }

    @Test
    void updateCandidateStatus_fallsBackToFindByConceptWhenThesaurusMismatch() {
        var candidatStatus = new CandidatStatus();
        when(candidatStatusRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Optional.empty());
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.of(candidatStatus));
        when(statusRepository.findById(1)).thenReturn(Optional.of(new Status()));

        assertTrue(persistence.updateCandidateStatus("TH1", "C1", 1));
        verify(candidatStatusRepository).save(candidatStatus);
    }

    @Test
    void updateCandidateStatus_returnsFalseWhenMissing() {
        when(candidatStatusRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Optional.empty());
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.empty());

        assertFalse(persistence.updateCandidateStatus("TH1", "C1", 1));
        verify(candidatStatusRepository, never()).save(any());
    }
}
