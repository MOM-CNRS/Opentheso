package fr.cnrs.opentheso.v2.concept.model;

/**
 * Concept concerné par une action de masse : occurrence directe (sélection / membre
 * de collection) ou transverse (descendant NT, y compris polyhiérarchie).
 */
public record ConceptSubtreeMember(
        String id,
        String label,
        String status,
        String placement
) {
    public static final String DIRECT = "direct";
    public static final String TRANSVERSAL = "transversal";
}
