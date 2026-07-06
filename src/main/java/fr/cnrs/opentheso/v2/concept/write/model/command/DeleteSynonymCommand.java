package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteSynonymCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        String value,
        int userId,
        String contributorName
) {
}
