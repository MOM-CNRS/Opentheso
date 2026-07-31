package fr.cnrs.opentheso.v2.proposition.model;

import java.io.Serializable;

/**
 * Détail d'une proposition pour le dialogue de revue.
 * Accesseurs {@code getXxx()} explicites pour l'EL JSF (les records seuls ne suffisent pas toujours).
 */
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

    public int getId() {
        return id;
    }

    public String getThesaurusId() {
        return thesaurusId;
    }

    public String getConceptId() {
        return conceptId;
    }

    public String getConceptLabel() {
        return conceptLabel;
    }

    public String getLang() {
        return lang;
    }

    public String getFlagCode() {
        return flagCode;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getComment() {
        return comment;
    }

    public String getStatus() {
        return status;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getAdminComment() {
        return adminComment;
    }
}
