package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteBroaderRelationCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        int userId,
        String contributorName
) {
}
