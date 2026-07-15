package fr.cnrs.opentheso.v2.collection.write.model.command;

public record DeleteCollectionCommand(
        String thesaurusId,
        String collectionId
) {
}
