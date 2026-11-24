package fr.cnrs.opentheso.graph.model;

import java.util.List;

/**
 * Une page de voisins d'une même famille.
 *
 * @param total nombre de voisins visibles par l'utilisateur, toutes pages confondues
 */
public record GraphNeighborGroup(
        List<GraphNeighbor> items,
        int total,
        int offset
) {
    public static GraphNeighborGroup empty() {
        return new GraphNeighborGroup(List.of(), 0, 0);
    }
}
