package fr.cnrs.opentheso.v2.toolbox.session;

public interface ThesaurusMaintenanceLegacySupport {

    int correctDisplayTopTerm(String thesaurusId);

    void reorganizeHierarchy(String thesaurusId);

    void reorganizeConceptsAndCollections(String thesaurusId);

    void switchRolesFromTermToConcept(String thesaurusId, String workLanguage);

    int generateArkFromConceptId(String thesaurusId, String prefix, String naan, boolean overwrite);

    int generateLocalArk(String thesaurusId, boolean overwrite);

    void generateSitemap(String thesaurusId);
}
