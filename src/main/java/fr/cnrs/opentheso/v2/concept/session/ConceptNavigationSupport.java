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

    void refreshAfterRename(String conceptId, String newLabel);

    void refreshAfterNotationUpdate(String conceptId, String notation);

    void invalidateConceptTree();

    void openThesaurusHome();

    void afterConceptDeleted(String fallbackConceptId);

    /**
     * Recharge arbres et panneau droit après un changement de langue de consultation.
     */
    void reloadAfterLanguageChange();
}
