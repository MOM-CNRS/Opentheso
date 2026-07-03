package fr.cnrs.opentheso.v2.toolbox.session;

import fr.cnrs.opentheso.models.thesaurus.Thesaurus;

public interface ModifyThesaurusLegacySupport {

    String getWorkLanguage(String thesaurusId);

    boolean setWorkLanguage(String languageCode, String thesaurusId);

    void setVisibility(String thesaurusId, boolean privateThesaurus);

    String generateArkIdForThesaurus(String thesaurusId);

    boolean changeThesaurusId(String currentId, String newId);

    void addLanguageTranslation(Thesaurus thesaurus);

    boolean updateLanguageTranslation(Thesaurus thesaurus);

    void deleteLanguageTranslation(String thesaurusId, String languageCode);
}
