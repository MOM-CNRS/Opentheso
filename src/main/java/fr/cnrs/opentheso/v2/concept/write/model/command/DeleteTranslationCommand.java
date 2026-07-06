package fr.cnrs.opentheso.v2.concept.write.model.command;

public record DeleteTranslationCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        int userId,
        String contributorName
) {
}
