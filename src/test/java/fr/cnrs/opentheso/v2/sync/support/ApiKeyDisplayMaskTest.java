package fr.cnrs.opentheso.v2.sync.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyDisplayMaskTest {

    @Test
    void mask_keepsFirstAndLastFourCharacters() {
        assertEquals("abcd********wxyz", ApiKeyDisplayMask.mask("abcd12345678wxyz"));
    }

    @Test
    void mask_shortKeysAreFullyMasked() {
        assertEquals("*******", ApiKeyDisplayMask.mask("api-key"));
        assertEquals("********", ApiKeyDisplayMask.mask("12345678"));
    }

    @Test
    void mask_blankStaysEmpty() {
        assertEquals("", ApiKeyDisplayMask.mask(null));
        assertEquals("", ApiKeyDisplayMask.mask("  "));
    }

    @Test
    void resolveForPersist_keepsStoredWhenDisplayIsMasked() {
        String stored = "abcd12345678wxyz";
        assertEquals(stored, ApiKeyDisplayMask.resolveForPersist(ApiKeyDisplayMask.mask(stored), stored));
        assertTrue(ApiKeyDisplayMask.isUnchanged(ApiKeyDisplayMask.mask(stored), stored));
    }

    @Test
    void resolveForPersist_usesNewValueWhenUserTyped() {
        assertEquals("brand-new-key", ApiKeyDisplayMask.resolveForPersist("brand-new-key", "old-secret-key"));
        assertFalse(ApiKeyDisplayMask.isUnchanged("brand-new-key", "old-secret-key"));
    }

    @Test
    void resolveForPersist_firstEntryKeepsTypedValue() {
        assertEquals("first-key", ApiKeyDisplayMask.resolveForPersist("first-key", null));
        assertNull(ApiKeyDisplayMask.resolveForPersist("  ", null));
    }
}
