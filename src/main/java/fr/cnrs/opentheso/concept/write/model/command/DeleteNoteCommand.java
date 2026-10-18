package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteNoteCommand(
        String thesaurusId,
        String conceptId,
        int noteId,
        String lang,
        String typeCode,
        int userId,
        String contributorName
) {
}
