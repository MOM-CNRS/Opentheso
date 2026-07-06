package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteHandleCommand(
        String thesaurusId,
        String conceptId,
        String handleId
) {
}
