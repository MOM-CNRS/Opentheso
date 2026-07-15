package fr.cnrs.opentheso.v2.collection.write.model.command;

public record MoveCollectionCommand(
        String thesaurusId,
        String collectionId,
        String targetParentCollectionId,
        boolean moveToRoot
) {
}
