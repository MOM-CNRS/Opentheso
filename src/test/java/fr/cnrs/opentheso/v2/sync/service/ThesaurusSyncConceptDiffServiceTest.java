package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusSyncConceptDiffServiceTest {

    private final ThesaurusSyncConceptDiffService service = new ThesaurusSyncConceptDiffService();

    @Test
    void diff_detectsPreferredLabelUpdateAndNewSynonym() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        master.setAltLabels(List.of(new ConceptTermLabel("fr", "Félin", "T1", 2)));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat domestique")
                .altLabel("fr", "Félin")
                .altLabel("fr", "Minou")
                .build();

        PropositionDraft draft = service.diff(incoming, master, "fr");

        assertFalse(draft.isEmpty());
        assertEquals(PropositionFieldAction.UPDATE, draft.getPreferredLabelChange().action());
        assertEquals("Chat domestique", draft.getPreferredLabelChange().value());
        assertEquals(1, draft.getSynonymChanges().size());
        assertEquals("Minou", draft.getSynonymChanges().get(0).value());
        assertEquals(PropositionFieldAction.ADD, draft.getSynonymChanges().get(0).action());
    }

    @Test
    void diff_returnsEmptyWhenIdentical() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        master.setDefinitions(List.of(new ConceptSnapshotNote(1, "fr", "Animal", null)));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .definition("fr", "Animal")
                .build();

        assertTrue(service.diff(incoming, master, "fr").isEmpty());
    }

    @Test
    void diff_detectsNewTranslation() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .prefLabel("en", "Cat")
                .build();

        PropositionDraft draft = service.diff(incoming, master, "fr");

        assertNull(draft.getPreferredLabelChange());
        assertEquals(1, draft.getTranslationChanges().size());
        assertEquals(PropositionFieldAction.ADD, draft.getTranslationChanges().get(0).action());
        assertEquals("en", draft.getTranslationChanges().get(0).lang());
        assertEquals("Cat", draft.getTranslationChanges().get(0).value());
    }

    @Test
    void diff_detectsUpdatedDefinition() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        master.setDefinitions(List.of(new ConceptSnapshotNote(1, "fr", "Ancien", null)));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .definition("fr", "Nouveau")
                .build();

        PropositionDraft draft = service.diff(incoming, master, "fr");

        assertNotNull(draft.getNoteChange("definition"));
        assertEquals(PropositionFieldCategory.DEFINITION, draft.getNoteChange("definition").category());
        assertEquals(PropositionFieldAction.UPDATE, draft.getNoteChange("definition").action());
        assertEquals("Nouveau", draft.getNoteChange("definition").value());
        assertEquals("Ancien", draft.getNoteChange("definition").oldValue());
    }

    @Test
    void diff_ignoresBlankIncomingPreferredLabel() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "   ")
                .build();

        assertTrue(service.diff(incoming, master, "fr").isEmpty());
    }

    @Test
    void diff_addsPreferredLabelWhenMasterWorkLangMissing() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .build();

        PropositionDraft draft = service.diff(incoming, master, "fr");

        assertNotNull(draft.getPreferredLabelChange());
        assertEquals(PropositionFieldAction.ADD, draft.getPreferredLabelChange().action());
        assertEquals("Chat", draft.getPreferredLabelChange().value());
    }

    @Test
    void diff_updatesExistingTranslation() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        master.setPrefLabelsTraduction(List.of(new ConceptTermLabel("en", "Cat", "T1", 2)));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .prefLabel("en", "Kitty")
                .build();

        PropositionDraft draft = service.diff(incoming, master, "fr");

        assertEquals(1, draft.getTranslationChanges().size());
        assertEquals(PropositionFieldAction.UPDATE, draft.getTranslationChanges().get(0).action());
        assertEquals("Kitty", draft.getTranslationChanges().get(0).value());
    }

    @Test
    void diff_addsNoteAndScopeNote() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .note("fr", "Note esclave")
                .scopeNote("fr", "Scope esclave")
                .build();

        PropositionDraft draft = service.diff(incoming, master, "fr");

        assertNotNull(draft.getNoteChange("note"));
        assertEquals(PropositionFieldAction.ADD, draft.getNoteChange("note").action());
        assertNotNull(draft.getNoteChange("scopeNote"));
        assertEquals(PropositionFieldAction.ADD, draft.getNoteChange("scopeNote").action());
    }

    @Test
    void diff_ignoresSynonymThatMatchesAfterNormalize() {
        ConceptFullSnapshot master = new ConceptFullSnapshot();
        master.setIdentifier("C1");
        master.setPrefLabel(new ConceptTermLabel("fr", "Chat", "T1", 1));
        master.setAltLabels(List.of(new ConceptTermLabel("fr", "Minou", "T1", 2)));

        SyncConceptPayload incoming = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .altLabel("fr", "  Minou  ")
                .build();

        assertTrue(service.diff(incoming, master, "fr").isEmpty());
    }
}
