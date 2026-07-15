package fr.cnrs.opentheso.v2.candidat.session;

/**
 * Pont vers les beans legacy d'alignement encore référencés par les dialogues JSF.
 */
public interface CandidatAlignmentSupport {

    void resetManualAlignment();

    void prepareAutoAlignment(String conceptLabel, String conceptId, String thesaurusId, String lang);

    boolean hasAlignmentSources();

    void addAlignment(String thesaurusId, String conceptId, int userId);
}
