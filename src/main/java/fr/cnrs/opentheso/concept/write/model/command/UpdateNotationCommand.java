package fr.cnrs.opentheso.concept.write.model.command;

public record UpdateNotationCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String notation
) {
}
