package fr.cnrs.opentheso.collection.write.model.command;

public record DeleteCollectionTranslationCommand(
        String thesaurusId,
        String collectionId,
        String lang
) {
}
