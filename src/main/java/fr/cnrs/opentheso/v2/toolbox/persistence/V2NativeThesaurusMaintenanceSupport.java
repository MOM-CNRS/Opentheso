package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.v2.toolbox.session.ThesaurusMaintenanceLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusMaintenanceSupport implements ThesaurusMaintenanceLegacySupport {

    private final ThesaurusMaintenancePersistence thesaurusMaintenancePersistence;

    @Override
    public int correctDisplayTopTerm(String thesaurusId) {
        return thesaurusMaintenancePersistence.correctDisplayTopTerm(thesaurusId);
    }

    @Override
    public void reorganizeHierarchy(String thesaurusId) {
        thesaurusMaintenancePersistence.reorganizeHierarchy(thesaurusId);
    }

    @Override
    public void reorganizeConceptsAndCollections(String thesaurusId) {
        thesaurusMaintenancePersistence.reorganizeConceptsAndCollections(thesaurusId);
    }

    @Override
    public void switchRolesFromTermToConcept(String thesaurusId, String workLanguage) {
        thesaurusMaintenancePersistence.switchRolesFromTermToConcept(thesaurusId, workLanguage);
    }

    @Override
    public int generateArkFromConceptId(String thesaurusId, String prefix, String naan, boolean overwrite) {
        return thesaurusMaintenancePersistence.generateArkFromConceptId(thesaurusId, prefix, naan, overwrite);
    }

    @Override
    public int generateLocalArk(String thesaurusId, boolean overwrite) {
        return thesaurusMaintenancePersistence.generateLocalArk(thesaurusId, overwrite);
    }

    @Override
    public void generateSitemap(String thesaurusId) {
        thesaurusMaintenancePersistence.generateSitemap(thesaurusId);
    }
}
