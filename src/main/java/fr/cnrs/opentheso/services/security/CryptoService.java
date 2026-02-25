package fr.cnrs.opentheso.services.security;

import fr.cnrs.opentheso.utils.SimpleCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private final SimpleCrypto crypto;

    public CryptoService(@Value("${crypto.openark.key}") String secretKey) {
        if (secretKey.length() != 32) {
            throw new IllegalStateException("La clé AES doit faire 32 caractères");
        }
        this.crypto = new SimpleCrypto(secretKey);
    }

    public String encrypt(String value) {
        return crypto.encrypt(value);
    }

    public String decrypt(String value) {
        return crypto.decrypt(value);
    }
}
