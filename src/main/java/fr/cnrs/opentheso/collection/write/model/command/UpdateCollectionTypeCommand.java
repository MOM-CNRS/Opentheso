package fr.cnrs.opentheso.collection.write.model.command;

public record UpdateCollectionTypeCommand(
        String thesaurusId,
        String collectionId,
        String typeCode
) {
}
