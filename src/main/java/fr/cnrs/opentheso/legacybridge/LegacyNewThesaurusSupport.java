package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.session.NewThesaurusLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyNewThesaurusSupport implements NewThesaurusLegacySupport {

    private final ThesaurusService thesaurusService;
    private final GroupService groupService;
    private final PreferenceService preferenceService;

    @Override
    public String createThesaurusId() {
        return thesaurusService.addThesaurusRollBack();
    }

    @Override
    public void addTranslation(Thesaurus thesaurus) {
        thesaurusService.addThesaurusTraductionRollBack(thesaurus);
    }

    @Override
    public void linkToProject(UserGroupThesaurus link) {
        groupService.saveUserGroupThesaurus(link);
    }

    @Override
    public void initPreferences(String thesaurusId, String language) {
        preferenceService.initPreferences(thesaurusId, language);
    }
}
