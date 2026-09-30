package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncPendingConcept;
import fr.cnrs.opentheso.v2.sync.repository.ThesaurusSyncQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncExportServiceTest {

    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusSyncQueryRepository thesaurusSyncQueryRepository;
    @Mock
    private ThesaurusSyncPayloadBuilder payloadBuilder;

    private ThesaurusSyncExportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusSyncExportService(
                toolboxPreferencePersistence,
                conceptRepository,
                thesaurusSyncQueryRepository,
                payloadBuilder
        );
    }

    @Test
    void listChanges_rejectsNonMasterThesaurus() {
        when(toolboxPreferencePersistence.findPreferences("TH1")).thenReturn(slavePrefs());

        assertThrows(IllegalStateException.class, () ->
                service.listChanges("TH1", (String) null, "fr"));
    }

    @Test
    void listChanges_listsAllWhenSinceBlank() {
        when(toolboxPreferencePersistence.findPreferences("TH_MASTER")).thenReturn(masterPrefs());
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH_MASTER", "CA"))
                .thenReturn(List.of(
                        Concept.builder().idConcept("C1").build(),
                        Concept.builder().idConcept("C2").build()
                ));
        when(thesaurusSyncQueryRepository.findPendingConcepts(eq("TH_MASTER"), isNull(), eq("fr"), eq(2000)))
                .thenReturn(List.of(new SyncPendingConcept("C1", "Chat", List.of("all"))));

        var response = service.listChanges("TH_MASTER", "  ", "fr");

        assertEquals(2, response.total());
        assertEquals(1, response.concepts().size());
        assertEquals("C1", response.concepts().get(0).id());
    }

    @Test
    void listChanges_usesChangedSinceWhenDateProvided() {
        when(toolboxPreferencePersistence.findPreferences("TH_MASTER")).thenReturn(masterPrefs());
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(conceptRepository.findConceptIdsChangedSince(eq("TH_MASTER"), any(Date.class)))
                .thenReturn(List.of("C1"));
        when(thesaurusSyncQueryRepository.findPendingConcepts(eq("TH_MASTER"), any(Date.class), eq("fr"), eq(2000)))
                .thenReturn(List.of(new SyncPendingConcept("C1", "Chat", List.of("prefLabel"))));

        var response = service.listChanges("TH_MASTER", "2026-07-01T08:00", null);

        assertEquals(1, response.total());
        assertEquals("prefLabel", response.concepts().get(0).changedFields().get(0));
    }

    @Test
    void exportConcepts_buildsPayloadsAndSkipsUnknown() {
        when(toolboxPreferencePersistence.findPreferences("TH_MASTER")).thenReturn(masterPrefs());
        when(toolboxPreferencePersistence.getWorkLanguage("TH_MASTER")).thenReturn("fr");
        when(payloadBuilder.build("TH_MASTER", "C1", "fr")).thenReturn(Optional.of(
                SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "Chat").build()));
        when(payloadBuilder.build("TH_MASTER", "C2", "fr")).thenReturn(Optional.empty());

        var response = service.exportConcepts("TH_MASTER", List.of("C1", "C1", " ", "C2"), "fr");

        assertEquals(1, response.concepts().size());
        assertEquals("C1", response.concepts().get(0).identifier());
        verify(payloadBuilder).build("TH_MASTER", "C1", "fr");
        verify(payloadBuilder).build("TH_MASTER", "C2", "fr");
    }

    @Test
    void parseSince_blankIsNull() {
        assertNull(ThesaurusSyncExportService.parseSince(null));
        assertNull(ThesaurusSyncExportService.parseSince("  "));
    }

    @Test
    void parseSince_localDateTime() {
        LocalDateTime parsed = ThesaurusSyncExportService.parseSince("2026-07-01T08:00");
        assertEquals(LocalDateTime.of(2026, Month.JULY, 1, 8, 0), parsed);
    }

    @Test
    void parseSince_invalidThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ThesaurusSyncExportService.parseSince("pas-une-date"));
        assertTrue(ex.getMessage().contains("since"));
    }

    private static Preferences masterPrefs() {
        return Preferences.builder()
                .idThesaurus("TH_MASTER")
                .master(true)
                .sourceLang("fr")
                .build();
    }

    private static Preferences slavePrefs() {
        return Preferences.builder()
                .idThesaurus("TH1")
                .master(false)
                .sourceLang("fr")
                .build();
    }
}
