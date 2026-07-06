package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteExternalResourceCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        String uri
) {
}
