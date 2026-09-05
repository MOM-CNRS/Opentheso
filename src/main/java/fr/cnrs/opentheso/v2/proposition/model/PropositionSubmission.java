package fr.cnrs.opentheso.v2.proposition.model;

import java.io.Serializable;

public record PropositionSubmission(
        String thesaurusId,
        String thesaurusTitle,
        String conceptId,
        String conceptLabel,
        String lang,
        String authorName,
        String authorEmail,
        String comment,
        boolean allowMultiplePending
) implements Serializable {
    /**
     * Soumission UI classique : une seule proposition pending par auteur/concept/langue.
     */
    public PropositionSubmission(
            String thesaurusId,
            String thesaurusTitle,
            String conceptId,
            String conceptLabel,
            String lang,
            String authorName,
            String authorEmail,
            String comment
    ) {
        this(thesaurusId, thesaurusTitle, conceptId, conceptLabel, lang,
                authorName, authorEmail, comment, false);
    }
}
