package fr.cnrs.opentheso.v2.collection.write.model.command;

public record RemoveMemberFromCollectionCommand(
        String thesaurusId,
        String collectionId,
        String conceptId,
        boolean applyToBranch
) {
}
