package fr.cnrs.opentheso.v2.collection.model;

import java.io.Serializable;

/**
 * Nœud de l'arbre Collections : collection racine, sous-collection, ou concept membre.
 */

public record CollectionTreeNode(
        String id,
        String label,
        String notation,
        String nodeType,
        boolean hasChildren,
        String status
) implements Serializable {
}
