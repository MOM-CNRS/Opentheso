package fr.cnrs.opentheso.v2.proposition.model;

public record PropositionSubmission(
        String thesaurusId,
        String thesaurusTitle,
        String conceptId,
        String conceptLabel,
        String lang,
        String authorName,
        String authorEmail,
        String comment
) {
}
