package fr.cnrs.opentheso.collection.write.model.command;

public record AddMemberToCollectionCommand(
        String thesaurusId,
        String collectionId,
        String conceptId,
        boolean applyToBranch
) {
}
