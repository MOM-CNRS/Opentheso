package fr.cnrs.opentheso.v2.concept.write.model.command;

public record AddManualAlignmentCommand(
        String thesaurusId,
        String conceptId,
        int typeId,
        String uri,
        String source,
        int userId,
        String contributorName
) {
}
