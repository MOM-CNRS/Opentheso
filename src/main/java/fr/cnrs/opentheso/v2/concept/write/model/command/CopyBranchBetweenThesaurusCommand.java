package fr.cnrs.opentheso.v2.concept.write.model.command;

/**
 * Copie d'une branche vers un autre thésaurus (duplication SKOS), pas un déplacement.
 */
public record CopyBranchBetweenThesaurusCommand(
        String sourceThesaurusId,
        String sourceConceptId,
        String targetThesaurusId,
        String targetParentConceptId,
        boolean dropToRoot,
        String identifierType,
        int userId
) {
}
