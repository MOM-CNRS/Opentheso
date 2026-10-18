package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteAlignmentCommand(
        String thesaurusId,
        String conceptId,
        int alignmentId,
        int userId,
        String contributorName
) {
}
