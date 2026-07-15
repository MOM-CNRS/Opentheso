package fr.cnrs.opentheso.v2.collection.write.model.command;

public record UpdateCollectionNotationCommand(
        String thesaurusId,
        String collectionId,
        String notation
) {
}
