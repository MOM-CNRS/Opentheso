package fr.cnrs.opentheso.v2.collection.write.model.command;

public record UpdateCollectionTranslationCommand(
        String thesaurusId,
        String collectionId,
        String lang,
        String label,
        int userId
) {
}
