package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropositionFieldChangeTest {

    @Test
    void update_showsOldAndNewValues() {
        var change = new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE,
                "fr", "Nouveau", "Ancien", false);

        assertEquals("Ancien", change.previousDisplay());
        assertEquals("Nouveau", change.nextDisplay());
        assertTrue(change.hasPrevious());
        assertTrue(change.hasNext());
        assertEquals("is-update", change.actionCss());
    }

    @Test
    void add_hasNoPreviousValue() {
        var change = new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD,
                "fr", "Ajouté", null, false);

        assertEquals("", change.previousDisplay());
        assertEquals("Ajouté", change.nextDisplay());
        assertFalse(change.hasPrevious());
        assertTrue(change.hasNext());
        assertEquals("is-add", change.actionCss());
    }

    @Test
    void delete_usesOldValueThenFallsBackToValue() {
        var withOld = new PropositionFieldChange(
                PropositionFieldCategory.NOTE, PropositionFieldAction.DELETE,
                "fr", "", "À retirer", false);
        var fallback = new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.DELETE,
                "fr", "À retirer", null, false);

        assertEquals("À retirer", withOld.previousDisplay());
        assertEquals("", withOld.nextDisplay());
        assertTrue(withOld.hasPrevious());
        assertFalse(withOld.hasNext());
        assertEquals("is-delete", withOld.actionCss());

        assertEquals("À retirer", fallback.previousDisplay());
        assertEquals("", fallback.nextDisplay());
    }
}
