package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropositionReviewFieldTest {

    @Test
    void toAppliedChange_keepsEditedAddValue() {
        var change = new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Keep", null, false);
        var field = new PropositionReviewField(change, "Synonym", "add", true);
        field.setNewValue("Keep edited");

        PropositionFieldChange applied = field.toAppliedChange();

        assertEquals(PropositionFieldAction.ADD, applied.action());
        assertEquals("Keep edited", applied.value());
    }

    @Test
    void toAppliedChange_returnsNullWhenRemoved() {
        var change = new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Keep", null, false);
        var field = new PropositionReviewField(change, "Synonym", "add", true);
        field.setRemoved(true);

        assertNull(field.toAppliedChange());
    }

    @Test
    void toAppliedChange_turnsClearedUpdateIntoDelete() {
        var change = new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New", "Old", false);
        var field = new PropositionReviewField(change, "Label", "update", true);
        field.setNewValue(" ");

        PropositionFieldChange applied = field.toAppliedChange();

        assertEquals(PropositionFieldAction.DELETE, applied.action());
        assertEquals("Old", applied.value());
    }

    @Test
    void isValueEditable_falseForDelete() {
        var change = new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.DELETE, "fr", "Gone", "Gone", false);
        var field = new PropositionReviewField(change, "Synonym", "delete", true);

        assertTrue(field.isDelete());
        assertEquals(false, field.isValueEditable());
    }
}
