package fr.cnrs.opentheso.concept.service;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

/**
 * Ouverture des URL de comptage de corpus.
 * Les guillemets des requêtes Elasticsearch sont encodés, et la racine
 * publique HARICA TLS RSA 2021 est ajoutée au magasin Java qui ne la contient pas encore.
 */
@Slf4j
final class CorpusHttp {

    static final String HARICA_TLS_RSA_2021_SHA256 =
            "D95D0E8EDA79525BF9BEB11B14D2100D3294985F0C62D9FABD9CD999ECCB7B1D";

    private static final String HARICA_RESOURCE = "/certs/harica-tls-rsa-root-2021.pem";

    private static volatile SSLContext sslContext;

    private CorpusHttp() {
    }

    static URI toUri(String raw) {
        try {
            return URI.create(raw);
        } catch (IllegalArgumentException ex) {
            return URI.create(encodeQuery(raw));
        }
    }

    static HttpURLConnection open(String raw) throws IOException {
        URLConnection connection = toUri(raw).toURL().openConnection();
        if (!(connection instanceof HttpURLConnection http)) {
            throw new IOException("Unsupported corpus URL " + raw);
        }
        if (http instanceof HttpsURLConnection https) {
            SSLContext context = sslContext();
            if (context != null) {
                https.setSSLSocketFactory(context.getSocketFactory());
            }
        }
        return http;
    }

    static String encodeQuery(String raw) {
        int split = raw.indexOf('?');
        if (split < 0) {
            return raw;
        }
        StringBuilder encoded = new StringBuilder(raw.substring(0, split + 1));
        byte[] query = raw.substring(split + 1).getBytes(StandardCharsets.UTF_8);
        for (byte value : query) {
            int c = value & 0xff;
            if (isQuerySafe(c)) {
                encoded.append((char) c);
            } else {
                encoded.append('%');
                encoded.append(Character.toUpperCase(Character.forDigit(c >>> 4, 16)));
                encoded.append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }
        return encoded.toString();
    }

    private static boolean isQuerySafe(int c) {
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '-' || c == '.' || c == '_' || c == '~' || c == '%'
                || c == '!' || c == '$' || c == '&' || c == '\'' || c == '(' || c == ')'
                || c == '*' || c == '+' || c == ',' || c == ';' || c == '='
                || c == ':' || c == '@' || c == '/' || c == '?';
    }

    private static SSLContext sslContext() {
        SSLContext current = sslContext;
        if (current != null) {
            return current;
        }
        synchronized (CorpusHttp.class) {
            if (sslContext == null) {
                sslContext = buildContext();
            }
            return sslContext;
        }
    }

    private static SSLContext buildContext() {
        try (InputStream in = CorpusHttp.class.getResourceAsStream(HARICA_RESOURCE)) {
            if (in == null) {
                log.warn("Certificat HARICA introuvable, les corpus HTTPS utilisent le magasin Java par défaut");
                return null;
            }
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate harica = (X509Certificate) factory.generateCertificate(in);
            String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(harica.getEncoded()))
                    .toUpperCase();
            if (!HARICA_TLS_RSA_2021_SHA256.equals(fingerprint)) {
                log.warn("Empreinte HARICA inattendue, certificat ignoré");
                return null;
            }
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            store.setCertificateEntry("harica-tls-rsa-root-2021", harica);
            TrustManagerFactory extraFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            extraFactory.init(store);
            X509TrustManager extra = firstX509(extraFactory.getTrustManagers());

            TrustManagerFactory defaultFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultFactory.init((KeyStore) null);
            X509TrustManager fallback = firstX509(defaultFactory.getTrustManagers());

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new CombinedTrust(fallback, extra)}, null);
            return context;
        } catch (Exception ex) {
            log.warn("Impossible de charger la racine HARICA TLS 2021", ex);
            return null;
        }
    }

    private static X509TrustManager firstX509(TrustManager[] managers) {
        for (TrustManager manager : managers) {
            if (manager instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new IllegalStateException("Aucun TrustManager X509");
    }

    private static final class CombinedTrust implements X509TrustManager {
        private final X509TrustManager primary;
        private final X509TrustManager additional;

        private CombinedTrust(X509TrustManager primary, X509TrustManager additional) {
            this.primary = primary;
            this.additional = additional;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            primary.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                primary.checkServerTrusted(chain, authType);
            } catch (CertificateException ex) {
                additional.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return primary.getAcceptedIssuers();
        }
    }
}
