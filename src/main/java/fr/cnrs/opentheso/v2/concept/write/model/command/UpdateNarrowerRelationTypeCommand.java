package fr.cnrs.opentheso.v2.concept.write.model.command;

public record UpdateNarrowerRelationTypeCommand(
        String thesaurusId,
        String conceptId,
        String targetConceptId,
        String ntRole,
        int userId,
        String contributorName
) {
}
