package fr.cnrs.opentheso.v2.concept.write.model.command;

public record UpdateAlignmentCommand(
        String thesaurusId,
        String conceptId,
        int alignmentId,
        int typeId,
        String uri,
        String source,
        int userId,
        String contributorName
) {
}
