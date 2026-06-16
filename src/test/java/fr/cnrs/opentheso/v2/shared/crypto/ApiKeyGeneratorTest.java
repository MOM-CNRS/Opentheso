package fr.cnrs.opentheso.v2.shared.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ApiKeyGeneratorTest {

    @Test
    void generate_producesNonEmptyDistinctValues() {
        String key1 = ApiKeyGenerator.generate(32);
        String key2 = ApiKeyGenerator.generate(32);

        assertFalse(key1.isBlank());
        assertFalse(key2.isBlank());
        assertNotEquals(key1, key2);
    }
}
