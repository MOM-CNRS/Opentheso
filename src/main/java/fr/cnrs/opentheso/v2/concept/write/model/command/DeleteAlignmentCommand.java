package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteAlignmentCommand(
        String thesaurusId,
        String conceptId,
        int alignmentId,
        int userId,
        String contributorName
) {
}
