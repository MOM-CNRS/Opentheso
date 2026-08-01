package fr.cnrs.opentheso.v2.concept.write.model.command;

import java.util.List;

/**
 * Déplacement d'un concept dans l'arbre (équivalent legacy {@code ConceptService.moveBranch*}).
 * {@code newBroaderId} null = racine (top concept).
 */
public record ReparentConceptCommand(
        String thesaurusId,
        String conceptId,
        List<String> broaderIdsToDetach,
        String newBroaderId,
        int userId,
        String contributorName
) {
}
