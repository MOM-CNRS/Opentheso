package fr.cnrs.opentheso.concept.write.model.command;

public record AddNarrowerRelationCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        int userId,
        String contributorName
) {
}
