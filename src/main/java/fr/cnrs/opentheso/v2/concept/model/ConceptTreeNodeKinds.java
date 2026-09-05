package fr.cnrs.opentheso.v2.concept.model;

/**
 * Types de nœuds de l'arbre conceptuel V2 (évite de répéter les littéraux).
 */
public final class ConceptTreeNodeKinds {

    public static final String CONCEPT = "concept";
    public static final String CANDIDAT = "candidat";
    public static final String REJETE = "rejete";
    public static final String FACET = "facet";
    public static final String GROUP = "group";
    public static final String SUB_GROUP = "subGroup";
    public static final String DEPRECIE = "deprecie";
    public static final String VALIDE = "valide";

    private ConceptTreeNodeKinds() {
    }
}
