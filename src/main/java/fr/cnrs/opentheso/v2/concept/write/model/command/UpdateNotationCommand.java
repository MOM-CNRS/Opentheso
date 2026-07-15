package fr.cnrs.opentheso.v2.concept.write.model.command;

public record UpdateNotationCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String notation
) {
}
