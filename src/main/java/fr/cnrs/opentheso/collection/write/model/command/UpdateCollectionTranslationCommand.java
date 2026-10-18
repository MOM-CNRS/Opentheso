package fr.cnrs.opentheso.collection.write.model.command;

public record UpdateCollectionTranslationCommand(
        String thesaurusId,
        String collectionId,
        String lang,
        String label,
        int userId
) {
}
