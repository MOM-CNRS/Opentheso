package fr.cnrs.opentheso.v2.toolbox.session;

import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;

public interface NewThesaurusLegacySupport {

    String createThesaurusId();

    void addTranslation(Thesaurus thesaurus);

    void linkToProject(UserGroupThesaurus link);

    void initPreferences(String thesaurusId, String language);
}
