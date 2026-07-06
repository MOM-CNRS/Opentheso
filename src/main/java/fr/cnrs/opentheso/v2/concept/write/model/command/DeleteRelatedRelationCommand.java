package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteRelatedRelationCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        int userId,
        String contributorName
) {
}
