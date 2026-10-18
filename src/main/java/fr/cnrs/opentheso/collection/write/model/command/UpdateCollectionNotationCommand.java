package fr.cnrs.opentheso.collection.write.model.command;

public record UpdateCollectionNotationCommand(
        String thesaurusId,
        String collectionId,
        String notation
) {
}
