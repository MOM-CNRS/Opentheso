package fr.cnrs.opentheso.collection.write.model.command;

public record AddCollectionTranslationCommand(
        String thesaurusId,
        String collectionId,
        String lang,
        String label
) {
}
