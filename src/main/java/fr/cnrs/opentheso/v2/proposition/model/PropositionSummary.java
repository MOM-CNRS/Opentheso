package fr.cnrs.opentheso.v2.proposition.model;

import fr.cnrs.opentheso.models.propositions.PropositionStatusEnum;
import java.io.Serializable;

/**
 * Résumé d'une proposition pour le tiroir latéral.
 * {@code RecordELResolver} résout {@code #{item.envoyer}} via {@code envoyer()},
 * pas via {@code isEnvoyer()}.
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

    public boolean envoyer() {
        return matches(PropositionStatusEnum.ENVOYER);
    }

    public boolean lu() {
        return matches(PropositionStatusEnum.LU);
    }

    public boolean approuver() {
        return matches(PropositionStatusEnum.APPROUVER);
    }

    public boolean refuser() {
        return matches(PropositionStatusEnum.REFUSER);
    }

    public boolean pending() {
        return envoyer() || lu();
    }

    /** Classe CSS pour le tableau de bord ({@code #{item.statusCss}}). */
    public String statusCss() {
        if (envoyer()) {
            return "is-new";
        }
        if (lu()) {
            return "is-read";
        }
        if (approuver()) {
            return "is-approved";
        }
        if (refuser()) {
            return "is-refused";
        }
        return "";
    }

    public boolean isEnvoyer() {
        return envoyer();
    }

    public boolean isLu() {
        return lu();
    }

    public boolean isApprouver() {
        return approuver();
    }

    public boolean isRefuser() {
        return refuser();
    }

    public boolean isPending() {
        return pending();
    }

    private boolean matches(PropositionStatusEnum expected) {
        return expected.name().equalsIgnoreCase(status);
    }
}
