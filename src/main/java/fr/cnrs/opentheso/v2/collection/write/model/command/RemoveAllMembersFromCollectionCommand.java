package fr.cnrs.opentheso.v2.collection.write.model.command;

public record RemoveAllMembersFromCollectionCommand(
        String thesaurusId,
        String collectionId
) {
}
