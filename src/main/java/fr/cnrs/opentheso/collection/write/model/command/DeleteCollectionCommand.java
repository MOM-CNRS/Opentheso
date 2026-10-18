package fr.cnrs.opentheso.collection.write.model.command;

public record DeleteCollectionCommand(
        String thesaurusId,
        String collectionId
) {
}
