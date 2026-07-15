package fr.cnrs.opentheso.v2.shared.session;

import java.util.Optional;

/**
 * Concept actuellement sélectionné dans l'UI legacy (arbre), pour les écrans v2 ouverts depuis le legacy.
 */
public interface ConceptSelectionSource {

    Optional<String> getSelectedConceptId();
}
