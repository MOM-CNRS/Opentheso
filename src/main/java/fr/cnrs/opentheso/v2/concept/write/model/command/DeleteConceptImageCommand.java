package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteConceptImageCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String uri
) {
}
