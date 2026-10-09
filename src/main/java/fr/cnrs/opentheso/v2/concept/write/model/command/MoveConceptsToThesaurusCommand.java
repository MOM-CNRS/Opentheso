package fr.cnrs.opentheso.v2.concept.write.model.command;

import java.util.List;

/**
 * Transfert groupé vers un autre thésaurus. {@code parentConceptId} null = racine cible.
 */
public record MoveConceptsToThesaurusCommand(
        String sourceThesaurusId,
        String targetThesaurusId,
        List<String> conceptIds,
        String lang,
        int userId,
        String contributorName,
        String parentConceptId
) {
}
