package fr.cnrs.opentheso.v2.proposition.model;

import org.apache.commons.lang3.StringUtils;

/**
 * Origine d'une proposition : synchronisation maître/copie, ou saisie manuelle.
 * Les anciennes lignes sans drapeau sont reconnues via le commentaire par défaut de la sync.
 */
public final class PropositionOrigin {

    static final String SYNC_COMMENT_PREFIX = "Synchronisation depuis";

    private PropositionOrigin() {
    }

    public static boolean fromSync(Boolean stored, String comment) {
        if (Boolean.TRUE.equals(stored)) {
            return true;
        }
        return StringUtils.startsWithIgnoreCase(
                StringUtils.trimToEmpty(comment), SYNC_COMMENT_PREFIX);
    }
}
