package fr.cnrs.opentheso.v2.collection.write.model.command;

public record UpdateCollectionTypeCommand(
        String thesaurusId,
        String collectionId,
        String typeCode
) {
}
