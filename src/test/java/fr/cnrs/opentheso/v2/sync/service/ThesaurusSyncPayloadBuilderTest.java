package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.service.ConceptFullReadService;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncPayloadBuilderTest {

    @Mock
    private ConceptFullReadService conceptFullReadService;

    private ThesaurusSyncPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ThesaurusSyncPayloadBuilder(conceptFullReadService);
    }

    @Test
    void build_returnsEmptyWhenConceptMissing() {
        when(conceptFullReadService.loadFullConcept("TH1", "C1", "fr", 0, true))
                .thenReturn(Optional.empty());

        assertTrue(builder.build("TH1", "C1", "fr").isEmpty());
    }

    @Test
    void fromSnapshot_mapsLabelsAndNotes() {
        ConceptFullSnapshot snapshot = new ConceptFullSnapshot();
        snapshot.setIdentifier("C1");
        snapshot.setPermanentId("ark:/1/c1");
        snapshot.setNotation("N1");
        snapshot.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        snapshot.setPrefLabelsTraduction(List.of(new ConceptTermLabel("en", "Cat", "T1", 2)));
        snapshot.setAltLabels(List.of(new ConceptTermLabel("fr", "Minou", "T1", 3)));
        snapshot.setDefinitions(List.of(new ConceptSnapshotNote(10, "fr", "Animal", null)));
        snapshot.setNotes(List.of(new ConceptSnapshotNote(11, "fr", "Note", null)));
        snapshot.setScopeNotes(List.of(new ConceptSnapshotNote(12, "fr", "Scope", null)));

        SyncConceptPayload payload = builder.fromSnapshot(snapshot);

        assertEquals("C1", payload.identifier());
        assertEquals("ark:/1/c1", payload.permanentId());
        assertEquals("N1", payload.notation());
        assertEquals("Chat", payload.prefLabels().get("fr"));
        assertEquals("Cat", payload.prefLabels().get("en"));
        assertEquals(List.of("Minou"), payload.altLabels().get("fr"));
        assertEquals(List.of("Animal"), payload.definitions().get("fr"));
        assertEquals(List.of("Note"), payload.notes().get("fr"));
        assertEquals(List.of("Scope"), payload.scopeNotes().get("fr"));
    }

    @Test
    void build_delegatesToFullReadService() {
        ConceptFullSnapshot snapshot = new ConceptFullSnapshot();
        snapshot.setIdentifier("C1");
        snapshot.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        when(conceptFullReadService.loadFullConcept("TH1", "C1", "fr", 0, true))
                .thenReturn(Optional.of(snapshot));

        Optional<SyncConceptPayload> payload = builder.build("TH1", "C1", "fr");

        assertTrue(payload.isPresent());
        assertEquals("C1", payload.get().identifier());
        assertEquals("Chat", payload.get().prefLabels().get("fr"));
    }

    @Test
    void fromSnapshot_skipsBlankNotesAndIncludesAltTraduction() {
        ConceptFullSnapshot snapshot = new ConceptFullSnapshot();
        snapshot.setIdentifier("C1");
        snapshot.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        snapshot.setAltLabelTraduction(List.of(new ConceptTermLabel("en", "Kitty", "T1", 2)));
        snapshot.setNotes(List.of(
                new ConceptSnapshotNote(1, "fr", "  ", null),
                new ConceptSnapshotNote(2, "fr", "Note ok", null)
        ));

        SyncConceptPayload payload = builder.fromSnapshot(snapshot);

        assertEquals(List.of("Kitty"), payload.altLabels().get("en"));
        assertEquals(List.of("Note ok"), payload.notes().get("fr"));
    }
}
