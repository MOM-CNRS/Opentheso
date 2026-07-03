package fr.cnrs.opentheso.v2.proposition.model;

public record PropositionSummary(
        int id,
        String thesaurusId,
        String conceptId,
        String conceptLabel,
        String authorName,
        String authorEmail,
        String status,
        String publishedAt,
        String lang,
        String flagCode
) {
}
