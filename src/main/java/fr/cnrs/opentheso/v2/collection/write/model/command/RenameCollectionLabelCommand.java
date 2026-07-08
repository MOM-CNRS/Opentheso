package fr.cnrs.opentheso.v2.collection.write.model.command;

public record RenameCollectionLabelCommand(
        String thesaurusId,
        String collectionId,
        String lang,
        String label,
        int userId
) {
}
