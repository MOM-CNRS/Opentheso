package fr.cnrs.opentheso.v2.sync.model;

import java.io.Serializable;

/**
 * Demande au maître la liste des concepts modifiés depuis une date.
 */
public record SyncChangesRequest(
        String since,
        String lang
) implements Serializable {
}
