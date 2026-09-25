package fr.cnrs.opentheso.v2.sync.model;

import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncFieldChangesTest {

    @Test
    void fromDraft_emptyWhenNullOrBlank() {
        assertTrue(SyncFieldChanges.fromDraft(null).isEmpty());
        assertTrue(SyncFieldChanges.fromDraft(new PropositionDraft()).isEmpty());
    }

    @Test
    void fromDraft_mapsPrefTranslationSynonymAndNote() {
        PropositionDraft draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "Nouveau", "Ancien", false));
        draft.getTranslationChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.TRADUCTION, PropositionFieldAction.ADD, "en", "Cat", null, false));
        draft.getSynonymChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Minou", null, false));
        draft.setNoteChange(new PropositionFieldChange(
                PropositionFieldCategory.DEFINITION, PropositionFieldAction.UPDATE, "fr", "Def 2", "Def 1", false));

        List<SyncFieldChange> changes = SyncFieldChanges.fromDraft(draft);

        assertEquals(4, changes.size());
        assertEquals("prefLabel", changes.get(0).field());
        assertEquals("Ancien", changes.get(0).oldValue());
        assertEquals("Nouveau", changes.get(0).newValue());
        assertEquals("translation", changes.get(1).field());
        assertEquals("synonym", changes.get(2).field());
        assertEquals("definition", changes.get(3).field());
    }
}
