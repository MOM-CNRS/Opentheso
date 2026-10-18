package fr.cnrs.opentheso.shared.session;

import java.util.Optional;

/**
 * Concept actuellement sélectionné dans la consultation V2.
 */
public interface ConceptSelectionSource {

    Optional<String> getSelectedConceptId();
}
