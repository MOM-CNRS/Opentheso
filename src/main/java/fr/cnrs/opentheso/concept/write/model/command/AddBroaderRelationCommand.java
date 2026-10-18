package fr.cnrs.opentheso.concept.write.model.command;

public record AddBroaderRelationCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        int userId,
        String contributorName
) {
}
