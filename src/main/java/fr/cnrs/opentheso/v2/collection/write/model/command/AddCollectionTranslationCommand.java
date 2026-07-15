package fr.cnrs.opentheso.v2.collection.write.model.command;

public record AddCollectionTranslationCommand(
        String thesaurusId,
        String collectionId,
        String lang,
        String label
) {
}
