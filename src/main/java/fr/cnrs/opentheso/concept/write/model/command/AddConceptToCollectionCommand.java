package fr.cnrs.opentheso.concept.write.model.command;

public record AddConceptToCollectionCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String collectionId,
        boolean applyToBranch
) {
}
