package fr.cnrs.opentheso.v2.proposition.model;

import java.io.Serializable;

public record PropositionDetail(
        int id,
        String thesaurusId,
        String conceptId,
        String conceptLabel,
        String lang,
        String flagCode,
        String authorName,
        String authorEmail,
        String comment,
        String status,
        String publishedAt,
        String reviewedBy,
        String adminComment
) implements Serializable {
}
