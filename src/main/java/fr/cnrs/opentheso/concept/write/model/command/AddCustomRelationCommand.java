package fr.cnrs.opentheso.concept.write.model.command;

public record AddCustomRelationCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        int userId,
        String contributorName
) {
}
