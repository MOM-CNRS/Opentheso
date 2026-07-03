package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.services.EditThesaurusService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.session.ModifyThesaurusLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyModifyThesaurusSupport implements ModifyThesaurusLegacySupport {

    private final ThesaurusService thesaurusService;
    private final PreferenceService preferenceService;
    private final EditThesaurusService editThesaurusService;

    @Override
    public String getWorkLanguage(String thesaurusId) {
        return preferenceService.getWorkLanguageOfThesaurus(thesaurusId);
    }

    @Override
    public boolean setWorkLanguage(String languageCode, String thesaurusId) {
        return preferenceService.setWorkLanguageOfThesaurus(languageCode, thesaurusId);
    }

    @Override
    public void setVisibility(String thesaurusId, boolean privateThesaurus) {
        thesaurusService.setThesaurusVisibility(thesaurusId, privateThesaurus);
    }

    @Override
    public String generateArkIdForThesaurus(String thesaurusId) {
        return editThesaurusService.generateArkIdForThesaurus(thesaurusId);
    }

    @Override
    public boolean changeThesaurusId(String currentId, String newId) {
        return thesaurusService.changeIdOfThesaurus(currentId, newId);
    }

    @Override
    public void addLanguageTranslation(Thesaurus thesaurus) {
        thesaurusService.addThesaurusTraductionRollBack(thesaurus);
    }

    @Override
    public boolean updateLanguageTranslation(Thesaurus thesaurus) {
        return thesaurusService.updateThesaurus(thesaurus);
    }

    @Override
    public void deleteLanguageTranslation(String thesaurusId, String languageCode) {
        thesaurusService.deleteThesaurusTraduction(thesaurusId, languageCode);
    }
}
