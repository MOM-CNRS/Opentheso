package fr.cnrs.opentheso.graph.model;

/**
 * Voisin d'un concept.
 *
 * @param role   famille affichée : TG, TS ou TA
 * @param type   rôle précis en base (BT, BTG, NTP, RT…)
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
