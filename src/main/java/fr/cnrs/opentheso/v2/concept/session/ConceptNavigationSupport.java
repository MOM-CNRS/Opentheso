package fr.cnrs.opentheso.v2.concept.session;

/**
 * Opérations de navigation dans l'écran de consultation.
 * <p>
 * Interface étroite pour éviter l'injection directe de {@code ThesaurusBrowseBean}
 * dans les beans satellites (recherche, alignements, propositions).
 */
public interface ConceptNavigationSupport {

    void openConcept(String conceptId);

    void focusGroup(String groupId);

    void focusFacet(String facetId);

    void refreshSelectedConcept();
}
