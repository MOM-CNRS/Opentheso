package fr.cnrs.opentheso.v2.concept.write.model.command;

public record AddRelatedRelationCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        String lang,
        int userId,
        String contributorName,
        boolean tagPrefLabel
) {
}
