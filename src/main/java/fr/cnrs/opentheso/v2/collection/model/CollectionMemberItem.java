package fr.cnrs.opentheso.v2.collection.model;

import java.io.Serializable;

/**
 * Concept rattaché à une collection (fiche détail).
 */

public record CollectionMemberItem(
        String conceptId,
        String label
) implements Serializable {
}
