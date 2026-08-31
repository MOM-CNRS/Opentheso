package fr.cnrs.opentheso.v2.user.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
