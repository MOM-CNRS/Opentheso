package fr.cnrs.opentheso.collection.write.model.command;

public record RemoveMemberFromCollectionCommand(
        String thesaurusId,
        String collectionId,
        String conceptId,
        boolean applyToBranch
) {
}
