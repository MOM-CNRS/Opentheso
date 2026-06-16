package fr.cnrs.opentheso.v2.shared.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ApiKeyCipherTest {

    private final ApiKeyCipher cipher = new ApiKeyCipher("test-secret-key-for-unit-tests");

    @Test
    void encryptAndDecrypt_roundTrip() {
        String plain = "ot_abc123secret";

        String encrypted = cipher.encrypt(plain);
        String decrypted = cipher.decrypt(encrypted);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, decrypted);
    }
}
