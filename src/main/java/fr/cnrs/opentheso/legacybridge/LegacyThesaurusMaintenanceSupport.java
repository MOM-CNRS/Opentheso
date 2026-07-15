package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.session.ThesaurusMaintenanceLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyThesaurusMaintenanceSupport implements ThesaurusMaintenanceLegacySupport {

    private final RestoreThesaurusService restoreThesaurusService;

    @Override
    public int correctDisplayTopTerm(String thesaurusId) {
        return restoreThesaurusService.correctDisplayTopTerm(thesaurusId);
    }

    @Override
    public void reorganizeHierarchy(String thesaurusId) {
        restoreThesaurusService.reorganizing(thesaurusId);
    }

    @Override
    public void reorganizeConceptsAndCollections(String thesaurusId) {
        restoreThesaurusService.reorganizeConceptsAndCollections(thesaurusId);
    }

    @Override
    public void switchRolesFromTermToConcept(String thesaurusId, String workLanguage) {
        restoreThesaurusService.switchRolesFromTermToConcept(thesaurusId, workLanguage);
    }

    @Override
    public int generateArkFromConceptId(String thesaurusId, String prefix, String naan, boolean overwrite) {
        return restoreThesaurusService.generateArkFromConceptId(thesaurusId, prefix, naan, overwrite);
    }

    @Override
    public int generateLocalArk(String thesaurusId, boolean overwrite) {
        return restoreThesaurusService.generateArkLacal(thesaurusId, overwrite);
    }

    @Override
    public void generateSitemap(String thesaurusId) {
        restoreThesaurusService.generateSitemap(thesaurusId);
    }
}
