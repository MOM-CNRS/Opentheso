package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteSynonymCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        String value,
        int userId,
        String contributorName
) {
}
