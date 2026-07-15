package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.session.EditionThesaurusLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyEditionThesaurusSupport implements EditionThesaurusLegacySupport {

    private final ConceptService conceptService;
    private final ThesaurusService thesaurusService;

    @Override
    public void deleteAllHandleIds(String thesaurusId) {
        conceptService.deleteAllIdHandle(thesaurusId);
    }

    @Override
    public void deleteRights(String thesaurusId) {
        thesaurusService.deleteDroitByThesaurus(thesaurusId);
    }

    @Override
    public boolean deleteThesaurus(String thesaurusId) {
        return thesaurusService.deleteThesaurus(thesaurusId);
    }
}
