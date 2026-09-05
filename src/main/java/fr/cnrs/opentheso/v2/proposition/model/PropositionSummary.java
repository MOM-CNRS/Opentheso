package fr.cnrs.opentheso.v2.proposition.model;

import fr.cnrs.opentheso.models.propositions.PropositionStatusEnum;
import java.io.Serializable;

/**
 * Résumé d'une proposition pour le tiroir latéral.
 * Accesseurs {@code getXxx()} pour l'EL JSF.
 */
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

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getStatus() {
        return status;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getLang() {
        return lang;
    }

    public String getFlagCode() {
        return flagCode;
    }

    public boolean isEnvoyer() {
        return PropositionStatusEnum.ENVOYER.name().equalsIgnoreCase(status);
    }

    public boolean isLu() {
        return PropositionStatusEnum.LU.name().equalsIgnoreCase(status);
    }

    public boolean isApprouver() {
        return PropositionStatusEnum.APPROUVER.name().equalsIgnoreCase(status);
    }

    public boolean isRefuser() {
        return PropositionStatusEnum.REFUSER.name().equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return isEnvoyer() || isLu();
    }
}
