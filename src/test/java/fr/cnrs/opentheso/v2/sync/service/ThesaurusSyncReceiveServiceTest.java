package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.service.ConceptFullReadService;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptOutcome;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncReceiveServiceTest {

    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ConceptFullReadService conceptFullReadService;
    @Mock
    private ThesaurusSyncConceptDiffService conceptDiffService;
    @Mock
    private PropositionMutationService propositionMutationService;
    @Mock
    private PropositionDraftService propositionDraftService;
    @Mock
    private CandidatMutationService candidatMutationService;

    private ThesaurusSyncReceiveService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ThesaurusSyncReceiveService(
                toolboxPreferencePersistence,
                conceptRepository,
                conceptFullReadService,
                conceptDiffService,
                propositionMutationService,
                propositionDraftService,
                candidatMutationService
        );
        user = User.builder().id(7).username("alice").mail("alice@example.com").build();
    }

    @Test
    void receiveBatch_rejectsNonMasterThesaurus() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(false);

        assertThrows(IllegalStateException.class, () ->
                service.receiveBatch("TH_MASTER", emptyRequest(), user));
    }

    @Test
    void receiveBatch_returnsEmptyForEmptyPayload() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);

        SyncBatchResponse response = service.receiveBatch("TH_MASTER", emptyRequest(), user);

        assertEquals(0, response.total());
        verify(conceptRepository, never()).existsByIdConceptAndIdThesaurus(anyString(), anyString());
    }

    @Test
    void receiveBatch_createsPropositionWhenConceptExistsAndDiffers() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C1", "TH_MASTER")).thenReturn(true);

        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        when(conceptFullReadService.loadFullConcept("TH_MASTER", "C1", "fr", 0, true))
                .thenReturn(Optional.of(master));

        PropositionDraft draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "Chat domestique", "Chat", false));
        when(conceptDiffService.diff(any(), eq(master), eq("fr"))).thenReturn(draft);
        when(propositionMutationService.submitDraft(any())).thenReturn(Optional.of(42));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat domestique")
                .build();

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, "bob", "bob@ex.com", "sync", true, List.of(incoming)),
                user
        );

        assertEquals(1, response.propositionsCreated());
        assertEquals(SyncConceptOutcome.PROPOSITION_CREATED, response.results().get(0).outcome());
        assertEquals(42, response.results().get(0).propositionId());
        verify(propositionDraftService).saveDraftDetails(eq(42), eq(draft));
    }

    @Test
    void receiveBatch_skipsWhenNoDiff() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C1", "TH_MASTER")).thenReturn(true);

        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        when(conceptFullReadService.loadFullConcept("TH_MASTER", "C1", "fr", 0, true))
                .thenReturn(Optional.of(master));
        when(conceptDiffService.diff(any(), eq(master), eq("fr"))).thenReturn(new PropositionDraft());

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .build();

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(incoming)),
                user
        );

        assertEquals(1, response.skipped());
        verify(propositionMutationService, never()).submitDraft(any());
    }

    @Test
    void receiveBatch_matchesByArkWhenIdUnknown() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C_NEW", "TH_MASTER")).thenReturn(false);
        when(conceptRepository.findConceptIdByArkIgnoreCase("ark:/123/abc", "TH_MASTER"))
                .thenReturn(Optional.of("C1"));

        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        when(conceptFullReadService.loadFullConcept("TH_MASTER", "C1", "fr", 0, true))
                .thenReturn(Optional.of(master));
        when(conceptDiffService.diff(any(), eq(master), eq("fr"))).thenReturn(new PropositionDraft());

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C_NEW")
                .permanentId("ark:/123/abc")
                .prefLabel("fr", "Chat")
                .build();

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(incoming)),
                user
        );

        assertEquals(1, response.skipped());
        assertEquals("C1", response.results().get(0).matchedConceptId());
    }

    @Test
    void receiveBatch_createsCandidateWhenConceptMissing() throws Exception {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C99", "TH_MASTER")).thenReturn(false);
        when(candidatMutationService.saveNewCandidat(any(), eq("TH_MASTER"), eq("fr"), eq(7), eq("alice"), eq("fr"), eq("Def")))
                .thenAnswer(invocation -> {
                    invocation.getArgument(0, fr.cnrs.opentheso.models.candidats.CandidatDto.class)
                            .setIdConcepte("CA99");
                    return true;
                });

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C99")
                .prefLabel("fr", "Nouveau")
                .definition("fr", "Def")
                .build();

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(incoming)),
                user
        );

        assertEquals(1, response.candidatesCreated());
        assertEquals(SyncConceptOutcome.CANDIDATE_CREATED, response.results().get(0).outcome());
        assertEquals("CA99", response.results().get(0).matchedConceptId());
    }

    @Test
    void receiveBatch_skipsUnknownConceptWhenCreateCandidatesDisabled() throws Exception {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C99", "TH_MASTER")).thenReturn(false);

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C99")
                .prefLabel("fr", "Nouveau")
                .build();

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, false, List.of(incoming)),
                user
        );

        assertEquals(1, response.skipped());
        assertEquals(SyncConceptOutcome.SKIPPED, response.results().get(0).outcome());
        verify(candidatMutationService, never()).saveNewCandidat(
                any(), anyString(), anyString(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    void receiveBatch_errorsWhenCandidateHasNoPrefLabel() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C99", "TH_MASTER")).thenReturn(false);

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C99")
                .build();

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(incoming)),
                user
        );

        assertEquals(1, response.errors());
        assertEquals(SyncConceptOutcome.ERROR, response.results().get(0).outcome());
    }

    @Test
    void receiveBatch_submitsPropositionWithAllowMultiplePendingTrue() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C1", "TH_MASTER")).thenReturn(true);

        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        when(conceptFullReadService.loadFullConcept("TH_MASTER", "C1", "fr", 0, true))
                .thenReturn(Optional.of(master));

        PropositionDraft draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New", "Old", false));
        when(conceptDiffService.diff(any(), eq(master), eq("fr"))).thenReturn(draft);
        when(propositionMutationService.submitDraft(any())).thenReturn(Optional.of(55));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "New")
                .build();

        service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, "bob", "bob@ex.com", "sync", true, List.of(incoming)),
                user
        );

        ArgumentCaptor<PropositionSubmission> captor = ArgumentCaptor.forClass(PropositionSubmission.class);
        verify(propositionMutationService).submitDraft(captor.capture());
        assertTrue(captor.getValue().allowMultiplePending());
    }

    @Test
    void receiveBatch_skipsWhenSubmitDraftReturnsEmpty() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C1", "TH_MASTER")).thenReturn(true);

        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        when(conceptFullReadService.loadFullConcept("TH_MASTER", "C1", "fr", 0, true))
                .thenReturn(Optional.of(master));

        PropositionDraft draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New", "Old", false));
        when(conceptDiffService.diff(any(), eq(master), eq("fr"))).thenReturn(draft);
        when(propositionMutationService.submitDraft(any())).thenReturn(Optional.empty());

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, "bob", "bob@ex.com", "sync", true, List.of(
                        SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "New").build())),
                user
        );

        assertEquals(1, response.skipped());
        verify(propositionDraftService, never()).saveDraftDetails(anyInt(), any());
    }

    @Test
    void receiveBatch_errorsWhenMasterSnapshotMissingAfterMatch() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C1", "TH_MASTER")).thenReturn(true);
        when(conceptFullReadService.loadFullConcept("TH_MASTER", "C1", "fr", 0, true))
                .thenReturn(Optional.empty());

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(
                        SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "X").build())),
                user
        );

        assertEquals(1, response.errors());
        assertTrue(response.results().get(0).message().contains("introuvable"));
    }

    @Test
    void receiveBatch_errorsWhenIdentifierBlank() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(
                        SyncConceptPayload.builder().identifier("  ").build())),
                user
        );

        assertEquals(1, response.errors());
        verify(conceptRepository, never()).existsByIdConceptAndIdThesaurus(anyString(), anyString());
    }

    @Test
    void receiveBatch_errorsWhenCandidateCreationRefused() throws Exception {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C99", "TH_MASTER")).thenReturn(false);
        when(candidatMutationService.saveNewCandidat(
                any(), eq("TH_MASTER"), eq("fr"), eq(7), eq("alice"), eq("fr"), isNull()))
                .thenReturn(false);

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(
                        SyncConceptPayload.builder().identifier("C99").prefLabel("fr", "Nouveau").build())),
                user
        );

        assertEquals(1, response.errors());
    }

    @Test
    void receiveBatch_returnsErrorResultWhenProcessingThrows() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C1", "TH_MASTER"))
                .thenThrow(new RuntimeException("db down"));

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(
                        SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "X").build())),
                user
        );

        assertEquals(1, response.errors());
        assertEquals("db down", response.results().get(0).message());
    }

    @Test
    void receiveBatch_nullRequestOrNullConcepts_returnsEmpty() {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);

        assertEquals(0, service.receiveBatch("TH_MASTER", null, user).total());
        assertEquals(0, service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, null),
                user).total());
    }

    @Test
    void receiveBatch_nullUser_usesDefaultSyncIdentityForCandidate() throws Exception {
        when(toolboxPreferencePersistence.isMaster("TH_MASTER")).thenReturn(true);
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.existsByIdConceptAndIdThesaurus("C99", "TH_MASTER")).thenReturn(false);
        when(candidatMutationService.saveNewCandidat(
                any(), eq("TH_MASTER"), eq("fr"), eq(1), eq("sync"), eq("fr"), isNull()))
                .thenReturn(true);

        SyncBatchResponse response = service.receiveBatch(
                "TH_MASTER",
                new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of(
                        SyncConceptPayload.builder().identifier("C99").prefLabel("en", "Cat").build())),
                null
        );

        assertEquals(1, response.candidatesCreated());
    }

    private static SyncBatchRequest emptyRequest() {
        return new SyncBatchRequest("TH_SLAVE", null, null, null, null, true, List.of());
    }
}
