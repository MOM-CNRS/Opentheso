package fr.cnrs.opentheso.v2.proposition.service;

import fr.cnrs.opentheso.entites.PropositionModification;
import fr.cnrs.opentheso.models.PropositionProjection;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.services.MailService;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class PropositionMutationServiceTest {

    @Mock
    private PropositionModificationRepository repository;
    @Mock
    private PropositionModificationDetailRepository detailRepository;
    @Mock
    private MailService mailService;

    private PropositionMutationService service;

    @BeforeEach
    void setUp() {
        service = new PropositionMutationService(repository, detailRepository, mailService);
    }

    private PropositionSubmission submission(String comment, String email) {
        return new PropositionSubmission("TH1", "Test", "C1", "Concept 1", "fr", "Author", email, comment);
    }

    @Test
    void submit_savesNewProposition() {
        when(repository.findPendingByConcept("C1", "TH1", "fr")).thenReturn(null);

        boolean saved = service.submit(submission("Please add a synonym", "a@b.fr"));

        assertTrue(saved);
        ArgumentCaptor<PropositionModification> captor = ArgumentCaptor.forClass(PropositionModification.class);
        verify(repository).save(captor.capture());
        assertEquals("ENVOYER", captor.getValue().getStatus());
        assertEquals("C1", captor.getValue().getIdConcept());
    }

    @Test
    void submit_rejectsBlankComment() {
        assertFalse(service.submit(submission("  ", "a@b.fr")));
        verify(repository, never()).save(any());
    }

    @Test
    void submit_rejectsDuplicateForSameAuthor() {
        PropositionProjection existing = org.mockito.Mockito.mock(PropositionProjection.class);
        when(existing.getEmail()).thenReturn("a@b.fr");
        when(repository.findPendingByConcept("C1", "TH1", "fr")).thenReturn(existing);

        assertFalse(service.submit(submission("Another one", "a@b.fr")));
        verify(repository, never()).save(any());
    }

    @Test
    void markRead_updatesEnvoyerToLu() {
        PropositionModification proposition = new PropositionModification();
        proposition.setStatus("ENVOYER");
        when(repository.findById(1)).thenReturn(Optional.of(proposition));

        service.markRead(1);

        assertEquals("LU", proposition.getStatus());
        verify(repository).save(proposition);
    }

    @Test
    void markRead_keepsAlreadyReviewedStatus() {
        PropositionModification proposition = new PropositionModification();
        proposition.setStatus("APPROUVER");
        when(repository.findById(1)).thenReturn(Optional.of(proposition));

        service.markRead(1);

        assertEquals("APPROUVER", proposition.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void approve_setsStatusAndReviewer() {
        PropositionModification proposition = new PropositionModification();
        proposition.setStatus("ENVOYER");
        proposition.setEmail("");
        when(repository.findById(2)).thenReturn(Optional.of(proposition));

        service.approve(2, "admin", "great idea", "Concept 1", "Test");

        assertEquals("APPROUVER", proposition.getStatus());
        assertEquals("admin", proposition.getApprouvePar());
        assertEquals("great idea", proposition.getAdminComment());
    }

    @Test
    void refuse_setsStatusAndReviewer() {
        PropositionModification proposition = new PropositionModification();
        proposition.setStatus("ENVOYER");
        proposition.setEmail("");
        when(repository.findById(3)).thenReturn(Optional.of(proposition));

        service.refuse(3, "admin", "not relevant", "Concept 1", "Test");

        assertEquals("REFUSER", proposition.getStatus());
        assertEquals("not relevant", proposition.getAdminComment());
    }

    @Test
    void delete_removesDetailAndProposition() {
        service.delete(7);

        verify(detailRepository).deleteAllByIdProposition(7);
        verify(repository).deleteById(7);
    }
}
