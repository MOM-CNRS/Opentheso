package fr.cnrs.opentheso.collection.write.model.command;

public record RemoveAllMembersFromCollectionCommand(
        String thesaurusId,
        String collectionId
) {
}
