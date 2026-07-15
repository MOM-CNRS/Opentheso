package fr.cnrs.opentheso.v2.concept.write.model.command;

public record RenamePreferredLabelCommand(
        String thesaurusId,
        String conceptId,
        String lang,
        int userId,
        String contributorName,
        String label,
        String source,
        boolean forced
) {
}
