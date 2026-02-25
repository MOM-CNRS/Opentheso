package fr.cnrs.opentheso.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SimpleCrypto {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final String CHARSET = "UTF-8";

    private final byte[] key;

    public SimpleCrypto(String keyString) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            this.key = sha.digest(keyString.getBytes(CHARSET)); // toujours 32 bytes
        } catch (Exception e) {
            throw new RuntimeException("Erreur initialisation clé AES", e);
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(CHARSET));

            byte[] encrypted = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Erreur chiffrement", e);
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) return null;

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            if (decoded.length < IV_LENGTH_BYTE) {
                throw new RuntimeException("Données chiffrées trop courtes pour déchiffrement");
            }

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byte[] cipherText = new byte[decoded.length - IV_LENGTH_BYTE];

            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTE);
            System.arraycopy(decoded, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, CHARSET);
        } catch (Exception e) {
            throw new RuntimeException("Erreur déchiffrement", e);
        }
    }
}
