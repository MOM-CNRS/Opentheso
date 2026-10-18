package fr.cnrs.opentheso.concept.write.model.command;

public record UpsertNoteCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        String typeCode,
        String value,
        String source,
        int userId,
        String contributorName
) {
}
