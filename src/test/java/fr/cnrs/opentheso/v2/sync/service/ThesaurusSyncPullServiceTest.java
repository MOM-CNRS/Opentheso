package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncChangesRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncChangesResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.sync.model.SyncExportRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncExportResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncPendingConcept;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncPullServiceTest {

    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private ThesaurusSyncRemoteClient remoteClient;
    @Mock
    private ThesaurusSyncReceiveService thesaurusSyncReceiveService;

    private ThesaurusSyncPullService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ThesaurusSyncPullService(
                toolboxPreferencePersistence, remoteClient, thesaurusSyncReceiveService);
        user = User.builder().id(4).username("bob").mail("b@ex.com").build();
    }

    @Test
    void solicit_sendsSinceFromLastSync() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(
                LocalDateTime.of(2026, Month.JULY, 1, 8, 0)));
        when(remoteClient.postChanges(anyString(), eq("api-key"), any()))
                .thenReturn(new SyncChangesResponse(1, List.of(
                        new SyncPendingConcept("C1", "Chat", List.of("prefLabel")))));

        SyncChangesResponse response = service.solicit("TH1", null);

        assertEquals(1, response.total());
        ArgumentCaptor<SyncChangesRequest> captor = ArgumentCaptor.forClass(SyncChangesRequest.class);
        verify(remoteClient).postChanges(
                eq("https://master.example/api/v2/thesaurus/TH_MASTER/sync/changes"),
                eq("api-key"),
                captor.capture());
        assertEquals("2026-07-01T08:00", captor.getValue().since());
        assertEquals("fr", captor.getValue().lang());
    }

    @Test
    void solicit_sendsNullSinceOnFirstSync() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        when(remoteClient.postChanges(anyString(), eq("api-key"), any()))
                .thenReturn(new SyncChangesResponse(0, List.of()));

        service.solicit("TH1", null);

        ArgumentCaptor<SyncChangesRequest> captor = ArgumentCaptor.forClass(SyncChangesRequest.class);
        verify(remoteClient).postChanges(anyString(), eq("api-key"), captor.capture());
        assertEquals(null, captor.getValue().since());
    }

    @Test
    void pull_exportsAppliesAndUpdatesLastSyncAt() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(
                LocalDateTime.of(2026, Month.JULY, 1, 8, 0)));
        SyncConceptPayload payload = SyncConceptPayload.builder()
                .identifier("C1").prefLabel("fr", "Chat").build();
        when(remoteClient.postExport(anyString(), eq("api-key"), any()))
                .thenReturn(new SyncExportResponse(List.of(payload)));
        when(thesaurusSyncReceiveService.applyIncoming(eq("TH1"), any(), eq(user)))
                .thenReturn(SyncBatchResponse.from(List.of(
                        SyncConceptResult.proposition("C1", "C1", 9))));

        SyncBatchResponse response = service.pull(
                "TH1",
                List.of("C1"),
                "bob",
                "b@ex.com",
                "sync",
                true,
                null,
                user,
                null
        );

        assertEquals(1, response.propositionsCreated());
        ArgumentCaptor<SyncExportRequest> exportCaptor = ArgumentCaptor.forClass(SyncExportRequest.class);
        verify(remoteClient).postExport(
                eq("https://master.example/api/v2/thesaurus/TH_MASTER/sync/export"),
                eq("api-key"),
                exportCaptor.capture());
        assertEquals(List.of("C1"), exportCaptor.getValue().conceptIds());
        ArgumentCaptor<SyncBatchRequest> applyCaptor = ArgumentCaptor.forClass(SyncBatchRequest.class);
        verify(thesaurusSyncReceiveService).applyIncoming(eq("TH1"), applyCaptor.capture(), eq(user));
        assertEquals("TH_MASTER", applyCaptor.getValue().sourceThesaurusId());
        verify(toolboxPreferencePersistence).updateLastSyncAt(eq("TH1"), any(LocalDateTime.class));
    }

    @Test
    void pull_doesNotUpdateLastSyncAtWhenErrors() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        when(remoteClient.postExport(anyString(), eq("api-key"), any()))
                .thenReturn(new SyncExportResponse(List.of(
                        SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "X").build())));
        when(thesaurusSyncReceiveService.applyIncoming(eq("TH1"), any(), eq(user)))
                .thenReturn(SyncBatchResponse.from(List.of(SyncConceptResult.error("C1", "échec"))));

        SyncBatchResponse response = service.pull(
                "TH1", List.of("C1"), "bob", "b@ex.com", "c", true, null, user, null);

        assertEquals(1, response.errors());
        verify(toolboxPreferencePersistence, never()).updateLastSyncAt(anyString(), any());
    }

    @Test
    void pull_recordsErrorWhenMasterOmitsConcept() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));
        when(remoteClient.postExport(anyString(), eq("api-key"), any()))
                .thenReturn(new SyncExportResponse(List.of()));

        SyncBatchResponse response = service.pull(
                "TH1", List.of("C1"), "bob", "b@ex.com", "c", true, null, user, null);

        assertEquals(1, response.errors());
        verify(thesaurusSyncReceiveService, never()).applyIncoming(anyString(), any(), any());
        verify(toolboxPreferencePersistence, never()).updateLastSyncAt(anyString(), any());
    }

    @Test
    void pull_rejectsEmptySelection() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs(null));

        assertThrows(InvalidToolboxDataException.class, () ->
                service.pull("TH1", List.of(), "bob", "b@ex.com", "c", true, null, user, null));
        verify(remoteClient, never()).postExport(anyString(), anyString(), any());
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
