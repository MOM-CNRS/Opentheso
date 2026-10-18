package fr.cnrs.opentheso.collection.write.model.command;

public record MoveCollectionCommand(
        String thesaurusId,
        String collectionId,
        String targetParentCollectionId,
        boolean moveToRoot
) {
}
