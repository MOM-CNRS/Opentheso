package fr.cnrs.opentheso.v2.candidat.model;

/**
 * Ligne du tableau de bord candidats V2 (EL Facelets).
 */
public record CandidatBoardItem(
        String conceptId,
        String title,
        String meta,
        String votes,
        String openType
) {

    public String getConceptId() {
        return conceptId;
    }

    public String getTitle() {
        return title;
    }

    public String getMeta() {
        return meta;
    }

    public String getVotes() {
        return votes;
    }

    public String getOpenType() {
        return openType;
    }
}
