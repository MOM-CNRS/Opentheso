package fr.cnrs.opentheso.concept.write.model.command;

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
