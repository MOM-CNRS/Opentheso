package fr.cnrs.opentheso.v2.shared.crypto;

import java.security.SecureRandom;
import java.util.Base64;

public final class ApiKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ApiKeyGenerator() {
    }

    public static String generate(int randomBytesLength) {
        byte[] randomBytes = new byte[randomBytesLength];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
