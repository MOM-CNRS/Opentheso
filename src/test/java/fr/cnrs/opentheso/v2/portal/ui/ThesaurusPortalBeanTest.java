package fr.cnrs.opentheso.v2.portal.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalProgressTracker;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalPublishService;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalPublishService.PortalConfig;
import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalPublishService.PublishResult;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusPortalBeanTest {

    @Mock
    private ThesaurusPortalPublishService thesaurusPortalPublishService;
    @Mock
    private UserSession userSession;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;

    private ThesaurusPortalProgressTracker progressTracker;
    private ThesaurusPortalBean bean;

    @BeforeEach
    void setUp() {
        progressTracker = new ThesaurusPortalProgressTracker();
        bean = new ThesaurusPortalBean(
                thesaurusPortalPublishService,
                progressTracker,
                userSession,
                thesaurusAccessService,
                thesaurusContext,
                thesaurusPreferenceService);
        bean.setPublishExecutor(Runnable::run);
    }

    @Test
    void init_clearsWhenGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);

        bean.init("TH1");

        assertFalse(bean.isFormAvailable());
        verify(thesaurusPortalPublishService, never()).loadConfig(anyString());
    }

    @Test
    void init_clearsWhenUserCannotManageThesaurus() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(2, false, "TH1")).thenReturn(false);

        bean.init("TH1");

        assertFalse(bean.isFormAvailable());
        verify(thesaurusPortalPublishService, never()).loadConfig(anyString());
    }

    @Test
    void init_loadsSavedLink() {
        stubAccess(true);
        when(thesaurusPortalPublishService.loadConfig("TH1")).thenReturn(new PortalConfig(
                "https://hsportal.espadon.net",
                "secret-key-value",
                "TH1",
                "alice",
                "Alice",
                "a@b.fr"
        ));
        when(thesaurusPortalPublishService.lastSyncAt("TH1"))
                .thenReturn(LocalDateTime.of(2026, Month.SEPTEMBER, 1, 10, 0));

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.init("TH1");
        }

        assertTrue(bean.isFormAvailable());
        assertEquals("https://hsportal.espadon.net", bean.getPortalUrl());
        assertEquals("TH1", bean.getPortalAcronym());
        assertEquals("01/09/2026 10:00", bean.getFormattedLastSyncAt());
        assertEquals("https://hsportal.espadon.net/ontologies/TH1", bean.getOntologyUrl());
    }

    @Test
    void saveLink_isIgnoredWhenGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);
        bean.setThesaurusId("TH1");

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.saveLink();
        }

        verify(thesaurusPortalPublishService, never()).saveLink(anyString(), any());
    }

    @Test
    void publish_isIgnoredWhenGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);
        bean.setThesaurusId("TH1");

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.publish();
        }

        verify(thesaurusPortalPublishService, never()).publish(anyString(), anyString(), any(), any());
    }

    @Test
    void publish_storesOntologyUrlOnSuccess() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        bean.setPortalUrl("https://hsportal.espadon.net");
        bean.setPortalApiKey("secret");
        bean.setPortalAcronym("TH1");
        bean.setPortalUsername("alice");
        bean.setPortalContactName("Alice");
        bean.setPortalContactEmail("a@b.fr");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Animaux");
        doNothing().when(thesaurusPortalPublishService).saveLink(anyString(), any());
        when(thesaurusPortalPublishService.publish(anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    BiConsumer<Integer, String> progress = invocation.getArgument(3);
                    if (progress != null) {
                        progress.accept(100, "v2.portal.progress.doneCreated");
                    }
                    return new PublishResult(
                            true,
                            "https://hsportal.espadon.net/ontologies/TH1",
                            "https://opentheso.fr/openapi/v1/thesaurus/TH1");
                });

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.publish();
        }

        assertTrue(bean.isCreatedOnPortal());
        assertEquals("https://hsportal.espadon.net/ontologies/TH1", bean.getOntologyUrl());
        assertEquals("https://opentheso.fr/openapi/v1/thesaurus/TH1", bean.getPullLocation());
        assertTrue(bean.isResultDialogVisible());
        assertFalse(bean.isResultFailed());
        assertEquals(" is-ok", bean.getResultDialogStyle());
        assertEquals("v2.portal.result.ok.createdTitle", bean.getResultTitleKey());
        assertEquals(100, bean.getResultProgress());
        verify(thesaurusPortalPublishService).publish(eq("TH1"), eq("Animaux"), any(), any());
    }

    @Test
    void publish_opensFailureDialogWhenServiceFails() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        bean.setPortalUrl("https://hsportal.espadon.net");
        bean.setPortalApiKey("secret");
        bean.setPortalAcronym("TH1");
        bean.setPortalUsername("alice");
        bean.setPortalContactName("Alice");
        bean.setPortalContactEmail("a@b.fr");
        doNothing().when(thesaurusPortalPublishService).saveLink(anyString(), any());
        doThrow(new IllegalStateException("Export SKOS impossible: timeout"))
                .when(thesaurusPortalPublishService).publish(anyString(), anyString(), any(), any());

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.publish();
        }

        assertTrue(bean.isResultDialogVisible());
        assertTrue(bean.isResultFailed());
        assertEquals(" is-err", bean.getResultDialogStyle());
        assertEquals("v2.portal.result.err.title", bean.getResultTitleKey());
        assertEquals("Export SKOS impossible: timeout", bean.getResultError());
        bean.dismissResult();
        assertFalse(bean.isResultDialogVisible());
    }

    @Test
    void publish_showsProgressBarWhileJobRuns() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        bean.setPortalUrl("https://hsportal.espadon.net");
        bean.setPortalApiKey("secret");
        bean.setPortalAcronym("TH1");
        bean.setPortalUsername("alice");
        bean.setPortalContactName("Alice");
        bean.setPortalContactEmail("a@b.fr");
        bean.setPublishExecutor(command -> { });
        doNothing().when(thesaurusPortalPublishService).saveLink(anyString(), any());

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.publish();
        }

        assertTrue(bean.isRunning());
        assertTrue(bean.isProgressVisible());
        assertFalse(bean.isResultDialogVisible());
        assertEquals(4, bean.getProgressValue());
        assertTrue(bean.isStatusLocalized());
        assertEquals(" is-on", bean.getPipeExportStyle());
        verify(thesaurusPortalPublishService, never()).publish(anyString(), anyString(), any(), any());
    }

    @Test
    void removeFromPortal_isIgnoredWhenGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);
        bean.setThesaurusId("TH1");

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.removeFromPortal();
        }

        verify(thesaurusPortalPublishService, never()).unpublish(anyString(), any(), any());
    }

    @Test
    void removeFromPortal_clearsOntologyUrlOnSuccess() {
        stubAccess(true);
        bean.setThesaurusId("TH1");
        bean.setPortalUrl("https://hsportal.espadon.net");
        bean.setPortalApiKey("secret");
        bean.setPortalAcronym("TH1");
        bean.setOntologyUrl("https://hsportal.espadon.net/ontologies/TH1");
        doNothing().when(thesaurusPortalPublishService).saveLink(anyString(), any());
        org.mockito.Mockito.doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            BiConsumer<Integer, String> progress = invocation.getArgument(2);
            if (progress != null) {
                progress.accept(100, "v2.portal.progress.removed");
            }
            return null;
        }).when(thesaurusPortalPublishService).unpublish(anyString(), any(), any());

        try (MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            bean.removeFromPortal();
        }

        assertTrue(bean.isRemovedFromPortal());
        assertTrue(bean.isResultDialogVisible());
        assertFalse(bean.isResultFailed());
        assertEquals("v2.portal.result.ok.removeTitle", bean.getResultTitleKey());
        assertNull(bean.getOntologyUrl());
        verify(thesaurusPortalPublishService).unpublish(eq("TH1"), any(), any());
    }

    @Test
    void isShortcutVisible_requiresManageRightsAndPreference() {
        stubAccess(true);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(preferencesWithPortail(true));
        assertTrue(bean.isShortcutVisible());

        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(preferencesWithPortail(false));
        assertFalse(bean.isShortcutVisible());
    }

    private static ThesaurusPreferences preferencesWithPortail(boolean enabled) {
        var base = SettingTestFixtures.samplePreferences();
        return new ThesaurusPreferences(
                base.thesaurusId(),
                base.sourceLang(),
                base.identifierType(),
                base.cheminSite(),
                base.idNaan(),
                base.preferredName(),
                base.originalUri(),
                base.exportUriType(),
                base.identifierServerType(),
                base.useHandle(),
                base.userHandle(),
                base.passHandle(),
                base.pathKeyHandle(),
                base.pathCertHandle(),
                base.urlApiHandle(),
                base.prefixIdHandle(),
                base.privatePrefixHandle(),
                base.uriArk(),
                base.useArk(),
                base.serverArk(),
                base.prefixArk(),
                base.userArk(),
                base.passArk(),
                base.generateHandle(),
                base.autoExpandTree(),
                base.sortByNotation(),
                base.treeCache(),
                base.useArkLocal(),
                base.naanArkLocal(),
                base.prefixArkLocal(),
                base.sizeIdArkLocal(),
                base.breadcrumb(),
                base.useConceptTree(),
                base.displayUserName(),
                base.suggestion(),
                base.useCustomRelation(),
                base.uppercaseForArk(),
                base.showHistoryNote(),
                base.showEditorialNote(),
                base.useHandleWithCertificat(),
                base.adminHandle(),
                base.indexHandle(),
                base.useDeeplTranslation(),
                base.deeplApiKey(),
                base.webservices(),
                base.kohaLink(),
                base.useOpenArk(),
                base.serverOpenArk(),
                base.naanOpenArk(),
                base.prefixOpenArk(),
                base.apiKeyOpenArk(),
                base.showCandidatesToGuests(),
                base.synchronisation(),
                enabled,
                base.languages()
        );
    }

    private void stubAccess(boolean granted) {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusAccessService.canManageThesaurus(2, false, "TH1")).thenReturn(granted);
    }
}
