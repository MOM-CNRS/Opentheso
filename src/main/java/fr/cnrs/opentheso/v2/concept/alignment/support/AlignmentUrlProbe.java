package fr.cnrs.opentheso.v2.concept.alignment.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Contrôles d'URL pour les alignements :
 * <ul>
 *   <li>format ({@code http}/{@code https}) — utilisé pour l'ajout / modification manuels</li>
 *   <li>joignabilité (HEAD) — réservé au contrôle batch admin ({@code checkUrls})</li>
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
     * Indique si l'URL répond (codes 2xx/3xx). Utilisé uniquement pour le contrôle
     * batch des URLs d'alignement en admin, pas à l'enregistrement manuel.
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
