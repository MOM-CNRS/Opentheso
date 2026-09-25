package fr.cnrs.opentheso.v2.sync.model;

import java.io.Serializable;

/**
 * Champ modifié pour un concept synchronisé (diff esclave / maître).
 */
public record SyncFieldChange(
        String field,
        String lang,
        String action,
        String oldValue,
        String newValue
) implements Serializable {
}
