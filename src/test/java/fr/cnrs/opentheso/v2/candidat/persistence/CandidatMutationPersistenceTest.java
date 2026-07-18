package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatMessages;
import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.entites.CandidatVote;
import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.NoteType;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.Status;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.CandidatMessageProjection;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.CandidatMessageRepository;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import fr.cnrs.opentheso.repositories.ConceptCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptTermCandidatRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.NoteHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.NoteTypeRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.PropositionRepository;
import fr.cnrs.opentheso.repositories.SearchRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import fr.cnrs.opentheso.repositories.TermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptDeletionWriteRepository;
import fr.cnrs.opentheso.v2.shared.mail.SystemMailSender;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatMutationPersistenceTest {

    @Mock private ConceptDeletionWriteRepository conceptDeletionWriteRepository;
    @Mock private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock private ConceptRepository conceptRepository;
    @Mock private ConceptHistoriqueRepository conceptHistoriqueRepository;
    @Mock private PreferencesRepository preferencesRepository;
    @Mock private CandidatStatusRepository candidatStatusRepository;
    @Mock private StatusRepository statusRepository;
    @Mock private CandidatVoteRepository candidatVoteRepository;
    @Mock private TermRepository termRepository;
    @Mock private TermHistoriqueRepository termHistoriqueRepository;
    @Mock private PreferredTermRepository preferredTermRepository;
    @Mock private NonPreferredTermRepository nonPreferredTermRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private AlignementRepository alignementRepository;
    @Mock private AlignementTypeRepository alignementTypeRepository;
    @Mock private ConceptGroupConceptRepository conceptGroupConceptRepository;
    @Mock private ConceptGroupLabelRepository conceptGroupLabelRepository;
    @Mock private HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    @Mock private SearchRepository searchRepository;
    @Mock private UserRepository userRepository;
    @Mock private CandidatMessageRepository candidatMessageRepository;
    @Mock private ConceptCandidatRepository conceptCandidatRepository;
    @Mock private ConceptTermCandidatRepository conceptTermCandidatRepository;
    @Mock private PropositionRepository propositionRepository;
    @Mock private CandidatReadPersistence candidatReadPersistence;
    @Mock private NoteTypeRepository noteTypeRepository;
    @Mock private NoteHistoriqueRepository noteHistoriqueRepository;
    @Mock private ImagesRepository imagesRepository;
    @Mock private SystemMailSender systemMailSender;

    @InjectMocks
    private CandidatMutationPersistence persistence;

    @Test
    void loadNoteTypes_sortsDefinitionAndScopeNoteFirst() {
        var note = newNoteType("note");
        var definition = newNoteType("definition");
        var scopeNote = newNoteType("scopeNote");
        when(noteTypeRepository.findAll()).thenReturn(List.of(note, scopeNote, definition));

        var result = persistence.loadNoteTypes().stream().map(NoteType::getCode).toList();

        assertEquals(List.of("definition", "scopeNote", "note"), result);
    }

    private NoteType newNoteType(String code) {
        var noteType = new NoteType();
        noteType.setCode(code);
        return noteType;
    }

    @Test
    void addOrUpdateCandidateNote_createsNewNoteWhenNoneExistsForLang() {
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang("C1", "TH1", "note", "fr"))
                .thenReturn(List.of());

        persistence.addOrUpdateCandidateNote("C1", "fr", "TH1", "Contenu", "note", "", 7);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertEquals("C1", captor.getValue().getIdentifier());
        assertEquals("fr", captor.getValue().getLang());
        assertEquals("note", captor.getValue().getNoteTypeCode());
        verify(noteHistoriqueRepository).save(any());
    }

    @Test
    void addOrUpdateCandidateNote_updatesExistingNoteForLang() {
        var existing = Note.builder().id(5).lexicalValue("Old").build();
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang("C1", "TH1", "note", "fr"))
                .thenReturn(List.of(existing));

        persistence.addOrUpdateCandidateNote("C1", "fr", "TH1", "New", "note", "", 7);

        verify(noteRepository).save(existing);
        assertEquals("New", existing.getLexicalValue());
    }

    @Test
    void updateCandidateNote_returnsFalseWhenNoteNotFound() {
        when(noteRepository.findByIdAndIdThesaurus(1, "TH1")).thenReturn(Optional.empty());

        assertFalse(persistence.updateCandidateNote(1, "C1", "fr", "TH1", "text", "src", "note", 7));
        verify(noteRepository, never()).save(any());
    }

    @Test
    void updateCandidateNote_updatesExistingNote() {
        var note = Note.builder().id(1).lexicalValue("Old").build();
        when(noteRepository.findByIdAndIdThesaurus(1, "TH1")).thenReturn(Optional.of(note));

        assertTrue(persistence.updateCandidateNote(1, "C1", "fr", "TH1", "New", "src", "note", 7));
        assertEquals("New", note.getLexicalValue());
        verify(noteRepository).save(note);
        verify(noteHistoriqueRepository).save(any());
    }

    @Test
    void deleteCandidateNote_deletesNoteAndVotes() {
        persistence.deleteCandidateNote(9, "C1", "fr", "TH1", "note", "old value", 7);

        verify(noteRepository).deleteByIdAndIdThesaurus(9, "TH1");
        verify(candidatVoteRepository).deleteAllByIdThesaurusAndIdConceptAndIdNote("TH1", "C1", "9");
        verify(noteHistoriqueRepository).save(any());
    }

    @Test
    void isLabelExistIgnoreCase_delegatesToTermRepository() {
        when(termRepository.existsTermIgnoreCase("Label", "TH1", "fr")).thenReturn(true);

        assertTrue(persistence.isLabelExistIgnoreCase("Label", "TH1", "fr"));
    }

    @Test
    void deleteCandidateTranslation_delegatesToTermRepository() {
        persistence.deleteCandidateTranslation("TH1", "T1", "en");

        verify(termRepository).deleteByIdTermAndLangAndIdThesaurus("T1", "en", "TH1");
    }

    @Test
    void addCandidateTranslation_savesTermAndHistorique() {
        var term = Term.builder()
                .idTerm("T1").idThesaurus("TH1").lang("en").lexicalValue("Value")
                .source("Candidat").status("D").build();

        persistence.addCandidateTranslation(term, 7);

        verify(termRepository).save(any(fr.cnrs.opentheso.entites.Term.class));
        verify(termHistoriqueRepository).save(any());
    }

    @Test
    void loadCandidateTranslations_mapsRawRows() {
        when(termRepository.getConceptTranslationsRaw("C1", "TH1", "fr"))
                .thenReturn(List.<Object[]>of(new Object[]{"en", "Value", "GB"}));

        var result = persistence.loadCandidateTranslations("C1", "TH1", "fr");

        assertEquals(1, result.size());
        assertEquals("en", result.get(0).getLangue());
        assertEquals("Value", result.get(0).getTraduction());
        assertEquals("GB", result.get(0).getCodePays());
    }

    @Test
    void loadExternalImages_delegatesToReadPersistence() {
        var images = List.of(new NodeImage(1, "C1", "TH1", "img", "creator", "copy", "http://x", ""));
        when(candidatReadPersistence.loadExternalImages("TH1", "C1")).thenReturn(images);

        assertEquals(images, persistence.loadExternalImages("TH1", "C1"));
    }

    @Test
    void addExternalImage_savesImage() {
        persistence.addExternalImage("C1", "TH1", "name", "copy", "http://x", "creator", 7);

        ArgumentCaptor<ImageExterne> captor = ArgumentCaptor.forClass(ImageExterne.class);
        verify(imagesRepository).save(captor.capture());
        assertEquals("C1", captor.getValue().getIdConcept());
        assertEquals("http://x", captor.getValue().getExternalUri());
    }

    @Test
    void deleteExternalImage_deletesAllWhenUriBlank() {
        persistence.deleteExternalImage("TH1", "C1", "");

        verify(imagesRepository).deleteAllByIdThesaurusAndIdConcept("TH1", "C1");
        verify(imagesRepository, never()).deleteByIdThesaurusAndIdConceptAndExternalUri(any(), any(), any());
    }

    @Test
    void deleteExternalImage_deletesSpecificUriWhenProvided() {
        persistence.deleteExternalImage("TH1", "C1", "http://x");

        verify(imagesRepository).deleteByIdThesaurusAndIdConceptAndExternalUri("TH1", "C1", "http://x");
    }

    @Test
    void sendDiscussionMessage_savesMessage() {
        persistence.sendDiscussionMessage("C1", "TH1", "Hello", 7);

        ArgumentCaptor<CandidatMessages> captor = ArgumentCaptor.forClass(CandidatMessages.class);
        verify(candidatMessageRepository).save(captor.capture());
        assertEquals("Hello", captor.getValue().getValue());
        assertEquals("C1", captor.getValue().getIdConcept());
        assertEquals(7, captor.getValue().getIdUser());
    }

    @Test
    void loadDiscussionParticipants_returnsEmptyWhenNoMessages() {
        when(candidatMessageRepository.findMessagesByConceptAndThesaurus("C1", "TH1")).thenReturn(List.of());

        assertTrue(persistence.loadDiscussionParticipants("C1", "TH1").isEmpty());
    }

    @Test
    void loadDiscussionParticipants_resolvesDistinctUsernames() {
        var projection = mockProjection(7, "alice", "hello", "2024-01-01");
        when(candidatMessageRepository.findMessagesByConceptAndThesaurus("C1", "TH1")).thenReturn(List.of(projection));
        var user = new User();
        user.setUsername("alice");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        var result = persistence.loadDiscussionParticipants("C1", "TH1");

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getIdUser());
        assertEquals("alice", result.get(0).getName());
    }

    @Test
    void loadDiscussionMessages_mapsMineFlagForCurrentUser() {
        var projection = mockProjection(7, "alice", "hello", "2024-01-01");
        when(candidatMessageRepository.findMessagesByConceptAndThesaurus("C1", "TH1")).thenReturn(List.of(projection));

        var result = persistence.loadDiscussionMessages("C1", "TH1", 7);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isMine());
        assertEquals("hello", result.get(0).getMsg());
    }

    @Test
    void notifyDiscussionParticipants_resolvesParticipantsWithoutThrowing() {
        var projection = mockProjection(7, "alice", "hello", "2024-01-01");
        when(candidatMessageRepository.findMessagesByConceptAndThesaurus("C1", "TH1")).thenReturn(List.of(projection));
        var user = new User();
        user.setUsername("alice");
        user.setMail("alice@example.com");
        user.setAlertMail(true);
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> persistence.notifyDiscussionParticipants("C1", "TH1", "Concept 1"));
    }

    @Test
    void sendMailInvitation_enqueuesMailAndReturnsTrue() {
        assertTrue(persistence.sendMailInvitation("test@example.com"));
    }

    @Test
    void deleteConcept_returnsTrueOnSuccess() {
        assertTrue(persistence.deleteConcept("C1", "TH1"));

        verify(conceptDeletionWriteRepository).deleteConcept("TH1", "C1");
    }

    @Test
    void deleteConcept_returnsFalseWhenRepositoryThrows() {
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(conceptDeletionWriteRepository).deleteConcept("TH1", "C1");

        assertFalse(persistence.deleteConcept("C1", "TH1"));
    }

    @Test
    void saveNewCandidat_rejectsExistingPreferredLabel() throws Exception {
        var candidat = new CandidatDto();
        candidat.setNomPref("Existing label");
        when(termRepository.existsPrefLabel("Existing label", "fr", "TH1")).thenReturn(true);

        try (var messageUtils = org.mockito.Mockito.mockStatic(fr.cnrs.opentheso.utils.MessageUtils.class)) {
            assertFalse(persistence.saveNewCandidat(candidat, "TH1", "fr", 7, "admin", "fr", "def"));
        }
        verify(conceptRepository, never()).save(any());
    }

    @Test
    void saveNewCandidat_rejectsExistingSynonym() throws Exception {
        var candidat = new CandidatDto();
        candidat.setNomPref("New label");
        when(termRepository.existsPrefLabel("New label", "fr", "TH1")).thenReturn(false);
        when(nonPreferredTermRepository.isAltLabelExist("New label", "TH1", "fr")).thenReturn(true);

        try (var messageUtils = org.mockito.Mockito.mockStatic(fr.cnrs.opentheso.utils.MessageUtils.class)) {
            assertFalse(persistence.saveNewCandidat(candidat, "TH1", "fr", 7, "admin", "fr", "def"));
        }
        verify(conceptRepository, never()).save(any());
    }

    @Test
    void saveNewCandidat_createsConceptTermAndDefinitionNote() throws Exception {
        var candidat = new CandidatDto();
        candidat.setNomPref("New label");
        when(termRepository.existsPrefLabel("New label", "fr", "TH1")).thenReturn(false);
        when(nonPreferredTermRepository.isAltLabelExist("New label", "TH1", "fr")).thenReturn(false);
        when(preferencesRepository.findByIdThesaurus("TH1")).thenReturn(Optional.empty());
        when(conceptRepository.getNextConceptNumericId()).thenReturn(100L);
        when(conceptRepository.findByIdConcept(any())).thenReturn(List.of());
        when(statusRepository.findById(1)).thenReturn(Optional.empty());
        when(termRepository.getMaxInternalId()).thenReturn(5);
        when(termRepository.findByIdTermAndIdThesaurus(any(), any())).thenReturn(Optional.empty());
        when(termRepository.save(any(fr.cnrs.opentheso.entites.Term.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(persistence.saveNewCandidat(candidat, "TH1", "fr", 7, "admin", "fr", "Une définition"));

        verify(conceptRepository).save(any(Concept.class));
        verify(termRepository, org.mockito.Mockito.atLeastOnce()).save(any(fr.cnrs.opentheso.entites.Term.class));
        verify(preferredTermRepository).save(any());
        ArgumentCaptor<fr.cnrs.opentheso.entites.Note> noteCaptor = ArgumentCaptor.forClass(fr.cnrs.opentheso.entites.Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        assertEquals("definition", noteCaptor.getValue().getNoteTypeCode());
        assertEquals("Une définition", noteCaptor.getValue().getLexicalValue());
    }

    @Test
    void updateCandidateDetails_rebuildsCollectionsRelationsAndSynonyms() {
        var candidat = new CandidatDto();
        candidat.setIdThesaurus("TH1");
        candidat.setIdConcepte("C1");
        candidat.setIdTerm("T1");
        candidat.setLang("fr");
        candidat.setCollections(List.of(fr.cnrs.opentheso.models.nodes.NodeIdValue.builder().id("G1").value("Group 1").build()));
        candidat.setTermesGenerique(List.of(fr.cnrs.opentheso.models.nodes.NodeIdValue.builder().id("BT1").build()));
        candidat.setTermesAssocies(List.of(fr.cnrs.opentheso.models.nodes.NodeIdValue.builder().id("RT1").build()));
        candidat.setEmployePourList(List.of("Synonym 1"));

        persistence.updateCandidateDetails(candidat);

        verify(conceptGroupConceptRepository).deleteAllByIdThesaurusAndIdConcept("TH1", "C1");
        ArgumentCaptor<fr.cnrs.opentheso.entites.ConceptGroupConcept> groupCaptor =
                ArgumentCaptor.forClass(fr.cnrs.opentheso.entites.ConceptGroupConcept.class);
        verify(conceptGroupConceptRepository).save(groupCaptor.capture());
        assertEquals("G1", groupCaptor.getValue().getIdGroup());

        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept1("TH1", "C1");
        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept2("TH1", "C1");
        ArgumentCaptor<HierarchicalRelationship> relationCaptor = ArgumentCaptor.forClass(HierarchicalRelationship.class);
        verify(hierarchicalRelationshipRepository, org.mockito.Mockito.times(2)).save(relationCaptor.capture());
        assertTrue(relationCaptor.getAllValues().stream().anyMatch(r -> "BT".equals(r.getRole())));
        assertTrue(relationCaptor.getAllValues().stream().anyMatch(r -> "RT".equals(r.getRole())));

        verify(nonPreferredTermRepository).deleteByIdThesaurusAndIdTermAndLang("TH1", "T1", "fr");
        ArgumentCaptor<NonPreferredTerm> synonymCaptor = ArgumentCaptor.forClass(NonPreferredTerm.class);
        verify(nonPreferredTermRepository).save(synonymCaptor.capture());
        assertEquals("Synonym 1", synonymCaptor.getValue().getLexicalValue());
    }

    @Test
    void updateCandidateStatus_returnsFalseWhenCandidateStatusMissing() {
        when(candidatStatusRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1")).thenReturn(Optional.empty());

        assertFalse(persistence.updateCandidateStatus("TH1", "C1", 1));
    }

    @Test
    void updateCandidateStatus_returnsFalseWhenTargetStatusMissing() {
        when(candidatStatusRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Optional.of(new CandidatStatus()));
        when(statusRepository.findById(9)).thenReturn(Optional.empty());

        assertFalse(persistence.updateCandidateStatus("TH1", "C1", 9));
    }

    @Test
    void updateCandidateStatus_updatesStatusWhenFound() {
        var candidatStatus = new CandidatStatus();
        when(candidatStatusRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Optional.of(candidatStatus));
        var newStatus = new Status();
        when(statusRepository.findById(1)).thenReturn(Optional.of(newStatus));

        assertTrue(persistence.updateCandidateStatus("TH1", "C1", 1));
        assertEquals(newStatus, candidatStatus.getStatus());
        verify(candidatStatusRepository).save(candidatStatus);
    }

    @Test
    void hasVote_returnsTrueWhenVoteExists() {
        when(candidatVoteRepository.findAllByIdConceptAndIdThesaurusAndIdUserAndIdNoteAndTypeVote(
                "C1", "TH1", 7, "1", "CA")).thenReturn(List.of(new CandidatVote()));

        assertTrue(persistence.hasVote("TH1", "C1", 7, "1", VoteType.CANDIDAT));
    }

    @Test
    void addVote_savesVote() {
        persistence.addVote("TH1", "C1", 7, "1", VoteType.CANDIDAT);

        ArgumentCaptor<CandidatVote> captor = ArgumentCaptor.forClass(CandidatVote.class);
        verify(candidatVoteRepository).save(captor.capture());
        assertEquals("C1", captor.getValue().getIdConcept());
        assertEquals("CA", captor.getValue().getTypeVote());
    }

    @Test
    void removeVote_delegatesToRepository() {
        persistence.removeVote("TH1", "C1", 7, "1", VoteType.CANDIDAT);

        verify(candidatVoteRepository).deleteAllByIdUserAndIdConceptAndIdThesaurusAndTypeVoteAndIdNote(
                7, "C1", "TH1", "CA", "1");
    }

    @Test
    void searchCollections_mapsRawRows() {
        when(conceptGroupLabelRepository.searchGroups("TH1", "fr", "grp"))
                .thenReturn(List.<Object[]>of(new Object[]{"G1", "Group 1"}));

        var result = persistence.searchCollections("TH1", "fr", "grp");

        assertEquals(1, result.size());
        assertEquals("G1", result.get(0).getId());
        assertEquals("Group 1", result.get(0).getValue());
    }

    @Test
    void addCollection_savesGroupConceptLink() {
        persistence.addCollection("G1", "TH1", "C1");

        verify(conceptGroupConceptRepository).save(any());
    }

    @Test
    void removeCollection_delegatesToRepository() {
        persistence.removeCollection("G1", "C1", "TH1");

        verify(conceptGroupConceptRepository).deleteByIdGroupAndIdConceptAndIdThesaurus("G1", "C1", "TH1");
    }

    @Test
    void searchRelationTerms_mergesPreferredAndAltLabelsWithoutDuplicates() {
        when(searchRepository.searchPreferredLabels(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"C1", "Label 1"}));
        when(searchRepository.searchAltLabels(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"C2", "Label 1"}, new Object[]{"C3", "Label 2"}));

        var result = persistence.searchRelationTerms("query", "fr", "TH1");

        assertEquals(2, result.size());
        assertEquals("C1", result.get(0).getId());
        assertEquals("C3", result.get(1).getId());
    }

    @Test
    void termExists_returnsTrueWhenPresent() {
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "fr"))
                .thenReturn(Optional.of(new fr.cnrs.opentheso.entites.Term()));

        assertTrue(persistence.termExists("T1", "TH1", "fr"));
    }

    @Test
    void updateTermLabel_updatesExistingTerm() {
        var term = new fr.cnrs.opentheso.entites.Term();
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "fr")).thenReturn(Optional.of(term));

        persistence.updateTermLabel("New label", "TH1", "fr", "T1");

        assertEquals("New label", term.getLexicalValue());
        verify(termRepository).save(term);
    }

    @Test
    void addTerm_savesConvertedLexicalValue() {
        var term = Term.builder().idTerm("T1").idThesaurus("TH1").lang("fr").lexicalValue("Value").status("D").build();

        persistence.addTerm(term);

        verify(termRepository).save(any(fr.cnrs.opentheso.entites.Term.class));
    }

    @Test
    void addSynonym_savesNonPreferredTerm() {
        persistence.addSynonym("Syn", "TH1", "fr", "T1");

        ArgumentCaptor<NonPreferredTerm> captor = ArgumentCaptor.forClass(NonPreferredTerm.class);
        verify(nonPreferredTermRepository).save(captor.capture());
        assertEquals("Syn", captor.getValue().getLexicalValue());
        assertEquals("T1", captor.getValue().getIdTerm());
    }

    @Test
    void deleteSynonym_delegatesToRepository() {
        persistence.deleteSynonym("T1", "TH1", "fr", "Syn");

        verify(nonPreferredTermRepository).deleteByIdThesaurusAndIdTermAndLangAndLexicalValue("TH1", "T1", "fr", "Syn");
    }

    @Test
    void loadCandidateNotes_mapsNoteEntities() {
        when(noteRepository.findAllByIdentifierAndIdThesaurus("C1", "TH1"))
                .thenReturn(List.of(Note.builder().id(1).noteTypeCode("note").idConcept("C1").lang("fr")
                        .lexicalValue("Value").idUser(7).build()));

        var result = persistence.loadCandidateNotes("C1", "TH1");

        assertEquals(1, result.size());
        assertEquals("Value", result.get(0).getLexicalValue());
    }

    @Test
    void addBroaderRelation_addsBtAndNtRelations() {
        persistence.addBroaderRelation("C1", "TH1", "C2");

        ArgumentCaptor<HierarchicalRelationship> captor = ArgumentCaptor.forClass(HierarchicalRelationship.class);
        verify(hierarchicalRelationshipRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(r ->
                "BT".equals(r.getRole()) && "C1".equals(r.getIdConcept1()) && "C2".equals(r.getIdConcept2())));
        assertTrue(captor.getAllValues().stream().anyMatch(r ->
                "NT".equals(r.getRole()) && "C2".equals(r.getIdConcept1()) && "C1".equals(r.getIdConcept2())));
    }

    @Test
    void loadBroaderRelations_sortsByLabel() {
        when(hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike("TH1", "C1", "BT"))
                .thenReturn(List.of(
                        HierarchicalRelationship.builder().idConcept1("C1").idConcept2("C3").build(),
                        HierarchicalRelationship.builder().idConcept1("C1").idConcept2("C2").build()));
        when(termRepository.getLexicalValueOfConcept("C3", "TH1", "fr")).thenReturn(Optional.of("Zebra"));
        when(termRepository.getLexicalValueOfConcept("C2", "TH1", "fr")).thenReturn(Optional.of("Apple"));

        var result = persistence.loadBroaderRelations("C1", "TH1", "fr");

        assertEquals(2, result.size());
        assertEquals("Apple", result.get(0).getValue());
        assertEquals("Zebra", result.get(1).getValue());
    }

    @Test
    void deleteBroaderRelation_deletesBtAndNtRelations() {
        persistence.deleteBroaderRelation("C1", "TH1", "C2", 7);

        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole("TH1", "C1", "C2", "BT");
        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole("TH1", "C2", "C1", "NT");
    }

    @Test
    void addRelatedTerm_addsRtRelation() {
        persistence.addRelatedTerm("C1", "TH1", "C2");

        ArgumentCaptor<HierarchicalRelationship> captor = ArgumentCaptor.forClass(HierarchicalRelationship.class);
        verify(hierarchicalRelationshipRepository).save(captor.capture());
        assertEquals("RT", captor.getValue().getRole());
    }

    @Test
    void deleteRelatedTerm_deletesBothDirections() {
        persistence.deleteRelatedTerm("C1", "TH1", "C2", 7);

        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole("TH1", "C1", "C2", "RT");
        verify(hierarchicalRelationshipRepository).deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole("TH1", "C2", "C1", "RT");
    }

    @Test
    void insertCandidate_returnsFalseAndUpdatesStatusWhenCandidateFound() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");
        var candidatStatus = new CandidatStatus();
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.of(candidatStatus));
        when(statusRepository.findById(2)).thenReturn(Optional.of(new Status()));

        assertFalse(persistence.insertCandidate(candidat, "Bravo", 7));

        assertEquals("Bravo", candidatStatus.getMessage());
        assertEquals(7, candidatStatus.getIdUserAdmin());
        verify(candidatStatusRepository).save(candidatStatus);
        verify(conceptRepository).setStatus("D", "C1", "TH1");
    }

    @Test
    void insertCandidate_returnsTrueWhenCandidateNotFound() {
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.empty());

        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");

        assertTrue(persistence.insertCandidate(candidat, "msg", 7));
        verify(conceptRepository, never()).setStatus(any(), any(), any());
    }

    @Test
    void rejectCandidate_updatesStatusToRefused() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setIdThesaurus("TH1");
        var candidatStatus = new CandidatStatus();
        when(candidatStatusRepository.findByIdConcept("C1")).thenReturn(Optional.of(candidatStatus));
        when(statusRepository.findById(3)).thenReturn(Optional.of(new Status()));

        assertFalse(persistence.rejectCandidate(candidat, "Nope", 7));

        assertEquals("Nope", candidatStatus.getMessage());
        verify(candidatStatusRepository).save(candidatStatus);
    }

    @Test
    void generateLocalArkForConcepts_skipsConceptsThatAlreadyHaveAnArk() {
        var preferences = Preferences.builder()
                .sizeIdArkLocal(8).uppercaseForArk(false).naanArkLocal("naan").prefixArkLocal("pfx").build();
        when(conceptRepository.findByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Optional.of(Concept.builder().idConcept("C1").idThesaurus("TH1").idArk("already-set").build()));

        persistence.generateLocalArkForConcepts("TH1", List.of("C1"), preferences);

        verify(conceptRepository, never()).setIdArk(any(), any(), any(), any());
    }

    @Test
    void generateLocalArkForConcepts_generatesArkForConceptsWithoutOne() {
        var preferences = Preferences.builder()
                .sizeIdArkLocal(8).uppercaseForArk(false).naanArkLocal("naan").prefixArkLocal("pfx").build();
        when(conceptRepository.findByIdConceptAndIdThesaurus("C1", "TH1"))
                .thenReturn(Optional.of(Concept.builder().idConcept("C1").idThesaurus("TH1").idArk("").build()));

        persistence.generateLocalArkForConcepts("TH1", List.of("C1"), preferences);

        ArgumentCaptor<String> arkCaptor = ArgumentCaptor.forClass(String.class);
        verify(conceptRepository).setIdArk(arkCaptor.capture(), any(), org.mockito.ArgumentMatchers.eq("C1"), org.mockito.ArgumentMatchers.eq("TH1"));
        assertTrue(arkCaptor.getValue().startsWith("naan/pfx"));
    }

    @Test
    void migrateOldCandidates_returnsMessageWhenNoOldCandidates() {
        when(conceptCandidatRepository.findAllByIdThesaurusAndStatus("TH1", "a")).thenReturn(List.of());

        assertEquals("Pas d'anciens candidats à récupérer", persistence.migrateOldCandidates("TH1", 7));
    }

    private CandidatMessageProjection mockProjection(int idUser, String username, String value, String date) {
        var projection = org.mockito.Mockito.mock(CandidatMessageProjection.class);
        org.mockito.Mockito.lenient().when(projection.getIdUser()).thenReturn(idUser);
        org.mockito.Mockito.lenient().when(projection.getUsername()).thenReturn(username);
        org.mockito.Mockito.lenient().when(projection.getValue()).thenReturn(value);
        org.mockito.Mockito.lenient().when(projection.getDate()).thenReturn(date);
        return projection;
    }
}
