package fr.cnrs.opentheso.concept.write.model.command;

public record DeleteTranslationCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        int userId,
        String contributorName
) {
}
