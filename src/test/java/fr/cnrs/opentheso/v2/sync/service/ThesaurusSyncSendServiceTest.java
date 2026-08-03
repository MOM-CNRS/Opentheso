package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptOutcome;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncSendServiceTest {

    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusSyncPayloadBuilder payloadBuilder;
    @Mock
    private ThesaurusSyncRemoteClient remoteClient;

    private ThesaurusSyncSendService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusSyncSendService(
                toolboxPreferencePersistence,
                conceptRepository,
                payloadBuilder,
                remoteClient
        );
    }

    @Test
    void prepare_rejectsMasterThesaurus() {
        when(toolboxPreferencePersistence.findPreferences("TH1"))
                .thenReturn(Preferences.builder().idThesaurus("TH1").master(true).build());

        assertThrows(InvalidToolboxDataException.class, () -> service.prepare("TH1"));
    }

    @Test
    void prepare_requiresMasterLink() {
        when(toolboxPreferencePersistence.findPreferences("TH1"))
                .thenReturn(Preferences.builder().idThesaurus("TH1").master(false).build());

        assertThrows(InvalidToolboxDataException.class, () -> service.prepare("TH1"));
    }

    @Test
    void prepare_countsDirtyConceptsSinceLastSync() {
        LocalDateTime lastSync = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(lastSync));
        when(conceptRepository.findConceptIdsChangedSince(eq("TH1"), any(Date.class)))
                .thenReturn(List.of("C1", "C2"));

        var preparation = service.prepare("TH1", false);

        assertEquals(2, preparation.conceptCount());
        assertEquals("https://master.example", preparation.masterServerUrl());
        assertEquals("TH_MASTER", preparation.masterThesaurusId());
        assertEquals("api-key", preparation.masterApiKey());
    }

    @Test
    void prepare_countsAllConceptsWhenSyncAll() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA"))
                .thenReturn(List.of(
                        Concept.builder().idConcept("C1").build(),
                        Concept.builder().idConcept("C2").build(),
                        Concept.builder().idConcept("C3").build()
                ));

        var preparation = service.prepare("TH1", true);

        assertEquals(3, preparation.conceptCount());
    }

    @Test
    void prepare_returnsZeroDirtyConceptsWhenNeverSynced() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));

        var preparation = service.prepare("TH1", false);

        assertEquals(0, preparation.conceptCount());
        verify(conceptRepository, never()).findConceptIdsChangedSince(anyString(), any());
        verify(conceptRepository, never()).findAllByIdThesaurusAndStatusNot(anyString(), anyString());
    }

    @Test
    void runSync_requiresApiKey() {
        when(toolboxPreferencePersistence.findPreferences("TH1"))
                .thenReturn(Preferences.builder()
                        .idThesaurus("TH1")
                        .master(false)
                        .masterServerUrl("https://master.example")
                        .masterThesaurusId("TH_MASTER")
                        .build());

        assertThrows(InvalidToolboxDataException.class, () ->
                service.runSync("TH1", "a", "a@b.fr", "c", false, null));
    }

    @Test
    void runSync_returnsEmptyWhenNoConcepts() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(LocalDateTime.now()));
        when(conceptRepository.findConceptIdsChangedSince(eq("TH1"), any(Date.class))).thenReturn(List.of());

        SyncBatchResponse response = service.runSync("TH1", "a", "a@b.fr", "c", false, null);

        assertEquals(0, response.total());
        verify(remoteClient, never()).postBatch(anyString(), anyString(), any());
        verify(toolboxPreferencePersistence, never()).updateLastSyncAt(anyString(), any());
    }

    @Test
    void runSync_postsBatchAndUpdatesLastSync() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA"))
                .thenReturn(List.of(Concept.builder().idConcept("C1").build()));

        SyncConceptPayload payload = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .build();
        when(payloadBuilder.build("TH1", "C1", "fr")).thenReturn(Optional.of(payload));

        SyncBatchResponse remoteResponse = SyncBatchResponse.from(List.of(
                SyncConceptResult.proposition("C1", "C1", 11)
        ));
        when(remoteClient.postBatch(anyString(), eq("api-key"), any())).thenReturn(remoteResponse);

        AtomicReference<ThesaurusSyncSendService.SyncProgress> lastProgress = new AtomicReference<>();
        // Première sync complète : le mode dirty avec last_sync_at null n'envoie rien.
        SyncBatchResponse response = service.runSync(
                "TH1", "alice", "a@b.fr", "comment", true, lastProgress::set);

        assertEquals(1, response.propositionsCreated());
        assertEquals(100, lastProgress.get().percent());

        ArgumentCaptor<SyncBatchRequest> requestCaptor = ArgumentCaptor.forClass(SyncBatchRequest.class);
        verify(remoteClient).postBatch(
                eq("https://master.example/api/v2/thesaurus/TH_MASTER/sync/concepts"),
                eq("api-key"),
                requestCaptor.capture()
        );
        assertEquals("TH1", requestCaptor.getValue().sourceThesaurusId());
        assertEquals(1, requestCaptor.getValue().concepts().size());
        verify(toolboxPreferencePersistence).updateLastSyncAt(eq("TH1"), any(LocalDateTime.class));
    }

    @Test
    void buildEndpoint_stripsTrailingSlash() {
        assertEquals(
                "https://master.example/api/v2/thesaurus/TH9/sync/concepts",
                ThesaurusSyncSendService.buildEndpoint("https://master.example/", "TH9")
        );
    }

    @Test
    void buildEndpoint_keepsUrlWithoutTrailingSlash() {
        assertEquals(
                "https://master.example/api/v2/thesaurus/TH9/sync/concepts",
                ThesaurusSyncSendService.buildEndpoint("https://master.example", "TH9")
        );
    }

    @Test
    void loadConfig_returnsMasterLinkWithoutValidatingCompleteness() {
        when(toolboxPreferencePersistence.findPreferences("TH1"))
                .thenReturn(Preferences.builder()
                        .idThesaurus("TH1")
                        .master(false)
                        .masterServerUrl(null)
                        .build());

        var config = service.loadConfig("TH1");

        assertEquals(null, config.masterServerUrl());
        assertEquals(null, config.masterThesaurusId());
    }

    @Test
    void saveMasterLink_persistsThroughPreferencePersistence() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));

        service.saveMasterLink("TH1", "https://master.example", "TH_MASTER", "k");

        verify(toolboxPreferencePersistence)
                .updateMasterLink("TH1", "https://master.example", "TH_MASTER", "k");
    }

    @Test
    void saveMasterLink_rejectsMasterThesaurus() {
        when(toolboxPreferencePersistence.findPreferences("TH1"))
                .thenReturn(Preferences.builder().idThesaurus("TH1").master(true).build());

        assertThrows(InvalidToolboxDataException.class, () ->
                service.saveMasterLink("TH1", "https://x", "TH", "k"));
    }

    @Test
    void prepare_throwsWhenPreferencesMissing() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(null);

        assertThrows(InvalidToolboxDataException.class, () -> service.prepare("TH1"));
    }

    @Test
    void prepare_defaultsWorkLangToFrWhenSourceLangBlank() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(
                Preferences.builder()
                        .idThesaurus("TH1")
                        .master(false)
                        .sourceLang(" ")
                        .masterServerUrl("https://master.example")
                        .masterThesaurusId("TH_MASTER")
                        .masterApiKey("api-key")
                        .lastSyncAt(LocalDateTime.now())
                        .build());
        when(conceptRepository.findConceptIdsChangedSince(eq("TH1"), any(Date.class)))
                .thenReturn(List.of());

        assertEquals("fr", service.prepare("TH1", false).workLang());
    }

    @Test
    void runSync_dirtyMode_postsOnlyConceptsChangedSinceLastSync() {
        when(toolboxPreferencePersistence.findPreferences("TH1"))
                .thenReturn(slavePrefs(LocalDateTime.of(2026, 1, 1, 10, 0)));
        when(conceptRepository.findConceptIdsChangedSince(eq("TH1"), any(Date.class)))
                .thenReturn(List.of("C9"));
        when(payloadBuilder.build("TH1", "C9", "fr")).thenReturn(Optional.of(
                SyncConceptPayload.builder().identifier("C9").prefLabel("fr", "X").build()));
        when(remoteClient.postBatch(anyString(), eq("api-key"), any()))
                .thenReturn(SyncBatchResponse.from(List.of(SyncConceptResult.skipped("C9", "C9", "ok"))));

        SyncBatchResponse response = service.runSync("TH1", "a", "a@b.fr", "c", false, null);

        assertEquals(1, response.skipped());
        verify(conceptRepository, never()).findAllByIdThesaurusAndStatusNot(anyString(), anyString());
        verify(toolboxPreferencePersistence).updateLastSyncAt(eq("TH1"), any(LocalDateTime.class));
    }

    @Test
    void runSync_nullLastSyncDirty_returnsEmptyWithoutRemoteCall() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));

        SyncBatchResponse response = service.runSync("TH1", "a", "a@b.fr", "c", false, null);

        assertEquals(0, response.total());
        verify(remoteClient, never()).postBatch(anyString(), anyString(), any());
        verify(toolboxPreferencePersistence, never()).updateLastSyncAt(anyString(), any());
    }

    @Test
    void runSync_splitsIntoMultipleBatchesWhenAboveDefaultBatchSize() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        var concepts = java.util.stream.IntStream.rangeClosed(1, 101)
                .mapToObj(i -> Concept.builder().idConcept("C" + i).build())
                .toList();
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA")).thenReturn(concepts);
        when(payloadBuilder.build(eq("TH1"), anyString(), eq("fr"))).thenAnswer(invocation ->
                Optional.of(SyncConceptPayload.builder()
                        .identifier(invocation.getArgument(1))
                        .prefLabel("fr", "L")
                        .build()));
        when(remoteClient.postBatch(anyString(), eq("api-key"), any()))
                .thenReturn(SyncBatchResponse.from(List.of(SyncConceptResult.skipped("Cx", "Cx", "ok"))));

        service.runSync("TH1", "a", "a@b.fr", "c", true, null);

        verify(remoteClient, times(2)).postBatch(anyString(), eq("api-key"), any());
    }

    @Test
    void runSync_skipsBatchWhenAllPayloadsEmpty_stillUpdatesLastSyncAt() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA"))
                .thenReturn(List.of(Concept.builder().idConcept("C1").build()));
        when(payloadBuilder.build("TH1", "C1", "fr")).thenReturn(Optional.empty());

        SyncBatchResponse response = service.runSync("TH1", "a", "a@b.fr", "c", true, null);

        assertEquals(0, response.total());
        verify(remoteClient, never()).postBatch(anyString(), anyString(), any());
        verify(toolboxPreferencePersistence).updateLastSyncAt(eq("TH1"), any(LocalDateTime.class));
    }

    @Test
    void runSync_doesNotUpdateLastSyncAtWhenRemoteThrows() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA"))
                .thenReturn(List.of(Concept.builder().idConcept("C1").build()));
        when(payloadBuilder.build("TH1", "C1", "fr")).thenReturn(Optional.of(
                SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "X").build()));
        doThrow(new InvalidToolboxDataException("boom"))
                .when(remoteClient).postBatch(anyString(), anyString(), any());

        assertThrows(InvalidToolboxDataException.class, () ->
                service.runSync("TH1", "a", "a@b.fr", "c", true, null));
        verify(toolboxPreferencePersistence, never()).updateLastSyncAt(anyString(), any());
    }

    private static Preferences slavePrefs(LocalDateTime lastSyncAt) {
        return Preferences.builder()
                .idThesaurus("TH1")
                .master(false)
                .sourceLang("fr")
                .masterServerUrl("https://master.example")
                .masterThesaurusId("TH_MASTER")
                .masterApiKey("api-key")
                .lastSyncAt(lastSyncAt)
                .build();
    }
}
