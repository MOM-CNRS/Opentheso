package fr.cnrs.opentheso.collection.write.model.command;

public record CreateSubgroupCommand(
        String thesaurusId,
        String parentCollectionId,
        String lang,
        String label,
        String notation,
        String typeCode,
        int userId
) {
}
