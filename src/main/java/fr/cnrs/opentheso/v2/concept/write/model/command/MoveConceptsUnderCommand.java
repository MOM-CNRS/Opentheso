package fr.cnrs.opentheso.v2.concept.write.model.command;

import java.util.List;

/**
 * Déplacement groupé dans le même thésaurus. {@code newBroaderId} null = racine.
 */
public record MoveConceptsUnderCommand(
        String thesaurusId,
        List<String> conceptIds,
        String newBroaderId,
        int userId,
        String contributorName
) {
}
