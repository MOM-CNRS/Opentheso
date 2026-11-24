package fr.cnrs.opentheso.graph.model;

/**
 * Concept central et ses voisins directs.
 *
 * @param found    {@code false} si le concept n'existe pas ou n'est pas visible par l'utilisateur
 * @param pageSize nombre maximal de voisins par famille dans cette réponse
 */
public record GraphNeighborhoodResponse(
        String id,
        String label,
        String status,
        boolean found,
        int pageSize,
        GraphNeighborGroup broader,
        GraphNeighborGroup narrower,
        GraphNeighborGroup related
) {
    public static GraphNeighborhoodResponse notFound(String id, int pageSize) {
        return new GraphNeighborhoodResponse(id == null ? "" : id, "", "", false, pageSize,
                GraphNeighborGroup.empty(), GraphNeighborGroup.empty(), GraphNeighborGroup.empty());
    }
}
