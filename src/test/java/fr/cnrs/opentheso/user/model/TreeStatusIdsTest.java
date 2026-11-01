package fr.cnrs.opentheso.user.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeStatusIdsTest {

    @Test
    void normalizeSelected_keepsKnownIdsInStableOrder() {
        Set<String> normalized = TreeStatusIds.normalizeSelected(List.of("deprecie", "ghost", "valide", "valide"));

        assertEquals(Set.of("valide", "deprecie"), normalized);
        assertTrue(normalized.contains("valide"));
    }

    @Test
    void normalizeSelected_emptyWhenNull() {
        assertTrue(TreeStatusIds.normalizeSelected(null).isEmpty());
    }

    @Test
    void defaults_includeDeprecatedAndKeepRejectedHidden() {
        assertTrue(TreeStatusIds.DEFAULT_SELECTED.contains(TreeStatusIds.DEPRECIE));
        assertTrue(TreeStatusIds.GUEST_SELECTED.contains(TreeStatusIds.DEPRECIE));
        assertFalse(TreeStatusIds.DEFAULT_SELECTED.contains(TreeStatusIds.REJETE));
        assertFalse(TreeStatusIds.GUEST_SELECTED.contains(TreeStatusIds.REJETE));
        assertFalse(TreeStatusIds.GUEST_SELECTED.contains(TreeStatusIds.CANDIDAT));
    }
}
