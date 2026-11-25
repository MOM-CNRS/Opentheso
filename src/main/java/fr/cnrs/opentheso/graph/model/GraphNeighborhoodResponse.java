package fr.cnrs.opentheso.graph.model;

/**
 * Concept (ou facette) au centre et ses voisins directs.
 *
 * @param kind     concept ou facet. Pour une facette, {@code broader} est son concept parent
 *                 et {@code narrower} ses membres
 * @param found    {@code false} si le concept n'existe pas ou n'est pas visible par l'utilisateur
 * @param pageSize nombre maximal de voisins par famille dans cette réponse
 */
public record GraphNeighborhoodResponse(
        String id,
        String kind,
        String label,
        String status,
        boolean found,
        int pageSize,
        GraphNeighborGroup broader,
        GraphNeighborGroup narrower,
        GraphNeighborGroup related,
        GraphNeighborGroup facets
) {
    public static GraphNeighborhoodResponse notFound(String id, int pageSize) {
        return new GraphNeighborhoodResponse(id == null ? "" : id, "", "", "", false, pageSize,
                GraphNeighborGroup.empty(), GraphNeighborGroup.empty(),
                GraphNeighborGroup.empty(), GraphNeighborGroup.empty());
    }
}
