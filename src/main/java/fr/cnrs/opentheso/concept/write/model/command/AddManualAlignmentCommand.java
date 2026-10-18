package fr.cnrs.opentheso.concept.write.model.command;

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
