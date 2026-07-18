package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropositionDraftMapperTest {

    @Test
    void toDraft_ignoresUnchangedPreferredLabel() {
        var draft = PropositionDraftMapper.toDraft(
                "C1", "TH1", "fr", "Old label", "Old label",
                List.of(), List.of(), List.of());

        assertNull(draft.getPreferredLabelChange());
    }

    @Test
    void toDraft_detectsPreferredLabelChange() {
        var draft = PropositionDraftMapper.toDraft(
                "C1", "TH1", "fr", "Old label", "New label",
                List.of(), List.of(), List.of());

        assertEquals(PropositionFieldAction.UPDATE, draft.getPreferredLabelChange().action());
        assertEquals("New label", draft.getPreferredLabelChange().value());
        assertEquals("Old label", draft.getPreferredLabelChange().oldValue());
    }

    @Test
    void toDraft_ignoresBlankPreferredLabel() {
        var draft = PropositionDraftMapper.toDraft(
                "C1", "TH1", "fr", "Old label", "  ",
                List.of(), List.of(), List.of());

        assertNull(draft.getPreferredLabelChange());
    }

    @Test
    void toDraft_mapsSynonymAdditionUpdateAndRemoval() {
        var toAdd = new PropositionSynonymOption();
        toAdd.setLang("fr");
        toAdd.setValue("Nouveau");
        toAdd.setToAdd(true);

        var toUpdate = new PropositionSynonymOption();
        toUpdate.setLang("fr");
        toUpdate.setValue("Modifié");
        toUpdate.setOldValue("Ancien");
        toUpdate.setToUpdate(true);

        var toRemove = new PropositionSynonymOption();
        toRemove.setLang("fr");
        toRemove.setValue("A supprimer");
        toRemove.setOldValue("A supprimer");
        toRemove.setToRemove(true);

        var untouched = new PropositionSynonymOption();
        untouched.setLang("fr");
        untouched.setValue("Inchangé");
        untouched.setOldValue("Inchangé");

        var draft = PropositionDraftMapper.toDraft(
                "C1", "TH1", "fr", "Label", "Label",
                List.of(toAdd, toUpdate, toRemove, untouched), List.of(), List.of());

        assertEquals(3, draft.getSynonymChanges().size());
        assertEquals(PropositionFieldAction.ADD, draft.getSynonymChanges().get(0).action());
        assertEquals(PropositionFieldAction.UPDATE, draft.getSynonymChanges().get(1).action());
        assertEquals(PropositionFieldAction.DELETE, draft.getSynonymChanges().get(2).action());
    }

    @Test
    void toDraft_mapsTranslationChanges() {
        var toAdd = new PropositionTranslationOption();
        toAdd.setLang("en");
        toAdd.setValue("New value");
        toAdd.setToAdd(true);

        var draft = PropositionDraftMapper.toDraft(
                "C1", "TH1", "fr", "Label", "Label",
                List.of(), List.of(toAdd), List.of());

        assertEquals(1, draft.getTranslationChanges().size());
        assertEquals(PropositionFieldCategory.TRADUCTION, draft.getTranslationChanges().get(0).category());
        assertEquals(PropositionFieldAction.ADD, draft.getTranslationChanges().get(0).action());
    }

    @Test
    void toDraft_inferNoteActionFromValueDiff() {
        var added = new PropositionNoteOption();
        added.setTypeCode("definition");
        added.setValue("New definition");

        var updated = new PropositionNoteOption();
        updated.setTypeCode("note");
        updated.setOldValue("Old note");
        updated.setValue("New note");

        var removed = new PropositionNoteOption();
        removed.setTypeCode("scopeNote");
        removed.setOldValue("Old scope");
        removed.setValue("");

        var unchanged = new PropositionNoteOption();
        unchanged.setTypeCode("example");
        unchanged.setOldValue("Same");
        unchanged.setValue("Same");

        var draft = PropositionDraftMapper.toDraft(
                "C1", "TH1", "fr", "Label", "Label",
                List.of(), List.of(), List.of(added, updated, removed, unchanged));

        assertEquals(PropositionFieldAction.ADD, draft.getNoteChange("definition").action());
        assertEquals(PropositionFieldAction.UPDATE, draft.getNoteChange("note").action());
        assertEquals(PropositionFieldAction.DELETE, draft.getNoteChange("scopeNote").action());
        assertNull(draft.getNoteChange("example"));
    }

    @Test
    void toDraft_returnsEmptyDraftWhenNothingChanged() {
        var draft = PropositionDraftMapper.toDraft(
                "C1", "TH1", "fr", "Label", "Label",
                List.of(), List.of(), List.of());

        assertTrue(draft.isEmpty());
    }
}
