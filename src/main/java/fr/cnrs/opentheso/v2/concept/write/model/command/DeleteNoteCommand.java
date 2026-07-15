package fr.cnrs.opentheso.v2.concept.write.model.command;

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
