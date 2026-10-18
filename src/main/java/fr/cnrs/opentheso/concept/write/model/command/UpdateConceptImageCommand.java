package fr.cnrs.opentheso.concept.write.model.command;

public record UpdateConceptImageCommand(
        String thesaurusId,
        String conceptId,
        int userId,
        String contributorName,
        int imageId,
        String uri,
        String name,
        String creator,
        String copyright
) {
}
