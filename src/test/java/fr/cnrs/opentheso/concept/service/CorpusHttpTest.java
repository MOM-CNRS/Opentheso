package fr.cnrs.opentheso.concept.service;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusHttpTest {

    @Test
    void toUri_encodesQuotesLeftInElasticsearchQuery() {
        String raw = "https://pro.frantiq.fr/es/koha_frantiq_biblios/_count?q=koha-auth-number:\"26678/pcrtd5d8pstxspv6dbhp7d8g\"";

        String uri = CorpusHttp.toUri(raw).toASCIIString();

        assertTrue(uri.contains("koha-auth-number:%2226678/pcrtd5d8pstxspv6dbhp7d8g%22"));
        assertTrue(uri.startsWith("https://pro.frantiq.fr/es/koha_frantiq_biblios/_count?"));
    }

    @Test
    void toUri_trimsALeadingSpaceBeforeTheScheme() {
        String raw = " https://ccj-corea.cnrs.fr/api/items?property%5B0%5D%5Btype%5D=ew&per_page=1";

        assertEquals(raw.trim(), CorpusHttp.toUri(raw).toString());
    }

    @Test
    void toUri_keepsAnAlreadyValidUrl() {
        String raw = "https://sculpturo.huma-num.fr/base/pactols-search/notices/total/354434";

        assertEquals(raw, CorpusHttp.toUri(raw).toString());
    }

    @Test
    void bundledHaricaRoot_matchesPublishedFingerprint() throws Exception {
        try (InputStream in = CorpusHttp.class.getResourceAsStream("/certs/harica-tls-rsa-root-2021.pem")) {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()))
                    .toUpperCase();

            assertEquals(CorpusHttp.HARICA_TLS_RSA_2021_SHA256, fingerprint);
        }
    }

    @Test
    void open_readsFrantiqElasticsearchCount() throws Exception {
        String raw = "https://pro.frantiq.fr/es/koha_frantiq_biblios/_count?q=koha-auth-number:\"26678/pcrton6An18MdT\"";
        HttpURLConnection connection = CorpusHttp.open(raw);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "JavaHttpClient");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        int status = connection.getResponseCode();
        String body = new String(connection.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        connection.disconnect();

        assertEquals(200, status);
        assertTrue(body.contains("\"count\":"));
    }

    @Test
    void open_followsHttpToHttpsAndReadsEpicherchellCount() throws Exception {
        HttpURLConnection connection = CorpusHttp.open(
                "http://ccj-epicherchel.huma-num.fr/interface/pactols-search/notices/total/17135",
                8000,
                8000);
        int status = connection.getResponseCode();
        String body = new String(connection.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        connection.disconnect();

        assertEquals(200, status);
        assertTrue(body.matches("(?s).*\"count\"\\s*:\\s*[1-9][0-9]*.*"));
    }

    @Test
    void open_reachesSculpturoWithoutHandshakeFailure() throws Exception {
        HttpURLConnection connection = CorpusHttp.open("https://sculpturo.huma-num.fr/");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        int status = connection.getResponseCode();
        connection.disconnect();

        assertTrue(status > 0);
    }
}
