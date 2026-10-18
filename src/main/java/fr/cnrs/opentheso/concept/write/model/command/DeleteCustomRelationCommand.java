package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteCustomRelationCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        String relationCode,
        boolean reciprocal,
        int userId,
        String contributorName
) {
}
