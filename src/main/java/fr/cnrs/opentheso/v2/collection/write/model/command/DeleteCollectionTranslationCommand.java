package fr.cnrs.opentheso.v2.collection.write.model.command;

public record DeleteCollectionTranslationCommand(
        String thesaurusId,
        String collectionId,
        String lang
) {
}
