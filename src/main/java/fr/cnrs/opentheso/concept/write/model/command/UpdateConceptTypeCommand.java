package fr.cnrs.opentheso.concept.write.model.command;

public record UpdateConceptTypeCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String conceptTypeCode,
        boolean applyToBranch
) {
}
