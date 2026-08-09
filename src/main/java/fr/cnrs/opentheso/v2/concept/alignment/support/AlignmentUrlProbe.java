package fr.cnrs.opentheso.v2.concept.alignment.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Contrôles d'URL pour les alignements manuels :
 * <ul>
 *   <li>format ({@code http}/{@code https})</li>
 *   <li>joignabilité (HEAD, timeout court)</li>
 * </ul>
 */
@Slf4j
public final class AlignmentUrlProbe {

    private static final int TIMEOUT_MS = 5_000;

    private AlignmentUrlProbe() {
    }

    public static boolean isValidFormat(String url) {
        return StringUtils.isNotBlank(url)
                && fr.cnrs.opentheso.utils.StringUtils.urlValidator(url.trim());
    }

    /**
     * Indique si l'URL répond (codes 2xx/3xx).
     * En création / modification manuelle, un échec bloque l'enregistrement.
     */
    public static boolean isReachable(String urlString) {
        if (StringUtils.isBlank(urlString)) {
            return false;
        }
        try {
            String normalized = urlString.trim().replaceFirst("(?i)^http://", "https://");
            URL url = URI.create(normalized).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            int code = connection.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception ex) {
            log.debug("URL d'alignement non joignable: {}", urlString, ex);
            return false;
        }
    }
}
