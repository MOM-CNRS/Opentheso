package fr.cnrs.opentheso.v2.portal.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusPortalPublishServiceTest {

    private static final String PULL_URL = "https://opentheso.fr/openapi/v1/thesaurus/TH1";
    private static final String ONTOLOGY_URI = "https://opentheso.fr/?idt=TH1";

    @Mock
    private OntoPortalClient ontoPortalClient;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;

    private ThesaurusPortalPublishService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusPortalPublishService(
                ontoPortalClient, toolboxPreferencePersistence, 0);
    }

    @Test
    void publish_createsThenSubmitsWhenOntologyMissing() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(samplePrefs());
        when(ontoPortalClient.probeOntology(anyString(), anyString(), anyString()))
                .thenReturn(OntoPortalClient.OntologyProbe.ABSENT);
        when(ontoPortalClient.createOntology(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        var result = service.publish("TH1", "Animaux", validConfig());

        assertTrue(result.created());
        assertEquals("https://hsportal.espadon.net/ontologies/TH1", result.ontologyUrl());
        assertEquals(PULL_URL, result.pullLocation());
        verify(ontoPortalClient).createOntology(
                "https://hsportal.espadon.net", "secret", "TH1", "Animaux", "alice");
        verify(ontoPortalClient).submitSkos(
                eq("https://hsportal.espadon.net"),
                eq("secret"),
                eq("TH1"),
                eq("Alice"),
                eq("a@b.fr"),
                anyString(),
                eq(PULL_URL),
                eq(ONTOLOGY_URI),
                eq("Animaux")
        );
        verify(toolboxPreferencePersistence).updatePortalLastSyncAt(eq("TH1"), any(LocalDateTime.class));
    }

    @Test
    void publish_reportsIncreasingProgressSteps() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(samplePrefs());
        when(ontoPortalClient.probeOntology(anyString(), anyString(), anyString()))
                .thenReturn(OntoPortalClient.OntologyProbe.ABSENT);
        when(ontoPortalClient.createOntology(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        List<Integer> percents = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        service.publish("TH1", "Animaux", validConfig(), (percent, message) -> {
            percents.add(percent);
            messages.add(message);
        });

        assertEquals(100, percents.get(percents.size() - 1));
        for (int i = 1; i < percents.size(); i++) {
            assertTrue(percents.get(i) >= percents.get(i - 1));
        }
        assertTrue(messages.contains("v2.portal.progress.export"));
        assertTrue(messages.contains("v2.portal.progress.submit"));
        assertEquals("v2.portal.progress.doneCreated", messages.get(messages.size() - 1));
    }

    @Test
    void publish_submitsOnlyWhenOntologyExists() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(samplePrefs());
        when(ontoPortalClient.probeOntology(anyString(), anyString(), anyString()))
                .thenReturn(OntoPortalClient.OntologyProbe.PRESENT);

        var result = service.publish("TH1", "Animaux", validConfig());

        assertFalse(result.created());
        assertEquals(PULL_URL, result.pullLocation());
        verify(ontoPortalClient, never()).createOntology(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(ontoPortalClient).submitSkos(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                eq(PULL_URL), eq(ONTOLOGY_URI), eq("Animaux"));
    }

    @Test
    void resolveOntologyUri_usesConceptSchemeUrl() {
        assertEquals(ONTOLOGY_URI, ThesaurusPortalPublishService.resolveOntologyUri(
                samplePrefs(), "TH1", PULL_URL));
    }

    @Test
    void publish_rejectsMissingPublicSiteUrl() {
        Preferences prefs = samplePrefs();
        prefs.setCheminSite(null);
        prefs.setOriginalUri(null);
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(prefs);

        InvalidToolboxDataException ex = assertThrows(
                InvalidToolboxDataException.class, () -> service.publish("TH1", "Animaux", validConfig()));
        assertTrue(ex.getMessage().contains("chemin du site"));
        verify(ontoPortalClient, never()).probeOntology(anyString(), anyString(), anyString());
    }

    @Test
    void resolvePullLocation_usesCheminSiteThenOriginalUri() {
        Preferences prefs = Preferences.builder()
                .idThesaurus("TH1")
                .originalUri("https://archive.example/opentheso")
                .build();

        assertEquals(
                "https://opentheso.fr/openapi/v1/thesaurus/TH1",
                ThesaurusPortalPublishService.resolvePullLocation(samplePrefs(), "TH1"));
        assertEquals(
                "https://archive.example/opentheso/openapi/v1/thesaurus/TH1",
                ThesaurusPortalPublishService.resolvePullLocation(prefs, "TH1"));
    }

    @Test
    void publish_requiresApiKey() {
        var config = new ThesaurusPortalPublishService.PortalConfig(
                "https://hsportal.espadon.net", null, "TH1", "alice", "Alice", "a@b.fr");

        InvalidToolboxDataException ex = assertThrows(
                InvalidToolboxDataException.class, () -> service.publish("TH1", "Animaux", config));
        assertTrue(ex.getMessage().contains("clé API"));
    }

    @Test
    void publish_submitsWhenOntologyReadIsUnreadable() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(samplePrefs());
        when(ontoPortalClient.probeOntology(anyString(), anyString(), anyString()))
                .thenReturn(OntoPortalClient.OntologyProbe.UNREADABLE);
        when(ontoPortalClient.createOntology(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new InvalidToolboxDataException("Erreur HTTP 500 lors de la lecture"));

        var result = service.publish("TH1", "Animaux", validConfig());

        assertFalse(result.created());
        verify(ontoPortalClient).submitSkos(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                eq(PULL_URL), eq(ONTOLOGY_URI), eq("Animaux"));
    }

    @Test
    void unpublish_deletesOntologyAndClearsLastSync() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(samplePrefs());
        when(ontoPortalClient.deleteOntology(anyString(), anyString(), anyString())).thenReturn(true);

        service.unpublish("TH1", validConfig());

        verify(ontoPortalClient).deleteOntology("https://hsportal.espadon.net", "secret", "TH1");
        verify(toolboxPreferencePersistence).updatePortalLastSyncAt("TH1", null);
    }

    @Test
    void unpublish_requiresApiKey() {
        var config = new ThesaurusPortalPublishService.PortalConfig(
                "https://hsportal.espadon.net", null, "TH1", "alice", "Alice", "a@b.fr");

        InvalidToolboxDataException ex = assertThrows(
                InvalidToolboxDataException.class, () -> service.unpublish("TH1", config));
        assertTrue(ex.getMessage().contains("clé API"));
        verify(ontoPortalClient, never()).deleteOntology(anyString(), anyString(), anyString());
    }

    @Test
    void loadConfig_defaultsPortalUrl() {
        Preferences prefs = samplePrefs();
        prefs.setPortalUrl(null);
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(prefs);

        var config = service.loadConfig("TH1");

        assertEquals(ThesaurusPortalPublishService.DEFAULT_PORTAL_URL, config.portalUrl());
    }

    private static ThesaurusPortalPublishService.PortalConfig validConfig() {
        return new ThesaurusPortalPublishService.PortalConfig(
                "https://hsportal.espadon.net",
                "secret",
                "th1",
                "alice",
                "Alice",
                "a@b.fr"
        );
    }

    private static Preferences samplePrefs() {
        return Preferences.builder()
                .idThesaurus("TH1")
                .cheminSite("https://opentheso.fr")
                .portalUrl("https://hsportal.espadon.net")
                .portalApiKey("secret")
                .portalAcronym("TH1")
                .build();
    }
}
