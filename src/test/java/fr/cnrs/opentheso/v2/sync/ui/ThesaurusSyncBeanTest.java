package fr.cnrs.opentheso.v2.sync.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncProgressTracker;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncSendService;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncBeanTest {

    @Mock
    private ThesaurusSyncSendService thesaurusSyncSendService;
    @Mock
    private UserSession userSession;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;

    private ThesaurusSyncProgressTracker progressTracker;
    private ThesaurusSyncBean bean;

    @BeforeEach
    void setUp() {
        progressTracker = new ThesaurusSyncProgressTracker();
        bean = new ThesaurusSyncBean(
                thesaurusSyncSendService, progressTracker, userSession, thesaurusAccessService);
        bean.setSyncExecutor(Runnable::run);
    }

    @Test
    void init_loadsConfigEvenWhenMasterLinkIncomplete() {
        stubAccess(true);
        when(thesaurusSyncSendService.loadConfig("TH1")).thenReturn(new ThesaurusSyncSendService.SyncConfig(
                "https://master.example",
                "TH_MASTER",
                "api-key",
                LocalDateTime.of(2026, 7, 1, 8, 0)
        ));
        when(thesaurusSyncSendService.prepare("TH1")).thenReturn(new ThesaurusSyncSendService.SyncPreparation(
                "TH1",
                "https://master.example",
                "TH_MASTER",
                "api-key",
                12,
                "fr",
                LocalDateTime.of(2026, 7, 1, 8, 0)
        ));

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.init("TH1");
        }

        assertTrue(bean.isFormAvailable());
        assertEquals("https://master.example", bean.getMasterServerUrl());
        assertEquals("TH_MASTER", bean.getMasterThesaurusId());
        assertEquals("*******", bean.getMasterApiKey());
        assertEquals(12, bean.getConceptCount());
        assertTrue(bean.isCreateCandidates());
    }

    @Test
    void init_keepsEditableFieldsWhenPreparationFails() {
        stubAccess(true);
        when(thesaurusSyncSendService.loadConfig("TH1")).thenReturn(new ThesaurusSyncSendService.SyncConfig(
                null, null, null, null));
        when(thesaurusSyncSendService.prepare("TH1"))
                .thenThrow(new InvalidToolboxDataException("Lien maître incomplet"));

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.init("TH1");
        }

        assertTrue(bean.isFormAvailable());
        assertNull(bean.getMasterServerUrl());
        assertEquals(0, bean.getConceptCount());
    }

    @Test
    void init_clearsStateWhenAccessDenied() {
        when(userSession.isLoggedIn()).thenReturn(false);

        bean.init("TH1");

        assertFalse(bean.isFormAvailable());
        verify(thesaurusSyncSendService, never()).loadConfig(anyString());
        verify(thesaurusSyncSendService, never()).prepare(anyString());
    }

    @Test
    void saveMasterLink_persistsFields() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        bean.setMasterServerUrl("https://master.example");
        bean.setMasterThesaurusId("TH_MASTER");
        bean.setMasterApiKey("api-key");
        when(thesaurusSyncSendService.prepare("TH1")).thenReturn(new ThesaurusSyncSendService.SyncPreparation(
                "TH1", "https://master.example", "TH_MASTER", "api-key", 3, "fr", null));

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.saveMasterLink();
            messages.verify(() -> MessageUtils.showInformationMessage(anyString()));
        }

        verify(thesaurusSyncSendService).saveMasterLink(
                "TH1", "https://master.example", "TH_MASTER", "api-key");
        assertEquals(3, bean.getConceptCount());
    }

    @Test
    void startSync_updatesProgressAndKeepsReport() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        bean.setComment("sync");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.getCurrentUsername()).thenReturn("alice");
        when(userSession.getCurrentUserEmail()).thenReturn("a@b.fr");

        doAnswer(invocation -> {
            Consumer<ThesaurusSyncSendService.SyncProgress> consumer = invocation.getArgument(5);
            consumer.accept(new ThesaurusSyncSendService.SyncProgress(
                    2, 2, 1, 1, 0, 0, "Lot 1 envoyé"));
            return SyncBatchResponse.from(List.of(
                    SyncConceptResult.skipped("C1", "C1", "ok"),
                    SyncConceptResult.proposition("C2", "C2", 9)
            ));
        }).when(thesaurusSyncSendService).runSync(
                eq("TH1"), eq("alice"), eq("a@b.fr"), eq("sync"), eq(false), any());

        when(thesaurusSyncSendService.prepare("TH1")).thenReturn(
                new ThesaurusSyncSendService.SyncPreparation(
                        "TH1", "https://master.example", "TH_MASTER", "api-key", 0, "fr", LocalDateTime.now()));

        bean.startSync();

        assertFalse(bean.isRunning());
        assertEquals(100, bean.getProgressValue());
        assertEquals(1, bean.getSkipped());
        assertEquals(1, bean.getPropositions());
        assertEquals(0, bean.getConceptCount());
        assertTrue(bean.isProgressVisible());
        assertEquals("2 / 2", bean.getProgressDetail());

        try (var faces = PrimeFacesTestSupport.open();
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.onProgressPoll();
            messages.verify(() -> MessageUtils.showInformationMessage(anyString()));
        }
    }

    @Test
    void startSync_showsErrorWhenServiceFails() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.getCurrentUsername()).thenReturn("alice");
        when(userSession.getCurrentUserEmail()).thenReturn("a@b.fr");
        when(thesaurusSyncSendService.runSync(anyString(), anyString(), anyString(), any(), anyBoolean(), any()))
                .thenThrow(new InvalidToolboxDataException("API key manquante"));

        bean.startSync();

        assertEquals("API key manquante", bean.getStatusMessage());
        assertFalse(bean.isRunning());

        try (var faces = PrimeFacesTestSupport.open();
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.onProgressPoll();
            messages.verify(() -> MessageUtils.showErrorMessage("API key manquante"));
        }
    }

    @Test
    void saveMasterLink_noopWhenAccessDenied() {
        when(userSession.isLoggedIn()).thenReturn(false);
        bean.setThesaurusId("TH1");

        bean.saveMasterLink();

        verify(thesaurusSyncSendService, never()).saveMasterLink(anyString(), any(), any(), any());
    }

    @Test
    void startSync_noopWhenAlreadyRunning() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.getCurrentUsername()).thenReturn("alice");
        when(userSession.getCurrentUserEmail()).thenReturn("a@b.fr");
        when(thesaurusSyncSendService.runSync(anyString(), anyString(), anyString(), any(), anyBoolean(), any()))
                .thenAnswer(invocation -> {
                    // Keep first sync "running" from bean perspective by not finishing yet —
                    // with sync executor Runnable::run it finishes immediately, so start twice before second can run.
                    return SyncBatchResponse.from(List.of());
                });
        when(thesaurusSyncSendService.prepare("TH1")).thenReturn(
                new ThesaurusSyncSendService.SyncPreparation(
                        "TH1", "https://m", "THM", "k", 0, "fr", null));

        bean.setSyncExecutor(command -> {
            // Leave running=true during nested start attempt
            ThesaurusSyncProgressTracker.ProgressState state = progressTracker.get(bean.getProgressKey());
            assertTrue(state.running);
            bean.startSync(); // should no-op
            command.run();
        });

        bean.startSync();
        verify(thesaurusSyncSendService, org.mockito.Mockito.times(1))
                .runSync(anyString(), anyString(), anyString(), any(), anyBoolean(), any());
    }

    @Test
    void onProgressPoll_ignoresWhileStillRunning() {
        progressTracker.start("manual").running = true;
        bean.setProgressKey("manual");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.onProgressPoll();
            messages.verifyNoInteractions();
        }
    }

    @Test
    void onProgressPoll_notifiesCompletionOnlyOnce() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.getCurrentUsername()).thenReturn("alice");
        when(userSession.getCurrentUserEmail()).thenReturn("a@b.fr");
        when(thesaurusSyncSendService.runSync(anyString(), anyString(), anyString(), any(), anyBoolean(), any()))
                .thenReturn(SyncBatchResponse.from(List.of()));
        when(thesaurusSyncSendService.prepare("TH1")).thenReturn(
                new ThesaurusSyncSendService.SyncPreparation(
                        "TH1", "https://m", "THM", "k", 0, "fr", null));

        bean.startSync();

        try (var faces = PrimeFacesTestSupport.open();
             MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.onProgressPoll();
            bean.onProgressPoll();
            messages.verify(() -> MessageUtils.showInformationMessage(anyString()),
                    org.mockito.Mockito.times(1));
        }
    }

    @Test
    void refreshPreparation_updatesConceptCount() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        when(thesaurusSyncSendService.prepare("TH1")).thenReturn(
                new ThesaurusSyncSendService.SyncPreparation(
                        "TH1", "https://m", "THM", "k", 8, "fr", null));

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.refreshPreparation();
            messages.verify(() -> MessageUtils.showInformationMessage(org.mockito.ArgumentMatchers.contains("8 concept")));
        }
        assertEquals(8, bean.getConceptCount());
    }

    @Test
    void startSync_passesCreateCandidatesFlagToService() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        bean.setCreateCandidates(false);
        bean.setComment("c");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.getCurrentUsername()).thenReturn("alice");
        when(userSession.getCurrentUserEmail()).thenReturn("a@b.fr");
        when(thesaurusSyncSendService.runSync(eq("TH1"), eq("alice"), eq("a@b.fr"), eq("c"), eq(false), any()))
                .thenReturn(SyncBatchResponse.from(List.of()));
        when(thesaurusSyncSendService.prepare("TH1")).thenReturn(
                new ThesaurusSyncSendService.SyncPreparation(
                        "TH1", "https://m", "THM", "k", 0, "fr", null));

        bean.startSync();

        verify(thesaurusSyncSendService).runSync(eq("TH1"), eq("alice"), eq("a@b.fr"), eq("c"), eq(false), any());
    }

    @Test
    void startSync_marksFailedOnUnexpectedException() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.getCurrentUsername()).thenReturn("alice");
        when(userSession.getCurrentUserEmail()).thenReturn("a@b.fr");
        when(thesaurusSyncSendService.runSync(anyString(), anyString(), anyString(), any(), anyBoolean(), any()))
                .thenThrow(new IllegalStateException("unexpected"));

        bean.startSync();

        assertEquals("unexpected", bean.getStatusMessage());
        assertFalse(bean.isRunning());
    }

    @Test
    void init_clearsWhenUserCannotManageThesaurus() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(2, false, "TH1")).thenReturn(false);

        bean.init("TH1");

        assertFalse(bean.isFormAvailable());
        verify(thesaurusSyncSendService, never()).loadConfig(anyString());
    }

    @Test
    void getFormattedLastSyncAt_formatsLocalDateTime() {
        bean.setLastSyncAt(LocalDateTime.of(2026, 8, 2, 15, 4));
        assertEquals("02/08/2026 15:04", bean.getFormattedLastSyncAt());
    }

    @Test
    void getProgressDetail_handlesZeroTotal() {
        assertEquals("", bean.getProgressDetail());
        var state = progressTracker.start("k");
        bean.setProgressKey("k");
        state.total = 0;
        state.processed = 3;
        assertEquals("3 concept(s) traité(s)", bean.getProgressDetail());
    }

    private void stubAccess(boolean granted) {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(2, false, "TH1")).thenReturn(granted);
    }
}
