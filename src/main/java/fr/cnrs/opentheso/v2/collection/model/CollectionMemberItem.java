package fr.cnrs.opentheso.v2.collection.model;

/**
 * Concept rattaché à une collection (fiche détail).
 */
public record CollectionMemberItem(
        String conceptId,
        String label
) {
}
