package fr.cnrs.opentheso.graph.model;

/**
 * Voisin d'un concept.
 *
 * @param role   famille affichée : TG, TS, TA ou FA (facette)
 * @param type   rôle précis en base (BT, BTG, NTP, RT…), PARENT ou MEMBER autour d'une facette,
 *               FACET (facette du concept) ou FACETOF (facette qui contient le concept)
 * @param status valide, candidat ou deprecie
 */
public record GraphNeighbor(
        String id,
        String label,
        String role,
        String type,
        String status
) {
}
