package fr.cnrs.opentheso.v2.collection.write.model.command;

public record CreateCollectionCommand(
        String thesaurusId,
        String lang,
        String label,
        String notation,
        String typeCode,
        int userId
) {
}
