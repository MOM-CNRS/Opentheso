package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteHandleCommand(
        String thesaurusId,
        String conceptId,
        String handleId
) {
}
