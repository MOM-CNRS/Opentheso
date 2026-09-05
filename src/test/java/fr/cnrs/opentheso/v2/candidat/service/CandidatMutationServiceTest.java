package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.NoteType;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.TraductionDto;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatMutationPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatMutationServiceTest {

    @Mock
    private CandidatMutationPersistence candidatMutationPersistence;
    @Mock
    private ConceptDcTermRepository conceptDcTermRepository;

    private CandidatMutationService service;

    @BeforeEach
    void setUp() {
        service = new CandidatMutationService(candidatMutationPersistence, conceptDcTermRepository);
    }

    @Test
    void deleteConcept_delegatesToPersistence() {
        when(candidatMutationPersistence.deleteConcept("C1", "TH1")).thenReturn(true);

        assertTrue(service.deleteConcept("C1", "TH1"));
    }

    @Test
    void saveContributorMetadata_persistsCreatorMetadata() {
        service.saveContributorMetadata("C1", "TH1", "admin");

        ArgumentCaptor<ConceptDcTerm> captor = ArgumentCaptor.forClass(ConceptDcTerm.class);
        verify(conceptDcTermRepository).save(captor.capture());
        assertEquals(DCMIResource.CREATOR, captor.getValue().getName());
        assertEquals("admin", captor.getValue().getValue());
        assertEquals("C1", captor.getValue().getIdConcept());
    }

    @Test
    void resolveUserName_delegatesToPersistence() {
        when(candidatMutationPersistence.resolveUserName(7)).thenReturn("admin");

        assertEquals("admin", service.resolveUserName(7));
    }

    @Test
    void loadNoteTypes_delegatesToPersistence() {
        var noteType = new NoteType();
        when(candidatMutationPersistence.loadNoteTypes()).thenReturn(List.of(noteType));

        assertEquals(List.of(noteType), service.loadNoteTypes());
    }

    @Test
    void addOrUpdateCandidateNote_delegatesToPersistence() {
        service.addOrUpdateCandidateNote("C1", "fr", "TH1", "note", "note", "src", 7);

        verify(candidatMutationPersistence).addOrUpdateCandidateNote("C1", "fr", "TH1", "note", "note", "src", 7);
    }

    @Test
    void updateCandidateNote_delegatesToPersistence() {
        when(candidatMutationPersistence.updateCandidateNote(1, "C1", "fr", "TH1", "note", "src", "note", 7))
                .thenReturn(true);

        assertTrue(service.updateCandidateNote(1, "C1", "fr", "TH1", "note", "src", "note", 7));
    }

    @Test
    void deleteCandidateNote_delegatesToPersistence() {
        service.deleteCandidateNote(1, "C1", "fr", "TH1", "note", "old", 7);

        verify(candidatMutationPersistence).deleteCandidateNote(1, "C1", "fr", "TH1", "note", "old", 7);
    }

    @Test
    void isLabelExistIgnoreCase_delegatesToPersistence() {
        when(candidatMutationPersistence.isLabelExistIgnoreCase("Label", "TH1", "fr")).thenReturn(true);

        assertTrue(service.isLabelExistIgnoreCase("Label", "TH1", "fr"));
    }

    @Test
    void deleteCandidateTranslation_delegatesToPersistence() {
        service.deleteCandidateTranslation("TH1", "T1", "en");

        verify(candidatMutationPersistence).deleteCandidateTranslation("TH1", "T1", "en");
    }

    @Test
    void addCandidateTranslation_delegatesToPersistence() {
        var term = new Term();
        service.addCandidateTranslation(term, 7);

        verify(candidatMutationPersistence).addCandidateTranslation(term, 7);
    }

    @Test
    void loadCandidateTranslations_delegatesToPersistence() {
        var translation = TraductionDto.builder().langue("en").traduction("Value").build();
        when(candidatMutationPersistence.loadCandidateTranslations("C1", "TH1", "fr")).thenReturn(List.of(translation));

        assertEquals(List.of(translation), service.loadCandidateTranslations("C1", "TH1", "fr"));
    }

    @Test
    void loadExternalImages_delegatesToPersistence() {
        var image = new NodeImage(1, "C1", "TH1", "img", "creator", "copy", "http://x", "");
        when(candidatMutationPersistence.loadExternalImages("TH1", "C1")).thenReturn(List.of(image));

        assertEquals(List.of(image), service.loadExternalImages("TH1", "C1"));
    }

    @Test
    void addExternalImage_delegatesToPersistence() {
        service.addExternalImage("C1", "TH1", "name", "copy", "http://x", "creator", 7);

        verify(candidatMutationPersistence).addExternalImage("C1", "TH1", "name", "copy", "http://x", "creator", 7);
    }

    @Test
    void deleteExternalImage_delegatesToPersistence() {
        service.deleteExternalImage("TH1", "C1", "http://x");

        verify(candidatMutationPersistence).deleteExternalImage("TH1", "C1", "http://x");
    }

    @Test
    void sendDiscussionMessage_delegatesToPersistence() {
        service.sendDiscussionMessage("C1", "TH1", "Hello", 7);

        verify(candidatMutationPersistence).sendDiscussionMessage("C1", "TH1", "Hello", 7);
    }

    @Test
    void loadDiscussionParticipants_delegatesToPersistence() {
        var participant = NodeUser.builder().idUser(7).name("alice").build();
        when(candidatMutationPersistence.loadDiscussionParticipants("C1", "TH1")).thenReturn(List.of(participant));

        assertEquals(List.of(participant), service.loadDiscussionParticipants("C1", "TH1"));
    }

    @Test
    void loadDiscussionMessages_delegatesToPersistence() {
        var message = MessageDto.builder().idUser(7).msg("Hello").build();
        when(candidatMutationPersistence.loadDiscussionMessages("C1", "TH1", 7)).thenReturn(List.of(message));

        assertEquals(List.of(message), service.loadDiscussionMessages("C1", "TH1", 7));
    }

    @Test
    void notifyDiscussionParticipants_delegatesToPersistence() {
        service.notifyDiscussionParticipants("C1", "TH1", "Concept 1");

        verify(candidatMutationPersistence).notifyDiscussionParticipants("C1", "TH1", "Concept 1");
    }

    @Test
    void sendMailInvitation_delegatesToPersistence() {
        service.sendMailInvitation("test@example.com");

        verify(candidatMutationPersistence).sendMailInvitation("test@example.com");
    }

    @Test
    void remainingDelegates_forwardToPersistence() throws Exception {
        when(candidatMutationPersistence.deleteAlignment(3, "TH1")).thenReturn(true);
        when(candidatMutationPersistence.updateCandidateStatus("TH1", "C1", 2)).thenReturn(true);
        when(candidatMutationPersistence.hasVote("TH1", "C1", 7, "n1", fr.cnrs.opentheso.models.candidats.enumeration.VoteType.CANDIDAT)).thenReturn(true);
        when(candidatMutationPersistence.termExists("T1", "TH1", "fr")).thenReturn(true);
        when(candidatMutationPersistence.loadUsedLanguages("TH1", "fr")).thenReturn(List.of());
        when(candidatMutationPersistence.loadAlignments("C1", "TH1")).thenReturn(List.of());
        when(candidatMutationPersistence.searchCollections("TH1", "fr", "q")).thenReturn(List.of());
        when(candidatMutationPersistence.searchRelationTerms("q", "fr", "TH1")).thenReturn(List.of());
        when(candidatMutationPersistence.loadCandidateNotes("C1", "TH1")).thenReturn(List.of());
        when(candidatMutationPersistence.loadBroaderRelations("C1", "TH1", "fr")).thenReturn(List.of());
        when(candidatMutationPersistence.loadRelatedTerms("C1", "TH1", "fr")).thenReturn(List.of());
        when(candidatMutationPersistence.migrateOldCandidates("TH1", 7)).thenReturn("ok");

        assertTrue(service.deleteAlignment(3, "TH1"));
        assertTrue(service.updateCandidateStatus("TH1", "C1", 2));
        assertTrue(service.hasVote("TH1", "C1", 7, "n1", fr.cnrs.opentheso.models.candidats.enumeration.VoteType.CANDIDAT));
        assertTrue(service.termExists("T1", "TH1", "fr"));
        assertEquals("ok", service.migrateOldCandidates("TH1", 7));
        assertTrue(service.loadUsedLanguages("TH1", "fr").isEmpty());
        assertTrue(service.loadAlignments("C1", "TH1").isEmpty());
        assertTrue(service.searchCollections("TH1", "fr", "q").isEmpty());
        assertTrue(service.searchRelationTerms("q", "fr", "TH1").isEmpty());
        assertTrue(service.loadCandidateNotes("C1", "TH1").isEmpty());
        assertTrue(service.loadBroaderRelations("C1", "TH1", "fr").isEmpty());
        assertTrue(service.loadRelatedTerms("C1", "TH1", "fr").isEmpty());

        service.updateCandidateDetails(fr.cnrs.opentheso.models.candidats.CandidatDto.builder().build());
        service.updateCandidateLabel("L", "TH1", "fr", "T1");
        service.removeVote("TH1", "C1", 7, "n1", fr.cnrs.opentheso.models.candidats.enumeration.VoteType.CANDIDAT);
        service.addVote("TH1", "C1", 7, "n1", fr.cnrs.opentheso.models.candidats.enumeration.VoteType.CANDIDAT);
        service.addCollection("G1", "TH1", "C1");
        service.removeCollection("G1", "C1", "TH1");
        service.updateTermLabel("L", "TH1", "fr", "T1");
        service.addTerm(new Term());
        service.addSynonym("syn", "TH1", "fr", "T1");
        service.deleteSynonym("T1", "TH1", "fr", "syn");
        service.addBroaderRelation("C1", "TH1", "P1");
        service.deleteBroaderRelation("C1", "TH1", "P1", 7);
        service.addRelatedTerm("C1", "TH1", "R1");
        service.deleteRelatedTerm("C1", "TH1", "R1", 7);
        service.updateAlignment(null, "C1", "TH1");

        verify(candidatMutationPersistence).updateCandidateDetails(any());
        verify(candidatMutationPersistence).addCollection("G1", "TH1", "C1");
    }
}
