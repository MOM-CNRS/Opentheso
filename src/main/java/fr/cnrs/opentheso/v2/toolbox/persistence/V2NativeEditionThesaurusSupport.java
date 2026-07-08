package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.v2.toolbox.session.EditionThesaurusLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeEditionThesaurusSupport implements EditionThesaurusLegacySupport {

    private final ThesaurusLifecyclePersistence thesaurusLifecyclePersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    @Override
    public void deleteAllHandleIds(String thesaurusId) {
        thesaurusLifecyclePersistence.deleteAllHandleIds(
                thesaurusId, toolboxPreferencePersistence.findPreferences(thesaurusId));
    }

    @Override
    public void deleteRights(String thesaurusId) {
        thesaurusLifecyclePersistence.deleteRights(thesaurusId);
    }

    @Override
    public boolean deleteThesaurus(String thesaurusId) {
        return thesaurusLifecyclePersistence.deleteThesaurus(thesaurusId);
    }
}
