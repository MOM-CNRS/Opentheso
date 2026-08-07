package fr.cnrs.opentheso.v2.sync.support;

import org.apache.commons.lang3.StringUtils;

/**
 * Masquage d'affichage des clés API : conserve les 4 premiers et 4 derniers caractères.
 * Une clé nouvellement saisie (pas encore chargée depuis le stockage) reste lisible en clair.
 */
public final class ApiKeyDisplayMask {

    private static final int VISIBLE_EDGE = 4;

    private ApiKeyDisplayMask() {
    }

    public static String mask(String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            return "";
        }
        String value = apiKey.trim();
        int length = value.length();
        if (length <= VISIBLE_EDGE * 2) {
            return "*".repeat(length);
        }
        return value.substring(0, VISIBLE_EDGE)
                + "*".repeat(length - VISIBLE_EDGE * 2)
                + value.substring(length - VISIBLE_EDGE);
    }

    /**
     * {@code true} si le champ affiché n'a pas été modifié par rapport à la valeur stockée.
     */
    public static boolean isUnchanged(String displayed, String stored) {
        if (StringUtils.isBlank(stored)) {
            return StringUtils.isBlank(displayed);
        }
        String trimmedDisplayed = StringUtils.trimToEmpty(displayed);
        return stored.equals(trimmedDisplayed) || mask(stored).equals(trimmedDisplayed);
    }

    /**
     * Valeur à persister : conserve le secret stocké si l'UI n'a pas changé le masque.
     */
    public static String resolveForPersist(String displayed, String stored) {
        if (isUnchanged(displayed, stored)) {
            return stored;
        }
        return StringUtils.trimToNull(displayed);
    }
}
