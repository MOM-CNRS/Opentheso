package fr.cnrs.opentheso.v2.user.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableColIdsTest {

    @Test
    void normalizeSelected_keepsKnownIdsInStableOrder() {
        Set<String> normalized = TableColIds.normalizeSelected(List.of("path", "ghost", "status", "status"));

        assertEquals(Set.of("status", "path"), normalized);
        assertTrue(normalized.contains("status"));
    }

    @Test
    void normalizeSelected_emptyWhenNull() {
        assertTrue(TableColIds.normalizeSelected(null).isEmpty());
    }
}
