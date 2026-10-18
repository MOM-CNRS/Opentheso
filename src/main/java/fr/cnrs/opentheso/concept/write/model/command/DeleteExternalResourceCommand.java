package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteExternalResourceCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String uri
) {
}
