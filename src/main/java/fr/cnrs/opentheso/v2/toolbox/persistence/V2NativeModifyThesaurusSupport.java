package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.v2.toolbox.session.ModifyThesaurusLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeModifyThesaurusSupport implements ModifyThesaurusLegacySupport {

    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxThesaurusArkPersistence toolboxThesaurusArkPersistence;
    private final ThesaurusLifecyclePersistence thesaurusLifecyclePersistence;

    @Override
    public String getWorkLanguage(String thesaurusId) {
        return toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
    }

    @Override
    public boolean setWorkLanguage(String languageCode, String thesaurusId) {
        return toolboxPreferencePersistence.setWorkLanguage(languageCode, thesaurusId);
    }

    @Override
    public void setVisibility(String thesaurusId, boolean privateThesaurus) {
        toolboxThesaurusPersistence.setVisibility(thesaurusId, privateThesaurus);
    }

    @Override
    public String generateArkIdForThesaurus(String thesaurusId) {
        return toolboxThesaurusArkPersistence.generateArkIdForThesaurus(thesaurusId);
    }

    @Override
    public boolean changeThesaurusId(String currentId, String newId) {
        return thesaurusLifecyclePersistence.changeThesaurusId(currentId, newId);
    }

    @Override
    public void addLanguageTranslation(Thesaurus thesaurus) {
        toolboxThesaurusPersistence.addTranslation(thesaurus);
    }

    @Override
    public boolean updateLanguageTranslation(Thesaurus thesaurus) {
        return toolboxThesaurusPersistence.updateTranslation(thesaurus);
    }

    @Override
    public void deleteLanguageTranslation(String thesaurusId, String languageCode) {
        toolboxThesaurusPersistence.deleteTranslation(thesaurusId, languageCode);
    }
}
