package fr.cnrs.opentheso.v2.concept.write.model.command;

public record ApplyNarrowerRelationToBranchCommand(
        String thesaurusId,
        String conceptId,
        String ntRole,
        int userId,
        String contributorName
) {
}
