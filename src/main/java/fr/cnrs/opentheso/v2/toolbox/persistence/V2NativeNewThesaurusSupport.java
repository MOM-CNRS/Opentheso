package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.v2.toolbox.session.NewThesaurusLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeNewThesaurusSupport implements NewThesaurusLegacySupport {

    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    @Override
    public String createThesaurusId() {
        return toolboxThesaurusPersistence.createThesaurusId();
    }

    @Override
    public void addTranslation(Thesaurus thesaurus) {
        toolboxThesaurusPersistence.addTranslation(thesaurus);
    }

    @Override
    public void linkToProject(UserGroupThesaurus link) {
        toolboxThesaurusPersistence.linkToProject(link);
    }

    @Override
    public void initPreferences(String thesaurusId, String language) {
        toolboxPreferencePersistence.initPreferences(thesaurusId, language);
    }
}
