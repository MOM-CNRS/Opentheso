package fr.cnrs.opentheso.concept.write.model.command;

public record AddReplacedByCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        int userId,
        String contributorName
) {
}
